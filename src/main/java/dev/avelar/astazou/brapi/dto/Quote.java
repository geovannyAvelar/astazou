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

  @JsonProperty("longName")
  private String longName;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("regularMarketPrice")
  private Double regularMarketPrice;

}

