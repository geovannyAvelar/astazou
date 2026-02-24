package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.CreditCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardRepository extends CrudRepository<CreditCard, Long> {

  Page<CreditCard> findByUsername(String username, Pageable pageable);

  CreditCard findByIdAndUsername(Long id, String username);

}
