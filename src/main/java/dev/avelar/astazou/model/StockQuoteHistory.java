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
@Table("stock_quote_history")
public class StockQuoteHistory {

  @Id
  private Long id;

  @Column("symbol")
  private String symbol;

  @Column("short_name")
  private String shortName;

  @Column("long_name")
  private String longName;

  @Column("currency")
  private String currency;

  @Column("price")
  private BigDecimal price;

  @Column("recorded_at")
  private OffsetDateTime recordedAt;

}

