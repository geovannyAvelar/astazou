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
@Table("report_tokens")
public class ReportToken {

  @Id
  private Long id;

  @Column("token")
  private String token;

  @Column("username")
  private String username;

  @Column("bank_account_id")
  private Long bankAccountId;

  @Column("account_name")
  private String accountName;

  @Column("report_month")
  private Integer reportMonth;

  @Column("report_year")
  private Integer reportYear;

  @Column("created_at")
  private OffsetDateTime createdAt;
}

