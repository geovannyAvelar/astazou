package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.StockQuote;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockQuoteRepository extends CrudRepository<StockQuote, Long> {

  Optional<StockQuote> findBySymbol(String symbol);

  @Modifying
  @Query("""
      INSERT INTO stock_quote (symbol, short_name, long_name, currency, price, updated_at)
      VALUES (:#{#q.symbol}, :#{#q.shortName}, :#{#q.longName}, :#{#q.currency}, :#{#q.price}, now())
      ON CONFLICT (symbol)
      DO UPDATE SET
          short_name = EXCLUDED.short_name,
          long_name  = EXCLUDED.long_name,
          currency   = EXCLUDED.currency,
          price      = EXCLUDED.price,
          updated_at = now()
      """)
  void upsert(@Param("q") StockQuote quote);

}

