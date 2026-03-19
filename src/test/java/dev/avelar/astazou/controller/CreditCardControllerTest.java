package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.CreditCardCreationForm;
import dev.avelar.astazou.model.CreditCard;
import dev.avelar.astazou.model.CreditCardTransaction;
import dev.avelar.astazou.service.CreditCardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
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
class CreditCardControllerTest {

  @Mock
  private CreditCardService service;

  @InjectMocks
  private CreditCardController controller;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(String username) {
    Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  void save_returnsUnauthorized_whenNoAuthentication() {
    CreditCardCreationForm form = CreditCardCreationForm.builder().name("Visa Gold").build();

    ResponseEntity<Void> response = controller.save(form);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void save_savesCardWithUsername_andReturnsOk() {
    setAuthentication("alice");
    CreditCardCreationForm form = CreditCardCreationForm.builder().name("Visa Gold").build();

    ResponseEntity<Void> response = controller.save(form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).save(argThat(card -> "alice".equals(card.getUsername()) && "Visa Gold".equals(card.getName())));
  }

  @Test
  void save_setsUsernameFromAuthenticationOnCard() {
    setAuthentication("bob");
    CreditCardCreationForm form = CreditCardCreationForm.builder().name("Mastercard").build();

    controller.save(form);

    verify(service).save(argThat(card -> "bob".equals(card.getUsername())));
  }

  @Test
  void find_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Page<CreditCard>> response = controller.find(0, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void find_returnsPageOfCards() {
    setAuthentication("alice");
    CreditCard card = CreditCard.builder().id(1L).name("Visa").username("alice").build();
    Page<CreditCard> page = new PageImpl<>(List.of(card));
    when(service.findByUsername("alice", 0, 10)).thenReturn(page);

    ResponseEntity<Page<CreditCard>> response = controller.find(0, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getTotalElements());
    assertEquals(card, response.getBody().getContent().getFirst());
  }

  @Test
  void find_passesCorrectPageAndSizeToService() {
    setAuthentication("alice");
    when(service.findByUsername("alice", 3, 5)).thenReturn(Page.empty());

    controller.find(3, 5);

    verify(service).findByUsername("alice", 3, 5);
  }

  @Test
  void find_returnsEmptyPage_whenNoCards() {
    setAuthentication("alice");
    when(service.findByUsername("alice", 0, 10)).thenReturn(Page.empty());

    ResponseEntity<Page<CreditCard>> response = controller.find(0, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().getTotalElements());
  }

  @Test
  void findById_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<CreditCard> response = controller.findById(1L);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void findById_returnsNotFound_whenCardDoesNotBelongToUser() {
    setAuthentication("alice");
    when(service.findByIdAndUsername(99L, "alice")).thenReturn(null);

    ResponseEntity<CreditCard> response = controller.findById(99L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void findById_returnsCard_whenFound() {
    setAuthentication("alice");
    CreditCard card = CreditCard.builder().id(1L).name("Amex").username("alice").build();
    when(service.findByIdAndUsername(1L, "alice")).thenReturn(card);

    ResponseEntity<CreditCard> response = controller.findById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(card, response.getBody());
  }

  @Test
  void findTransactions_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<List<CreditCardTransaction>> response = controller.findTransactions(1L, 3, 2026);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void findTransactions_returnsListOfTransactions() {
    setAuthentication("alice");
    CreditCardTransaction tx = CreditCardTransaction.builder()
        .id("tx-1")
        .amount(BigDecimal.valueOf(49.99))
        .description("Netflix")
        .transactionDate(OffsetDateTime.now())
        .statementDate(LocalDate.of(2026, 3, 1))
        .creditCardId(1L)
        .build();
    when(service.getTransactionsByStatement(1L, "alice", 3, 2026)).thenReturn(List.of(tx));

    ResponseEntity<List<CreditCardTransaction>> response = controller.findTransactions(1L, 3, 2026);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("tx-1", response.getBody().getFirst().getId());
  }

  @Test
  void findTransactions_passesCorrectParamsToService() {
    setAuthentication("alice");
    when(service.getTransactionsByStatement(2L, "alice", 12, 2025)).thenReturn(List.of());

    controller.findTransactions(2L, 12, 2025);

    verify(service).getTransactionsByStatement(2L, "alice", 12, 2025);
  }

  @Test
  void findTransactions_returnsEmptyList_whenNoTransactions() {
    setAuthentication("alice");
    when(service.getTransactionsByStatement(1L, "alice", 1, 2026)).thenReturn(List.of());

    ResponseEntity<List<CreditCardTransaction>> response = controller.findTransactions(1L, 1, 2026);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  @Test
  void parseOfxFile_returnsBadRequest_whenFileIsNull() {
    ResponseEntity<Void> response = controller.parseOfxFile(null, 1L);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsBadRequest_whenFileIsEmpty() {
    MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

    ResponseEntity<Void> response = controller.parseOfxFile(emptyFile, 1L);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsUnauthorized_whenNoAuthentication() {
    MultipartFile file = new MockMultipartFile("file", "test.ofx", "text/plain", "data".getBytes());

    ResponseEntity<Void> response = controller.parseOfxFile(file, 1L);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsUnauthorized_whenAnonymousUser() {
    Authentication anonAuth = new AnonymousAuthenticationToken(
        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    MultipartFile file = new MockMultipartFile("file", "test.ofx", "text/plain", "data".getBytes());

    ResponseEntity<Void> response = controller.parseOfxFile(file, 1L);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void parseOfxFile_returnsForbidden_whenCardDoesNotBelongToUser() {
    setAuthentication("alice");
    when(service.findByIdAndUsername(1L, "alice")).thenReturn(null);

    MultipartFile file = new MockMultipartFile("file", "test.ofx", "text/plain", "data".getBytes());

    ResponseEntity<Void> response = controller.parseOfxFile(file, 1L);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(service, never()).parseAndSaveOfxFile(any(), any());
  }

  @Test
  void parseOfxFile_returnsAccepted_onSuccess() throws Exception {
    setAuthentication("alice");
    CreditCard card = CreditCard.builder().id(1L).name("Visa").username("alice").build();
    when(service.findByIdAndUsername(1L, "alice")).thenReturn(card);

    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);
    doNothing().when(mockFile).transferTo(any(java.io.File.class));

    ResponseEntity<Void> response = controller.parseOfxFile(mockFile, 1L);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    verify(service).parseAndSaveOfxFile(any(java.io.File.class), eq(1L));
  }

  @Test
  void parseOfxFile_returnsInternalServerError_onException() throws Exception {
    setAuthentication("alice");
    CreditCard card = CreditCard.builder().id(1L).name("Visa").username("alice").build();
    when(service.findByIdAndUsername(1L, "alice")).thenReturn(card);

    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);
    doThrow(new RuntimeException("IO failure")).when(mockFile).transferTo(any(java.io.File.class));

    ResponseEntity<Void> response = controller.parseOfxFile(mockFile, 1L);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void deleteTransaction_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Void> response = controller.deleteTransaction("tx-1");

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void deleteTransaction_returnsOk_onSuccess() {
    setAuthentication("alice");
    doNothing().when(service).deleteTransaction("tx-1", "alice");

    ResponseEntity<Void> response = controller.deleteTransaction("tx-1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(service).deleteTransaction("tx-1", "alice");
  }

  @Test
  void deleteTransaction_returnsForbidden_whenServiceThrowsIllegalArgument() {
    setAuthentication("alice");
    doThrow(new IllegalArgumentException("not your transaction"))
        .when(service).deleteTransaction("tx-99", "alice");

    ResponseEntity<Void> response = controller.deleteTransaction("tx-99");

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void deleteTransaction_usesAuthenticatedUsername() {
    setAuthentication("bob");
    doNothing().when(service).deleteTransaction(anyString(), eq("bob"));

    controller.deleteTransaction("tx-5");

    verify(service).deleteTransaction("tx-5", "bob");
  }
}

