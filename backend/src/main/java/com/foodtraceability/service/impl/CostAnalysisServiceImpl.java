package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.product.CostAnalysisQueryDTO;
import com.foodtraceability.dto.product.CostAnalysisVO;
import com.foodtraceability.dto.product.CostTrendDataVO;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.FoodCategoryNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodCategoryNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.OrderItemNewMapper;
import com.foodtraceability.service.CostAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成本分析服务实现类
 * 提供完整的成本核算、毛利分析和报表功能
 */
@Service
public class CostAnalysisServiceImpl implements CostAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CostAnalysisServiceImpl.class);

    private final FoodNewMapper foodNewMapper;
    private final DishComboNewMapper comboNewMapper;
    private final FoodCategoryNewMapper categoryNewMapper;
    private final OrderItemNewMapper orderItemNewMapper;

    public CostAnalysisServiceImpl(FoodNewMapper foodNewMapper,
                                   DishComboNewMapper comboNewMapper,
                                   FoodCategoryNewMapper categoryNewMapper,
                                   OrderItemNewMapper orderItemNewMapper) {
        this.foodNewMapper = foodNewMapper;
        this.comboNewMapper = comboNewMapper;
        this.categoryNewMapper = categoryNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
    }

    @Override
    public Page<CostAnalysisVO> queryAnalysis(CostAnalysisQueryDTO queryDto) {
        // 根据产品类型分别查询
        List<CostAnalysisVO> allData = new ArrayList<>();

        if ("ALL".equals(queryDto.getProductType()) || "FOOD".equals(queryDto.getProductType())) {
            allData.addAll(queryFoodData(queryDto));
        }
        if ("ALL".equals(queryDto.getProductType()) || "COMBO".equals(queryDto.getProductType())) {
            allData.addAll(queryComboData(queryDto));
        }

        // 应用毛利率筛选
        if (queryDto.getMinProfitRate() != null || queryDto.getMaxProfitRate() != null) {
            allData = allData.stream()
                    .filter(vo -> {
                        if (vo.getProfitRate() == null) return false;
                        if (queryDto.getMinProfitRate() != null && vo.getProfitRate() < queryDto.getMinProfitRate()) return false;
                        if (queryDto.getMaxProfitRate() != null && vo.getProfitRate() > queryDto.getMaxProfitRate()) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // 排序
        sortData(allData, queryDto.getSortField(), queryDto.getSortOrder());

        // 手动分页
        int total = allData.size();
        int start = (queryDto.getPage() - 1) * queryDto.getSize();
        int end = Math.min(start + queryDto.getSize(), total);

        List<CostAnalysisVO> pageData = start < total ?
                allData.subList(start, end) : new ArrayList<>();

        // 为分页后的数据补充周销售量和周转率（避免对全量数据计算，提升性能）
        enrichWithWeeklySales(pageData);

        Page<CostAnalysisVO> result = new Page<>(queryDto.getPage(), queryDto.getSize(), total);
        result.setRecords(pageData);

        return result;
    }

    /**
     * 为VO列表补充周销售量和周转率
     * 周销售量：从 order_items 表聚合近 7 天的销量
     * 周转率(%) = 周销售量 / 当前库存 * 100（库存为 0 或 null 时不计算，返回 null）
     *
     * 性能考虑：批量查询，避免 N+1 问题
     */
    private void enrichWithWeeklySales(List<CostAnalysisVO> vos) {
        if (vos == null || vos.isEmpty()) return;

        try {
            // 收集所有菜品和套餐的ID
            List<Long> foodIds = vos.stream()
                    .filter(vo -> "FOOD".equals(vo.getProductType()) && vo.getProductId() != null)
                    .map(CostAnalysisVO::getProductId)
                    .collect(Collectors.toList());
            List<Long> comboIds = vos.stream()
                    .filter(vo -> "COMBO".equals(vo.getProductType()) && vo.getProductId() != null)
                    .map(CostAnalysisVO::getProductId)
                    .collect(Collectors.toList());

            // 批量查询周销售量（一次性查全表，再按ID过滤）
            Map<Long, Integer> foodSalesMap = buildWeeklySalesMap(
                    orderItemNewMapper.selectWeeklySalesForFoods(), "foodId");
            Map<Long, Integer> comboSalesMap = buildWeeklySalesMap(
                    orderItemNewMapper.selectWeeklySalesForCombos(), "comboId");

            // 批量查询菜品库存（套餐无库存概念，周转率返回 null）
            Map<Long, Integer> stockMap = new HashMap<>();
            if (!foodIds.isEmpty()) {
                List<FoodNew> foods = foodNewMapper.selectBatchIds(foodIds);
                for (FoodNew f : foods) {
                    if (f.getFoodId() != null) {
                        stockMap.put(f.getFoodId(), f.getStock() != null ? f.getStock() : 0);
                    }
                }
            }

            // 填充 weeklySales 和 turnoverRate
            for (CostAnalysisVO vo : vos) {
                if (vo.getProductId() == null) continue;
                Long id = vo.getProductId();

                if ("FOOD".equals(vo.getProductType())) {
                    int weeklySales = foodSalesMap.getOrDefault(id, 0);
                    vo.setWeeklySales(weeklySales);
                    Integer stock = stockMap.get(id);
                    if (stock != null && stock > 0) {
                        // 周转率 = 周销售量 / 库存 * 100，保留 1 位小数
                        double rate = Math.round((double) weeklySales / stock * 1000) / 10.0;
                        vo.setTurnoverRate(rate);
                    } else {
                        vo.setTurnoverRate(null);
                    }
                } else if ("COMBO".equals(vo.getProductType())) {
                    int weeklySales = comboSalesMap.getOrDefault(id, 0);
                    vo.setWeeklySales(weeklySales);
                    // 套餐无库存概念，周转率返回 null
                    vo.setTurnoverRate(null);
                }
            }
        } catch (Exception e) {
            log.warn("补充周销售量数据失败，忽略错误继续返回基础数据", e);
        }
    }

    /**
     * 将 Mapper 返回的 List<Map> 转换为 ID → 周销售量 的 Map
     */
    private Map<Long, Integer> buildWeeklySalesMap(List<Map<String, Object>> rows, String idKey) {
        Map<Long, Integer> map = new HashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> row : rows) {
            Object idObj = row.get(idKey);
            Object salesObj = row.get("weeklySales");
            if (idObj != null) {
                Long id = toLong(idObj);
                int sales = (int) toLong(salesObj);
                map.put(id, sales);
            }
        }
        return map;
    }

    @Override
    public Map<String, Object> getSummary(CostAnalysisQueryDTO queryDto) {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        // 获取所有在售产品
        LambdaQueryWrapper<FoodNew> foodWrapper = new LambdaQueryWrapper<>();
        foodWrapper.eq(FoodNew::getStatus, 1)
                   .isNotNull(FoodNew::getSalePrice)
                   .isNotNull(FoodNew::getCostPrice);
        List<FoodNew> foods = foodNewMapper.selectList(foodWrapper);

        LambdaQueryWrapper<DishComboNew> comboWrapper = new LambdaQueryWrapper<>();
        comboWrapper.eq(DishComboNew::getStatus, 1)
                   .isNotNull(DishComboNew::getComboPrice);
        List<DishComboNew> combos = comboNewMapper.selectList(comboWrapper);

        // 统计菜品
        long foodCount = foods.size();
        long foodTotalSales = 0L;   // 总销售额（分）
        long foodTotalCost = 0L;    // 总成本（分）
        long foodTotalProfit = 0L;  // 总毛利（分）

        for (FoodNew f : foods) {
            if (f.getSalePrice() != null) foodTotalSales += f.getSalePrice();
            if (f.getCostPrice() != null) foodTotalCost += f.getCostPrice();
        }
        foodTotalProfit = foodTotalSales - foodTotalCost;

        // 统计套餐
        long comboCount = combos.size();
        long comboTotalSales = 0L;
        long comboTotalCost = 0L;

        for (DishComboNew c : combos) {
            if (c.getComboPrice() != null) comboTotalSales += c.getComboPrice();
            // 套餐成本需要从明细计算，这里简化处理
        }
        long comboTotalProfit = comboTotalSales - comboTotalCost;

        // 汇总
        long totalCount = foodCount + comboCount;
        long totalSales = foodTotalSales + comboTotalSales;
        long totalCost = foodTotalCost + comboTotalCost;
        long totalProfit = foodTotalProfit + comboTotalProfit;

        summary.put("totalProducts", totalCount);
        summary.put("foodCount", foodCount);
        summary.put("comboCount", comboCount);
        summary.put("totalSalesAmount", totalSales);      // 总销售额（分）
        summary.put("totalCostAmount", totalCost);         // 总成本（分）
        summary.put("totalProfitAmount", totalProfit);      // 总毛利（分）
        summary.put("avgProfitRate", totalCost > 0 ? 
                Math.round((double) totalProfit / totalCost * 10000) / 100.0 : 0.0);
        summary.put("foodAvgProfitRate", foodTotalCost > 0 ?
                Math.round((double) foodTotalProfit / foodTotalCost * 10000) / 100.0 : 0.0);

        log.debug("成本分析汇总: products={}, profitRate={}%", 
                totalCount, summary.get("avgProfitRate"));

        return summary;
    }

    @Override
    public Map<String, Integer> getProfitRateDistribution(CostAnalysisQueryDTO queryDto) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        // 初始化区间
        distribution.put("<10%", 0);
        distribution.put("10%-20%", 0);
        distribution.put("20%-30%", 0);
        distribution.put("30%-50%", 0);
        distribution.put("50%-70%", 0);
        distribution.put(">70%", 0);

        // 统计菜品
        List<FoodNew> foods = foodNewMapper.selectList(
                new LambdaQueryWrapper<FoodNew>()
                        .eq(FoodNew::getStatus, 1)
                        .isNotNull(FoodNew::getSalePrice)
                        .isNotNull(FoodNew::getCostPrice)
        );

        for (FoodNew f : foods) {
            double rate = calculateProfitRate(f.getSalePrice(), f.getCostPrice());
            String range = getProfitRange(rate);
            distribution.merge(range, 1, Integer::sum);
        }

        return distribution;
    }

    @Override
    public List<Map<String, Object>> getCategoryCostRanking(int topN) {
        // 获取所有分类
        List<FoodCategoryNew> categories = categoryNewMapper.selectList(
                new LambdaQueryWrapper<FoodCategoryNew>()
                        .eq(FoodCategoryNew::getStatus, 1)
                        .orderByAsc(FoodCategoryNew::getCategoryId)
        );

        List<Map<String, Object>> ranking = new ArrayList<>();

        for (FoodCategoryNew cat : categories) {
            // 统计该分类下的菜品
            LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FoodNew::getCategoryId, cat.getCategoryId())
                   .eq(FoodNew::getStatus, 1)
                   .isNotNull(FoodNew::getSalePrice)
                   .isNotNull(FoodNew::getCostPrice);

            List<FoodNew> foods = foodNewMapper.selectList(wrapper);
            
            if (!foods.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("categoryId", cat.getCategoryId());
                item.put("categoryName", cat.getCategoryName());
                item.put("foodCount", foods.size());

                long totalSalePrice = 0L;
                long totalCostPrice = 0L;

                for (FoodNew f : foods) {
                    if (f.getSalePrice() != null) totalSalePrice += f.getSalePrice();
                    if (f.getCostPrice() != null) totalCostPrice += f.getCostPrice();
                }

                item.put("totalSalePrice", totalSalePrice);
                item.put("totalCostPrice", totalCostPrice);
                item.put("totalProfit", totalSalePrice - totalCostPrice);
                item.put("avgProfitRate", totalCostPrice > 0 ? 
                        Math.round((double)(totalSalePrice - totalCostPrice) / totalCostPrice * 10000) / 100.0 : 0.0);

                ranking.add(item);
            }
        }

        // 按总销售额降序排列
        ranking.sort((a, b) -> ((Long)b.get("totalSalePrice")).compareTo((Long)a.get("totalSalePrice")));

        return ranking.stream().limit(topN).collect(Collectors.toList());
    }

    @Override
    public List<CostAnalysisVO> getLowProfitWarning(double threshold, int limit) {
        // 获取所有低毛利的在售菜品
        List<FoodNew> foods = foodNewMapper.selectList(
                new LambdaQueryWrapper<FoodNew>()
                        .eq(FoodNew::getStatus, 1)
                        .isNotNull(FoodNew::getSalePrice)
                        .isNotNull(FoodNew::getCostPrice)
                        .orderByAsc(FoodNew::getSalePrice)
        );

        return foods.stream()
                .map(f -> {
                    double rate = calculateProfitRate(f.getSalePrice(), f.getCostPrice());
                    if (rate < threshold) {
                        CostAnalysisVO vo = convertFoodToAnalysisVO(f);
                        vo.setProductType("FOOD");
                        return vo;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(CostAnalysisVO::getProfitRate))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<CostTrendDataVO> getTrend(String startDate, String endDate, Long dishId) {
        // 当前未接入成本历史表，返回空列表
        // 后续接入成本历史表后，按日期聚合计算 avgCost/maxCost/minCost/dishCount
        log.debug("查询成本趋势：startDate={}, endDate={}, dishId={}", startDate, endDate, dishId);
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getSalesSummary(int topN) {
        // 从 order_items 表聚合销量与销售额，按销售额降序返回 TOP N
        // 当订单明细表为空时返回空列表，前端据此显示"暂无销售数据"提示
        int limit = Math.max(1, Math.min(topN, 50));
        List<Map<String, Object>> rows = orderItemNewMapper.selectSalesSummary(limit);
        if (rows == null) {
            return new ArrayList<>();
        }
        // 规范化字段类型，避免 H2/PostgreSQL 在 SUM 结果上返回 BigDecimal/Long 不一致
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productName", row.get("productName"));
            // productType: 1单品 2套餐，转换为前端可读字符串
            Object ptRaw = row.get("productType");
            String productType = "FOOD";
            if (ptRaw != null && "2".equals(String.valueOf(ptRaw))) {
                productType = "COMBO";
            }
            item.put("productType", productType);
            item.put("salesCount", toLong(row.get("salesCount")));
            item.put("salesAmount", toLong(row.get("salesAmount")));
            result.add(item);
        }
        log.debug("销售数据汇总: 返回 {} 条 TOP {} 记录", result.size(), limit);
        return result;
    }

    /**
     * 将 Number/Object 安全转换为 long，无效值返回 0
     */
    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查询菜品数据并转换为分析VO
     * status 为 null 时不过滤状态（查全部），有值时按 status 过滤
     * keyword 有值时按 food_name 或 food_code 模糊匹配
     */
    private List<CostAnalysisVO> queryFoodData(CostAnalysisQueryDTO queryDto) {
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(FoodNew::getSalePrice);

        if (queryDto.getCategoryId() != null) {
            wrapper.eq(FoodNew::getCategoryId, queryDto.getCategoryId());
        }
        // status 为 null 时不过滤状态，避免默认 status=1 导致停售菜品被过滤
        if (queryDto.getStatus() != null) {
            wrapper.eq(FoodNew::getStatus, queryDto.getStatus());
        }
        // keyword 模糊匹配菜品名称或编码
        if (StringUtils.hasText(queryDto.getKeyword())) {
            String kw = queryDto.getKeyword().trim();
            wrapper.and(w -> w.like(FoodNew::getFoodName, kw)
                    .or().like(FoodNew::getFoodCode, kw));
        }

        List<FoodNew> foods = foodNewMapper.selectList(wrapper);
        return foods.stream().map(f -> {
            CostAnalysisVO vo = convertFoodToAnalysisVO(f);
            vo.setProductType("FOOD");
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询套餐数据并转换为分析VO
     * status 为 null 时不过滤状态（查全部），有值时按 status 过滤
     * keyword 有值时按 combo_name 或 combo_code 模糊匹配
     */
    private List<CostAnalysisVO> queryComboData(CostAnalysisQueryDTO queryDto) {
        LambdaQueryWrapper<DishComboNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(DishComboNew::getComboPrice);

        // status 为 null 时不过滤状态，避免默认 status=1 导致停售套餐被过滤
        if (queryDto.getStatus() != null) {
            wrapper.eq(DishComboNew::getStatus, queryDto.getStatus());
        }
        // keyword 模糊匹配套餐名称或编码
        if (StringUtils.hasText(queryDto.getKeyword())) {
            String kw = queryDto.getKeyword().trim();
            wrapper.and(w -> w.like(DishComboNew::getComboName, kw)
                    .or().like(DishComboNew::getComboCode, kw));
        }

        List<DishComboNew> combos = comboNewMapper.selectList(wrapper);
        return combos.stream().map(c -> {
            CostAnalysisVO vo = convertComboToAnalysisVO(c);
            vo.setProductType("COMBO");
            return vo;
        }).collect(Collectors.toList());
    }

    private CostAnalysisVO convertFoodToAnalysisVO(FoodNew food) {
        CostAnalysisVO vo = new CostAnalysisVO();
        vo.setProductId(food.getFoodId());
        vo.setProductCode(food.getFoodCode());
        vo.setProductName(food.getFoodName());
        vo.setSpecification(food.getSpecification());
        vo.setUnit(food.getUnit());
        vo.setSalePrice(food.getSalePrice());
        vo.setCostPrice(food.getCostPrice());
        vo.setStatus(food.getStatus());

        // 计算毛利
        if (food.getSalePrice() != null && food.getCostPrice() != null) {
            long profit = food.getSalePrice() - food.getCostPrice();
            vo.setProfit(profit);
            double rate = calculateProfitRate(food.getSalePrice(), food.getCostPrice());
            vo.setProfitRate(rate);
        }

        // 设置分类名称
        if (food.getCategoryId() != null) {
            FoodCategoryNew category = categoryNewMapper.selectById(food.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }

        return vo;
    }

    private CostAnalysisVO convertComboToAnalysisVO(DishComboNew combo) {
        CostAnalysisVO vo = new CostAnalysisVO();
        vo.setProductId(combo.getComboId());
        vo.setProductCode(combo.getComboCode());
        vo.setProductName(combo.getComboName());
        vo.setSalePrice(combo.getComboPrice());
        vo.setCostPrice(null);  // 套餐成本需从明细计算，此处简化
        vo.setStatus(combo.getStatus());

        if (combo.getComboPrice() != null && combo.getOriginalPrice() != null) {
            long discount = combo.getOriginalPrice() - combo.getComboPrice();
            vo.setProfit(discount);
            if (combo.getOriginalPrice() > 0) {
                vo.setProfitRate(Math.round((double) discount / combo.getOriginalPrice() * 10000) / 100.0);
            }
        }

        return vo;
    }

    private double calculateProfitRate(Long salePrice, Long costPrice) {
        if (salePrice == null || costPrice == null || costPrice <= 0) {
            return 0.0;
        }
        return Math.round((double)(salePrice - costPrice) / costPrice * 10000) / 100.0;
    }

    private void sortData(List<CostAnalysisVO> data, String sortField, String sortOrder) {
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        Comparator<CostAnalysisVO> comparator = switch (sortField) {
            case "profitRate" -> Comparator.comparingDouble(
                    v -> v.getProfitRate() != null ? v.getProfitRate() : 0.0);
            case "salePrice" -> Comparator.comparingLong(
                    v -> v.getSalePrice() != null ? v.getSalePrice() : 0L);
            case "costPrice" -> Comparator.comparingLong(
                    v -> v.getCostPrice() != null ? v.getCostPrice() : 0L);
            case "productName" -> Comparator.comparing(
                    v -> v.getProductName() != null ? v.getProductName() : "");
            default -> Comparator.comparingLong(v -> v.getProductId() != null ? v.getProductId() : 0L);
        };

        data.sort(isAsc ? comparator : comparator.reversed());
    }

    private String getProfitRange(double rate) {
        if (rate < 10) return "<10%";
        if (rate < 20) return "10%-20%";
        if (rate < 30) return "20%-30%";
        if (rate < 50) return "30%-50%";
        if (rate < 70) return "50%-70%";
        return ">70%";
    }
}
