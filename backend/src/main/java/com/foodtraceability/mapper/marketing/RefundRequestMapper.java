package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.RefundRequest;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 退款申请 Mapper
 */
@Mapper
@Repository
public interface RefundRequestMapper extends BaseMapper<RefundRequest> {
}
