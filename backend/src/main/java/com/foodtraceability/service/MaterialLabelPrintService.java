package com.foodtraceability.service;

import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.dto.LabelPrintConfigDTO;

public interface MaterialLabelPrintService {
    
    /**
     * 打印原料标签
     * @param traceCode 原料追溯码
     * @return 打印结果
     */
    Boolean printMaterialLabel(MaterialTraceCode traceCode);
    
    /**
     * 剥离模式打印原料标签（即打即贴）
     * @param traceCode 原料追溯码
     * @param config 打印配置
     * @return 打印结果
     */
    Boolean printMaterialLabelWithPeelMode(MaterialTraceCode traceCode, LabelPrintConfigDTO config);
    
    /**
     * 批量打印原料标签
     * @param traceCodes 原料追溯码列表
     * @return 打印结果
     */
    Boolean batchPrintMaterialLabels(java.util.List<MaterialTraceCode> traceCodes);
    
    /**
     * 剥离模式批量打印原料标签（即打即贴）
     * @param traceCodes 原料追溯码列表
     * @param config 打印配置
     * @return 打印结果
     */
    Boolean batchPrintMaterialLabelsWithPeelMode(java.util.List<MaterialTraceCode> traceCodes, LabelPrintConfigDTO config);
    
    /**
     * 生成标签打印数据（ZPL格式）
     * @param traceCode 原料追溯码
     * @return ZPL打印数据
     */
    String generateZPLData(MaterialTraceCode traceCode);
    
    /**
     * 生成剥离模式标签打印数据（ZPL格式）
     * @param traceCode 原料追溯码
     * @param config 打印配置
     * @return ZPL打印数据
     */
    String generateZPLDataWithPeelMode(MaterialTraceCode traceCode, LabelPrintConfigDTO config);
    
    /**
     * 生成标签打印数据（TSPL格式）
     * @param traceCode 原料追溯码
     * @return TSPL打印数据
     */
    String generateTSPLData(MaterialTraceCode traceCode);
    
    /**
     * 生成剥离模式标签打印数据（TSPL格式）
     * @param traceCode 原料追溯码
     * @param config 打印配置
     * @return TSPL打印数据
     */
    String generateTSPLDataWithPeelMode(MaterialTraceCode traceCode, LabelPrintConfigDTO config);
    
    /**
     * 生成HTML标签预览
     * @param traceCode 原料追溯码
     * @return HTML内容
     */
    String generateHTMLPreview(MaterialTraceCode traceCode);
}
