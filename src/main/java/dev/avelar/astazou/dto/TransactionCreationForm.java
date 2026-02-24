package dev.avelar.astazou.dto;

import dev.avelar.astazou.model.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionCreationForm {

  private LocalDate transactionDate;

  private String description;

  private BigDecimal amount;

  private String type;

  private Long bankAccountId;

  private Boolean updateAccount;

  public Transaction toModel() {
    Transaction transaction = new Transaction();

    transaction.setTransactionDate(transactionDate);
    transaction.setDescription(description);
    transaction.setType(type);
    transaction.setBankAccountId(bankAccountId);
    transaction.setAmount(amount);

    return transaction;
  }

}
