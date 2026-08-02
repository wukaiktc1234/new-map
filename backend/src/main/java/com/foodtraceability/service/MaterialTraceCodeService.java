package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.MaterialTraceCodeGenerateDTO;
import com.foodtraceability.entity.MaterialTraceCode;

import java.util.List;

/**
 * 原料追溯码服务接口
 */
public interface MaterialTraceCodeService extends IService<MaterialTraceCode> {

    /**
     * 批量生成原料追溯码
     */
    List<MaterialTraceCode> generateBatch(MaterialTraceCodeGenerateDTO dto);

    /**
     * 根据追溯码查询
     */
    MaterialTraceCode getByTraceCode(String traceCode);

    /**
     * 根据采购入库单ID查询
     */
    List<MaterialTraceCode> getByStockinId(Long stockinId);

    /**
     * 根据商品ID查询
     */
    List<MaterialTraceCode> getByProductId(Long productId);

    /**
     * 更新追溯码状态
     */
    boolean updateStatus(String traceCodeId, String newStatus, String operatorName, String reason);

    /**
     * 领用追溯码
     */
    boolean pickTraceCode(String traceCode, Long usedById, String usedByName, String usagePurpose);

    /**
     * 使用追溯码
     */
    boolean useTraceCode(String traceCode, String foodTraceCode, Long usedById, String usedByName);

    /**
     * 退回追溯码
     */
    boolean returnTraceCode(String traceCode, String reason);

    /**
     * 获取过期追溯码
     */
    List<MaterialTraceCode> getExpiredCodes();

    /**
     * 获取即将过期追溯码
     */
    List<MaterialTraceCode> getExpiringSoonCodes(int days);

    /**
     * 标记过期追溯码
     */
    int markExpiredCodes();

    /**
     * 生成二维码
     */
    String generateQrCode(String traceCodeId);

    /**
     * 批量打印追溯码
     */
    boolean batchPrint(List<String> traceCodeIds, Long printerId);

    /**
     * 统计各状态数量
     */
    int countByStatus(String status);

    /**
     * 扫码确认入库
     * 将待入库状态的追溯码确认为已入库，同时更新库存和创建采购入库记录
     * @param traceCode 追溯码
     * @param operatorName 操作人
     * @return 是否成功
     */
    boolean confirmStockin(String traceCode, String operatorName);

    /**
     * 批量扫码确认入库
     * @param traceCodes 追溯码列表
     * @param operatorName 操作人
     * @return 成功数量
     */
    int batchConfirmStockin(List<String> traceCodes, String operatorName);
}
