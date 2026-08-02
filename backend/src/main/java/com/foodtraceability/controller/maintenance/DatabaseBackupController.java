package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.DatabaseBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据库备份管理Controller
 */
@RestController
@RequestMapping("/v1/backup")
@Tag(name = "数据库备份管理", description = "数据库备份、恢复、管理相关接口")
public class DatabaseBackupController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupController.class);


    public DatabaseBackupController(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    private final DatabaseBackupService databaseBackupService;

    /**
     * 执行手动备份
     */
    @PostMapping("/manual")
    @Operation(summary = "执行手动备份", description = "手动触发数据库备份")
    public Result<String> manualBackup(
            @Parameter(description = "备份名称") @RequestParam(required = false) String backupName,
            @Parameter(description = "备份描述") @RequestParam(required = false) String description,
            @Parameter(description = "当前用户ID") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "当前用户名") @RequestHeader(value = "X-Username", required = false) String username) {
        // 优先使用用户名，如果没有则使用用户ID
        String currentUser = (username != null && !username.isEmpty()) ? username : (userId != null && !userId.isEmpty() ? userId : "system");
        log.info("【DatabaseBackupController】执行手动备份: name={}, desc={}, user={}", backupName, description, currentUser);
        return databaseBackupService.manualBackup(backupName, description, currentUser);
    }

    /**
     * 获取备份列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取备份列表", description = "获取所有数据库备份记录")
    public Result<List<DatabaseBackupService.BackupInfo>> getBackupList() {
        log.info("【DatabaseBackupController】获取备份列表");
        return databaseBackupService.getBackupList();
    }

    /**
     * 删除备份
     */
    @DeleteMapping("/{backupId}")
    @Operation(summary = "删除备份", description = "删除指定的数据库备份")
    public Result<Void> deleteBackup(
            @Parameter(description = "备份ID") @PathVariable String backupId) {
        log.info("【DatabaseBackupController】删除备份: {}", backupId);
        return databaseBackupService.deleteBackup(backupId);
    }

    /**
     * 恢复备份
     */
    @PostMapping("/restore/{backupId}")
    @Operation(summary = "恢复备份", description = "从指定备份恢复数据库")
    public Result<Void> restoreBackup(
            @Parameter(description = "备份ID") @PathVariable String backupId,
            @Parameter(description = "当前用户ID") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "当前用户名") @RequestHeader(value = "X-Username", required = false) String username) {
        // 优先使用用户名，如果没有则使用用户ID
        String currentUser = (username != null && !username.isEmpty()) ? username : (userId != null && !userId.isEmpty() ? userId : "system");
        log.info("【DatabaseBackupController】恢复备份: {}, user={}", backupId, currentUser);
        return databaseBackupService.restoreBackup(backupId, currentUser);
    }

    /**
     * 清理过期备份
     */
    @PostMapping("/cleanup")
    @Operation(summary = "清理过期备份", description = "清理指定天数之前的备份")
    public Result<Void> cleanupOldBackups(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") int retainDays) {
        log.info("【DatabaseBackupController】清理过期备份: retainDays={}", retainDays);
        return databaseBackupService.cleanupOldBackups(retainDays);
    }

    /**
     * 获取备份存储路径
     */
    @GetMapping("/storage-path")
    @Operation(summary = "获取备份存储路径", description = "获取当前备份文件存储路径")
    public Result<String> getBackupStoragePath() {
        log.info("【DatabaseBackupController】获取备份存储路径");
        return Result.success(databaseBackupService.getBackupStoragePath());
    }

    /**
     * 上传备份到云存储
     */
    @PostMapping("/upload-cloud/{backupId}")
    @Operation(summary = "上传备份到云存储", description = "将指定备份上传到云存储")
    public Result<Map<String, Object>> uploadBackupToCloud(
            @Parameter(description = "备份ID") @PathVariable String backupId) {
        log.info("【DatabaseBackupController】上传备份到云存储: {}", backupId);
        return databaseBackupService.uploadBackupToCloud(backupId);
    }

    /**
     * 从云存储下载备份
     */
    @PostMapping("/download-cloud/{backupId}")
    @Operation(summary = "从云存储下载备份", description = "从云存储下载指定备份到本地")
    public Result<String> downloadBackupFromCloud(
            @Parameter(description = "备份ID") @PathVariable String backupId) {
        log.info("【DatabaseBackupController】从云存储下载备份: {}", backupId);
        return databaseBackupService.downloadBackupFromCloud(backupId);
    }

    /**
     * 检查外键约束
     */
    @GetMapping("/check/foreign-keys")
    @Operation(summary = "检查外键约束", description = "检查数据库外键约束是否被违反")
    public Result<Map<String, Object>> checkForeignKeyConstraints() {
        log.info("【DatabaseBackupController】检查外键约束");
        return databaseBackupService.checkForeignKeyConstraints();
    }

    /**
     * 检查字符集和排序规则
     */
    @GetMapping("/check/charset")
    @Operation(summary = "检查字符集和排序规则", description = "检查数据库字符集和排序规则配置")
    public Result<Map<String, Object>> checkCharsetAndCollation() {
        log.info("【DatabaseBackupController】检查字符集和排序规则");
        return databaseBackupService.checkCharsetAndCollation();
    }

    /**
     * 检查数据类型和长度
     */
    @GetMapping("/check/data-types")
    @Operation(summary = "检查数据类型和长度", description = "检查数据库字段数据类型和长度配置")
    public Result<Map<String, Object>> checkDataTypesAndLength() {
        log.info("【DatabaseBackupController】检查数据类型和长度");
        return databaseBackupService.checkDataTypesAndLength();
    }

    /**
     * 验证备份完整性
     */
    @PostMapping("/verify/{backupId}")
    @Operation(summary = "验证备份完整性", description = "验证指定备份文件的完整性")
    public Result<Map<String, Object>> verifyBackupIntegrity(
            @Parameter(description = "备份ID") @PathVariable String backupId) {
        log.info("【DatabaseBackupController】验证备份完整性: {}", backupId);
        return databaseBackupService.verifyBackupIntegrity(backupId);
    }
}
