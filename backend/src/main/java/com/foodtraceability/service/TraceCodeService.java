package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.TraceCode;
import com.foodtraceability.entity.TraceCodeLog;

import java.util.List;

/**
 * 追溯码服务接口
 * 用于定义追溯码管理的业务逻辑方法
 */
public interface TraceCodeService extends IService<TraceCode> {
    
    /**
     * 批量生成追溯码
     * @param productId 产品ID
     * @param productName 产品名称
     * @param quantity 生成数量
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param batchNumber 批次号
     * @param warehouseId 仓库ID
     * @param warehouseName 仓库名称
     * @return 生成的追溯码列表
     */
    List<TraceCode> generateTraceCodes(Long productId, String productName, Integer quantity, 
                                      String sourceType, String sourceId, String batchNumber,
                                      Long warehouseId, String warehouseName);
    
    /**
     * 扫描追溯码出库
     * @param code 追溯码
     * @param operator 操作人
     * @param operatorId 操作人ID
     * @param location 操作地点
     * @param purpose 使用目的
     * @return 更新后的追溯码对象
     */
    TraceCode scanTraceCodeOutbound(String code, String operator, Long operatorId, 
                                 String location, String purpose);
    
    /**
     * 未拆封退回
     * @param code 追溯码
     * @param operator 操作人
     * @param operatorId 操作人ID
     * @param location 操作地点
     * @param reason 退回原因
     * @return 退回结果
     */
    boolean returnUnopenedTraceCode(String code, String operator, Long operatorId, 
                                   String location, String reason);
    
    /**
     * 查询追溯码信息
     * @param code 追溯码
     * @return 追溯码信息
     */
    TraceCode getTraceCodeByCode(String code);
    
    /**
     * 查询追溯码操作日志
     * @param traceCodeId 追溯码ID
     * @return 操作日志列表
     */
    List<TraceCodeLog> getTraceCodeLogs(String traceCodeId);
    
    /**
     * 追溯码入库
     * @param traceCodeId 追溯码ID
     * @return 入库结果
     */
    boolean inboundTraceCode(String traceCodeId);
    
    /**
     * 批量入库追溯码
     * @param traceCodeIds 追溯码ID列表
     * @return 入库结果
     */
    boolean batchInboundTraceCodes(List<String> traceCodeIds);
    
    /**
     * 查询产品的追溯码列表
     * @param productId 产品ID
     * @param status 追溯码状态
     * @return 追溯码列表
     */
    List<TraceCode> getTraceCodesByProduct(Long productId, String status);
}
