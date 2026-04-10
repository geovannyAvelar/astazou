package dev.avelar.astazou.controller;

import dev.avelar.astazou.repository.BankAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private BankAccountRepository bankAccountRepository;

  @InjectMocks
  private UserController controller;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(String username) {
    Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  // ── getAccountCurrencies ──────────────────────────────────────────────────

  @Test
  void getAccountCurrencies_returnsUnauthorized_whenNoAuthentication() {
    ResponseEntity<List<String>> response = controller.getAccountCurrencies();

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(bankAccountRepository);
  }

  @Test
  void getAccountCurrencies_returnsUserCurrencies() {
    setAuthentication("alice");
    when(bankAccountRepository.findDistinctCurrenciesByUsername("alice"))
        .thenReturn(List.of("BRL", "USD"));

    ResponseEntity<List<String>> response = controller.getAccountCurrencies();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(List.of("BRL", "USD"), response.getBody());
  }

  @Test
  void getAccountCurrencies_fallsBackToBRL_whenNoCurrenciesFound() {
    setAuthentication("alice");
    when(bankAccountRepository.findDistinctCurrenciesByUsername("alice"))
        .thenReturn(List.of());

    ResponseEntity<List<String>> response = controller.getAccountCurrencies();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(List.of("BRL"), response.getBody());
  }

  @Test
  void getAccountCurrencies_delegatesToRepositoryWithAuthenticatedUsername() {
    setAuthentication("bob");
    when(bankAccountRepository.findDistinctCurrenciesByUsername("bob"))
        .thenReturn(List.of("EUR"));

    controller.getAccountCurrencies();

    verify(bankAccountRepository).findDistinctCurrenciesByUsername("bob");
  }
}
