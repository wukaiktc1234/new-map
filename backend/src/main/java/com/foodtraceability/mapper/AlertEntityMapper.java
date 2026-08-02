package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.AlertEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertEntityMapper extends BaseMapper<AlertEntity> {

    IPage<AlertEntity> selectAlertPage(Page<AlertEntity> page,
                                        @Param("severity") String severity,
                                        @Param("status") String status,
                                        @Param("source") String source);

    List<AlertEntity> selectActiveAlerts();

    long countBySeverity(@Param("severity") String severity);

    long countByStatus(@Param("status") String status);
}
