package dev.avelar.astazou.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table("users")
public class User {

  @Id
  @Column("username")
  private String username;

  @Column("name")
  private String name;

  @Column("email")
  private String email;

  @Column("password")
  private String password;

  private List<String> roles;

  @Column("preferred_currency")
  private String preferredCurrency;

}
