package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.InvestmentContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestmentContributionRepository extends CrudRepository<InvestmentContribution, Long> {

  Page<InvestmentContribution> findByUsername(String username, Pageable pageable);

  @Query("""
      SELECT * FROM investment_contribution
      WHERE id = :id AND username = :username
      """)
  Optional<InvestmentContribution> findByIdAndUsername(Long id, String username);

  @Modifying
  @Query("""
      DELETE FROM investment_contribution
      WHERE id = :id AND username = :username
      """)
  void deleteByIdAndUsername(Long id, String username);

}

