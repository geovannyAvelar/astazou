package dev.avelar.astazou.scheduler;

import dev.avelar.astazou.service.BrapiStockService;
import dev.avelar.astazou.service.StockQuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockQuoteScheduler {

  private final BrapiStockService brapiStockService;
  private final StockQuoteService stockQuoteService;

  @Value("${brapi.scheduler.enabled:false}")
  private boolean schedulerEnabled;

  private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

  private static final LocalTime MARKET_OPEN = LocalTime.of(10, 0);

  private static final LocalTime MARKET_CLOSE = LocalTime.of(18, 0);

  /**
   * Matches B3 common/preferred stocks (e.g. PETR3, PETR4, VALE5, PRIO3) and FIIs (e.g. MXRF11, HGLG11). Pattern:
   * exactly 4 letters followed by 3, 4, 5, 6 (stocks) or 11 (FIIs).
   */
  private static final Pattern B3_TICKER = Pattern.compile("^[A-Z]{4}(3|4|5|6|11)$");

  /**
   * Refreshes quotes for all tickers available on BrAPI periodically. Interval is controlled by
   * {@code brapi.scheduler.interval-ms} (default: 10 minutes). Can be disabled via {@code brapi.scheduler.enabled} /
   * {@code BRAPI_SCHEDULER_ENABLED} env var (default: {@code false}).
   */
  @Scheduled(fixedRateString = "${brapi.scheduler.interval-ms:300000}")
  public void refreshQuotes() {
    if (!schedulerEnabled) {
      log.debug("StockQuoteScheduler is disabled (brapi.scheduler.enabled=false). Skipping refresh.");
      return;
    }

    DayOfWeek today = LocalDate.now(SAO_PAULO).getDayOfWeek();
    if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
      log.debug("Skipping quote refresh on weekend ({}).", today);
      return;
    }

    ZonedDateTime nowSP = ZonedDateTime.now(SAO_PAULO);
    LocalTime timeSP = nowSP.toLocalTime();
    if (timeSP.isBefore(MARKET_OPEN) || !timeSP.isBefore(MARKET_CLOSE)) {
      log.debug("Skipping quote refresh outside B3 market hours ({}).", timeSP);
      return;
    }

    // @formatter:off
    List<String> tickers =
        brapiStockService.listTickers().stream()
            .filter(t -> t != null && !t.isBlank())
            .map(String::toUpperCase)
            .filter(t -> B3_TICKER.matcher(t).matches()).toList();
    // @formatter:on

    if (tickers.isEmpty()) {
      log.warn("No tickers returned from BrAPI, skipping refresh.");
      return;
    }

    log.info("Refreshing quotes for {} ticker(s) from BrAPI.", tickers.size());
    stockQuoteService.refreshQuotes(tickers.toArray(String[]::new));
  }

}






