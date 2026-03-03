package dev.avelar.astazou.repository;

import dev.avelar.astazou.model.StockQuoteHistory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQuoteHistoryRepository extends CrudRepository<StockQuoteHistory, Long> {

  @Query("""
      SELECT * FROM stock_quote_history
      WHERE symbol = :symbol
      ORDER BY recorded_at DESC
      """)
  List<StockQuoteHistory> findBySymbolOrderByRecordedAtDesc(@Param("symbol") String symbol);

}

