package dev.avelar.astazou.brapi;

import dev.avelar.astazou.brapi.dto.CryptoCoin;
import dev.avelar.astazou.brapi.dto.Quote;
import dev.avelar.astazou.brapi.dto.Stock;
import dev.avelar.astazou.brapi.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrapiService {

  private final BrapiClient brapiClient;

  /**
   * Fetches quotes for one or more ticker symbols.
   */
  public List<Quote> findAssetByTicker(String... tickers) {
    String tickersParam = String.join(",", tickers);
    return brapiClient.findAssetByTicker(tickersParam).getResults();
  }

  /**
   * Searches available tickers by keyword.
   */
  public List<String> searchTickers(String keyword) {
    return brapiClient.searchTickers(keyword).getStocks();
  }

  /**
   * Lists all available stocks.
   */
  public List<Stock> listStocks() {
    return brapiClient.listStocks().getStocks();
  }

  /**
   * Lists all available crypto coin identifiers.
   */
  public List<String> listCryptoCoins() {
    return brapiClient.listCryptoCoins().getCoins();
  }

  /**
   * Fetches data for the given crypto coins, optionally converted to the specified currency.
   *
   * @param coins    list of coin identifiers (e.g. "BTC", "ETH")
   * @param currency target currency code (e.g. "BRL"), or null/empty to use the default
   */
  public List<CryptoCoin> findCryptoCoins(List<String> coins, String currency) {
    if (coins == null || coins.isEmpty()) {
      throw new IllegalArgumentException("At least one coin must be informed.");
    }
    String coinsParam = String.join(",", coins);
    String currencyParam = (currency != null && !currency.isBlank()) ? currency : null;
    return brapiClient.findCryptoCoins(coinsParam, currencyParam).getCoins();
  }

}
