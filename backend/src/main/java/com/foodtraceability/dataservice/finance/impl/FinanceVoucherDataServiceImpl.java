package com.foodtraceability.dataservice.finance.impl;

import com.foodtraceability.dataservice.finance.FinanceVoucherDataService;
import com.foodtraceability.dto.finance.FinanceVoucherBasicInfo;
import com.foodtraceability.entity.finance.FinanceVoucher;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
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
 * 记账凭证 DataService 实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class FinanceVoucherDataServiceImpl implements FinanceVoucherDataService {

    private static final Logger log = LoggerFactory.getLogger(FinanceVoucherDataServiceImpl.class);

    private final FinanceVoucherMapper mapper;

    public FinanceVoucherDataServiceImpl(FinanceVoucherMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, FinanceVoucherBasicInfo> batchGetFinanceVoucherBasicInfo(List<String> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, FinanceVoucherBasicInfo> result = new LinkedHashMap<>();
        List<Long> dbLongIds = new ArrayList<>();

        // 解析合法ID，收集需查库的ID
        for (String id : voucherIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            try {
                dbLongIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("凭证ID格式非法，跳过: voucherId={}", id);
            }
        }

        if (dbLongIds.isEmpty()) {
            return result;
        }

        // DB 批量查询（避免 N+1）
        List<FinanceVoucher> entities = mapper.selectBatchIds(dbLongIds);
        Map<Long, FinanceVoucher> entityMap = entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        FinanceVoucher::getVoucherId,
                        e -> e,
                        (v1, v2) -> v1));

        for (String id : voucherIds) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            Long longId;
            try {
                longId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                continue;
            }
            FinanceVoucher entity = entityMap.get(longId);
            if (entity != null) {
                result.put(id, convertToBasicInfo(entity));
            }
        }

        return result;
    }

    @Override
    public FinanceVoucherBasicInfo getFinanceVoucherBasicInfo(String voucherId) {
        if (voucherId == null || voucherId.isEmpty()) {
            return null;
        }

        Long longId;
        try {
            longId = Long.parseLong(voucherId);
        } catch (NumberFormatException e) {
            log.warn("凭证ID格式非法: voucherId={}", voucherId);
            return null;
        }

        FinanceVoucher entity = mapper.selectById(longId);
        if (entity == null) {
            return null;
        }
        return convertToBasicInfo(entity);
    }

    @Override
    public void clearFinanceVoucherCache(String voucherId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearFinanceVoucherBatchCache(List<String> voucherIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearAllFinanceVoucherCache() {
        // 缓存已移除，空实现保留接口契约
    }

    /**
     * 实体转 BasicInfo
     * totalAmount 取借方合计（平衡凭证借贷相等）；period 由凭证日期派生为 yyyy-MM
     */
    private FinanceVoucherBasicInfo convertToBasicInfo(FinanceVoucher entity) {
        if (entity == null) {
            return null;
        }
        FinanceVoucherBasicInfo info = new FinanceVoucherBasicInfo();
        info.setVoucherId(entity.getVoucherId());
        info.setVoucherNo(entity.getVoucherNo());
        info.setVoucherDate(entity.getVoucherDate());
        info.setTotalAmount(entity.getTotalDebit());
        info.setStatus(entity.getVoucherStatus());
        info.setPeriod(derivePeriod(entity.getVoucherDate()));
        return info;
    }

    /**
     * 由凭证日期派生会计期间（yyyy-MM）
     */
    private String derivePeriod(java.time.LocalDate voucherDate) {
        if (voucherDate == null) {
            return null;
        }
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();
        return String.format("%04d-%02d", year, month);
    }
}
