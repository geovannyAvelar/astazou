package dev.avelar.astazou.service;

import dev.avelar.astazou.dto.Balance;
import dev.avelar.astazou.dto.MonthlySummaryDto;
import dev.avelar.astazou.exception.NotFoundException;
import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.model.User;
import dev.avelar.astazou.repository.BankAccountRepository;
import dev.avelar.astazou.repository.ReportTokenRepository;
import dev.avelar.astazou.repository.TransactionRepository;
import dev.avelar.astazou.repository.UserRepository;
import dev.avelar.jambock.reports.ReportEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @Mock
  private TransactionRepository repository;

  @Mock
  private BankAccountRepository bankAccountRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ReportEngine reportEngine;

  @Mock
  private ReportTokenRepository reportTokenRepository;

  @Mock
  private QrCodeService qrCodeService;

  @InjectMocks
  private TransactionService service;

  @Test
  void parseItauCsv_parsesValidRows(@TempDir Path tmp) throws IOException {
    String csv =
        "date,description,amount,type,page\n" + "2025-01-10,\"Coffee shop\",-15.00,debit,1\n" + "2025-01-10,\"Supermarket\",-50.00,debit,1\n" + "2025-01-11,\"Salary\",3000.00,credit,1\n";
    File csvFile = tmp.resolve("test.csv").toFile();
    Files.writeString(csvFile.toPath(), csv);

    List<Transaction> result = service.parseItauCsv(csvFile);

    assertEquals(3, result.size());
    assertEquals(1, result.get(0).getSequence());
    assertEquals(2, result.get(1).getSequence());
    assertEquals(1, result.get(2).getSequence());
    assertEquals(LocalDate.of(2025, 1, 10), result.get(0).getTransactionDate());
    assertEquals("Coffee shop", result.get(0).getDescription());
    assertEquals(new BigDecimal("-15.00"), result.get(0).getAmount());
  }

  @Test
  void parseItauCsv_skipsBalanceRows(@TempDir Path tmp) throws IOException {
    String csv =
        "date,description,amount,type,page\n" + "2025-03-01,\"SALDO DO DIA\",1000.00,credit,1\n" + "2025-03-01,\"PIX Received\",200.00,credit,1\n";
    File csvFile = tmp.resolve("balance.csv").toFile();
    Files.writeString(csvFile.toPath(), csv);

    List<Transaction> result = service.parseItauCsv(csvFile);

    assertEquals(1, result.size());
    assertEquals("PIX Received", result.get(0).getDescription());
  }

  @Test
  void parseItauCsv_emptyFile_returnsEmptyList(@TempDir Path tmp) throws IOException {
    File csvFile = tmp.resolve("empty.csv").toFile();
    Files.writeString(csvFile.toPath(), "");

    List<Transaction> result = service.parseItauCsv(csvFile);

    assertTrue(result.isEmpty());
  }

  @Test
  void parseItauCsv_nullFile_throwsNPE() {
    assertThrows(NullPointerException.class, () -> service.parseItauCsv(null));
  }

  @Test
  void parseOfxFile_parsesDebitAndCredit(@TempDir Path tmp) throws IOException {
    String ofx =
        "<OFX>\n" + "<STMTTRN>\n" + "<TRNTYPE>DEBIT</TRNTYPE>\n" + "<DTPOSTED>20250210</DTPOSTED>\n" + "<TRNAMT>-120.00</TRNAMT>\n" + "<MEMO>Electricity bill</MEMO>\n" + "</STMTTRN>\n" + "<STMTTRN>\n" + "<TRNTYPE>CREDIT</TRNTYPE>\n" + "<DTPOSTED>20250210</DTPOSTED>\n" + "<TRNAMT>500.00</TRNAMT>\n" + "<MEMO>Refund</MEMO>\n" + "</STMTTRN>\n" + "</OFX>\n";
    File ofxFile = tmp.resolve("test.ofx").toFile();
    Files.writeString(ofxFile.toPath(), ofx);

    List<Transaction> result = service.parseOfxFile(ofxFile);

    assertEquals(2, result.size());
    assertEquals("debit", result.get(0).getType());
    assertEquals(new BigDecimal("-120.00"), result.get(0).getAmount());
    assertEquals("credit", result.get(1).getType());
    assertEquals(2, result.get(1).getSequence()); // second entry on same day
  }

  @Test
  void parseOfxFile_skipsTransactionsWithMissingDate(@TempDir Path tmp) throws IOException {
    String ofx =
        "<OFX>\n" + "<STMTTRN>\n" + "<TRNTYPE>DEBIT</TRNTYPE>\n" + "<TRNAMT>-50.00</TRNAMT>\n" + "</STMTTRN>\n" + "</OFX>\n";
    File ofxFile = tmp.resolve("missing.ofx").toFile();
    Files.writeString(ofxFile.toPath(), ofx);

    List<Transaction> result = service.parseOfxFile(ofxFile);

    assertTrue(result.isEmpty());
  }

  @Test
  void parseOfxFile_fallsBackToNameWhenNoMemo(@TempDir Path tmp) throws IOException {
    String ofx =
        "<OFX>\n" + "<STMTTRN>\n" + "<DTPOSTED>20250301</DTPOSTED>\n" + "<TRNAMT>100.00</TRNAMT>\n" + "<NAME>Direct deposit</NAME>\n" + "</STMTTRN>\n" + "</OFX>\n";
    File ofxFile = tmp.resolve("name.ofx").toFile();
    Files.writeString(ofxFile.toPath(), ofx);

    List<Transaction> result = service.parseOfxFile(ofxFile);

    assertEquals(1, result.size());
    assertEquals("Direct deposit", result.get(0).getDescription());
  }

  // ── findByAccountIdAndMonth ───────────────────────────────────────────────

  @Test
  void findByAccountIdAndMonth_defaultsItemsPerPageWhenZeroOrNegative() {
    when(repository.findByAccountIdAndMonth(1L, 3, 2025, 10, 0)).thenReturn(List.of());
    when(repository.countByAccountIdAndMonth(1L, 3, 2025)).thenReturn(0L);

    Page<Transaction> page = service.findByAccountIdAndMonth(1L, 3, 2025, 0, 0);

    assertNotNull(page);
    verify(repository).findByAccountIdAndMonth(1L, 3, 2025, 10, 0);
  }

  @Test
  void findByAccountIdAndMonth_returnsPagedResults() {
    Transaction t = Transaction.builder().id(1L).build();
    when(repository.findByAccountIdAndMonth(1L, 3, 2025, 5, 0)).thenReturn(List.of(t));
    when(repository.countByAccountIdAndMonth(1L, 3, 2025)).thenReturn(1L);

    Page<Transaction> page = service.findByAccountIdAndMonth(1L, 3, 2025, 0, 5);

    assertEquals(1, page.getTotalElements());
    assertEquals(t, page.getContent().get(0));
  }

  @Test
  void search_normalisesNullQueryToEmpty() {
    LocalDate start = LocalDate.of(2025, 1, 1);
    LocalDate end = LocalDate.of(2025, 1, 31);
    when(repository.searchTransactions("user", 1L, "", start, end, 10, 0)).thenReturn(List.of());
    when(repository.countSearchTransactions("user", 1L, "", start, end)).thenReturn(0L);

    service.search("user", 1L, null, start, end, 0, 10);

    verify(repository).searchTransactions("user", 1L, "", start, end, 10, 0);
  }

  @Test
  void search_normalisesBlankQueryToEmpty() {
    LocalDate start = LocalDate.of(2025, 1, 1);
    LocalDate end = LocalDate.of(2025, 1, 31);
    when(repository.searchTransactions("user", 1L, "", start, end, 10, 0)).thenReturn(List.of());
    when(repository.countSearchTransactions("user", 1L, "", start, end)).thenReturn(0L);

    service.search("user", 1L, "   ", start, end, 0, 10);

    verify(repository).searchTransactions("user", 1L, "", start, end, 10, 0);
  }

  @Test
  void search_defaultsItemsPerPageWhenZero() {
    LocalDate start = LocalDate.of(2025, 1, 1);
    LocalDate end = LocalDate.of(2025, 1, 31);
    when(repository.searchTransactions(any(), any(), any(), any(), any(), eq(10), eq(0))).thenReturn(List.of());
    when(repository.countSearchTransactions(any(), any(), any(), any(), any())).thenReturn(0L);

    service.search("user", 1L, "coffee", start, end, 0, 0);

    verify(repository).searchTransactions("user", 1L, "coffee", start, end, 10, 0);
  }

  // ── calculateMonthBalance ─────────────────────────────────────────────────

  @Test
  void calculateMonthBalance_returnsCorrectBalance() {
    User user = User.builder().username("user").preferredCurrency("BRL").build();
    when(userRepository.findById("user")).thenReturn(Optional.of(user));
    when(repository.calculateIncomeByUsernameAndMonth("user", 3, 2025, "BRL")).thenReturn(1000.0);
    when(repository.calculateExpenseByUsernameAndMonth("user", 3, 2025, "BRL")).thenReturn(400.0);

    Balance balance = service.calculateMonthBalance("user", 3, 2025);

    assertEquals(1000.0, balance.getIncome());
    assertEquals(400.0, balance.getExpenses());
    assertEquals(600.0, balance.getAmount());
  }

  @Test
  void findLast10_delegatesToRepository() {
    Transaction t = Transaction.builder().id(99L).build();
    when(repository.findLast10("user")).thenReturn(List.of(t));

    Page<Transaction> page = service.findLast10("user");

    assertEquals(1, page.getTotalElements());
    assertEquals(t, page.getContent().get(0));
  }

  @Test
  void save_throwsNotFound_whenAccountDoesNotExist() {
    Transaction tx = Transaction.builder().bankAccountId(99L).transactionDate(LocalDate.now()).build();
    when(bankAccountRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.save(tx, "user", false));
  }

  @Test
  void save_throwsIllegalState_whenUserDoesNotOwnAccount() {
    Transaction tx = Transaction.builder().bankAccountId(1L).transactionDate(LocalDate.now()).build();
    BankAccount account = BankAccount.builder().id(1L).name("Acc").balance(BigDecimal.ZERO).username("other").build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

    assertThrows(IllegalStateException.class, () -> service.save(tx, "user", false));
  }

  @Test
  void save_negatesPositiveDebitAmount() {
    Transaction tx = Transaction.builder().bankAccountId(1L).transactionDate(LocalDate.of(2025, 1, 10)).type("debit")
        .amount(new BigDecimal("100.00")).build();
    BankAccount account = BankAccount.builder().id(1L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(repository.getLastDaySequence(1L, tx.getTransactionDate())).thenReturn(0);

    service.save(tx, "user", false);

    assertEquals(new BigDecimal("100.00").negate(), tx.getAmount());
    verify(repository).upsert(tx);
    verify(repository, never()).updateBankAccountBalance(anyDouble(), anyLong());
  }

  @Test
  void save_doesNotNegateAlreadyNegativeDebitAmount() {
    Transaction tx = Transaction.builder().bankAccountId(1L).transactionDate(LocalDate.of(2025, 1, 10)).type("debit")
        .amount(new BigDecimal("-100.00")).build();
    BankAccount account = BankAccount.builder().id(1L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(repository.getLastDaySequence(1L, tx.getTransactionDate())).thenReturn(0);

    service.save(tx, "user", false);

    assertEquals(new BigDecimal("-100.00"), tx.getAmount());
  }

  @Test
  void save_updatesAccountBalance_whenFlagIsTrue() {
    Transaction tx = Transaction.builder().bankAccountId(1L).transactionDate(LocalDate.of(2025, 1, 10)).type("credit")
        .amount(new BigDecimal("200.00")).build();
    BankAccount account = BankAccount.builder().id(1L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(repository.getLastDaySequence(1L, tx.getTransactionDate())).thenReturn(2);

    service.save(tx, "user", true);

    verify(repository).updateBankAccountBalance(200.0, 1L);
    assertEquals(3, tx.getSequence());
  }

  @Test
  void saveBatch_skipsTransactions_whenAccountNotFound() {
    Transaction tx = Transaction.builder().transactionDate(LocalDate.now()).amount(new BigDecimal("10.00")).build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

    // should not throw
    service.save(List.of(tx), "user", 1L, false);

    verify(repository, never()).upsert(any());
  }

  @Test
  void saveBatch_upserts_whenValidAccount() {
    Transaction tx = Transaction.builder().transactionDate(LocalDate.now()).amount(new BigDecimal("10.00")).build();
    BankAccount account = BankAccount.builder().id(1L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

    service.save(List.of(tx), "user", 1L, false);

    verify(repository).upsert(tx);
    assertEquals(1L, tx.getBankAccountId());
    verify(repository, never()).updateBankAccountBalance(anyDouble(), anyLong());
  }

  @Test
  void saveBatch_updatesBalance_whenFlagIsTrue() {
    Transaction tx = Transaction.builder().transactionDate(LocalDate.now()).amount(new BigDecimal("50.00")).build();
    BankAccount account = BankAccount.builder().id(1L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

    service.save(List.of(tx), "user", 1L, true);

    verify(repository).updateBankAccountBalance(50.0, 1L);
  }

  @Test
  void delete_delegatesToRepository() {
    service.delete(42L, "user");
    verify(repository).delete(42L, "user");
  }

  @Test
  void update_throwsNotFound_whenTransactionDoesNotExist() {
    when(repository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class,
        () -> service.update(1L, "user", LocalDate.now(), "desc", BigDecimal.TEN, "debit"));
  }

  @Test
  void update_throwsIllegalState_whenAccountDoesNotBelongToUser() {
    Transaction tx = Transaction.builder().id(1L).bankAccountId(5L).build();
    BankAccount account = BankAccount.builder().id(5L).name("Acc").balance(BigDecimal.ZERO).username("other").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));

    assertThrows(IllegalStateException.class,
        () -> service.update(1L, "user", LocalDate.now(), "desc", BigDecimal.TEN, "debit"));
  }

  @Test
  void update_negatesPositiveDebitAmount() {
    Transaction tx = Transaction.builder().id(1L).bankAccountId(5L).build();
    BankAccount account = BankAccount.builder().id(5L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));

    service.update(1L, "user", LocalDate.of(2025, 3, 1), "desc", new BigDecimal("50.00"), "debit");

    verify(repository).update(eq(1L), eq("user"), eq(LocalDate.of(2025, 3, 1)), eq("desc"),
        eq(new BigDecimal("-50.00")), eq("debit"));
  }

  @Test
  void update_doesNotNegateAlreadyNegativeDebitAmount() {
    Transaction tx = Transaction.builder().id(1L).bankAccountId(5L).build();
    BankAccount account = BankAccount.builder().id(5L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));

    service.update(1L, "user", LocalDate.of(2025, 3, 1), "desc", new BigDecimal("-50.00"), "debit");

    verify(repository).update(eq(1L), eq("user"), any(), any(), eq(new BigDecimal("-50.00")), eq("debit"));
  }

  @Test
  void update_doesNotNegateCredit() {
    Transaction tx = Transaction.builder().id(1L).bankAccountId(5L).build();
    BankAccount account = BankAccount.builder().id(5L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));

    service.update(1L, "user", LocalDate.of(2025, 3, 1), "desc", new BigDecimal("100.00"), "credit");

    verify(repository).update(eq(1L), eq("user"), any(), any(), eq(new BigDecimal("100.00")), eq("credit"));
  }

  @Test
  void transformToTransfer_throwsNotFound_whenTransactionMissing() {
    when(repository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.transformToTransfer(1L, 2L, "user"));
  }

  @Test
  void transformToTransfer_throwsIllegalState_whenSameSourceAndDestinationAccount() {
    Transaction tx = Transaction.builder().id(1L).bankAccountId(5L).type("debit").transactionDate(LocalDate.now())
        .amount(new BigDecimal("-100")).build();
    BankAccount account = BankAccount.builder().id(5L).name("Acc").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(account));

    assertThrows(IllegalStateException.class, () -> service.transformToTransfer(1L, 5L, "user"));
  }

  @Test
  void transformToTransfer_throwsIllegalState_whenTransactionTypeIsNotDebitOrCredit() {
    Transaction tx = Transaction.builder().id(1L).bankAccountId(1L).type("transfer").transactionDate(LocalDate.now())
        .amount(new BigDecimal("-100")).build();
    BankAccount source = BankAccount.builder().id(1L).name("Src").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(source));

    assertThrows(IllegalStateException.class, () -> service.transformToTransfer(1L, 2L, "user"));
  }

  @Test
  void transformToTransfer_debit_createsCreditOnDestinationAndChangesSourceType() {
    Transaction tx =
        Transaction.builder().id(1L).bankAccountId(1L).type("debit").transactionDate(LocalDate.of(2025, 3, 10))
            .description("Payment").amount(new BigDecimal("-100.00")).build();
    BankAccount source =
        BankAccount.builder().id(1L).name("Checking").balance(BigDecimal.ZERO).username("user").build();
    BankAccount dest = BankAccount.builder().id(2L).name("Savings").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(source));
    when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(dest));
    when(repository.getLastDaySequence(2L, tx.getTransactionDate())).thenReturn(0);

    service.transformToTransfer(1L, 2L, "user");

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(repository, times(2)).upsert(captor.capture());

    Transaction creditTx =
        captor.getAllValues().stream().filter(t -> "transfer_credit".equals(t.getType())).findFirst().orElseThrow();
    assertEquals(new BigDecimal("100.00"), creditTx.getAmount());
    assertEquals(2L, creditTx.getBankAccountId());
    assertEquals("transfer", tx.getType());
  }

  @Test
  void transformToTransfer_credit_createsDebitOnDestinationAndChangesSourceType() {
    Transaction tx =
        Transaction.builder().id(1L).bankAccountId(1L).type("credit").transactionDate(LocalDate.of(2025, 3, 10))
            .description("Income").amount(new BigDecimal("200.00")).build();
    BankAccount source =
        BankAccount.builder().id(1L).name("Checking").balance(BigDecimal.ZERO).username("user").build();
    BankAccount dest = BankAccount.builder().id(2L).name("Savings").balance(BigDecimal.ZERO).username("user").build();
    when(repository.findById(1L)).thenReturn(Optional.of(tx));
    when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(source));
    when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(dest));
    when(repository.getLastDaySequence(2L, tx.getTransactionDate())).thenReturn(0);

    service.transformToTransfer(1L, 2L, "user");

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(repository, times(2)).upsert(captor.capture());

    Transaction debitTx =
        captor.getAllValues().stream().filter(t -> "transfer_debit".equals(t.getType())).findFirst().orElseThrow();
    assertEquals(new BigDecimal("-200.00"), debitTx.getAmount());
    assertEquals(2L, debitTx.getBankAccountId());
    assertEquals("transfer_credit", tx.getType());
  }

  @Test
  void updateTags_throwsNotFound_whenTransactionMissing() {
    when(repository.existsById(99L)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.updateTags(99L, new String[] {"food"}, "user"));
  }

  @Test
  void updateTags_normalisesTrimsLowercaseAndDeduplicates() {
    when(repository.existsById(1L)).thenReturn(true);

    service.updateTags(1L, new String[] {"  Food  ", "food", "TRANSPORT"}, "user");

    ArgumentCaptor<String> pgArrayCaptor = ArgumentCaptor.forClass(String.class);
    verify(repository).updateTags(eq(1L), eq("user"), pgArrayCaptor.capture());

    String pgArray = pgArrayCaptor.getValue();
    assertTrue(pgArray.contains("\"food\""));
    assertTrue(pgArray.contains("\"transport\""));
    // "food" must appear only once (deduplication)
    assertEquals(pgArray.indexOf("\"food\""), pgArray.lastIndexOf("\"food\""));
  }

  @Test
  void updateTags_handlesNullTagsArray() {
    when(repository.existsById(1L)).thenReturn(true);

    service.updateTags(1L, null, "user");

    verify(repository).updateTags(1L, "user", "{}");
  }

  @Test
  void updateTags_filtersBlankEntries() {
    when(repository.existsById(1L)).thenReturn(true);

    service.updateTags(1L, new String[] {"", "  ", "food"}, "user");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(repository).updateTags(eq(1L), eq("user"), captor.capture());

    String pgArray = captor.getValue();
    assertTrue(pgArray.contains("\"food\""));
    assertFalse(pgArray.contains("\"\""));
  }

  @Test
  void findAllTags_delegatesToRepository() {
    when(repository.findAllTagsByUsername("user")).thenReturn(List.of("food", "transport"));

    List<String> tags = service.findAllTags("user");

    assertEquals(List.of("food", "transport"), tags);
  }

  @Test
  void getMonthlySummary_always12Months_andFillsZeroForMissing() {
    User user = User.builder().username("user").preferredCurrency("BRL").build();
    when(userRepository.findById("user")).thenReturn(Optional.of(user));
    when(repository.getMonthlySummary("user", 2025, "BRL")).thenReturn(List.of(new MonthlySummaryDto(3, 1000.0, 400.0)));

    List<MonthlySummaryDto> result = service.getMonthlySummary("user", 2025);

    assertEquals(12, result.size());
    assertEquals(1000.0, result.get(2).income());
    assertEquals(400.0, result.get(2).expenses());
    assertEquals(0.0, result.get(0).income());   // month 1 missing
    assertEquals(0.0, result.get(11).expenses()); // month 12 missing
  }

  @Test
  void getMonthlySummary_allMonthsPresent_noGapFilling() {
    User user = User.builder().username("user").preferredCurrency("BRL").build();
    when(userRepository.findById("user")).thenReturn(Optional.of(user));
    List<MonthlySummaryDto> rows = new ArrayList<>();
    for (int m = 1; m <= 12; m++) {
      rows.add(new MonthlySummaryDto(m, m * 100.0, m * 50.0));
    }
    when(repository.getMonthlySummary("user", 2025, "BRL")).thenReturn(rows);

    List<MonthlySummaryDto> result = service.getMonthlySummary("user", 2025);

    assertEquals(12, result.size());
    for (int m = 1; m <= 12; m++) {
      assertEquals(m * 100.0, result.get(m - 1).income());
      assertEquals(m * 50.0, result.get(m - 1).expenses());
    }
  }

  @Test
  void getMonthlySummary_emptyRepositoryResult_returns12ZeroMonths() {
    User user = User.builder().username("user").preferredCurrency("BRL").build();
    when(userRepository.findById("user")).thenReturn(Optional.of(user));
    when(repository.getMonthlySummary("user", 2025, "BRL")).thenReturn(List.of());

    List<MonthlySummaryDto> result = service.getMonthlySummary("user", 2025);

    assertEquals(12, result.size());
    result.forEach(dto -> {
      assertEquals(0.0, dto.income());
      assertEquals(0.0, dto.expenses());
    });
  }
}