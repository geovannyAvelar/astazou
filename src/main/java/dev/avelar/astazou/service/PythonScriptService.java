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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PythonScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PythonScriptService.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Value("${astazou.scripts.max-transactions:500}")
  private int maxTransactions;

  @Value("${astazou.scripts.timeout-seconds:30}")
  private int timeoutSeconds;

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

  public ScriptExecutionResult execute(Long scriptId, String username) {
    PythonScript script = findByIdAndUsername(scriptId, username);

    List<Transaction> transactions = transactionRepository.findRecentByUsername(username, maxTransactions);
    List<BankAccount> accounts = bankAccountRepository.findAllByUsername(username);

    return runWithTimeout(script.getCode(), transactions, accounts);
  }

  private ScriptExecutionResult runWithTimeout(String code,
      List<Transaction> transactions, List<BankAccount> accounts) {

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<ScriptExecutionResult> future = executor.submit(
          () -> runScript(code, transactions, accounts));
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      return new ScriptExecutionResult("",
          "Script execution timed out after " + timeoutSeconds + " seconds.", 1,
          (long) timeoutSeconds * 1000);
    } catch (Exception e) {
      return new ScriptExecutionResult("", e.getMessage(), 1, 0L);
    } finally {
      executor.shutdownNow();
    }
  }

  private ScriptExecutionResult runScript(String code,
      List<Transaction> transactions, List<BankAccount> accounts) {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    long start = System.currentTimeMillis();
    int exitCode = 0;

    try {
      String txJson = MAPPER.writeValueAsString(buildTransactionMaps(transactions));
      String acJson = MAPPER.writeValueAsString(buildAccountMaps(accounts));

      try (Context context = Context.newBuilder("python")
          .allowAllAccess(false)
          .allowIO(IOAccess.NONE)
          .out(out)
          .err(err)
          .build()) {

        context.getBindings("python").putMember("_tx_json", txJson);
        context.getBindings("python").putMember("_ac_json", acJson);

        String fullScript = """
            import json
            transactions = json.loads(_tx_json)
            accounts = json.loads(_ac_json)
            """ + "\n" + code;

        context.eval("python", fullScript);
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
        System.currentTimeMillis() - start
    );
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

