package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务审计日志Mapper接口
 */
@Mapper
public interface FinanceAuditLogMapper extends BaseMapper<FinanceAuditLog> {
}
