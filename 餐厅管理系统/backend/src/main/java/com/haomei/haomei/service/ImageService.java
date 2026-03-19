package com.haomei.haomei.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class ImageService {
    private final Path storageDir;
    private final HttpClient httpClient;

    public ImageService(@Value("${app.images.storageDir}") String storageDir) {
        this.storageDir = Path.of(storageDir);
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create image storage directory: " + storageDir, e);
        }
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String downloadDishImage(String query, String filenameWithoutExt, String extension) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String safeExt = (extension == null || extension.isBlank()) ? "jpg" : extension;
        String filename = filenameWithoutExt + "." + safeExt;
        Path target = storageDir.resolve(filename);

        String encoded = URLEncoder.encode(query + " food restaurant", StandardCharsets.UTF_8);
        // Unsplash Source: returns an image by keyword (redirected). This may be rate-limited by their service.
        String url = "https://source.unsplash.com/600x600/?" + encoded;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; HaomeiBot/1.0)")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return null;
            }
            Files.write(target, resp.body(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return filename;
        } catch (Exception e) {
            // If online download fails, we just skip image.
            return null;
        }
    }
}

