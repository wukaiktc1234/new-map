package com.foodtraceability.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ImageUploadUtils {

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;

    public static class UploadResult {
        private String url;
        private String fileName;
        private String originalName;
        private long fileSize;

        public UploadResult(String url, String fileName, String originalName, long fileSize) {
            this.url = url;
            this.fileName = fileName;
            this.originalName = originalName;
            this.fileSize = fileSize;
        }

        public String getUrl() {
            return url;
        }

        public String getFileName() {
            return fileName;
        }

        public String getOriginalName() {
            return originalName;
        }

        public long getFileSize() {
            return fileSize;
        }
    }

    public static UploadResult uploadImage(MultipartFile file, String uploadDir) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFileName = generateFileName(extension);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFileName);

        try {
            Thumbnails.of(file.getInputStream())
                    .size(MAX_WIDTH, MAX_HEIGHT)
                    .outputQuality(0.85)
                    .toFile(filePath.toFile());
        } catch (Exception e) {
            Files.copy(file.getInputStream(), filePath);
        }

        long fileSize = Files.size(filePath);

        return new UploadResult(
                "/uploads/" + newFileName,
                newFileName,
                originalFilename,
                fileSize
        );
    }

    private static void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IOException("不支持的文件类型，仅支持 JPG、PNG、GIF、WEBP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("文件大小不能超过5MB");
        }
    }

    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (ext.equals("jpeg")) {
            ext = "jpg";
        }
        return ext;
    }

    private static String generateFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + "." + extension;
    }

    public static boolean deleteImage(String uploadDir, String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
