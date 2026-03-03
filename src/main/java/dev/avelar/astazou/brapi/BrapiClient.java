package dev.avelar.astazou.brapi;

import dev.avelar.astazou.brapi.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "brapi", url = "${brapi.base-url:https://brapi.dev/api}", configuration = BrapiClientConfig.class)
public interface BrapiClient {

  /**
   * Fetches quotes for one or more tickers.
   * e.g. GET /quote/PETR4,VALE3
   */
  @GetMapping("/quote/{tickers}")
  QuoteApiResponse findAssetByTicker(@PathVariable("tickers") String tickers);

  /**
   * Searches available tickers by keyword.
   * e.g. GET /available?search=PETR
   */
  @GetMapping("/available")
  TickerApiResponse searchTickers(@RequestParam("search") String search);

  /**
   * Lists all available stocks.
   * e.g. GET /quote/list
   */
  @GetMapping("/quote/list")
  StockApiResponse listStocks();

  /**
   * Lists all available crypto coins.
   * e.g. GET /v2/crypto/available
   */
  @GetMapping("/v2/crypto/available")
  CoinsApiResponse listCryptoCoins();

  /**
   * Fetches data for one or more crypto coins, optionally converted to a given currency.
   * e.g. GET /v2/crypto?coin=BTC,ETH&currency=BRL
   */
  @GetMapping("/v2/crypto")
  CryptoApiResponse findCryptoCoins(
      @RequestParam("coin") String coins,
      @RequestParam(value = "currency", required = false) String currency
  );

}

