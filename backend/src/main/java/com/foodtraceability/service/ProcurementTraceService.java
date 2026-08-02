package com.foodtraceability.service;

import com.foodtraceability.dto.ProcurementTraceInfoVO;
import com.foodtraceability.dto.ProcurementTraceQueryDTO;

import java.util.List;

/**
 * 采购溯源服务接口
 * 提供采购全链路信息追溯能力
 */
public interface ProcurementTraceService {

    /**
     * 根据物料ID查询采购溯源信息
     * @param materialId 物料ID
     * @return 采购溯源信息列表（按订单分组）
     */
    List<ProcurementTraceInfoVO> getTraceInfoByMaterialId(Long materialId);

    /**
     * 根据采购订单编号查询采购溯源信息
     * @param orderNo 采购订单编号
     * @return 采购溯源信息
     */
    ProcurementTraceInfoVO getTraceInfoByOrderNo(String orderNo);

    /**
     * 根据采购申请编号查询采购溯源信息
     * @param requestNo 采购申请编号
     * @return 采购溯源信息列表（一个申请可能生成多个订单）
     */
    List<ProcurementTraceInfoVO> getTraceInfoByRequestNo(String requestNo);

    /**
     * 根据入库单号查询采购溯源信息
     * @param stockinCode 入库单号
     * @return 采购溯源信息
     */
    ProcurementTraceInfoVO getTraceInfoByStockinCode(String stockinCode);

    /**
     * 根据批次号查询采购溯源信息
     * @param batchNo 批次号
     * @return 采购溯源信息列表
     */
    List<ProcurementTraceInfoVO> getTraceInfoByBatchNo(String batchNo);

    /**
     * 综合查询采购溯源信息
     * @param queryDTO 查询条件
     * @return 采购溯源信息列表
     */
    List<ProcurementTraceInfoVO> searchTraceInfo(ProcurementTraceQueryDTO queryDTO);
}
