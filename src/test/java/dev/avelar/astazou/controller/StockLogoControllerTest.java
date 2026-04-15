package dev.avelar.astazou.controller;

import dev.avelar.astazou.service.StockLogoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLogoControllerTest {

  @Mock
  private StockLogoService stockLogoService;

  @InjectMocks
  private StockLogoController controller;

  @TempDir
  Path tempDir;

  // ── happy paths ──────────────────────────────────────────────────────────────

  @Test
  void getLogo_returnsOk_whenFileExists() throws IOException {
    Path file = tempDir.resolve("PETR4.png");
    Files.write(file, "fake-image".getBytes());
    when(stockLogoService.resolve("PETR4.png")).thenReturn(file);

    ResponseEntity<Resource> response = controller.getLogo("PETR4.png");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void getLogo_returnsImagePngContentType_forPngFile() throws IOException {
    Path file = tempDir.resolve("VALE3.png");
    Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}); // PNG magic bytes
    when(stockLogoService.resolve("VALE3.png")).thenReturn(file);

    ResponseEntity<Resource> response = controller.getLogo("VALE3.png");

    assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
  }

  @Test
  void getLogo_returnsImageJpegContentType_forJpgFile() throws IOException {
    Path file = tempDir.resolve("ITUB4.jpg");
    Files.write(file, "fake-jpg".getBytes());
    when(stockLogoService.resolve("ITUB4.jpg")).thenReturn(file);

    ResponseEntity<Resource> response = controller.getLogo("ITUB4.jpg");

    assertNotNull(response.getHeaders().getContentType());
    assertTrue(response.getHeaders().getContentType().toString().contains("image/jpeg") ||
               response.getHeaders().getContentType().toString().contains("image/jpg"));
  }

  @Test
  void getLogo_returnsSvgContentType_forSvgFile() throws IOException {
    Path file = tempDir.resolve("BBAS3.svg");
    Files.write(file, "<svg/>".getBytes());
    when(stockLogoService.resolve("BBAS3.svg")).thenReturn(file);

    ResponseEntity<Resource> response = controller.getLogo("BBAS3.svg");

    assertNotNull(response.getHeaders().getContentType());
    assertTrue(response.getHeaders().getContentType().toString().contains("svg"));
  }

  @Test
  void getLogo_returnsOctetStream_forUnknownExtension() throws IOException {
    Path file = tempDir.resolve("MGLU3.bin");
    Files.write(file, "data".getBytes());
    when(stockLogoService.resolve("MGLU3.bin")).thenReturn(file);

    ResponseEntity<Resource> response = controller.getLogo("MGLU3.bin");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
  }

  @Test
  void getLogo_resourceBodyIsReadable() throws IOException {
    byte[] content = "real-image-data".getBytes();
    Path file = tempDir.resolve("PETR4.png");
    Files.write(file, content);
    when(stockLogoService.resolve("PETR4.png")).thenReturn(file);

    ResponseEntity<Resource> response = controller.getLogo("PETR4.png");

    assertNotNull(response.getBody());
    assertArrayEquals(content, response.getBody().getContentAsByteArray());
  }

  // ── not found ────────────────────────────────────────────────────────────────

  @Test
  void getLogo_returnsNotFound_whenFileDoesNotExist() {
    Path missing = tempDir.resolve("MISSING.png");
    when(stockLogoService.resolve("MISSING.png")).thenReturn(missing);

    ResponseEntity<Resource> response = controller.getLogo("MISSING.png");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  // ── path traversal guards ────────────────────────────────────────────────────

  @Test
  void getLogo_returnsBadRequest_whenFilenameContainsDoubleDot() {
    ResponseEntity<Resource> response = controller.getLogo("../etc/passwd");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(stockLogoService);
  }

  @Test
  void getLogo_returnsBadRequest_whenFilenameContainsForwardSlash() {
    ResponseEntity<Resource> response = controller.getLogo("subdir/PETR4.png");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(stockLogoService);
  }

  @Test
  void getLogo_returnsBadRequest_whenFilenameContainsBackslash() {
    ResponseEntity<Resource> response = controller.getLogo("subdir\\PETR4.png");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(stockLogoService);
  }

  @Test
  void getLogo_returnsBadRequest_whenFilenameIsJustDoubleDot() {
    ResponseEntity<Resource> response = controller.getLogo("..");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(stockLogoService);
  }

}

