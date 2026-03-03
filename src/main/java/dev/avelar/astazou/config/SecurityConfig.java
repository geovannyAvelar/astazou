package dev.avelar.astazou.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  @Value("${astazou.cors.allowed-origins}")
  private String origins;

  private final AuthFilter authFilter;

  @Autowired
  public SecurityConfig(AuthFilter authFilter) {
    this.authFilter = authFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.addFilterBefore(authFilter, AuthenticationFilter.class)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(matcher -> matcher
            .requestMatchers(
                "/",
                "/token/**",
                "/actuator/**",
                "/reports/validate/**")
            .permitAll())
        .authorizeHttpRequests(matcher -> matcher.anyRequest().authenticated())
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  @Bean
  public AuthenticationManager noOpAuthenticationManager() {
    return authentication -> null;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    java.util.List<String> allowed = new java.util.ArrayList<>();
    for (String o : (origins == null ? new String[0] : origins.split(","))) {
      String t = o.trim();
      if (!t.isEmpty()) {
        allowed.add(t);
      }
    }

    if (allowed.isEmpty()) {
      // fallback to allow all origins (no credentials) if none configured
      config.setAllowedOriginPatterns(Arrays.asList("*"));
      config.setAllowCredentials(false);
    } else {
      config.setAllowedOriginPatterns(allowed);
    }

    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

}
