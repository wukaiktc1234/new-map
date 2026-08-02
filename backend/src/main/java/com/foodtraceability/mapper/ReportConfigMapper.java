package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.report.ReportConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报表用户配置Mapper接口
 * 对应数据库表 report_configs
 */
@Mapper
public interface ReportConfigMapper extends BaseMapper<ReportConfig> {
}
