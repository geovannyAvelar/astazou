package dev.avelar.astazou.service;

import dev.avelar.astazou.model.BankAccount;
import dev.avelar.astazou.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class BankAccountService {

  private final BankAccountRepository repository;

  @Autowired
  public BankAccountService(BankAccountRepository repository) {
    this.repository = repository;
  }

  public BankAccount save(BankAccount account) {
    return repository.save(account);
  }

  public Page<BankAccount> findByUsername(String username, int page, int itemsPerPage) {
    return repository.findByUsername(username, PageRequest.of(page, itemsPerPage));
  }

}
