package dev.avelar.astazou.brapi;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class BrapiClientConfig {

  @Value("${brapi.token:}")
  private String token;

  @Bean
  public RequestInterceptor brapiTokenInterceptor() {
    return requestTemplate -> {
      if (token != null && !token.isBlank()) {
        requestTemplate.query("token", token);
      }
    };
  }

}

