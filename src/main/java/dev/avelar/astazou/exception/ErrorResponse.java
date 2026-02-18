package dev.avelar.astazou.exception;

import java.util.List;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ErrorResponse {

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Instant timestamp;

  private int status;

  private String error;

  private String message;

  private String path;

  private List<String> errors;

}
