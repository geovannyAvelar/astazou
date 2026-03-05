package dev.avelar.astazou.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionUpdateForm {

  private LocalDate transactionDate;

  private String description;

  private BigDecimal amount;

  private String type;

}

