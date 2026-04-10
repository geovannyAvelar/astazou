package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends CrudRepository<BankAccount, Long> {


  @Query("""
      UPDATE bank_account
      SET balance = balance + :value
      WHERE id = :accountId
      """)
  void changeBalance(Long accountId, BigDecimal value);

  Page<BankAccount> findByUsername(String username, Pageable pageable);

  @Query("""
      SELECT * FROM bank_account
      WHERE id = :id AND username = :username
      """)
  Optional<BankAccount> findByIdAndUsername(Long id, String username);

  @Query("SELECT DISTINCT currency FROM bank_account WHERE username = :username ORDER BY currency")
  List<String> findDistinctCurrenciesByUsername(String username);

}
