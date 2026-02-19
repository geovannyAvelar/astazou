package dev.avelar.astazou.service;

import dev.avelar.astazou.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @InjectMocks
  private TransactionService service;

  @Test
  void parseItauPdf() throws IOException {
    service.pythonInterpreter = "scripts/python/.venv/bin/python3";
    service.itauPdfParserScript = "scripts/python/parse_itau_history_pdf.py";

    ClassLoader cl = getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream("itau.pdf")) {
      assertNotNull(is, "resource itau.pdf should exist in test resources");
      Path temp = Files.createTempFile("itau-", ".pdf");
      Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);

      List<Transaction> transactions = service.parseItauPdf(temp.toFile());

      assertFalse(transactions.isEmpty());
      assertEquals(1, transactions.getFirst().getSequence());
    }
  }
}