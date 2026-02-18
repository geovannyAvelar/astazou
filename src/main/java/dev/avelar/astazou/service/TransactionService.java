package dev.avelar.astazou.service;

import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

  @Value("${astazou.python.interpreter}")
  private String pythonInterpreter;

  @Value("${astazou.python.itau-pdf-parser}")
  private String itauPdfParserScript;

  private final TransactionRepository repository;

  @Autowired
  public TransactionService(TransactionRepository repository) {
    this.repository = repository;
  }

  public void save(File pdf, String username) {
    save(parseItauPdf(pdf), username);
  }

  protected void save(List<Transaction> transactions, String username) {
    for (Transaction transaction : transactions) {
      try {
        transaction.setUsername(username);
        save(transaction);
      } catch (Exception e) {
        LOGGER.warn("Cannot process transaction. Cause: {}", e.getMessage());
      }
    }
  }

  protected Transaction save(Transaction transaction) {
    return repository.save(transaction);
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

    ProcessBuilder pb = new ProcessBuilder(pythonInterpreter, itauPdfParserScript, pdf.getAbsolutePath(), outputCsv.getAbsolutePath());
    pb.redirectErrorStream(true);

    try {
      Process p = pb.start();
      // consume process output to avoid blocking
      try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        while (br.readLine() != null) { /* consume */ }
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

      while (header != null && header.trim().toUpperCase().startsWith("SALDO DO DIA")) {
        header = br.readLine();
      }

      String line;
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty()) continue;

        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        for (int i = 0; i < parts.length; i++) {
          parts[i] = parts[i].trim().replaceAll("^\"|\"$", "");
        }

        if (parts.length < 5) continue;

        LocalDate date = LocalDate.parse(parts[0], fmt);
        String description = parts[1];
        BigDecimal amount = new BigDecimal(parts[2]);
        String type = parts[3];
        int page = Integer.parseInt(parts[4]);

        transactions.add(Transaction.builder()
            .transactionDate(date)
            .description(description)
            .amount(amount)
            .type(type)
            .page(page)
            .createdAt(OffsetDateTime.now())
            .build());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return transactions;
  }

}
