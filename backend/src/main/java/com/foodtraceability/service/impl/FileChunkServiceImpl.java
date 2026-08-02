package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.FileAttachment;
import com.foodtraceability.mapper.FileAttachmentMapper;
import com.foodtraceability.service.FileChunkService;
import com.foodtraceability.utils.FileSecurityUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FileChunkServiceImpl implements FileChunkService {

    private static final Logger logger = LoggerFactory.getLogger(FileChunkServiceImpl.class);
    private static final long CHUNK_EXPIRE_HOURS = 24;
    private static final long MAX_TOTAL_SIZE = 200 * 1024 * 1024;

    public FileChunkServiceImpl(FileAttachmentMapper fileAttachmentMapper) {
        this.fileAttachmentMapper = fileAttachmentMapper;
    }

    @Value("${file.upload-path:./data/uploads}")
    private String baseUploadPath;
    @Value("${file.chunk-temp-path:./data/uploads/.chunks}")
    private String chunkTempPath;

    private final FileAttachmentMapper fileAttachmentMapper;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(chunkTempPath));
        } catch (IOException e) {
            logger.error("创建分片临时目录失败: {}", e.getMessage());
        }
    }

    @Override
    public String initChunkUpload(String fileName, long fileSize, String contentType,
                                   String businessType, String userId, String username, int totalChunks) {
        if (fileName == null || fileName.isEmpty()) throw new IllegalArgumentException("文件名不能为空");
        if (totalChunks <= 0 || totalChunks > 1000) throw new IllegalArgumentException("分片数量无效(1-1000)");
        if (fileSize > MAX_TOTAL_SIZE) throw new IllegalArgumentException("文件大小超过限制(最大200MB)");

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusHours(CHUNK_EXPIRE_HOURS);
        Path chunkDir = Paths.get(chunkTempPath, uploadId);

        try {
            Files.createDirectories(chunkDir);

            Path metaFile = chunkDir.resolve("_meta.json");
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("uploadId", uploadId);
            meta.put("fileName", FileSecurityUtil.sanitizeFileName(fileName));
            meta.put("fileSize", fileSize);
            meta.put("contentType", contentType != null ? contentType : "application/octet-stream");
            meta.put("businessType", businessType != null ? businessType : "general");
            meta.put("userId", userId != null ? userId : "");
            meta.put("username", username != null ? username : "");
            meta.put("totalChunks", totalChunks);
            meta.put("status", "uploading");
            meta.put("createdAt", LocalDateTime.now().toString());
            meta.put("expireAt", expireTime.toString());

            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta);
            Files.writeString(metaFile, json);

            logger.info("初始化分片上传: id={}, name={}, chunks={}, size={}",
                uploadId, fileName, totalChunks, fileSize);
            return uploadId;
        } catch (Exception e) {
            cleanupDir(chunkDir);
            throw new RuntimeException("初始化分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean uploadChunk(String uploadId, int chunkNumber, MultipartFile chunk) {
        if (uploadId == null || uploadId.isEmpty()) return false;
        if (chunk == null || chunk.isEmpty()) return false;
        if (chunkNumber < 0) return false;

        Path chunkDir = Paths.get(chunkTempPath, uploadId);
        if (!Files.exists(chunkDir)) {
            throw new IllegalArgumentException("分片上传会话不存在或已过期: " + uploadId);
        }

        Path chunkFile = chunkDir.resolve("chunk_" + String.format("%05d", chunkNumber));

        try {
            if (Files.exists(chunkFile)) {
                Files.delete(chunkFile);
            }
            Files.copy(chunk.getInputStream(), chunkFile, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("分片上传成功: id={}, chunk={}", uploadId, chunkNumber);
            return true;
        } catch (IOException e) {
            logger.error("分片写入失败: id={}, chunk={}, error={}", uploadId, chunkNumber, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getUploadProgress(String uploadId) {
        Map<String, Object> progress = new HashMap<>();
        Path chunkDir = Paths.get(chunkTempPath, uploadId);

        if (!Files.exists(chunkDir)) {
            progress.put("status", "not_found");
            return progress;
        }

        try {
            Path metaFile = chunkDir.resolve("_meta.json");
            if (!Files.exists(metaFile)) {
                progress.put("status", "corrupted");
                return progress;
            }

            String json = Files.readString(metaFile);
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, Map.class);

            int totalChunks = ((Number) meta.getOrDefault("totalChunks", 0)).intValue();
            int uploadedChunks = 0;
            long uploadedSize = 0;

            if (Files.isDirectory(chunkDir)) {
                try (var entries = Files.newDirectoryStream(chunkDir)) {
                    for (Path entry : entries) {
                        String fn = entry.getFileName().toString();
                        if (fn.startsWith("chunk_") && !fn.equals("_meta.json")) {
                            uploadedChunks++;
                            uploadedSize += Files.size(entry);
                        }
                    }
                }
            }

            progress.putAll(meta);
            progress.remove("userId");
            progress.remove("username");
            progress.put("uploadedChunks", uploadedChunks);
            progress.put("totalChunks", totalChunks);
            progress.put("progressPercent", totalChunks > 0 ? Math.round(uploadedChunks * 100.0 / totalChunks) : 0);
            progress.put("uploadedSize", uploadedSize);
            progress.put("status", uploadedChunks >= totalChunks && totalChunks > 0 ? "ready_to_merge" : "uploading");
        } catch (Exception e) {
            logger.error("获取上传进度失败: id={}", uploadId, e);
            progress.put("status", "error");
            progress.put("error", e.getMessage());
        }
        return progress;
    }

    @Override
    public FileAttachment mergeChunks(String uploadId) {
        Path chunkDir = Paths.get(chunkTempPath, uploadId);
        if (!Files.exists(chunkDir)) {
            throw new IllegalArgumentException("分片会话不存在: " + uploadId);
        }

        try {
            Path metaFile = chunkDir.resolve("_meta.json");
            String json = Files.readString(metaFile);
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, Map.class);

            String originalName = (String) meta.get("fileName");
            String contentType = (String) meta.get("contentType");
            String businessType = (String) meta.get("businessType");
            String userId = (String) meta.getOrDefault("userId", "");
            String username = (String) meta.getOrDefault("username", "");
            long fileSize = ((Number) meta.getOrDefault("fileSize", 0L)).longValue();

            String extension = FileSecurityUtil.getExtension(originalName).toLowerCase();
            String storedName = FileAttachmentServiceImpl.generateStoredName(extension);
            String datePath = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = datePath + "/" + storedName;
            Path targetPath = Paths.get(baseUploadPath, relativePath);

            Files.createDirectories(targetPath.getParent());

            List<Path> chunks = new ArrayList<>();
            try (var entries = Files.newDirectoryStream(chunkDir)) {
                for (Path entry : entries) {
                    String fn = entry.getFileName().toString();
                    if (fn.startsWith("chunk_")) {
                        chunks.add(entry);
                    }
                }
            }

            chunks.sort(Comparator.comparing(p -> p.getFileName().toString()));

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetPath));
                 DigestInputStream dis = new DigestInputStream(
                     new BufferedInputStream(new FileInputStream(chunks.get(0).toFile())), md5)) {

                byte[] buffer = new byte[8192];
                for (Path chunk : chunks) {
                    try (InputStream cis = new BufferedInputStream(new FileInputStream(chunk.toFile()))) {
                        int bytesRead;
                        while ((bytesRead = cis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                            sha256.update(buffer, 0, bytesRead);
                        }
                    }
                }
            }

            StringBuilder md5Sb = new StringBuilder();
            for (byte b : md5.digest()) md5Sb.append(String.format("%02x", b & 0xff));
            String md5Hash = md5Sb.toString();

            StringBuilder shaSb = new StringBuilder();
            for (byte b : sha256.digest()) shaSb.append(String.format("%02x", b & 0xff));
            String shaHash = shaSb.toString();

            FileAttachment attachment = new FileAttachment();
            attachment.setBusinessType(businessType);
            attachment.setOriginalName(originalName);
            attachment.setStoredName(storedName);
            attachment.setFilePath(relativePath);
            attachment.setFileExtension(extension);
            attachment.setContentType(contentType);
            attachment.setFileSize(fileSize > 0 ? fileSize : Files.size(targetPath));
            attachment.setFileHashMd5(md5Hash);
            attachment.setFileHashSha256(shaHash);
            attachment.setStorageType("LOCAL");
            attachment.setUploadUserId(userId);
            attachment.setUploadUsername(username);
            attachment.setDownloadCount(0L);
            attachment.setStatus("active");
            attachment.setCreatedAt(LocalDateTime.now());
            attachment.setUpdatedAt(LocalDateTime.now());

            if (contentType.startsWith("image/")) {
                attachment.setThumbnailPath(relativePath);
            }

            fileAttachmentMapper.insert(attachment);

            cleanupDir(chunkDir);

            logger.info("分片合并完成: id={}, name={}, size={}, md5={}",
                attachment.getAttachmentId(), originalName, attachment.getFileSize(), md5Hash);
            return attachment;

        } catch (Exception e) {
            logger.error("分片合并失败: id={}, error={}", uploadId, e.getMessage(), e);
            throw new RuntimeException("分片合并失败: " + e.getMessage());
        }
    }

    @Override
    public boolean cancelUpload(String uploadId) {
        Path chunkDir = Paths.get(chunkTempPath, uploadId);
        if (!Files.exists(chunkDir)) return false;
        cleanupDir(chunkDir);
        logger.info("取消分片上传: id={}", uploadId);
        return true;
    }

    @Override
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupExpiredChunks() {
        logger.info("开始清理过期分片数据...");
        int cleaned = 0;
        Path tempRoot = Paths.get(chunkTempPath);
        if (!Files.exists(tempRoot)) return;

        try (var dirs = Files.newDirectoryStream(tempRoot)) {
            for (Path dir : dirs) {
                if (!Files.isDirectory(dir)) continue;
                Path metaFile = dir.resolve("_meta.json");
                if (Files.exists(metaFile)) {
                    try {
                        String json = Files.readString(metaFile);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> meta = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(json, Map.class);
                        String expireAtStr = (String) meta.get("expireAt");
                        if (expireAtStr != null) {
                            LocalDateTime expireAt = LocalDateTime.parse(expireAtStr);
                            if (LocalDateTime.now().isAfter(expireAt)) {
                                cleanupDir(dir);
                                cleaned++;
                            }
                        }
                    } catch (Exception e) {
                        if (isExpiredDir(dir)) {
                            cleanupDir(dir);
                            cleaned++;
                        }
                    }
                } else if (isExpiredDir(dir)) {
                    cleanupDir(dir);
                    cleaned++;
                }
            }
        } catch (Exception e) {
            logger.error("清理过期分片失败: {}", e.getMessage());
        }
        logger.info("过期分片清理完成: 清理{}个目录", cleaned);
    }

    private boolean isExpiredDir(Path dir) {
        try {
            return Files.getLastModifiedTime(dir).toInstant()
                .toEpochMilli() < System.currentTimeMillis() - CHUNK_EXPIRE_HOURS * 3600000;
        } catch (Exception e) {
            return true;
        }
    }

    private void cleanupDir(Path dir) {
        try {
            if (Files.isDirectory(dir)) {
                try (var entries = Files.newDirectoryStream(dir)) {
                    for (Path entry : entries) {
                        Files.deleteIfExists(entry);
                    }
                }
            }
            Files.deleteIfExists(dir);
        } catch (Exception e) {
            logger.warn("清理目录失败: {}", dir, e);
        }
    }

    protected static String generateStoredNameInternal(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + uuid.substring(0, 8) + "." + extension;
    }
}
