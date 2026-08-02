package com.foodtraceability.service;

import com.foodtraceability.common.Result;

import java.util.Map;

/**
 * 备份通知服务接口
 */
public interface BackupNotificationService {

    /**
     * 发送备份成功通知
     *
     * @param backupId   备份ID
     * @param backupName 备份名称
     * @param fileSize   文件大小
     * @param duration   备份耗时（秒）
     * @return 发送结果
     */
    Result<Void> sendBackupSuccessNotification(String backupId, String backupName, String fileSize, long duration);

    /**
     * 发送备份失败通知
     *
     * @param backupId    备份ID
     * @param backupName  备份名称
     * @param errorMessage 错误信息
     * @return 发送结果
     */
    Result<Void> sendBackupFailureNotification(String backupId, String backupName, String errorMessage);

    /**
     * 发送恢复成功通知
     *
     * @param backupId   备份ID
     * @param backupName 备份名称
     * @param operator   操作人
     * @return 发送结果
     */
    Result<Void> sendRestoreSuccessNotification(String backupId, String backupName, String operator);

    /**
     * 发送恢复失败通知
     *
     * @param backupId     备份ID
     * @param backupName   备份名称
     * @param operator     操作人
     * @param errorMessage 错误信息
     * @return 发送结果
     */
    Result<Void> sendRestoreFailureNotification(String backupId, String backupName, String operator, String errorMessage);

    /**
     * 发送存储空间警告
     *
     * @param usedSpace    已用空间
     * @param totalSpace   总空间
     * @param usagePercent 使用率百分比
     * @return 发送结果
     */
    Result<Void> sendStorageWarning(long usedSpace, long totalSpace, double usagePercent);

    /**
     * 发送数据一致性检查报告
     *
     * @param checkType   检查类型
     * @param issues      发现的问题
     * @param totalChecks 总检查项
     * @return 发送结果
     */
    Result<Void> sendDataConsistencyReport(String checkType, int issues, int totalChecks);

    /**
     * 检查通知服务是否可用
     *
     * @return 是否可用
     */
    Result<Boolean> isAvailable();

    /**
     * 获取通知方式
     *
     * @return 通知方式（如：EMAIL, SMS, WEBHOOK, LOG）
     */
    String getNotificationType();
}
