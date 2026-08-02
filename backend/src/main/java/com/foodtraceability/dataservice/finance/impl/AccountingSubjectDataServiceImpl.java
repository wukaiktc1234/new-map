package com.foodtraceability.dataservice.finance.impl;

import com.foodtraceability.dataservice.finance.AccountingSubjectDataService;
import com.foodtraceability.dto.finance.AccountingSubjectBasicInfo;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
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
 * 会计科目 DataService 实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class AccountingSubjectDataServiceImpl implements AccountingSubjectDataService {

    private static final Logger log = LoggerFactory.getLogger(AccountingSubjectDataServiceImpl.class);

    private final AccountingSubjectMapper mapper;

    public AccountingSubjectDataServiceImpl(AccountingSubjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, AccountingSubjectBasicInfo> batchGetAccountingSubjectBasicInfo(List<String> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, AccountingSubjectBasicInfo> result = new LinkedHashMap<>();
        List<Long> dbLongIds = new ArrayList<>();

        // 解析合法ID，收集需查库的ID
        for (String id : subjectIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            try {
                dbLongIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("会计科目ID格式非法，跳过: subjectId={}", id);
            }
        }

        if (dbLongIds.isEmpty()) {
            return result;
        }

        // DB 批量查询（避免 N+1）
        List<AccountingSubject> entities = mapper.selectBatchIds(dbLongIds);
        Map<Long, AccountingSubject> entityMap = entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        AccountingSubject::getSubjectId,
                        e -> e,
                        (v1, v2) -> v1));

        for (String id : subjectIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            Long longId;
            try {
                longId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                continue;
            }
            AccountingSubject entity = entityMap.get(longId);
            if (entity != null) {
                result.put(id, convertToBasicInfo(entity));
            }
        }

        return result;
    }

    @Override
    public AccountingSubjectBasicInfo getAccountingSubjectBasicInfo(String subjectId) {
        if (subjectId == null || subjectId.isEmpty()) {
            return null;
        }

        Long longId;
        try {
            longId = Long.parseLong(subjectId);
        } catch (NumberFormatException e) {
            log.warn("会计科目ID格式非法: subjectId={}", subjectId);
            return null;
        }

        AccountingSubject entity = mapper.selectById(longId);
        if (entity == null) {
            return null;
        }
        return convertToBasicInfo(entity);
    }

    @Override
    public void clearAccountingSubjectCache(String subjectId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearAccountingSubjectBatchCache(List<String> subjectIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearAllAccountingSubjectCache() {
        // 缓存已移除，空实现保留接口契约
    }

    /**
     * 实体转 BasicInfo
     */
    private AccountingSubjectBasicInfo convertToBasicInfo(AccountingSubject entity) {
        if (entity == null) {
            return null;
        }
        AccountingSubjectBasicInfo info = new AccountingSubjectBasicInfo();
        info.setSubjectId(entity.getSubjectId());
        info.setSubjectCode(entity.getSubjectCode());
        info.setSubjectName(entity.getSubjectName());
        info.setSubjectType(entity.getSubjectType());
        info.setBalanceDirection(entity.getDirection());
        info.setParentId(entity.getParentId());
        return info;
    }
}
