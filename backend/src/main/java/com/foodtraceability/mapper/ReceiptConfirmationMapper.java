package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ReceiptConfirmation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货确认单主表数据访问接口
 */
@Mapper
public interface ReceiptConfirmationMapper extends BaseMapper<ReceiptConfirmation> {
}
