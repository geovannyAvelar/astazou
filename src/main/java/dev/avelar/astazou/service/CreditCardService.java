package dev.avelar.astazou.service;

import dev.avelar.astazou.model.CreditCard;
import dev.avelar.astazou.model.CreditCardTransaction;
import dev.avelar.astazou.repository.CreditCardRepository;
import dev.avelar.astazou.repository.CreditCardTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CreditCardService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardService.class);

  private final CreditCardRepository repository;

  private final CreditCardTransactionRepository transactionRepository;

  @Autowired
  public CreditCardService(CreditCardRepository repository, CreditCardTransactionRepository transactionRepository) {
    this.repository = repository;
    this.transactionRepository = transactionRepository;
  }

  public void save(CreditCard creditCard) {
    repository.save(creditCard);
  }

  public Page<CreditCard> findByUsername(String username, int page, int itemsPerPage) {
    return repository.findByUsername(username, PageRequest.of(page, itemsPerPage));
  }

  public CreditCard findByIdAndUsername(Long cardId, String username) {
    return repository.findByIdAndUsername(cardId, username);
  }

  public List<CreditCardTransaction> getTransactionsByStatement(Long creditCardId, String username, int month, int year) {
    return transactionRepository.getTransactions(username, creditCardId, month, year);
  }

  @Transactional
  public void parseAndSaveOfxFile(File ofxFile, Long creditCardId) {
    Objects.requireNonNull(ofxFile, "OFX file is null");
    Objects.requireNonNull(creditCardId, "Credit card ID is null");

    LOGGER.info("Starting OFX file parsing for card ID: {}", creditCardId);

    List<CreditCardTransaction> transactions = parseOfxFile(ofxFile, creditCardId);

    LOGGER.info("Parsed {} transactions from OFX file", transactions.size());

    if (transactions.isEmpty()) {
      LOGGER.warn("No transactions found in OFX file");
      return;
    }

    int savedCount = 0;
    for (CreditCardTransaction transaction : transactions) {
      if (transaction.getDescription().toUpperCase().contains("PAGAMENTO RECEBIDO")) {
        continue;
      }

      try {
        LOGGER.debug("Saving transaction: {} - {} - {} - CardID: {}",
          transaction.getId(),
          transaction.getDescription(),
          transaction.getAmount(),
          transaction.getCreditCardId());

        // Use the custom insert method for explicit control
        transactionRepository.insertTransaction(
          transaction.getId(),
          transaction.getAmount(),
          transaction.getDescription(),
          transaction.getCreditCardId(),
          transaction.getStatementDate(),
          transaction.getTransactionDate(),
          transaction.getCreatedAt()
        );

        LOGGER.debug("Successfully saved transaction with ID: {}", transaction.getId());
        savedCount++;
      } catch (Exception e) {
        LOGGER.error("Cannot process transaction: {}. CardID: {}. Error: {}",
          transaction.getDescription(),
          transaction.getCreditCardId(),
          e.getMessage(),
          e);
      }
    }

    LOGGER.info("Successfully saved {} out of {} transactions", savedCount, transactions.size());
  }

  protected List<CreditCardTransaction> parseOfxFile(File ofxFile, Long creditCardId) {
    Objects.requireNonNull(ofxFile, "OFX file is null");
    List<CreditCardTransaction> transactions = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(ofxFile))) {
      String line;
      LocalDate statementEndDate = null;

      while ((line = br.readLine()) != null) {
        line = line.trim();

        // Extract statement period
        if (line.startsWith("<DTEND>")) {
          String dateStr = extractValue(line, "<DTEND>", "</DTEND>");
          statementEndDate = dateStr != null ? parseOFXDate(dateStr) : LocalDate.now();
        }

        // Extract transactions (STMTTRN blocks)
        if (line.startsWith("<STMTTRN>")) {
          CreditCardTransaction transaction = parseOFXTransaction(br, creditCardId, statementEndDate);
          if (transaction != null) {
            transactions.add(transaction);
          }
        }
      }

      return transactions;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected CreditCardTransaction parseOFXTransaction(BufferedReader br, Long creditCardId, LocalDate statementDate) throws IOException {
    String trnType = null;
    String dtPosted = null;
    String trnAmt = null;
    String memo = null;
    String line;

    while ((line = br.readLine()) != null) {
      line = line.trim();

      if (line.startsWith("</STMTTRN>")) {
        break;
      }

      if (line.startsWith("<TRNTYPE>")) {
        trnType = extractValue(line, "<TRNTYPE>", "</TRNTYPE>");
      }
      if (line.startsWith("<DTPOSTED>")) {
        dtPosted = extractValue(line, "<DTPOSTED>", "</DTPOSTED>");
      }
      if (line.startsWith("<TRNAMT>")) {
        trnAmt = extractValue(line, "<TRNAMT>", "</TRNAMT>");
      }
      if (line.startsWith("<MEMO>")) {
        memo = extractValue(line, "<MEMO>", "</MEMO>");
      }
    }

    if (dtPosted == null || trnAmt == null) {
      LOGGER.warn("Skipping transaction - missing required fields. Type: {}, Date: {}, Amount: {}",
        trnType, dtPosted, trnAmt);
      return null;
    }

    try {
      LocalDate transactionDate = parseOFXDate(dtPosted);
      BigDecimal amount = new BigDecimal(trnAmt);

      CreditCardTransaction transaction = CreditCardTransaction.builder()
          .id(UUID.randomUUID().toString())
          .amount(amount)
          .description(memo != null ? memo : trnType != null ? trnType : "Transaction")
          .transactionDate(transactionDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime())
          .statementDate(statementDate != null ? statementDate : transactionDate)
          .creditCardId(creditCardId)
          .createdAt(OffsetDateTime.now())
          .build();

      LOGGER.debug("Created transaction object: ID={}, Amount={}, Description={}, CardID={}",
        transaction.getId(),
        transaction.getAmount(),
        transaction.getDescription(),
        transaction.getCreditCardId());

      return transaction;
    } catch (Exception e) {
      LOGGER.error("Cannot parse OFX transaction with type: {}, date: {}, amount: {}. Error: {}",
        trnType, dtPosted, trnAmt, e.getMessage(), e);
      return null;
    }
  }

  protected String extractValue(String line, String startTag, String endTag) {
    int startIndex = line.indexOf(startTag);
    int endIndex = line.indexOf(endTag);

    if (startIndex == -1 || endIndex == -1) {
      return null;
    }

    return line.substring(startIndex + startTag.length(), endIndex).trim();
  }

  protected LocalDate parseOFXDate(String dateStr) {
    // OFX date format is YYYYMMDD
    if (dateStr == null || dateStr.length() < 8) {
      return LocalDate.now();
    }

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

}
