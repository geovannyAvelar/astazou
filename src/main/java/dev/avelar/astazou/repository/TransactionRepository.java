package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {

  Page<Transaction> findAll(Pageable pageable);

}
