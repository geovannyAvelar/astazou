package dev.avelar.astazou.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeService {

  /**
   * Generates a QR code PNG image for the given URL and returns it as a
   * Base64-encoded data URI suitable for embedding in HTML/PDF templates.
   *
   * @param url    the URL to encode
   * @param sizePx width and height in pixels
   * @return {@code data:image/png;base64,...} string
   */
  public String generateQrCodeDataUri(String url, int sizePx) {
    try {
      QRCodeWriter writer = new QRCodeWriter();
      Map<EncodeHintType, Object> hints = Map.of(
          EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
          EncodeHintType.MARGIN, 1
      );
      BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

      MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
      BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, config);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "PNG", baos);
      String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
      return "data:image/png;base64," + base64;
    } catch (WriterException | IOException e) {
      throw new RuntimeException("Failed to generate QR code for URL: " + url, e);
    }
  }
}

