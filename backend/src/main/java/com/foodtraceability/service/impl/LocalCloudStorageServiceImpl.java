package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.CloudStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 本地云存储服务实现（模拟云存储，实际存储在本地目录）
 */
@Service
public class LocalCloudStorageServiceImpl implements CloudStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalCloudStorageServiceImpl.class);

    @Value("${app.cloud-storage.local-path:./cloud-storage}")
    private String cloudStoragePath;

    @Override
    public Result<Map<String, Object>> uploadFile(File file, String remotePath) {
        log.info("上传文件到本地云存储: {} -> {}", file.getAbsolutePath(), remotePath);

        try {
            Path targetDir = Paths.get(cloudStoragePath, remotePath).getParent();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path targetPath = Paths.get(cloudStoragePath, remotePath);
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> result = new HashMap<>();
            result.put("remotePath", remotePath);
            result.put("size", file.length());
            result.put("uploadTime", LocalDateTime.now());

            log.info("文件上传成功: {}", remotePath);
            return Result.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<File> downloadFile(String remotePath, String localPath) {
        log.info("从本地云存储下载文件: {} -> {}", remotePath, localPath);

        try {
            Path sourcePath = Paths.get(cloudStoragePath, remotePath);
            if (!Files.exists(sourcePath)) {
                return Result.error("文件不存在: " + remotePath);
            }

            Path targetPath = Paths.get(localPath);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件下载成功: {} -> {}", remotePath, localPath);
            return Result.success(targetPath.toFile());

        } catch (IOException e) {
            log.error("文件下载失败", e);
            return Result.error("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteFile(String remotePath) {
        log.info("从本地云存储删除文件: {}", remotePath);

        try {
            Path filePath = Paths.get(cloudStoragePath, remotePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", remotePath);
                return Result.success();
            } else {
                return Result.error("文件不存在: " + remotePath);
            }

        } catch (IOException e) {
            log.error("文件删除失败", e);
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> listFiles(String prefix) {
        log.info("列出本地云存储文件，前缀: {}", prefix);

        try {
            Path searchPath = Paths.get(cloudStoragePath, prefix != null ? prefix : "");
            if (!Files.exists(searchPath)) {
                return Result.success(new ArrayList<>());
            }

            List<Map<String, Object>> files;
            try (Stream<Path> paths = Files.walk(searchPath)) {
                files = paths
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        Map<String, Object> info = new HashMap<>();
                        try {
                            info.put("path", path.toString().replace(cloudStoragePath, ""));
                            info.put("name", path.getFileName().toString());
                            info.put("size", Files.size(path));
                            info.put("lastModified", LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()));
                        } catch (IOException ex) {
                            log.warn("获取文件信息失败: {}", path, ex);
                        }
                        return info;
                    })
                    .collect(Collectors.toList());
            }

            return Result.success(files);

        } catch (IOException e) {
            log.error("列出文件失败", e);
            return Result.error("列出文件失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> getFileInfo(String remotePath) {
        log.info("获取本地云存储文件信息: {}", remotePath);

        try {
            Path filePath = Paths.get(cloudStoragePath, remotePath);
            if (!Files.exists(filePath)) {
                return Result.error("文件不存在: " + remotePath);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("path", remotePath);
            info.put("name", filePath.getFileName().toString());
            info.put("size", Files.size(filePath));
            info.put("lastModified", LocalDateTime.ofInstant(
                Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault()));
            info.put("exists", true);

            return Result.success(info);

        } catch (IOException e) {
            log.error("获取文件信息失败", e);
            return Result.error("获取文件信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> fileExists(String remotePath) {
        Path filePath = Paths.get(cloudStoragePath, remotePath);
        boolean exists = Files.exists(filePath);
        return Result.success(exists);
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    @Override
    public Result<Boolean> checkAvailability() {
        try {
            Path path = Paths.get(cloudStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            boolean writable = Files.isWritable(path);
            return Result.success(writable);
        } catch (IOException e) {
            log.error("检查存储可用性失败", e);
            return Result.success(false);
        }
    }
}
