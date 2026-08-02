-- ============================================================
-- 生产环境调度任务 handler 修复脚本
-- 版本: V20260707_003
-- 说明: 将种子数据中指向不存在的 com.example.demo 包 handler
--       更新为当前项目 com.foodtraceability 下实际存在的内置 handler
-- ============================================================

UPDATE scheduled_task
SET job_handler = CASE task_code
    WHEN 'log-cleanup'      THEN 'cleanupExpiredDataTask'
    WHEN 'file-cleanup'     THEN 'cleanupExpiredDataTask'
    WHEN 'db-backup'        THEN 'dataBackupTask'
    WHEN 'cache-warmup'     THEN 'systemHealthCheckTask'
    WHEN 'report-generate'  THEN 'systemHealthCheckTask'
    WHEN 'cert-check'       THEN 'systemHealthCheckTask'
    WHEN 'session-cleanup'  THEN 'cleanupExpiredDataTask'
    ELSE job_handler
END,
    version = version + 1
WHERE job_handler LIKE 'com.example.demo.%';
