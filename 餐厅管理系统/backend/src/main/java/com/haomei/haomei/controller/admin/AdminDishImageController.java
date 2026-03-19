package com.haomei.haomei.controller.admin;

import com.haomei.haomei.dto.DishResponse;
import com.haomei.haomei.entity.Dish;
import com.haomei.haomei.repository.DishRepository;
import com.haomei.haomei.service.websocket.MenuUpdateMessage;
import com.haomei.haomei.service.websocket.RealtimeNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

@RestController
@RequestMapping("/api/admin/dishes")
public class AdminDishImageController {

    private final DishRepository dishRepository;
    private final RealtimeNotificationService realtimeNotificationService;
    private final Path storageDir;

    public AdminDishImageController(
            DishRepository dishRepository,
            RealtimeNotificationService realtimeNotificationService,
            @Value("${app.images.storageDir}") String storageDir
    ) {
        this.dishRepository = dishRepository;
        this.realtimeNotificationService = realtimeNotificationService;
        this.storageDir = Path.of(storageDir);
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create image storage directory", e);
        }
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DishResponse upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片文件");
        }
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Dish not found: " + id));

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "jpg";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        String filename = "dish-" + id + "-upload-" + Instant.now().toEpochMilli() + "." + ext;
        Path target = storageDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        dish.setImageFilename(filename);
        Dish saved = dishRepository.save(dish);

        DishResponse resp = new DishResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getPrice(),
                saved.isAvailable(),
                saved.getCategory().name(),
                saved.getPortionOption() == null ? "BOTH" : saved.getPortionOption().name(),
                "/api/images/" + saved.getImageFilename()
        );
        realtimeNotificationService.broadcastMenuUpdate(new MenuUpdateMessage("UPDATED", saved.getId(), saved.getName(), resp.imageUrl()));
        return resp;
    }
}

