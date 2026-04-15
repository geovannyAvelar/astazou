package dev.avelar.astazou.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table("brapi_stock")
public class BrapiStock {

  @Id
  private Long id;

  @Column("ticker")
  private String ticker;

  @Column("name")
  private String name;

  @Column("sector")
  private String sector;

  @Column("logo_url")
  private String logoUrl;

  @Column("synced_at")
  private OffsetDateTime syncedAt;

}

