package dev.avelar.astazou.service;

import tools.jackson.databind.ObjectMapper;
import dev.avelar.astazou.dto.PythonScriptForm;
import dev.avelar.astazou.dto.ScriptExecutionResult;
import dev.avelar.astazou.exception.NotFoundException;
import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.model.PythonScript;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.repository.BankAccountRepository;
import dev.avelar.astazou.repository.PythonScriptRepository;
import dev.avelar.astazou.repository.TransactionRepository;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class PythonScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PythonScriptService.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Value("${astazou.scripts.max-transactions:500}")
  private int maxTransactions;

  @Value("${astazou.scripts.timeout-seconds:30}")
  private int timeoutSeconds;

  @Value("${astazou.scripts.pip-timeout-seconds:60}")
  private int pipTimeoutSeconds;

  @Value("${astazou.python.interpreter:python3}")
  private String pythonInterpreter;

  private final PythonScriptRepository repository;
  private final TransactionRepository transactionRepository;
  private final BankAccountRepository bankAccountRepository;

  @Autowired
  public PythonScriptService(PythonScriptRepository repository,
      TransactionRepository transactionRepository,
      BankAccountRepository bankAccountRepository) {
    this.repository = repository;
    this.transactionRepository = transactionRepository;
    this.bankAccountRepository = bankAccountRepository;
  }

  // ── CRUD ──────────────────────────────────────────────────────────────────

  public List<PythonScript> findByUsername(String username) {
    return repository.findByUsername(username);
  }

  public PythonScript findByIdAndUsername(Long id, String username) {
    return repository.findByIdAndUsername(id, username)
        .orElseThrow(() -> new NotFoundException("Script not found: " + id));
  }

  public PythonScript create(String username, PythonScriptForm form) {
    PythonScript script = PythonScript.builder()
        .username(username)
        .name(form.getName())
        .description(form.getDescription())
        .code(form.getCode() != null ? form.getCode() : "")
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
    return repository.save(script);
  }

  public PythonScript update(Long id, String username, PythonScriptForm form) {
    PythonScript script = findByIdAndUsername(id, username);
    script.setName(form.getName());
    script.setDescription(form.getDescription());
    script.setCode(form.getCode() != null ? form.getCode() : "");
    script.setUpdatedAt(OffsetDateTime.now());
    return repository.save(script);
  }

  public void delete(Long id, String username) {
    repository.deleteByIdAndUsername(id, username);
  }

  // ── EXECUTION ─────────────────────────────────────────────────────────────

  /**
   * Execute a stored script.
   *
   * @param scriptId     the script to run
   * @param username     the owner (used for auth + data fetch)
   * @param requirements pip-format package list (may be null or blank = no install)
   */
  public ScriptExecutionResult execute(Long scriptId, String username, String requirements) {
    PythonScript script = findByIdAndUsername(scriptId, username);

    List<Transaction> transactions = transactionRepository.findRecentByUsername(username, maxTransactions);
    List<BankAccount> accounts = bankAccountRepository.findAllByUsername(username);

    String cleanReqs = (requirements != null && !requirements.isBlank()) ? requirements.strip() : null;
    return runWithTimeout(script.getCode(), transactions, accounts, cleanReqs);
  }

  // ── Private execution helpers ─────────────────────────────────────────────

  private ScriptExecutionResult runWithTimeout(String code,
      List<Transaction> transactions, List<BankAccount> accounts, String requirements) {

    // ── Ephemeral dependency install ────────────────────────────────────────
    Path depsDir = null;
    String installLog = null;

    if (requirements != null) {
      try {
        depsDir = Files.createTempDirectory("astazou_deps_");
      } catch (IOException e) {
        return new ScriptExecutionResult("",
            "Failed to create temp directory for dependencies: " + e.getMessage(),
            1, 0L, null);
      }

      PipResult pip = installDependencies(requirements, depsDir);
      installLog = pip.log();

      if (!pip.success()) {
        cleanupTempDir(depsDir);
        return new ScriptExecutionResult("",
            "Dependency installation failed. See install log for details.",
            2, 0L, installLog);
      }
    }

    // ── Script execution with timeout ───────────────────────────────────────
    final Path finalDepsDir = depsDir;
    final String finalInstallLog = installLog;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<ScriptExecutionResult> future = executor.submit(
          () -> runScript(code, transactions, accounts, finalDepsDir, finalInstallLog));
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      return new ScriptExecutionResult("",
          "Script execution timed out after " + timeoutSeconds + " seconds.",
          1, (long) timeoutSeconds * 1000, finalInstallLog);
    } catch (Exception e) {
      return new ScriptExecutionResult("", e.getMessage() != null ? e.getMessage() : e.getClass().getName(),
          1, 0L, finalInstallLog);
    } finally {
      executor.shutdownNow();
      if (finalDepsDir != null) {
        cleanupTempDir(finalDepsDir);
      }
    }
  }

  private ScriptExecutionResult runScript(String code,
      List<Transaction> transactions, List<BankAccount> accounts,
      Path depsDir, String installLog) {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    long start = System.currentTimeMillis();
    int exitCode = 0;

    try {
      String txJson = MAPPER.writeValueAsString(buildTransactionMaps(transactions));
      String acJson = MAPPER.writeValueAsString(buildAccountMaps(accounts));

      // Allow host file access when custom deps need to be imported
      IOAccess ioAccess = depsDir != null
          ? IOAccess.newBuilder().allowHostFileAccess(true).build()
          : IOAccess.NONE;

      try (Context context = Context.newBuilder("python")
          .allowAllAccess(false)
          .allowIO(ioAccess)
          .out(out)
          .err(err)
          .build()) {

        context.getBindings("python").putMember("_tx_json", txJson);
        context.getBindings("python").putMember("_ac_json", acJson);

        // Build the script preamble
        StringBuilder preamble = new StringBuilder();
        if (depsDir != null) {
          // Prepend the ephemeral package directory to sys.path
          String safePath = depsDir.toAbsolutePath().toString()
              .replace("\\", "\\\\").replace("'", "\\'");
          preamble.append("import sys\n");
          preamble.append("sys.path.insert(0, '").append(safePath).append("')\n");
        }
        preamble.append("import json\n");
        preamble.append("transactions = json.loads(_tx_json)\n");
        preamble.append("accounts = json.loads(_ac_json)\n");

        context.eval("python", preamble + "\n" + code);
      }

    } catch (PolyglotException e) {
      writeErr(err, formatPolyglotError(e));
      exitCode = 1;
    } catch (Exception e) {
      writeErr(err, e.getMessage() != null ? e.getMessage() : e.getClass().getName());
      exitCode = 1;
    }

    return new ScriptExecutionResult(
        out.toString(StandardCharsets.UTF_8),
        err.toString(StandardCharsets.UTF_8),
        exitCode,
        System.currentTimeMillis() - start,
        installLog
    );
  }

  // ── Pip install ───────────────────────────────────────────────────────────

  private record PipResult(String log, boolean success) {}

  private PipResult installDependencies(String requirements, Path depsDir) {
    try {
      // Write requirements.txt to temp file alongside the deps dir
      Path reqFile = Files.createTempFile(depsDir, "requirements", ".txt");
      Files.writeString(reqFile, requirements, StandardCharsets.UTF_8);

      ProcessBuilder pb = new ProcessBuilder(
          pythonInterpreter, "-m", "pip", "install",
          "--no-cache-dir",
          "--disable-pip-version-check",
          "--target", depsDir.toString(),
          "-r", reqFile.toString()
      );
      pb.environment().put("PYTHONNOUSERSITE", "1");
      pb.environment().put("PIP_NO_CACHE_DIR", "1");
      pb.environment().put("PYTHONDONTWRITEBYTECODE", "1");
      pb.redirectErrorStream(true); // merge stderr into stdout for a single log

      LOGGER.info("Installing ephemeral pip deps into {}", depsDir);
      Process process = pb.start();

      String log;
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        log = reader.lines().collect(Collectors.joining("\n"));
      }

      boolean finished = process.waitFor(pipTimeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        return new PipResult(log + "\n[ERROR] pip install timed out after "
            + pipTimeoutSeconds + " seconds.", false);
      }

      int rc = process.exitValue();
      LOGGER.info("pip install exited with code {} for dir {}", rc, depsDir);
      return new PipResult(log, rc == 0);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new PipResult("pip install was interrupted.", false);
    } catch (Exception e) {
      return new PipResult("Failed to run pip: " + e.getMessage(), false);
    }
  }

  // ── Cleanup ───────────────────────────────────────────────────────────────

  private void cleanupTempDir(Path dir) {
    try (var stream = Files.walk(dir)) {
      stream.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
          });
    } catch (IOException e) {
      LOGGER.warn("Failed to clean up temp dir {}: {}", dir, e.getMessage());
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private String formatPolyglotError(PolyglotException e) {
    if (e.isSyntaxError()) {
      var loc = e.getSourceLocation();
      if (loc != null) {
        return "SyntaxError at line " + loc.getStartLine() + ": " + e.getMessage();
      }
      return "SyntaxError: " + e.getMessage();
    }
    return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
  }

  private void writeErr(ByteArrayOutputStream err, String message) {
    try {
      err.write(message.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ignored) {
    }
  }

  private List<Map<String, Object>> buildTransactionMaps(List<Transaction> list) {
    return list.stream().map(t -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", t.getId());
      m.put("date", t.getTransactionDate() != null ? t.getTransactionDate().toString() : null);
      m.put("description", t.getDescription() != null ? t.getDescription() : "");
      m.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
      m.put("type", t.getType());
      m.put("account_id", t.getBankAccountId());
      m.put("tags", t.getTags() != null ? List.of(t.getTags()) : List.of());
      return m;
    }).toList();
  }

  private List<Map<String, Object>> buildAccountMaps(List<BankAccount> list) {
    return list.stream().map(a -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", a.getId());
      m.put("name", a.getName());
      m.put("balance", a.getBalance() != null ? a.getBalance().doubleValue() : 0.0);
      m.put("currency", a.getCurrency());
      return m;
    }).toList();
  }
}

