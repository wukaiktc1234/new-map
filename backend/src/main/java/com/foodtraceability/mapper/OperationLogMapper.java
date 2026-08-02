package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {

    /**
     * 根据条件查询操作日志列表
     */
    List<OperationLogEntity> selectByConditions(@Param("operationModule") String operationModule,
                                          @Param("operationType") String operationType,
                                          @Param("operatorName") String operatorName,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
