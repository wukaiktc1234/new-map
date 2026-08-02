package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ExpiryWarningRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 临期预警规则Mapper接口
 */
@Mapper
public interface ExpiryWarningRuleMapper extends BaseMapper<ExpiryWarningRule> {

    /**
     * 根据预警类型查询启用的规则
     * @param warningType 预警类型
     * @return 规则列表
     */
    List<ExpiryWarningRule> selectEnabledByType(@Param("warningType") Integer warningType);

    /**
     * 查询所有启用的规则
     * @return 规则列表
     */
    List<ExpiryWarningRule> selectAllEnabled();
}
