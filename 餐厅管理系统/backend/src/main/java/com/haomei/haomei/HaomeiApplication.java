package com.haomei.haomei;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class HaomeiApplication {
    public static void main(String[] args) throws IOException {
        // SQLite 与图片存储目录若不存在则创建，避免启动/上传时报错
        for (String dir : new String[] { "data", "uploads/dish-images" }) {
            Path p = Paths.get(dir);
            if (!Files.exists(p)) {
                Files.createDirectories(p);
            }
        }
        SpringApplication.run(HaomeiApplication.class, args);
    }
}

