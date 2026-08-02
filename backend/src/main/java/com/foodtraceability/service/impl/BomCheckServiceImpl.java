package com.foodtraceability.service.impl;

import com.foodtraceability.dto.product.AlternativeDishVO;
import com.foodtraceability.dto.product.BatchCheckRequestDTO;
import com.foodtraceability.dto.product.BatchCheckResultVO;
import com.foodtraceability.dto.product.BomCheckResultVO;
import com.foodtraceability.dto.product.BomCheckStatsVO;
import com.foodtraceability.dto.product.BomStockWarningConfigVO;
import com.foodtraceability.entity.DishInventory;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.mapper.DishInventoryMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.service.BomCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BOM 库存联动检查服务实现
 *
 * <p>实现策略：
 * <ul>
 *   <li>预警配置：内存维护（单机），后续可迁移至数据库</li>
 *   <li>检查逻辑：通过 DishInventoryMapper 查询菜品 BOM 配方，
 *       逐项比对 InventoryMapper 中的可用库存（当前数量 - 锁定数量）</li>
 *   <li>替代推荐：返回空列表，待接入推荐算法（V2 功能）</li>
 *   <li>统计数据：返回空统计，待接入日志聚合（V2 功能）</li>
 * </ul>
 */
@Service
public class BomCheckServiceImpl implements BomCheckService {

    private static final Logger log = LoggerFactory.getLogger(BomCheckServiceImpl.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 预警配置缓存（key 固定为 "default"） */
    private final Map<String, BomStockWarningConfigVO> warningConfigCache = new ConcurrentHashMap<>();

    private final FoodNewMapper foodNewMapper;
    private final DishInventoryMapper dishInventoryMapper;
    private final InventoryMapper inventoryMapper;

    public BomCheckServiceImpl(FoodNewMapper foodNewMapper,
                               DishInventoryMapper dishInventoryMapper,
                               InventoryMapper inventoryMapper) {
        this.foodNewMapper = foodNewMapper;
        this.dishInventoryMapper = dishInventoryMapper;
        this.inventoryMapper = inventoryMapper;
        // 初始化默认预警配置
        BomStockWarningConfigVO defaultConfig = new BomStockWarningConfigVO();
        defaultConfig.setId("default");
        defaultConfig.setEnabled(true);
        defaultConfig.setCheckPoint("both");
        defaultConfig.setAutoSuggestAlternative(true);
        defaultConfig.setLowStockThreshold(20.0);
        defaultConfig.setWarnBeforeOrder(true);
        warningConfigCache.put("default", defaultConfig);
    }

    @Override
    public BomCheckResultVO checkDish(String dishId, Integer quantity) {
        log.debug("检查菜品 BOM 库存：dishId={}, quantity={}", dishId, quantity);
        int qty = quantity == null || quantity < 1 ? 1 : quantity;

        BomCheckResultVO result = new BomCheckResultVO();
        result.setDishId(dishId);
        result.setTotalRequired(qty);
        result.setCheckedAt(LocalDateTime.now().format(ISO_FORMATTER));
        result.setInsufficientItems(new ArrayList<>());
        result.setAlternativeDishes(new ArrayList<>());

        // 查询菜品信息
        FoodNew food = null;
        try {
            Long foodId = parseLong(dishId);
            if (foodId != null) {
                food = foodNewMapper.selectById(foodId);
            }
        } catch (Exception e) {
            log.warn("查询菜品信息失败：dishId={}, error={}", dishId, e.getMessage());
        }

        // 菜品不存在时返回可制作（避免阻塞订单），但记录警告
        if (food == null) {
            result.setDishName("未知菜品");
            result.setCanMake(true);
            log.warn("菜品不存在，跳过 BOM 检查：dishId={}", dishId);
            return result;
        }

        result.setDishName(food.getFoodName());

        // 查询菜品 BOM 配方（dish_inventory 表）
        List<DishInventory> bomItems;
        try {
            bomItems = dishInventoryMapper.selectByDishId(dishId);
        } catch (Exception e) {
            log.warn("查询菜品 BOM 配方失败：dishId={}, error={}", dishId, e.getMessage());
            // BOM 查询失败时不阻塞业务，默认可制作
            result.setCanMake(true);
            return result;
        }

        // 无 BOM 配方记录，视为可制作（菜品未配置原料）
        if (bomItems == null || bomItems.isEmpty()) {
            result.setCanMake(true);
            return result;
        }

        // 逐项检查库存是否足够
        List<BomCheckResultVO.InsufficientItem> insufficientItems = result.getInsufficientItems();
        for (DishInventory bomItem : bomItems) {
            Long inventoryId = bomItem.getInventoryId();
            if (inventoryId == null) {
                continue;
            }

            Inventory inventory = inventoryMapper.selectById(inventoryId);
            if (inventory == null) {
                // 库存记录缺失，记为缺料
                insufficientItems.add(buildInsufficientItem(
                        bomItem, BigDecimal.ZERO, qty, "high"));
                continue;
            }

            // 可用库存 = 当前数量 - 锁定数量
            BigDecimal currentQty = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();
            BigDecimal lockedQty = inventory.getLockedQuantity() == null ? BigDecimal.ZERO : inventory.getLockedQuantity();
            BigDecimal availableQty = currentQty.subtract(lockedQty);

            // 单份需求量 * 制作份数 = 总需求量
            BigDecimal requiredQty = bomItem.getQuantity() == null
                    ? BigDecimal.ZERO
                    : bomItem.getQuantity().multiply(BigDecimal.valueOf(qty));

            if (availableQty.compareTo(requiredQty) < 0) {
                BigDecimal shortageQty = requiredQty.subtract(availableQty);
                double shortagePercent = requiredQty.compareTo(BigDecimal.ZERO) > 0
                        ? shortageQty.doubleValue() / requiredQty.doubleValue() * 100.0
                        : 100.0;
                String urgency = classifyUrgency(shortagePercent);
                insufficientItems.add(buildInsufficientItem(
                        bomItem, availableQty, qty, urgency, shortageQty, shortagePercent));
            }
        }

        result.setCanMake(insufficientItems.isEmpty());
        return result;
    }

    /**
     * 构建 BOM 缺料项（无缺口明细的简化版，用于库存记录缺失场景）
     */
    private BomCheckResultVO.InsufficientItem buildInsufficientItem(DishInventory bomItem,
                                                                    BigDecimal availableQty,
                                                                    int qty,
                                                                    String urgency) {
        return buildInsufficientItem(bomItem, availableQty, qty, urgency,
                bomItem.getQuantity() == null ? BigDecimal.ZERO : bomItem.getQuantity().multiply(BigDecimal.valueOf(qty)),
                100.0);
    }

    /**
     * 构建 BOM 缺料项（含缺口明细）
     */
    private BomCheckResultVO.InsufficientItem buildInsufficientItem(DishInventory bomItem,
                                                                    BigDecimal availableQty,
                                                                    int qty,
                                                                    String urgency,
                                                                    BigDecimal shortageQty,
                                                                    double shortagePercent) {
        BomCheckResultVO.InsufficientItem item = new BomCheckResultVO.InsufficientItem();
        item.setMaterialId(bomItem.getInventoryId() == null ? null : String.valueOf(bomItem.getInventoryId()));
        // 优先使用 BOM 关联的物料名称，回退到 inventoryName 字段
        item.setMaterialName(bomItem.getInventoryName());
        item.setRequiredQty(bomItem.getQuantity() == null
                ? null
                : bomItem.getQuantity().multiply(BigDecimal.valueOf(qty)).doubleValue());
        item.setAvailableQty(availableQty.doubleValue());
        item.setShortageQty(shortageQty.doubleValue());
        item.setShortagePercent(shortagePercent);
        item.setUnit(bomItem.getUnit());
        item.setUrgency(urgency);
        return item;
    }

    /**
     * 根据缺口百分比划分紧急程度
     * - 缺口 >= 70%：high
     * - 缺口 >= 30%：medium
     * - 其余：low
     */
    private String classifyUrgency(double shortagePercent) {
        if (shortagePercent >= 70.0) {
            return "high";
        }
        if (shortagePercent >= 30.0) {
            return "medium";
        }
        return "low";
    }

    @Override
    public BatchCheckResultVO batchCheck(BatchCheckRequestDTO request) {
        BatchCheckResultVO result = new BatchCheckResultVO();
        result.setResults(new ArrayList<>());

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            result.setTotalChecked(0);
            result.setCanMakeCount(0);
            result.setInsufficientCount(0);
            result.setHealthScore(100.0);
            return result;
        }

        int canMake = 0;
        int insufficient = 0;
        for (BatchCheckRequestDTO.CheckItem item : request.getItems()) {
            BomCheckResultVO itemResult = checkDish(item.getDishId(), item.getQuantity());
            result.getResults().add(itemResult);
            if (Boolean.TRUE.equals(itemResult.getCanMake())) {
                canMake++;
            } else {
                insufficient++;
            }
        }

        result.setTotalChecked(request.getItems().size());
        result.setCanMakeCount(canMake);
        result.setInsufficientCount(insufficient);
        // 健康度 = 可制作菜品数 / 总数 * 100
        result.setHealthScore(request.getItems().isEmpty() ? 100.0
                : Math.round((double) canMake / request.getItems().size() * 10000) / 100.0);

        return result;
    }

    @Override
    public Map<String, Object> preOrderCheck(List<String> dishIds) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> ready = new ArrayList<>();
        List<Map<String, Object>> issues = new ArrayList<>();

        if (dishIds != null) {
            for (String dishId : dishIds) {
                BomCheckResultVO checkResult = checkDish(dishId, 1);
                if (Boolean.TRUE.equals(checkResult.getCanMake())) {
                    ready.add(dishId);
                } else {
                    Map<String, Object> issue = new LinkedHashMap<>();
                    issue.put("dishId", dishId);
                    issue.put("dishName", checkResult.getDishName());
                    // 基于真实缺料结果填充问题明细
                    issue.put("issue", "库存不足");
                    // 根据最高缺料紧急程度决定严重级别
                    issue.put("severity", resolveSeverity(checkResult));
                    // 透传缺料明细，便于前端展示
                    issue.put("insufficientItems", checkResult.getInsufficientItems());
                    issues.add(issue);
                }
            }
        }

        result.put("ready", ready);
        result.put("issues", issues);
        return result;
    }

    /**
     * 根据缺料项的最高紧急程度决定下单前预检查的严重级别
     * - 存在 high 紧急缺料：阻塞（error）
     * - 存在 medium 紧急缺料：警告（warn）
     * - 其余：警告（warn）
     */
    private String resolveSeverity(BomCheckResultVO checkResult) {
        if (checkResult == null || checkResult.getInsufficientItems() == null
                || checkResult.getInsufficientItems().isEmpty()) {
            return "warn";
        }
        for (BomCheckResultVO.InsufficientItem item : checkResult.getInsufficientItems()) {
            if ("high".equals(item.getUrgency())) {
                return "error";
            }
        }
        return "warn";
    }

    @Override
    public BomStockWarningConfigVO getWarningConfig() {
        return warningConfigCache.getOrDefault("default", defaultConfig());
    }

    @Override
    public BomStockWarningConfigVO saveWarningConfig(BomStockWarningConfigVO config) {
        if (config == null) {
            config = defaultConfig();
        }
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId("default");
        }
        warningConfigCache.put(config.getId(), config);
        log.info("保存 BOM 预警配置：enabled={}, checkPoint={}", config.getEnabled(), config.getCheckPoint());
        return config;
    }

    @Override
    public List<AlternativeDishVO> getAlternatives(String dishId, Integer limit) {
        // 当前未接入推荐算法，返回空列表
        log.debug("查询替代菜品推荐：dishId={}, limit={}", dishId, limit);
        return Collections.emptyList();
    }

    @Override
    public BomCheckStatsVO getStats(String startDate, String endDate) {
        // 当前未接入日志聚合，返回空统计
        log.debug("查询 BOM 检查统计：startDate={}, endDate={}", startDate, endDate);
        BomCheckStatsVO stats = new BomCheckStatsVO();
        stats.setTotalChecks(0);
        stats.setBlockedOrders(0);
        stats.setAvgHealthScore(100.0);
        stats.setTopShortageMaterials(new ArrayList<>());
        return stats;
    }

    // ==================== 私有辅助方法 ====================

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BomStockWarningConfigVO defaultConfig() {
        BomStockWarningConfigVO config = new BomStockWarningConfigVO();
        config.setId("default");
        config.setEnabled(true);
        config.setCheckPoint("both");
        config.setAutoSuggestAlternative(true);
        config.setLowStockThreshold(20.0);
        config.setWarnBeforeOrder(true);
        return config;
    }
}
