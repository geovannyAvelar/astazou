package dev.avelar.astazou.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonScriptForm {

  private String name;
  private String description;
  private String code;

}

