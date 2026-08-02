package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.DataBackupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据备份记录 Mapper 接口
 * 提供备份记录的查询、统计等数据访问操作
 */
@Mapper
public interface DataBackupMapper extends BaseMapper<DataBackupRecord> {

    /**
     * 分页查询备份记录（支持多条件过滤）
     * @param page 分页参数
     * @param backupName 备份名称（模糊匹配）
     * @param backupType 备份类型
     * @param status 状态
     * @param storageLocation 存储位置
     * @param triggeredBy 触发方式
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    IPage<DataBackupRecord> selectBackupPage(
        Page<DataBackupRecord> page,
        @Param("backupName") String backupName,
        @Param("backupType") Integer backupType,
        @Param("status") Integer status,
        @Param("storageLocation") String storageLocation,
        @Param("triggeredBy") String triggeredBy,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询备份统计信息
     * 返回: totalCount, successCount, failedCount, pendingCount, expiredCount,
     *       totalSizeBytes, avgDurationMs, latestBackupTime
     */
    Map<String, Object> selectBackupStatistics();

    /**
     * 查询存储使用情况
     * 返回: totalBackups, totalSizeBytes, availableSizeBytes, usagePercent,
     *       oldestBackupTime, latestBackupTime, expiringSoonCount(7天内过期)
     */
    Map<String, Object> selectStorageUsage();

    /**
     * 查询即将过期的备份列表（指定天数内）
     * @param days 天数阈值
     * @return 即将过期的备份列表
     */
    List<DataBackupRecord> selectExpiringBackups(@Param("days") int days);

    /**
     * 查询已过期的备份列表
     * @return 已过期且未删除的备份列表
     */
    List<DataBackupRecord> selectExpiredBackups();

    /**
     * 按状态统计备份数量
     * 返回各状态对应的数量Map
     */
    Map<String, Long> selectStatusCount();

    /**
     * 按月份统计备份数量（用于图表展示）
     * @param months 统计月数（默认12个月）
     * @return 月度统计数据列表
     */
    List<Map<String, Object>> selectMonthlyStats(@Param("months") int months);

    /**
     * 查询最近N条成功的备份记录
     * @param limit 限制条数
     * @return 最近成功备份列表
     */
    List<DataBackupRecord> selectLatestSuccessBackups(@Param("limit") int limit);
}
