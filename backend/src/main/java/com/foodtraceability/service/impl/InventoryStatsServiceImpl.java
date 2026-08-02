package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Inventory;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryWarningMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.InventoryStatsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 库存统计服务实现类
 * 实现库存统计相关的业务方法
 */
@Service
public class InventoryStatsServiceImpl implements InventoryStatsService {


    public InventoryStatsServiceImpl(InventoryMapper inventoryMapper, InventoryWarningMapper inventoryWarningMapper,
                                     WarehouseMapper warehouseMapper) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryWarningMapper = inventoryWarningMapper;
        this.warehouseMapper = warehouseMapper;
    }

    private final InventoryMapper inventoryMapper;

    private final InventoryWarningMapper inventoryWarningMapper;

    private final WarehouseMapper warehouseMapper;

    /**
     * 获取库存统计概览
     * 返回字段与前端 InventoryStatsOverviewBackend 接口对齐：
     * totalItems/totalQuantity/totalValue(分)/warningCount/outOfStockCount/expiringCount/warehouseCount/turnoverRate
     */
    @Override
    public Map<String, Object> getStatsOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 查询所有未删除库存记录
        List<Inventory> inventories = inventoryMapper.selectList(null);

        // 库存商品种类数（不同物料数）
        long totalItems = inventories.stream()
                .map(Inventory::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        overview.put("totalItems", (int) totalItems);

        // 库存总数量
        BigDecimal totalQuantity = inventories.stream()
                .map(Inventory::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalQuantity", totalQuantity);

        // 库存总金额（分）：优先使用 totalCost，回退到 quantity * unitCost
        long totalValue = inventories.stream()
                .filter(inv -> inv.getQuantity() != null)
                .mapToLong(inv -> {
                    if (inv.getTotalCost() != null) {
                        return inv.getTotalCost();
                    }
                    if (inv.getUnitCost() != null && inv.getQuantity() != null) {
                        return inv.getUnitCost() * inv.getQuantity().longValue();
                    }
                    return 0L;
                })
                .sum();
        overview.put("totalValue", totalValue);

        // 预警商品数
        Long warningCountLong = inventoryWarningMapper.selectCount(null);
        overview.put("warningCount", warningCountLong != null ? warningCountLong.intValue() : 0);

        // 缺货商品数（库存数量 <= 0）
        long outOfStockCount = inventories.stream()
                .filter(inv -> inv.getQuantity() == null || inv.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                .count();
        overview.put("outOfStockCount", (int) outOfStockCount);

        // 临期商品数（有效期在未来7天内）
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(7);
        long expiringCount = inventories.stream()
                .filter(inv -> inv.getExpiryDate() != null)
                .filter(inv -> !inv.getExpiryDate().isBefore(today) && !inv.getExpiryDate().isAfter(deadline))
                .count();
        overview.put("expiringCount", (int) expiringCount);

        // 仓库数量
        Long warehouseCountLong = warehouseMapper.selectCount(null);
        overview.put("warehouseCount", warehouseCountLong != null ? warehouseCountLong.intValue() : 0);

        // 周转率（暂返回0.0，需出入库流水计算）
        overview.put("turnoverRate", 0.0);

        return overview;
    }

    /**
     * 获取库存趋势统计
     */
    @Override
    public List<Map<String, Object>> getStatsTrend(String startTime, String endTime, String type) {
        List<Map<String, Object>> trendData = new ArrayList<>();

        // 返回空数据，等待实际数据实现
        return trendData;
    }

    /**
     * 获取库存分类统计
     */
    @Override
    public List<Map<String, Object>> getStatsCategory(Long warehouseId) {
        List<Map<String, Object>> categoryData = new ArrayList<>();

        // 返回空数据，等待实际数据实现
        return categoryData;
    }
    
    /**
     * 获取消耗趋势统计
     */
    @Override
    public List<Map<String, Object>> getConsumptionTrend(String startTime, String endTime, String type) {
        List<Map<String, Object>> trendData = new ArrayList<>();

        // 返回空数据，等待实际数据实现
        return trendData;
    }
    
    /**
     * 获取库存同比/环比数据
     */
    @Override
    public Map<String, Object> getStatsComparison() {
        Map<String, Object> comparison = new HashMap<>();
        
        // 返回空数据，等待实际数据实现
        Map<String, Object> yearOnYear = new HashMap<>();
        yearOnYear.put("totalStock", 0.0);
        yearOnYear.put("warningCount", 0.0);
        yearOnYear.put("turnoverDays", 0.0);
        
        Map<String, Object> monthOnMonth = new HashMap<>();
        monthOnMonth.put("totalStock", 0.0);
        monthOnMonth.put("warningCount", 0.0);
        monthOnMonth.put("turnoverDays", 0.0);
        
        comparison.put("yearOnYear", yearOnYear);
        comparison.put("monthOnMonth", monthOnMonth);
        
        return comparison;
    }
    
    /**
     * 获取预警历史趋势
     */
    @Override
    public List<Map<String, Object>> getWarningHistoryTrend(String startTime, String endTime, String type) {
        List<Map<String, Object>> trendData = new ArrayList<>();
        
        // 返回空数据，等待实际数据实现
        return trendData;
    }
    
    /**
     * 获取预警级别分布
     */
    @Override
    public Map<String, Object> getWarningLevelDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        
        // 返回空数据，等待实际数据实现
        List<Map<String, Object>> levelData = new ArrayList<>();
        distribution.put("levelDistribution", levelData);
        return distribution;
    }
    
    /**
     * 获取预警状态分布
     */
    @Override
    public Map<String, Object> getWarningStatusDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        
        // 返回空数据，等待实际数据实现
        List<Map<String, Object>> statusData = new ArrayList<>();
        distribution.put("statusDistribution", statusData);
        return distribution;
    }
}