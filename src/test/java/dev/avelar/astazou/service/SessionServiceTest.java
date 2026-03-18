package dev.avelar.astazou.service;

import dev.avelar.astazou.model.Session;
import dev.avelar.astazou.model.User;
import dev.avelar.astazou.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

  @Mock private SessionRepository repository;
  @Mock private UserService userService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks
  private SessionService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "expirationTime", 30L);
  }

  @Test
  void findByToken_delegatesToRepository() {
    Session session = Session.builder().token("tok").username("user").build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));

    Optional<Session> result = service.findByToken("tok");

    assertTrue(result.isPresent());
    assertEquals("tok", result.get().getToken());
  }

  @Test
  void findByToken_returnsEmpty_whenNotFound() {
    when(repository.findByToken("missing")).thenReturn(Optional.empty());

    assertTrue(service.findByToken("missing").isEmpty());
  }

  @Test
  void findByValue_delegatesToRepository() {
    Session session = Session.builder().token("tok").username("user").build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));

    Optional<Session> result = service.findByValue("tok");

    assertTrue(result.isPresent());
    verify(repository).findByToken("tok");
  }

  @Test
  void login_throwsBadCredentials_whenUserNotFound() {
    when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThrows(BadCredentialsException.class,
        () -> service.login("unknown", "pass"));
  }

  @Test
  void login_throwsBadCredentials_whenPasswordDoesNotMatch() {
    User user = User.builder().username("user").password("hashed").build();
    when(userService.findByUsername("user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

    assertThrows(BadCredentialsException.class,
        () -> service.login("user", "wrong"));
  }

  @Test
  void login_returnsSession_whenCredentialsAreValid() {
    User user = User.builder().username("user").password("hashed").build();
    Session saved = Session.builder().token("tok").username("user").build();
    when(userService.findByUsername("user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
    when(repository.findByUsername("user")).thenReturn(Optional.empty());
    when(repository.save(any(Session.class))).thenReturn(saved);

    Session result = service.login("user", "pass");

    assertNotNull(result);
    assertEquals("user", result.getUsername());
  }

  @Test
  void login_reusesExistingSession_whenOneAlreadyExists() {
    User user = User.builder().username("user").password("hashed").build();
    Session existing = Session.builder().token("existing-tok").username("user").build();
    when(userService.findByUsername("user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
    when(repository.findByUsername("user")).thenReturn(Optional.of(existing));

    Session result = service.login("user", "pass");

    assertEquals("existing-tok", result.getToken());
    assertEquals(30L, result.getExpiresIn());
    verify(repository, never()).save(any());
  }

  @Test
  void validate_returnsNull_whenTokenNotFound() {
    when(repository.findByToken("bad")).thenReturn(Optional.empty());

    assertNull(service.validate("bad"));
  }

  @Test
  void validate_returnsNull_whenSessionExpired() {
    Session session = Session.builder()
        .token("tok").username("user")
        .expiresAt(OffsetDateTime.now().minusMinutes(1))
        .build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));

    assertNull(service.validate("tok"));
  }

  @Test
  void validate_returnsNull_whenUserNoLongerExists() {
    Session session = Session.builder()
        .token("tok").username("user")
        .expiresAt(OffsetDateTime.now().plusMinutes(10))
        .build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));
    when(userService.findByUsername("user")).thenReturn(Optional.empty());

    assertNull(service.validate("tok"));
  }

  @Test
  void validate_returnsSession_andSetsRolesAndExpiresIn_whenValid() {
    Session session = Session.builder()
        .token("tok").username("user")
        .expiresAt(OffsetDateTime.now().plusMinutes(10))
        .build();
    User user = User.builder().username("user").roles(List.of("ROLE_USER")).build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));
    when(userService.findByUsername("user")).thenReturn(Optional.of(user));

    Session result = service.validate("tok");

    assertNotNull(result);
    assertEquals(List.of("ROLE_USER"), result.getRoles());
    assertEquals(30L, result.getExpiresIn());
  }

  @Test
  void revokeExpiredTokens_callsRepositoryWithCurrentTime() {
    service.revokeExpiredTokens();

    ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(repository).revokeExpiredTokens(captor.capture());
    // captured time should be at most a few seconds in the past
    assertTrue(captor.getValue().isAfter(OffsetDateTime.now().minusSeconds(5)));
  }

  @Test
  void generateToken_reusesSession_whenUsernameAlreadyHasOne() {
    Session existing = Session.builder().token("old-tok").username("user").build();
    when(repository.findByUsername("user")).thenReturn(Optional.of(existing));

    Session result = service.generateToken("user", List.of());

    assertEquals("old-tok", result.getToken());
    assertEquals(30L, result.getExpiresIn());
    verify(repository, never()).save(any());
  }

  @Test
  void generateToken_createsNewSession_whenNoneExists() {
    Session saved = Session.builder().token("new-tok").username("user").build();
    when(repository.findByUsername("user")).thenReturn(Optional.empty());
    when(repository.save(any(Session.class))).thenReturn(saved);

    Session result = service.generateToken("user", List.of("ROLE_USER"));

    assertNotNull(result.getToken());
    assertEquals(30L, result.getExpiresIn());
    verify(repository).save(any(Session.class));
  }

  @Test
  void generateToken_setsExpiresAt_aboutTenMinutesFromNow() {
    when(repository.findByUsername("user")).thenReturn(Optional.empty());
    when(repository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

    Session result = service.generateToken("user", List.of());

    OffsetDateTime expectedExpiry = OffsetDateTime.now().plusMinutes(10);
    // allow ±5 seconds tolerance
    assertTrue(result.getExpiresAt().isAfter(expectedExpiry.minusSeconds(5)));
    assertTrue(result.getExpiresAt().isBefore(expectedExpiry.plusSeconds(5)));
  }

  @Test
  void renew_returnsNull_whenTokenNotFound() {
    when(repository.findByToken("bad")).thenReturn(Optional.empty());

    assertNull(service.renew("bad"));
  }

  @Test
  void renew_returnsNull_whenSessionExpired() {
    Session session = Session.builder()
        .token("tok").username("user")
        .expiresAt(OffsetDateTime.now().minusMinutes(1))
        .build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));

    assertNull(service.renew("tok"));
    verify(repository, never()).save(any());
  }

  @Test
  void renew_updatesExpiresAtAndExpiresIn_whenValid() {
    Session session = Session.builder()
        .token("tok").username("user")
        .expiresAt(OffsetDateTime.now().plusMinutes(5))
        .build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));

    Session result = service.renew("tok");

    assertNotNull(result);
    assertEquals(30L, result.getExpiresIn());
    assertTrue(result.getExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(29)));
    verify(repository).save(session);
  }

  @Test
  void renew_marksSessionAsNotNew_beforeSaving() {
    Session session = Session.builder()
        .token("tok").username("user")
        .expiresAt(OffsetDateTime.now().plusMinutes(5))
        .build();
    when(repository.findByToken("tok")).thenReturn(Optional.of(session));

    service.renew("tok");

    assertFalse(session.isNew());
  }

  @Test
  void revoke_callsRepositoryDelete() {
    Session session = Session.builder().token("tok").username("user").build();

    service.revoke(session);

    verify(repository).delete(session);
  }

  @Test
  void generateRandomString_returns32CharHexString() {
    String result = service.generateRandomString();

    assertNotNull(result);
    assertEquals(32, result.length());
    assertTrue(result.matches("[0-9a-f]{32}"), "Should be lowercase hex: " + result);
  }

  @Test
  void generateRandomString_returnsDifferentValueEachCall() {
    String first  = service.generateRandomString();
    String second = service.generateRandomString();

    assertNotEquals(first, second);
  }
}

