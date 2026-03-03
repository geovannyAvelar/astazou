package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.ReportToken;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportTokenRepository extends CrudRepository<ReportToken, Long> {

  @Query("SELECT * FROM report_tokens WHERE token = :token")
  Optional<ReportToken> findByToken(@Param("token") String token);
}

