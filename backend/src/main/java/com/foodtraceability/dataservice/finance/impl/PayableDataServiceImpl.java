package com.foodtraceability.dataservice.finance.impl;

import com.foodtraceability.dataservice.finance.PayableDataService;
import com.foodtraceability.dto.finance.PayableBasicInfo;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.mapper.finance.PayableMapper;
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
 * 应付账款 DataService 实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class PayableDataServiceImpl implements PayableDataService {

    private static final Logger log = LoggerFactory.getLogger(PayableDataServiceImpl.class);

    private final PayableMapper mapper;

    public PayableDataServiceImpl(PayableMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, PayableBasicInfo> batchGetPayableBasicInfo(List<String> payableIds) {
        if (payableIds == null || payableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, PayableBasicInfo> result = new LinkedHashMap<>();
        List<Long> dbLongIds = new ArrayList<>();

        // 解析合法ID，收集需查库的ID
        for (String id : payableIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            try {
                dbLongIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("应付ID格式非法，跳过: payableId={}", id);
            }
        }

        if (dbLongIds.isEmpty()) {
            return result;
        }

        // DB 批量查询（避免 N+1）
        List<Payable> entities = mapper.selectBatchIds(dbLongIds);
        Map<Long, Payable> entityMap = entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Payable::getPayableId,
                        e -> e,
                        (v1, v2) -> v1));

        for (String id : payableIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            Long longId;
            try {
                longId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                continue;
            }
            Payable entity = entityMap.get(longId);
            if (entity != null) {
                result.put(id, convertToBasicInfo(entity));
            }
        }

        return result;
    }

    @Override
    public PayableBasicInfo getPayableBasicInfo(String payableId) {
        if (payableId == null || payableId.isEmpty()) {
            return null;
        }

        Long longId;
        try {
            longId = Long.parseLong(payableId);
        } catch (NumberFormatException e) {
            log.warn("应付ID格式非法: payableId={}", payableId);
            return null;
        }

        Payable entity = mapper.selectById(longId);
        if (entity == null) {
            return null;
        }
        return convertToBasicInfo(entity);
    }

    @Override
    public void clearPayableCache(String payableId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearPayableBatchCache(List<String> payableIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearAllPayableCache() {
        // 缓存已移除，空实现保留接口契约
    }

    /**
     * 实体转 BasicInfo
     * amount 取原始应付金额（originalAmount）
     */
    private PayableBasicInfo convertToBasicInfo(Payable entity) {
        if (entity == null) {
            return null;
        }
        PayableBasicInfo info = new PayableBasicInfo();
        info.setPayableId(entity.getPayableId());
        info.setSupplierId(entity.getSupplierId());
        info.setAmount(entity.getOriginalAmount());
        info.setPaidAmount(entity.getPaidAmount());
        info.setStatus(entity.getStatus());
        info.setDueDate(entity.getDueDate());
        return info;
    }
}
