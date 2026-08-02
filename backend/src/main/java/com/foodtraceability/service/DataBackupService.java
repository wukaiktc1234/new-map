package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.BackupCreateDTO;
import com.foodtraceability.dto.BackupQueryDTO;
import com.foodtraceability.dto.BackupRestoreDTO;
import com.foodtraceability.entity.BackupRestoreLog;
import com.foodtraceability.entity.DataBackupRecord;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 数据备份与恢复服务接口
 * 提供数据库备份、恢复、管理的完整功能
 * 包含爆炸半径权限校验和安全控制机制
 */
public interface DataBackupService {

    // ==================== 备份管理 ====================

    /**
     * 创建新的数据备份
     * 支持全量/增量/仅结构三种备份类型
     * @param createDTO 备份创建请求
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 创建的备份记录
     */
    DataBackupRecord createBackup(BackupCreateDTO createDTO, Long userId, String username);

    /**
     * 分页查询备份记录列表
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<DataBackupRecord> getBackupList(Page<DataBackupRecord> page, BackupQueryDTO query);

    /**
     * 获取备份详情
     * @param backupId 备份ID
     * @return 备份记录详情
     */
    DataBackupRecord getBackupDetail(Long backupId);

    /**
     * 逻辑删除备份记录（非物理删除）
     * 爆炸半径: 中高 - 不可逆操作
     * @param backupId 备份ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean deleteBackup(Long backupId, Long userId, String username);

    /**
     * 下载备份文件
     * 爆炸半径: 中 - 数据导出
     * @param backupId 备份ID
     * @param response HTTP响应对象（用于文件流输出）
     */
    void downloadBackup(Long backupId, HttpServletResponse response);

    // ==================== 恢复管理 ====================

    /**
     * 执行数据恢复操作（高爆炸半径操作）
     * 包含完整的二次确认流程:
     * 1. 权限检查 (backup:restore)
     * 2. 爆炸半径警告记录
     * 3. 密码验证
     * 4. 存储空间检查(>10%可用)
     * 5. 恢复前自动快照
     * 6. 执行pg_restore
     * 7. 记录恢复日志
     * 8. 发送通知给所有管理员
     *
     * @param backupId 备份ID
     * @param restoreDTO 恢复请求（含二次确认密码）
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 恢复日志记录
     */
    BackupRestoreLog restoreBackup(Long backupId, BackupRestoreDTO restoreDTO,
                                    Long userId, String username);

    /**
     * 获取恢复日志分页列表
     * @param page 分页参数
     * @param backupId 关联的备份ID（可选）
     * @param status 恢复状态（可选）
     * @param mode 恢复模式（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分页结果
     */
    IPage<BackupRestoreLog> getRestoreLog(Page<BackupRestoreLog> page, Long backupId,
                                          Integer status, Integer mode,
                                          java.time.LocalDateTime startTime,
                                          java.time.LocalDateTime endTime);

    /**
     * 回滚已执行的恢复操作
     * @param logId 恢复日志ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean rollbackRestore(Long logId, Long userId, String username);

    // ==================== 存储管理 ====================

    /**
     * 获取存储使用情况统计
     * 返回: 总备份数、总大小、可用空间、使用率等
     * @return 存储使用情况Map
     */
    Map<String, Object> getStorageUsage();

    /**
     * 清理过期备份（定时任务调用）
     * 基于retention_days字段判断是否过期
     * 使用逻辑删除，不进行物理删除
     * @return 清理的备份数量
     */
    int cleanupExpiredBackups();

    // ==================== 统计与仪表盘 ====================

    /**
     * 获取备份统计数据
     * 返回: 总数、成功数、失败数、各状态分布等
     * @return 统计数据Map
     */
    Map<String, Object> getStatistics();

    /**
     * 获取仪表盘聚合数据
     * 包含: 最近备份列表、存储使用情况、月度趋势图数据、即将过期的备份等
     * @return 仪表盘数据Map
     */
    Map<String, Object> getDashboardData();
}
