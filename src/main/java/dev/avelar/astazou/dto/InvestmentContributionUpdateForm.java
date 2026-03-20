package dev.avelar.astazou.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvestmentContributionUpdateForm {

  private String symbol;
  private LocalDate purchaseDate;
  private BigDecimal quantity;
  private BigDecimal purchasePrice;

}

