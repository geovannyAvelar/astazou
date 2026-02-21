package dev.avelar.astazou.service;

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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

  public Page<Transaction> findByAccount(Long accountId, int page, int itemsPerPage) {
    return repository.findByBankAccountId(accountId,
        PageRequest.of(page, itemsPerPage, Sort.Direction.DESC, "transaction_date"));
  }

  public Page<Transaction> findLast10(String username) {
    return new PageImpl<>(repository.findLast10(username));
  }

  public void save(File pdf, String username, Long bankAccountId) {
    save(parseItauPdf(pdf), username, bankAccountId);
  }

  protected void save(List<Transaction> transactions, String username, Long bankAccountId) {
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
