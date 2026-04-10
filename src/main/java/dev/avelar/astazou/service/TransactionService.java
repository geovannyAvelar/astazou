package dev.avelar.astazou.service;

import dev.avelar.astazou.dto.Balance;
import dev.avelar.astazou.dto.MonthlySummaryDto;
import dev.avelar.astazou.exception.NotFoundException;
import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.model.ReportToken;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.model.User;
import dev.avelar.astazou.repository.BankAccountRepository;
import dev.avelar.astazou.repository.ReportTokenRepository;
import dev.avelar.astazou.repository.TransactionRepository;
import dev.avelar.astazou.repository.UserRepository;
import dev.avelar.jambock.reports.ReportBuilder;
import dev.avelar.jambock.reports.ReportEngine;
import dev.avelar.jambock.reports.ReportGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Base64;

@Service
public class TransactionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

  @Value("${astazou.python.interpreter}")
  protected String pythonInterpreter;

  @Value("${astazou.python.itau-pdf-parser}")
  protected String itauPdfParserScript;

  @Value("${astazou.base-url}")
  private String baseUrl;

  private final TransactionRepository repository;

  private final BankAccountRepository bankAccountRepository;

  private final UserRepository userRepository;

  private final ReportEngine reportEngine;

  private final ReportTokenRepository reportTokenRepository;

  private final QrCodeService qrCodeService;

  @Autowired
  public TransactionService(TransactionRepository repository, BankAccountRepository bankAccountRepository,
      UserRepository userRepository, ReportEngine reportEngine, ReportTokenRepository reportTokenRepository,
      QrCodeService qrCodeService) {
    this.repository = repository;
    this.bankAccountRepository = bankAccountRepository;
    this.userRepository = userRepository;
    this.reportEngine = reportEngine;
    this.reportTokenRepository = reportTokenRepository;
    this.qrCodeService = qrCodeService;
  }

  public Page<Transaction> findByAccountIdAndMonth(Long bankAccountId, Integer month, Integer year, int page,
      int itemsPerPage) {
    if (itemsPerPage <= 0) {
      itemsPerPage = 10;
    }

    var offset = page * itemsPerPage;

    if (offset < 0) {
      offset = 0;
    }

    List<Transaction> transactions =
        repository.findByAccountIdAndMonth(bankAccountId, month, year, itemsPerPage, offset);
    Long count = repository.countByAccountIdAndMonth(bankAccountId, month, year);
    return new PageImpl<>(transactions, PageRequest.of(page, itemsPerPage), count);
  }

  public Page<Transaction> search(String username, Long bankAccountId, String query, LocalDate startDate, LocalDate endDate, int page, int itemsPerPage) {
    if (itemsPerPage <= 0) {
      itemsPerPage = 10;
    }

    var offset = page * itemsPerPage;

    if (offset < 0) {
      offset = 0;
    }

    if (query == null || query.trim().isEmpty()) {
      query = "";
    }

    List<Transaction> transactions =
        repository.searchTransactions(username, bankAccountId, query, startDate, endDate, itemsPerPage, offset);
    Long count = repository.countSearchTransactions(username, bankAccountId, query, startDate, endDate);
    return new PageImpl<>(transactions, PageRequest.of(page, itemsPerPage), count);
  }

  public Balance calculateMonthBalance(String username, Integer month, Integer year) {
    String currency = resolveUserCurrency(username);
    Double income = repository.calculateIncomeByUsernameAndMonth(username, month, year, currency);
    Double expenses = repository.calculateExpenseByUsernameAndMonth(username, month, year, currency);
    Double balance = income - expenses;
    return new Balance(income, expenses, balance);
  }

  public Page<Transaction> findLast10(String username) {
    return new PageImpl<>(repository.findLast10(username));
  }

  @Transactional
  public void save(Transaction transaction, String username, Boolean updateAccount) {
    Optional<BankAccount> opt = bankAccountRepository.findById(transaction.getBankAccountId());

    if (opt.isEmpty()) {
      throw new NotFoundException("Cannot find an account with this ID");
    }

    BankAccount bankAccount = opt.get();
    if (!bankAccount.getUsername().contains(username)) {
      throw new IllegalStateException("Cannot create a transaction without account ownership");
    }

    int lastSequence = repository.getLastDaySequence(bankAccount.getId(), transaction.getTransactionDate());
    transaction.setSequence(lastSequence + 1);
    transaction.setPage(0);
    transaction.setCreatedAt(OffsetDateTime.now());

    if ("debit".equalsIgnoreCase(transaction.getType())) {
      BigDecimal amt = transaction.getAmount();
      if (amt != null && amt.compareTo(BigDecimal.ZERO) >= 0) {
        transaction.setAmount(amt.negate());
      }
    }

    repository.upsert(transaction);

    if (updateAccount != null && updateAccount) {
      repository.updateBankAccountBalance(transaction.getAmount().doubleValue(), transaction.getBankAccountId());
    }
  }

  public void save(File pdf, String username, Long bankAccountId, boolean updateAccount) {
    save(parseItauPdf(pdf), username, bankAccountId, updateAccount);
  }

  @Transactional
  public void saveOfx(File ofxFile, String username, Long bankAccountId, boolean updateAccount) {
    Objects.requireNonNull(ofxFile, "OFX file is null");
    LOGGER.info("Starting OFX file parsing for bank account ID: {}", bankAccountId);
    List<Transaction> transactions = parseOfxFile(ofxFile);
    LOGGER.info("Parsed {} transactions from OFX file", transactions.size());
    save(transactions, username, bankAccountId, updateAccount);
  }

  protected List<Transaction> parseOfxFile(File ofxFile) {
    Objects.requireNonNull(ofxFile, "OFX file is null");
    List<Transaction> transactions = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(ofxFile))) {
      String line;
      LocalDate currentDate = null;
      int positionOnDay = 0;

      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("<STMTTRN>")) {
          Transaction tx = parseOfxTransaction(br);
          if (tx != null) {
            if (!tx.getTransactionDate().equals(currentDate)) {
              currentDate = tx.getTransactionDate();
              positionOnDay = 1;
            } else {
              positionOnDay++;
            }
            tx.setSequence(positionOnDay);
            tx.setPage(0);
            tx.setCreatedAt(OffsetDateTime.now());
            transactions.add(tx);
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return transactions;
  }

  private Transaction parseOfxTransaction(BufferedReader br) throws IOException {
    String trnType = null;
    String dtPosted = null;
    String trnAmt = null;
    String memo = null;
    String line;

    while ((line = br.readLine()) != null) {
      line = line.trim();
      if (line.startsWith("</STMTTRN>")) break;
      if (line.startsWith("<TRNTYPE>")) trnType = extractOfxValue(line, "<TRNTYPE>", "</TRNTYPE>");
      else if (line.startsWith("<DTPOSTED>")) dtPosted = extractOfxValue(line, "<DTPOSTED>", "</DTPOSTED>");
      else if (line.startsWith("<TRNAMT>")) trnAmt = extractOfxValue(line, "<TRNAMT>", "</TRNAMT>");
      else if (line.startsWith("<MEMO>")) memo = extractOfxValue(line, "<MEMO>", "</MEMO>");
      else if (line.startsWith("<NAME>") && memo == null) memo = extractOfxValue(line, "<NAME>", "</NAME>");
    }

    if (dtPosted == null || trnAmt == null) {
      LOGGER.warn("Skipping OFX transaction - missing required fields. Type: {}, Date: {}, Amount: {}",
          trnType, dtPosted, trnAmt);
      return null;
    }

    try {
      LocalDate transactionDate = parseOfxDate(dtPosted);
      BigDecimal amount = new BigDecimal(trnAmt);
      String type;
      if (amount.compareTo(BigDecimal.ZERO) >= 0) {
        type = "credit";
      } else {
        type = "debit";
      }

      return Transaction.builder()
          .transactionDate(transactionDate)
          .description(memo != null ? memo : (trnType != null ? trnType : "Transaction"))
          .amount(amount)
          .type(type)
          .build();
    } catch (Exception e) {
      LOGGER.error("Cannot parse OFX transaction. Date: {}, Amount: {}. Error: {}", dtPosted, trnAmt, e.getMessage());
      return null;
    }
  }

  private String extractOfxValue(String line, String startTag, String endTag) {
    int startIndex = line.indexOf(startTag);
    int endIndex = line.indexOf(endTag);
    if (startIndex == -1) {
      // self-closing / no closing tag: value starts after startTag
      startIndex = line.indexOf(startTag);
      if (startIndex == -1) return null;
      String value = line.substring(startIndex + startTag.length()).trim();
      return value.isEmpty() ? null : value;
    }
    if (endIndex == -1) {
      String value = line.substring(startIndex + startTag.length()).trim();
      return value.isEmpty() ? null : value;
    }
    return line.substring(startIndex + startTag.length(), endIndex).trim();
  }

  private LocalDate parseOfxDate(String dateStr) {
    if (dateStr == null || dateStr.length() < 8) return LocalDate.now();
    try {
      int year = Integer.parseInt(dateStr.substring(0, 4));
      int month = Integer.parseInt(dateStr.substring(4, 6));
      int day = Integer.parseInt(dateStr.substring(6, 8));
      return LocalDate.of(year, month, day);
    } catch (Exception e) {
      LOGGER.warn("Cannot parse OFX date: {}. Cause: {}", dateStr, e.getMessage());
      return LocalDate.now();
    }
  }

  public void delete(Long transactionId, String username) {
    repository.delete(transactionId, username);
  }

  @Transactional
  public void update(Long transactionId, String username, java.time.LocalDate transactionDate,
      String description, java.math.BigDecimal amount, String type) {
    Optional<Transaction> opt = repository.findById(transactionId);
    if (opt.isEmpty()) {
      throw new NotFoundException("Transaction not found");
    }
    Transaction existing = opt.get();

    Optional<BankAccount> accountOpt = bankAccountRepository.findById(existing.getBankAccountId());
    if (accountOpt.isEmpty() || !accountOpt.get().getUsername().equals(username)) {
      throw new IllegalStateException("Transaction does not belong to user");
    }

    BigDecimal finalAmount = amount;
    if ("debit".equalsIgnoreCase(type)) {
      if (finalAmount != null && finalAmount.compareTo(BigDecimal.ZERO) > 0) {
        finalAmount = finalAmount.negate();
      }
    }

    repository.update(transactionId, username, transactionDate, description, finalAmount, type);
  }

  @Transactional
  public void transformToTransfer(Long transactionId, Long destinationAccountId, String username) {
    Optional<Transaction> transactionOpt = repository.findById(transactionId);
    if (transactionOpt.isEmpty()) {
      throw new NotFoundException("Transaction not found");
    }

    Transaction sourceTransaction = transactionOpt.get();

    Optional<BankAccount> sourceAccountOpt = bankAccountRepository.findById(sourceTransaction.getBankAccountId());
    if (sourceAccountOpt.isEmpty() || !sourceAccountOpt.get().getUsername().equals(username)) {
      throw new IllegalStateException("Transaction does not belong to user");
    }

    boolean isDebit = "debit".equals(sourceTransaction.getType());
    boolean isCredit = "credit".equals(sourceTransaction.getType());

    if (!isDebit && !isCredit) {
      throw new IllegalStateException("Only debit or credit transactions can be transformed to transfers");
    }

    Optional<BankAccount> destinationAccountOpt = bankAccountRepository.findById(destinationAccountId);
    if (destinationAccountOpt.isEmpty() || !destinationAccountOpt.get().getUsername().equals(username)) {
      throw new IllegalStateException("Destination account not found or does not belong to user");
    }

    if (sourceTransaction.getBankAccountId().equals(destinationAccountId)) {
      throw new IllegalStateException("Source and destination accounts must be different");
    }

    int lastSequence = repository.getLastDaySequence(destinationAccountId, sourceTransaction.getTransactionDate());

    if (isDebit) {
      // @formatter:off
      Transaction creditTransaction = Transaction.builder()
          .transactionDate(sourceTransaction.getTransactionDate())
          .description("Transfer from " + sourceAccountOpt.get().getName() + ": " + sourceTransaction.getDescription())
          .amount(sourceTransaction.getAmount().abs())
          .type("transfer_credit")
          .page(0)
          .sequence(lastSequence + 1)
          .createdAt(OffsetDateTime.now())
          .bankAccountId(destinationAccountId).build();
      // @formatter:on

      repository.upsert(creditTransaction);

      sourceTransaction.setType("transfer");
    } else {
      // isCredit: generate a debit on the destination account
      // @formatter:off
      Transaction debitTransaction = Transaction.builder()
          .transactionDate(sourceTransaction.getTransactionDate())
          .description("Transfer to " + sourceAccountOpt.get().getName() + ": " + sourceTransaction.getDescription())
          .amount(sourceTransaction.getAmount().abs().negate())
          .type("transfer_debit")
          .page(0)
          .sequence(lastSequence + 1)
          .createdAt(OffsetDateTime.now())
          .bankAccountId(destinationAccountId).build();
      // @formatter:on

      repository.upsert(debitTransaction);

      sourceTransaction.setType("transfer_credit");
    }

    repository.upsert(sourceTransaction);
  }

  public byte[] generateMonthlyReport(String username, Long bankAccountId, Integer month, Integer year)
      throws ReportGenerationException {
    return generateMonthlyReport(username, bankAccountId, month, year, "en");
  }

  public byte[] generateMonthlyReport(String username, Long bankAccountId, Integer month, Integer year, String lang)
      throws ReportGenerationException {
    List<Transaction> transactions =
        repository.findByAccountIdAndMonth(bankAccountId, month, year, Integer.MAX_VALUE, 0);

    Balance balance = calculateMonthBalance(username, month, year);

    Optional<BankAccount> accountOpt = bankAccountRepository.findById(bankAccountId);
    String accountName = accountOpt.map(BankAccount::getName).orElse("—");

    Locale locale = resolveLocale(lang);
    String monthName = Month.of(month).getDisplayName(TextStyle.FULL, locale);
    String datePattern = "pt".equalsIgnoreCase(lang) || "es".equalsIgnoreCase(lang)
        ? "dd/MM/yyyy HH:mm"
        : "MM/dd/yyyy HH:mm";
    String generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ofPattern(datePattern));

    String logoDataUri = "";
    try (InputStream logoStream = getClass().getResourceAsStream("/templates/logo.png")) {
      if (logoStream != null) {
        byte[] logoBytes = logoStream.readAllBytes();
        logoDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoBytes);
      }
    } catch (IOException e) {
      LOGGER.warn("Could not load logo for report: {}", e.getMessage());
    }

    // Generate a unique token for report validation and persist it
    String token = UUID.randomUUID().toString();
    ReportToken reportToken = ReportToken.builder()
        .token(token)
        .username(username)
        .bankAccountId(bankAccountId)
        .accountName(accountName)
        .reportMonth(month)
        .reportYear(year)
        .createdAt(OffsetDateTime.now())
        .build();
    reportTokenRepository.save(reportToken);

    // Build the validation URL and generate QR code
    String validationUrl = baseUrl + "/validate-report/" + token;
    String qrCodeDataUri = "";
    try {
      qrCodeDataUri = qrCodeService.generateQrCodeDataUri(validationUrl, 120);
    } catch (Exception e) {
      LOGGER.warn("Could not generate QR code for report: {}", e.getMessage());
    }

    dev.avelar.astazou.dto.ReportLabels labels = dev.avelar.astazou.dto.ReportLabels.forLocale(lang);

    Map<String, Object> data = new HashMap<>();
    data.put("transactions", transactions);
    data.put("month", month);
    data.put("monthName", monthName);
    data.put("year", year);
    data.put("accountName", accountName);
    data.put("income", balance.getIncome() != null ? balance.getIncome() : 0.0);
    data.put("expenses", balance.getExpenses() != null ? balance.getExpenses() : 0.0);
    data.put("balance", balance.getAmount() != null ? balance.getAmount() : 0.0);
    data.put("generatedAt", generatedAt);
    data.put("logoDataUri", logoDataUri);
    data.put("labels", labels);
    data.put("qrCodeDataUri", qrCodeDataUri);
    data.put("validationUrl", validationUrl);

    return new ReportBuilder(reportEngine)
        .withTemplate("monthly-transactions-report.ftl")
        .withData(data)
        .portrait()
        .generateAsBytes();
  }

  private static Locale resolveLocale(String lang) {
    if (lang == null) return Locale.ENGLISH;
    return switch (lang.toLowerCase()) {
      case "pt" -> Locale.of("pt", "BR");
      case "es" -> Locale.of("es");
      default   -> Locale.ENGLISH;
    };
  }

  private String resolveUserCurrency(String username) {
    return userRepository.findById(username)
        .map(User::getPreferredCurrency)
        .filter(c -> c != null && !c.isBlank())
        .orElse("BRL");
  }

  public List<MonthlySummaryDto> getMonthlySummary(String username, int year) {
    String currency = resolveUserCurrency(username);
    var rows = repository.getMonthlySummary(username, year, currency);

    var map = new java.util.HashMap<Integer, MonthlySummaryDto>();
    for (var row : rows) {
      map.put(row.month(), row);
    }

    var result = new java.util.ArrayList<MonthlySummaryDto>(12);
    for (int month = 1; month <= 12; month++) {
      var row = map.get(month);
      if (row == null) {
        result.add(new MonthlySummaryDto(month, 0.0, 0.0));
      } else {
        result.add(row);
      }
    }

    return result;
  }

  protected void save(List<Transaction> transactions, String username, Long bankAccountId, boolean updateAccount) {
    for (Transaction transaction : transactions) {
      try {
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findById(bankAccountId);

        if (bankAccountOpt.isEmpty()) {
          throw new IllegalAccessException("Bank account not found");
        }

        BankAccount bankAccount = bankAccountOpt.get();

        if (!bankAccount.getUsername().equals(username)) {
          throw new IllegalAccessException("You are not the owner of this bank account");
        }

        transaction.setBankAccountId(bankAccount.getId());

        repository.upsert(transaction);

        if (updateAccount) {
          repository.updateBankAccountBalance(transaction.getAmount().doubleValue(), transaction.getBankAccountId());
        }
      } catch (Exception e) {
        LOGGER.warn("Cannot process transaction. Cause: {}", e.getMessage());
      }
    }
  }

  protected List<Transaction> parseItauPdf(File pdf) {
    Objects.requireNonNull(pdf, "pdf file is null");

    File outputCsv;
    File parent = pdf.getParentFile();
    String csvName = pdf.getName().replaceAll("(?i)\\.pdf$", ".csv");
    if (parent != null) {
      outputCsv = new File(parent, csvName);
    } else {
      outputCsv = new File(csvName);
    }

    ProcessBuilder pb =
        new ProcessBuilder(pythonInterpreter, itauPdfParserScript, pdf.getAbsolutePath(), outputCsv.getAbsolutePath());
    pb.redirectErrorStream(true);

    try {
      Process p = pb.start();
      List<String> logRows = new LinkedList<>();

      try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String row = "";
        while ((row = br.readLine()) != null) {
          logRows.add(row);
        }
      }

      int exit = p.waitFor();
      if (exit != 0) {
        LOGGER.error(String.join(System.lineSeparator(), logRows));
        throw new RuntimeException("Parser exited with code " + exit);
      }

      return parseItauCsv(outputCsv);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  protected List<Transaction> parseItauCsv(File csv) {
    Objects.requireNonNull(csv, "csv file is null");
    List<Transaction> transactions = new ArrayList<>();
    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

    try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
      String header = br.readLine();
      if (header == null) {
        return transactions;
      }

      LocalDate currentDate = null;
      int positionOnDay = 0;

      String line;
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty())
          continue;

        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        for (int i = 0; i < parts.length; i++) {
          parts[i] = parts[i].trim().replaceAll("^\"|\"$", "");
        }

        if (parts.length < 5)
          continue;

        LocalDate date = LocalDate.parse(parts[0], fmt);
        String description = parts[1];

        if (description.toUpperCase().contains("SALDO DO DIA") ||
            description.toUpperCase().contains("SALDO TOTAL DISPONÃVEL DIA") ||
            description.toUpperCase().contains("SALDO ANTERIOR")) {
          continue;
        }

        BigDecimal amount = new BigDecimal(parts[2]);
        String type = parts[3];
        int page = Integer.parseInt(parts[4]);

        if (!date.equals(currentDate)) {
          currentDate = date;
          positionOnDay = 1;
        } else {
          positionOnDay++;
        }

        transactions.add(
            Transaction.builder().transactionDate(date).description(description).amount(amount).type(type).page(page)
                .sequence(positionOnDay).createdAt(OffsetDateTime.now()).build());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return transactions;
  }

  @Transactional
  public void updateTags(Long transactionId, String[] tags, String username) {
    if (!repository.existsById(transactionId)) {
      throw new NotFoundException("Transaction not found");
    }
    // Normalise: trim, lowercase, deduplicate, remove empty entries
    String[] normalised = Arrays.stream(tags == null ? new String[0] : tags)
        .filter(t -> t != null && !t.isBlank())
        .map(t -> t.trim().toLowerCase())
        .distinct()
        .toArray(String[]::new);

    // Build a Postgres array literal: {"tag1","tag2"}
    String pgArray = "{" + String.join(",",
        Arrays.stream(normalised)
              .map(t -> "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
              .toList()) + "}";

    repository.updateTags(transactionId, username, pgArray);
  }

  public List<String> findAllTags(String username) {
    return repository.findAllTagsByUsername(username);
  }

}
