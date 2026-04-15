package dev.avelar.astazou.controller;

import dev.avelar.astazou.service.StockLogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/logos")
@RequiredArgsConstructor
public class StockLogoController {

  private final StockLogoService stockLogoService;

  /**
   * Serves a stock logo file stored on the local filesystem.
   * <p>
   * Example: {@code GET /logos/PETR4.png}
   */
  @GetMapping("/{filename}")
  public ResponseEntity<Resource> getLogo(@PathVariable String filename) {
    // Security: reject path traversal attempts
    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
      return ResponseEntity.badRequest().build();
    }

    Path file = stockLogoService.resolve(filename);

    if (!Files.exists(file) || !Files.isReadable(file)) {
      return ResponseEntity.notFound().build();
    }

    Resource resource = new FileSystemResource(file);
    MediaType mediaType = MediaTypeFactory.getMediaType(filename)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);

    return ResponseEntity.ok()
        .contentType(mediaType)
        .body(resource);
  }

}



