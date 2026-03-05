package dev.avelar.astazou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table("transactions")
public class Transaction {

  @Id
  private Long id;

  @Column("transaction_date")
  private LocalDate transactionDate;

  @Column("description")
  private String description;

  @Column("amount")
  private BigDecimal amount;

  @Column("type")
  private String type;

  @Column("page")
  private Integer page;

  @Column("created_at")
  private OffsetDateTime createdAt;

  @Column("bank_account_id")
  private Long bankAccountId;

  private int sequence;

  @Column("tags")
  @Builder.Default
  private String[] tags = new String[0];

}
