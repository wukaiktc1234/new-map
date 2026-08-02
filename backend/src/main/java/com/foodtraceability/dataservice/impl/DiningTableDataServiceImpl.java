package com.foodtraceability.dataservice.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dataservice.DiningTableDataService;
import com.foodtraceability.dto.store.operation.vo.DiningTableVO;
import com.foodtraceability.entity.DiningTableNew;
import com.foodtraceability.mapper.DiningTableNewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 桌台数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 缓存键格式: diningTable:basic:{tableId}
 */
@Service
@CacheConfig(cacheNames = "diningTable")
public class DiningTableDataServiceImpl implements DiningTableDataService {

    private static final Logger log = LoggerFactory.getLogger(DiningTableDataServiceImpl.class);

    /** 日期时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiningTableNewMapper diningTableNewMapper;

    /**
     * 构造函数注入
     *
     * @param diningTableNewMapper 桌台Mapper
     */
    public DiningTableDataServiceImpl(DiningTableNewMapper diningTableNewMapper) {
        this.diningTableNewMapper = diningTableNewMapper;
    }

    @Override
    @Cacheable(key = "'basic:' + #tableId", unless = "#result == null")
    public DiningTableVO getDiningTableBasicInfo(Long tableId) {
        if (tableId == null) {
            return null;
        }

        DiningTableNew table = diningTableNewMapper.selectById(tableId);
        return convertToVO(table);
    }

    @Override
    @Cacheable(key = "#tableIds", unless = "#result == null || #result.isEmpty()")
    public Map<Long, DiningTableVO> batchGetDiningTableBasicInfo(List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DiningTableNew> tables = diningTableNewMapper.selectBatchIds(tableIds);
        return tables.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        DiningTableNew::getTableId,
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(key = "'store:' + #storeId", unless = "#result == null || #result.isEmpty()")
    public List<DiningTableVO> getDiningTablesByStoreId(Long storeId) {
        if (storeId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DiningTableNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiningTableNew::getStoreId, storeId)
                .orderByAsc(DiningTableNew::getSortOrder, DiningTableNew::getTableId);
        List<DiningTableNew> tables = diningTableNewMapper.selectList(wrapper);

        return tables.stream()
                .filter(Objects::nonNull)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(key = "'basic:' + #tableId")
    public void clearDiningTableCache(Long tableId) {
        log.debug("清除桌台缓存: tableId={}", tableId);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void clearDiningTableBatchCache(List<Long> tableIds) {
        log.debug("批量清除桌台缓存: count={}", tableIds != null ? tableIds.size() : 0);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void clearStoreDiningTableCache(Long storeId) {
        log.debug("清除门店桌台缓存: storeId={}", storeId);
    }

    /**
     * 将实体转换为视图对象
     *
     * @param table 桌台实体
     * @return 视图对象
     */
    private DiningTableVO convertToVO(DiningTableNew table) {
        if (table == null) {
            return null;
        }

        DiningTableVO vo = new DiningTableVO();
        vo.setTableId(table.getTableId());
        vo.setStoreId(table.getStoreId());
        vo.setTableCode(table.getTableCode());
        vo.setTableName(table.getTableName());
        vo.setAreaId(table.getAreaId());
        vo.setTableType(table.getTableType());
        vo.setTableTypeName(getTableTypeName(table.getTableType()));
        vo.setSeatsCount(table.getSeatsCount());
        vo.setMinPeople(table.getMinPeople());
        vo.setMaxPeople(table.getMaxPeople());
        vo.setStatus(table.getStatus());
        vo.setStatusName(getStatusName(table.getStatus()));
        vo.setCurrentOrderId(table.getCurrentOrderId());
        vo.setQrCode(table.getQrCode());
        vo.setSortOrder(table.getSortOrder());
        vo.setCreateTime(formatDateTime(table.getCreateTime()));
        vo.setUpdateTime(formatDateTime(table.getUpdateTime()));

        return vo;
    }

    /** 获取状态名称 */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "空闲";
            case 2: return "用餐中";
            case 3: return "预订";
            case 4: return "维护中";
            case 5: return "停用";
            default: return "未知";
        }
    }

    /** 获取桌台类型名称 */
    private String getTableTypeName(Integer tableType) {
        if (tableType == null) return "未知";
        switch (tableType) {
            case 1: return "大厅";
            case 2: return "包厢";
            case 3: return "吧台";
            case 4: return "户外";
            default: return "未知";
        }
    }

    /** 格式化LocalDateTime为字符串 */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }
}
