package com.foodtraceability.controller.integration;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.FileAttachment;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.FileAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "文件附件管理", description = "文件上传、下载、查询、删除接口")
@RestController
@RequestMapping("/v1/files")
public class FileAttachmentController {

    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentController.class);


    public FileAttachmentController(FileAttachmentService fileAttachmentService) {
        this.fileAttachmentService = fileAttachmentService;
    }

    private final FileAttachmentService fileAttachmentService;

    @Operation(summary = "上传单个文件")
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "上传文件", operationType = OperationType.CREATE, module = "文件管理")
    public Result<FileAttachment> uploadFile(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型") @RequestParam(required = false, defaultValue = "general") String businessType,
            @Parameter(description = "关联业务ID") @RequestParam(required = false) String businessId) {
        try {
            FileAttachment result = fileAttachmentService.uploadFile(
                file, businessType, businessId,
                currentUser.getUserId(), currentUser.getUsername());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            logger.error("上传文件失败", e);
            return Result.error("上传文件失败");
        }
    }

    @Operation(summary = "批量上传文件")
    @PostMapping("/upload/batch")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "批量上传文件", operationType = OperationType.CREATE, module = "文件管理")
    public Result<List<FileAttachment>> uploadFiles(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "文件列表") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "业务类型") @RequestParam(required = false, defaultValue = "general") String businessType,
            @Parameter(description = "关联业务ID") @RequestParam(required = false) String businessId) {
        try {
            List<FileAttachment> results = fileAttachmentService.uploadFiles(
                files, businessType, businessId,
                currentUser.getUserId(), currentUser.getUsername());
            return Result.success(results);
        } catch (Exception e) {
            logger.error("批量上传失败", e);
            return Result.error("批量上传失败");
        }
    }

    @Operation(summary = "下载文件")
    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "下载文件", operationType = OperationType.VIEW, module = "文件管理")
    public void downloadFile(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "附件ID") @PathVariable Long attachmentId,
            HttpServletResponse response) {
        try {
            boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            fileAttachmentService.downloadFile(attachmentId, response,
                currentUser.getUserId(), isAdmin);
        } catch (IllegalArgumentException e) {
            sendError(response, 404, e.getMessage());
        } catch (SecurityException e) {
            sendError(response, 403, "无权访问该文件");
        } catch (Exception e) {
            logger.error("下载文件失败: id={}", attachmentId, e);
            sendError(response, 500, "下载失败");
        }
    }

    @Operation(summary = "获取文件信息")
    @GetMapping("/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    public Result<FileAttachment> getFileInfo(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        try {
            boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            FileAttachment file = fileAttachmentService.getFileInfo(attachmentId,
                currentUser.getUserId(), isAdmin);
            if (file == null) {
                return Result.error(404, "文件不存在或无权访问");
            }
            return Result.success(file);
        } catch (Exception e) {
            logger.error("获取文件信息失败: id={}", attachmentId, e);
            return Result.error("获取文件信息失败");
        }
    }

    @Operation(summary = "分页查询文件列表")
    @GetMapping
    @PreAuthorize("hasAuthority('file:query')")
    public Result<IPage<FileAttachment>> getFileList(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "业务类型") @RequestParam(required = false) String businessType,
            @Parameter(description = "业务ID") @RequestParam(required = false) String businessId,
            @Parameter(description = "上传用户ID") @RequestParam(required = false) String uploadUserId,
            @Parameter(description = "MIME类型") @RequestParam(required = false) String contentType,
            @Parameter(description = "原始文件名(模糊)") @RequestParam(required = false) String originalName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<FileAttachment> page = new Page<>(current, size);
            IPage<FileAttachment> result = fileAttachmentService.getFilePage(
                page, businessType, businessId, uploadUserId,
                contentType, originalName, status, startTime, endTime);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询文件列表失败", e);
            return Result.error("查询文件列表失败");
        }
    }

    @Operation(summary = "按业务查询关联文件")
    @GetMapping("/business/{businessType}/{businessId}")
    @PreAuthorize("isAuthenticated()")
    public Result<List<FileAttachment>> getFilesByBusiness(
            @Parameter(description = "业务类型") @PathVariable String businessType,
            @Parameter(description = "业务ID") @PathVariable String businessId) {
        List<FileAttachment> files = fileAttachmentService.getFilesByBusiness(businessType, businessId);
        return Result.success(files);
    }

    @Operation(summary = "删除单个文件")
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "删除文件", operationType = OperationType.DELETE, module = "文件管理")
    public Result<Void> deleteFile(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        try {
            boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean success = fileAttachmentService.deleteFile(attachmentId,
                currentUser.getUserId(), isAdmin);
            if (success) {
                return Result.success();
            }
            return Result.error(404, "文件不存在或删除失败");
        } catch (SecurityException e) {
            return Result.error(403, "无权删除该文件");
        } catch (Exception e) {
            logger.error("删除文件失败", e);
            return Result.error("删除失败");
        }
    }

    @Operation(summary = "批量删除文件")
    @DeleteMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "批量删除文件", operationType = OperationType.DELETE, module = "文件管理")
    public Result<Map<String, Object>> deleteFiles(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<Long> attachmentIds) {
        try {
            boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean success = fileAttachmentService.deleteFiles(attachmentIds,
                currentUser.getUserId(), isAdmin);
            if (success) {
                return Result.success(Map.of("deletedCount", attachmentIds.size()));
            }
            return Result.error("批量删除失败");
        } catch (SecurityException e) {
            return Result.error(403, "无权删除部分文件");
        } catch (Exception e) {
            logger.error("批量删除失败", e);
            return Result.error("批量删除失败");
        }
    }

    @Operation(summary = "获取用户存储统计")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getStorageStats(
            @AuthenticationPrincipal SecurityUser currentUser) {
        Map<String, Object> stats = fileAttachmentService.getStorageStats(currentUser.getUserId());
        return Result.success(stats);
    }

    @Operation(summary = "检查MD5是否已存在（去重）")
    @GetMapping("/exists/md5/{md5}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> checkMd5Exists(
            @Parameter(description = "MD5哈希值") @PathVariable String md5) {
        boolean exists = fileAttachmentService.existsByMd5(md5);
        FileAttachment existing = exists ? fileAttachmentService.getByMd5(md5) : null;
        Map<String, Object> result = new HashMap<>();
        result.put("exists", exists);
        if (existing != null) {
            result.put("attachmentId", existing.getAttachmentId());
            result.put("originalName", existing.getOriginalName());
            result.put("fileSize", existing.getFileSize());
        }
        return Result.success(result);
    }

    private void sendError(HttpServletResponse response, int code, String message) {
        try {
            response.sendError(code, message);
        } catch (Exception e) {
            logger.error("发送错误响应失败", e);
        }
    }
}
