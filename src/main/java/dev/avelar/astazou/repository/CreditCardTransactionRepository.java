package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.CreditCardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditCardTransactionRepository extends CrudRepository<CreditCardTransaction, String> {

  @Query("""
            SELECT t.*
            FROM credit_card_transactions t
            JOIN credit_cards c ON t.credit_card = c.id
            JOIN users u ON c.username = u.username
            WHERE u.username = :username
              AND c.id = :creditCardId
              AND EXTRACT(MONTH FROM t.statement_date) = :month
              AND EXTRACT(YEAR FROM t.statement_date) = :year
            ORDER BY t.statement_date DESC
          """)
  List<CreditCardTransaction> getTransactions(String username, Long creditCardId, int month, int year);

}
