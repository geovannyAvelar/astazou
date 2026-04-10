package dev.avelar.astazou.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table("bank_account")
public class BankAccount {

  @Id
  @Column("id")
  private Long id;

  @NonNull
  @Column("name")
  private String name;

  @NonNull
  @Column("balance")
  private BigDecimal balance;

  @NonNull
  @Column("username")
  private String username;

  @Column("currency")
  private String currency;

}
