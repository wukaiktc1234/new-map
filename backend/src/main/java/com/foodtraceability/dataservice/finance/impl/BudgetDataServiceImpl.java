package com.foodtraceability.dataservice.finance.impl;

import com.foodtraceability.dataservice.finance.BudgetDataService;
import com.foodtraceability.dto.finance.BudgetBasicInfo;
import com.foodtraceability.entity.finance.Budget;
import com.foodtraceability.mapper.finance.BudgetMapper;
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
 * 预算 DataService 实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class BudgetDataServiceImpl implements BudgetDataService {

    private static final Logger log = LoggerFactory.getLogger(BudgetDataServiceImpl.class);

    private final BudgetMapper mapper;

    public BudgetDataServiceImpl(BudgetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, BudgetBasicInfo> batchGetBudgetBasicInfo(List<String> budgetIds) {
        if (budgetIds == null || budgetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BudgetBasicInfo> result = new LinkedHashMap<>();
        List<Long> dbLongIds = new ArrayList<>();

        // 解析合法ID，收集需查库的ID
        for (String id : budgetIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            try {
                dbLongIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("预算ID格式非法，跳过: budgetId={}", id);
            }
        }

        if (dbLongIds.isEmpty()) {
            return result;
        }

        // DB 批量查询（避免 N+1）
        List<Budget> entities = mapper.selectBatchIds(dbLongIds);
        Map<Long, Budget> entityMap = entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Budget::getBudgetId,
                        e -> e,
                        (v1, v2) -> v1));

        for (String id : budgetIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            Long longId;
            try {
                longId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                continue;
            }
            Budget entity = entityMap.get(longId);
            if (entity != null) {
                result.put(id, convertToBasicInfo(entity));
            }
        }

        return result;
    }

    @Override
    public BudgetBasicInfo getBudgetBasicInfo(String budgetId) {
        if (budgetId == null || budgetId.isEmpty()) {
            return null;
        }

        Long longId;
        try {
            longId = Long.parseLong(budgetId);
        } catch (NumberFormatException e) {
            log.warn("预算ID格式非法: budgetId={}", budgetId);
            return null;
        }

        Budget entity = mapper.selectById(longId);
        if (entity == null) {
            return null;
        }
        return convertToBasicInfo(entity);
    }

    @Override
    public void clearBudgetCache(String budgetId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearBudgetBatchCache(List<String> budgetIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearAllBudgetCache() {
        // 缓存已移除，空实现保留接口契约
    }

    /**
     * 实体转 BasicInfo
     * period 由预算年份/月份派生；实体暂无 budgetName/status 字段，留 null
     */
    private BudgetBasicInfo convertToBasicInfo(Budget entity) {
        if (entity == null) {
            return null;
        }
        BudgetBasicInfo info = new BudgetBasicInfo();
        info.setBudgetId(entity.getBudgetId());
        info.setBudgetName(null);
        info.setPeriod(derivePeriod(entity.getBudgetYear(), entity.getBudgetMonth()));
        info.setTotalAmount(entity.getBudgetAmount());
        info.setUsedAmount(entity.getActualAmount());
        info.setDepartmentId(entity.getResponsibleDeptId());
        info.setStatus(null);
        return info;
    }

    /**
     * 派生预算期间：月份为空返回 yyyy，否则返回 yyyy-MM
     */
    private String derivePeriod(Integer year, Integer month) {
        if (year == null) {
            return null;
        }
        if (month == null) {
            return String.format("%04d", year);
        }
        return String.format("%04d-%02d", year, month);
    }
}
