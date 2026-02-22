package dev.avelar.astazou.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BankAccountUpdateForm {

  private String name;

  private BigDecimal balance;

}

