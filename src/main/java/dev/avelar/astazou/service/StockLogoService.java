package dev.avelar.astazou.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Downloads stock logo images from remote URLs and stores them in the configured
 * local directory. Returns the public-facing relative path used to serve each logo.
 */
@Slf4j
@Service
public class StockLogoService {

  private final Path storageDir;

  private final HttpClient httpClient;

  public StockLogoService(@Value("${astazou.logos.storage-path:./logos}") String storagePath) {
    this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  /** Package-private constructor used in unit tests to inject a mock {@link HttpClient}. */
  StockLogoService(String storagePath, HttpClient httpClient) {
    this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
    this.httpClient = httpClient;
  }

  /**
   * Downloads the logo for the given ticker from {@code logoUrl} and persists it to disk.
   *
   * @param ticker  the stock ticker (e.g. "PETR4"), used as the file base name
   * @param logoUrl the remote URL to fetch the image from
   * @return the relative public URL path (e.g. "/logos/PETR4.png"), or {@code null} on failure
   */
  public String downloadAndStore(String ticker, String logoUrl) {
    if (logoUrl == null || logoUrl.isBlank()) {
      return null;
    }
    try {
      Files.createDirectories(storageDir);

      String extension = extractExtension(logoUrl);
      String filename  = ticker.toUpperCase() + extension;
      Path   target    = storageDir.resolve(filename);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(logoUrl))
          .timeout(Duration.ofSeconds(15))
          .GET()
          .build();

      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("Failed to download logo for {}: HTTP {}", ticker, response.statusCode());
        return null;
      }

      try (InputStream in = response.body()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }

      log.debug("Logo saved for {}: {}", ticker, target);
      return "/logos/" + filename;

    } catch (IOException | InterruptedException e) {
      log.warn("Could not download logo for ticker {}: {}", ticker, e.getMessage());
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return null;
    }
  }

  /**
   * Returns the absolute {@link Path} of a stored logo file by its filename.
   */
  public Path resolve(String filename) {
    return storageDir.resolve(filename).normalize();
  }

  /**
   * Returns the configured storage directory.
   */
  public Path getStorageDir() {
    return storageDir;
  }

  // -------------------------------------------------------------------------

  private static String extractExtension(String url) {
    try {
      String path = URI.create(url).getPath();
      int dot = path.lastIndexOf('.');
      if (dot >= 0 && dot < path.length() - 1) {
        String ext = path.substring(dot); // includes the dot
        // Basic sanity check – only common image extensions
        if (ext.matches("\\.(png|jpg|jpeg|svg|webp|gif)")) {
          return ext.toLowerCase();
        }
      }
    } catch (Exception ignored) {
      // fall through to default
    }
    return ".png";
  }

}


