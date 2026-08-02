package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.BackupRestoreLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据恢复日志 Mapper 接口
 * 提供恢复日志的查询、统计等数据访问操作
 */
@Mapper
public interface BackupRestoreLogMapper extends BaseMapper<BackupRestoreLog> {

    /**
     * 分页查询恢复日志（支持多条件过滤）
     * @param page 分页参数
     * @param backupId 关联的备份ID
     * @param restoreStatus 恢复状态
     * @param restoreMode 恢复模式
     * @param createUserId 操作用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    IPage<BackupRestoreLog> selectRestoreLogPage(
        Page<BackupRestoreLog> page,
        @Param("backupId") Long backupId,
        @Param("restoreStatus") Integer restoreStatus,
        @Param("restoreMode") Integer restoreMode,
        @Param("createUserId") Long createUserId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据备份ID查询恢复历史
     * @param backupId 备份ID
     * @return 该备份的所有恢复记录
     */
    List<BackupRestoreLog> selectByBackupId(@Param("backupId") Long backupId);

    /**
     * 按恢复状态统计数量
     * 返回各状态对应的数量Map
     */
    Map<String, Long> selectRestoreStatusCount();

    /**
     * 查询恢复日志统计信息
     * 返回: totalCount, successCount, failedCount, rolledBackCount,
     *       pendingCount, inProgressCount, avgDurationMs
     */
    Map<String, Object> selectRestoreLogStatistics();

    /**
     * 查询最近的恢复操作记录
     * @param limit 限制条数
     * @return 最近恢复记录列表
     */
    List<BackupRestoreLog> selectLatestRestoreLogs(@Param("limit") int limit);
}
