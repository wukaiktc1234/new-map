package com.foodtraceability.service.impl.seal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.seal.Seal;
import com.foodtraceability.entity.seal.SealUsageLog;
import com.foodtraceability.mapper.seal.SealMapper;
import com.foodtraceability.mapper.seal.SealUsageLogMapper;
import com.foodtraceability.service.seal.SealService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 电子签章服务实现类
 * 实现印章的增删改查、状态管理、授权查询及使用记录管理
 *
 * <p>JSON 字段处理：
 * authorizedUsers / authorizedScenes 在数据库中以 JSON 数组字符串存储，
 * 对外交互时通过 ObjectMapper 在 List 与 JSON 字符串之间转换。
 *
 * <p>作废语义说明：
 * "删除"接口实际为作废操作——将状态改为 revoked，记录保持可见，便于审计追溯。
 */
@Service
public class SealServiceImpl extends ServiceImpl<SealMapper, Seal> implements SealService {

    private static final Logger log = LoggerFactory.getLogger(SealServiceImpl.class);

    /** 日期时间格式化器(与全局 Jackson 配置保持一致) */
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 默认页码 */
    private static final int DEFAULT_PAGE = 1;
    /** 默认每页条数 */
    private static final int DEFAULT_SIZE = 10;

    private final SealUsageLogMapper sealUsageLogMapper;
    private final ObjectMapper objectMapper;

    public SealServiceImpl(SealUsageLogMapper sealUsageLogMapper, ObjectMapper objectMapper) {
        this.sealUsageLogMapper = sealUsageLogMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== 印章管理 ====================

    @Override
    public PageResult<Map<String, Object>> getSealList(Integer page, Integer size,
                                                       String sealType, String status, String keyword) {
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);

        LambdaQueryWrapper<Seal> wrapper = new LambdaQueryWrapper<>();
        if (isNotBlank(sealType)) {
            wrapper.eq(Seal::getSealType, sealType);
        }
        if (isNotBlank(status)) {
            wrapper.eq(Seal::getStatus, status);
        }
        if (isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Seal::getSealName, kw)
                    .or().like(Seal::getKeeper, kw));
        }
        wrapper.orderByDesc(Seal::getCreateTime);

        Page<Seal> result = baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(this::sealToMap)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), records, result.getCurrent(), result.getSize());
    }

    @Override
    public Map<String, Object> getSealById(String id) {
        Seal seal = baseMapper.selectById(id);
        if (seal == null) {
            return null;
        }
        return sealToMap(seal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSeal(Map<String, Object> data) {
        Seal seal = mapToSeal(data);
        seal.setStatus(Seal.STATUS_ACTIVE);
        seal.setDeleted(0);
        baseMapper.insert(seal);
        log.info("创建印章成功: sealId={}, sealName={}", seal.getSealId(), seal.getSealName());
        return sealToMap(seal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSeal(String id, Map<String, Object> data) {
        Seal seal = baseMapper.selectById(id);
        if (seal == null) {
            throw new RuntimeException("印章不存在：" + id);
        }
        applyUpdate(seal, data);
        baseMapper.updateById(seal);
        log.info("更新印章成功: sealId={}", id);
        return sealToMap(seal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeSeal(String id) {
        Seal seal = baseMapper.selectById(id);
        if (seal == null) {
            throw new RuntimeException("印章不存在：" + id);
        }
        seal.setStatus(Seal.STATUS_REVOKED);
        int rows = baseMapper.updateById(seal);
        log.info("作废印章: sealId={}, 影响行数={}", id, rows);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSealStatus(String id, String status) {
        Seal seal = baseMapper.selectById(id);
        if (seal == null) {
            throw new RuntimeException("印章不存在：" + id);
        }
        seal.setStatus(status);
        baseMapper.updateById(seal);
        log.info("切换印章状态: sealId={}, status={}", id, status);
        return sealToMap(seal);
    }

    @Override
    public List<Map<String, Object>> getAuthorizedSeals(String scene, String userId) {
        LambdaQueryWrapper<Seal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seal::getStatus, Seal.STATUS_ACTIVE);
        List<Seal> seals = baseMapper.selectList(wrapper);

        return seals.stream()
                .filter(s -> contains(jsonToList(s.getAuthorizedScenes()), scene)
                        && contains(jsonToList(s.getAuthorizedUsers()), userId))
                .map(this::sealToMap)
                .collect(Collectors.toList());
    }

    // ==================== 印章使用记录 ====================

    @Override
    public PageResult<SealUsageLog> getUsageLogs(Integer page, Integer size,
                                                 String sealId, String businessType) {
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);

        LambdaQueryWrapper<SealUsageLog> wrapper = new LambdaQueryWrapper<>();
        if (isNotBlank(sealId)) {
            wrapper.eq(SealUsageLog::getSealId, sealId);
        }
        if (isNotBlank(businessType)) {
            wrapper.eq(SealUsageLog::getBusinessType, businessType);
        }
        wrapper.orderByDesc(SealUsageLog::getOperateTime);

        Page<SealUsageLog> result = sealUsageLogMapper.selectPage(
                new Page<>(currentPage, pageSize), wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SealUsageLog recordUsage(Map<String, Object> data, String ipAddress) {
        String sealId = asString(data.get("sealId"));
        Seal seal = baseMapper.selectById(sealId);
        if (seal == null) {
            throw new RuntimeException("印章不存在：" + sealId);
        }

        SealUsageLog usageLog = new SealUsageLog();
        usageLog.setSealId(sealId);
        usageLog.setSealName(seal.getSealName());
        usageLog.setBusinessType(asString(data.get("businessType")));
        usageLog.setBusinessId(asString(data.get("businessId")));
        usageLog.setBusinessNo(asString(data.get("businessNo")));
        usageLog.setOperator(asString(data.get("operator")));
        usageLog.setOperateTime(LocalDateTime.now());
        usageLog.setIpAddress(ipAddress);
        usageLog.setRemark(asString(data.get("remark")));
        usageLog.setDeleted(0);

        sealUsageLogMapper.insert(usageLog);
        log.info("记录印章使用: sealId={}, businessType={}, businessNo={}",
                sealId, usageLog.getBusinessType(), usageLog.getBusinessNo());
        return usageLog;
    }

    // ==================== 私有辅助方法：实体与Map转换 ====================

    /**
     * 印章实体转Map(授权字段还原为数组,时间格式化为字符串)
     */
    private Map<String, Object> sealToMap(Seal seal) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sealId", seal.getSealId());
        map.put("sealName", seal.getSealName());
        map.put("sealType", seal.getSealType());
        map.put("sealImage", seal.getSealImage());
        map.put("status", seal.getStatus());
        map.put("keeper", seal.getKeeper());
        map.put("authorizedUsers", jsonToList(seal.getAuthorizedUsers()));
        map.put("authorizedScenes", jsonToList(seal.getAuthorizedScenes()));
        map.put("createBy", seal.getCreateBy());
        map.put("remark", seal.getRemark());
        map.put("createTime", formatDateTime(seal.getCreateTime()));
        map.put("updateTime", formatDateTime(seal.getUpdateTime()));
        return map;
    }

    /**
     * 将请求Map转换为印章实体(数组字段序列化为JSON字符串)
     */
    private Seal mapToSeal(Map<String, Object> data) {
        Seal seal = new Seal();
        seal.setSealName(asString(data.get("sealName")));
        seal.setSealType(asString(data.get("sealType")));
        seal.setSealImage(asString(data.get("sealImage")));
        seal.setKeeper(asString(data.get("keeper")));
        seal.setAuthorizedUsers(listToJson(asStringList(data.get("authorizedUsers"))));
        seal.setAuthorizedScenes(listToJson(asStringList(data.get("authorizedScenes"))));
        seal.setRemark(asString(data.get("remark")));
        seal.setCreateBy(asString(data.get("createBy")));
        return seal;
    }

    /**
     * 将请求Map中的字段应用到已有印章实体(仅更新传入的字段)
     */
    private void applyUpdate(Seal seal, Map<String, Object> data) {
        if (data.containsKey("sealName")) {
            seal.setSealName(asString(data.get("sealName")));
        }
        if (data.containsKey("sealType")) {
            seal.setSealType(asString(data.get("sealType")));
        }
        if (data.containsKey("sealImage")) {
            seal.setSealImage(asString(data.get("sealImage")));
        }
        if (data.containsKey("keeper")) {
            seal.setKeeper(asString(data.get("keeper")));
        }
        if (data.containsKey("authorizedUsers")) {
            seal.setAuthorizedUsers(listToJson(asStringList(data.get("authorizedUsers"))));
        }
        if (data.containsKey("authorizedScenes")) {
            seal.setAuthorizedScenes(listToJson(asStringList(data.get("authorizedScenes"))));
        }
        if (data.containsKey("remark")) {
            seal.setRemark(asString(data.get("remark")));
        }
    }

    // ==================== 私有辅助方法：JSON与类型转换 ====================

    /**
     * List序列化为JSON数组字符串
     */
    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("List转JSON失败,使用空数组: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * JSON数组字符串反序列化为List
     */
    private List<String> jsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON转List失败,返回空列表: json={}, error={}", json, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 安全转换为String
     */
    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 安全转换为String列表(处理JSON数组反序列化后的List)
     */
    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(item == null ? null : item.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * 格式化日期时间为字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
    }

    // ==================== 私有辅助方法：通用工具 ====================

    /**
     * 判断字符串是否非空
     */
    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 判断列表是否包含指定值(忽略null)
     */
    private boolean contains(List<String> list, String value) {
        return list != null && value != null && list.contains(value);
    }

    /**
     * 规范化页码(空或小于1时返回默认值)
     */
    private long normalizePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    /**
     * 规范化每页条数(空或小于1时返回默认值)
     */
    private long normalizeSize(Integer size) {
        return (size == null || size < 1) ? DEFAULT_SIZE : size;
    }
}
