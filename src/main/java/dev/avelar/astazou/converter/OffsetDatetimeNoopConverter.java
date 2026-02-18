package dev.avelar.astazou.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.OffsetDateTime;

@WritingConverter
public class OffsetDatetimeNoopConverter implements Converter<OffsetDateTime, OffsetDateTime> {

  /* This is strange, but it's necessary since Spring forces me to have a converter. It causes an exception if there's
    no converter available
   */
  @Override
  public OffsetDateTime convert(OffsetDateTime source) {
    return source;
  }

}
