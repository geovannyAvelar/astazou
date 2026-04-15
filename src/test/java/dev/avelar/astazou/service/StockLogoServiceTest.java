package dev.avelar.astazou.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Note: doReturn().when() is used instead of when().thenReturn() throughout this test because
// HttpClient.send() is a generic method and Mockito's type inference resolves T to Object,
// making thenReturn(HttpResponse<InputStream>) fail at compile time.

@ExtendWith(MockitoExtension.class)
class StockLogoServiceTest {

  @Mock
  private HttpClient httpClient;

  @SuppressWarnings("unchecked")
  @Mock
  private HttpResponse<InputStream> httpResponse;

  @TempDir
  Path tempDir;

  private StockLogoService service;

  @BeforeEach
  void setUp() {
    service = new StockLogoService(tempDir.toString(), httpClient);
  }

  // ── downloadAndStore – null / blank guards ──────────────────────────────────

  @Test
  void downloadAndStore_returnsNull_whenLogoUrlIsNull() {
    String result = service.downloadAndStore("PETR4", null);

    assertNull(result);
    verifyNoInteractions(httpClient);
  }

  @Test
  void downloadAndStore_returnsNull_whenLogoUrlIsBlank() {
    String result = service.downloadAndStore("PETR4", "   ");

    assertNull(result);
    verifyNoInteractions(httpClient);
  }

  @Test
  void downloadAndStore_returnsNull_whenLogoUrlIsEmpty() {
    String result = service.downloadAndStore("PETR4", "");

    assertNull(result);
    verifyNoInteractions(httpClient);
  }

  // ── downloadAndStore – happy path ───────────────────────────────────────────

  @Test
  void downloadAndStore_returnsRelativePath_onSuccess() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("fake-image".getBytes()));

    String result = service.downloadAndStore("PETR4", "https://example.com/logos/PETR4.png");

    assertEquals("/logos/PETR4.png", result);
  }

  @Test
  void downloadAndStore_writesFileToStorageDir() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    byte[] content = "image-bytes".getBytes();
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream(content));

    service.downloadAndStore("VALE3", "https://example.com/VALE3.png");

    Path written = tempDir.resolve("VALE3.png");
    assertTrue(Files.exists(written));
    assertArrayEquals(content, Files.readAllBytes(written));
  }

  @Test
  void downloadAndStore_uppercasesTicker_inFilename() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

    String result = service.downloadAndStore("itub4", "https://example.com/itub4.png");

    assertEquals("/logos/ITUB4.png", result);
    assertTrue(Files.exists(tempDir.resolve("ITUB4.png")));
  }

  @Test
  void downloadAndStore_overwritesExistingFile() throws Exception {
    Path existing = tempDir.resolve("PETR4.png");
    Files.write(existing, "old".getBytes());

    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("new".getBytes()));

    service.downloadAndStore("PETR4", "https://example.com/PETR4.png");

    assertArrayEquals("new".getBytes(), Files.readAllBytes(existing));
  }

  // ── downloadAndStore – HTTP error handling ──────────────────────────────────

  @Test
  void downloadAndStore_returnsNull_on404Response() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(404);

    String result = service.downloadAndStore("PETR4", "https://example.com/PETR4.png");

    assertNull(result);
    assertFalse(Files.exists(tempDir.resolve("PETR4.png")));
  }

  @Test
  void downloadAndStore_returnsNull_on500Response() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(500);

    String result = service.downloadAndStore("PETR4", "https://example.com/PETR4.png");

    assertNull(result);
  }

  @Test
  void downloadAndStore_returnsNull_onIOException() throws Exception {
    doThrow(new IOException("connection refused")).when(httpClient).send(any(), any());

    String result = service.downloadAndStore("PETR4", "https://example.com/PETR4.png");

    assertNull(result);
  }

  @Test
  void downloadAndStore_returnsNull_onInterruptedException() throws Exception {
    doThrow(new InterruptedException("interrupted")).when(httpClient).send(any(), any());

    String result = service.downloadAndStore("PETR4", "https://example.com/PETR4.png");

    assertNull(result);
    assertTrue(Thread.interrupted(), "interrupt flag should be restored");
  }

  // ── extension extraction (tested via downloadAndStore) ──────────────────────

  @Test
  void downloadAndStore_preservesJpgExtension() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

    String result = service.downloadAndStore("VALE3", "https://example.com/VALE3.jpg");

    assertEquals("/logos/VALE3.jpg", result);
  }

  @Test
  void downloadAndStore_preservesSvgExtension() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

    String result = service.downloadAndStore("BBAS3", "https://example.com/BBAS3.svg");

    assertEquals("/logos/BBAS3.svg", result);
  }

  @Test
  void downloadAndStore_defaultsToPng_whenExtensionIsUnknown() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

    String result = service.downloadAndStore("MGLU3", "https://example.com/logo?ticker=MGLU3");

    assertEquals("/logos/MGLU3.png", result);
  }

  @Test
  void downloadAndStore_defaultsToPng_whenExtensionIsExe() throws Exception {
    doReturn(httpResponse).when(httpClient).send(any(), any());
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

    String result = service.downloadAndStore("MGLU3", "https://example.com/logo.exe");

    assertEquals("/logos/MGLU3.png", result);
  }

  // ── resolve ─────────────────────────────────────────────────────────────────

  @Test
  void resolve_returnsAbsolutePathUnderStorageDir() {
    Path resolved = service.resolve("PETR4.png");

    assertEquals(tempDir.resolve("PETR4.png").normalize(), resolved);
  }

}
