package com.foodtraceability.controller.maintenance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.BackupCreateDTO;
import com.foodtraceability.dto.BackupQueryDTO;
import com.foodtraceability.dto.BackupRestoreDTO;
import com.foodtraceability.entity.BackupRestoreLog;
import com.foodtraceability.entity.DataBackupRecord;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.DataBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据备份与恢复控制器
 * 提供数据备份、恢复、管理的REST API接口
 *
 * 爆炸半径权限分级:
 * - \ud83d\udfe2 低风险: 查看备份列表、获取统计信息 (backup:query)
 * - \ud83d\udfe1 中风险: 创建备份、下载文件、配置策略 (backup:create, backup:download, backup:config)
 * - \ud83e\udc19 中高风险: 删除备份 (backup:delete) - 需要强确认
 * - \ud83d\udd34 高风险: 恢复数据 (backup:restore) - 需要二次密码确认+强确认
 */
@Tag(name = "数据备份与恢复", description = "数据库备份创建、恢复操作、存储管理接口")
@RestController
@RequestMapping("/v1/data-backup")
public class DataBackupController {

    private static final Logger logger = LoggerFactory.getLogger(DataBackupController.class);


    public DataBackupController(DataBackupService dataBackupService) {
        this.dataBackupService = dataBackupService;
    }

    private final DataBackupService dataBackupService;

    // ==================== 备份管理接口 ====================

    /**
     * 创建新的数据备份
     * 爆炸半径: \ud83d\udfe1 中(读全部库+写磁盘)
     * 权限要求: backup:create
     * 确认机制: 普通确认
     */
    @Operation(summary = "创建数据备份", description = "创建全量/增量/仅结构数据库备份")
    @PostMapping("/backups")
    @PreAuthorize("hasAuthority('backup:create')")
    public Result<DataBackupRecord> createBackup(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody BackupCreateDTO createDTO) {

        try {
            logger.info("[备份操作] 用户 {}({}) 创建备份: 名称={}, 类型={}",
                    currentUser.getUsername(), currentUser.getUserId(),
                    createDTO.getBackupName(), createDTO.getBackupType());

            DataBackupRecord record = dataBackupService.createBackup(
                    createDTO, Long.parseLong(currentUser.getUserId()), currentUser.getUsername());

            return Result.success(record, "备份创建成功");
        } catch (Exception e) {
            logger.error("创建备份失败: {}", e.getMessage(), e);
            return Result.error("创建备份失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询备份记录列表
     * 爆炸半径: \ud83d\udfe2 低(只读)
     * 权限要求: backup:query
     * 确认机制: 无需确认
     */
    @Operation(summary = "查询备份列表", description = "分页查询数据备份记录，支持多条件过滤")
    @GetMapping("/backups")
    @PreAuthorize("hasAuthority('backup:query')")
    public Result<IPage<DataBackupRecord>> getBackupList(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "备份名称") @RequestParam(required = false) String backupName,
            @Parameter(description = "备份类型") @RequestParam(required = false) Integer backupType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "存储位置") @RequestParam(required = false) String storageLocation,
            @Parameter(description = "触发方式") @RequestParam(required = false) String triggeredBy,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        try {
            // 参数校验
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            // 构建查询条件
            BackupQueryDTO query = new BackupQueryDTO();
            query.setBackupName(backupName);
            query.setBackupType(backupType);
            query.setStatus(status);
            query.setStorageLocation(storageLocation);
            query.setTriggeredBy(triggeredBy);
            query.setStartTime(startTime);
            query.setEndTime(endTime);
            query.setCurrent(current);
            query.setSize(size);

            Page<DataBackupRecord> page = new Page<>(current, size);
            IPage<DataBackupRecord> result = dataBackupService.getBackupList(page, query);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询备份列表失败: {}", e.getMessage(), e);
            return Result.error("查询备份列表失败");
        }
    }

    /**
     * 获取备份详情
     * 爆炸半径: \ud83d\udfe2 低(只读)
     * 权限要求: backup:query
     */
    @Operation(summary = "获取备份详情", description = "根据ID获取备份记录的详细信息")
    @GetMapping("/backups/{backupId}")
    @PreAuthorize("hasAuthority('backup:query')")
    public Result<DataBackupRecord> getBackupDetail(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "备份ID") @PathVariable Long backupId) {

        try {
            DataBackupRecord record = dataBackupService.getBackupDetail(backupId);
            return Result.success(record);
        } catch (Exception e) {
            logger.error("获取备份详情失败: backupId={}", backupId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取备份详情失败");
        }
    }

    /**
     * 逻辑删除备份记录
     * 爆炸半径: \ud83e\udc19 中高(不可逆)
     * 权限要求: backup:delete
     * 确认机制: 强确认（前端二次弹窗）
     */
    @Operation(summary = "删除备份记录", description = "逻辑删除指定的备份记录（不可逆操作）")
    @DeleteMapping("/backups/{backupId}")
    @PreAuthorize("hasAuthority('backup:delete')")
    public Result<Boolean> deleteBackup(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "备份ID") @PathVariable Long backupId) {

        try {
            logger.warn("[高爆炸半径-删除操作] 用户 {}({}) 正在删除备份 ID={}",
                    currentUser.getUsername(), currentUser.getUserId(), backupId);

            boolean success = dataBackupService.deleteBackup(
                    backupId, Long.parseLong(currentUser.getUserId()), currentUser.getUsername());

            return success ? Result.success(true, "删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            logger.error("删除备份失败: backupId={}", backupId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除备份失败");
        }
    }

    /**
     * 下载备份文件
     * 爆炸半径: \ud83d\udfe1 中(导出数据)
     * 权限要求: backup:download
     * 确认机制: 二次确认（前端）
     */
    @Operation(summary = "下载备份文件", description = "下载指定备份的SQL文件")
    @GetMapping("/backups/{backupId}/download")
    @PreAuthorize("hasAuthority('backup:download')")
    public void downloadBackup(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "备份ID") @PathVariable Long backupId,
            HttpServletResponse response) throws Exception {

        logger.info("[下载操作] 用户 {}({}) 下载备份文件 ID={}",
                currentUser.getUsername(), currentUser.getUserId(), backupId);

        dataBackupService.downloadBackup(backupId, response);
    }

    // ==================== 恢复管理接口 ====================

    /**
     * 执行数据恢复操作
     * 爆炸半径: \ud83d\udd34 高(覆盖全部数据!)
     * 权限要求: backup:restore
     * 确认机制: **强确认+密码验证**
     *
     * 安全检查链:
     * 1. @PreAuthorize权限检查
     * 2. 备份记录存在性验证
     * 3. DTO中的@Valid校验（含confirmPassword必填）
     * 4. Service层的二次密码验证
     * 5. 存储空间安全检查(>10%可用)
     * 6. 恢复前自动快照创建
     * 7. 执行pg_restore命令
     * 8. 记录完整恢复日志
     * 9. 发送管理员通知
     */
    @Operation(summary = "执行数据恢复【高危险操作】",
               description = "从备份数据恢复数据库（将覆盖当前数据!需要二次密码确认）")
    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('backup:restore')")
    public Result<BackupRestoreLog> restoreBackup(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody BackupRestoreDTO restoreDTO) {

        try {
            logger.warn("[\ud83d\udd34高爆炸半径-危险操作] 用户 {}({}) 请求数据恢复: backupID={}, 原因={}",
                    currentUser.getUsername(), currentUser.getUserId(),
                    restoreDTO.getBackupId(), restoreDTO.getReason());

            BackupRestoreLog restoreLog = dataBackupService.restoreBackup(
                    restoreDTO.getBackupId(),
                    restoreDTO,
                    Long.parseLong(currentUser.getUserId()),
                    currentUser.getUsername()
            );

            return Result.success(restoreLog, "数据恢复操作已完成，请验证数据完整性");
        } catch (Exception e) {
            logger.error("[\ud83d\udd34危险操作失败] 数据恢复失败: {}", e.getMessage(), e);
            return Result.error("数据恢复失败: " + e.getMessage());
        }
    }

    /**
     * 获取恢复日志分页列表
     * 爆炸半径: \ud83d\udfe2 低(只读)
     * 权限要求: backup:query
     */
    @Operation(summary = "查询恢复日志", description = "分页查询数据恢复操作的日志记录")
    @GetMapping("/restore/logs")
    @PreAuthorize("hasAuthority('backup:query')")
    public Result<IPage<BackupRestoreLog>> getRestoreLog(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "关联的备份ID") @RequestParam(required = false) Long backupId,
            @Parameter(description = "恢复状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "恢复模式") @RequestParam(required = false) Integer mode,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<BackupRestoreLog> page = new Page<>(current, size);
            IPage<BackupRestoreLog> result = dataBackupService.getRestoreLog(
                    page, backupId, status, mode, startTime, endTime);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询恢复日志失败: {}", e.getMessage(), e);
            return Result.error("查询恢复日志失败");
        }
    }

    /**
     * 回滚已执行的恢复操作
     * 爆炸半径: \ud83d\udd34 高(再次修改数据!)
     * 权限要求: backup:restore
     * 确认机制: 强确认
     */
    @Operation(summary = "回滚恢复操作", description = "回滚已执行的恢复操作（基于快照）")
    @PostMapping("/restore/{logId}/rollback")
    @PreAuthorize("hasAuthority('backup:restore')")
    public Result<Boolean> rollbackRestore(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "恢复日志ID") @PathVariable Long logId) {

        try {
            logger.warn("[回滚操作] 用户 {}({}) 回滚恢复操作 logID={}",
                    currentUser.getUsername(), currentUser.getUserId(), logId);

            boolean success = dataBackupService.rollbackRestore(
                    logId, Long.parseLong(currentUser.getUserId()), currentUser.getUsername());

            return success ? Result.success(true, "回滚成功") : Result.error("回滚失败");
        } catch (Exception e) {
            logger.error("回滚恢复操作失败: logId={}", logId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "回滚失败");
        }
    }

    // ==================== 存储管理接口 ====================

    /**
     * 获取存储使用情况
     * 爆炸半径: \ud83d\udfe2 低(只读)
     * 权限要求: backup:query
     */
    @Operation(summary = "获取存储使用情况", description = "查询备份存储空间使用统计")
    @GetMapping("/storage/usage")
    @PreAuthorize("hasAuthority('backup:query')")
    public Result<Map<String, Object>> getStorageUsage() {
        try {
            Map<String, Object> usage = dataBackupService.getStorageUsage();
            return Result.success(usage);
        } catch (Exception e) {
            logger.error("获取存储使用情况失败: {}", e.getMessage(), e);
            return Result.error("获取存储使用情况失败");
        }
    }

    // ==================== 统计与仪表盘接口 ====================

    /**
     * 获取备份统计数据
     * 爆炸半径: \ud83d\udfe2 低(只读)
     * 权限要求: backup:query
     */
    @Operation(summary = "获取备份统计数据", description = "查询备份操作的统计概览")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('backup:query')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = dataBackupService.getStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            logger.error("获取备份统计失败: {}", e.getMessage(), e);
            return Result.error("获取备份统计失败");
        }
    }

    /**
     * 获取仪表盘聚合数据
     * 爆炸半径: \ud83d\udfe2 低(只读)
     * 权限要求: backup:query
     */
    @Operation(summary = "获取仪表盘数据", description = "获取备份模块的完整仪表盘数据（含图表、趋势等）")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('backup:query')")
    public Result<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboardData = dataBackupService.getDashboardData();
            return Result.success(dashboardData);
        } catch (Exception e) {
            logger.error("获取仪表盘数据失败: {}", e.getMessage(), e);
            return Result.error("获取仪表盘数据失败");
        }
    }

    /**
     * 手动触发清理过期备份（管理员功能）
     * 爆炸半径: \ud83e\udc19 中高
     * 权限要求: backup:config 或 ADMIN角色
     */
    @Operation(summary = "清理过期备份", description = "手动清理过期的备份记录（定时任务也会自动执行）")
    @PostMapping("/cleanup-expired")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('backup:config')")
    public Result<Map<String, Object>> cleanupExpiredBackups(
            @AuthenticationPrincipal SecurityUser currentUser) {

        try {
            int cleanedCount = dataBackupService.cleanupExpiredBackups();
            logger.info("[管理操作] 用户 {}({}) 手动清理过期备份: 清理{}条",
                    currentUser.getUsername(), currentUser.getUserId(), cleanedCount);

            return Result.success(Map.of(
                    "cleanedCount", cleanedCount,
                    "message", "过期备份清理完成"
            ));
        } catch (Exception e) {
            logger.error("清理过期备份失败: {}", e.getMessage(), e);
            return Result.error("清理过期备份失败: " + e.getMessage());
        }
    }
}
