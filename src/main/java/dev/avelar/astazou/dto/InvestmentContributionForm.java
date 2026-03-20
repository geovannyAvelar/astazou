package dev.avelar.astazou.dto;

import dev.avelar.astazou.model.InvestmentContribution;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvestmentContributionForm {

  private String symbol;
  private LocalDate purchaseDate;
  private BigDecimal quantity;
  private BigDecimal purchasePrice;

  public InvestmentContribution toModel() {
    var contribution = new InvestmentContribution();
    contribution.setSymbol(symbol != null ? symbol.toUpperCase().trim() : null);
    contribution.setPurchaseDate(purchaseDate);
    contribution.setQuantity(quantity);
    contribution.setPurchasePrice(purchasePrice);
    return contribution;
  }

}

