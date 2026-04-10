package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.Balance;
import dev.avelar.astazou.dto.BalanceByCurrencyDto;
import dev.avelar.astazou.dto.MonthlySummaryDto;
import dev.avelar.astazou.dto.TransactionCreationForm;
import dev.avelar.astazou.dto.TransactionUpdateForm;
import dev.avelar.astazou.dto.TransformToTransferForm;
import dev.avelar.astazou.dto.UpdateTagsForm;
import dev.avelar.astazou.model.Transaction;
import dev.avelar.astazou.service.TransactionService;
import dev.avelar.jambock.reports.ReportGenerationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
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
    service.save(transaction, auth.getName(), data.getUpdateAccount());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{account_id}")
  public ResponseEntity<Page<Transaction>> getTransactions(@PathVariable("account_id") Long accountId,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int itemsPerPage,
      @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
    var now = OffsetDateTime.now();

    if (month == null || month < 0 || month > 12) {
      month = now.getMonthValue();
    }

    if (year == null) {
      year = now.getYear();
    }

    return ResponseEntity.ok(service.findByAccountIdAndMonth(accountId, month, year, page, itemsPerPage));
  }

  @DeleteMapping("/{transaction_id}")
  public ResponseEntity<Void> delete(@PathVariable("transaction_id") Long transactionId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    service.delete(transactionId, username);

    return ResponseEntity.ok().build();
  }

  @PutMapping("/{transaction_id}")
  public ResponseEntity<Void> update(@PathVariable("transaction_id") Long transactionId,
      @RequestBody TransactionUpdateForm data) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    try {
      service.update(transactionId, username, data.getTransactionDate(), data.getDescription(),
          data.getAmount(), data.getType());
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping("/{transaction_id}/transform-to-transfer")
  public ResponseEntity<Void> transformToTransfer(@PathVariable("transaction_id") Long transactionId,
      @RequestBody TransformToTransferForm data) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    try {
      service.transformToTransfer(transactionId, data.getDestinationAccountId(), username);
      return ResponseEntity.ok().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/balance")
  public ResponseEntity<java.util.List<BalanceByCurrencyDto>> calculateBalanceMonth(
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var now = OffsetDateTime.now();

    if (month == null || month < 1 || month > 12) {
      month = now.getMonthValue();
    }

    if (year == null) {
      year = now.getYear();
    }

    return ResponseEntity.ok(service.calculateAllCurrencyBalances(auth.getName(), month, year));
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
      @PathVariable("account_id") Long accountId,
      @RequestParam(required = false, defaultValue = "false") boolean updateAccount) {
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
      service.save(tmp, username, accountId, updateAccount);
      tmp.delete();
      return ResponseEntity.accepted().build();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @PostMapping(path = "/ofx/{account_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> parseOfxFile(@RequestParam("file") MultipartFile file,
      @PathVariable("account_id") Long accountId,
      @RequestParam(required = false, defaultValue = "false") boolean updateAccount) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).build();
    }
    String username = auth.getName();

    try {
      File tmp = File.createTempFile("ofx-", ".ofx");
      file.transferTo(tmp);
      service.saveOfx(tmp, username, accountId, updateAccount);
      //noinspection ResultOfMethodCallIgnored
      tmp.delete();
      return ResponseEntity.accepted().build();
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/search/{account_id}")
  public ResponseEntity<Page<Transaction>> search(@PathVariable("account_id") Long accountId,
      @RequestParam(required = false, defaultValue = "") String query,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int itemsPerPage) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = auth.getName();

    // Default date range: last 3 months if not specified
    if (endDate == null) {
      endDate = LocalDate.now();
    }
    if (startDate == null) {
      startDate = endDate.minusMonths(3);
    }

    return ResponseEntity.ok(service.search(username, accountId, query, startDate, endDate, page, itemsPerPage));
  }

  @GetMapping("/summary")
  public ResponseEntity<java.util.Map<String, java.util.List<MonthlySummaryDto>>> getMonthlySummary(
      @RequestParam(required = false) Integer year) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var now = OffsetDateTime.now();

    if (year == null) {
      year = now.getYear();
    }

    return ResponseEntity.ok(service.getAllCurrencySummaries(auth.getName(), year));
  }

  @GetMapping(value = "/report/{account_id}", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateMonthlyReport(
      @PathVariable("account_id") Long accountId,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false, defaultValue = "en") String lang) throws ReportGenerationException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var now = OffsetDateTime.now();

    if (month == null || month < 1 || month > 12) {
      month = now.getMonthValue();
    }

    if (year == null) {
      year = now.getYear();
    }

    byte[] pdf = service.generateMonthlyReport(auth.getName(), accountId, month, year, lang);

    String filename = String.format("transactions-%d-%02d.pdf", year, month);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(filename).build());
    headers.setContentLength(pdf.length);

    return ResponseEntity.ok().headers(headers).body(pdf);
  }

  @PutMapping("/{transaction_id}/tags")
  public ResponseEntity<Void> updateTags(@PathVariable("transaction_id") Long transactionId,
      @RequestBody UpdateTagsForm form) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      service.updateTags(transactionId, form.getTags(), auth.getName());
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/tags")
  public ResponseEntity<java.util.List<String>> getAllTags() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(service.findAllTags(auth.getName()));
  }

}
