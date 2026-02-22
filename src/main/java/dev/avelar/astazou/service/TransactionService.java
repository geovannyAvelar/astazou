package dev.avelar.astazou.service;

import dev.avelar.astazou.dto.Balance;
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
    List<Transaction> transactions =
        repository.findByAccountIdAndMonth(bankAccountId, month, year, itemsPerPage, page * itemsPerPage);
    Long count = repository.countByAccountIdAndMonth(bankAccountId, month, year);
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

    if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
      transaction.setType("debit");
    } else {
      transaction.setType("credit");
    }

    transaction.setCreatedAt(OffsetDateTime.now());

    repository.upsert(transaction);

    if (updateAccount != null && updateAccount) {
      repository.updateBankAccountBalance(transaction.getAmount().doubleValue(),  transaction.getBankAccountId());
    }
  }

  public void save(File pdf, String username, Long bankAccountId, boolean updateAccount) {
    save(parseItauPdf(pdf), username, bankAccountId, updateAccount);
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

    // Create credit transaction on destination account
    int lastSequence = repository.getLastDaySequence(destinationAccountId, sourceTransaction.getTransactionDate());

    Transaction creditTransaction = Transaction.builder()
        .transactionDate(sourceTransaction.getTransactionDate())
        .description("Transfer from " + sourceAccountOpt.get().getName() + ": " + sourceTransaction.getDescription())
        .amount(sourceTransaction.getAmount().abs())
        .type("credit")
        .page(0)
        .sequence(lastSequence + 1)
        .createdAt(OffsetDateTime.now())
        .bankAccountId(destinationAccountId)
        .build();

    repository.upsert(creditTransaction);

    // Update source transaction type to transfer
    sourceTransaction.setType("transfer");
    repository.upsert(sourceTransaction);
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
          repository.updateBankAccountBalance(transaction.getAmount().doubleValue(),  transaction.getBankAccountId());
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
        throw new RuntimeException("Python parser exited with code " + exit);
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

        if (description.toUpperCase().contains("SALDO DO DIA")) {
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
