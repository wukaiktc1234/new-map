package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.product.PricingCreateDTO;
import com.foodtraceability.dto.product.PricingQueryDTO;
import com.foodtraceability.dto.product.PricingVO;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.entity.ProductPricingHistory;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.ProductPricingHistoryMapper;
import com.foodtraceability.service.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品定价服务实现类
 * 管理产品价格变动历史和批量调价
 */
@Service
public class PricingServiceImpl implements PricingService {

    private static final Logger log = LoggerFactory.getLogger(PricingServiceImpl.class);

    private final ProductPricingHistoryMapper pricingMapper;
    private final FoodNewMapper foodNewMapper;
    private final DishComboNewMapper comboNewMapper;

    public PricingServiceImpl(ProductPricingHistoryMapper pricingMapper,
                             FoodNewMapper foodNewMapper,
                             DishComboNewMapper comboNewMapper) {
        this.pricingMapper = pricingMapper;
        this.foodNewMapper = foodNewMapper;
        this.comboNewMapper = comboNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PricingVO create(PricingCreateDTO dto) {
        // 校验产品类型
        validateProductType(dto.getProductType());

        // 获取当前价格
        Long oldPrice = getCurrentPrice(dto.getProductType(), dto.getProductId());
        String productName = getProductName(dto.getProductType(), dto.getProductId());

        if (productName == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在: type=" + dto.getProductType() + ", id=" + dto.getProductId());
        }

        // 创建定价记录
        ProductPricingHistory history = new ProductPricingHistory();
        history.setProductType(dto.getProductType());
        history.setProductName(productName);
        history.setProductId(dto.getProductId());
        history.setOldSalePrice(oldPrice);
        history.setNewSalePrice(dto.getSalePrice());
        history.setCostPrice(dto.getCostPrice());
        history.setPricingStrategy(dto.getPricingStrategy() != null ? dto.getPricingStrategy() : "MANUAL");
        history.setRemark(dto.getRemark());
        history.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now());

        pricingMapper.insert(history);

        // 更新产品当前售价
        updateProductPrice(dto.getProductType(), dto.getProductId(), dto.getSalePrice());

        log.info("创建定价记录: pricingId={}, product={}, oldPrice={}, newPrice={}",
                history.getPricingId(), productName, oldPrice, dto.getSalePrice());

        return convertToVO(history);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PricingVO> batchPricing(String productType, List<PricingCreateDTO> pricingList) {
        validateProductType(productType);

        // 安全性校验：限制单次批量调价数量，防止恶意大批量请求
        if (pricingList.size() > 100) {
            throw new IllegalArgumentException("单次批量调价不能超过100条记录");
        }

        List<PricingVO> results = new ArrayList<>();
        
        for (PricingCreateDTO dto : pricingList) {
            dto.setProductType(productType);  // 强制使用传入的类型
            results.add(create(dto));
        }

        log.info("批量调价完成: type={}, count={}", productType, pricingList.size());
        return results;
    }

    @Override
    public Page<PricingVO> queryHistory(PricingQueryDTO queryDto) {
        Page<ProductPricingHistory> page = new Page<>(queryDto.getPage(), queryDto.getSize());

        LambdaQueryWrapper<ProductPricingHistory> wrapper = new LambdaQueryWrapper<>();

        // 产品类型筛选
        if (StringUtils.hasText(queryDto.getProductType()) && !"ALL".equals(queryDto.getProductType())) {
            wrapper.eq(ProductPricingHistory::getProductType, queryDto.getProductType());
        }
        // 产品筛选
        if (queryDto.getProductId() != null) {
            wrapper.eq(ProductPricingHistory::getProductId, queryDto.getProductId());
        }
        // 定价策略筛选
        if (StringUtils.hasText(queryDto.getPricingStrategy())) {
            wrapper.eq(ProductPricingHistory::getPricingStrategy, queryDto.getPricingStrategy());
        }
        // 日期范围筛选
        if (queryDto.getStartDate() != null) {
            wrapper.ge(ProductPricingHistory::getEffectiveDate, queryDto.getStartDate());
        }
        if (queryDto.getEndDate() != null) {
            wrapper.le(ProductPricingHistory::getEffectiveDate, queryDto.getEndDate());
        }
        // 操作人筛选
        if (StringUtils.hasText(queryDto.getOperatorName())) {
            wrapper.like(ProductPricingHistory::getOperatorName, queryDto.getOperatorName());
        }

        // 按时间倒序排列
        boolean isDesc = "desc".equalsIgnoreCase(queryDto.getSortOrder());
        wrapper.orderBy(true, isDesc, ProductPricingHistory::getCreateTime);

        Page<ProductPricingHistory> resultPage = pricingMapper.selectPage(page, wrapper);

        Page< PricingVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public Long getCurrentPrice(String productType, Long productId) {
        if ("FOOD".equalsIgnoreCase(productType)) {
            FoodNew food = foodNewMapper.selectById(productId);
            return food != null ? food.getSalePrice() : null;
        } else if ("COMBO".equalsIgnoreCase(productType)) {
            DishComboNew combo = comboNewMapper.selectById(productId);
            return combo != null ? combo.getComboPrice() : null;
        }
        return null;
    }

    @Override
    public List<PricingVO> getPriceHistory(String productType, Long productId, int limit) {
        // 安全性校验：限制最大返回数量，防止恶意请求
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<ProductPricingHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductPricingHistory::getProductType, productType)
               .eq(ProductPricingHistory::getProductId, productId)
               .orderByDesc(ProductPricingHistory::getCreateTime)
               .last("LIMIT " + safeLimit);

        List<ProductPricingHistory> histories = pricingMapper.selectList(wrapper);
        return histories.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 更新产品的当前售价
     */
    private void updateProductPrice(String productType, Long productId, Long newPrice) {
        if ("FOOD".equalsIgnoreCase(productType)) {
            FoodNew food = foodNewMapper.selectById(productId);
            if (food != null) {
                food.setSalePrice(newPrice);
                foodNewMapper.updateById(food);
            }
        } else if ("COMBO".equalsIgnoreCase(productType)) {
            DishComboNew combo = comboNewMapper.selectById(productId);
            if (combo != null) {
                combo.setComboPrice(newPrice);
                // 重新计算优惠金额
                if (combo.getOriginalPrice() != null && combo.getOriginalPrice() > newPrice) {
                    combo.setDiscountAmount(combo.getOriginalPrice() - newPrice);
                } else {
                    combo.setDiscountAmount(0L);
                }
                comboNewMapper.updateById(combo);
            }
        }
    }

    /**
     * 获取产品名称
     */
    private String getProductName(String productType, Long productId) {
        if ("FOOD".equalsIgnoreCase(productType)) {
            FoodNew food = foodNewMapper.selectById(productId);
            return food != null ? food.getFoodName() : null;
        } else if ("COMBO".equalsIgnoreCase(productType)) {
            DishComboNew combo = comboNewMapper.selectById(productId);
            return combo != null ? combo.getComboName() : null;
        }
        return null;
    }

    private void validateProductType(String productType) {
        if (!"FOOD".equalsIgnoreCase(productType) && !"COMBO".equalsIgnoreCase(productType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的产品类型: " + productType + "，应为FOOD或COMBO");
        }
    }

    /**
     * 将实体转换为VO
     */
    private PricingVO convertToVO(ProductPricingHistory history) {
        if (history == null) {
            return null;
        }

        PricingVO vo = new PricingVO();
        vo.setPricingId(history.getPricingId());
        vo.setProductType(history.getProductType());
        vo.setProductName(history.getProductName());
        vo.setProductId(history.getProductId());
        vo.setOldSalePrice(history.getOldSalePrice());
        vo.setNewSalePrice(history.getNewSalePrice());
        vo.setCostPrice(history.getCostPrice());
        vo.setPricingStrategy(history.getPricingStrategy());
        vo.setPricingStrategyName(getStrategyName(history.getPricingStrategy()));
        vo.setRemark(history.getRemark());
        vo.setOperatorName(history.getOperatorName());
        vo.setEffectiveDate(history.getEffectiveDate());
        vo.setCreateTime(history.getCreateTime());

        // 计算价格变动
        if (history.getOldSalePrice() != null && history.getNewSalePrice() != null) {
            long change = history.getNewSalePrice() - history.getOldSalePrice();
            vo.setPriceChange(change >= 0 ? "+" + change : String.valueOf(change));
            
            if (history.getOldSalePrice() > 0) {
                double percent = Math.round((double) change / history.getOldSalePrice() * 10000) / 100.0;
                vo.setChangePercent(percent);
            }
        }

        // 计算毛利和毛利率
        if (history.getNewSalePrice() != null && history.getCostPrice() != null 
                && history.getCostPrice() > 0) {
            long profit = history.getNewSalePrice() - history.getCostPrice();
            vo.setProfit(profit);
            double profitRate = Math.round((double) profit / history.getCostPrice() * 10000) / 100.0;
            vo.setProfitRate(profitRate);
        }

        return vo;
    }

    private String getStrategyName(String strategy) {
        if (strategy == null) return "未知";
        return switch (strategy.toUpperCase()) {
            case "MANUAL" -> "手动调整";
            case "AUTO" -> "自动计算";
            case "BATCH" -> "批量调价";
            default -> strategy;
        };
    }
}
