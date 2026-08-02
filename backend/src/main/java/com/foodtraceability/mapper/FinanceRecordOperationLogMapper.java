package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceRecordOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务收支记录操作日志Mapper
 * @author example
 * @since 2025-12-07
 */
@Mapper
public interface FinanceRecordOperationLogMapper extends BaseMapper<FinanceRecordOperationLog> {
    // 继承BaseMapper，包含基本的CRUD方法
}
