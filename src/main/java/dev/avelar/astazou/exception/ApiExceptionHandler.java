package dev.avelar.astazou.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final HttpHeaders JSON_HEADERS = new HttpHeaders();

  static {
    JSON_HEADERS.setContentType(MediaType.APPLICATION_JSON);
  }

  private ErrorResponse buildResponse(HttpStatus status, String message, String path, List<String> errors) {
    return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, errors);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
    ErrorResponse body = buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(JSON_HEADERS).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
    ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(JSON_HEADERS).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.toList());

    ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(JSON_HEADERS).body(body);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
    ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request", request.getRequestURI(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(JSON_HEADERS).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    ErrorResponse body = buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "Unexpected error", request.getRequestURI(), null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(JSON_HEADERS).body(body);
  }

  @ExceptionHandler(FeignException.class)
  public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex, HttpServletRequest request) {
    String sanitizedMessage = ex.getMessage();
    if (sanitizedMessage != null) {
      sanitizedMessage = sanitizedMessage.replaceAll("(?i)(client_secret=)[^&\\s]+", "$1[client_secret removed]");
    }
    LOGGER.error("Feign exception: {}", sanitizedMessage);
    ErrorResponse body = buildResponse(HttpStatus.BAD_GATEWAY, "Cannot connect to external service",
        request.getRequestURI(), null);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).headers(JSON_HEADERS).body(body);
  }
}
