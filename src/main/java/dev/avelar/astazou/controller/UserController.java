package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.UserPreferencesForm;
import dev.avelar.astazou.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

  private static final List<String> SUPPORTED_CURRENCIES = List.of(
      "ARS", "AUD", "BRL", "CAD", "CHF", "CLP", "EUR", "GBP",
      "JPY", "MXN", "PEN", "USD", "UYU"
  );

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PutMapping("/me/preferences")
  public ResponseEntity<?> updatePreferences(@RequestBody UserPreferencesForm form) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String currency = form.getPreferredCurrency();
    if (currency == null || currency.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "preferredCurrency is required"));
    }

    if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Unsupported currency: " + currency));
    }

    userService.updatePreferredCurrency(auth.getName(), currency);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/currencies")
  public ResponseEntity<List<String>> getSupportedCurrencies() {
    return ResponseEntity.ok(SUPPORTED_CURRENCIES);
  }

}

