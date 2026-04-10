package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.UserPreferencesForm;
import dev.avelar.astazou.service.UserService;
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
  private UserService userService;

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

  // ── updatePreferences ─────────────────────────────────────────────────────

  @Test
  void updatePreferences_returnsUnauthorized_whenNoAuthentication() {
    UserPreferencesForm form = new UserPreferencesForm("BRL");

    ResponseEntity<?> response = controller.updatePreferences(form);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(userService);
  }

  @Test
  void updatePreferences_returnsBadRequest_whenCurrencyIsNull() {
    setAuthentication("alice");
    UserPreferencesForm form = new UserPreferencesForm(null);

    ResponseEntity<?> response = controller.updatePreferences(form);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(userService);
  }

  @Test
  void updatePreferences_returnsBadRequest_whenCurrencyIsBlank() {
    setAuthentication("alice");
    UserPreferencesForm form = new UserPreferencesForm("   ");

    ResponseEntity<?> response = controller.updatePreferences(form);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(userService);
  }

  @Test
  void updatePreferences_returnsBadRequest_whenCurrencyIsUnsupported() {
    setAuthentication("alice");
    UserPreferencesForm form = new UserPreferencesForm("XYZ");

    ResponseEntity<?> response = controller.updatePreferences(form);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(userService);
  }

  @Test
  void updatePreferences_returnsOk_whenCurrencyIsValid() {
    setAuthentication("alice");
    UserPreferencesForm form = new UserPreferencesForm("USD");

    ResponseEntity<?> response = controller.updatePreferences(form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(userService).updatePreferredCurrency("alice", "USD");
  }

  @Test
  void updatePreferences_acceptsLowercaseCurrency_andDelegatesToService() {
    setAuthentication("alice");
    UserPreferencesForm form = new UserPreferencesForm("brl");

    ResponseEntity<?> response = controller.updatePreferences(form);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(userService).updatePreferredCurrency("alice", "brl");
  }

  @Test
  void updatePreferences_delegatesToService_withAuthenticatedUsername() {
    setAuthentication("bob");
    UserPreferencesForm form = new UserPreferencesForm("EUR");

    controller.updatePreferences(form);

    verify(userService).updatePreferredCurrency("bob", "EUR");
  }

  @Test
  void updatePreferences_returnsOk_forEachSupportedCurrency() {
    setAuthentication("alice");

    List<String> supported = List.of(
        "ARS", "AUD", "BRL", "CAD", "CHF", "CLP", "EUR", "GBP", "JPY", "MXN", "PEN", "USD", "UYU"
    );

    for (String currency : supported) {
      UserPreferencesForm form = new UserPreferencesForm(currency);
      ResponseEntity<?> response = controller.updatePreferences(form);
      assertEquals(HttpStatus.OK, response.getStatusCode(),
          "Expected OK for currency: " + currency);
    }

    verify(userService, times(supported.size())).updatePreferredCurrency(eq("alice"), anyString());
  }

  // ── getSupportedCurrencies ────────────────────────────────────────────────

  @Test
  void getSupportedCurrencies_returnsOk() {
    ResponseEntity<List<String>> response = controller.getSupportedCurrencies();

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getSupportedCurrencies_returnsNonEmptyList() {
    ResponseEntity<List<String>> response = controller.getSupportedCurrencies();

    assertNotNull(response.getBody());
    assertFalse(response.getBody().isEmpty());
  }

  @Test
  void getSupportedCurrencies_containsExpectedCurrencies() {
    ResponseEntity<List<String>> response = controller.getSupportedCurrencies();

    List<String> currencies = response.getBody();
    assertNotNull(currencies);
    assertTrue(currencies.contains("BRL"));
    assertTrue(currencies.contains("USD"));
    assertTrue(currencies.contains("EUR"));
  }

  @Test
  void getSupportedCurrencies_returns13Currencies() {
    ResponseEntity<List<String>> response = controller.getSupportedCurrencies();

    assertNotNull(response.getBody());
    assertEquals(13, response.getBody().size());
  }
}

