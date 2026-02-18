package dev.avelar.astazou.model;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table("sessions")
public class Session implements Persistable<String>  {

  @Id
  private String token;

  @NonNull
  @Column("username")
  private String username;

  @Column("expires_at")
  private OffsetDateTime expiresAt;

  @Transient
  private Long expiresIn;

  @Transient
  private List<String> roles;

  @Transient
  @Builder.Default
  private boolean isNew = true;

  @Override
  public String getId() {
    return token;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public void markNotNew() {
    this.isNew = false;
  }

  public Boolean isValid() {
    return expiresAt != null && expiresAt.isAfter(OffsetDateTime.now());
  }

  public String toString() {
    return this.username;
  }

}