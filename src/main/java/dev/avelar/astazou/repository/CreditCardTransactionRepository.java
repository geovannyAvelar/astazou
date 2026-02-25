package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.CreditCardTransaction;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

  @Modifying
  @Query("""
            INSERT INTO credit_card_transactions
            (id, amount, description, credit_card, statement_date, transaction_date, created_at)
            VALUES (:id, :amount, :description, :creditCardId, :statementDate, :transactionDate, :createdAt)
            ON CONFLICT (id) DO NOTHING
          """)
  void insertTransaction(String id, BigDecimal amount, String description, Long creditCardId,
                         LocalDate statementDate, OffsetDateTime transactionDate, OffsetDateTime createdAt);

}
