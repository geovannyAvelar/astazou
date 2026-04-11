package dev.avelar.astazou.repository;

import dev.avelar.astazou.dto.MonthlySummaryDto;
import dev.avelar.astazou.model.Transaction;
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
        AND ba.currency = :currency
        AND EXTRACT(YEAR FROM t.transaction_date) = :year
        AND EXTRACT(MONTH FROM t.transaction_date) = :month
      """)
  Double calculateIncomeByUsernameAndMonth(@Param("username") String username, @Param("month") Integer month,
      @Param("year") Integer year, @Param("currency") String currency);

  @Query("""
        SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
        AND (t.type = 'debit')
        AND ba.currency = :currency
        AND EXTRACT(YEAR FROM t.transaction_date) = :year
        AND EXTRACT(MONTH FROM t.transaction_date) = :month
      """)
  Double calculateExpenseByUsernameAndMonth(@Param("username") String username, @Param("month") Integer month,
      @Param("year") Integer year, @Param("currency") String currency);

  @Query("""
        SELECT t.* FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
          AND t.transaction_date <= CURRENT_DATE
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

  @Query("""
        SELECT t.* FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
          AND t.bank_account_id = :bankAccountId
          AND (:searchQuery = '' OR t.description_search @@ plainto_tsquery('english', :searchQuery))
          AND t.transaction_date >= :startDate
          AND t.transaction_date <= :endDate
        ORDER BY t.transaction_date DESC
        LIMIT :limit OFFSET :offset
      """)
  List<Transaction> searchTransactions(@Param("username") String username,
      @Param("bankAccountId") Long bankAccountId, @Param("searchQuery") String searchQuery,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
      @Param("limit") int limit, @Param("offset") int offset);

  @Query("""
        SELECT COUNT(t.id) FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
          AND t.bank_account_id = :bankAccountId
          AND (:searchQuery = '' OR t.description_search @@ plainto_tsquery('english', :searchQuery))
          AND t.transaction_date >= :startDate
          AND t.transaction_date <= :endDate
      """)
  Long countSearchTransactions(@Param("username") String username,
      @Param("bankAccountId") Long bankAccountId, @Param("searchQuery") String searchQuery,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query("""
        SELECT EXTRACT(MONTH FROM t.transaction_date) AS month,
               COALESCE(SUM(CASE WHEN t.type = 'credit' THEN t.amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN t.type = 'debit' THEN ABS(t.amount) ELSE 0 END), 0) AS expenses
        FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
          AND ba.currency = :currency
          AND EXTRACT(YEAR FROM t.transaction_date) = :year
          AND (t.type = 'credit' or t.type = 'debit')
        GROUP BY month
        ORDER BY month
      """)
  List<MonthlySummaryDto> getMonthlySummary(String username, int year, String currency);

  @Modifying
  @Query("""
      UPDATE transactions t
      SET tags = :tags::text[]
      FROM bank_account ba
      WHERE t.bank_account_id = ba.id
        AND t.id = :transactionId
        AND ba.username = :username
      """)
  void updateTags(@Param("transactionId") Long transactionId,
      @Param("username") String username,
      @Param("tags") String tags);

  @Query("""
      SELECT DISTINCT UNNEST(t.tags) AS tag
      FROM transactions t
      JOIN bank_account ba ON t.bank_account_id = ba.id
      WHERE ba.username = :username
      ORDER BY tag
      """)
  List<String> findAllTagsByUsername(@Param("username") String username);

  @Modifying
  @Query("""
      UPDATE transactions t
      SET transaction_date = :transactionDate,
          description = :description,
          amount = :amount,
          type = :type
      FROM bank_account ba
      WHERE t.bank_account_id = ba.id
        AND t.id = :transactionId
        AND ba.username = :username
      """)
  void update(@Param("transactionId") Long transactionId,
              @Param("username") String username,
              @Param("transactionDate") java.time.LocalDate transactionDate,
              @Param("description") String description,
              @Param("amount") java.math.BigDecimal amount,
              @Param("type") String type);

}