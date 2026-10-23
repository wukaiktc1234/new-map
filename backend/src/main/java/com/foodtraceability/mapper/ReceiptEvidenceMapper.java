package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ReceiptEvidence;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货留证照片表数据访问接口
 */
@Mapper
public interface ReceiptEvidenceMapper extends BaseMapper<ReceiptEvidence> {
}
