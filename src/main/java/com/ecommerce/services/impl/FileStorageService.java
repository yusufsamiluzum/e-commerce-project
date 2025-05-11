// FileStorageService.java
package com.ecommerce.services.impl;

import com.ecommerce.services.IFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // For filename cleaning
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService implements IFileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${image.upload.dir}")
    private String uploadDir;

    private Path storageLocation;

    @PostConstruct
    public void init() {
        try {
            storageLocation = Paths.get(this.uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(storageLocation); // Create directory if it doesn't exist
            log.info("File storage location initialized at: {}", storageLocation);
        } catch (Exception ex) {
            log.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not initialize storage location: " + this.uploadDir, ex);
        }
    }

    @Override
    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot save null or empty file.");
        }

        // Generate a unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        // Sanitize filename (optional, but recommended)
        String baseName = originalFilename.substring(0, extensionIndex > 0 ? extensionIndex : originalFilename.length())
                                          .replaceAll("[^a-zA-Z0-9.-]", "_"); // Replace invalid chars
        String uniqueFileName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

        // Prevent directory traversal
        if (uniqueFileName.contains("..")) {
            throw new IOException("Filename contains invalid path sequence: " + uniqueFileName);
        }

        Path targetLocation = this.storageLocation.resolve(uniqueFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved file: {}", uniqueFileName);
            return uniqueFileName; // Return only the filename, not the full path
        } catch (IOException ex) {
            log.error("Could not store file {}. Please try again!", uniqueFileName, ex);
            throw new IOException("Could not store file " + uniqueFileName + ". Error: " + ex.getMessage(), ex);
        }
    }

     @Override
     public void deleteFile(String filename) throws IOException {
         if (filename == null || filename.isBlank()) {
             log.warn("Attempted to delete file with null or blank name.");
             return; // Or throw exception
         }
          // Prevent directory traversal
         if (filename.contains("..")) {
            throw new IOException("Filename contains invalid path sequence: " + filename);
        }

         try {
             Path filePath = this.storageLocation.resolve(filename).normalize();
             if (Files.exists(filePath)) {
                 Files.delete(filePath);
                 log.info("Successfully deleted file: {}", filename);
             } else {
                 log.warn("Attempted to delete non-existent file: {}", filename);
             }
         } catch (IOException ex) {
             log.error("Could not delete file: {}", filename, ex);
             throw new IOException("Could not delete file " + filename + ". Error: " + ex.getMessage(), ex);
         }
     }

    @Override
    public Path getStorageBasePath() {
        return this.storageLocation;
    }
}