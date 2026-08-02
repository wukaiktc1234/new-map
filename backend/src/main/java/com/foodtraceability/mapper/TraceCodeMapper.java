package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TraceCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 追溯码Mapper接口
 */
@Mapper
public interface TraceCodeMapper extends BaseMapper<TraceCode> {

    /**
     * 根据追溯码查询
     * @param traceCode 追溯码
     * @return 追溯码实体
     */
    TraceCode selectByTraceCode(@Param("traceCode") String traceCode);

    /**
     * 根据批次号查询追溯码列表
     * @param batchNo 批次号
     * @return 追溯码列表
     */
    List<TraceCode> selectByBatchNo(@Param("batchNo") String batchNo);

    /**
     * 根据供应商ID查询追溯码列表
     * @param supplierId 供应商ID
     * @return 追溯码列表
     */
    List<TraceCode> selectBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 查询即将过期或已过期的追溯码
     * @return 追溯码列表
     */
    List<TraceCode> selectExpiringOrExpired();

    /**
     * 查询高风险追溯码
     * @param riskLevel 风险等级
     * @return 追溯码列表
     */
    List<TraceCode> selectByRiskLevel(@Param("riskLevel") Integer riskLevel);
}
