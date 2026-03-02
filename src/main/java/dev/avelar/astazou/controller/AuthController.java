package dev.avelar.astazou.controller;

import java.time.Duration;
import java.util.Optional;

import dev.avelar.astazou.dto.LoginResponse;
import dev.avelar.astazou.model.Session;
import dev.avelar.astazou.model.User;
import dev.avelar.astazou.service.SessionService;
import dev.avelar.astazou.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
public class AuthController {

  private final SessionService service;

  private final UserService userService;

  @Autowired
  public AuthController(SessionService service, UserService userService) {
    this.service = service;
    this.userService = userService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<LoginResponse> login(@RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret) {
    Session session = service.login(clientId, clientSecret);

    ResponseCookie cookie = ResponseCookie.from("SESSION", session.getToken())
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofMinutes(session.getExpiresIn()))
        .sameSite("Strict")
        .build();

    var res = LoginResponse.builder()
        .authenticated(true)
        .username(session.getUsername())
        .scope("read write")
        .accessToken(session.getToken())
        .expiresAt(session.getExpiresAt())
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(res);
  }

  @GetMapping("/validate")
  public ResponseEntity<LoginResponse> validate(
      @CookieValue(value = "SESSION", required = false) String sessionToken) {
    if (sessionToken == null || sessionToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Session session = service.validate(sessionToken);

    if (session == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Optional<User> opt = userService.findByUsername(session.getUsername());

    if (opt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    User user = opt.get();

    var res = LoginResponse.builder()
        .authenticated(true)
        .completeUsername(user.getName())
        .username(session.getUsername())
        .email(user.getEmail())
        .scope("read write")
        .accessToken(session.getToken())
        .expiresAt(session.getExpiresAt())
        .build();

    return ResponseEntity.ok(res);
  }

  @PostMapping("/renew")
  public ResponseEntity<?> renew(
      @CookieValue(value = "SESSION", required = false) String sessionToken) {
    if (sessionToken == null || sessionToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Session session = service.renew(sessionToken);

    if (session == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    ResponseCookie cookie = ResponseCookie.from("SESSION", session.getToken())
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofMinutes(session.getExpiresIn()))
        .sameSite("Strict")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .build();
  }

  @PostMapping("/revoke")
  public ResponseEntity<?> revoke(
      @CookieValue(value = "SESSION", required = false) String sessionToken) {
    if (sessionToken == null || sessionToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    service.findByValue(sessionToken).ifPresent(service::revoke);

    ResponseCookie deleteCookie = ResponseCookie.from("SESSION", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("Strict")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
        .build();
  }

}
