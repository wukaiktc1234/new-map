package com.foodtraceability.service.trace;

import com.foodtraceability.dto.trace.InspectionVO;
import com.foodtraceability.dto.trace.SupplierBatchVO;
import com.foodtraceability.dto.trace.SupplierTraceVO;

import java.util.List;
import java.util.Map;

/**
 * 供应商追溯服务接口
 * 提供供应商维度的追溯信息聚合查询，包括批次、合格率、召回、检验等
 */
public interface SupplierTraceService {

    /**
     * 供应商追溯汇总（含批次、合格率、召回、检验）
     * @param supplierId 供应商ID
     * @return 供应商追溯汇总信息
     */
    SupplierTraceVO getSupplierTrace(Long supplierId);

    /**
     * 供应商所有原料批次
     * @param supplierId 供应商ID
     * @return 批次列表
     */
    List<SupplierBatchVO> getSupplierBatches(Long supplierId);

    /**
     * 供应商合格率
     * @param supplierId 供应商ID
     * @return 合格率统计信息
     */
    Map<String, Object> getQualityRate(Long supplierId);

    /**
     * 供应商召回记录
     * @param supplierId 供应商ID
     * @return 召回记录列表
     */
    List<Map<String, Object>> getRecalls(Long supplierId);

    /**
     * 供应商检验记录
     * @param supplierId 供应商ID
     * @return 检验记录列表
     */
    List<InspectionVO> getInspections(Long supplierId);

    /**
     * 供应商追溯统计
     * @return 统计结果
     */
    Map<String, Object> getStatistics();
}
