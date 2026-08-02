package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.ScheduleRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 排班规则配置Mapper接口
 */
@Mapper
public interface ScheduleRuleMapper extends BaseMapper<ScheduleRule> {

    /**
     * 查询门店的所有规则(含停用)
     * @param storeId 门店ID
     * @return 规则列表
     */
    List<ScheduleRule> selectAllByStoreId(Long storeId);

    /**
     * 查询门店的启用规则
     * @param storeId 门店ID
     * @return 启用的规则列表
     */
    List<ScheduleRule> selectActiveByStoreId(Long storeId);

    /**
     * 查询门店的系统预置规则
     * @param storeId 门店ID
     * @return 系统预置规则列表
     */
    List<ScheduleRule> selectSystemRulesByStoreId(Long storeId);

    /**
     * 按分类查询门店规则
     * @param storeId 门店ID
     * @param category 规则分类(hard_constraint/soft_constraint)
     * @return 规则列表
     */
    List<ScheduleRule> selectByStoreAndCategory(
            Long storeId,
            String category);

    /**
     * 根据规则编码查询
     * @param storeId 门店ID
     * @param ruleCode 规则编码
     * @return 规则
     */
    ScheduleRule selectByStoreAndCode(
            Long storeId,
            String ruleCode);

    /**
     * 批量查询规则(按规则ID列表)
     * @param ruleIds 规则ID列表
     * @return 规则列表
     */
    List<ScheduleRule> selectByIds(List<String> ruleIds);
}
