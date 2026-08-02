package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.aimodel.AIModelConfigCreateDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigQueryDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigUpdateDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigVO;
import com.foodtraceability.entity.AIModelConfig;
import com.foodtraceability.mapper.AIModelConfigMapper;
import com.foodtraceability.service.AIModelConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * AI 模型配置服务实现
 * 管理智能补货建议的 AI 模型接入，支持 local（本地规则引擎）和 api（外部 AI 服务）两种类型
 */
@Service
public class AIModelConfigServiceImpl extends ServiceImpl<AIModelConfigMapper, AIModelConfig> implements AIModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(AIModelConfigServiceImpl.class);

    /** modelCode 前缀 */
    private static final String MODEL_CODE_PREFIX = "model-";
    /** modelCode 随机数字位数 */
    private static final int MODEL_CODE_RANDOM_DIGITS = 6;
    /** modelCode 生成最大重试次数 */
    private static final int MODEL_CODE_MAX_RETRY = 10;

    private final AIModelConfigMapper aiModelConfigMapper;

    public AIModelConfigServiceImpl(AIModelConfigMapper aiModelConfigMapper) {
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    // ==================== 查询接口 ====================

    @Override
    public PageResult<AIModelConfigVO> getList(AIModelConfigQueryDTO query) {
        int current = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1;
        int size = query.getSize() != null && query.getSize() > 0 ? query.getSize() : 10;

        Page<AIModelConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<AIModelConfig> wrapper = new LambdaQueryWrapper<>();

        if (query.getModelType() != null && !query.getModelType().isEmpty()) {
            wrapper.eq(AIModelConfig::getModelType, query.getModelType());
        }
        if (query.getProvider() != null && !query.getProvider().isEmpty()) {
            wrapper.eq(AIModelConfig::getProvider, query.getProvider());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(AIModelConfig::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(AIModelConfig::getModelName, kw)
                    .or().like(AIModelConfig::getEndpoint, kw));
        }
        wrapper.orderByDesc(AIModelConfig::getCreatedAt);

        Page<AIModelConfig> result = aiModelConfigMapper.selectPage(page, wrapper);

        PageResult<AIModelConfigVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent((long) current);
        pageResult.setSize((long) size);
        return pageResult;
    }

    @Override
    public AIModelConfigVO getById(Integer id) {
        if (id == null) {
            return null;
        }
        AIModelConfig entity = aiModelConfigMapper.selectById(id);
        return entity != null ? toVO(entity) : null;
    }

    @Override
    public AIModelConfigVO getByCode(String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<AIModelConfig> wrapper = new LambdaQueryWrapper<AIModelConfig>()
                .eq(AIModelConfig::getModelCode, modelCode);
        AIModelConfig entity = aiModelConfigMapper.selectOne(wrapper);
        return entity != null ? toVO(entity) : null;
    }

    // ==================== 写操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AIModelConfigVO create(AIModelConfigCreateDTO dto) {
        AIModelConfig entity = new AIModelConfig();
        entity.setModelCode(generateUniqueModelCode());
        entity.setModelName(dto.getModelName());
        entity.setModelType(dto.getModelType());
        entity.setEndpoint(dto.getEndpoint());
        entity.setApiKey(dto.getApiKey());
        // 默认状态为启用
        entity.setStatus("active");
        // 置信度默认 medium
        entity.setConfidence(dto.getConfidence() != null ? dto.getConfidence() : "medium");
        entity.setDescription(dto.getDescription());
        // 创建时 lastSyncTime 为 null
        entity.setLastSyncTime(null);
        entity.setModelPath(dto.getModelPath());
        entity.setModelParams(dto.getModelParams());
        // api 类型默认超时 30 秒
        entity.setTimeout(dto.getTimeout() != null ? dto.getTimeout() : 30);
        entity.setProvider(dto.getProvider());
        entity.setModelVersion(dto.getModelVersion());
        entity.setDefaultModel(dto.getDefaultModel());
        entity.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : 0.7);
        entity.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 2048);
        entity.setContextLength(dto.getContextLength() != null ? dto.getContextLength() : 4096);
        entity.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 3);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        aiModelConfigMapper.insert(entity);
        log.info("创建 AI 模型配置: modelCode={}, modelName={}, modelType={}",
                entity.getModelCode(), entity.getModelName(), entity.getModelType());
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AIModelConfigVO update(Integer id, AIModelConfigUpdateDTO dto) {
        AIModelConfig existing = aiModelConfigMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("AI 模型配置不存在: id=" + id);
        }

        if (dto.getModelName() != null) existing.setModelName(dto.getModelName());
        if (dto.getModelType() != null) existing.setModelType(dto.getModelType());
        if (dto.getEndpoint() != null) existing.setEndpoint(dto.getEndpoint());
        if (dto.getApiKey() != null) existing.setApiKey(dto.getApiKey());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getConfidence() != null) existing.setConfidence(dto.getConfidence());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        // 仅在显式传入 lastSyncTime 时更新
        if (dto.getLastSyncTime() != null) existing.setLastSyncTime(dto.getLastSyncTime());
        if (dto.getModelPath() != null) existing.setModelPath(dto.getModelPath());
        if (dto.getModelParams() != null) existing.setModelParams(dto.getModelParams());
        if (dto.getTimeout() != null) existing.setTimeout(dto.getTimeout());
        if (dto.getProvider() != null) existing.setProvider(dto.getProvider());
        if (dto.getModelVersion() != null) existing.setModelVersion(dto.getModelVersion());
        if (dto.getDefaultModel() != null) existing.setDefaultModel(dto.getDefaultModel());
        if (dto.getTemperature() != null) existing.setTemperature(dto.getTemperature());
        if (dto.getMaxTokens() != null) existing.setMaxTokens(dto.getMaxTokens());
        if (dto.getContextLength() != null) existing.setContextLength(dto.getContextLength());
        if (dto.getMaxRetries() != null) existing.setMaxRetries(dto.getMaxRetries());
        existing.setUpdatedAt(LocalDateTime.now());

        aiModelConfigMapper.updateById(existing);
        log.info("更新 AI 模型配置: id={}, modelCode={}", id, existing.getModelCode());
        return toVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Integer id) {
        if (id == null) {
            return false;
        }
        AIModelConfig existing = aiModelConfigMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        // 逻辑删除（@TableLogic 由 MyBatis-Plus 自动处理）
        int rows = aiModelConfigMapper.deleteById(id);
        log.info("删除 AI 模型配置: id={}, modelCode={}", id, existing.getModelCode());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Integer id, String status) {
        if (id == null || status == null || status.isEmpty()) {
            return false;
        }
        AIModelConfig existing = aiModelConfigMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        int rows = aiModelConfigMapper.updateById(existing);
        log.info("更新 AI 模型状态: id={}, modelCode={}, status={}", id, existing.getModelCode(), status);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncModel(Integer id) {
        if (id == null) {
            return false;
        }
        AIModelConfig existing = aiModelConfigMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        existing.setLastSyncTime(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        int rows = aiModelConfigMapper.updateById(existing);
        log.info("触发 AI 模型同步: id={}, modelCode={}", id, existing.getModelCode());
        return rows > 0;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<AIModelConfig> all = aiModelConfigMapper.selectList(null);
        long total = all.size();
        long active = all.stream().filter(e -> "active".equals(e.getStatus())).count();
        long local = all.stream().filter(e -> "local".equals(e.getModelType())).count();
        long api = all.stream().filter(e -> "api".equals(e.getModelType())).count();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("inactive", total - active);
        stats.put("local", local);
        stats.put("api", api);
        return stats;
    }

    @Override
    public Map<String, Object> testConnection(Integer id) {
        Map<String, Object> result = new HashMap<>();
        if (id == null) {
            result.put("success", false);
            result.put("message", "模型ID不能为空");
            return result;
        }
        AIModelConfig entity = aiModelConfigMapper.selectById(id);
        if (entity == null) {
            result.put("success", false);
            result.put("message", "AI 模型配置不存在: id=" + id);
            return result;
        }

        String modelType = entity.getModelType();
        result.put("modelType", modelType);
        result.put("modelName", entity.getModelName());

        if ("local".equals(modelType)) {
            return testLocalConnection(entity);
        } else if ("api".equals(modelType)) {
            return testApiConnection(entity);
        } else {
            result.put("success", false);
            result.put("message", "未知的模型类型: " + modelType);
            return result;
        }
    }

    /**
     * 测试本地模型连接
     * 规则：
     * 1. modelPath 为空或为"内置规则引擎"时，视为可用（无需外部依赖）
     * 2. 其他情况检查文件/目录是否存在
     */
    private Map<String, Object> testLocalConnection(AIModelConfig entity) {
        Map<String, Object> result = new HashMap<>();
        result.put("modelType", "local");
        String modelPath = entity.getModelPath();
        long start = System.currentTimeMillis();

        if (modelPath == null || modelPath.isEmpty() || "内置规则引擎".equals(modelPath)) {
            result.put("success", true);
            result.put("message", "内置规则引擎可用，无需外部连接");
            result.put("responseTimeMs", System.currentTimeMillis() - start);
            return result;
        }

        try {
            Path path = Paths.get(modelPath);
            if (Files.exists(path)) {
                result.put("success", true);
                result.put("message", "本地模型路径存在: " + modelPath);
            } else {
                result.put("success", false);
                result.put("message", "本地模型路径不存在: " + modelPath);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查本地模型路径失败: " + e.getMessage());
        }
        result.put("responseTimeMs", System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 测试 API 模型连接
     * 发送 HTTP GET 请求到 endpoint，根据响应状态码判断可用性。
     * 若配置了 apiKey，则添加 Authorization: Bearer {apiKey} 头。
     * 使用模型配置的 timeout（默认 30 秒）。
     */
    private Map<String, Object> testApiConnection(AIModelConfig entity) {
        Map<String, Object> result = new HashMap<>();
        result.put("modelType", "api");
        String endpoint = entity.getEndpoint();
        long start = System.currentTimeMillis();

        if (endpoint == null || endpoint.isEmpty()) {
            result.put("success", false);
            result.put("message", "API 模型的 endpoint 未配置");
            return result;
        }

        int timeoutSec = entity.getTimeout() != null ? entity.getTimeout() : 30;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .GET();

            // 若配置了 apiKey，则添加 Bearer 认证头
            String apiKey = entity.getApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSec))
                    .build();

            HttpResponse<Void> response = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.discarding());

            int status = response.statusCode();
            result.put("httpStatus", status);
            result.put("responseTimeMs", System.currentTimeMillis() - start);

            if (status >= 200 && status < 300) {
                result.put("success", true);
                result.put("message", "连接成功，HTTP " + status);
            } else if (status == 401 || status == 403) {
                result.put("success", false);
                result.put("message", "认证失败，请检查 API Key（HTTP " + status + "）");
            } else if (status == 404) {
                result.put("success", false);
                result.put("message", "接口地址不存在（HTTP 404）");
            } else {
                result.put("success", false);
                result.put("message", "服务端返回异常状态码: HTTP " + status);
            }
        } catch (java.net.ConnectException e) {
            result.put("success", false);
            result.put("message", "无法连接到目标服务: " + e.getMessage());
            result.put("responseTimeMs", System.currentTimeMillis() - start);
        } catch (java.net.http.HttpTimeoutException e) {
            result.put("success", false);
            result.put("message", "连接超时（" + timeoutSec + "秒内未响应）");
            result.put("responseTimeMs", System.currentTimeMillis() - start);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
            result.put("responseTimeMs", System.currentTimeMillis() - start);
        }
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成唯一的 modelCode（格式：model-{6位随机数字}）
     * 通过查询数据库确保唯一性，最多重试 10 次
     */
    private String generateUniqueModelCode() {
        for (int i = 0; i < MODEL_CODE_MAX_RETRY; i++) {
            String code = MODEL_CODE_PREFIX + generateRandomDigits(MODEL_CODE_RANDOM_DIGITS);
            LambdaQueryWrapper<AIModelConfig> wrapper = new LambdaQueryWrapper<AIModelConfig>()
                    .eq(AIModelConfig::getModelCode, code);
            Long count = aiModelConfigMapper.selectCount(wrapper);
            if (count == null || count == 0) {
                return code;
            }
        }
        // 重试耗尽后使用时间戳兜底，保证唯一性
        return MODEL_CODE_PREFIX + System.currentTimeMillis();
    }

    /**
     * 生成指定位数的随机数字字符串
     */
    private String generateRandomDigits(int digits) {
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 实体转视图对象（apiKey 脱敏：api 类型且非空时保留前3位+****）
     */
    private AIModelConfigVO toVO(AIModelConfig entity) {
        AIModelConfigVO vo = new AIModelConfigVO();
        vo.setId(entity.getId());
        vo.setModelCode(entity.getModelCode());
        vo.setModelName(entity.getModelName());
        vo.setModelType(entity.getModelType());
        vo.setEndpoint(entity.getEndpoint());
        vo.setApiKey(maskApiKey(entity));
        vo.setStatus(entity.getStatus());
        vo.setConfidence(entity.getConfidence());
        vo.setDescription(entity.getDescription());
        vo.setLastSyncTime(entity.getLastSyncTime());
        vo.setModelPath(entity.getModelPath());
        vo.setModelParams(entity.getModelParams());
        vo.setTimeout(entity.getTimeout());
        vo.setProvider(entity.getProvider());
        vo.setModelVersion(entity.getModelVersion());
        vo.setDefaultModel(entity.getDefaultModel());
        vo.setTemperature(entity.getTemperature());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setContextLength(entity.getContextLength());
        vo.setMaxRetries(entity.getMaxRetries());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * API 密钥脱敏：api 类型且 apiKey 非空时保留前3位+****，其余情况返回 null
     */
    private String maskApiKey(AIModelConfig entity) {
        if (!"api".equals(entity.getModelType())) {
            return null;
        }
        String key = entity.getApiKey();
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.length() <= 3) {
            return "****";
        }
        return key.substring(0, 3) + "****";
    }
}
