package dev.avelar.astazou.service;

import dev.avelar.astazou.exception.NotFoundException;
import dev.avelar.astazou.model.InvestmentContribution;
import dev.avelar.astazou.repository.InvestmentContributionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InvestmentService {

  private final InvestmentContributionRepository repository;

  @Autowired
  public InvestmentService(InvestmentContributionRepository repository) {
    this.repository = repository;
  }

  public InvestmentContribution save(InvestmentContribution contribution) {
    return repository.save(contribution);
  }

  public Page<InvestmentContribution> findByUsername(String username, int page, int itemsPerPage) {
    return repository.findByUsername(
        username,
        PageRequest.of(page, itemsPerPage, Sort.by(Sort.Direction.DESC, "id"))
    );
  }

  public Optional<InvestmentContribution> findByIdAndUsername(Long id, String username) {
    return repository.findByIdAndUsername(id, username);
  }

  public InvestmentContribution update(InvestmentContribution contribution) {
    return repository.save(contribution);
  }

  public void delete(Long id, String username) {
    var opt = repository.findByIdAndUsername(id, username);
    if (opt.isEmpty()) {
      throw new NotFoundException("Investment contribution not found");
    }
    repository.deleteByIdAndUsername(id, username);
  }

}

