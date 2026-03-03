package dev.avelar.astazou.brapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Stock {

  @JsonProperty("stock")
  private String stock;

  @JsonProperty("name")
  private String name;

  @JsonProperty("close")
  private Double close;

  @JsonProperty("change")
  private Double change;

  @JsonProperty("volume")
  private Long volume;

  @JsonProperty("market_cap")
  private Double marketCap;

  @JsonProperty("logo")
  private String logo;

  @JsonProperty("sector")
  private String sector;

}

