package dev.avelar.astazou.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import dev.avelar.astazou.model.Session;
import dev.avelar.astazou.model.User;
import dev.avelar.astazou.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

  @Value("${astazou.auth.session.expiration-time}")
  private Long expirationTime;

  private final SessionRepository repository;

  private final UserService service;

  private final PasswordEncoder passwordEncoder;

  @Autowired
  public SessionService(SessionRepository repository, UserService service, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.service = service;
    this.passwordEncoder = passwordEncoder;
  }

  public Optional<Session> findByToken(String token) {
    return repository.findByToken(token);
  }

  public Session login(String username, String password) {
    Optional<User> maybeUser = service.findByUsername(username);
    if (maybeUser.isEmpty()) {
      throw new BadCredentialsException("Invalid credentials");
    }

    User user = maybeUser.get();

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    return generateToken(user.getUsername(), new LinkedList<>());
  }

  public Session validate(String tokenValue) {
    Optional<Session> opt = repository.findByToken(tokenValue);
    if (opt.isEmpty()) {
      return null;
    }

    Session session = opt.get();

    if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
      return null;
    }

    Optional<User> userOpt = service.findByUsername(session.getUsername());

    if (userOpt.isEmpty()) {
      return null;
    }

    User user = userOpt.get();
    session.setRoles(user.getRoles());
    session.setExpiresIn(expirationTime);

    return session;
  }

  public void revokeExpiredTokens() {
    repository.revokeExpiredTokens(OffsetDateTime.now());
  }

  protected Session generateToken(String username, List<String> roles) {
    Optional<Session> maybeToken = repository.findByUsername(username);
    if (maybeToken.isPresent()) {
      Session session = maybeToken.get();
      session.setExpiresIn(expirationTime);
      return session;
    }

    Session session = new Session();
    session.setUsername(username);
    session.setToken(generateRandomString());

    var now = OffsetDateTime.now();
    var expiresAt = now.plusMinutes(10);

    session.setExpiresAt(expiresAt);
    session = repository.save(session);
    session.setExpiresIn(expirationTime);

    return session;
  }

  public Optional<Session> findByValue(String value) {
    return repository.findByToken(value);
  }

  public Session renew(String tokenValue) {
    Optional<Session> opt = repository.findByToken(tokenValue);
    if (opt.isEmpty()) {
      return null;
    }

    Session session = opt.get();

    if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
      return null;
    }

    session.setExpiresAt(OffsetDateTime.now().plusMinutes(expirationTime));
    session.setExpiresIn(expirationTime);
    session.markNotNew();
    repository.save(session);

    return session;
  }

  public void revoke(Session token) {
    repository.delete(token);
  }

  protected String generateRandomString() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    StringBuilder sb = new StringBuilder(32);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

}
