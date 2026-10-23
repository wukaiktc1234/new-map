package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ReceiptSignature;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货确认单签名存证数据访问接口
 */
@Mapper
public interface ReceiptSignatureMapper extends BaseMapper<ReceiptSignature> {
}
