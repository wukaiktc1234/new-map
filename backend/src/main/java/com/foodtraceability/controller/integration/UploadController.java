package com.foodtraceability.controller.integration;

import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.Result;
import com.foodtraceability.utils.ImageUploadUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/upload")
@Tag(name = "文件上传API", description = "文件上传相关接口")
public class UploadController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UploadController.class);
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/image")
    @Operation(summary = "上传图片")
    @RequiresPermission(value = "system:upload:image", action = "upload")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("开始上传图片: {}", file.getOriginalFilename());
            ImageUploadUtils.UploadResult result = ImageUploadUtils.uploadImage(file, uploadDir);
            Map<String, Object> data = new HashMap<>();
            data.put("url", result.getUrl());
            data.put("fileName", result.getFileName());
            data.put("originalName", result.getOriginalName());
            data.put("fileSize", result.getFileSize());
            log.info("图片上传成功: {}", result.getUrl());
            return Result.success(data);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/image/{fileName}")
    @Operation(summary = "删除图片")
    @RequiresPermission(value = "system:upload:delete", action = "delete")
    public Result<Void> deleteImage(@PathVariable String fileName) {
        try {
            log.info("开始删除图片: {}", fileName);
            boolean deleted = ImageUploadUtils.deleteImage(uploadDir, fileName);
            if (deleted) {
                log.info("图片删除成功: {}", fileName);
                return Result.success();
            } else {
                log.warn("图片不存在或删除失败: {}", fileName);
                return Result.error("图片不存在或删除失败");
            }
        } catch (Exception e) {
            log.error("图片删除失败", e);
            return Result.error("图片删除失败: " + e.getMessage());
        }
    }
}
