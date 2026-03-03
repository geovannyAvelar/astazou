package dev.avelar.astazou.brapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Quote {

  @JsonProperty("symbol")
  private String symbol;

  @JsonProperty("shortName")
  private String shortName;

  @JsonProperty("LongName")
  private String longName;

  @JsonProperty("Currency")
  private String currency;

  @JsonProperty("RegularMarketPrice")
  private Double regularMarketPrice;

}

