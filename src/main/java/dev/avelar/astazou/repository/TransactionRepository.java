package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
          page = EXCLUDED.page;
      
      UPDATE bank_account acc
          SET acc.balance = acc.balance + :#{#transaction.amount}
          WHERE acc.id = :#{#transaction.bankAccountId};
      """)
  void upsert(@Param("transaction") Transaction transaction);

  Page<Transaction> findByBankAccountId(Long bankAccountId, Pageable pageable);

  @Query("""
        SELECT t.* FROM transactions t
        JOIN bank_account ba ON t.bank_account_id = ba.id
        WHERE ba.username = :username
        ORDER BY t.transaction_date DESC
        LIMIT 10
      """)
  List<Transaction> findLast10(String username);

}