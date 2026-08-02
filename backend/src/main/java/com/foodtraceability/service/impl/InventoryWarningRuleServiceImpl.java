package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.InventoryWarningRule;
import com.foodtraceability.mapper.InventoryWarningRuleMapper;
import com.foodtraceability.service.InventoryWarningRuleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预警规则服务实现类
 * 实现库存预警规则管理相关的业务方法
 */
@Service
public class InventoryWarningRuleServiceImpl extends ServiceImpl<InventoryWarningRuleMapper, InventoryWarningRule> implements InventoryWarningRuleService {


    public InventoryWarningRuleServiceImpl(InventoryWarningRuleMapper warningRuleMapper) {
        this.warningRuleMapper = warningRuleMapper;
    }

    private final InventoryWarningRuleMapper warningRuleMapper;

    @Override
    public InventoryWarningRule createWarningRule(InventoryWarningRule rule) {
        LocalDateTime now = LocalDateTime.now();
        // 修复：使用实体类实际字段名 createTime/updateTime（原代码使用了不存在的 createdAt/updatedAt）
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        this.save(rule);
        return rule;
    }

    @Override
    public InventoryWarningRule getWarningRuleById(Long id) {
        return this.getById(id);
    }

    @Override
    public InventoryWarningRule updateWarningRule(Long id, InventoryWarningRule rule) {
        // 修复：使用实体类实际主键字段名 ruleId（原代码使用了不存在的 setId）
        rule.setRuleId(id);
        // 修复：使用实体类实际字段名 updateTime（原代码使用了不存在的 setUpdatedAt）
        rule.setUpdateTime(LocalDateTime.now());
        this.updateById(rule);
        return this.getById(id);
    }

    @Override
    public void deleteWarningRule(Long id) {
        this.removeById(id);
    }

    @Override
    public IPage<InventoryWarningRule> getWarningRulePage(
            Page<InventoryWarningRule> page,
            String ruleName,
            String category,
            Integer warningLevel,
            Integer enabled) {
        // 修复：改用标准的MyBatis Plus QueryWrapper分页查询（原代码调用了Mapper中不存在的自定义方法 selectWarningRulePage）
        QueryWrapper<InventoryWarningRule> wrapper = new QueryWrapper<>();

        // 按规则名称模糊查询
        if (StringUtils.hasText(ruleName)) {
            wrapper.like("rule_name", ruleName);
        }

        // 按预警类型查询（warningLevel参数映射到warningType字段）
        if (warningLevel != null) {
            wrapper.eq("warning_type", warningLevel);
        }

        // 按启用状态查询（Integer转Boolean：1→true, 0→false）
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled == 1);
        }

        // 注意：category参数在当前实体中无对应字段，如需支持可扩展conditionField查询

        return this.page(page, wrapper);
    }

    @Override
    public List<InventoryWarningRule> getAllEnabledRules() {
        QueryWrapper<InventoryWarningRule> wrapper = new QueryWrapper<>();
        // 修复：使用实体类实际字段名 is_enabled 和Boolean值（原代码使用了不存在的 enabled 字段和整数值）
        wrapper.eq("is_enabled", true);
        return this.list(wrapper);
    }

    @Override
    public InventoryWarningRule toggleRuleStatus(Long id, Integer enabled) {
        InventoryWarningRule rule = this.getById(id);
        if (rule != null) {
            // 修复：使用实体类实际字段名 setIsEnabled(Boolean)（原代码使用了不存在的 setEnabled(Integer)）
            rule.setIsEnabled(enabled == 1);
            // 修复：使用实体类实际字段名 updateTime（原代码使用了不存在的 setUpdatedAt）
            rule.setUpdateTime(LocalDateTime.now());
            this.updateById(rule);
        }
        return rule;
    }
}
