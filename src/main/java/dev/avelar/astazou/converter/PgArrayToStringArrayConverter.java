package dev.avelar.astazou.converter;

import org.postgresql.jdbc.PgArray;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.sql.SQLException;

@ReadingConverter
public class PgArrayToStringArrayConverter implements Converter<Object, String[]> {

  @Override
  public String[] convert(Object source) {
    try {
      if (source instanceof PgArray pgArray) {
        Object array = pgArray.getArray();
        if (array instanceof String[] strings) {
          return strings;
        }
        if (array instanceof Object[] objects) {
          String[] result = new String[objects.length];
          for (int i = 0; i < objects.length; i++) {
            result[i] = objects[i] != null ? objects[i].toString() : null;
          }
          return result;
        }
      }
      return new String[0];
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to convert PgArray to String[]", e);
    }
  }
}

