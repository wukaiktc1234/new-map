package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.AuditLog;
import com.foodtraceability.mapper.AuditLogMapper;
import com.foodtraceability.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOG");
    private static final Logger securityAuditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private static final int BUFFER_CAPACITY = 1000;
    private static final int FLUSH_INTERVAL_MS = 5000;
    private static final int MAX_EXPORT_ROWS = 50000;
    private static final int MAX_PARAM_LENGTH = 2000;


    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    private final AuditLogMapper auditLogMapper;

    private final BlockingQueue<AuditLog> bufferQueue = new LinkedBlockingQueue<>(BUFFER_CAPACITY);
    private final AtomicLong droppedCount = new AtomicLong(0);
    private volatile long lastFlushTime = System.currentTimeMillis();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Async("auditLogExecutor")
    public void saveAsync(AuditLog auditLog) {
        if (auditLog == null) return;
        
        try {
            desensitizeSensitiveData(auditLog);
            
            boolean offered = bufferQueue.offer(auditLog);
            if (!offered) {
                droppedCount.incrementAndGet();
                logger.warn("审计日志缓冲区已满，丢弃日志: operation={}", auditLog.getOperation());
                forceFlush();
                boolean retryOffered = false;
                try {
                    retryOffered = bufferQueue.offer(auditLog, 3, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                if (!retryOffered) {
                    droppedCount.incrementAndGet();
                    logger.error("审计日志重试写入也失败，永久丢弃: operation={}", auditLog.getOperation());
                    return;
                }
            }
            
            if (bufferQueue.size() >= 100 || 
                (System.currentTimeMillis() - lastFlushTime) > FLUSH_INTERVAL_MS) {
                flushBuffer();
            }
        } catch (Exception e) {
            logger.error("异步保存审计日志失败: {}", e.getMessage(), e);
            try {
                auditLogMapper.insert(auditLog);
            } catch (Exception ex) {
                logger.error("同步回退保存也失败(日志可能丢失): {}", ex.getMessage());
            }
        }
    }

    @Override
    public void saveBatchAsync(List<AuditLog> auditLogs) {
        if (auditLogs == null || auditLogs.isEmpty()) return;
        for (AuditLog log : auditLogs) {
            saveAsync(log);
        }
    }

    @Override
    public IPage<AuditLog> getAuditLogPage(
            Page<AuditLog> page,
            String userId, String username, String operationType,
            String module, String riskLevel, String ip,
            Integer sensitiveFlag, LocalDateTime startTime, LocalDateTime endTime) {
        
        return auditLogMapper.selectAuditLogPage(
            page, userId, username, operationType, module,
            riskLevel, ip, sensitiveFlag, startTime, endTime
        );
    }

    @Override
    public void exportToCsv(Map<String, Object> params, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", 
            "attachment; filename=audit_log_" + System.currentTimeMillis() + ".csv");

        var outputStream = response.getOutputStream();
        outputStream.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
        var osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        PrintWriter writer = new PrintWriter(osw);
        
        writer.println("ID,操作用户,操作类型,模块,操作描述,风险等级," +
            "IP地址,请求URL,请求方法,响应状态,执行耗时(ms),业务主键," +
            "是否敏感,创建时间");

        Page<AuditLog> page = new Page<>(1, MAX_EXPORT_ROWS);
        String userId = (String) params.getOrDefault("userId", null);
        String operationType = (String) params.getOrDefault("operationType", null);
        LocalDateTime startTime = params.containsKey("startTime") ? 
            (LocalDateTime) params.get("startTime") : null;
        LocalDateTime endTime = params.containsKey("endTime") ? 
            (LocalDateTime) params.get("endTime") : null;

        IPage<AuditLog> result = getAuditLogPage(page, userId, null, operationType,
            null, null, null, null, startTime, endTime);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (AuditLog log : result.getRecords()) {
            writer.println(String.join(",",
                escapeCsv(String.valueOf(log.getId())),
                escapeCsv(log.getUsername()),
                escapeCsv(log.getOperationType()),
                escapeCsv(log.getModule()),
                escapeCsv(log.getOperation()),
                escapeCsv(log.getRiskLevel()),
                escapeCsv(log.getIp()),
                escapeCsv(log.getRequestUrl()),
                escapeCsv(log.getRequestMethod()),
                escapeCsv(log.getResponseStatus()),
                escapeCsv(log.getExecutionTime() != null ? log.getExecutionTime().toString() : ""),
                escapeCsv(log.getBusinessKey()),
                escapeCsv(log.getSensitiveFlag() != null && log.getSensitiveFlag() == 1 ? "是" : "否"),
                escapeCsv(log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "")
            ));
            writer.flush();
        }

        writer.flush();
        logger.info("导出审计日志完成: 共{}条记录", result.getTotal());
    }

    @Override
    public long countToday() {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(AuditLog::getCreatedAt, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        return auditLogMapper.selectCount(wrapper);
    }

    @Override
    public long countByRiskLevel(String riskLevel, LocalDateTime startTime) {
        return auditLogMapper.countByRiskLevel(riskLevel, 
            startTime != null ? startTime : LocalDateTime.now().minusDays(7));
    }

    @Override
    public Map<String, Object> getStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusDays(7);
        if (endDate == null) endDate = LocalDateTime.now();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", countByPeriod(startDate, endDate));
        stats.put("critical", countByRiskLevel("CRITICAL", startDate));
        stats.put("high", countByRiskLevel("HIGH", startDate));
        stats.put("medium", countByRiskLevel("MEDIUM", startDate));
        stats.put("low", countByRiskLevel("LOW", startDate));
        stats.put("failedCount", countByStatus("FAILED", startDate));
        stats.put("todayCount", countToday());
        stats.put("droppedCount", droppedCount.get());
        return stats;
    }

    @Override
    public int archiveLogs(int retentionDays) {
        LocalDateTime archiveDate = LocalDateTime.now().minusDays(retentionDays);
        // 审计日志仅允许 INSERT，禁止 UPDATE/DELETE（会计法/网络安全法要求）。
        // 归档操作仅将历史日志拷贝到 audit_log_archive 表，不修改也不删除原表数据。
        int archived = auditLogMapper.archiveLogsBeforeDate(archiveDate);
        logger.info("归档审计日志: 保留{}天, 归档{}条(原表数据保留不变)", retentionDays, archived);
        return archived;
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    @PreDestroy
    public void destroy() {
        logger.info("审计日志服务关闭, 强制刷新缓冲区...");
        try {
            flushBuffer();
            int remaining = bufferQueue.size();
            if (remaining > 0) {
                logger.warn("仍有{}条日志未写入数据库，尝试逐条写入", remaining);
                List<AuditLog> lastBatch = new ArrayList<>();
                bufferQueue.drainTo(lastBatch);
                int successCount = 0;
                for (AuditLog log : lastBatch) {
                    try { 
                        auditLogMapper.insert(log); 
                        successCount++;
                    } catch (Exception e) {
                        logger.warn("关闭时写入审计日志失败(id={}): {}", log.getId(), e.getMessage());
                    }
                }
                logger.info("关闭时强制刷新完成: 成功{}/{}", successCount, lastBatch.size());
            }
            logger.info("审计日志缓冲区已清空");
        } catch (Exception e) {
            logger.error("关闭时强制刷新失败: {}", e.getMessage());
        }
    }

    private void flushBuffer() {
        List<AuditLog> batch = new ArrayList<>();
        bufferQueue.drainTo(batch);
        if (batch.isEmpty()) return;

        int successCount = 0;
        try {
            for (AuditLog log : batch) {
                writeLogToAppropriateChannel(log);
                auditLogMapper.insert(log);
                successCount++;
            }
            lastFlushTime = System.currentTimeMillis();
            logger.debug("批量写入审计日志成功: {}/{}", successCount, batch.size());
        } catch (Exception e) {
            logger.error("批量写入审计日志失败: 已写入{}/{}条, error={}", successCount, batch.size(), e.getMessage(), e);
            for (int i = successCount; i < batch.size(); i++) {
                try { auditLogMapper.insert(batch.get(i)); } catch (Exception ex) {
                    logger.warn("单条回退写入失败: {}", ex.getMessage());
                }
            }
        }
    }

    private void writeLogToAppropriateChannel(AuditLog log) {
        try {
            String jsonLog = objectMapper.writeValueAsString(log);
            
            if ("SECURITY".equals(log.getOperationType()) || "CRITICAL".equals(log.getRiskLevel())) {
                securityAuditLogger.warn("[AUDIT] {}", jsonLog);
            } else if ("HIGH".equals(log.getRiskLevel())) {
                auditLogger.info("[AUDIT-HIGH] {}", jsonLog);
            } else {
                auditLogger.debug("[AUDIT] {}", jsonLog);
            }
        } catch (Exception e) {
            logger.debug("写审计日志到文件失败: {}", e.getMessage());
        }
    }

    private void desensitizeSensitiveData(AuditLog log) {
        if (log.getSensitiveFlag() != null && log.getSensitiveFlag() == 1) {
            if (log.getRequestParams() != null && !log.getRequestParams().isEmpty()) {
                log.setRequestParams(desensitizeJson(log.getRequestParams()));
            }
            if (log.getResponseBody() != null && !log.getResponseBody().isEmpty()) {
                log.setResponseBody("[SENSITIVE_DATA]");
            }
        }
    }

    private String desensitizeJson(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            desensitizeMap(map);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "[DESENSITIZED]";
        }
    }

    @SuppressWarnings("unchecked")
    private void desensitizeMap(Map<String, Object> map) {
        String[] sensitiveKeys = {"password", "oldPassword", "newPassword", "idCard", 
                                   "phone", "email", "bankCard", "token"};
        Set<String> keySet = new HashSet<>(Arrays.asList(sensitiveKeys));

        for (Map.Entry<String, Object> entry : new ArrayList<>(map.entrySet())) {
            String key = entry.getKey().toLowerCase();
            if (keySet.contains(key)) {
                map.put(entry.getKey(), "***");
            }
        }
    }

    private long countByPeriod(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(AuditLog::getCreatedAt, start)
               .le(AuditLog::getCreatedAt, end);
        return auditLogMapper.selectCount(wrapper);
    }

    private long countByStatus(String status, LocalDateTime start) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResponseStatus, status)
               .ge(AuditLog::getCreatedAt, start);
        return auditLogMapper.selectCount(wrapper);
    }

    private void forceFlush() {
        flushBuffer();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.length() > MAX_PARAM_LENGTH) {
            value = value.substring(0, MAX_PARAM_LENGTH) + "...[TRUNCATED]";
        }
        boolean needsQuote = value.contains(",") || value.contains("\"") || 
                             value.contains("\n") || value.contains("\r") ||
                             value.startsWith("=") || value.startsWith("+") || 
                             value.startsWith("-") || value.startsWith("@") || 
                             value.startsWith("\t") || value.startsWith("\r");
        if (needsQuote) {
            if (value.startsWith("=") || value.startsWith("+") || 
                value.startsWith("-") || value.startsWith("@") || 
                value.startsWith("\t")) {
                value = "'" + value;
            }
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
