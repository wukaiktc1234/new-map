package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.FileAttachment;
import com.foodtraceability.mapper.FileAttachmentMapper;
import com.foodtraceability.service.FileAttachmentService;
import com.foodtraceability.utils.FileSecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FileAttachmentServiceImpl implements FileAttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentServiceImpl.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter NAMEDTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public FileAttachmentServiceImpl(FileAttachmentMapper fileAttachmentMapper) {
        this.fileAttachmentMapper = fileAttachmentMapper;
    }

    @Value("${file.upload-path:./data/uploads}")
    private String baseUploadPath;

    private final FileAttachmentMapper fileAttachmentMapper;

    @Override
    public FileAttachment uploadFile(MultipartFile file, String businessType, String businessId,
                                     String userId, String username) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        FileSecurityUtil.ValidationResult validation = FileSecurityUtil.validateFile(file);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        try {
            String originalName = FileSecurityUtil.sanitizeFileName(file.getOriginalFilename());
            String extension = getExtension(originalName).toLowerCase();
            String contentType = validation.getDetectedContentType();
            long fileSize = validation.getFileSize();
            String storedName = generateStoredName(extension);
            String datePath = LocalDateTime.now().format(DTF);
            String relativePath = datePath + "/" + storedName;
            Path fullPath = Paths.get(baseUploadPath, relativePath);

            Files.createDirectories(fullPath.getParent());
            Files.copy(file.getInputStream(), fullPath);

            String md5 = calculateMd5(fullPath);
            String sha256 = calculateSha256(fullPath);

            FileAttachment existing = getByMd5Internal(md5);
            if (existing != null) {
                Files.deleteIfExists(fullPath);
                logger.info("文件MD5重复，复用已有记录: attachmentId={}, md5={}", existing.getAttachmentId(), md5);
                return existing;
            }

            Long userTotalSize = fileAttachmentMapper.sumFileSizeByUser(userId != null ? userId : "");
            final long maxStorage = 500 * 1024 * 1024L;
            if (userTotalSize + fileSize > maxStorage) {
                Files.deleteIfExists(fullPath);
                throw new IllegalStateException("用户存储空间不足(剩余" +
                    ((maxStorage - userTotalSize) / 1024 / 1024) + "MB)");
            }

            FileAttachment attachment = new FileAttachment();
            attachment.setBusinessType(businessType != null ? businessType : "general");
            attachment.setBusinessId(businessId != null ? businessId : "");
            attachment.setOriginalName(originalName);
            attachment.setStoredName(storedName);
            attachment.setFilePath(relativePath);
            attachment.setFileExtension(extension);
            attachment.setContentType(contentType);
            attachment.setFileSize(fileSize);
            attachment.setFileHashMd5(md5);
            attachment.setFileHashSha256(sha256);
            attachment.setStorageType("LOCAL");
            attachment.setUploadUserId(userId != null ? userId : "");
            attachment.setUploadUsername(username != null ? username : "");
            attachment.setDownloadCount(0L);
            attachment.setStatus("active");
            attachment.setCreatedAt(LocalDateTime.now());
            attachment.setUpdatedAt(LocalDateTime.now());

            if (contentType.startsWith("image/") && !contentType.equals("image/svg+xml")) {
                attachment.setThumbnailPath(relativePath);
            }

            fileAttachmentMapper.insert(attachment);
            logger.info("文件上传成功: id={}, name={}, size={}, user={}",
                attachment.getAttachmentId(), originalName, fileSize, userId);
            return attachment;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileAttachment> uploadFiles(MultipartFile[] files, String businessType, String businessId,
                                            String userId, String username) {
        List<FileAttachment> results = new ArrayList<>();
        if (files == null || files.length == 0) return results;
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    results.add(uploadFile(file, businessType, businessId, userId, username));
                } catch (IllegalArgumentException e) {
                    logger.warn("批量上传中跳过无效文件: {}", e.getMessage());
                }
            }
        }
        return results;
    }

    @Override
    public void downloadFile(Long attachmentId, HttpServletResponse response, String currentUserId,
                              boolean isAdmin) {
        FileAttachment attachment = fileAttachmentMapper.selectById(attachmentId);
        if (attachment == null || isDeleted(attachment)) {
            throw new IllegalArgumentException("文件不存在或已删除");
        }

        if (!isAdmin && currentUserId != null && !currentUserId.equals(attachment.getUploadUserId())) {
            logger.warn("越权下载尝试: fileId={}, requester={}, owner={}",
                attachmentId, currentUserId, attachment.getUploadUserId());
            throw new SecurityException("无权访问该文件");
        }

        Path filePath = Paths.get(baseUploadPath, attachment.getFilePath());
        if (!isSafeFilePath(filePath)) {
            logger.warn("非法路径访问: id={}, path={}", attachmentId, attachment.getFilePath());
            throw new SecurityException("非法路径访问");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("物理文件不存在");
        }

        fileAttachmentMapper.incrementDownloadCount(attachmentId);

        String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8)
            .replace("+", "%20");
        response.setContentType(safeContentType(attachment));
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(attachment.getFileSize());

        try (InputStream is = new FileInputStream(filePath.toFile());
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            logger.error("文件下载失败: id={}, error={}", attachmentId, e.getMessage());
            throw new RuntimeException("文件下载失败");
        }
    }

    @Override
    public void downloadByPath(String filePath, String originalName, HttpServletResponse response) {
        Path fullPath = Paths.get(baseUploadPath, sanitizePath(filePath));
        if (!fullPath.startsWith(Paths.get(baseUploadPath).normalize())) {
            throw new SecurityException("非法路径访问");
        }
        if (!Files.exists(fullPath)) {
            throw new IllegalArgumentException("文件不存在");
        }

        long fileSize;
        try {
            fileSize = Files.size(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("获取文件大小失败");
        }

        String fileName = originalName != null ? originalName : fullPath.getFileName().toString();
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(probeContentType(fullPath));
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(fileSize);

        try (InputStream is = new FileInputStream(fullPath.toFile());
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败");
        }
    }

    @Override
    public FileAttachment getFileInfo(Long attachmentId, String currentUserId, boolean isAdmin) {
        FileAttachment attachment = fileAttachmentMapper.selectById(attachmentId);
        if (attachment == null || isDeleted(attachment)) return null;

        if (!isAdmin && currentUserId != null && !currentUserId.equals(attachment.getUploadUserId())) {
            return null;
        }
        return attachment;
    }

    @Override
    public IPage<FileAttachment> getFilePage(Page<FileAttachment> page, String businessType, String businessId,
                                              String uploadUserId, String contentType, String originalName,
                                              String status, LocalDateTime startTime, LocalDateTime endTime) {
        return fileAttachmentMapper.selectFilePage(page, businessType, businessId,
            uploadUserId, contentType, truncateString(originalName), status, startTime, endTime);
    }

    @Override
    public List<FileAttachment> getFilesByBusiness(String businessType, String businessId) {
        return fileAttachmentMapper.selectByBusinessId(businessType, businessId);
    }

    @Override
    public boolean deleteFile(Long attachmentId, String userId, boolean isAdmin) {
        FileAttachment attachment = fileAttachmentMapper.selectById(attachmentId);
        if (attachment == null || isDeleted(attachment)) return false;

        if (!isAdmin && userId != null && !userId.equals(attachment.getUploadUserId())) {
            logger.warn("越权删除尝试: fileId={}, requester={}, owner={}",
                attachmentId, userId, attachment.getUploadUserId());
            throw new SecurityException("无权删除该文件");
        }

        int result = fileAttachmentMapper.deleteById(attachmentId);
        if (result > 0) {
            logger.info("逻辑删除文件: id={}, name={}, operator={}", attachmentId, attachment.getOriginalName(), userId);
        }
        return result > 0;
    }

    @Override
    public boolean deleteFiles(List<Long> attachmentIds, String userId, boolean isAdmin) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return false;
        int result = fileAttachmentMapper.deleteBatchIds(attachmentIds);
        logger.info("批量删除文件: count={}, ids={}, operator={}", result, attachmentIds, userId);
        return result > 0;
    }

    @Override
    public Map<String, Object> getStorageStats(String userId) {
        Map<String, Object> stats = new HashMap<>();
        final long maxStorage = 500 * 1024 * 1024L;
        String uid = userId != null ? userId : "";
        stats.put("totalFiles", fileAttachmentMapper.countByUser(uid));
        long totalSize = fileAttachmentMapper.sumFileSizeByUser(uid);
        stats.put("totalSize", totalSize);
        stats.put("maxStorage", maxStorage);
        stats.put("usedPercent", maxStorage > 0 ?
            Math.round((double) totalSize / maxStorage * 10000) / 100.0 : 0.0);
        LambdaQueryWrapper<FileAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileAttachment::getUploadUserId, uid)
               .eq(FileAttachment::getDeleted, 0)
               .orderByDesc(FileAttachment::getCreatedAt)
               .last("LIMIT 10");
        stats.put("recentFiles", fileAttachmentMapper.selectList(wrapper));
        return stats;
    }

    @Override
    public boolean existsByMd5(String md5) {
        if (md5 == null || md5.isEmpty()) return false;
        LambdaQueryWrapper<FileAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileAttachment::getFileHashMd5, md5)
               .eq(FileAttachment::getDeleted, 0)
               .last("LIMIT 1");
        return fileAttachmentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public FileAttachment getByMd5(String md5) {
        return getByMd5Internal(md5);
    }

    private FileAttachment getByMd5Internal(String md5) {
        if (md5 == null || md5.isEmpty()) return null;
        LambdaQueryWrapper<FileAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileAttachment::getFileHashMd5, md5)
               .eq(FileAttachment::getDeleted, 0)
               .last("LIMIT 1");
        return fileAttachmentMapper.selectOne(wrapper);
    }

    private boolean isDeleted(FileAttachment attachment) {
        return attachment.getDeleted() != null && attachment.getDeleted() == 1;
    }

    private boolean isSafeFilePath(Path filePath) {
        Path baseNormalized = Paths.get(baseUploadPath).normalize().toAbsolutePath();
        Path targetNormalized = filePath.normalize().toAbsolutePath();
        return targetNormalized.startsWith(baseNormalized);
    }

    private String safeContentType(FileAttachment attachment) {
        String ct = attachment.getContentType();
        if ("image/svg+xml".equals(ct)) {
            return "application/octet-stream";
        }
        return ct != null ? ct : "application/octet-stream";
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "bin";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "bin";
    }

    public static String generateStoredName(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now().format(NAMEDTF);
        return timestamp + "_" + uuid.substring(0, 8) + "." + extension;
    }

    private String calculateMd5(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(
                new FileInputStream(filePath.toFile()), md)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) { }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private String calculateSha256(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream dis = new DigestInputStream(
                new FileInputStream(filePath.toFile()), md)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) { }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private String probeContentType(Path filePath) {
        try {
            String probe = Files.probeContentType(filePath);
            if ("image/svg+xml".equals(probe)) {
                return "application/octet-stream";
            }
            return probe != null ? probe : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private String sanitizePath(String path) {
        if (path == null) return "";
        return path.replaceAll("\\.\\.", "").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String truncateString(String value) {
        if (value == null) return null;
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
}
