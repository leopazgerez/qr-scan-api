package com.owner.qrscan.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface PhotoService {
     Optional<Map<String, String>> processPhotoForUser(MultipartFile file,String id) throws IOException;
     Optional<Map<String, String>>  processPhoto(MultipartFile file) throws IOException;

}
