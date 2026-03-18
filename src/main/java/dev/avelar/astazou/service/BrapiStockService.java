package dev.avelar.astazou.service;

import dev.avelar.astazou.brapi.BrapiService;
import dev.avelar.astazou.model.BrapiStock;
import dev.avelar.astazou.repository.BrapiStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrapiStockService {

  private final BrapiService brapiService;

  private final BrapiStockRepository brapiStockRepository;

  /**
   * Fetches all available stocks from BrAPI and upserts them into the {@code brapi_stock} table.
   */
  @Transactional
  public void syncStocks() {
    List<BrapiStock> stocks = brapiService.listStocks().stream()
        .filter(s -> s.getStock() != null && !s.getStock().isBlank())
        .map(s -> BrapiStock.builder()
            .ticker(s.getStock().toUpperCase())
            .name(s.getName())
            .sector(s.getSector())
            .build())
        .toList();

    log.info("Syncing {} stock(s) from BrAPI into brapi_stock table.", stocks.size());
    stocks.forEach(brapiStockRepository::upsert);
    log.info("BrAPI stock sync complete.");
  }

  /**
   * Returns all tickers currently stored in the {@code brapi_stock} table.
   */
  public List<String> listTickers() {
    return brapiStockRepository.findAll().stream()
        .map(BrapiStock::getTicker)
        .toList();
  }

}

