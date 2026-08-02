package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AutoVoucherLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动凭证日志Mapper
 * 用于操作auto_voucher_log表，提供凭证处理日志的查询和记录功能
 * 支持审计追溯和问题排查
 * @author example
 * @since 2026-04-04
 */
@Mapper
public interface AutoVoucherLogMapper extends BaseMapper<AutoVoucherLog> {
    
    // 继承BaseMapper后自动具备CRUD基础能力
    // 可根据需要添加自定义查询方法
}
