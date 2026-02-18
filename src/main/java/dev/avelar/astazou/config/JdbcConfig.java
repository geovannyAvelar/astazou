package dev.avelar.astazou.config;

import dev.avelar.astazou.converter.JsonbToMapConverter;
import dev.avelar.astazou.converter.MapToJsonbConverter;
import dev.avelar.astazou.converter.OffsetDatetimeNoopConverter;
import dev.avelar.astazou.converter.TimestampToOffsetDatetimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class JdbcConfig {
  @Bean
  public JdbcCustomConversions jdbcCustomConversions(ObjectMapper mapper) {
    return new JdbcCustomConversions(List.of(
        new MapToJsonbConverter(mapper),
        new JsonbToMapConverter(mapper),
        new OffsetDatetimeNoopConverter(),
        new TimestampToOffsetDatetimeConverter()
    ));
  }
}
