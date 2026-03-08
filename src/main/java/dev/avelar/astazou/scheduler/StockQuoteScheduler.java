package dev.avelar.astazou.scheduler;

import dev.avelar.astazou.brapi.BrapiService;
import dev.avelar.astazou.brapi.dto.Stock;
import dev.avelar.astazou.service.StockQuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockQuoteScheduler {

  private final BrapiService brapiService;
  private final StockQuoteService stockQuoteService;

  @Value("${brapi.scheduler.enabled:false}")
  private boolean schedulerEnabled;

  /**
   * Refreshes quotes for all tickers available on BrAPI periodically.
   * Interval is controlled by {@code brapi.scheduler.interval-ms} (default: 10 minutes).
   * Can be disabled via {@code brapi.scheduler.enabled} / {@code BRAPI_SCHEDULER_ENABLED} env var (default: {@code false}).
   */
  @Scheduled(fixedRateString = "${brapi.scheduler.interval-ms:300000}")
  public void refreshQuotes() {
    if (!schedulerEnabled) {
      log.debug("StockQuoteScheduler is disabled (brapi.scheduler.enabled=false). Skipping refresh.");
      return;
    }

    List<String> tickers = brapiService.listStocks()
        .stream()
        .map(Stock::getStock)
        .filter(t -> t != null && !t.isBlank())
        .toList();

    if (tickers.isEmpty()) {
      log.warn("No tickers returned from BrAPI, skipping refresh.");
      return;
    }

    log.info("Refreshing quotes for {} ticker(s) from BrAPI.", tickers.size());
    stockQuoteService.refreshQuotes(tickers.toArray(String[]::new));
  }

}






