package dev.avelar.astazou.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginResponse {

  private boolean authenticated;

  private String completeUsername;

  private String email;

  private String username;

  private String scope;

  @JsonProperty("access_token")
  private String accessToken;

  private List<String> roles;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("expires_at")
  private OffsetDateTime expiresAt;

  @JsonProperty("preferred_currency")
  private String preferredCurrency;

}
