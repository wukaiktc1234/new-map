package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.LossOutbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LossOutboundMapper extends BaseMapper<LossOutbound> {

    IPage<LossOutbound> selectPageByCondition(Page<LossOutbound> page,
            @Param("lossNo") String lossNo,
            @Param("status") String status,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
