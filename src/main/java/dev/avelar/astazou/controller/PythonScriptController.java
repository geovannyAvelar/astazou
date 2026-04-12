package dev.avelar.astazou.controller;

import dev.avelar.astazou.dto.ExecuteRequest;
import dev.avelar.astazou.dto.PythonScriptForm;
import dev.avelar.astazou.dto.ScriptExecutionResult;
import dev.avelar.astazou.exception.NotFoundException;
import dev.avelar.astazou.model.PythonScript;
import dev.avelar.astazou.service.PythonScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scripts")
public class PythonScriptController {

  private final PythonScriptService service;

  @Autowired
  public PythonScriptController(PythonScriptService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<PythonScript>> list() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(service.findByUsername(auth.getName()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PythonScript> get(@PathVariable Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    try {
      return ResponseEntity.ok(service.findByIdAndUsername(id, auth.getName()));
    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<PythonScript> create(@RequestBody PythonScriptForm form) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    if (form.getName() == null || form.getName().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(auth.getName(), form));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PythonScript> update(@PathVariable Long id,
      @RequestBody PythonScriptForm form) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    try {
      return ResponseEntity.ok(service.update(id, auth.getName(), form));
    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    service.delete(id, auth.getName());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/execute")
  public ResponseEntity<ScriptExecutionResult> execute(@PathVariable Long id,
      @RequestBody(required = false) ExecuteRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    try {
      String requirements = request != null ? request.requirements() : null;
      return ResponseEntity.ok(service.execute(id, auth.getName(), requirements));
    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

