package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.entity.ComboIngredientNew;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.service.ComboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐服务实现类
 * 完整的套餐业务逻辑：CRUD、明细管理、成本计算、缓存更新
 */
@Service
public class ComboServiceImpl implements ComboService {

    private static final Logger log = LoggerFactory.getLogger(ComboServiceImpl.class);
    /** 套餐状态常量（遵循项目统一标准：1在售 0停售） */
    private static final int STATUS_ON_SALE = 1;
    private static final int STATUS_OFF_SALE = 0;

    private final DishComboNewMapper comboMapper;
    private final ComboIngredientNewMapper ingredientMapper;
    private final FoodNewMapper foodNewMapper;

    public ComboServiceImpl(DishComboNewMapper comboMapper,
                            ComboIngredientNewMapper ingredientMapper,
                            FoodNewMapper foodNewMapper) {
        this.comboMapper = comboMapper;
        this.ingredientMapper = ingredientMapper;
        this.foodNewMapper = foodNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComboVO create(ComboCreateDTO dto) {
        // 校验并生成编码
        String comboCode = dto.getComboCode();
        if (!StringUtils.hasText(comboCode)) {
            comboCode = generateComboCode();
        } else {
            checkComboCodeDuplicate(null, comboCode);
        }

        // 创建套餐主记录
        DishComboNew combo = new DishComboNew();
        combo.setComboName(dto.getComboName());
        combo.setComboCode(comboCode);
        combo.setComboPrice(dto.getComboPrice() != null ? dto.getComboPrice() : 0L);
        combo.setOriginalPrice(dto.getOriginalPrice());
        combo.setDiscountAmount(calculateDiscount(dto.getOriginalPrice(), dto.getComboPrice()));
        combo.setImageUrl(dto.getImageUrl());
        combo.setDescription(dto.getDescription());
        combo.setValidStartDate(dto.getValidStartDate());
        combo.setValidEndDate(dto.getValidEndDate());
        combo.setDailyLimit(dto.getDailyLimit());
        combo.setSoldToday(0);
        combo.setStatus(dto.getStatus() != null ? dto.getStatus() : STATUS_ON_SALE);
        combo.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        comboMapper.insert(combo);

        // 创建套餐明细
        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            saveIngredients(combo.getComboId(), dto.getIngredients());

            // 如果没有提供原价，根据明细自动计算
            if (dto.getOriginalPrice() == null) {
                long autoOriginalPrice = calculateOriginalPriceFromIngredients(combo.getComboId());
                combo.setOriginalPrice(autoOriginalPrice);
                combo.setDiscountAmount(calculateDiscount(autoOriginalPrice, combo.getComboPrice()));
                comboMapper.updateById(combo);
            }
        }

        log.info("创建套餐成功: comboId={}, comboName={}", combo.getComboId(), combo.getComboName());
        return convertToVO(combo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComboVO update(Long comboId, ComboUpdateDTO dto) {
        DishComboNew existing = getExistingCombo(comboId);

        // 更新基本字段
        if (dto.getComboName() != null) {
            existing.setComboName(dto.getComboName());
        }
        if (dto.getComboPrice() != null) {
            existing.setComboPrice(dto.getComboPrice());
        }
        if (dto.getOriginalPrice() != null) {
            existing.setOriginalPrice(dto.getOriginalPrice());
            existing.setDiscountAmount(calculateDiscount(dto.getOriginalPrice(), existing.getComboPrice()));
        }
        if (dto.getImageUrl() != null) {
            existing.setImageUrl(dto.getImageUrl());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getValidStartDate() != null) {
            existing.setValidStartDate(dto.getValidStartDate());
        }
        if (dto.getValidEndDate() != null) {
            existing.setValidEndDate(dto.getValidEndDate());
        }
        if (dto.getDailyLimit() != null) {
            existing.setDailyLimit(dto.getDailyLimit());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            existing.setSortOrder(dto.getSortOrder());
        }

        // 全量更新明细
        if (dto.getIngredients() != null) {
            // 删除旧明细
            deleteIngredients(comboId);
            // 保存新明细
            saveIngredients(comboId, dto.getIngredients());

            // 自动重新计算原价和优惠
            long autoOriginalPrice = calculateOriginalPriceFromIngredients(comboId);
            if (dto.getOriginalPrice() == null) {
                existing.setOriginalPrice(autoOriginalPrice);
                existing.setDiscountAmount(calculateDiscount(autoOriginalPrice, existing.getComboPrice()));
            }
        }

        comboMapper.updateById(existing);
        log.info("更新套餐成功: comboId={}", comboId);

        return convertToVO(existing);
    }

    @Override
    public ComboVO getById(Long comboId) {
        DishComboNew combo = getExistingCombo(comboId);
        return convertToVO(combo);
    }

    @Override
    public Page<ComboVO> queryPage(ComboQueryDTO queryDto) {
        Page<DishComboNew> page = new Page<>(queryDto.getPage(), queryDto.getSize());

        LambdaQueryWrapper<DishComboNew> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDto.getComboName())) {
            wrapper.like(DishComboNew::getComboName, queryDto.getComboName());
        }
        if (StringUtils.hasText(queryDto.getComboCode())) {
            wrapper.eq(DishComboNew::getComboCode, queryDto.getComboCode());
        }
        if (queryDto.getStatus() != null) {
            wrapper.eq(DishComboNew::getStatus, queryDto.getStatus());
        }

        // 排序
        boolean isAsc = "asc".equalsIgnoreCase(queryDto.getSortOrder());
        if ("combo_name".equals(queryDto.getSortField())) {
            wrapper.orderBy(true, isAsc, DishComboNew::getComboName);
        } else if ("combo_price".equals(queryDto.getSortField())) {
            wrapper.orderBy(true, isAsc, DishComboNew::getComboPrice);
        } else {
            wrapper.orderByAsc(DishComboNew::getSortOrder).orderByDesc(DishComboNew::getComboId);
        }

        Page<DishComboNew> resultPage = comboMapper.selectPage(page, wrapper);

        Page<ComboVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public List<ComboVO> listOnSale() {
        LambdaQueryWrapper<DishComboNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishComboNew::getStatus, STATUS_ON_SALE)
               .orderByAsc(DishComboNew::getSortOrder)
               .orderByDesc(DishComboNew::getComboId);

        List<DishComboNew> combos = comboMapper.selectList(wrapper);
        return combos.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public Long calculateCost(Long comboId) {
        getExistingCombo(comboId);  // 存在性校验

        // 获取所有明细
        LambdaQueryWrapper<ComboIngredientNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComboIngredientNew::getComboId, comboId);
        List<ComboIngredientNew> ingredients = ingredientMapper.selectList(wrapper);

        long totalCost = 0;
        for (ComboIngredientNew ing : ingredients) {
            FoodNew food = foodNewMapper.selectById(ing.getFoodId());
            if (food != null && food.getCostPrice() != null && ing.getQuantity() != null) {
                totalCost += food.getCostPrice() * ing.getQuantity();
            }
        }

        return totalCost;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long comboId, Integer status) {
        DishComboNew combo = getExistingCombo(comboId);
        
        if (status != STATUS_ON_SALE && status != STATUS_OFF_SALE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的状态值: " + status);
        }
        
        combo.setStatus(status);
        comboMapper.updateById(combo);
        
        log.info("更新套餐状态: comboId={}, status={}", comboId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long comboId) {
        getExistingCombo(comboId);  // 存在性校验
        
        // 先删除明细
        deleteIngredients(comboId);
        // 再删除主记录
        comboMapper.deleteById(comboId);
        
        log.info("删除套餐: comboId={}", comboId);
    }

    // ==================== 私有辅助方法 ====================

    private DishComboNew getExistingCombo(Long comboId) {
        DishComboNew combo = comboMapper.selectById(comboId);
        if (combo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在: " + comboId);
        }
        return combo;
    }

    /**
     * 保存套餐明细列表
     */
    private void saveIngredients(Long comboId, List<ComboIngredientDTO> ingredients) {
        for (int i = 0; i < ingredients.size(); i++) {
            ComboIngredientDTO item = ingredients.get(i);
            
            // 校验菜品是否存在且在售
            FoodNew food = foodNewMapper.selectById(item.getFoodId());
            if (food == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在: " + item.getFoodId());
            }

            ComboIngredientNew entity = new ComboIngredientNew();
            entity.setComboId(comboId);
            entity.setFoodId(item.getFoodId());
            entity.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
            entity.setUnit(item.getUnit() != null ? item.getUnit() : food.getUnit());
            entity.setIsRequired(item.getIsRequired() != null ? item.getIsRequired() : true);
            entity.setMaxSelect(item.getMaxSelect());
            entity.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);

            ingredientMapper.insert(entity);
        }
    }

    /**
     * 删除套餐的所有明细
     */
    private void deleteIngredients(Long comboId) {
        LambdaQueryWrapper<ComboIngredientNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComboIngredientNew::getComboId, comboId);
        ingredientMapper.delete(wrapper);
    }

    /**
     * 根据明细计算原价（各单品售价之和）
     */
    private Long calculateOriginalPriceFromIngredients(Long comboId) {
        LambdaQueryWrapper<ComboIngredientNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComboIngredientNew::getComboId, comboId);
        List<ComboIngredientNew> ingredients = ingredientMapper.selectList(wrapper);

        long total = 0;
        for (ComboIngredientNew ing : ingredients) {
            FoodNew food = foodNewMapper.selectById(ing.getFoodId());
            if (food != null && food.getSalePrice() != null && ing.getQuantity() != null) {
                total += food.getSalePrice() * ing.getQuantity();
            }
        }
        return total > 0 ? total : null;
    }

    /**
     * 计算优惠金额
     */
    private Long calculateDiscount(Long originalPrice, Long comboPrice) {
        if (originalPrice == null || comboPrice == null || originalPrice <= comboPrice) {
            return 0L;
        }
        return originalPrice - comboPrice;
    }

    /**
     * 生成套餐编码
     */
    private String generateComboCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CB" + datePart;

        LambdaQueryWrapper<DishComboNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DishComboNew::getComboCode, prefix)
               .orderByDesc(DishComboNew::getComboCode)
               .last("LIMIT 1");

        DishComboNew lastCombo = comboMapper.selectOne(wrapper);

        int seq = 1;
        if (lastCombo != null && lastCombo.getComboCode() != null) {
            try {
                String lastSeqStr = lastCombo.getComboCode().substring(prefix.length());
                seq = Integer.parseInt(lastSeqStr) + 1;
            } catch (Exception e) {
                seq = 1;
            }
        }

        return String.format("%s%04d", prefix, seq);
    }

    private void checkComboCodeDuplicate(Long excludeId, String comboCode) {
        LambdaQueryWrapper<DishComboNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishComboNew::getComboCode, comboCode);
        if (excludeId != null) {
            wrapper.ne(DishComboNew::getComboId, excludeId);
        }
        Long count = comboMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "套餐编码已存在: " + comboCode);
        }
    }

    /**
     * 将实体转换为VO
     */
    private ComboVO convertToVO(DishComboNew combo) {
        if (combo == null) {
            return null;
        }

        ComboVO vo = new ComboVO();
        vo.setComboId(combo.getComboId());
        vo.setComboCode(combo.getComboCode());
        vo.setComboName(combo.getComboName());
        vo.setComboPrice(combo.getComboPrice());
        vo.setOriginalPrice(combo.getOriginalPrice());
        vo.setDiscountAmount(combo.getDiscountAmount());
        vo.setImageUrl(combo.getImageUrl());
        vo.setDescription(combo.getDescription());
        vo.setValidStartDate(combo.getValidStartDate());
        vo.setValidEndDate(combo.getValidEndDate());
        vo.setDailyLimit(combo.getDailyLimit());
        vo.setSoldToday(combo.getSoldToday());
        vo.setStatus(combo.getStatus());
        vo.setStatusName(getStatusName(combo.getStatus()));
        vo.setSortOrder(combo.getSortOrder());
        vo.setCreateTime(combo.getCreateTime());
        vo.setUpdateTime(combo.getUpdateTime());

        // 计算优惠率
        if (combo.getOriginalPrice() != null && combo.getOriginalPrice() > 0 
                && combo.getDiscountAmount() != null) {
            double discountRate = Math.round((double) combo.getDiscountAmount() / combo.getOriginalPrice() * 10000) / 100.0;
            vo.setDiscountRate(discountRate);
        }

        // 加载明细
        List<ComboIngredientVO> ingredientVOs = loadIngredients(combo.getComboId());
        vo.setIngredients(ingredientVOs);

        // 计算成本和毛利（基于 combo_ingredients 关联 foods.cost_price）
        long totalCost = calculateCostInternal(combo.getComboId(), ingredientVOs);
        vo.setTotalCost(totalCost);
        vo.setCostPrice(totalCost);

        // 计算利润和利润率：利润率 = (comboPrice - totalCost) / comboPrice * 100
        if (combo.getComboPrice() != null && combo.getComboPrice() > 0 && totalCost > 0) {
            long profit = combo.getComboPrice() - totalCost;
            vo.setProfit(profit);
            double profitRate = (double) profit / combo.getComboPrice() * 100;
            vo.setProfitRate(Math.round(profitRate * 100) / 100.0);
        } else {
            vo.setProfitRate(0.0);
        }

        return vo;
    }

    /**
     * 加载套餐明细并转换为VO
     */
    private List<ComboIngredientVO> loadIngredients(Long comboId) {
        LambdaQueryWrapper<ComboIngredientNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComboIngredientNew::getComboId, comboId)
               .orderByAsc(ComboIngredientNew::getSortOrder);

        List<ComboIngredientNew> ingredients = ingredientMapper.selectList(wrapper);
        List<ComboIngredientVO> result = new ArrayList<>();

        for (ComboIngredientNew ing : ingredients) {
            ComboIngredientVO vo = new ComboIngredientVO();
            vo.setIngredientId(ing.getIngredientId());
            vo.setComboId(ing.getComboId());
            vo.setFoodId(ing.getFoodId());
            vo.setQuantity(ing.getQuantity());
            vo.setUnit(ing.getUnit());
            vo.setIsRequired(ing.getIsRequired());
            vo.setMaxSelect(ing.getMaxSelect());
            vo.setSortOrder(ing.getSortOrder());

            // 关联菜品信息
            FoodNew food = foodNewMapper.selectById(ing.getFoodId());
            if (food != null) {
                vo.setFoodName(food.getFoodName());
                vo.setFoodCode(food.getFoodCode());
                vo.setUnitPrice(food.getSalePrice());
                
                // 计算小计
                if (food.getSalePrice() != null && ing.getQuantity() != null) {
                    vo.setSubtotal(food.getSalePrice() * ing.getQuantity());
                }
            }

            result.add(vo);
        }

        return result;
    }

    /**
     * 内部成本计算（基于已加载的明细）
     */
    private long calculateCostInternal(Long comboId, List<ComboIngredientVO> ingredients) {
        long totalCost = 0;
        for (ComboIngredientVO ing : ingredients) {
            FoodNew food = foodNewMapper.selectById(ing.getFoodId());
            if (food != null && food.getCostPrice() != null && ing.getQuantity() != null) {
                totalCost += food.getCostPrice() * ing.getQuantity();
            }
        }
        return totalCost;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_ON_SALE -> "在售";
            case STATUS_OFF_SALE -> "停售";
            default -> "未知";
        };
    }
}
