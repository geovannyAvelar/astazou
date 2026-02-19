package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

  private final TransactionService service;

  @Autowired
  public TransactionController(TransactionService service) {
    this.service = service;
  }

  @GetMapping("/{account_id}")
  public ResponseEntity<Page<Transaction>> getTransactions(@PathVariable("account_id") Long accountId,
      @RequestParam int page, @RequestParam int itemsPerPage) {
    return ResponseEntity.ok(service.findAll(page, itemsPerPage));
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
