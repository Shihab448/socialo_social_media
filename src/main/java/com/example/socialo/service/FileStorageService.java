package com.example.socialo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE =
            Arrays.asList("image/jpeg","image/png","image/gif","image/webp");
    private static final List<String> ALLOWED_VIDEO =
            Arrays.asList("video/mp4","video/webm","video/ogg","video/quicktime");

    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String ct = file.getContentType();
        boolean img = ALLOWED_IMAGE.contains(ct);
        boolean vid = ALLOWED_VIDEO.contains(ct);
        if (!img && !vid) throw new RuntimeException("Unsupported file type: " + ct);
        if (img && file.getSize() > 10_000_000L) throw new RuntimeException("Image too large (max 10MB).");
        if (vid && file.getSize() > 100_000_000L) throw new RuntimeException("Video too large (max 100MB).");

        String orig = file.getOriginalFilename();
        String ext  = (orig != null && orig.contains(".")) ? orig.substring(orig.lastIndexOf(".")) : ".bin";
        String name = UUID.randomUUID() + ext;
        try {
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    // ── Save Base64 (from Cropper.js) ─────────────────────────
    public String saveBase64Image(String base64Data) {
        if (base64Data == null || base64Data.isBlank()) return null;
        try {
            // Strip data URL prefix: "data:image/png;base64,..."
            String encoded = base64Data;
            if (encoded.contains(",")) encoded = encoded.substring(encoded.indexOf(",") + 1);

            byte[] bytes = Base64.getDecoder().decode(encoded);
            String name  = UUID.randomUUID() + ".png";
            Path dir     = Paths.get(uploadDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Files.write(dir.resolve(name), bytes);
            return "/uploads/" + name;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save cropped image: " + e.getMessage(), e);
        }
    }

    // ── Delete ────────────────────────────────────────────────
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || fileUrl.startsWith("http")) return;
        try {
            String name = fileUrl.replace("/uploads/", "");
            Files.deleteIfExists(Paths.get(uploadDir).resolve(name));
        } catch (IOException ignored) {}
    }

    public boolean isImage(MultipartFile f) {
        return f != null && !f.isEmpty() && ALLOWED_IMAGE.contains(f.getContentType());
    }

    public boolean isVideo(MultipartFile f) {
        return f != null && !f.isEmpty() && ALLOWED_VIDEO.contains(f.getContentType());
    }
}
