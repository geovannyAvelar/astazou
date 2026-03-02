package dev.avelar.astazou.service;

import dev.avelar.astazou.dto.Balance;
import dev.avelar.astazou.dto.MonthlySummaryDto;
import dev.avelar.astazou.exception.NotFoundException;
import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.repository.BankAccountRepository;
import dev.avelar.astazou.repository.TransactionRepository;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TransactionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

  @Value("${astazou.python.interpreter}")
  protected String pythonInterpreter;

  @Value("${astazou.python.itau-pdf-parser}")
  protected String itauPdfParserScript;

  private final TransactionRepository repository;

  private final BankAccountRepository bankAccountRepository;

  @Autowired
  public TransactionService(TransactionRepository repository, BankAccountRepository bankAccountRepository) {
    this.repository = repository;
    this.bankAccountRepository = bankAccountRepository;
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
    Double income = repository.calculateIncomeByUsernameAndMonth(username, month, year);
    Double expenses = repository.calculateExpenseByUsernameAndMonth(username, month, year);
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

    if (!"debit".equals(sourceTransaction.getType())) {
      throw new IllegalStateException("Only debit transactions can be transformed to transfers");
    }

    Optional<BankAccount> destinationAccountOpt = bankAccountRepository.findById(destinationAccountId);
    if (destinationAccountOpt.isEmpty() || !destinationAccountOpt.get().getUsername().equals(username)) {
      throw new IllegalStateException("Destination account not found or does not belong to user");
    }

    if (sourceTransaction.getBankAccountId().equals(destinationAccountId)) {
      throw new IllegalStateException("Source and destination accounts must be different");
    }

    int lastSequence = repository.getLastDaySequence(destinationAccountId, sourceTransaction.getTransactionDate());

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
    repository.upsert(sourceTransaction);
  }

  public List<MonthlySummaryDto> getMonthlySummary(String username, int year) {
    var rows = repository.getMonthlySummary(username, year);

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

}
