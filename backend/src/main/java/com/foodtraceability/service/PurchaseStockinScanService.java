package com.foodtraceability.service;

import com.foodtraceability.dto.*;
import com.foodtraceability.entity.MaterialTraceCode;
import java.util.List;

/**
 * 采购入库扫描服务接口
 */
public interface PurchaseStockinScanService {
    
    /**
     * 扫描追溯码确认入库
     * @param scanDTO 扫描数据
     * @return 扫描结果
     */
    ScanConfirmResult scanConfirmStockin(PurchaseStockinScanDTO scanDTO);
    
    /**
     * 批量扫描确认入库
     * @param scanDTOs 扫描数据列表
     * @return 扫描结果列表
     */
    List<ScanConfirmResult> batchScanConfirm(List<PurchaseStockinScanDTO> scanDTOs);
    
    /**
     * 获取入库进度
     * @param stockinId 入库单ID
     * @return 入库进度
     */
    StockinProgressDTO getStockinProgress(Long stockinId);
    
    /**
     * 处理人工介入
     * @param intervention 人工介入数据
     * @return 处理结果
     */
    ScanConfirmResult handleManualIntervention(ManualInterventionDTO intervention);
    
    /**
     * 获取待处理的介入任务
     * @param stockinId 入库单ID
     * @return 介入任务列表
     */
    List<ManualInterventionDTO> getPendingInterventions(Long stockinId);
    
    /**
     * 取消入库单
     * @param stockinId 入库单ID
     * @param reason 取消原因
     * @return 操作结果
     */
    boolean cancelStockin(Long stockinId, String reason);
    
    /**
     * 完成入库单
     * @param stockinId 入库单ID
     * @return 操作结果
     */
    boolean completeStockin(Long stockinId);
    
    /**
     * 生成追溯码并打印标签
     * @param stockinId 入库单ID
     * @param printConfig 打印配置
     * @return 生成的追溯码列表
     */
    List<MaterialTraceCode> generateAndPrintLabels(Long stockinId, LabelPrintConfigDTO printConfig);
}
