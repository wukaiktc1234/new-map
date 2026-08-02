package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.store.operation.DiningTableQueryDTO;
import com.foodtraceability.entity.DiningTableNew;
import com.foodtraceability.mapper.DiningTableNewMapper;
import com.foodtraceability.service.DiningTableNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 桌台服务实现类
 *
 * 重构说明：
 * - 已移除 RabbitMQ 依赖（RabbitTemplate）
 * - 桌台配置变更消息改为日志记录
 * - 保留所有业务逻辑和数据库操作
 */
@Service
public class DiningTableNewServiceImpl implements DiningTableNewService {

    private static final Logger log = LoggerFactory.getLogger(DiningTableNewServiceImpl.class);

    private final DiningTableNewMapper diningTableNewMapper;

    public DiningTableNewServiceImpl(DiningTableNewMapper diningTableNewMapper) {
        this.diningTableNewMapper = diningTableNewMapper;
    }

    /**
     * 发布桌台配置变更事件
     * 重构说明：已移除 RabbitMQ，改为日志记录
     * @param storeId 门店ID
     * @param tableId 桌台ID
     * @param action 操作类型（create/update/updateStatus）
     */
    private void publishTableConfigChanged(Long storeId, Long tableId, String action) {
        log.info("桌台配置变更事件：storeId={}, tableId={}, action={}, timestamp={}",
                storeId, tableId, action, System.currentTimeMillis());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningTableNew create(DiningTableNew table) {
        log.info("创建桌台: {}", table.getTableName());
        if (diningTableNewMapper.selectByTableCode(table.getTableCode(), table.getStoreId()) != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "桌台编码已存在: " + table.getTableCode());
        }
        diningTableNewMapper.insert(table);
        publishTableConfigChanged(table.getStoreId(), table.getTableId(), "create");
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningTableNew update(DiningTableNew table) {
        log.info("更新桌台, ID: {}", table.getTableId());
        diningTableNewMapper.updateById(table);
        DiningTableNew updated = diningTableNewMapper.selectById(table.getTableId());
        publishTableConfigChanged(updated.getStoreId(), updated.getTableId(), "update");
        return updated;
    }

    @Override
    public DiningTableNew getById(Long tableId) {
        DiningTableNew table = diningTableNewMapper.selectById(tableId);
        if (table == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "桌台不存在，ID: " + tableId);
        }
        return table;
    }

    @Override
    public DiningTableNew getByCode(String tableCode) {
        DiningTableNew table = diningTableNewMapper.selectByTableCode(tableCode, null);
        if (table == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "桌台不存在，编码: " + tableCode);
        }
        return table;
    }

    @Override
    public List<DiningTableNew> listAll() {
        return diningTableNewMapper.selectList(null);
    }

    @Override
    public List<DiningTableNew> listAvailable() {
        return diningTableNewMapper.selectAvailableTables();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long tableId, Integer status, String currentOrderId) {
        log.info("更新桌台状态, ID: {}, 状态: {}", tableId, status);
        diningTableNewMapper.updateStatus(tableId, status, currentOrderId);
        DiningTableNew table = diningTableNewMapper.selectById(tableId);
        if (table != null) {
            publishTableConfigChanged(table.getStoreId(), table.getTableId(), "updateStatus");
        }
    }

    @Override
    public Map<Integer, Long> countByStatus() {
        Map<Integer, Long> result = new HashMap<>();
        List<Map<String, Object>> counts = diningTableNewMapper.countByStatus();
        for (Map<String, Object> item : counts) {
            result.put(((Number) item.get("status")).intValue(), ((Number) item.get("cnt")).longValue());
        }
        return result;
    }

    @Override
    public IPage<DiningTableNew> listByStoreId(Long storeId, DiningTableQueryDTO query) {
        log.info("按门店查询桌台列表, storeId: {}", storeId);
        Page<DiningTableNew> page = new Page<>(query.getPage(), query.getSize());
        return diningTableNewMapper.selectByStoreId(page, storeId, query.getStatus(), query.getTableType(), query.getKeyword());
    }

    @Override
    public Map<String, Object> getStatsByStoreId(Long storeId) {
        log.info("按门店统计桌台, storeId: {}", storeId);
        Map<String, Object> stats = new HashMap<>();

        List<Map<String, Object>> counts = diningTableNewMapper.countGroupByStatus(storeId);
        long total = 0L;
        long idle = 0L, dining = 0L, reserved = 0L, maintenance = 0L, disabled = 0L;
        for (Map<String, Object> item : counts) {
            int s = ((Number) item.get("status")).intValue();
            long c = ((Number) item.get("cnt")).longValue();
            total += c;
            switch (s) {
                case 1: idle = c; break;
                case 2: dining = c; break;
                case 3: reserved = c; break;
                case 4: maintenance = c; break;
                case 5: disabled = c; break;
                default: break;
            }
        }

        stats.put("total", total);
        stats.put("idle", idle);
        stats.put("dining", dining);
        stats.put("reserved", reserved);
        stats.put("maintenance", maintenance);
        stats.put("disabled", disabled);
        return stats;
    }

    @Override
    public boolean isTableCodeUnique(Long storeId, String tableCode, Long excludeTableId) {
        log.info("检查桌台编码唯一性, storeId: {}, tableCode: {}, excludeTableId: {}", storeId, tableCode, excludeTableId);
        LambdaQueryWrapper<DiningTableNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiningTableNew::getStoreId, storeId);
        wrapper.eq(DiningTableNew::getTableCode, tableCode);
        if (excludeTableId != null) {
            wrapper.ne(DiningTableNew::getTableId, excludeTableId);
        }
        return diningTableNewMapper.selectCount(wrapper) == 0;
    }
}
