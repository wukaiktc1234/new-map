package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryWarningRule;

import java.util.List;

/**
 * 库存预警规则服务接口
 * 定义库存预警规则管理相关的业务方法
 */
public interface InventoryWarningRuleService {
    
    /**
     * 创建库存预警规则
     * 
     * @param rule 库存预警规则实体
     * @return 创建后的库存预警规则
     */
    InventoryWarningRule createWarningRule(InventoryWarningRule rule);
    
    /**
     * 根据ID获取库存预警规则
     * 
     * @param id 规则ID
     * @return 库存预警规则实体
     */
    InventoryWarningRule getWarningRuleById(Long id);
    
    /**
     * 更新库存预警规则
     * 
     * @param id 规则ID
     * @param rule 库存预警规则实体
     * @return 更新后的库存预警规则
     */
    InventoryWarningRule updateWarningRule(Long id, InventoryWarningRule rule);
    
    /**
     * 删除库存预警规则
     * 
     * @param id 规则ID
     */
    void deleteWarningRule(Long id);
    
    /**
     * 分页查询库存预警规则列表
     * 
     * @param page 分页对象
     * @param ruleName 规则名称
     * @param category 产品类别
     * @param warningLevel 预警级别
     * @param enabled 是否启用
     * @return 分页结果
     */
    IPage<InventoryWarningRule> getWarningRulePage(
            Page<InventoryWarningRule> page,
            String ruleName,
            String category,
            Integer warningLevel,
            Integer enabled);
    
    /**
     * 获取所有启用的预警规则
     * 
     * @return 启用的预警规则列表
     */
    List<InventoryWarningRule> getAllEnabledRules();
    
    /**
     * 启用/禁用库存预警规则
     * 
     * @param id 规则ID
     * @param enabled 是否启用（0：禁用，1：启用）
     * @return 更新后的库存预警规则
     */
    InventoryWarningRule toggleRuleStatus(Long id, Integer enabled);
}
