package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.BrapiStock;
import dev.avelar.astazou.repository.BrapiStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class BrapiStockController {

  private final BrapiStockRepository brapiStockRepository;

  /**
   * Returns metadata (name, sector, logo URL) for the given ticker.
   * <p>
   * Example: {@code GET /stocks/PETR4}
   */
  @GetMapping("/{ticker}")
  public ResponseEntity<BrapiStock> getStock(@PathVariable String ticker) {
    return brapiStockRepository.findByTicker(ticker.toUpperCase())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

}

