package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.Budget;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.BudgetMapper;
import com.foodtraceability.service.finance.BudgetService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预算管理Service实现
 */
@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget>
        implements BudgetService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetVO create(BudgetCreateDTO dto) {
        // 检查唯一约束（year + month + type + category）
        LambdaQueryWrapper<Budget> uniqueCheck = new LambdaQueryWrapper<>();
        uniqueCheck.eq(Budget::getBudgetYear, dto.getBudgetYear())
                   .eq(Budget::getBudgetType, dto.getBudgetType())
                   .eq(Budget::getCategoryId, dto.getCategoryId());
        if (dto.getBudgetMonth() != null) {
            uniqueCheck.eq(Budget::getBudgetMonth, dto.getBudgetMonth());
        } else {
            uniqueCheck.isNull(Budget::getBudgetMonth);
        }
        if (baseMapper.selectCount(uniqueCheck) > 0) {
            throw new BusinessException("该预算已存在，请勿重复创建");
        }

        Budget entity = new Budget();
        BeanUtils.copyProperties(dto, entity);
        entity.setActualAmount(0L);
        entity.setVariance(dto.getBudgetAmount());
        entity.setVarianceRate(BigDecimal.ZERO);

        baseMapper.insert(entity);
        return getDetail(entity.getBudgetId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(BudgetUpdateDTO dto) {
        Budget existing = baseMapper.selectById(dto.getBudgetId());
        if (existing == null) {
            throw new BusinessException("预算不存在");
        }

        Budget updateEntity = new Budget();
        updateEntity.setBudgetId(dto.getBudgetId());

        if (dto.getBudgetAmount() != null) {
            updateEntity.setBudgetAmount(dto.getBudgetAmount());
            // 重新计算差异和差异率
            updateEntity.setVariance(dto.getBudgetAmount() - existing.getActualAmount());
            if (dto.getBudgetAmount() > 0) {
                updateEntity.setVarianceRate(
                        BigDecimal.valueOf(updateEntity.getVariance())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(dto.getBudgetAmount()), 4, RoundingMode.HALF_UP)
                );
            }
        }
        if (dto.getResponsibleDeptId() != null) {
            updateEntity.setResponsibleDeptId(dto.getResponsibleDeptId());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    public IPage<BudgetVO> getPage(BudgetQueryDTO query) {
        Page<Budget> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();

        if (query.getBudgetYear() != null) {
            wrapper.eq(Budget::getBudgetYear, query.getBudgetYear());
        }
        if (query.getBudgetMonth() != null) {
            wrapper.eq(Budget::getBudgetMonth, query.getBudgetMonth());
        } else {
            // 不指定月份时，优先显示年度预算（month为null的）
        }
        if (query.getBudgetType() != null) {
            wrapper.eq(Budget::getBudgetType, query.getBudgetType());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Budget::getCategoryId, query.getCategoryId());
        }
        wrapper.orderByDesc(Budget::getBudgetYear)
               .orderByAsc(Budget::getBudgetMonth);

        IPage<Budget> entityPage = baseMapper.selectPage(page, wrapper);

        Page<BudgetVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<BudgetVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<BudgetVO> result = (IPage<BudgetVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActualAmount(Long budgetId, Long actualAmount) {
        Budget existing = baseMapper.selectById(budgetId);
        if (existing == null) {
            throw new BusinessException("预算不存在");
        }

        Budget updateEntity = new Budget();
        updateEntity.setBudgetId(budgetId);
        updateEntity.setActualAmount(actualAmount != null ? actualAmount : 0L);
        updateEntity.setVariance(existing.getBudgetAmount() - updateEntity.getActualAmount());

        // 计算差异率: (预算-实际)/预算 * 100%
        if (existing.getBudgetAmount() > 0) {
            updateEntity.setVarianceRate(
                    BigDecimal.valueOf(updateEntity.getVariance())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(existing.getBudgetAmount()), 4, RoundingMode.HALF_UP)
            );
        } else {
            updateEntity.setVarianceRate(BigDecimal.ZERO);
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    public BudgetVO getDetail(Long budgetId) {
        Budget entity = baseMapper.selectById(budgetId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    private BudgetVO convertToVO(Budget entity) {
        BudgetVO vo = new BudgetVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
