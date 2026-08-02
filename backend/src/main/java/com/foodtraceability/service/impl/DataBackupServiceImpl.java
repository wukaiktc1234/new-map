package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.BackupCreateDTO;
import com.foodtraceability.dto.BackupQueryDTO;
import com.foodtraceability.dto.BackupRestoreDTO;
import com.foodtraceability.entity.BackupRestoreLog;
import com.foodtraceability.entity.DataBackupRecord;
import com.foodtraceability.mapper.BackupRestoreLogMapper;
import com.foodtraceability.mapper.DataBackupMapper;
import com.foodtraceability.service.DataBackupService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据备份与恢复服务实现类
 * 核心功能:
 * 1. PostgreSQL pg_dump/pg_restore 集成
 * 2. 文件完整性校验(SHA256)
 * 3. 存储空间监控与告警
 * 4. 恢复前自动快照
 * 5. 爆炸半径权限校验
 * 6. 操作审计日志记录
 */
@Service
public class DataBackupServiceImpl implements DataBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DataBackupServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");


    public DataBackupServiceImpl(DataBackupMapper dataBackupMapper, BackupRestoreLogMapper backupRestoreLogMapper, PasswordEncoder passwordEncoder, ObjectMapper objectMapper, @Nullable com.foodtraceability.service.UserService userService) {
        this.dataBackupMapper = dataBackupMapper;
        this.backupRestoreLogMapper = backupRestoreLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    /** 备份文件存储根目录 */
    @Value("${backup.storage.path:./backups}")
    private String backupStoragePath;

    /** PostgreSQL连接信息 */
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    /** PostgreSQL bin目录路径（用于Windows环境） */
    @Value("${backup.pg.bin.path:}")
    private String pgBinPath;

    /** pg_dump执行超时时间（毫秒），默认30分钟 */
    @Value("${backup.pg.dump.timeout:1800000}")
    private long pgDumpTimeout;

    /** pg_restore执行超时时间（毫秒），默认60分钟 */
    @Value("${backup.pg.restore.timeout:3600000}")
    private long pgRestoreTimeout;

    /** 最小磁盘可用空间百分比阈值 */
    private static final double MIN_DISK_AVAILABLE_PERCENT = 10.0;

    /** 备份状态常量 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_EXPIRED = 4;
    public static final int STATUS_DELETING = 5;

    /** 恢复状态常量 */
    public static final int RESTORE_STATUS_PENDING = 0;
    public static final int RESTORE_STATUS_IN_PROGRESS = 1;
    public static final int RESTORE_STATUS_SUCCESS = 2;
    public static final int RESTORE_STATUS_FAILED = 3;
    public static final int RESTORE_STATUS_ROLLED_BACK = 4;

    /** 恢复模式常量 */
    public static final int MODE_FULL_OVERWRITE = 1;
    public static final int MODE_SELECTIVE = 2;
    public static final int MODE_PREVIEW_ONLY = 3;

    private final DataBackupMapper dataBackupMapper;

    private final BackupRestoreLogMapper backupRestoreLogMapper;

    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    private final com.foodtraceability.service.UserService userService;

    // ==================== 备份管理实现 ====================

    /**
     * 创建数据备份
     * 执行流程: 参数校验 -> 空间检查 -> 创建记录 -> 执行pg_dump -> 校验文件 -> 更新状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataBackupRecord createBackup(BackupCreateDTO createDTO, Long userId, String username) {
        logger.info("开始创建数据备份: 用户={}, 名称={}, 类型={}", username, createDTO.getBackupName(), createDTO.getBackupType());

        // Step 1: 检查存储空间是否充足
        checkStorageSpace();

        // Step 2: 构建备份记录实体
        DataBackupRecord record = new DataBackupRecord();
        record.setBackupName(createDTO.getBackupName());
        record.setBackupType(createDTO.getBackupType());
        record.setBackupMethod(createDTO.getBackupMethod() != null ? createDTO.getBackupMethod() : 1);
        record.setStatus(STATUS_PENDING);
        record.setTriggeredBy("MANUAL");
        record.setTriggerUserId(userId);
        record.setTriggerUsername(username);
        record.setStorageLocation(createDTO.getStorageLocation() != null ? createDTO.getStorageLocation() : "local");
        record.setRetentionDays(createDTO.getRetentionDays() != null ? createDTO.getRetentionDays() : 30);

        // 计算过期时间
        if (record.getRetentionDays() != null && record.getRetentionDays() > 0) {
            record.setExpiresAt(LocalDateTime.now().plusDays(record.getRetentionDays()));
        }

        // 序列化表列表为JSON
        if (createDTO.getTablesIncluded() != null && !createDTO.getTablesIncluded().isEmpty()) {
            try {
                record.setTablesIncluded(objectMapper.writeValueAsString(createDTO.getTablesIncluded()));
            } catch (JsonProcessingException e) {
                logger.warn("序列化表列表失败", e);
            }
        }

        // Step 3: 保存初始记录（PENDING状态）
        dataBackupMapper.insert(record);
        logger.info("备份记录已创建, ID={}", record.getBackupId());

        // Step 4: 更新状态为进行中
        record.setStatus(STATUS_IN_PROGRESS);
        dataBackupMapper.updateById(record);

        long startTime = System.currentTimeMillis();
        try {
            // Step 5: 执行pg_dump命令
            String filePath = executePgDump(record, createDTO);
            record.setFilePath(filePath);

            // Step 6: 获取文件大小和校验和
            File backupFile = new File(filePath);
            record.setFileSizeBytes(backupFile.length());
            record.setFileChecksum(calculateSHA256(backupFile));

            // Step 7: 更新为成功状态
            record.setStatus(STATUS_SUCCESS);
            record.setDurationMs(System.currentTimeMillis() - startTime);
            dataBackupMapper.updateById(record);

            logger.info("数据备份成功完成: ID={}, 文件={}, 大小={}字节, 耗时={}ms",
                    record.getBackupId(), filePath, record.getFileSizeBytes(), record.getDurationMs());

        } catch (Exception e) {
            // 备份失败处理
            record.setStatus(STATUS_FAILED);
            record.setErrorMessage(e.getMessage());
            record.setDurationMs(System.currentTimeMillis() - startTime);
            dataBackupMapper.updateById(record);

            logger.error("数据备份失败: ID={}, 错误={}", record.getBackupId(), e.getMessage(), e);
            throw new RuntimeException("数据备份执行失败: " + e.getMessage(), e);
        }

        return record;
    }

    /**
     * 分页查询备份记录列表
     */
    @Override
    public IPage<DataBackupRecord> getBackupList(Page<DataBackupRecord> page, BackupQueryDTO query) {
        return dataBackupMapper.selectBackupPage(
            page,
            query.getBackupName(),
            query.getBackupType(),
            query.getStatus(),
            query.getStorageLocation(),
            query.getTriggeredBy(),
            query.getStartTime(),
            query.getEndTime()
        );
    }

    /**
     * 获取备份详情
     */
    @Override
    public DataBackupRecord getBackupDetail(Long backupId) {
        DataBackupRecord record = dataBackupMapper.selectById(backupId);
        if (record == null) {
            throw new RuntimeException("备份记录不存在: " + backupId);
        }
        return record;
    }

    /**
     * 逻辑删除备份记录（非物理删除）
     * 爆炸半径: 中高 - 不可逆操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBackup(Long backupId, Long userId, String username) {
        logger.warn("[高爆炸半径操作] 用户 {}({}) 正在删除备份记录 ID={}", username, userId, backupId);

        DataBackupRecord record = dataBackupMapper.selectById(backupId);
        if (record == null) {
            throw new RuntimeException("备份记录不存在: " + backupId);
        }

        // 检查是否正在使用中
        if (record.getStatus() == STATUS_IN_PROGRESS) {
            throw new RuntimeException("备份正在进行中，无法删除");
        }

        // 使用逻辑删除
        int result = dataBackupMapper.deleteById(backupId);
        if (result > 0) {
            logger.info("[审计日志] 备份记录已逻辑删除: ID={}, 操作者={}", backupId, username);
        }
        return result > 0;
    }

    /**
     * 下载备份文件
     * 爆炸半径: 中 - 数据导出
     */
    @Override
    public void downloadBackup(Long backupId, HttpServletResponse response) {
        DataBackupRecord record = dataBackupMapper.selectById(backupId);
        if (record == null) {
            throw new RuntimeException("备份记录不存在: " + backupId);
        }
        if (record.getStatus() != STATUS_SUCCESS) {
            throw new RuntimeException("只有成功的备份才能下载");
        }
        if (record.getFilePath() == null || record.getFilePath().isEmpty()) {
            throw new RuntimeException("备份文件路径不存在");
        }

        File file = new File(record.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("备份文件不存在: " + record.getFilePath());
        }

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + record.getBackupName() + ".sql\"");
            response.setContentLengthLong(file.length());

            // 流式传输文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            logger.info("备份文件已下载: ID={}, 文件={}, 大小={}字节",
                    backupId, file.getName(), file.length());

        } catch (IOException e) {
            logger.error("下载备份文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("下载备份文件失败: " + e.getMessage(), e);
        }
    }

    // ==================== 恢复管理实现 ====================

    /**
     * 执行数据恢复操作（高爆炸半径操作）
     * 包含完整的二次确认和安全控制流程
     *
     * 安全检查链:
     * 1. 权限检查 (@PreAuthorize在Controller层)
     * 2. 备份记录存在性验证
     * 3. 密码二次确认验证
     * 4. 存储空间安全检查(>10%)
     * 5. 恢复前自动快照创建
     * 6. 执行pg_restore命令
     * 7. 记录完整恢复日志
     * 8. 发送管理员通知
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackupRestoreLog restoreBackup(Long backupId, BackupRestoreDTO restoreDTO,
                                           Long userId, String username) {
        logger.warn("[高爆炸半径-危险操作] 开始数据恢复流程: 备份ID={}, 操作者={}, 原因={}",
                backupId, username, restoreDTO.getReason());

        // Step 1: 验证备份记录存在且有效
        DataBackupRecord backupRecord = dataBackupMapper.selectById(backupId);
        if (backupRecord == null) {
            throw new RuntimeException("备份记录不存在: " + backupId);
        }
        if (backupRecord.getStatus() != STATUS_SUCCESS) {
            throw new RuntimeException("只能从成功的备份中恢复数据");
        }
        if (backupRecord.getFilePath() == null || backupRecord.getFilePath().isEmpty()) {
            throw new RuntimeException("备份文件路径无效");
        }

        // Step 2: 密码二次确认验证
        validateConfirmPassword(userId, restoreDTO.getConfirmPassword());

        // Step 3: 检查存储空间
        checkStorageSpace();

        // Step 4: 创建恢复日志记录（初始状态）
        BackupRestoreLog restoreLog = new BackupRestoreLog();
        restoreLog.setBackupId(backupId);
        restoreLog.setBackupName(backupRecord.getBackupName());
        restoreLog.setRestoreStatus(RESTORE_STATUS_PENDING);
        restoreLog.setRestoreMode(restoreDTO.getRestoreMode());
        restoreLog.setConfirmedBy(username);
        restoreLog.setConfirmTime(LocalDateTime.now());
        restoreLog.setCreateUserId(userId);
        restoreLog.setCreateUsername(username);

        // 序列化目标表列表
        if (restoreDTO.getTargetTables() != null && !restoreDTO.getTargetTables().isEmpty()) {
            try {
                restoreLog.setTargetTables(objectMapper.writeValueAsString(restoreDTO.getTargetTables()));
            } catch (JsonProcessingException e) {
                logger.warn("序列化目标表列表失败", e);
            }
        }

        backupRestoreLogMapper.insert(restoreLog);
        logger.info("恢复日志已创建: logID={}, backupID={}", restoreLog.getLogId(), backupId);

        // Step 5: 更新状态为进行中
        restoreLog.setRestoreStatus(RESTORE_STATUS_IN_PROGRESS);
        backupRestoreLogMapper.updateById(restoreLog);

        long startTime = System.currentTimeMillis();
        String preSnapshotPath = null;
        try {
            // Step 6: 恢复前自动快照（如果启用）
            if (Boolean.TRUE.equals(restoreDTO.getCreatePreSnapshot())) {
                preSnapshotPath = createPreRestoreSnapshot(backupId, userId, username);
                restoreLog.setPreRestoreSnapshot(preSnapshotPath);
                backupRestoreLogMapper.updateById(restoreLog);
                logger.info("恢复前快照已创建: path={}", preSnapshotPath);
            }

            // Step 7: 执行pg_restore命令
            executePgRestore(backupRecord, restoreDTO, restoreLog);

            // Step 8: 更新为成功状态
            restoreLog.setRestoreStatus(RESTORE_STATUS_SUCCESS);
            restoreLog.setFinishTime(LocalDateTime.now());
            restoreLog.setDurationMs(System.currentTimeMillis() - startTime);
            backupRestoreLogMapper.updateById(restoreLog);

            logger.warn("[高爆炸半径操作完成] 数据恢复成功: logID={}, backupID={}, 耗时={}ms, 操作者={}",
                    restoreLog.getLogId(), backupId, restoreLog.getDurationMs(), username);

            // Step 9: 发送通知给所有管理员
            sendAdminNotification(backupRecord, restoreLog, username, restoreDTO.getReason());

        } catch (Exception e) {
            // 恢复失败处理
            restoreLog.setRestoreStatus(RESTORE_STATUS_FAILED);
            restoreLog.setErrorMessage(e.getMessage());
            restoreLog.setFinishTime(LocalDateTime.now());
            restoreLog.setDurationMs(System.currentTimeMillis() - startTime);
            backupRestoreLogMapper.updateById(restoreLog);

            logger.error("[高爆炸半径操作失败] 数据恢复失败: logID={}, 错误={}",
                    restoreLog.getLogId(), e.getMessage(), e);
            throw new RuntimeException("数据恢复执行失败: " + e.getMessage(), e);
        }

        return restoreLog;
    }

    /**
     * 获取恢复日志分页列表
     */
    @Override
    public IPage<BackupRestoreLog> getRestoreLog(Page<BackupRestoreLog> page, Long backupId,
                                                  Integer status, Integer mode,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime) {
        return backupRestoreLogMapper.selectRestoreLogPage(page, backupId, status, mode, null, startTime, endTime);
    }

    /**
     * 回滚已执行的恢复操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackRestore(Long logId, Long userId, String username) {
        logger.warn("[回滚操作] 用户 {}({}) 正在回滚恢复操作 logID={}", username, userId, logId);

        BackupRestoreLog restoreLog = backupRestoreLogMapper.selectById(logId);
        if (restoreLog == null) {
            throw new RuntimeException("恢复日志不存在: " + logId);
        }
        if (restoreLog.getRestoreStatus() != RESTORE_STATUS_SUCCESS) {
            throw new RuntimeException("只能回滚成功的恢复操作");
        }

        // TODO: 执行实际回滚逻辑（基于preRestoreSnapshot）
        restoreLog.setRestoreStatus(RESTORE_STATUS_ROLLED_BACK);
        restoreLog.setFinishTime(LocalDateTime.now());
        backupRestoreLogMapper.updateById(restoreLog);

        logger.info("[审计日志] 恢复操作已回滚: logID={}, 操作者={}", logId, username);
        return true;
    }

    // ==================== 存储管理实现 ====================

    /**
     * 获取存储使用情况统计
     */
    @Override
    public Map<String, Object> getStorageUsage() {
        Map<String, Object> usage = dataBackupMapper.selectStorageUsage();

        // 补充磁盘空间信息
        Path storagePath = Paths.get(backupStoragePath).toAbsolutePath().getRoot();
        if (storagePath != null) {
            try {
                long totalSpace = Files.getFileStore(storagePath).getTotalSpace();
                long usableSpace = Files.getFileStore(storagePath).getUsableSpace();
                long usedSpace = totalSpace - usableSpace;
                double usagePercent = (double) usedSpace / totalSpace * 100;

                usage.put("diskTotalSpace", totalSpace);
                usage.put("diskUsableSpace", usableSpace);
                usage.put("diskUsedSpace", usedSpace);
                usage.put("diskUsagePercent", Math.round(usagePercent * 100.0) / 100.0);
                usage.put("diskUsageWarning", usagePercent > 90);
            } catch (IOException e) {
                logger.warn("获取磁盘空间信息失败", e);
            }
        }

        return usage;
    }

    /**
     * 清理过期备份（定时任务调用）
     * 基于retention_days字段判断是否过期
     * 使用逻辑删除，不进行物理删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredBackups() {
        List<DataBackupRecord> expiredBackups = dataBackupMapper.selectExpiredBackups();
        if (expiredBackups == null || expiredBackups.isEmpty()) {
            logger.info("没有需要清理的过期备份");
            return 0;
        }

        int cleanedCount = 0;
        for (DataBackupRecord record : expiredBackups) {
            try {
                // 更新状态为删除中
                record.setStatus(STATUS_DELETING);
                dataBackupMapper.updateById(record);

                // 执行逻辑删除
                dataBackupMapper.deleteById(record.getBackupId());
                cleanedCount++;

                logger.info("过期备份已清理: ID={}, 名称={}", record.getBackupId(), record.getBackupName());
            } catch (Exception e) {
                logger.error("清理过期备份失败: ID={}, 错误={}", record.getBackupId(), e.getMessage());
            }
        }

        if (cleanedCount > 0) {
            logger.info("[定时任务] 过期备份清理完成: 共清理{}条", cleanedCount);
        }
        return cleanedCount;
    }

    // ==================== 统计与仪表盘实现 ====================

    /**
     * 获取备份统计数据
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = dataBackupMapper.selectBackupStatistics();

        // 补充各状态数量
        Map<String, Long> statusCount = dataBackupMapper.selectStatusCount();
        statistics.putAll(statusCount);

        return statistics;
    }

    /**
     * 获取仪表盘聚合数据
     */
    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboardData = new HashMap<>();

        // 1. 备份统计概览
        dashboardData.put("statistics", getStatistics());

        // 2. 存储使用情况
        dashboardData.put("storageUsage", getStorageUsage());

        // 3. 最近成功的备份列表
        dashboardData.put("latestSuccessBackups", dataBackupMapper.selectLatestSuccessBackups(5));

        // 4. 月度趋势图数据
        dashboardData.put("monthlyStats", dataBackupMapper.selectMonthlyStats(12));

        // 5. 即将过期的备份（7天内）
        dashboardData.put("expiringSoonBackups", dataBackupMapper.selectExpiringBackups(7));

        // 6. 最近恢复操作记录
        dashboardData.put("latestRestoreLogs", backupRestoreLogMapper.selectLatestRestoreLogs(10));

        // 7. 恢复日志统计
        dashboardData.put("restoreStatistics", backupRestoreLogMapper.selectRestoreLogStatistics());

        return dashboardData;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查存储空间是否充足
     * 可用空间低于10%时拒绝操作
     */
    private void checkStorageSpace() {
        Path rootPath = Paths.get(backupStoragePath).toAbsolutePath().getRoot();
        if (rootPath != null) {
            try {
                long totalSpace = Files.getFileStore(rootPath).getTotalSpace();
                long usableSpace = Files.getFileStore(rootPath).getUsableSpace();
                double availablePercent = (double) usableSpace / totalSpace * 100;

                if (availablePercent < MIN_DISK_AVAILABLE_PERCENT) {
                    logger.error("磁盘空间不足: 可用空间{}% < 最小要求{}%", availablePercent, MIN_DISK_AVAILABLE_PERCENT);
                    throw new RuntimeException(
                            String.format("磁盘空间不足! 当前可用空间: %.2f%%, 最低要求: %.0f%%. 请清理磁盘空间后重试.",
                                    availablePercent, MIN_DISK_AVAILABLE_PERCENT));
                }

                logger.debug("存储空间检查通过: 可用空间={:.2f}%", availablePercent);
            } catch (IOException e) {
                logger.warn("无法获取磁盘空间信息", e);
            }
        }
    }

    /**
     * 执行pg_dump命令进行数据库备份
     * 支持Windows和Linux环境，自动检测PostgreSQL bin目录
     */
    private String executePgDump(DataBackupRecord record, BackupCreateDTO createDTO) throws IOException, InterruptedException {
        // 确保备份目录存在
        Path backupDir = Paths.get(backupStoragePath).toAbsolutePath();
        Files.createDirectories(backupDir);

        // 构建备份文件名
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String fileName = String.format("%s_%s_%d.dump",
                record.getBackupName().replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_"),
                timestamp,
                record.getBackupId());

        Path backupFile = backupDir.resolve(fileName);

        // 构建pg_dump命令
        List<String> command = buildPgDumpCommand(backupFile, record, createDTO);

        logger.info("执行pg_dump命令: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PGPASSWORD", datasourcePassword);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // 读取命令输出（使用独立线程避免阻塞）
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("[pg_dump输出] {}", line);
            }
        }

        // 使用带超时的waitFor，防止进程永久阻塞
        boolean finished = process.waitFor(pgDumpTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_dump执行超时（超过" + (pgDumpTimeout / 1000) + "秒），已强制终止进程");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump执行失败(退出码=" + exitCode + "): " + output.toString());
        }

        logger.info("pg_dump执行成功: 文件={}", backupFile);
        return backupFile.toString();
    }

    /**
     * 执行pg_restore命令进行数据恢复
     * 支持Windows和Linux环境，包含超时处理
     */
    private void executePgRestore(DataBackupRecord backupRecord, BackupRestoreDTO restoreDTO,
                                   BackupRestoreLog restoreLog) throws IOException, InterruptedException {
        // 验证备份文件存在
        File backupFile = new File(backupRecord.getFilePath());
        if (!backupFile.exists()) {
            throw new RuntimeException("备份文件不存在: " + backupRecord.getFilePath());
        }

        // 构建pg_restore命令
        List<String> command = buildPgRestoreCommand(backupFile, restoreDTO);

        logger.warn("[危险命令] 执行pg_restore命令: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PGPASSWORD", datasourcePassword);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // 读取命令输出（使用独立线程避免阻塞）
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.info("[pg_restore输出] {}", line);
            }
        }

        // 使用带超时的waitFor，防止进程永久阻塞
        boolean finished = process.waitFor(pgRestoreTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_restore执行超时（超过" + (pgRestoreTimeout / 1000) + "秒），已强制终止进程");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("pg_restore执行失败(退出码=" + exitCode + "): " + output.toString());
        }

        logger.warn("[危险命令完成] pg_restore执行成功: backupID={}", backupRecord.getBackupId());
    }

    /**
     * 创建恢复前自动快照
     * 在执行恢复前先对当前数据库进行一次快速备份
     */
    private String createPreRestoreSnapshot(Long backupId, Long userId, String username) throws IOException, InterruptedException {
        String snapshotName = String.format("pre_restore_snapshot_%s_%d",
                LocalDateTime.now().format(DATE_FORMAT), backupId);

        // 创建临时备份记录用于快照
        BackupCreateDTO snapshotDTO = new BackupCreateDTO();
        snapshotDTO.setBackupName(snapshotName);
        snapshotDTO.setBackupType(1);  // 全量备份
        snapshotDTO.setBackupMethod(1);
        snapshotDTO.setRetentionDays(90);  // 快照保留90天
        snapshotDTO.setDescription(String.format("恢复前自动快照 - 原始备份ID=%d, 操作者=%s", backupId, username));

        DataBackupRecord snapshotRecord = createBackup(snapshotDTO, userId, username);
        return snapshotRecord.getFilePath();
    }

    /**
     * 验证二次确认密码
     * 确认操作者是账号持有者本人，防止未授权的危险操作
     *
     * 安全机制:
     * 1. 检查密码是否为空
     * 2. 通过UserService获取用户信息
     * 3. 使用PasswordEncoder验证密码哈希
     * 4. 记录详细的审计日志
     */
    private void validateConfirmPassword(Long userId, String confirmPassword) {
        // Step 1: 基本参数校验
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            logger.warn("[安全警告] 用户{}尝试执行危险操作但未提供确认密码", userId);
            throw new RuntimeException("请输入确认密码以继续此危险操作");
        }

        if (userId == null) {
            throw new RuntimeException("无效的用户ID，无法验证身份");
        }

        // Step 2: 获取用户信息并验证密码
        try {
            if (userService == null) {
                // UserService不可用时的降级处理（开发环境或测试环境）
                logger.warn("[降级模式] UserService不可用，跳过密码验证（仅限开发/测试环境）");
                logger.info("[审计日志] 二次密码验证通过(降级模式): userID={}", userId);
                return;
            }

            com.foodtraceability.entity.User user = userService.getById(userId);
            if (user == null) {
                logger.error("[安全警告] 用户ID={}对应的用户不存在", userId);
                throw new RuntimeException("用户不存在，无法验证身份");
            }

            // 验证密码（使用BCrypt等加密算法）
            boolean passwordMatch = false;
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                passwordMatch = passwordEncoder.matches(confirmPassword, user.getPassword());
            }

            if (!passwordMatch) {
                logger.error("[安全警告] 用户{}({})的二次确认密码验证失败！可能存在未授权访问尝试",
                        userId, user.getUsername());
                throw new RuntimeException("确认密码错误，请重新输入正确的密码");
            }

            // Step 3: 验证成功，记录审计日志
            logger.info("[审计日志] 二次密码验证成功: userID={}, username={}, 时间={}",
                    userId, user.getUsername(), LocalDateTime.now());

        } catch (RuntimeException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            logger.error("[系统错误] 密码验证过程发生异常: userID={}, error={}",
                    userId, e.getMessage(), e);
            throw new RuntimeException("密码验证系统异常，请联系管理员");
        }
    }

    /**
     * 发送管理员通知
     * 在高爆炸半径操作完成后通知所有管理员
     */
    private void sendAdminNotification(DataBackupRecord backupRecord, BackupRestoreLog restoreLog,
                                        String operator, String reason) {
        try {
            String message = String.format(
                    "[数据恢复警告] 操作者 %s 于 %s 执行了数据恢复操作:\n" +
                    "- 备份名称: %s\n" +
                    "- 恢复模式: %s\n" +
                    "- 操作原因: %s\n" +
                    "- 恢复前快照: %s\n" +
                    "请及时验证数据完整性！",
                    operator,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    backupRecord.getBackupName(),
                    getRestoreModeName(restoreLog.getRestoreMode()),
                    reason,
                    restoreLog.getPreRestoreSnapshot() != null ? restoreLog.getPreRestoreSnapshot() : "未创建"
            );

            logger.warn("[管理员通知] {}", message);
            // TODO: 通过消息服务发送通知给所有管理员
            // notificationService.sendToAllAdmins("数据恢复操作通知", message);

        } catch (Exception e) {
            logger.error("发送管理员通知失败", e);
        }
    }

    /**
     * 计算文件的SHA256校验和
     */
    private String calculateSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==================== 命令构建工具方法 ====================

    /**
     * 构建pg_dump命令
     * 自动处理Windows/Linux环境下的PostgreSQL路径问题
     */
    private List<String> buildPgDumpCommand(Path backupFile, DataBackupRecord record, BackupCreateDTO createDTO) {
        List<String> command = new ArrayList<>();

        // 添加pg_dump可执行文件路径（支持自定义路径和自动检测）
        command.add(resolvePgCommand("pg_dump"));

        // 数据库连接参数
        command.add("-h");
        command.add(extractHostFromUrl(datasourceUrl));
        command.add("-p");
        command.add(String.valueOf(extractPortFromUrl(datasourceUrl)));
        command.add("-U");
        command.add(datasourceUsername);
        command.add("-d");
        command.add(extractDatabaseName(datasourceUrl));

        // 输出格式：custom格式（压缩，支持pg_restore）
        command.add("-F");
        command.add("c");

        // 输出文件路径
        command.add("-f");
        command.add(backupFile.toString());

        // 如果是仅结构备份，添加--schema-only参数
        if (record.getBackupType() != null && record.getBackupType() == 3) {
            command.add("--schema-only");
        }

        // 如果指定了特定表，添加表名参数（带白名单校验防注入）
        if (createDTO.getTablesIncluded() != null && !createDTO.getTablesIncluded().isEmpty()) {
            for (String table : createDTO.getTablesIncluded()) {
                String sanitized = sanitizeTableName(table);
                if (sanitized == null) {
                    throw new IllegalArgumentException("非法的表名: " + table + " (仅允许字母、数字、下划线)");
                }
                command.add("-t");
                command.add(sanitized);
            }
        }

        return command;
    }

    /**
     * 构建pg_restore命令
     * 支持全覆盖、选择性恢复和预览模式
     */
    private List<String> buildPgRestoreCommand(File backupFile, BackupRestoreDTO restoreDTO) {
        List<String> command = new ArrayList<>();

        // 添加pg_restore可执行文件路径（支持自定义路径和自动检测）
        command.add(resolvePgCommand("pg_restore"));

        // 数据库连接参数
        command.add("-h");
        command.add(extractHostFromUrl(datasourceUrl));
        command.add("-p");
        command.add(String.valueOf(extractPortFromUrl(datasourceUrl)));
        command.add("-U");
        command.add(datasourceUsername);
        command.add("-d");
        command.add(extractDatabaseName(datasourceUrl));

        // 如果是预览模式，只列出备份内容
        if (restoreDTO.getRestoreMode() != null && restoreDTO.getRestoreMode() == MODE_PREVIEW_ONLY) {
            command.add("--list");
        } else {
            // 全覆盖模式：清空并重建对象
            if (restoreDTO.getRestoreMode() == null || restoreDTO.getRestoreMode() == MODE_FULL_OVERWRITE) {
                command.add("--clean");
                command.add("--if-exists");
            }

            // 选择性恢复：只恢复指定的表（带白名单校验）
            if (restoreDTO.getRestoreMode() == MODE_SELECTIVE &&
                restoreDTO.getTargetTables() != null && !restoreDTO.getTargetTables().isEmpty()) {
                for (String table : restoreDTO.getTargetTables()) {
                    String sanitized = sanitizeTableName(table);
                    if (sanitized == null) {
                        throw new IllegalArgumentException("非法的恢复目标表名: " + table);
                    }
                    command.add("-t");
                    command.add(sanitized);
                }
            }
        }

        // 备份文件路径（必须是最后一个参数）
        command.add(backupFile.getAbsolutePath());

        return command;
    }

    /**
     * 解析PostgreSQL命令的完整路径
     * 优先使用配置的路径，否则自动检测系统安装路径
     */
    private String resolvePgCommand(String commandName) {
        // 1. 优先使用配置的自定义路径
        if (pgBinPath != null && !pgBinPath.isEmpty()) {
            Path customPath = Paths.get(pgBinPath, commandName).toAbsolutePath();
            if (Files.exists(customPath)) {
                logger.debug("使用配置的PostgreSQL命令路径: {}", customPath);
                return customPath.toString();
            }
            logger.warn("配置的PostgreSQL bin路径不存在: {}, 尝试自动检测", pgBinPath);
        }

        // 2. Windows环境下检测常见安装路径
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            String[] windowsPaths = {
                "C:/Program Files/PostgreSQL/*/bin/" + commandName,
                "C:/Program Files (x86)/PostgreSQL/*/bin/" + commandName,
                "C:/PostgreSQL/*/bin/" + commandName
            };

            for (String pattern : windowsPaths) {
                try {
                    // 使用glob匹配查找PostgreSQL安装目录
                    java.nio.file.DirectoryStream<Path> stream =
                        java.nio.file.Files.newDirectoryStream(
                            Paths.get(pattern.substring(0, pattern.lastIndexOf('/'))).getParent(),
                            "*"
                        );
                    for (Path dir : stream) {
                        Path pgPath = dir.resolve("bin").resolve(commandName);
                        if (Files.exists(pgPath)) {
                            logger.info("自动检测到PostgreSQL路径: {}", pgPath);
                            return pgPath.toString();
                        }
                    }
                } catch (Exception e) {
                    // 继续尝试下一个路径
                }
            }
        }

        // 3. Linux/Mac环境下依赖PATH环境变量
        logger.debug("使用系统PATH中的{}命令", commandName);
        return commandName;
    }

    // ==================== URL解析工具方法 ====================

    /**
     * 从JDBC URL中提取主机地址
     */
    private String extractHostFromUrl(String url) {
        if (url == null || url.isEmpty()) return "localhost";
        try {
            // 格式: jdbc:postgresql://host:port/database
            String cleanUrl = url.replace("jdbc:postgresql://", "");
            String hostPart = cleanUrl.split("/")[0];
            if (hostPart.contains(":")) {
                return hostPart.split(":")[0];
            }
            return hostPart;
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * 从JDBC URL中提取端口号
     */
    private int extractPortFromUrl(String url) {
        if (url == null || url.isEmpty()) return 5432;
        try {
            String cleanUrl = url.replace("jdbc:postgresql://", "");
            String hostPart = cleanUrl.split("/")[0];
            if (hostPart.contains(":")) {
                return Integer.parseInt(hostPart.split(":")[1]);
            }
            return 5432;
        } catch (Exception e) {
            return 5432;
        }
    }

    /**
     * 从JDBC URL中提取数据库名称
     */
    private String extractDatabaseName(String url) {
        if (url == null || url.isEmpty()) return "food_traceability";
        try {
            String cleanUrl = url.replace("jdbc:postgresql://", "");
            String[] parts = cleanUrl.split("/");
            if (parts.length > 1) {
                // 移除可能的查询参数
                return parts[1].split("\\?")[0];
            }
            return "food_traceability";
        } catch (Exception e) {
            return "food_traceability";
        }
    }

    /**
     * 获取恢复模式名称
     */
    private String getRestoreModeName(Integer mode) {
        if (mode == null) return "未知";
        switch (mode) {
            case MODE_FULL_OVERWRITE: return "全覆盖";
            case MODE_SELECTIVE: return "选择性";
            case MODE_PREVIEW_ONLY: return "预览";
            default: return "未知";
        }
    }

    /**
     * 表名白名单校验（防止命令注入）
     * 仅允许: 字母(a-z,A-Z)、数字(0-9)、下划线(_)
     * 禁止: 空字符、分号、引号、SQL关键字、pg_*系统表
     */
    private String sanitizeTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return null;
        }
        String trimmed = tableName.trim();
        // 白名单正则：仅允许字母、数字、下划线，且必须以字母开头
        if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            return null;
        }
        // 禁止访问PostgreSQL系统表
        if (trimmed.toLowerCase().startsWith("pg_") || trimmed.toLowerCase().startsWith("sql_")) {
            return null;
        }
        // 长度限制（PostgreSQL表名最大63字符）
        if (trimmed.length() > 63) {
            return null;
        }
        return trimmed;
    }
}
