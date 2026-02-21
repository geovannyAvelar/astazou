package dev.avelar.astazou.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Balance {

  private Double income;

  private Double expenses;

  private Double amount;

}
