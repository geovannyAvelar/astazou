package dev.avelar.astazou.brapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StockApiResponse {

  @JsonProperty("stocks")
  private List<Stock> stocks;

}

