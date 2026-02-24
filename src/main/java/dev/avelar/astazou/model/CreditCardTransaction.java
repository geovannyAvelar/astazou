package dev.avelar.astazou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table("credit_card_transactions")
public class CreditCardTransaction {

  @Id
  private String id;

  @Column("amount")
  private BigDecimal amount;

  @Column("description")
  private String description;

  @Column("transaction_date")
  private OffsetDateTime transactionDate;

}