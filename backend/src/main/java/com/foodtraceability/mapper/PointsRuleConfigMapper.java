package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PointsRuleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 积分规则配置数据访问层
 */
@Mapper
public interface PointsRuleConfigMapper extends BaseMapper<PointsRuleConfig> {

    /**
     * 根据规则键名查询规则
     */
    @Select("SELECT * FROM points_rule_config WHERE rule_key = #{ruleKey} AND status = 1 AND deleted = 0")
    PointsRuleConfig selectByKey(@Param("ruleKey") String ruleKey);

    /**
     * 查询指定分组的所有启用规则
     */
    @Select("SELECT * FROM points_rule_config WHERE rule_group = #{ruleGroup} AND status = 1 AND deleted = 0 ORDER BY rule_id")
    List<PointsRuleConfig> selectByGroup(@Param("ruleGroup") String ruleGroup);

    /**
     * 查询所有启用规则的Map（key-value形式）
     */
    @Select("SELECT rule_key, rule_value FROM points_rule_config WHERE status = 1 AND deleted = 0")
    List<Map<String, String>> selectAllEnabledRules();
}
