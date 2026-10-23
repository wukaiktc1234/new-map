package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ReceiptPrintLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货确认单打印日志数据访问接口
 */
@Mapper
public interface ReceiptPrintLogMapper extends BaseMapper<ReceiptPrintLog> {
}
