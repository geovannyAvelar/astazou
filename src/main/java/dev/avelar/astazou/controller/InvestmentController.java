package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.InvestmentContributionForm;
import dev.avelar.astazou.dto.InvestmentContributionUpdateForm;
import dev.avelar.astazou.model.InvestmentContribution;
import dev.avelar.astazou.service.InvestmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investments")
public class InvestmentController {

  private final InvestmentService service;

  @Autowired
  public InvestmentController(InvestmentService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<InvestmentContribution> save(@RequestBody InvestmentContributionForm data) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    var contribution = data.toModel();
    contribution.setUsername(auth.getName());
    return ResponseEntity.ok(service.save(contribution));
  }

  @GetMapping
  public ResponseEntity<Page<InvestmentContribution>> findAll(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int itemsPerPage) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(service.findByUsername(auth.getName(), page, itemsPerPage));
  }

  @PutMapping("/{id}")
  public ResponseEntity<InvestmentContribution> update(
      @PathVariable Long id,
      @RequestBody InvestmentContributionUpdateForm data) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var opt = service.findByIdAndUsername(id, auth.getName());
    if (opt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    var contribution = opt.get();

    if (data.getSymbol() != null && !data.getSymbol().isBlank()) {
      contribution.setSymbol(data.getSymbol().toUpperCase().trim());
    }
    if (data.getPurchaseDate() != null) {
      contribution.setPurchaseDate(data.getPurchaseDate());
    }
    if (data.getQuantity() != null) {
      contribution.setQuantity(data.getQuantity());
    }
    if (data.getPurchasePrice() != null) {
      contribution.setPurchasePrice(data.getPurchasePrice());
    }

    return ResponseEntity.ok(service.update(contribution));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    service.delete(id, auth.getName());
    return ResponseEntity.noContent().build();
  }

}

