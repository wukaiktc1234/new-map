package com.foodtraceability.dataservice.finance.impl;

import com.foodtraceability.dataservice.finance.ReceivableDataService;
import com.foodtraceability.dto.finance.ReceivableBasicInfo;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 应收账款 DataService 实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class ReceivableDataServiceImpl implements ReceivableDataService {

    private static final Logger log = LoggerFactory.getLogger(ReceivableDataServiceImpl.class);

    private final ReceivableMapper mapper;

    public ReceivableDataServiceImpl(ReceivableMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, ReceivableBasicInfo> batchGetReceivableBasicInfo(List<String> receivableIds) {
        if (receivableIds == null || receivableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ReceivableBasicInfo> result = new LinkedHashMap<>();
        List<Long> dbLongIds = new ArrayList<>();

        // 解析合法ID，收集需查库的ID
        for (String id : receivableIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            try {
                dbLongIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("应收ID格式非法，跳过: receivableId={}", id);
            }
        }

        if (dbLongIds.isEmpty()) {
            return result;
        }

        // DB 批量查询（避免 N+1）
        List<Receivable> entities = mapper.selectBatchIds(dbLongIds);
        Map<Long, Receivable> entityMap = entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Receivable::getReceivableId,
                        e -> e,
                        (v1, v2) -> v1));

        for (String id : receivableIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            Long longId;
            try {
                longId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                continue;
            }
            Receivable entity = entityMap.get(longId);
            if (entity != null) {
                result.put(id, convertToBasicInfo(entity));
            }
        }

        return result;
    }

    @Override
    public ReceivableBasicInfo getReceivableBasicInfo(String receivableId) {
        if (receivableId == null || receivableId.isEmpty()) {
            return null;
        }

        Long longId;
        try {
            longId = Long.parseLong(receivableId);
        } catch (NumberFormatException e) {
            log.warn("应收ID格式非法: receivableId={}", receivableId);
            return null;
        }

        Receivable entity = mapper.selectById(longId);
        if (entity == null) {
            return null;
        }
        return convertToBasicInfo(entity);
    }

    @Override
    public void clearReceivableCache(String receivableId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearReceivableBatchCache(List<String> receivableIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearAllReceivableCache() {
        // 缓存已移除，空实现保留接口契约
    }

    /**
     * 实体转 BasicInfo
     * amount 取原始应收金额（originalAmount）
     */
    private ReceivableBasicInfo convertToBasicInfo(Receivable entity) {
        if (entity == null) {
            return null;
        }
        ReceivableBasicInfo info = new ReceivableBasicInfo();
        info.setReceivableId(entity.getReceivableId());
        info.setCustomerId(entity.getCustomerId());
        info.setAmount(entity.getOriginalAmount());
        info.setReceivedAmount(entity.getReceivedAmount());
        info.setStatus(entity.getStatus());
        info.setDueDate(entity.getDueDate());
        return info;
    }
}
