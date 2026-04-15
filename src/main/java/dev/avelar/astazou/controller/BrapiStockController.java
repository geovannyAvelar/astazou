package dev.avelar.astazou.controller;

import dev.avelar.astazou.model.BrapiStock;
import dev.avelar.astazou.repository.BrapiStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class BrapiStockController {

  private final BrapiStockRepository brapiStockRepository;

  /**
   * Returns metadata (name, sector, logo URL) for the given ticker.
   */
  @GetMapping("/{ticker}")
  public ResponseEntity<BrapiStock> getStock(@PathVariable String ticker) {
    return brapiStockRepository.findByTicker(ticker.toUpperCase())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns up to 10 stocks whose ticker starts with {@code q} or whose name
   * contains {@code q} (case-insensitive).
   */
  @GetMapping("/search")
  public List<BrapiStock> search(@RequestParam(defaultValue = "") String q) {
    if (q.isBlank()) return List.of();
    return brapiStockRepository.search(q.toUpperCase());
  }

}

