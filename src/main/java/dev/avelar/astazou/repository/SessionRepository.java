package dev.avelar.astazou.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import dev.avelar.astazou.model.Session;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SessionRepository extends CrudRepository<Session, Long> {

  Optional<Session> findByToken(String token);

  Optional<Session> findByUsername(String username);

  @Transactional
  @Modifying
  @Query("DELETE FROM \"sessions\" WHERE expires_at is not null and expires_at < :now")
  void revokeExpiredTokens(OffsetDateTime now);

}