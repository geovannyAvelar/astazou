package dev.avelar.astazou.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table("investment_contribution")
public class InvestmentContribution {

  @Id
  @Column("id")
  private Long id;

  @Column("symbol")
  private String symbol;

  @Column("purchase_date")
  private LocalDate purchaseDate;

  @Column("quantity")
  private BigDecimal quantity;

  @Column("purchase_price")
  private BigDecimal purchasePrice;

  @Column("username")
  private String username;

  @Column("created_at")
  private OffsetDateTime createdAt;

}

