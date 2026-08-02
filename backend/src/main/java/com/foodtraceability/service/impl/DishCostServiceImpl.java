package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.product.CostAlertRuleVO;
import com.foodtraceability.dto.product.CostSummaryVO;
import com.foodtraceability.dto.product.CostTrendDataVO;
import com.foodtraceability.dto.product.DishCostQueryDTO;
import com.foodtraceability.dto.product.DishCostSnapshotVO;
import com.foodtraceability.entity.FoodCategoryNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.FoodCategoryNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.service.DishCostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 菜品成本服务实现
 *
 * <p>当前实现策略：
 * <ul>
 *   <li>成本快照：基于 FoodNew 的 costPrice 和 salePrice 计算毛利</li>
 *   <li>BOM 明细：当前未接入 Recipe 模块，返回空列表</li>
 *   <li>成本趋势：返回空列表，待接入成本历史表</li>
 *   <li>预警规则：内存维护（单机），后续可迁移至数据库</li>
 *   <li>重算任务：返回模拟任务信息，待接入异步任务框架</li>
 *   <li>报表导出：返回空字节数组，待接入报表引擎</li>
 * </ul>
 */
@Service
public class DishCostServiceImpl implements DishCostService {

    private static final Logger log = LoggerFactory.getLogger(DishCostServiceImpl.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 预警规则缓存（key = ruleId） */
    private final Map<String, CostAlertRuleVO> alertRuleCache = new ConcurrentHashMap<>();
    private final AtomicLong ruleIdSeq = new AtomicLong(1);

    private final FoodNewMapper foodNewMapper;
    private final FoodCategoryNewMapper categoryNewMapper;

    public DishCostServiceImpl(FoodNewMapper foodNewMapper, FoodCategoryNewMapper categoryNewMapper) {
        this.foodNewMapper = foodNewMapper;
        this.categoryNewMapper = categoryNewMapper;
        initDefaultAlertRules();
    }

    private void initDefaultAlertRules() {
        CostAlertRuleVO rule1 = new CostAlertRuleVO();
        rule1.setId("rule-001");
        rule1.setName("毛利率低于30%预警");
        rule1.setRuleType("margin_below");
        rule1.setThreshold(30.0);
        rule1.setEnabled(true);
        rule1.setCreatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        alertRuleCache.put(rule1.getId(), rule1);

        CostAlertRuleVO rule2 = new CostAlertRuleVO();
        rule2.setId("rule-002");
        rule2.setName("成本上涨超过10%预警");
        rule2.setRuleType("cost_increase");
        rule2.setThreshold(10.0);
        rule2.setEnabled(true);
        rule2.setCreatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        alertRuleCache.put(rule2.getId(), rule2);
    }

    @Override
    public DishCostSnapshotVO getDishCost(String dishId) {
        log.debug("获取菜品成本快照：dishId={}", dishId);
        DishCostSnapshotVO snapshot = new DishCostSnapshotVO();
        snapshot.setDishId(dishId);
        snapshot.setBomItems(new ArrayList<>());
        snapshot.setLastUpdated(LocalDateTime.now().format(ISO_FORMATTER));

        FoodNew food = loadFood(dishId);
        if (food != null) {
            fillSnapshotFromFood(snapshot, food);
        } else {
            snapshot.setDishName("未知菜品");
            snapshot.setStatus("normal");
        }
        return snapshot;
    }

    @Override
    public PageResult<DishCostSnapshotVO> getDishCostList(DishCostQueryDTO queryDto) {
        if (queryDto == null) {
            queryDto = new DishCostQueryDTO();
        }
        int page = queryDto.getPage() == null || queryDto.getPage() < 1 ? 1 : queryDto.getPage();
        int size = queryDto.getSize() == null || queryDto.getSize() < 1 ? 20 : queryDto.getSize();

        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(FoodNew::getSalePrice);
        if (queryDto.getCategoryId() != null && !queryDto.getCategoryId().isEmpty()) {
            Long catId = parseLong(queryDto.getCategoryId());
            if (catId != null) {
                wrapper.eq(FoodNew::getCategoryId, catId);
            }
        }
        if (queryDto.getStatus() != null && !queryDto.getStatus().isEmpty()) {
            // status 映射到菜品状态：normal=1在售、warning=3售罄、danger=2停售
            Integer statusCode = mapStatusToCode(queryDto.getStatus());
            if (statusCode != null) {
                wrapper.eq(FoodNew::getStatus, statusCode);
            }
        } else {
            wrapper.eq(FoodNew::getStatus, 1); // 默认只查在售
        }
        if (queryDto.getKeyword() != null && !queryDto.getKeyword().isEmpty()) {
            wrapper.like(FoodNew::getFoodName, queryDto.getKeyword());
        }

        List<FoodNew> foods = foodNewMapper.selectList(wrapper);

        // 转换为 VO
        List<DishCostSnapshotVO> allData = foods.stream().map(food -> {
            DishCostSnapshotVO vo = new DishCostSnapshotVO();
            vo.setDishId(String.valueOf(food.getFoodId()));
            fillSnapshotFromFood(vo, food);
            return vo;
        }).collect(Collectors.toList());

        // 排序
        if (queryDto.getSortBy() != null) {
            sortBy(allData, queryDto.getSortBy());
        }

        // 手动分页
        int total = allData.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<DishCostSnapshotVO> pageData = start < total ? allData.subList(start, end) : new ArrayList<>();

        PageResult<DishCostSnapshotVO> result = new PageResult<>();
        result.setTotal((long) total);
        result.setRecords(pageData);
        result.setCurrent((long) page);
        result.setSize((long) size);
        return result;
    }

    @Override
    public Map<String, Object> recalculateAll() {
        // 当前未接入异步任务框架，返回模拟任务信息
        long totalDishes = foodNewMapper.selectCount(
                new LambdaQueryWrapper<FoodNew>().eq(FoodNew::getStatus, 1));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", UUID.randomUUID().toString());
        result.put("totalDishes", totalDishes);
        result.put("estimatedSeconds", Math.max(1, totalDishes / 10));
        log.info("触发菜品成本重算任务：totalDishes={}", totalDishes);
        return result;
    }

    @Override
    public List<CostTrendDataVO> getCostTrend(String dishId, Integer days) {
        // 当前未接入成本历史表，返回空列表
        int targetDays = days == null || days < 1 ? 30 : days;
        log.debug("查询菜品成本趋势：dishId={}, days={}", dishId, targetDays);
        return new ArrayList<>();
    }

    @Override
    public DishCostSnapshotVO getBomDetail(String dishId) {
        DishCostSnapshotVO snapshot = getDishCost(dishId);
        // 当前未接入 Recipe 模块，BOM 明细为空列表
        if (snapshot.getBomItems() == null) {
            snapshot.setBomItems(new ArrayList<>());
        }
        return snapshot;
    }

    @Override
    public void updateBomQuantity(String dishId, String materialId, Double quantity) {
        // 当前未接入 Recipe 模块，仅记录日志
        log.info("更新 BOM 项数量：dishId={}, materialId={}, quantity={}", dishId, materialId, quantity);
    }

    @Override
    public List<CostAlertRuleVO> getAlertRules() {
        return new ArrayList<>(alertRuleCache.values());
    }

    @Override
    public CostAlertRuleVO saveAlertRule(CostAlertRuleVO rule) {
        if (rule == null) {
            throw new IllegalArgumentException("预警规则不能为空");
        }
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId("rule-" + String.format("%03d", ruleIdSeq.getAndIncrement()));
            rule.setCreatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        }
        alertRuleCache.put(rule.getId(), rule);
        log.info("保存成本预警规则：id={}, name={}", rule.getId(), rule.getName());
        return rule;
    }

    @Override
    public void toggleAlertRule(String id, Boolean enabled) {
        CostAlertRuleVO rule = alertRuleCache.get(id);
        if (rule != null) {
            rule.setEnabled(enabled);
            log.info("切换预警规则启用状态：id={}, enabled={}", id, enabled);
        } else {
            log.warn("预警规则不存在：id={}", id);
        }
    }

    @Override
    public CostSummaryVO getCostSummary(String startDate, String endDate) {
        log.debug("查询成本汇总：startDate={}, endDate={}", startDate, endDate);
        CostSummaryVO summary = new CostSummaryVO();
        summary.setTopCostIncrease(new ArrayList<>());
        summary.setTopNegativeProfit(new ArrayList<>());

        List<FoodNew> foods = foodNewMapper.selectList(
                new LambdaQueryWrapper<FoodNew>()
                        .eq(FoodNew::getStatus, 1)
                        .isNotNull(FoodNew::getSalePrice)
                        .isNotNull(FoodNew::getCostPrice));

        int normalCount = 0;
        int warningCount = 0;
        int dangerCount = 0;
        double totalMarginRate = 0.0;
        int validCount = 0;

        for (FoodNew food : foods) {
            double marginRate = calculateMarginRate(food.getSalePrice(), food.getCostPrice());
            String status = classifyMarginRate(marginRate);
            if ("normal".equals(status)) normalCount++;
            else if ("warning".equals(status)) warningCount++;
            else if ("danger".equals(status)) dangerCount++;

            if (food.getSalePrice() != null && food.getSalePrice() > 0) {
                totalMarginRate += marginRate;
                validCount++;
            }
        }

        summary.setTotalDishes(foods.size());
        summary.setNormalCount(normalCount);
        summary.setWarningCount(warningCount);
        summary.setDangerCount(dangerCount);
        summary.setAvgMarginRate(validCount > 0 ? Math.round(totalMarginRate / validCount * 100) / 100.0 : 0.0);
        summary.setTotalCostChange(0.0); // 当前未接入成本历史，无变化数据
        return summary;
    }

    @Override
    public byte[] exportCostReport(String category, String status) {
        // 当前未接入报表引擎，返回空字节数组
        log.info("导出成本报表：category={}, status={}", category, status);
        return new byte[0];
    }

    // ==================== 私有辅助方法 ====================

    private FoodNew loadFood(String dishId) {
        Long foodId = parseLong(dishId);
        if (foodId == null) return null;
        try {
            return foodNewMapper.selectById(foodId);
        } catch (Exception e) {
            log.warn("查询菜品失败：dishId={}, error={}", dishId, e.getMessage());
            return null;
        }
    }

    private void fillSnapshotFromFood(DishCostSnapshotVO snapshot, FoodNew food) {
        snapshot.setDishId(String.valueOf(food.getFoodId()));
        snapshot.setDishName(food.getFoodName());
        if (food.getCategoryId() != null) {
            snapshot.setCategoryId(String.valueOf(food.getCategoryId()));
            try {
                FoodCategoryNew cat = categoryNewMapper.selectById(food.getCategoryId());
                if (cat != null) {
                    snapshot.setCategoryName(cat.getCategoryName());
                }
            } catch (Exception ignored) {
                // 分类查询失败不影响主流程
            }
        }
        snapshot.setSalePrice(food.getSalePrice());
        snapshot.setTotalCost(food.getCostPrice());
        if (food.getSalePrice() != null && food.getCostPrice() != null) {
            snapshot.setProfit(food.getSalePrice() - food.getCostPrice());
            double marginRate = calculateMarginRate(food.getSalePrice(), food.getCostPrice());
            snapshot.setMarginRate(marginRate);
            snapshot.setStatus(classifyMarginRate(marginRate));
        } else {
            snapshot.setStatus("normal");
        }
        snapshot.setCostChangePercent(0.0); // 当前未接入成本历史
        snapshot.setLastUpdated(LocalDateTime.now().format(ISO_FORMATTER));
    }

    private double calculateMarginRate(Long salePrice, Long costPrice) {
        if (salePrice == null || costPrice == null || salePrice <= 0) return 0.0;
        return Math.round((double) (salePrice - costPrice) / salePrice * 10000) / 100.0;
    }

    private String classifyMarginRate(double marginRate) {
        if (marginRate < 0) return "danger";
        if (marginRate < 30) return "warning";
        return "normal";
    }

    private Integer mapStatusToCode(String status) {
        if (status == null) return null;
        return switch (status) {
            case "normal" -> 1;
            case "warning" -> 3;
            case "danger" -> 2;
            default -> null;
        };
    }

    private void sortBy(List<DishCostSnapshotVO> data, String sortBy) {
        if (sortBy == null) return;
        Comparator<DishCostSnapshotVO> comparator = switch (sortBy) {
            case "cost_change" -> Comparator.comparingDouble(
                    v -> v.getCostChangePercent() != null ? v.getCostChangePercent() : 0.0);
            case "margin_rate" -> Comparator.comparingDouble(
                    v -> v.getMarginRate() != null ? v.getMarginRate() : 0.0);
            case "name" -> Comparator.comparing(
                    v -> v.getDishName() != null ? v.getDishName() : "");
            default -> Comparator.comparing(
                    v -> v.getDishId() != null ? v.getDishId() : "");
        };
        data.sort(comparator.reversed()); // 默认降序
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
