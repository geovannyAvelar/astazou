package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.BankAccountCreationForm;
import dev.avelar.astazou.dto.BankAccountUpdateForm;
import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.service.BankAccountService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountControllerTest {

  @Mock
  private BankAccountService service;

  @InjectMocks
  private BankAccountController controller;

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
    BankAccountCreationForm form = BankAccountCreationForm.builder()
        .name("Checking")
        .initialBalance(BigDecimal.valueOf(1000))
        .build();

    ResponseEntity<BankAccount> response = controller.save(form);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void save_savesAccountWithUsername_andReturnsOk() {
    setAuthentication("alice");

    BankAccountCreationForm form = BankAccountCreationForm.builder()
        .name("Savings")
        .initialBalance(BigDecimal.valueOf(500))
        .build();

    BankAccount saved = BankAccount.builder()
        .id(1L)
        .name("Savings")
        .balance(BigDecimal.valueOf(500))
        .username("alice")
        .build();

    when(service.save(any(BankAccount.class))).thenReturn(saved);

    ResponseEntity<BankAccount> response = controller.save(form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("alice", response.getBody().getUsername());
    assertEquals("Savings", response.getBody().getName());
    assertEquals(BigDecimal.valueOf(500), response.getBody().getBalance());
    verify(service).save(any(BankAccount.class));
  }

  @Test
  void save_setsUsernameFromAuthenticationOnAccount() {
    setAuthentication("bob");

    BankAccountCreationForm form = BankAccountCreationForm.builder()
        .name("Wallet")
        .initialBalance(BigDecimal.ZERO)
        .build();

    when(service.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

    ResponseEntity<BankAccount> response = controller.save(form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("bob", response.getBody().getUsername());
  }

  @Test
  void findAll_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<Page<BankAccount>> response = controller.findAll(0, 10);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void findAll_returnsPageOfAccounts() {
    setAuthentication("alice");

    BankAccount account = BankAccount.builder()
        .id(1L)
        .name("Checking")
        .balance(BigDecimal.valueOf(200))
        .username("alice")
        .build();
    Page<BankAccount> page = new PageImpl<>(List.of(account));

    when(service.findByUsername("alice", 0, 10)).thenReturn(page);

    ResponseEntity<Page<BankAccount>> response = controller.findAll(0, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getTotalElements());
    assertEquals(account, response.getBody().getContent().getFirst());
  }

  @Test
  void findAll_passesCorrectPageAndSizeToService() {
    setAuthentication("alice");
    when(service.findByUsername("alice", 2, 5)).thenReturn(Page.empty());

    controller.findAll(2, 5);

    verify(service).findByUsername("alice", 2, 5);
  }

  @Test
  void findAll_returnsEmptyPage_whenNoAccounts() {
    setAuthentication("alice");
    when(service.findByUsername("alice", 0, 10)).thenReturn(Page.empty());

    ResponseEntity<Page<BankAccount>> response = controller.findAll(0, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().getTotalElements());
  }

  @Test
  void update_returnsUnauthorized_whenNoAuthentication() {
    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name("New Name")
        .balance(BigDecimal.TEN)
        .build();

    ResponseEntity<BankAccount> response = controller.update(1L, form);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(service);
  }

  @Test
  void update_returnsNotFound_whenAccountDoesNotBelongToUser() {
    setAuthentication("alice");

    when(service.findByIdAndUsername(99L, "alice")).thenReturn(Optional.empty());

    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name("New Name")
        .build();

    ResponseEntity<BankAccount> response = controller.update(99L, form);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(service, never()).update(any());
  }

  @Test
  void update_updatesNameAndBalance_whenBothProvided() {
    setAuthentication("alice");

    BankAccount existing = BankAccount.builder()
        .id(1L)
        .name("Old Name")
        .balance(BigDecimal.valueOf(100))
        .username("alice")
        .build();

    BankAccount updated = BankAccount.builder()
        .id(1L)
        .name("New Name")
        .balance(BigDecimal.valueOf(999))
        .username("alice")
        .build();

    when(service.findByIdAndUsername(1L, "alice")).thenReturn(Optional.of(existing));
    when(service.update(existing)).thenReturn(updated);

    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name("New Name")
        .balance(BigDecimal.valueOf(999))
        .build();

    ResponseEntity<BankAccount> response = controller.update(1L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("New Name", response.getBody().getName());
    assertEquals(BigDecimal.valueOf(999), response.getBody().getBalance());
  }

  @Test
  void update_doesNotChangeName_whenNameIsNull() {
    setAuthentication("alice");

    BankAccount existing = BankAccount.builder()
        .id(1L)
        .name("Original")
        .balance(BigDecimal.valueOf(100))
        .username("alice")
        .build();

    when(service.findByIdAndUsername(1L, "alice")).thenReturn(Optional.of(existing));
    when(service.update(existing)).thenAnswer(inv -> inv.getArgument(0));

    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name(null)
        .balance(BigDecimal.valueOf(250))
        .build();

    ResponseEntity<BankAccount> response = controller.update(1L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Original", response.getBody().getName());
    assertEquals(BigDecimal.valueOf(250), response.getBody().getBalance());
  }

  @Test
  void update_doesNotChangeName_whenNameIsBlank() {
    setAuthentication("alice");

    BankAccount existing = BankAccount.builder()
        .id(1L)
        .name("Original")
        .balance(BigDecimal.valueOf(100))
        .username("alice")
        .build();

    when(service.findByIdAndUsername(1L, "alice")).thenReturn(Optional.of(existing));
    when(service.update(existing)).thenAnswer(inv -> inv.getArgument(0));

    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name("   ")
        .balance(BigDecimal.valueOf(300))
        .build();

    ResponseEntity<BankAccount> response = controller.update(1L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Original", response.getBody().getName());
  }

  @Test
  void update_doesNotChangeBalance_whenBalanceIsNull() {
    setAuthentication("alice");

    BankAccount existing = BankAccount.builder()
        .id(1L)
        .name("Savings")
        .balance(BigDecimal.valueOf(500))
        .username("alice")
        .build();

    when(service.findByIdAndUsername(1L, "alice")).thenReturn(Optional.of(existing));
    when(service.update(existing)).thenAnswer(inv -> inv.getArgument(0));

    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name("Updated Name")
        .balance(null)
        .build();

    ResponseEntity<BankAccount> response = controller.update(1L, form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(BigDecimal.valueOf(500), response.getBody().getBalance());
    assertEquals("Updated Name", response.getBody().getName());
  }

  @Test
  void update_delegatesToService_withModifiedAccount() {
    setAuthentication("alice");

    BankAccount existing = BankAccount.builder()
        .id(1L)
        .name("Old")
        .balance(BigDecimal.ONE)
        .username("alice")
        .build();

    when(service.findByIdAndUsername(1L, "alice")).thenReturn(Optional.of(existing));
    when(service.update(existing)).thenReturn(existing);

    BankAccountUpdateForm form = BankAccountUpdateForm.builder()
        .name("New")
        .balance(BigDecimal.TEN)
        .build();

    controller.update(1L, form);

    verify(service).update(existing);
  }
}

