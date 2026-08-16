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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预算管理Service实现
 */
@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget>
        implements BudgetService {

    /**
     * finance/Budget 实体映射列中，budgets 表（旧采购预算结构 V20260704_001）缺失的列（实测 9 列）。
     * 结构对齐方向属业务决策（PD-016 待裁决，不猜测）；裁决前作为结构护栏使用。
     */
    private static final List<String> BUDGET_ENTITY_MAPPED_COLUMNS = Arrays.asList(
            "budget_year", "budget_month", "budget_type", "category_id",
            "actual_amount", "variance", "variance_rate", "responsible_dept_id", "remark"
    );

    /**
     * 结构护栏（OICBE-B1-003 / KL-055，防假空）：
     * budgets 表结构与 finance/Budget 实体列漂移（实体 9 列在表中不存在，information_schema 实测），
     * 空表场景下分页 COUNT 短路会返回 code:0 空列表（假空形态）。
     * 在 COUNT 短路前显式校验结构：未对齐则抛明确错误态（code:500），禁止伪装空数据。
     */
    private void assertBudgetSchemaAligned() {
        int present = baseMapper.countEntityColumnsPresent(BUDGET_ENTITY_MAPPED_COLUMNS);
        if (present != BUDGET_ENTITY_MAPPED_COLUMNS.size()) {
            throw new BusinessException(500, "预算表结构与财务预算实体未对齐，预算数据暂不可用（待结构对齐处理）");
        }
    }

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
        // 结构护栏先行（防假空）：结构未对齐时明确错误态，不返回 code:0 空列表
        assertBudgetSchemaAligned();
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
