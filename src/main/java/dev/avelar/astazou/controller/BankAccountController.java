package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.BankAccountCreationForm;
import dev.avelar.astazou.dto.BankAccountUpdateForm;
import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.service.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {

  private final BankAccountService service;

  @Autowired
  public BankAccountController(BankAccountService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<BankAccount> save(@RequestBody BankAccountCreationForm data) {
    var account = data.toModel();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    account.setUsername(auth.getName());

    BankAccount savedAccount = service.save(account);

    return ResponseEntity.ok(savedAccount);
  }

  @GetMapping
  public ResponseEntity<Page<BankAccount>> findAll(@RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int itemsPerPage) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Page<BankAccount> accounts = service.findByUsername(auth.getName(), page, itemsPerPage);

    return ResponseEntity.ok(accounts);
  }

  @PutMapping("/{account_id}")
  public ResponseEntity<BankAccount> update(@PathVariable("account_id") Long accountId,
      @RequestBody BankAccountUpdateForm data) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    var accountOpt = service.findByIdAndUsername(accountId, username);

    if (accountOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    var account = accountOpt.get();

    if (data.getName() != null && !data.getName().isBlank()) {
      account.setName(data.getName());
    }

    if (data.getBalance() != null) {
      account.setBalance(data.getBalance());
    }

    BankAccount updatedAccount = service.update(account);

    return ResponseEntity.ok(updatedAccount);
  }

}
