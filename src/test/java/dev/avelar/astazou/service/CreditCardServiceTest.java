package dev.avelar.astazou.service;

import dev.avelar.astazou.model.CreditCard;
import dev.avelar.astazou.model.CreditCardTransaction;
import dev.avelar.astazou.repository.CreditCardRepository;
import dev.avelar.astazou.repository.CreditCardTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

  @Mock
  private CreditCardRepository repository;

  @Mock
  private CreditCardTransactionRepository transactionRepository;

  @InjectMocks
  private CreditCardService service;

  @Test
  void save_delegatesToRepository() {
    CreditCard card = CreditCard.builder().id(1L).name("Visa").username("user").build();

    service.save(card);

    verify(repository).save(card);
  }

  @Test
  void findByUsername_returnsPage() {
    CreditCard card = CreditCard.builder().id(1L).name("Visa").username("user").build();
    Page<CreditCard> expected = new PageImpl<>(List.of(card));
    when(repository.findByUsername("user", PageRequest.of(0, 10))).thenReturn(expected);

    Page<CreditCard> result = service.findByUsername("user", 0, 10);

    assertEquals(1, result.getTotalElements());
    assertEquals(card, result.getContent().getFirst());
  }

  @Test
  void findByUsername_passesCorrectPageRequest() {
    when(repository.findByUsername(eq("user"), eq(PageRequest.of(2, 5))))
        .thenReturn(Page.empty());

    service.findByUsername("user", 2, 5);

    verify(repository).findByUsername("user", PageRequest.of(2, 5));
  }

  @Test
  void findByIdAndUsername_delegatesToRepository() {
    CreditCard card = CreditCard.builder().id(1L).name("Visa").username("user").build();
    when(repository.findByIdAndUsername(1L, "user")).thenReturn(card);

    CreditCard result = service.findByIdAndUsername(1L, "user");

    assertEquals(card, result);
  }

  @Test
  void findByIdAndUsername_returnsNullWhenNotFound() {
    when(repository.findByIdAndUsername(99L, "user")).thenReturn(null);

    assertNull(service.findByIdAndUsername(99L, "user"));
  }

  @Test
  void getTransactionsByStatement_delegatesToRepository() {
    CreditCardTransaction tx = CreditCardTransaction.builder().id("abc").build();
    when(transactionRepository.getTransactions("user", 1L, 3, 2025)).thenReturn(List.of(tx));

    List<CreditCardTransaction> result = service.getTransactionsByStatement(1L, "user", 3, 2025);

    assertEquals(1, result.size());
    assertEquals("abc", result.getFirst().getId());
  }

  @Test
  void getTransactionsByStatement_returnsEmptyListWhenNoTransactions() {
    when(transactionRepository.getTransactions("user", 1L, 3, 2025)).thenReturn(List.of());

    assertTrue(service.getTransactionsByStatement(1L, "user", 3, 2025).isEmpty());
  }

  @Test
  void parseAndSaveOfxFile_throwsNPE_whenFileIsNull() {
    assertThrows(NullPointerException.class,
        () -> service.parseAndSaveOfxFile(null, 1L));
  }

  @Test
  void parseAndSaveOfxFile_throwsNPE_whenCreditCardIdIsNull(@TempDir Path tmp) throws Exception {
    File f = tmp.resolve("dummy.ofx").toFile();
    Files.writeString(f.toPath(), "");

    assertThrows(NullPointerException.class,
        () -> service.parseAndSaveOfxFile(f, null));
  }

  @Test
  void parseAndSaveOfxFile_doesNotSave_whenNoTransactionsParsed(@TempDir Path tmp) throws Exception {
    File f = tmp.resolve("empty.ofx").toFile();
    Files.writeString(f.toPath(), "<OFX></OFX>");

    service.parseAndSaveOfxFile(f, 1L);

    verify(transactionRepository, never()).insertTransaction(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void parseAndSaveOfxFile_savesAllParsedTransactions(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<DTEND>20250331</DTEND>\n"
        + "<STMTTRN>\n"
        + "<TRNTYPE>DEBIT</TRNTYPE>\n"
        + "<DTPOSTED>20250310</DTPOSTED>\n"
        + "<TRNAMT>-45.00</TRNAMT>\n"
        + "<MEMO>Grocery</MEMO>\n"
        + "</STMTTRN>\n"
        + "<STMTTRN>\n"
        + "<TRNTYPE>CREDIT</TRNTYPE>\n"
        + "<DTPOSTED>20250315</DTPOSTED>\n"
        + "<TRNAMT>200.00</TRNAMT>\n"
        + "<MEMO>Refund</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("transactions.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    service.parseAndSaveOfxFile(f, 1L);

    verify(transactionRepository, times(2))
        .insertTransaction(any(), any(), any(), eq(1L), any(), any(), any());
  }

  @Test
  void parseAndSaveOfxFile_continuesOnSaveError(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250310</DTPOSTED>\n"
        + "<TRNAMT>-10.00</TRNAMT>\n"
        + "<MEMO>First</MEMO>\n"
        + "</STMTTRN>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250311</DTPOSTED>\n"
        + "<TRNAMT>-20.00</TRNAMT>\n"
        + "<MEMO>Second</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("error.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    // first call throws, second succeeds
    doThrow(new RuntimeException("DB error"))
        .doNothing()
        .when(transactionRepository)
        .insertTransaction(any(), any(), any(), any(), any(), any(), any());

    // should not propagate the exception
    assertDoesNotThrow(() -> service.parseAndSaveOfxFile(f, 1L));
    verify(transactionRepository, times(2))
        .insertTransaction(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void parseOfxFile_throwsNPE_whenFileIsNull() {
    assertThrows(NullPointerException.class,
        () -> service.parseOfxFile(null, 1L));
  }

  @Test
  void parseOfxFile_parsesTransactionFields(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<TRNTYPE>DEBIT</TRNTYPE>\n"
        + "<DTPOSTED>20250210</DTPOSTED>\n"
        + "<TRNAMT>-55.50</TRNAMT>\n"
        + "<MEMO>Coffee</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("parse.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    List<CreditCardTransaction> result = service.parseOfxFile(f, 7L);

    assertEquals(1, result.size());
    CreditCardTransaction tx = result.getFirst();
    assertEquals(new BigDecimal("-55.50"), tx.getAmount());
    assertEquals("Coffee", tx.getDescription());
    assertEquals(7L, tx.getCreditCardId());
    assertEquals(LocalDate.of(2025, 2, 10), tx.getTransactionDate().toLocalDate());
  }

  @Test
  void parseOfxFile_usesDtEndAsStatementDate(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<DTEND>20250331</DTEND>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250310</DTPOSTED>\n"
        + "<TRNAMT>-10.00</TRNAMT>\n"
        + "<MEMO>Test</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("dtend.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    List<CreditCardTransaction> result = service.parseOfxFile(f, 1L);

    assertEquals(LocalDate.of(2025, 3, 31), result.getFirst().getStatementDate());
  }

  @Test
  void parseOfxFile_fallsBackToTransactionDateForStatement_whenNoDtEnd(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250210</DTPOSTED>\n"
        + "<TRNAMT>-10.00</TRNAMT>\n"
        + "<MEMO>Test</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("nodtend.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    List<CreditCardTransaction> result = service.parseOfxFile(f, 1L);

    assertEquals(LocalDate.of(2025, 2, 10), result.getFirst().getStatementDate());
  }

  @Test
  void parseOfxFile_skipsTransaction_whenDtPostedMissing(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<TRNAMT>-10.00</TRNAMT>\n"
        + "<MEMO>No date</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("nodate.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    assertTrue(service.parseOfxFile(f, 1L).isEmpty());
  }

  @Test
  void parseOfxFile_skipsTransaction_whenTrnAmtMissing(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250210</DTPOSTED>\n"
        + "<MEMO>No amount</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("noamt.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    assertTrue(service.parseOfxFile(f, 1L).isEmpty());
  }

  @Test
  void parseOfxFile_fallsBackToTrnType_whenMemoMissing(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<TRNTYPE>CREDIT</TRNTYPE>\n"
        + "<DTPOSTED>20250210</DTPOSTED>\n"
        + "<TRNAMT>100.00</TRNAMT>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("nomemo.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    List<CreditCardTransaction> result = service.parseOfxFile(f, 1L);

    assertEquals("CREDIT", result.getFirst().getDescription());
  }

  @Test
  void parseOfxFile_fallsBackToTransaction_whenBothMemoAndTypeMissing(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250210</DTPOSTED>\n"
        + "<TRNAMT>100.00</TRNAMT>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("nomenootype.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    List<CreditCardTransaction> result = service.parseOfxFile(f, 1L);

    assertEquals("Transaction", result.getFirst().getDescription());
  }

  @Test
  void parseOfxFile_parsesMultipleTransactions(@TempDir Path tmp) throws Exception {
    String ofx = "<OFX>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250201</DTPOSTED>\n"
        + "<TRNAMT>-10.00</TRNAMT>\n"
        + "<MEMO>A</MEMO>\n"
        + "</STMTTRN>\n"
        + "<STMTTRN>\n"
        + "<DTPOSTED>20250202</DTPOSTED>\n"
        + "<TRNAMT>-20.00</TRNAMT>\n"
        + "<MEMO>B</MEMO>\n"
        + "</STMTTRN>\n"
        + "</OFX>\n";
    File f = tmp.resolve("multi.ofx").toFile();
    Files.writeString(f.toPath(), ofx);

    assertEquals(2, service.parseOfxFile(f, 1L).size());
  }

  @Test
  void parseOfxFile_returnsEmptyList_whenNoTransactions(@TempDir Path tmp) throws Exception {
    File f = tmp.resolve("empty.ofx").toFile();
    Files.writeString(f.toPath(), "<OFX></OFX>");

    assertTrue(service.parseOfxFile(f, 1L).isEmpty());
  }

  @Test
  void extractValue_returnsValueBetweenTags() {
    assertEquals("20250310", service.extractValue("<DTPOSTED>20250310</DTPOSTED>", "<DTPOSTED>", "</DTPOSTED>"));
  }

  @Test
  void extractValue_trimsWhitespace() {
    assertEquals("hello", service.extractValue("<MEMO>  hello  </MEMO>", "<MEMO>", "</MEMO>"));
  }

  @Test
  void extractValue_returnsNull_whenStartTagMissing() {
    assertNull(service.extractValue("<MEMO>value</MEMO>", "<TRNTYPE>", "</TRNTYPE>"));
  }

  @Test
  void extractValue_returnsNull_whenEndTagMissing() {
    assertNull(service.extractValue("<DTPOSTED>20250310", "<DTPOSTED>", "</DTPOSTED>"));
  }

  @Test
  void extractValue_returnsEmptyString_whenTagsAreAdjacent() {
    assertEquals("", service.extractValue("<MEMO></MEMO>", "<MEMO>", "</MEMO>"));
  }

  @Test
  void parseOFXDate_parsesValidDate() {
    assertEquals(LocalDate.of(2025, 3, 15), service.parseOFXDate("20250315"));
  }

  @Test
  void parseOFXDate_returnsToday_whenInputIsNull() {
    assertEquals(LocalDate.now(), service.parseOFXDate(null));
  }

  @Test
  void parseOFXDate_returnsToday_whenInputIsTooShort() {
    assertEquals(LocalDate.now(), service.parseOFXDate("202503"));
  }

  @Test
  void parseOFXDate_returnsToday_whenInputIsInvalidFormat() {
    assertEquals(LocalDate.now(), service.parseOFXDate("ABCDEFGH"));
  }

  @Test
  void parseOFXDate_ignoresExtraCharsAfterEightDigits() {
    // OFX dates can have timezone offsets like 20250315[-3:BRT], should still parse
    assertEquals(LocalDate.of(2025, 3, 15), service.parseOFXDate("20250315[-3:BRT]"));
  }

  @Test
  void deleteTransaction_throwsNPE_whenTransactionIdIsNull() {
    assertThrows(NullPointerException.class,
        () -> service.deleteTransaction(null, "user"));
  }

  @Test
  void deleteTransaction_throwsNPE_whenUsernameIsNull() {
    assertThrows(NullPointerException.class,
        () -> service.deleteTransaction("tx-1", null));
  }

  @Test
  void deleteTransaction_throwsIllegalArgument_whenTransactionNotFound() {
    when(transactionRepository.findById("tx-1")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
        () -> service.deleteTransaction("tx-1", "user"));
  }

  @Test
  void deleteTransaction_throwsIllegalArgument_whenCardDoesNotBelongToUser() {
    CreditCardTransaction tx = CreditCardTransaction.builder()
        .id("tx-1").creditCardId(5L).build();
    when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(tx));
    when(repository.findByIdAndUsername(5L, "user")).thenReturn(null);

    assertThrows(IllegalArgumentException.class,
        () -> service.deleteTransaction("tx-1", "user"));
  }

  @Test
  void deleteTransaction_deletesTransaction_whenAuthorized() {
    CreditCardTransaction tx = CreditCardTransaction.builder()
        .id("tx-1").creditCardId(5L).build();
    CreditCard card = CreditCard.builder().id(5L).username("user").build();
    when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(tx));
    when(repository.findByIdAndUsername(5L, "user")).thenReturn(card);

    service.deleteTransaction("tx-1", "user");

    verify(transactionRepository).deleteById("tx-1");
  }

  @Test
  void deleteTransaction_doesNotDeleteWhenExceptionBeforeDelete() {
    when(transactionRepository.findById("tx-1")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
        () -> service.deleteTransaction("tx-1", "user"));
    verify(transactionRepository, never()).deleteById(any());
  }
}

