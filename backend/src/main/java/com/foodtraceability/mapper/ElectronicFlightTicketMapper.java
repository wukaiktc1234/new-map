package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ElectronicFlightTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 航空电子客票Mapper接口
 */
@Mapper
public interface ElectronicFlightTicketMapper extends BaseMapper<ElectronicFlightTicket> {
    
    /**
     * 根据凭证ID查询航空客票
     */
    ElectronicFlightTicket selectByVoucherId(@Param("voucherId") Long voucherId);
}
