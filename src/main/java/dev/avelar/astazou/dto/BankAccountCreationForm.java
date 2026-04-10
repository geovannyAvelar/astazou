package dev.avelar.astazou.dto;

import dev.avelar.astazou.model.BankAccount;
import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BankAccountCreationForm {

  private String name;

  private BigDecimal initialBalance;

  private String currency;

  public BankAccount toModel() {
    var account = new BankAccount();

    account.setName(name);
    account.setBalance(initialBalance);
    account.setCurrency(currency != null && !currency.isBlank() ? currency.toUpperCase() : "BRL");

    return account;
  }

}
