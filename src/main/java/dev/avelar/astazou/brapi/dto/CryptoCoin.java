package dev.avelar.astazou.brapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CryptoCoin {

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("currencyRateFromUSD")
  private Double currencyRateFromUSD;

  @JsonProperty("coinName")
  private String coinName;

  @JsonProperty("coin")
  private String coin;

  @JsonProperty("regularMarketChange")
  private Double regularMarketChange;

  @JsonProperty("regularMarketPrice")
  private Double regularMarketPrice;

  @JsonProperty("regularMarketChangePercent")
  private Double regularMarketChangePercent;

  @JsonProperty("regularMarketDayLow")
  private Double regularMarketDayLow;

  @JsonProperty("regularMarketDayHigh")
  private Double regularMarketDayHigh;

  @JsonProperty("regularMarketDayRange")
  private String regularMarketDayRange;

  @JsonProperty("regularMarketVolume")
  private Double regularMarketVolume;

  @JsonProperty("marketCap")
  private Double marketCap;

  @JsonProperty("regularMarketTime")
  private Integer regularMarketTime;

  @JsonProperty("coinImageUrl")
  private String coinImageUrl;

}

