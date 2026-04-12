package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.PythonScript;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PythonScriptRepository extends CrudRepository<PythonScript, Long> {

  List<PythonScript> findByUsername(String username);

  Optional<PythonScript> findByIdAndUsername(Long id, String username);

  @Modifying
  @Query("DELETE FROM python_scripts WHERE id = :id AND username = :username")
  void deleteByIdAndUsername(@Param("id") Long id, @Param("username") String username);

}

