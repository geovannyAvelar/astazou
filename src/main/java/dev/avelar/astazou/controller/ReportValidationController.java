package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.ReportToken;
import dev.avelar.astazou.repository.ReportTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reports")
public class ReportValidationController {

  private final ReportTokenRepository reportTokenRepository;

  @Autowired
  public ReportValidationController(ReportTokenRepository reportTokenRepository) {
    this.reportTokenRepository = reportTokenRepository;
  }

  /**
   * Public endpoint to validate a report token.
   * Returns report metadata if the token is valid, or 404 if not found.
   */
  @GetMapping("/validate/{token}")
  public ResponseEntity<Map<String, Object>> validate(@PathVariable("token") String token) {
    Optional<ReportToken> opt = reportTokenRepository.findByToken(token);

    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    ReportToken reportToken = opt.get();

    Map<String, Object> response = Map.of(
        "valid", true,
        "username", reportToken.getUsername(),
        "accountName", reportToken.getAccountName(),
        "reportMonth", reportToken.getReportMonth(),
        "reportYear", reportToken.getReportYear(),
        "generatedAt", reportToken.getCreatedAt().toString()
    );

    return ResponseEntity.ok(response);
  }
}

