package dev.avelar.astazou.scheduler;

import dev.avelar.astazou.service.BrapiStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrapiStockSyncScheduler {

  private final BrapiStockService brapiStockService;

  @Value("${brapi.scheduler.enabled:false}")
  private boolean schedulerEnabled;

  /**
   * Runs the stock sync once as soon as the application is ready.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void syncOnStartup() {
    log.info("Starting BrAPI stock list sync.");
    syncStocks();
  }

  /**
   * Syncs the list of available stocks from BrAPI into the {@code brapi_stock} table.
   * Runs once a week (every Monday at 06:00 São Paulo time).
   * Can be disabled via {@code brapi.scheduler.enabled} / {@code BRAPI_SCHEDULER_ENABLED} env var.
   */
  @Scheduled(cron = "${brapi.stock-sync.cron:0 0 6 * * MON}", zone = "America/Sao_Paulo")
  public void syncStocks() {
    if (!schedulerEnabled) {
      log.debug("BrapiStockSyncScheduler is disabled (brapi.scheduler.enabled=false). Skipping sync.");
      return;
    }

    log.info("Starting weekly BrAPI stock list sync.");
    try {
      brapiStockService.syncStocks();
    } catch (Exception e) {
      log.error("Failed to sync BrAPI stock list.", e);
    }
  }

}

