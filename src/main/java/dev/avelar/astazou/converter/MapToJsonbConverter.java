package dev.avelar.astazou.converter;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.Map;

@WritingConverter
public class MapToJsonbConverter implements Converter<Map<String, Object>, PGobject> {
  private final ObjectMapper mapper;

  public MapToJsonbConverter(ObjectMapper mapper) { this.mapper = mapper; }

  @Override
  public PGobject convert(Map<String, Object> source) {
    try {
      PGobject pg = new PGobject();
      pg.setType("jsonb");
      pg.setValue(mapper.writeValueAsString(source));
      return pg;
    } catch (SQLException | RuntimeException ex) {
      throw new IllegalStateException(ex);
    }
  }
}