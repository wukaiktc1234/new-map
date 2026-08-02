package com.foodtraceability.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileSecurityUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileSecurityUtil.class);

    private static final long MAX_SINGLE_FILE_SIZE = 50 * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_DOCUMENT_SIZE = 20 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain", "text/csv", "text/xml",
        "application/json"
    );
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "war", "class",
        "php", "asp", "aspx", "jsp", "cgi", "py", "rb", "pl"
    );

    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
        "image/jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "image/png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47},
        "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46},
        "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}
    );

    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        private String detectedContentType;
        private long fileSize;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public ValidationResult(boolean valid, String errorMessage, String detectedContentType, long fileSize) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.detectedContentType = detectedContentType;
            this.fileSize = fileSize;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getDetectedContentType() { return detectedContentType; }
        public long getFileSize() { return fileSize; }

        public static ValidationResult ok(String contentType, long size) {
            return new ValidationResult(true, null, contentType, size);
        }

        public static ValidationResult fail(String msg) {
            return new ValidationResult(false, msg);
        }
    }

    public static ValidationResult validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.fail("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            return ValidationResult.fail("文件名不能为空");
        }

        String extension = getExtension(originalName).toLowerCase();
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            logger.warn("拒绝上传危险文件类型: ext={}, name={}", extension, originalName);
            return ValidationResult.fail("不允许的文件类型: ." + extension);
        }

        if (containsPathTraversal(originalName)) {
            logger.warn("检测到路径遍历攻击尝试: name={}", originalName);
            return ValidationResult.fail("文件名包含非法字符");
        }

        long size = file.getSize();
        if (size <= 0) {
            return ValidationResult.fail("文件大小无效");
        }

        String declaredType = file.getContentType();
        if (declaredType == null) {
            declaredType = "application/octet-stream";
        }

        if (size > MAX_SINGLE_FILE_SIZE) {
            return ValidationResult.fail("文件大小超过限制(最大" + (MAX_SINGLE_FILE_SIZE / 1024 / 1024) + "MB)");
        }

        if (ALLOWED_IMAGE_TYPES.contains(declaredType)) {
            if (size > MAX_IMAGE_SIZE) {
                return ValidationResult.fail("图片大小超过限制(最大" + (MAX_IMAGE_SIZE / 1024 / 1024) + "MB)");
            }
        } else if (ALLOWED_DOCUMENT_TYPES.contains(declaredType)) {
            if (size > MAX_DOCUMENT_SIZE) {
                return ValidationResult.fail("文档大小超过限制(最大" + (MAX_DOCUMENT_SIZE / 1024 / 1024) + "MB)");
            }
        } else {
            return ValidationResult.fail("不支持的文件类型: " + declaredType);
        }

        String detectedType = detectMagicNumber(file);
        if (detectedType != null && !declaredType.startsWith(detectedType.split("/")[0])) {
            logger.warn("MIME类型不匹配: 声明={}, 检测={}", declaredType, detectedType);
            return ValidationResult.fail("文件内容与声明的类型不匹配");
        }

        return ValidationResult.ok(declaredType, size);
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "unnamed_" + System.currentTimeMillis();
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_")
                       .replaceAll("\\.{2,}", ".")
                       .replaceAll("^\\.+|\\.+$", "");
        if (fileName.length() > 200) {
            String ext = getExtension(fileName);
            fileName = fileName.substring(0, Math.min(196, fileName.length() - ext.length() - 1)) + "." + ext;
        }
        return fileName.isEmpty() ? "unnamed_" + System.currentTimeMillis() : fileName;
    }

    public static String sanitizePath(String relativePath) {
        if (relativePath == null) return "";
        return relativePath.replaceAll("\\.\\.", "")
                         .replaceAll("\\\\+", "/")
                         .replaceAll("[\\\\/:*?\"<>|]", "_")
                         .replaceAll("^/+", "")
                         .replaceAll("/+$", "");
    }

    public static boolean isSafePath(String basePath, String requestedPath) {
        try {
            Path base = Paths.get(basePath).normalize().toAbsolutePath();
            Path target = Paths.get(basePath, sanitizePath(requestedPath)).normalize().toAbsolutePath();
            return target.startsWith(base);
        } catch (Exception e) {
            return false;
        }
    }

    private static String detectMagicNumber(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) return null;

            for (Map.Entry<String, byte[]> entry : MAGIC_NUMBERS.entrySet()) {
                byte[] magic = entry.getValue();
                if (read >= magic.length) {
                    boolean match = true;
                    for (int i = 0; i < magic.length; i++) {
                        if ((header[i] & 0xFF) != (magic[i] & 0xFF)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) return entry.getKey();
                }
            }
            return null;
        } catch (IOException e) {
            logger.debug("魔数检测失败: {}", e.getMessage());
            return null;
        }
    }

    private static boolean containsPathTraversal(String fileName) {
        return fileName.contains("..") || fileName.contains("/") ||
               fileName.contains("\\") || fileName.contains("\0") ||
               fileName.startsWith(".") && !fileName.startsWith(".");
    }

    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "bin";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "bin";
    }
}
