// IFileStorageService.java
package com.ecommerce.services;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.io.IOException;

public interface IFileStorageService {
    /**
     * Saves the uploaded file to the configured directory.
     * Generates a unique filename to prevent collisions.
     *
     * @param file The uploaded file.
     * @return The unique filename under which the file was saved.
     * @throws IOException If an error occurs during file saving.
     */
    String saveFile(MultipartFile file) throws IOException;

    /**
     * Deletes a file from the storage directory.
     *
     * @param filename The name of the file to delete.
     * @throws IOException If an error occurs during deletion.
     */
    void deleteFile(String filename) throws IOException;

    /**
     * Gets the full path to the storage directory.
     * @return The Path object representing the storage directory.
     */
    Path getStorageBasePath();
}

