package dev.avelar.astazou.converter;

import org.springframework.core.convert.converter.Converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

public class TimestampToOffsetDatetimeConverter implements Converter<Timestamp, OffsetDateTime> {

  @Override
  public OffsetDateTime convert(Timestamp source) {
    return OffsetDateTime.ofInstant(source.toInstant(), java.time.ZoneId.systemDefault());
  }

}
