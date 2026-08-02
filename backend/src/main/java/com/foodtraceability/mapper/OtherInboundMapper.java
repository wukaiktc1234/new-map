package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.OtherInbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OtherInboundMapper extends BaseMapper<OtherInbound> {
    
    IPage<OtherInbound> selectPageByCondition(
        Page<OtherInbound> page,
        @Param("inboundType") String inboundType,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );
}
