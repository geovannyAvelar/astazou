package dev.avelar.astazou.service;

import dev.avelar.astazou.model.CreditCard;
import dev.avelar.astazou.model.CreditCardTransaction;
import dev.avelar.astazou.repository.CreditCardRepository;
import dev.avelar.astazou.repository.CreditCardTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditCardService {

  private final CreditCardRepository repository;

  private final CreditCardTransactionRepository transactionRepository;

  @Autowired
  public CreditCardService(CreditCardRepository repository, CreditCardTransactionRepository transactionRepository) {
    this.repository = repository;
    this.transactionRepository = transactionRepository;
  }

  public CreditCard save(CreditCard creditCard) {
    return repository.save(creditCard);
  }

  public Page<CreditCard> findByUsername(String username, int page, int itemsPerPage) {
    return repository.findByUsername(username, PageRequest.of(page, itemsPerPage));
  }

  public CreditCard findByIdAndUsername(Long cardId, String username) {
    return repository.findByIdAndUsername(cardId, username);
  }

  public List<CreditCardTransaction> getTransactionsByStatement(Long creditCardId, String username, int month, int year) {
    return transactionRepository.getTransactions(username, creditCardId, month, year);
  }

}
