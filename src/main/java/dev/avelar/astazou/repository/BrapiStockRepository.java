package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.BrapiStock;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BrapiStockRepository extends CrudRepository<BrapiStock, Long> {

  List<BrapiStock> findAll();

  @Modifying
  @Transactional
  @Query("""
      INSERT INTO brapi_stock (ticker, name, sector, logo_url, synced_at)
      VALUES (:#{#s.ticker}, :#{#s.name}, :#{#s.sector}, :#{#s.logoUrl}, now())
      ON CONFLICT (ticker)
      DO UPDATE SET
          name      = EXCLUDED.name,
          sector    = EXCLUDED.sector,
          logo_url  = EXCLUDED.logo_url,
          synced_at = now()
      """)
  void upsert(@Param("s") BrapiStock stock);

}

