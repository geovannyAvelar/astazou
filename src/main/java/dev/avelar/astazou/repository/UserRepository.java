package dev.avelar.astazou.repository;

import java.util.Optional;

import dev.avelar.astazou.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, String> {

  Optional<User> findByUsername(String username);

}
