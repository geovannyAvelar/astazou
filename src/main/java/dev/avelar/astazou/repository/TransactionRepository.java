package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO transactions (transaction_date, description, amount, type, page, sequence, bank_account_id, created_at)
      VALUES (:#{#transaction.transactionDate}, :#{#transaction.description}, :#{#transaction.amount}, 
              :#{#transaction.type}, :#{#transaction.page}, :#{#transaction.sequence}, 
              :#{#transaction.bankAccountId}, COALESCE(:#{#transaction.createdAt}, now()))
      ON CONFLICT (transaction_date, bank_account_id, sequence)
      DO UPDATE SET
          description = EXCLUDED.description,
          amount = EXCLUDED.amount,
          type = EXCLUDED.type,
          page = EXCLUDED.page
      """)
  void upsert(@Param("transaction") Transaction transaction);

  @Modifying
  @Query(value = """
     UPDATE bank_account
     SET balance = balance + :amount
     WHERE id = :accountId
    """)
  void updateBankAccountBalance(@Param("amount") Double amount, @Param("accountId") Long accountId);

  Page<Transaction> findByBankAccountId(Long bankAccountId, Pageable pageable);

  @Query("""
        SELECT t.* FROM transactions t
        WHERE t.bank_account_id = :bankAccountId
        AND EXTRACT(YEAR FROM t.transaction_date) = :year
        AND EXTRACT(MONTH FROM t.transaction_date) = :month
        ORDER BY t.transaction_date DESC
        LIMIT :limit OFFSET :offset
      """)
  List<Transaction> findByAccountIdAndMonth(@Param("bankAccountId") Long bankAccountId, @Param("month") Integer month,
      @Param("year") Integer year, @Param("limit") int limit, @Param("offset") int offset);

  @Query("""
        SELECT COUNT(t.id) FROM transactions t
        WHERE t.bank_account_id = :bankAccountId
        AND EXTRACT(YEAR FROM t.transaction_date) = :year
        AND EXTRACT(MONTH FROM t.transaction_date) = :month
      """)
  Long countByAccountIdAndMonth(@Param("bankAccountId") Long bankAccountId, @Param("month") Integer month,
      @Param("year") Integer year);

  @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
        AND t.type = 'credit'
        AND EXTRACT(YEAR FROM t.transaction_date) = :year
        AND EXTRACT(MONTH FROM t.transaction_date) = :month
      """)
  Double calculateIncomeByUsernameAndMonth(@Param("username") String username, @Param("month") Integer month,
      @Param("year") Integer year);

  @Query("""
        SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
        AND t.type = 'debit' or t.type = 'transfer'
        AND EXTRACT(YEAR FROM t.transaction_date) = :year
        AND EXTRACT(MONTH FROM t.transaction_date) = :month
      """)
  Double calculateExpenseByUsernameAndMonth(@Param("username") String username, @Param("month") Integer month,
      @Param("year") Integer year);

  @Query("""
        SELECT t.* FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
        ORDER BY t.transaction_date DESC
        LIMIT 10
      """)
  List<Transaction> findLast10(String username);

  @Query("""
      SELECT COALESCE(MAX(t.sequence), 0) 
      FROM transactions t
      WHERE t.bank_account_id = :accountId AND t.transaction_date = :day
      """)
  int getLastDaySequence(Long accountId, LocalDate day);

  @Modifying
  @Query("""
    DELETE FROM transactions t 
    USING bank_account ba 
    WHERE t.bank_account_id = ba.id AND 
            t.id = :transactionId AND 
                ba.username = :username
    """)
  void delete(Long transactionId, String username);

}