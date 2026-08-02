package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SysConfigHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysConfigHistoryMapper extends BaseMapper<SysConfigHistory> {

    IPage<SysConfigHistory> selectHistoryPage(
        Page<SysConfigHistory> page,
        @Param("configId") Long configId,
        @Param("operatorId") String operatorId,
        @Param("changeType") String changeType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
