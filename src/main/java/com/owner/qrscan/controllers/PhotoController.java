package com.owner.qrscan.controllers;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.owner.qrscan.services.PhotoService;
import com.owner.qrscan.socket.SocketConnectionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api")
public class PhotoController {
    @Autowired
    private PhotoService photoService;

    @PostMapping("/photo/{id}")
    public ResponseEntity<?> processPhotoForUser(@RequestParam("photo") MultipartFile file, @PathVariable String id) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se recibió ningún archivo.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("El archivo debe ser una imagen.");
        }
        return ResponseEntity.of(photoService.processPhotoForUser(file, id));
    }

    @PostMapping("/photo")
    public ResponseEntity<?> processPhoto(@RequestParam("photo") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se recibió ningún archivo.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("El archivo debe ser una imagen.");
        }
        return ResponseEntity.of(photoService.processPhoto(file));

    }


}