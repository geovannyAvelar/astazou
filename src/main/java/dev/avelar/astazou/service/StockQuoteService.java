package dev.avelar.astazou.service;

import dev.avelar.astazou.brapi.BrapiService;
import dev.avelar.astazou.brapi.dto.Quote;
import dev.avelar.astazou.model.StockQuote;
import dev.avelar.astazou.model.StockQuoteHistory;
import dev.avelar.astazou.repository.StockQuoteHistoryRepository;
import dev.avelar.astazou.repository.StockQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockQuoteService {

  private final BrapiService brapiService;
  private final StockQuoteRepository stockQuoteRepository;
  private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

  /**
   * Fetches fresh quotes from BrAPI for the given tickers, upserts the current snapshot
   * in {@code stock_quote} and appends a new row to {@code stock_quote_history}.
   */
  @Transactional
  public List<StockQuote> refreshQuotes(String... tickers) {
    List<Quote> quotes = brapiService.findAssetByTicker(tickers);

    return quotes.stream()
        .filter(q -> q.getRegularMarketPrice() != null)
        .map(this::persist)
        .toList();
  }

  /**
   * Returns the latest stored snapshot for the given symbol.
   */
  public Optional<StockQuote> getCurrentQuote(String symbol) {
    return stockQuoteRepository.findBySymbol(symbol.toUpperCase());
  }

  /**
   * Returns the full price history for the given symbol, most-recent first.
   */
  public List<StockQuoteHistory> getHistory(String symbol) {
    return stockQuoteHistoryRepository.findBySymbolOrderByRecordedAtDesc(symbol.toUpperCase());
  }

  // --- private helpers ---

  private StockQuote persist(Quote quote) {
    BigDecimal price = BigDecimal.valueOf(quote.getRegularMarketPrice());

    StockQuote snapshot = StockQuote.builder()
        .symbol(quote.getSymbol())
        .shortName(quote.getShortName())
        .longName(quote.getLongName())
        .currency(quote.getCurrency())
        .price(price)
        .build();

    stockQuoteRepository.upsert(snapshot);

    StockQuoteHistory entry = StockQuoteHistory.builder()
        .symbol(quote.getSymbol())
        .shortName(quote.getShortName())
        .longName(quote.getLongName())
        .currency(quote.getCurrency())
        .price(price)
        .build();

    stockQuoteHistoryRepository.save(entry);

    log.info("Refreshed quote for {}: {}", quote.getSymbol(), price);

    return stockQuoteRepository.findBySymbol(quote.getSymbol()).orElse(snapshot);
  }

}



