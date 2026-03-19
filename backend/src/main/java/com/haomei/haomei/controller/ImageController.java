package com.haomei.haomei.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    private final Path storageDir;

    public ImageController(@Value("${app.images.storageDir}") String storageDir) {
        this.storageDir = Path.of(storageDir);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        // Basic path traversal protection: only allow plain filenames.
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") ) {
            return ResponseEntity.notFound().build();
        }

        Path file = storageDir.resolve(filename).normalize();
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(file);
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;

        InputStream is = Files.newInputStream(file);
        InputStreamResource resource = new InputStreamResource(is);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}

