package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.*;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.service.TransactionService;
import dev.avelar.jambock.reports.ReportGenerationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

  @Mock
  private TransactionService service;

  @InjectMocks
  private TransactionController controller;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(String username) {
    Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private void setAnonymousAuthentication() {
    Authentication anon = new AnonymousAuthenticationToken(
        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anon);
  }

  @Test
  void create_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Void> response = controller.create(new TransactionCreationForm());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void create_savesTransaction_andReturnsOk() {
    setAuthentication("alice");
    TransactionCreationForm form = TransactionCreationForm.builder()
        .description("Groceries").amount(BigDecimal.valueOf(50))
        .type("debit").bankAccountId(1L).updateAccount(true).build();

    ResponseEntity<Void> response = controller.create(form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).save(any(Transaction.class), eq("alice"), eq(true));
  }

  @Test
  void create_forwardsUpdateAccountFlag() {
    setAuthentication("alice");
    TransactionCreationForm form = TransactionCreationForm.builder()
        .updateAccount(false).build();

    controller.create(form);

    verify(service).save(any(Transaction.class), eq("alice"), eq(false));
  }

  @Test
  void getTransactions_returnsPage() {
    Transaction tx = Transaction.builder().id(1L).description("Coffee").build();
    when(service.findByAccountIdAndMonth(1L, 3, 2026, 0, 10))
        .thenReturn(new PageImpl<>(List.of(tx)));

    ResponseEntity<Page<Transaction>> response = controller.getTransactions(1L, 0, 10, 3, 2026);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().getTotalElements());
  }

  @Test
  void getTransactions_defaultsToCurrentMonthAndYear_whenParamsAreNull() {
    var now = OffsetDateTime.now();
    when(service.findByAccountIdAndMonth(eq(1L), eq(now.getMonthValue()), eq(now.getYear()), anyInt(), anyInt()))
        .thenReturn(Page.empty());

    ResponseEntity<Page<Transaction>> response = controller.getTransactions(1L, 0, 10, null, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).findByAccountIdAndMonth(1L, now.getMonthValue(), now.getYear(), 0, 10);
  }

  @Test
  void getTransactions_resetsMonth_whenNegative() {
    var now = OffsetDateTime.now();
    when(service.findByAccountIdAndMonth(eq(1L), eq(now.getMonthValue()), anyInt(), anyInt(), anyInt()))
        .thenReturn(Page.empty());

    controller.getTransactions(1L, 0, 10, -1, 2026);

    verify(service).findByAccountIdAndMonth(1L, now.getMonthValue(), 2026, 0, 10);
  }

  @Test
  void getTransactions_resetsMonth_whenGreaterThan12() {
    var now = OffsetDateTime.now();
    when(service.findByAccountIdAndMonth(eq(1L), eq(now.getMonthValue()), anyInt(), anyInt(), anyInt()))
        .thenReturn(Page.empty());

    controller.getTransactions(1L, 0, 10, 13, 2026);

    verify(service).findByAccountIdAndMonth(1L, now.getMonthValue(), 2026, 0, 10);
  }

  @Test
  void getTransactions_passesCorrectPageAndSize() {
    when(service.findByAccountIdAndMonth(1L, 5, 2026, 2, 25)).thenReturn(Page.empty());

    controller.getTransactions(1L, 2, 25, 5, 2026);

    verify(service).findByAccountIdAndMonth(1L, 5, 2026, 2, 25);
  }

  @Test
  void delete_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Void> response = controller.delete(1L);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void delete_deletesTransaction_andReturnsOk() {
    setAuthentication("alice");

    ResponseEntity<Void> response = controller.delete(42L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).delete(42L, "alice");
  }

  @Test
  void delete_usesAuthenticatedUsername() {
    setAuthentication("bob");

    controller.delete(7L);

    verify(service).delete(7L, "bob");
  }

  @Test
  void update_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Void> response = controller.update(1L, new TransactionUpdateForm());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void update_updatesTransaction_andReturnsOk() {
    setAuthentication("alice");
    TransactionUpdateForm form = TransactionUpdateForm.builder()
        .transactionDate(LocalDate.of(2026, 3, 1))
        .description("Lunch").amount(BigDecimal.valueOf(20)).type("debit").build();

    ResponseEntity<Void> response = controller.update(5L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).update(5L, "alice",
        LocalDate.of(2026, 3, 1), "Lunch", BigDecimal.valueOf(20), "debit");
  }

  @Test
  void update_returnsBadRequest_whenServiceThrowsException() {
    setAuthentication("alice");
    doThrow(new RuntimeException("not found"))
        .when(service).update(anyLong(), anyString(), any(), any(), any(), any());

    ResponseEntity<Void> response = controller.update(99L, new TransactionUpdateForm());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void transformToTransfer_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Void> response =
        controller.transformToTransfer(1L, new TransformToTransferForm());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void transformToTransfer_transformsAndReturnsOk() {
    setAuthentication("alice");
    TransformToTransferForm form = TransformToTransferForm.builder()
        .destinationAccountId(2L).build();

    ResponseEntity<Void> response = controller.transformToTransfer(1L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).transformToTransfer(1L, 2L, "alice");
  }

  @Test
  void transformToTransfer_returnsBadRequest_whenServiceThrowsIllegalState() {
    setAuthentication("alice");
    doThrow(new IllegalStateException("already a transfer"))
        .when(service).transformToTransfer(anyLong(), anyLong(), anyString());

    ResponseEntity<Void> response = controller.transformToTransfer(1L,
        TransformToTransferForm.builder().destinationAccountId(2L).build());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void calculateBalanceMonth_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Balance> response = controller.calculateBalanceMonth(3, 2026);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void calculateBalanceMonth_returnsBalance() {
    setAuthentication("alice");
    Balance balance = Balance.builder().income(3000.0).expenses(1500.0).amount(1500.0).build();
    when(service.calculateMonthBalance("alice", 3, 2026)).thenReturn(balance);

    ResponseEntity<Balance> response = controller.calculateBalanceMonth(3, 2026);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(balance, response.getBody());
  }

  @Test
  void calculateBalanceMonth_defaultsToCurrentMonthAndYear_whenParamsAreNull() {
    setAuthentication("alice");
    var now = OffsetDateTime.now();
    when(service.calculateMonthBalance("alice", now.getMonthValue(), now.getYear()))
        .thenReturn(new Balance());

    controller.calculateBalanceMonth(null, null);

    verify(service).calculateMonthBalance("alice", now.getMonthValue(), now.getYear());
  }

  @Test
  void calculateBalanceMonth_resetsMonth_whenZero() {
    setAuthentication("alice");
    var now = OffsetDateTime.now();
    when(service.calculateMonthBalance(eq("alice"), eq(now.getMonthValue()), anyInt()))
        .thenReturn(new Balance());

    controller.calculateBalanceMonth(0, 2026);

    verify(service).calculateMonthBalance("alice", now.getMonthValue(), 2026);
  }

  @Test
  void calculateBalanceMonth_resetsMonth_whenGreaterThan12() {
    setAuthentication("alice");
    var now = OffsetDateTime.now();
    when(service.calculateMonthBalance(eq("alice"), eq(now.getMonthValue()), anyInt()))
        .thenReturn(new Balance());

    controller.calculateBalanceMonth(13, 2026);

    verify(service).calculateMonthBalance("alice", now.getMonthValue(), 2026);
  }

  @Test
  void getLastTransactions_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Page<Transaction>> response = controller.getLastTransactions();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void getLastTransactions_returnsPageForAuthenticatedUser() {
    setAuthentication("alice");
    Page<Transaction> page = new PageImpl<>(List.of(Transaction.builder().id(1L).build()));
    when(service.findLast10("alice")).thenReturn(page);

    ResponseEntity<Page<Transaction>> response = controller.getLastTransactions();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().getTotalElements());
  }

  @Test
  void parseItauCsv_returnsBadRequest_whenFileIsNull() {
    ResponseEntity<Void> response = controller.parseItauCsv(null, 1L, false);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseItauCsv_returnsBadRequest_whenFileIsEmpty() {
    MultipartFile empty = new MockMultipartFile("file", new byte[0]);

    ResponseEntity<Void> response = controller.parseItauCsv(empty, 1L, false);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseItauCsv_returnsUnauthorized_whenNoAuthentication() {
    MultipartFile file = new MockMultipartFile("file", "itau.pdf", "application/pdf", "data".getBytes());

    ResponseEntity<Void> response = controller.parseItauCsv(file, 1L, false);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseItauCsv_returnsUnauthorized_whenAnonymousUser() {
    setAnonymousAuthentication();
    MultipartFile file = new MockMultipartFile("file", "itau.pdf", "application/pdf", "data".getBytes());

    ResponseEntity<Void> response = controller.parseItauCsv(file, 1L, false);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseItauCsv_returnsAccepted_onSuccess() throws Exception {
    setAuthentication("alice");
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);
    doNothing().when(mockFile).transferTo(any(java.io.File.class));

    ResponseEntity<Void> response = controller.parseItauCsv(mockFile, 1L, true);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    verify(service).save(any(java.io.File.class), eq("alice"), eq(1L), eq(true));
  }

  @Test
  void parseItauCsv_throwsIllegalState_onTransferException() throws Exception {
    setAuthentication("alice");
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);
    doThrow(new java.io.IOException("disk full")).when(mockFile).transferTo(any(java.io.File.class));

    assertThrows(IllegalStateException.class, () -> controller.parseItauCsv(mockFile, 1L, false));
  }

  @Test
  void parseOfxFile_returnsBadRequest_whenFileIsNull() {
    ResponseEntity<Void> response = controller.parseOfxFile(null, 1L, false);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsBadRequest_whenFileIsEmpty() {
    MultipartFile empty = new MockMultipartFile("file", new byte[0]);

    ResponseEntity<Void> response = controller.parseOfxFile(empty, 1L, false);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsUnauthorized_whenNoAuthentication() {
    MultipartFile file = new MockMultipartFile("file", "bank.ofx", "text/plain", "data".getBytes());

    ResponseEntity<Void> response = controller.parseOfxFile(file, 1L, false);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsUnauthorized_whenAnonymousUser() {
    setAnonymousAuthentication();
    MultipartFile file = new MockMultipartFile("file", "bank.ofx", "text/plain", "data".getBytes());

    ResponseEntity<Void> response = controller.parseOfxFile(file, 1L, false);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsAccepted_onSuccess() throws Exception {
    setAuthentication("alice");
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);
    doNothing().when(mockFile).transferTo(any(java.io.File.class));

    ResponseEntity<Void> response = controller.parseOfxFile(mockFile, 1L, false);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    verify(service).saveOfx(any(java.io.File.class), eq("alice"), eq(1L), eq(false));
  }

  @Test
  void parseOfxFile_returnsInternalServerError_onException() throws Exception {
    setAuthentication("alice");
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);
    doThrow(new java.io.IOException("disk error")).when(mockFile).transferTo(any(java.io.File.class));

    ResponseEntity<Void> response = controller.parseOfxFile(mockFile, 1L, false);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void search_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Page<Transaction>> response =
        controller.search(1L, "coffee", null, null, 0, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void search_returnsMatchingTransactions() {
    setAuthentication("alice");
    Transaction tx = Transaction.builder().id(1L).description("Coffee shop").build();
    LocalDate end = LocalDate.of(2026, 3, 19);
    LocalDate start = end.minusMonths(3);
    when(service.search("alice", 1L, "coffee", start, end, 0, 10))
        .thenReturn(new PageImpl<>(List.of(tx)));

    ResponseEntity<Page<Transaction>> response =
        controller.search(1L, "coffee", start, end, 0, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().getTotalElements());
  }

  @Test
  void search_defaultsEndDateToToday_whenNull() {
    setAuthentication("alice");
    when(service.search(eq("alice"), eq(1L), any(), any(LocalDate.class), eq(LocalDate.now()), anyInt(), anyInt()))
        .thenReturn(Page.empty());

    controller.search(1L, "", null, null, 0, 10);

    ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(service).search(eq("alice"), eq(1L), any(), any(), endCaptor.capture(), anyInt(), anyInt());
    assertEquals(LocalDate.now(), endCaptor.getValue());
  }

  @Test
  void search_defaultsStartDateToThreeMonthsBeforeEnd_whenNull() {
    setAuthentication("alice");
    LocalDate end = LocalDate.of(2026, 3, 19);
    when(service.search(any(), any(), any(), any(), eq(end), anyInt(), anyInt()))
        .thenReturn(Page.empty());

    controller.search(1L, "", null, end, 0, 10);

    ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(service).search(any(), any(), any(), startCaptor.capture(), eq(end), anyInt(), anyInt());
    assertEquals(end.minusMonths(3), startCaptor.getValue());
  }

  @Test
  void search_usesProvidedDates_whenNotNull() {
    setAuthentication("alice");
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 3, 1);
    when(service.search("alice", 1L, "gym", start, end, 0, 10)).thenReturn(Page.empty());

    controller.search(1L, "gym", start, end, 0, 10);

    verify(service).search("alice", 1L, "gym", start, end, 0, 10);
  }

  @Test
  void getMonthlySummary_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<java.util.List<MonthlySummaryDto>> response = controller.getMonthlySummary(2026);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void getMonthlySummary_returnsList() {
    setAuthentication("alice");
    List<MonthlySummaryDto> summary = List.of(
        new MonthlySummaryDto(1, 3000.0, 1200.0),
        new MonthlySummaryDto(2, 3200.0, 1400.0));
    when(service.getMonthlySummary("alice", 2026)).thenReturn(summary);

    ResponseEntity<java.util.List<MonthlySummaryDto>> response = controller.getMonthlySummary(2026);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
  }

  @Test
  void getMonthlySummary_defaultsToCurrentYear_whenNull() {
    setAuthentication("alice");
    int currentYear = OffsetDateTime.now().getYear();
    when(service.getMonthlySummary("alice", currentYear)).thenReturn(List.of());

    controller.getMonthlySummary(null);

    verify(service).getMonthlySummary("alice", currentYear);
  }

  @Test
  void generateMonthlyReport_returnsUnauthorized_whenNoAuthentication() throws ReportGenerationException {
    ResponseEntity<byte[]> response = controller.generateMonthlyReport(1L, 3, 2026, "en");

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void generateMonthlyReport_returnsPdfBytes() throws ReportGenerationException {
    setAuthentication("alice");
    byte[] pdf = "PDF-CONTENT".getBytes();
    when(service.generateMonthlyReport("alice", 1L, 3, 2026, "en")).thenReturn(pdf);

    ResponseEntity<byte[]> response = controller.generateMonthlyReport(1L, 3, 2026, "en");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertArrayEquals(pdf, response.getBody());
  }

  @Test
  void generateMonthlyReport_setsContentTypeHeader() throws ReportGenerationException {
    setAuthentication("alice");
    when(service.generateMonthlyReport(any(), any(), any(), any(), any()))
        .thenReturn(new byte[1]);

    ResponseEntity<byte[]> response = controller.generateMonthlyReport(1L, 3, 2026, "en");

    assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
  }

  @Test
  void generateMonthlyReport_setsContentDispositionWithFilename() throws ReportGenerationException {
    setAuthentication("alice");
    when(service.generateMonthlyReport(any(), any(), any(), any(), any()))
        .thenReturn(new byte[1]);

    ResponseEntity<byte[]> response = controller.generateMonthlyReport(1L, 3, 2026, "en");

    String disposition = response.getHeaders().getFirst("Content-Disposition");
    assertNotNull(disposition);
    assertTrue(disposition.contains("transactions-2026-03.pdf"));
  }

  @Test
  void generateMonthlyReport_defaultsToCurrentMonthAndYear_whenNull() throws ReportGenerationException {
    setAuthentication("alice");
    var now = OffsetDateTime.now();
    when(service.generateMonthlyReport("alice", 1L, now.getMonthValue(), now.getYear(), "en"))
        .thenReturn(new byte[1]);

    controller.generateMonthlyReport(1L, null, null, "en");

    verify(service).generateMonthlyReport("alice", 1L, now.getMonthValue(), now.getYear(), "en");
  }

  @Test
  void generateMonthlyReport_resetsMonth_whenOutOfRange() throws ReportGenerationException {
    setAuthentication("alice");
    var now = OffsetDateTime.now();
    when(service.generateMonthlyReport(eq("alice"), eq(1L), eq(now.getMonthValue()), eq(2026), any()))
        .thenReturn(new byte[1]);

    controller.generateMonthlyReport(1L, 13, 2026, "en");

    verify(service).generateMonthlyReport("alice", 1L, now.getMonthValue(), 2026, "en");
  }

  @Test
  void updateTags_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Void> response = controller.updateTags(1L, new UpdateTagsForm());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void updateTags_updatesAndReturnsOk() {
    setAuthentication("alice");
    UpdateTagsForm form = new UpdateTagsForm(new String[]{"food", "personal"});

    ResponseEntity<Void> response = controller.updateTags(5L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).updateTags(5L, new String[]{"food", "personal"}, "alice");
  }

  @Test
  void updateTags_returnsBadRequest_whenServiceThrowsException() {
    setAuthentication("alice");
    doThrow(new RuntimeException("not found"))
        .when(service).updateTags(anyLong(), any(), anyString());

    ResponseEntity<Void> response = controller.updateTags(99L, new UpdateTagsForm());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getAllTags_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<java.util.List<String>> response = controller.getAllTags();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void getAllTags_returnsTagList() {
    setAuthentication("alice");
    when(service.findAllTags("alice")).thenReturn(List.of("food", "transport", "health"));

    ResponseEntity<java.util.List<String>> response = controller.getAllTags();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of("food", "transport", "health"), response.getBody());
  }

  @Test
  void getAllTags_returnsEmptyList_whenNoTags() {
    setAuthentication("alice");
    when(service.findAllTags("alice")).thenReturn(List.of());

    ResponseEntity<java.util.List<String>> response = controller.getAllTags();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }
}

