package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.StockQuote;
import dev.avelar.astazou.model.StockQuoteHistory;
import dev.avelar.astazou.service.StockQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
public class StockQuoteController {

  private static final int STALE_THRESHOLD_MINUTES = 20;

  private final StockQuoteService stockQuoteService;

  /**
   * Returns the latest price for a ticker.
   * If the stored quote is missing or older than 20 minutes, it fetches a fresh one from BrAPI
   * and persists it before returning.
   */
  @GetMapping("/{ticker}")
  public ResponseEntity<StockQuote> getQuote(@PathVariable String ticker) {
    String symbol = ticker.toUpperCase();

    Optional<StockQuote> cached = stockQuoteService.getCurrentQuote(symbol);

    boolean needsRefresh = cached.isEmpty()
        || cached.get().getUpdatedAt() == null
        || cached.get().getUpdatedAt().isBefore(OffsetDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES));

    if (needsRefresh) {
      List<StockQuote> refreshed = stockQuoteService.refreshQuotes(symbol);
      if (refreshed.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }
      return ResponseEntity.ok(refreshed.getFirst());
    }

    return ResponseEntity.ok(cached.get());
  }

  /**
   * Returns the full price history for a ticker (most-recent first).
   */
  @GetMapping("/{ticker}/history")
  public ResponseEntity<List<StockQuoteHistory>> getHistory(@PathVariable String ticker) {
    List<StockQuoteHistory> history = stockQuoteService.getHistory(ticker.toUpperCase());
    return ResponseEntity.ok(history);
  }

}


