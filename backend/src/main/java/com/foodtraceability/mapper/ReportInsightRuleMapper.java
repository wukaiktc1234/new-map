package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.report.ReportInsightRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能洞察规则Mapper接口
 * 对应数据库表 report_insight_rules
 */
@Mapper
public interface ReportInsightRuleMapper extends BaseMapper<ReportInsightRule> {
}
