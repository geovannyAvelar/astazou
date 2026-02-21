package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankAccountRepository extends CrudRepository<BankAccount, Long> {

  Page<BankAccount> findByUsername(String username, Pageable pageable);

}
