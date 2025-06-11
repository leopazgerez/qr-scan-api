package com.owner.qrscan.services.implementation;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.owner.qrscan.services.PhotoService;
import com.owner.qrscan.socket.SocketConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PhotoServiceImpl implements PhotoService {
    private static final Logger logger = LoggerFactory.getLogger(PhotoServiceImpl.class);
    @Autowired
    private SocketConnectionHandler socketHandler;

    @Override
    public Optional<Map<String, String>> processPhotoForUser(MultipartFile file, String id) throws IOException {
        try {
            BufferedImage imagen = ImageIO.read(file.getInputStream());

            if (imagen == null) {
                throw new IOException("No se pudo leer la imagen");
            }
            // Intentar detectar código con diferentes configuraciones
            String result = detectCode(imagen);

            Map<String, String> response = new HashMap<>();
            if (result != null) {
                response.put("status", "success");
                response.put("data", result);
                if (id != null) {
                    socketHandler.sendMessageToClient(id, result);
                }
            } else {
                response.put("status", "not_found");
                response.put("message", "No se detectó ningún código QR ni de barras en la imagen.");
            }
            logger.info("🔗 processPhotoForUser  Status: {}, Data: {}", response.get("status"), response.get("data"));
            return Optional.ofNullable(response);

        } catch (IOException e) {
            logger.error("⚠️ Excepcion del processPhotoForUser", e);
            throw e;
            //return ResponseEntity.status(500).body("Error al leer la imagen: " + e.getMessage());
        } catch (Exception e) {
            logger.error("⚠️ Excepcion del processPhotoForUser", e);
            throw e;
            //return ResponseEntity.status(500).body("Error procesando imagen: " + e.getMessage());
        }
    }

    @Override
    public Optional<Map<String, String>> processPhoto(MultipartFile file) throws IOException {
        try {
            BufferedImage imagen = ImageIO.read(file.getInputStream());

            if (imagen == null) {
                throw new IOException("No se pudo leer la imagen");
            }

            // Intentar detectar código con diferentes configuraciones
            String result = detectCode(imagen);

            Map<String, String> response = new HashMap<>();
            if (result != null) {
                response.put("status", "success");
                response.put("data", result);
            } else {
                response.put("status", "not_found");
                response.put("message", "No se detectó ningún código QR ni de barras en la imagen.");
            }
            return Optional.ofNullable(response);

        } catch (IOException e) {
            throw e;
            //return ResponseEntity.status(500).body("Error al leer la imagen: " + e.getMessage());
        } catch (Exception e) {
            throw e;
            //return ResponseEntity.status(500).body("Error procesando imagen: " + e.getMessage());
        }
    }


    private String detectCode(BufferedImage imagen) {
        try {
            // Configurar hints para mejorar la detección
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.Arrays.asList(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.PDF_417
            ));

            // Crear fuente de luminancia
            LuminanceSource source = new BufferedImageLuminanceSource(imagen);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Intentar decodificar con MultiFormatReader
            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(hints);

            try {
                Result result = reader.decode(bitmap);
                return result.getText();
            } catch (NotFoundException e) {
                // Si no encuentra con el método normal, intentar con rotación
                return tryRotating(imagen, hints);
            }

        } catch (Exception e) {
            System.err.println("Error en detectarCodigo: " + e.getMessage());
            return null;
        }
    }

    private String tryRotating(BufferedImage image, Map<DecodeHintType, Object> hints) {
        try {
            // Probar rotando la imagen 90, 180 y 270 grados
            for (int angle : new int[]{90, 180, 270}) {
                BufferedImage imagenRotada = turnImage(image, angle);

                LuminanceSource source = new BufferedImageLuminanceSource(imagenRotada);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                MultiFormatReader reader = new MultiFormatReader();
                reader.setHints(hints);

                try {
                    Result resultado = reader.decode(bitmap);
                    return resultado.getText();
                } catch (NotFoundException e) {
                    // Continuar con la siguiente rotación
                }
            }
        } catch (Exception e) {
            System.err.println("Error en intentarConRotacion: " + e.getMessage());
        }

        return null;
    }

    private BufferedImage turnImage(BufferedImage image, int angle) {
        int ancho = image.getWidth();
        int alto = image.getHeight();

        BufferedImage imagenRotada;
        if (angle == 90 || angle == 270) {
            imagenRotada = new BufferedImage(alto, ancho, image.getType());
        } else {
            imagenRotada = new BufferedImage(ancho, alto, image.getType());
        }

        java.awt.Graphics2D g2d = imagenRotada.createGraphics();

        if (angle == 90 || angle == 270) {
            g2d.translate(alto / 2, ancho / 2);
        } else {
            g2d.translate(ancho / 2, alto / 2);
        }

        g2d.rotate(Math.toRadians(angle));
        g2d.translate(-ancho / 2, -alto / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return imagenRotada;
    }
}
