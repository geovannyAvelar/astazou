package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.CreditCardCreationForm;
import dev.avelar.astazou.model.CreditCard;
import dev.avelar.astazou.model.CreditCardTransaction;
import dev.avelar.astazou.service.CreditCardService;
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
import java.util.List;

@RestController
@RequestMapping("/credit-cards")
public class CreditCardController {

  private final CreditCardService service;

  @Autowired
  public CreditCardController(CreditCardService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<Void> save(@RequestBody CreditCardCreationForm form) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    CreditCard creditCard = form.toModel();
    creditCard.setUsername(username);

    service.save(creditCard);

    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<Page<CreditCard>> find(@RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int itemsPerPage) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    return ResponseEntity.ok(service.findByUsername(username, page, itemsPerPage));
  }

  @GetMapping("/{cardId}")
  public ResponseEntity<CreditCard> findById(@PathVariable Long cardId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    var card = service.findByIdAndUsername(cardId, username);

    if (card == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(card);
  }

  @GetMapping("/transactions/{cardId}/{month}/{year}")
  public ResponseEntity<List<CreditCardTransaction>> findTransactions(@PathVariable Long cardId,
      @PathVariable Integer month, @PathVariable Integer year) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    var transactions = service.getTransactionsByStatement(cardId, username, month, year);

    return ResponseEntity.ok(transactions);
  }

  @PostMapping(path = "/ofx/{cardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> parseOfxFile(@RequestParam("file") MultipartFile file,
      @PathVariable("cardId") Long cardId) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).build();
    }

    String username = auth.getName();

    // Verify the card belongs to the user
    CreditCard card = service.findByIdAndUsername(cardId, username);
    if (card == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    try {
      File tmp = File.createTempFile("ofx-", ".txt");
      file.transferTo(tmp);
      service.parseAndSaveOfxFile(tmp, cardId);
      //noinspection ResultOfMethodCallIgnored
      tmp.delete();
      return ResponseEntity.accepted().build();
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @DeleteMapping("/transactions/{transactionId}")
  public ResponseEntity<Void> deleteTransaction(@PathVariable String transactionId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    try {
      service.deleteTransaction(transactionId, username);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

}
