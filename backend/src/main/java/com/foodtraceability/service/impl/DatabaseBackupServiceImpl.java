package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.BackupProgress;
import com.foodtraceability.service.BackupNotificationService;
import com.foodtraceability.service.CloudStorageService;
import com.foodtraceability.service.DatabaseBackupService;
import com.foodtraceability.service.SysSettingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 数据库备份服务实现
 */
@Service
public class DatabaseBackupServiceImpl implements DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupServiceImpl.class);

    public DatabaseBackupServiceImpl(JdbcTemplate jdbcTemplate, SysSettingService sysSettingService, CloudStorageService cloudStorageService, BackupNotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sysSettingService = sysSettingService;
        this.cloudStorageService = cloudStorageService;
        this.notificationService = notificationService;
    }

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/food_traceability}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:postgres}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.backup.storage-path:./backups}")
    private String backupStoragePath;

    @Value("${app.backup.retain-days:30}")
    private int defaultRetainDays;

    @Value("${app.backup.compress:false}")
    private boolean compressBackup;

    @Value("${app.backup.encrypt:false}")
    private boolean encryptBackup;

    @Value("${app.backup.encryption-key:}")
    private String encryptionKey;

    private final JdbcTemplate jdbcTemplate;

    private final SysSettingService sysSettingService;

    private final CloudStorageService cloudStorageService;

    private final BackupNotificationService notificationService;

    @Value("${app.backup.cloud-upload:false}")
    private boolean autoUploadToCloud;

    @Value("${app.backup.storage-threshold:80}")
    private int storageThresholdPercent;

    // 内存中存储备份记录（生产环境应使用数据库表）
    private final Map<String, BackupInfo> backupRecords = new ConcurrentHashMap<>();

    // 内存中存储备份进度（用于断点继续备份）
    private final Map<String, BackupProgress> backupProgressMap = new ConcurrentHashMap<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @PostConstruct
    public void init() {
        // 创建备份目录
        try {
            Path backupPath = Paths.get(backupStoragePath);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
                log.info("创建备份目录: {}", backupPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("创建备份目录失败", e);
        }

        // 加载已有的备份记录
        loadExistingBackups();
    }

    @Override
    public Result<String> manualBackup(String backupName, String description, String operator) {
        log.info("开始执行手动备份, 操作人: {}", operator);
        return performBackup(backupName, description, operator, "MANUAL");
    }

    @Override
    public boolean autoBackup() {
        log.info("开始执行自动备份");
        Result<String> result = performBackup(null, "系统自动备份", "system", "AUTO");
        return result.getCode() == 0 || result.getCode() == 200;
    }

    /**
     * 执行备份操作
     */
    private Result<String> performBackup(String backupName, String description, String operator, String backupType) {
        String backupId = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now().format(formatter);

        // 获取动态配置的备份路径
        String currentBackupPath = getBackupStoragePath();

        // 确保备份目录存在
        try {
            Path backupDir = Paths.get(currentBackupPath);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                log.info("创建备份目录: {}", backupDir.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("创建备份目录失败", e);
            return Result.error("创建备份目录失败: " + e.getMessage());
        }

        // 解析数据库名称
        String databaseName = extractDatabaseName(jdbcUrl);
        if (databaseName == null) {
            return Result.error("无法解析数据库名称");
        }

        // 生成文件名
        String fileName = generateBackupFileName(backupName, timestamp, databaseName, backupType);
        String filePath = currentBackupPath + File.separator + fileName;

        BackupInfo backupInfo = new BackupInfo();
        backupInfo.setBackupId(backupId);
        backupInfo.setBackupName(backupName != null ? backupName : ("AUTO".equals(backupType) ? "自动备份_" + timestamp : "手动备份_" + timestamp));
        backupInfo.setFileName(fileName);
        backupInfo.setFilePath(filePath);
        backupInfo.setBackupTime(LocalDateTime.now());
        backupInfo.setBackupType(backupType);
        backupInfo.setDescription(description);
        backupInfo.setOperator(operator);
        backupInfo.setStatus("RUNNING");

        // 记录备份开始时间
        LocalDateTime backupStartTime = LocalDateTime.now();

        try {
            // 检查存储空间
            checkStorageSpace();

            // 执行Java方式备份
            boolean success;
            String actualFilePath = filePath;

            if (compressBackup) {
                actualFilePath = filePath + ".gz";
                success = executeJavaBackupWithCompression(filePath, actualFilePath);
            } else {
                success = executeJavaBackup(filePath);
            }

            if (success) {
                // 获取文件大小
                File backupFile = new File(actualFilePath);
                long fileSize = backupFile.length();
                backupInfo.setFileSize(fileSize);
                backupInfo.setFileSizeFormatted(formatFileSize(fileSize));
                backupInfo.setStatus("SUCCESS");

                // 保存备份记录
                backupRecords.put(backupId, backupInfo);
                saveBackupRecord(backupInfo);

                // 计算备份耗时
                long duration = java.time.Duration.between(backupStartTime, LocalDateTime.now()).getSeconds();

                log.info("备份成功: {}, 文件大小: {}, 耗时: {} 秒", fileName, backupInfo.getFileSizeFormatted(), duration);

                // 发送备份成功通知
                notificationService.sendBackupSuccessNotification(
                    backupId,
                    backupInfo.getBackupName(),
                    backupInfo.getFileSizeFormatted(),
                    duration
                );

                // 自动上传到云存储
                if (autoUploadToCloud) {
                    log.info("自动上传备份到云存储: {}", backupId);
                    Result<Map<String, Object>> uploadResult = uploadBackupToCloud(backupId);
                    if (!uploadResult.isSuccess()) {
                        log.warn("自动上传备份失败: {}", uploadResult.getMessage());
                    }
                }

                // 清理过期备份（使用动态配置的保留天数）
                cleanupOldBackups(getRetainDays());

                return Result.success(backupId);
            } else {
                backupInfo.setStatus("FAILED");
                backupInfo.setErrorMessage("备份执行失败");
                backupRecords.put(backupId, backupInfo);
                saveBackupRecord(backupInfo);

                // 发送备份失败通知
                notificationService.sendBackupFailureNotification(
                    backupId,
                    backupInfo.getBackupName(),
                    "备份执行失败"
                );

                log.error("备份失败: {}", fileName);
                return Result.error("备份失败");
            }
        } catch (Exception e) {
            backupInfo.setStatus("FAILED");
            backupInfo.setErrorMessage(e.getMessage());
            backupRecords.put(backupId, backupInfo);
            saveBackupRecord(backupInfo);

            // 发送备份失败通知
            notificationService.sendBackupFailureNotification(
                backupId,
                backupInfo.getBackupName(),
                e.getMessage()
            );

            log.error("备份异常", e);
            return Result.error("备份异常: " + e.getMessage());
        }
    }

    /**
     * 使用Java代码执行数据库备份（带压缩）
     */
    private boolean executeJavaBackupWithCompression(String sqlFile, String outputFile) {
        try (FileInputStream fis = new FileInputStream(sqlFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzipOS.write(buffer, 0, len);
            }
            
            gzipOS.finish();
            log.info("备份文件压缩完成: {} -> {}", sqlFile, outputFile);
            return true;
        } catch (Exception e) {
            log.error("备份文件压缩失败", e);
            return false;
        }
    }

    /**
     * 使用Java代码执行数据库备份
     */
    private boolean executeJavaBackup(String outputFile) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // 获取数据库连接
            Connection conn = jdbcTemplate.getDataSource().getConnection();
            DatabaseMetaData metaData = conn.getMetaData();

            // 写入SQL文件头
            writer.println("-- Backup generated at " + LocalDateTime.now());
            writer.println("-- Database: " + extractDatabaseName(jdbcUrl));
            writer.println("SET FOREIGN_KEY_CHECKS=0;");
            writer.println();

            // 获取所有表
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }

            // 创建备份进度跟踪
            BackupProgress progress = new BackupProgress();
            progress.setTotalTables(tableNames.size());
            progress.setStartTime(LocalDateTime.now());
            backupProgressMap.put(outputFile, progress);

            // 备份每个表的结构和数据
            for (int i = 0; i < tableNames.size(); i++) {
                String tableName = tableNames.get(i);
                try {
                    backupTable(conn, metaData, tableName, writer);
                    progress.setCompletedTables(i + 1);
                    progress.setCurrentTable(tableName);
                    progress.setProgress((i + 1) * 100 / tableNames.size());
                    progress.setLastUpdateTime(LocalDateTime.now());
                    
                    // 每10个表记录一次日志
                    if ((i + 1) % 10 == 0) {
                        log.info("备份进度: {}/{} ({:.2f}%)", i + 1, tableNames.size(), progress.getProgress());
                    }
                } catch (Exception e) {
                    log.error("备份表 {} 失败", tableName, e);
                }
            }

            writer.println("SET FOREIGN_KEY_CHECKS=1;");
            writer.println("-- Backup completed");
            writer.flush();

            conn.close();

            // 更新备份进度为完成
            progress.setProgress(100);
            progress.setStatus("COMPLETED");
            progress.setEndTime(LocalDateTime.now());
            progress.setDuration(Duration.between(progress.getStartTime(), progress.getEndTime()).getSeconds());
            
            // 计算备份文件的MD5
            String md5 = calculateFileMD5(new File(outputFile));
            progress.setMd5(md5);
            
            log.info("备份完成: {}, 耗时: {} 秒, MD5: {}", outputFile, progress.getDuration(), md5);
            return true;

        } catch (Exception e) {
            log.error("执行Java备份失败", e);
            return false;
        }
    }

    /**
     * 备份单个表
     */
    private void backupTable(Connection conn, DatabaseMetaData metaData, String tableName, PrintWriter writer) throws SQLException {
        // 写入表结构
        writer.println("-- Table structure for table `" + tableName + "`");
        writer.println("DROP TABLE IF EXISTS `" + tableName + "`;");

        // 获取建表语句
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + tableName + "`");
        if (rs.next()) {
            String createTable = rs.getString(2);
            writer.println(createTable + ";");
        }
        writer.println();

        // 获取表数据
        ResultSet dataRs = stmt.executeQuery("SELECT * FROM `" + tableName + "`");
        ResultSetMetaData dataMeta = dataRs.getMetaData();
        int columnCount = dataMeta.getColumnCount();

        // 写入数据
        writer.println("-- Dumping data for table `" + tableName + "`");

        int rowCount = 0;
        while (dataRs.next()) {
            StringBuilder insert = new StringBuilder();
            insert.append("INSERT INTO `").append(tableName).append("` VALUES (");

            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) insert.append(", ");

                Object value = dataRs.getObject(i);
                if (value == null) {
                    insert.append("NULL");
                } else if (value instanceof String || value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                    String strValue = value.toString().replace("'", "\\'");
                    insert.append("'").append(strValue).append("'");
                } else if (value instanceof Boolean) {
                    insert.append((Boolean) value ? 1 : 0);
                } else {
                    insert.append(value);
                }
            }

            insert.append(");");
            writer.println(insert.toString());
            rowCount++;
        }

        writer.println();
        log.debug("备份表 {} 完成，共 {} 行数据", tableName, rowCount);
        dataRs.close();
        stmt.close();
    }

    /**
     * 计算文件的MD5
     */
    private String calculateFileMD5(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("计算文件MD5失败", e);
            return null;
        }
    }

    /**
     * 加密文件
     */
    private boolean encryptFile(String inputFile, String outputFile) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.info("未配置加密密钥，跳过加密");
            return false;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            try (FileInputStream fis = new FileInputStream(inputFile);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] encrypted = cipher.update(buffer, 0, bytesRead);
                    fos.write(encrypted);
                }

                byte[] finalBytes = cipher.doFinal();
                fos.write(finalBytes);
            }

            log.info("文件加密完成: {} -> {}", inputFile, outputFile);
            return true;
        } catch (Exception e) {
            log.error("文件加密失败", e);
            return false;
        }
    }

    /**
     * 解密文件
     */
    private boolean decryptFile(String inputFile, String outputFile) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.info("未配置加密密钥，跳过解密");
            return false;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            try (FileInputStream fis = new FileInputStream(inputFile);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] decrypted = cipher.update(buffer, 0, bytesRead);
                    fos.write(decrypted);
                }

                byte[] finalBytes = cipher.doFinal();
                fos.write(finalBytes);
            }

            log.info("文件解密完成: {} -> {}", inputFile, outputFile);
            return true;
        } catch (Exception e) {
            log.error("文件解密失败", e);
            return false;
        }
    }

    @Override
    public Result<Void> restoreBackup(String backupId, String operator) {
        log.info("开始恢复备份, backupId: {}, 操作人: {}", backupId, operator);

        BackupInfo backupInfo = backupRecords.get(backupId);
        if (backupInfo == null) {
            return Result.error("备份记录不存在");
        }

        if (!"SUCCESS".equals(backupInfo.getStatus())) {
            return Result.error("备份文件不可用");
        }

        File backupFile = new File(backupInfo.getFilePath());
        if (!backupFile.exists()) {
            return Result.error("备份文件不存在");
        }

        try {
            // 恢复前自动备份当前数据库
            log.info("恢复前自动备份当前数据库");
            Result<String> preBackupResult = performBackup("恢复前备份_" + LocalDateTime.now().format(formatter), "恢复前自动备份", operator, "PRE_RESTORE");
            
            if (preBackupResult.getCode() != 0 && preBackupResult.getCode() != 200) {
                log.error("恢复前备份失败，取消恢复操作");
                return Result.error("恢复前备份失败，取消恢复操作");
            }
            
            String preBackupId = preBackupResult.getData();
            log.info("恢复前备份成功，备份ID: {}", preBackupId);

            // 执行Java方式恢复
            boolean success = executeJavaRestore(backupInfo.getFilePath());

            if (success) {
                log.info("恢复成功: {}", backupId);

                // 发送恢复成功通知
                notificationService.sendRestoreSuccessNotification(
                    backupId,
                    backupInfo.getBackupName(),
                    operator
                );

                return Result.success();
            } else {
                // 恢复失败，清理恢复前备份
                log.error("恢复失败，清理恢复前备份");
                deleteBackup(preBackupId);

                // 发送恢复失败通知
                notificationService.sendRestoreFailureNotification(
                    backupId,
                    backupInfo.getBackupName(),
                    operator,
                    "恢复执行失败"
                );

                return Result.error("恢复失败");
            }
        } catch (Exception e) {
            log.error("恢复异常", e);

            // 发送恢复失败通知
            notificationService.sendRestoreFailureNotification(
                backupId,
                backupInfo.getBackupName(),
                operator,
                e.getMessage()
            );

            return Result.error("恢复异常: " + e.getMessage());
        }
    }

    /**
     * 使用Java代码执行数据库恢复
     */
    private boolean executeJavaRestore(String backupFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
            Connection conn = jdbcTemplate.getDataSource().getConnection();
            Statement stmt = conn.createStatement();

            // 禁用外键检查
            try {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                log.info("已禁用外键检查");
            } catch (SQLException e) {
                log.warn("禁用外键检查失败: {}", e.getMessage());
            }

            StringBuilder sqlBuffer = new StringBuilder();
            String line;
            int lineCount = 0;
            int executedCount = 0;
            int errorCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;

                // 跳过注释和空行
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("--") || trimmedLine.isEmpty()) {
                    continue;
                }

                // 跳过 SET FOREIGN_KEY_CHECKS 语句（我们已经手动执行了）
                if (trimmedLine.toUpperCase().contains("FOREIGN_KEY_CHECKS")) {
                    continue;
                }

                // 添加当前行到缓冲区
                if (sqlBuffer.length() > 0) {
                    sqlBuffer.append(" ");
                }
                sqlBuffer.append(trimmedLine);

                // 如果语句以分号结束，执行它
                if (trimmedLine.endsWith(";")) {
                    String sql = sqlBuffer.toString();
                    sqlBuffer.setLength(0);

                    try {
                        stmt.execute(sql);
                        executedCount++;
                        
                        // 每100条SQL记录一次日志
                        if (executedCount % 100 == 0) {
                            log.info("已执行 {} 条SQL", executedCount);
                        }
                    } catch (SQLException e) {
                        errorCount++;
                        // 只记录错误信息的前100个字符，避免日志过长
                        String errorSql = sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
                        log.error("执行SQL失败 (错误 #{}): {}, 错误: {}", errorCount, errorSql, e.getMessage());
                        
                        // 如果错误太多，可能是严重问题，停止恢复
                        if (errorCount > 50) {
                            log.error("错误太多 ({} 个)，停止恢复", errorCount);
                            break;
                        }
                    }
                }
            }

            // 执行剩余的SQL（如果有）
            if (sqlBuffer.length() > 0) {
                String sql = sqlBuffer.toString();
                try {
                    stmt.execute(sql);
                    executedCount++;
                } catch (SQLException e) {
                    errorCount++;
                    log.error("执行最后SQL失败: {}, 错误: {}", sql.substring(0, Math.min(100, sql.length())), e.getMessage());
                }
            }

            // 重新启用外键检查
            try {
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
                log.info("已重新启用外键检查");
            } catch (SQLException e) {
                log.warn("重新启用外键检查失败: {}", e.getMessage());
            }

            stmt.close();
            conn.close();

            log.info("恢复完成，共读取 {} 行，成功执行 {} 条SQL，失败 {} 条", lineCount, executedCount, errorCount);
            return errorCount == 0 || (executedCount > 0 && (double)errorCount / executedCount < 0.1);

        } catch (Exception e) {
            log.error("执行Java恢复失败", e);
            return false;
        }
    }

    @Override
    public Result<Void> deleteBackup(String backupId) {
        BackupInfo backupInfo = backupRecords.get(backupId);
        if (backupInfo == null) {
            return Result.error("备份记录不存在");
        }

        try {
            // 删除文件
            File file = new File(backupInfo.getFilePath());
            if (file.exists()) {
                file.delete();
            }

            // 删除记录
            backupRecords.remove(backupId);
            deleteBackupRecord(backupId);

            log.info("删除备份成功: {}", backupId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除备份失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<BackupInfo>> getBackupList() {
        List<BackupInfo> list = backupRecords.values().stream()
                .sorted((a, b) -> b.getBackupTime().compareTo(a.getBackupTime()))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @Override
    public Result<Void> cleanupOldBackups(int retainDays) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retainDays);

            List<String> toDelete = backupRecords.values().stream()
                    .filter(backup -> backup.getBackupTime().isBefore(cutoffTime))
                    .filter(backup -> "AUTO".equals(backup.getBackupType())) // 只删除自动备份
                    .map(BackupInfo::getBackupId)
                    .collect(Collectors.toList());

            for (String backupId : toDelete) {
                deleteBackup(backupId);
            }

            log.info("清理过期备份完成，删除 {} 个备份", toDelete.size());
            return Result.success();
        } catch (Exception e) {
            log.error("清理过期备份失败", e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }

    @Override
    public String getBackupStoragePath() {
        // 优先从系统设置中读取
        try {
            Result<String> pathResult = sysSettingService.getSettingValue("backup_storage_path", "global", null);
            if (pathResult.isSuccess() && pathResult.getData() != null && !pathResult.getData().isEmpty()) {
                return pathResult.getData();
            }
        } catch (Exception e) {
            log.warn("从系统设置读取备份路径失败，使用默认配置", e);
        }
        return backupStoragePath;
    }

    /**
     * 获取备份保留天数
     */
    private int getRetainDays() {
        try {
            Result<String> daysResult = sysSettingService.getSettingValue("backup_retain_days", "global", null);
            if (daysResult.isSuccess() && daysResult.getData() != null && !daysResult.getData().isEmpty()) {
                return Integer.parseInt(daysResult.getData());
            }
        } catch (Exception e) {
            log.warn("从系统设置读取保留天数失败，使用默认配置", e);
        }
        return defaultRetainDays;
    }

    /**
     * 解析数据库名称
     */
    private String extractDatabaseName(String jdbcUrl) {
        try {
            // 解析 jdbc:postgresql://localhost:5432/database_name?params 或 jdbc:h2:mem:testdb;params
            int lastSlash = jdbcUrl.lastIndexOf('/');
            int questionMark = jdbcUrl.indexOf('?', lastSlash);

            if (lastSlash > 0) {
                if (questionMark > lastSlash) {
                    return jdbcUrl.substring(lastSlash + 1, questionMark);
                } else {
                    return jdbcUrl.substring(lastSlash + 1);
                }
            }
        } catch (Exception e) {
            log.error("解析数据库名称失败", e);
        }
        return null;
    }

    /**
     * 生成备份文件名
     */
    private String generateBackupFileName(String backupName, String timestamp, String databaseName, String backupType) {
        String baseName = backupName != null ? backupName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") : databaseName;
        String typePrefix = "AUTO".equals(backupType) ? "auto" : "manual";
        return String.format("%s_%s_%s_%s.sql", typePrefix, baseName, timestamp, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 加载已有的备份文件
     */
    private void loadExistingBackups() {
        try {
            String currentBackupPath = getBackupStoragePath();
            Path backupPath = Paths.get(currentBackupPath);
            if (!Files.exists(backupPath)) {
                return;
            }

            Files.list(backupPath)
                    .filter(path -> path.toString().endsWith(".sql"))
                    .forEach(path -> {
                        try {
                            File file = path.toFile();
                            BackupInfo info = new BackupInfo();
                            info.setBackupId(UUID.randomUUID().toString().replace("-", ""));
                            info.setBackupName(file.getName());
                            info.setFileName(file.getName());
                            info.setFilePath(file.getAbsolutePath());
                            info.setFileSize(file.length());
                            info.setFileSizeFormatted(formatFileSize(file.length()));
                            info.setBackupTime(LocalDateTime.ofInstant(
                                    Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant(),
                                    ZoneId.systemDefault()));
                            info.setBackupType(file.getName().startsWith("auto") ? "AUTO" : "MANUAL");
                            info.setDescription("历史备份文件");
                            info.setOperator("system");
                            info.setStatus("SUCCESS");

                            backupRecords.put(info.getBackupId(), info);
                        } catch (Exception e) {
                            log.error("加载备份文件失败: {}", path, e);
                        }
                    });

            log.info("加载了 {} 个历史备份文件", backupRecords.size());
        } catch (Exception e) {
            log.error("加载备份文件列表失败", e);
        }
    }

    /**
     * 保存备份记录到文件（简单实现）
     */
    private void saveBackupRecord(BackupInfo backupInfo) {
        // 生产环境应该保存到数据库
        // 这里仅作演示，记录已保存在内存中
    }

    /**
     * 删除备份记录
     */
    private void deleteBackupRecord(String backupId) {
        // 生产环境应该从数据库删除
        // 这里仅作演示，记录已从内存中删除
    }

    /**
     * 检查外键约束
     */
    public Result<Map<String, Object>> checkForeignKeyConstraints() {
        log.info("开始检查外键约束");
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> constraints = new ArrayList<>();
        List<Map<String, String>> violations = new ArrayList<>();

        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseName = extractDatabaseName(jdbcUrl);

            // 获取所有表
            ResultSet tables = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                // 获取外键信息
                ResultSet foreignKeys = metaData.getImportedKeys(databaseName, null, tableName);
                while (foreignKeys.next()) {
                    Map<String, String> fk = new HashMap<>();
                    fk.put("table", tableName);
                    fk.put("column", foreignKeys.getString("FKCOLUMN_NAME"));
                    fk.put("referencedTable", foreignKeys.getString("PKTABLE_NAME"));
                    fk.put("referencedColumn", foreignKeys.getString("PKCOLUMN_NAME"));
                    fk.put("constraintName", foreignKeys.getString("FK_NAME"));
                    constraints.add(fk);

                    // 检查外键约束是否被违反
                    checkForeignKeyViolation(conn, tableName, fk.get("column"),
                            fk.get("referencedTable"), fk.get("referencedColumn"), violations);
                }
            }

            result.put("constraints", constraints);
            result.put("violations", violations);
            result.put("constraintCount", constraints.size());
            result.put("violationCount", violations.size());

            if (violations.isEmpty()) {
                log.info("外键约束检查完成，未发现违反约束的数据");
                return Result.success(result);
            } else {
                log.warn("外键约束检查完成，发现 {} 个违反约束的数据", violations.size());
                return Result.error("发现外键约束违反: " + violations.size() + " 个", result);
            }

        } catch (Exception e) {
            log.error("检查外键约束失败", e);
            return Result.error("检查外键约束失败: " + e.getMessage());
        }
    }

    /**
     * 检查外键约束是否被违反
     */
    private void checkForeignKeyViolation(Connection conn, String tableName, String columnName,
                                          String refTableName, String refColumnName,
                                          List<Map<String, String>> violations) throws SQLException {
        String sql = String.format(
            "SELECT t.%s FROM %s t LEFT JOIN %s r ON t.%s = r.%s WHERE t.%s IS NOT NULL AND r.%s IS NULL LIMIT 10",
            columnName, tableName, refTableName, columnName, refColumnName, columnName, refColumnName
        );

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> violation = new HashMap<>();
                violation.put("table", tableName);
                violation.put("column", columnName);
                violation.put("value", rs.getString(1));
                violation.put("referencedTable", refTableName);
                violation.put("referencedColumn", refColumnName);
                violations.add(violation);
            }
        }
    }

    /**
     * 检查字符集和排序规则
     */
    public Result<Map<String, Object>> checkCharsetAndCollation() {
        log.info("开始检查字符集和排序规则");
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> tableCharsets = new ArrayList<>();
        List<Map<String, String>> columnCharsets = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            String databaseName = extractDatabaseName(jdbcUrl);

            // 检查数据库默认字符集
            String dbCharsetSql = "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME " +
                    "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(dbCharsetSql)) {
                pstmt.setString(1, databaseName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    result.put("databaseCharset", rs.getString("DEFAULT_CHARACTER_SET_NAME"));
                    result.put("databaseCollation", rs.getString("DEFAULT_COLLATION_NAME"));
                }
            }

            // 检查表的字符集
            String tableCharsetSql = "SELECT TABLE_NAME, TABLE_COLLATION " +
                    "FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'";
            try (PreparedStatement pstmt = conn.prepareStatement(tableCharsetSql)) {
                pstmt.setString(1, databaseName);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Map<String, String> info = new HashMap<>();
                    info.put("tableName", rs.getString("TABLE_NAME"));
                    info.put("collation", rs.getString("TABLE_COLLATION"));
                    tableCharsets.add(info);

                    // 检查是否与数据库默认字符集不一致
                    String dbCollation = (String) result.get("databaseCollation");
                    if (dbCollation != null && !dbCollation.equals(rs.getString("TABLE_COLLATION"))) {
                        warnings.add(String.format("表 %s 的排序规则(%s)与数据库默认(%s)不一致",
                                rs.getString("TABLE_NAME"), rs.getString("TABLE_COLLATION"), dbCollation));
                    }
                }
            }

            // 检查列的字符集
            String columnCharsetSql = "SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME " +
                    "FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = ? AND CHARACTER_SET_NAME IS NOT NULL";
            try (PreparedStatement pstmt = conn.prepareStatement(columnCharsetSql)) {
                pstmt.setString(1, databaseName);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Map<String, String> info = new HashMap<>();
                    info.put("tableName", rs.getString("TABLE_NAME"));
                    info.put("columnName", rs.getString("COLUMN_NAME"));
                    info.put("charset", rs.getString("CHARACTER_SET_NAME"));
                    info.put("collation", rs.getString("COLLATION_NAME"));
                    columnCharsets.add(info);
                }
            }

            result.put("tableCharsets", tableCharsets);
            result.put("columnCharsets", columnCharsets);
            result.put("warnings", warnings);
            result.put("tableCount", tableCharsets.size());
            result.put("columnCount", columnCharsets.size());

            log.info("字符集检查完成，表数量: {}, 列数量: {}, 警告: {}",
                    tableCharsets.size(), columnCharsets.size(), warnings.size());
            return Result.success(result);

        } catch (Exception e) {
            log.error("检查字符集失败", e);
            return Result.error("检查字符集失败: " + e.getMessage());
        }
    }

    /**
     * 检查数据类型和长度
     */
    public Result<Map<String, Object>> checkDataTypesAndLength() {
        log.info("开始检查数据类型和长度");
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> columnInfo = new ArrayList<>();
        List<Map<String, Object>> potentialIssues = new ArrayList<>();

        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseName = extractDatabaseName(jdbcUrl);

            // 获取所有表
            ResultSet tables = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                // 获取列信息
                ResultSet columns = metaData.getColumns(databaseName, null, tableName, "%");
                while (columns.next()) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("tableName", tableName);
                    info.put("columnName", columns.getString("COLUMN_NAME"));
                    info.put("dataType", columns.getInt("DATA_TYPE"));
                    info.put("typeName", columns.getString("TYPE_NAME"));
                    info.put("columnSize", columns.getInt("COLUMN_SIZE"));
                    info.put("decimalDigits", columns.getInt("DECIMAL_DIGITS"));
                    info.put("nullable", columns.getInt("NULLABLE"));
                    info.put("columnDef", columns.getString("COLUMN_DEF"));
                    columnInfo.add(info);

                    // 检查潜在问题
                    checkPotentialDataIssues(conn, tableName, info, potentialIssues);
                }
            }

            result.put("columnInfo", columnInfo);
            result.put("potentialIssues", potentialIssues);
            result.put("columnCount", columnInfo.size());
            result.put("issueCount", potentialIssues.size());

            log.info("数据类型检查完成，列数量: {}, 潜在问题: {}",
                    columnInfo.size(), potentialIssues.size());
            return Result.success(result);

        } catch (Exception e) {
            log.error("检查数据类型失败", e);
            return Result.error("检查数据类型失败: " + e.getMessage());
        }
    }

    /**
     * 检查潜在的数据问题
     */
    private void checkPotentialDataIssues(Connection conn, String tableName,
                                          Map<String, Object> columnInfo,
                                          List<Map<String, Object>> issues) throws SQLException {
        String columnName = (String) columnInfo.get("columnName");
        String typeName = (String) columnInfo.get("typeName");
        int columnSize = (Integer) columnInfo.get("columnSize");

        // 检查VARCHAR字段是否有超长数据
        if (typeName.toLowerCase().contains("varchar") && columnSize > 0) {
            String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE LENGTH(%s) > %d",
                tableName, columnName, columnSize
            );
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("tableName", tableName);
                    issue.put("columnName", columnName);
                    issue.put("issueType", "DATA_TOO_LONG");
                    issue.put("description", String.format("有 %d 条记录的数据长度超过字段定义(%d)",
                            rs.getInt(1), columnSize));
                    issues.add(issue);
                }
            }
        }

        // 检查数值字段是否有NULL值（如果定义为NOT NULL）
        int nullable = (Integer) columnInfo.get("nullable");
        if (nullable == DatabaseMetaData.columnNoNulls) {
            String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s IS NULL", tableName, columnName);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("tableName", tableName);
                    issue.put("columnName", columnName);
                    issue.put("issueType", "NULL_VIOLATION");
                    issue.put("description", String.format("有 %d 条记录在NOT NULL字段上有NULL值", rs.getInt(1)));
                    issues.add(issue);
                }
            }
        }
    }

    /**
     * 验证备份文件的完整性
     */
    public Result<Map<String, Object>> verifyBackupIntegrity(String backupId) {
        log.info("开始验证备份完整性, backupId: {}", backupId);
        Map<String, Object> result = new HashMap<>();

        BackupInfo backupInfo = backupRecords.get(backupId);
        if (backupInfo == null) {
            return Result.error("备份记录不存在");
        }

        File backupFile = new File(backupInfo.getFilePath());
        if (!backupFile.exists()) {
            return Result.error("备份文件不存在");
        }

        try {
            // 1. 检查文件大小
            long fileSize = backupFile.length();
            result.put("fileSize", fileSize);
            result.put("fileSizeFormatted", formatFileSize(fileSize));

            if (fileSize == 0) {
                return Result.error("备份文件大小为0", result);
            }

            // 2. 计算并验证MD5
            String currentMD5 = calculateFileMD5(backupFile);
            result.put("currentMD5", currentMD5);

            // 3. 检查SQL语法（简单检查）
            int lineCount = 0;
            int statementCount = 0;
            int errorCount = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (line.trim().endsWith(";")) {
                        statementCount++;
                    }
                    if (line.toLowerCase().contains("error") || line.toLowerCase().contains("exception")) {
                        errorCount++;
                    }
                }
            }

            result.put("lineCount", lineCount);
            result.put("statementCount", statementCount);
            result.put("errorKeywords", errorCount);

            // 4. 检查必要的SQL语句
            boolean hasSetForeignKeyChecks = false;
            boolean hasCreateTable = false;
            boolean hasInsert = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String upperLine = line.toUpperCase();
                    if (upperLine.contains("SET FOREIGN_KEY_CHECKS")) {
                        hasSetForeignKeyChecks = true;
                    }
                    if (upperLine.contains("CREATE TABLE")) {
                        hasCreateTable = true;
                    }
                    if (upperLine.contains("INSERT INTO")) {
                        hasInsert = true;
                    }
                }
            }

            result.put("hasSetForeignKeyChecks", hasSetForeignKeyChecks);
            result.put("hasCreateTable", hasCreateTable);
            result.put("hasInsert", hasInsert);

            // 综合判断
            boolean isValid = fileSize > 0 && statementCount > 0 && hasCreateTable;
            result.put("isValid", isValid);

            if (isValid) {
                log.info("备份文件验证通过: {}", backupId);
                return Result.success(result);
            } else {
                log.warn("备份文件验证失败: {}", backupId);
                return Result.error("备份文件验证失败", result);
            }

        } catch (Exception e) {
            log.error("验证备份完整性失败", e);
            return Result.error("验证失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> uploadBackupToCloud(String backupId) {
        log.info("上传备份到云存储, backupId: {}", backupId);

        BackupInfo backupInfo = backupRecords.get(backupId);
        if (backupInfo == null) {
            return Result.error("备份记录不存在");
        }

        File backupFile = new File(backupInfo.getFilePath());
        if (!backupFile.exists()) {
            return Result.error("备份文件不存在");
        }

        // 检查云存储可用性
        Result<Boolean> availabilityResult = cloudStorageService.checkAvailability();
        if (!availabilityResult.isSuccess() || !availabilityResult.getData()) {
            return Result.error("云存储服务不可用");
        }

        // 构建远程路径
        String remotePath = String.format("backups/%s/%s",
                backupInfo.getBackupTime().format(DateTimeFormatter.ofPattern("yyyy/MM")),
                backupInfo.getFileName());

        // 上传文件
        Result<Map<String, Object>> uploadResult = cloudStorageService.uploadFile(backupFile, remotePath);

        if (uploadResult.isSuccess()) {
            log.info("备份上传成功: {} -> {}", backupId, remotePath);

            // 更新备份记录，标记已上传
            Map<String, Object> resultData = uploadResult.getData();
            resultData.put("backupId", backupId);
            resultData.put("remotePath", remotePath);
            resultData.put("uploadTime", LocalDateTime.now());

            return Result.success(resultData);
        } else {
            log.error("备份上传失败: {}", backupId);
            return Result.error("上传失败: " + uploadResult.getMessage());
        }
    }

    @Override
    public Result<String> downloadBackupFromCloud(String backupId) {
        log.info("从云存储下载备份, backupId: {}", backupId);

        BackupInfo backupInfo = backupRecords.get(backupId);
        if (backupInfo == null) {
            return Result.error("备份记录不存在");
        }

        // 构建远程路径
        String remotePath = String.format("backups/%s/%s",
                backupInfo.getBackupTime().format(DateTimeFormatter.ofPattern("yyyy/MM")),
                backupInfo.getFileName());

        // 检查文件是否存在于云存储
        Result<Boolean> existsResult = cloudStorageService.fileExists(remotePath);
        if (!existsResult.isSuccess() || !existsResult.getData()) {
            return Result.error("备份文件不存在于云存储");
        }

        // 确保本地备份目录存在
        String localPath = backupInfo.getFilePath();
        File localDir = new File(localPath).getParentFile();
        if (!localDir.exists()) {
            localDir.mkdirs();
        }

        // 下载文件
        Result<File> downloadResult = cloudStorageService.downloadFile(remotePath, localPath);

        if (downloadResult.isSuccess()) {
            log.info("备份下载成功: {} -> {}", backupId, localPath);
            return Result.success(localPath);
        } else {
            log.error("备份下载失败: {}", backupId);
            return Result.error("下载失败: " + downloadResult.getMessage());
        }
    }

    /**
     * 检查存储空间
     */
    private void checkStorageSpace() {
        try {
            String currentBackupPath = getBackupStoragePath();
            File backupDir = new File(currentBackupPath);

            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            long totalSpace = backupDir.getTotalSpace();
            long usableSpace = backupDir.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;

            double usagePercent = totalSpace > 0 ? (usedSpace * 100.0 / totalSpace) : 0;

            log.info("存储空间检查 - 总空间: {}, 已用: {}, 可用: {}, 使用率: {:.2f}%",
                formatFileSize(totalSpace), formatFileSize(usedSpace),
                formatFileSize(usableSpace), usagePercent);

            // 如果使用率超过阈值，发送警告
            if (usagePercent >= storageThresholdPercent) {
                notificationService.sendStorageWarning(usedSpace, totalSpace, usagePercent);
            }

        } catch (Exception e) {
            log.warn("检查存储空间失败", e);
        }
    }
}
