package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.ReportToken;
import dev.avelar.astazou.repository.ReportTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportValidationControllerTest {

  @Mock
  private ReportTokenRepository reportTokenRepository;

  @InjectMocks
  private ReportValidationController controller;

  @Test
  void validate_returnsNotFound_whenTokenDoesNotExist() {
    when(reportTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

    ResponseEntity<Map<String, Object>> response = controller.validate("unknown-token");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void validate_returnsOk_whenTokenExists() {
    ReportToken token = buildToken();
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(token));

    ResponseEntity<Map<String, Object>> response = controller.validate("abc-123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void validate_responseContainsValidTrue() {
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(buildToken()));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertEquals(true, body.get("valid"));
  }

  @Test
  void validate_responseContainsCorrectUsername() {
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(buildToken()));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertEquals("alice", body.get("username"));
  }

  @Test
  void validate_responseContainsCorrectAccountName() {
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(buildToken()));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertEquals("Savings", body.get("accountName"));
  }

  @Test
  void validate_responseContainsCorrectReportMonth() {
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(buildToken()));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertEquals(3, body.get("reportMonth"));
  }

  @Test
  void validate_responseContainsCorrectReportYear() {
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(buildToken()));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertEquals(2026, body.get("reportYear"));
  }

  @Test
  void validate_responseContainsGeneratedAt_asString() {
    OffsetDateTime createdAt = OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC);
    ReportToken token = ReportToken.builder()
        .id(1L).token("abc-123").username("alice").accountName("Savings")
        .bankAccountId(10L).reportMonth(3).reportYear(2026).createdAt(createdAt).build();
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(token));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertEquals(createdAt.toString(), body.get("generatedAt"));
  }

  @Test
  void validate_delegatesToRepositoryWithCorrectToken() {
    when(reportTokenRepository.findByToken("my-token")).thenReturn(Optional.empty());

    controller.validate("my-token");

    verify(reportTokenRepository).findByToken("my-token");
  }

  @Test
  void validate_responseHasExactlyExpectedKeys() {
    when(reportTokenRepository.findByToken("abc-123")).thenReturn(Optional.of(buildToken()));

    Map<String, Object> body = controller.validate("abc-123").getBody();

    assertNotNull(body);
    assertTrue(body.containsKey("valid"));
    assertTrue(body.containsKey("username"));
    assertTrue(body.containsKey("accountName"));
    assertTrue(body.containsKey("reportMonth"));
    assertTrue(body.containsKey("reportYear"));
    assertTrue(body.containsKey("generatedAt"));
    assertEquals(6, body.size());
  }

  private ReportToken buildToken() {
    return ReportToken.builder()
        .id(1L)
        .token("abc-123")
        .username("alice")
        .accountName("Savings")
        .bankAccountId(10L)
        .reportMonth(3)
        .reportYear(2026)
        .createdAt(OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC))
        .build();
  }
}


