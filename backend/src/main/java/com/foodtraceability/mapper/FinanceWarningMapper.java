package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceWarning;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务风险预警Mapper接口
 * 用于管理财务风险预警信息的数据库操作
 * @author example
 * @since 2025-12-05
 */
@Mapper
public interface FinanceWarningMapper extends BaseMapper<FinanceWarning> {
    // 可以添加自定义查询方法
}