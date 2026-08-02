package com.foodtraceability.controller.integration;

import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.FileAttachment;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.FileChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "分片文件上传", description = "大文件分片上传、合并、进度查询")
@RestController
@RequestMapping("/v1/files/chunk")
public class FileChunkController {

    private static final Logger logger = LoggerFactory.getLogger(FileChunkController.class);


    public FileChunkController(FileChunkService fileChunkService) {
        this.fileChunkService = fileChunkService;
    }

    private final FileChunkService fileChunkService;

    @Operation(summary = "初始化分片上传")
    @PostMapping("/init")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> initUpload(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "原始文件名") @RequestParam String fileName,
            @Parameter(description = "总文件大小(字节)") @RequestParam long fileSize,
            @Parameter(description = "MIME类型") @RequestParam(required = false) String contentType,
            @Parameter(description = "业务类型") @RequestParam(required = false, defaultValue = "general") String businessType,
            @Parameter(description = "总分片数") @RequestParam(defaultValue = "1") int totalChunks) {
        try {
            String uploadId = fileChunkService.initChunkUpload(
                fileName, fileSize, contentType, businessType,
                currentUser.getUserId(), currentUser.getUsername(), totalChunks);
            return Result.success(Map.of("uploadId", uploadId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("初始化分片上传失败", e);
            return Result.error("初始化失败");
        }
    }

    @Operation(summary = "上传单个分片")
    @PostMapping("/{uploadId}/chunk/{chunkNumber}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> uploadChunk(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "上传会话ID") @PathVariable String uploadId,
            @Parameter(description = "分片序号(从0开始)") @PathVariable int chunkNumber,
            @Parameter(description = "分片数据") @RequestParam("chunk") MultipartFile chunk) {
        boolean success = fileChunkService.uploadChunk(uploadId, chunkNumber, chunk);
        if (success) {
            return Result.success();
        }
        return Result.error("分片上传失败");
    }

    @Operation(summary = "查询上传进度")
    @GetMapping("/{uploadId}/progress")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getProgress(
            @Parameter(description = "上传会话ID") @PathVariable String uploadId) {
        Map<String, Object> progress = fileChunkService.getUploadProgress(uploadId);
        return Result.success(progress);
    }

    @Operation(summary = "合并所有分片")
    @PostMapping("/{uploadId}/merge")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "分片上传-合并文件", operationType = OperationType.CREATE, module = "文件管理")
    public Result<FileAttachment> mergeChunks(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "上传会话ID") @PathVariable String uploadId) {
        try {
            FileAttachment result = fileChunkService.mergeChunks(uploadId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("合并分片失败: id={}", uploadId, e);
            return Result.error("合并失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消分片上传")
    @DeleteMapping("/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "取消分片上传", operationType = OperationType.DELETE, module = "文件管理")
    public Result<Void> cancelUpload(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "上传会话ID") @PathVariable String uploadId) {
        boolean success = fileChunkService.cancelUpload(uploadId);
        if (success) {
            return Result.success();
        }
        return Result.error(404, "会话不存在或已清理");
    }
}
