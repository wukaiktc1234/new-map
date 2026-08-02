package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryWarningRule;
import org.springframework.stereotype.Repository;

/**
 * 预警规则Mapper接口
 * 提供预警规则相关的数据库操作
 */
@Repository
public interface InventoryWarningRuleMapper extends BaseMapper<InventoryWarningRule> {
}
