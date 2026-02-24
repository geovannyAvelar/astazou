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

  public CreditCard toModel() {
    CreditCard creditCard = new CreditCard();
    creditCard.setName(name);
    return creditCard;
  }

}
