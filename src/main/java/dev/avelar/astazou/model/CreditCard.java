package dev.avelar.astazou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table("credit_cards")
public class CreditCard {

  @Id
  private Long id;

  private String name;

  private String username;

  private String currency;

}
