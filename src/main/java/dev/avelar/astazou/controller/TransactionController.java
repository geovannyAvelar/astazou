package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.Balance;
import dev.avelar.astazou.dto.TransactionCreationForm;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

  private final TransactionService service;

  @Autowired
  public TransactionController(TransactionService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<Void> create(@RequestBody TransactionCreationForm data) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var transaction = data.toModel();
    service.save(transaction, auth.getName());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{account_id}")
  public ResponseEntity<Page<Transaction>> getTransactions(
      @PathVariable("account_id") Long accountId,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int itemsPerPage,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year) {
    var now = OffsetDateTime.now();

    if (month == null || month < 0 || month >= 12) {
      month = now.getMonthValue();
    }

    if (year == null) {
      year = now.getYear();
    }

    return ResponseEntity.ok(service.findByAccountIdAndMonth(accountId, month, year, page, itemsPerPage));
  }

  @GetMapping("/balance")
  public ResponseEntity<Balance> calculateBalanceMonth(@RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var now = OffsetDateTime.now();

    if (month == null || month < 0 || month >= 12) {
      month = now.getMonthValue();
    }

    if (year == null) {
      year = now.getYear();
    }

    return ResponseEntity.ok(service.calculateMonthBalance(auth.getName(), month, year));
  }

  @GetMapping("/last")
  public ResponseEntity<Page<Transaction>> getLastTransactions() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(service.findLast10(auth.getName()));
  }

  @PostMapping(path = "/itau/{account_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> parseItauCsv(@RequestParam("file") MultipartFile file,
                                           @PathVariable("account_id") Long accountId) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).build();
    }
    String username = auth.getName();

    try {
      File tmp = File.createTempFile("itau-", ".pdf");
      file.transferTo(tmp);
      service.save(tmp, username, accountId);
      tmp.delete();
      return ResponseEntity.accepted().build();
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

}
