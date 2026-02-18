package dev.avelar.astazou.converter;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@ReadingConverter
public class JsonbToMapConverter implements Converter<Object, Map<String, Object>> {
  private final ObjectMapper mapper;

  public JsonbToMapConverter(ObjectMapper mapper) { this.mapper = mapper; }

  @Override
  public Map<String, Object> convert(Object source) {
    try {
      String json = source instanceof PGobject ? ((PGobject) source).getValue() : source.toString();
      return mapper.readValue(json, Map.class);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
