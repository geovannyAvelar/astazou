package dev.avelar.astazou.controller;

import dev.avelar.astazou.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

  private final BankAccountRepository bankAccountRepository;

  @Autowired
  public UserController(BankAccountRepository bankAccountRepository) {
    this.bankAccountRepository = bankAccountRepository;
  }

  /** Returns the distinct currencies used across the authenticated user's bank accounts. */
  @GetMapping("/currencies")
  public ResponseEntity<List<String>> getAccountCurrencies() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<String> currencies = bankAccountRepository.findDistinctCurrenciesByUsername(auth.getName());
    if (currencies.isEmpty()) {
      currencies = List.of("BRL");
    }
    return ResponseEntity.ok(currencies);
  }

}
