package dev.avelar.astazou.dto;


import dev.avelar.astazou.model.CreditCard;
import dev.avelar.astazou.service.CreditCardService;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreditCardCreationForm {

  private String name;

  private String currency;

  public CreditCard toModel() {
    CreditCard creditCard = new CreditCard();
    creditCard.setName(name);
    creditCard.setCurrency(currency != null && !currency.isBlank() ? currency.toUpperCase() : "BRL");
    return creditCard;
  }

}
