package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ReceiptConfirmationItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货确认单明细表数据访问接口
 */
@Mapper
public interface ReceiptConfirmationItemMapper extends BaseMapper<ReceiptConfirmationItem> {
}
