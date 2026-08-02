package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ElectronicTrainTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 铁路电子客票Mapper接口
 */
@Mapper
public interface ElectronicTrainTicketMapper extends BaseMapper<ElectronicTrainTicket> {
    
    /**
     * 根据凭证ID查询铁路客票
     */
    ElectronicTrainTicket selectByVoucherId(@Param("voucherId") Long voucherId);
}
