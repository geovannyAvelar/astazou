package dev.avelar.astazou.service;

import java.util.Optional;

import dev.avelar.astazou.model.User;
import dev.avelar.astazou.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository repository;

  @Autowired
  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public Optional<User> findByUsername(String username) {
    return repository.findById(username);
  }

}