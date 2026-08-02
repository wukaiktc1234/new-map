package com.foodtraceability.service;

import java.util.List;
import java.util.Map;

/**
 * 标签打印服务接口
 * 专门用于 TSPL 标签打印机
 */
public interface LabelPrintService {

    /**
     * 打印追溯码标签（简化版）
     *
     * @param traceCode    追溯码
     * @param materialName 物料名称
     * @return 打印是否成功
     */
    boolean printTraceabilityLabel(String traceCode, String materialName);

    /**
     * 打印追溯码标签（完整版）
     *
     * @param traceCode    追溯码
     * @param materialName 物料名称
     * @param batchNo      批次号
     * @param expiryDate   到期日期
     * @param supplierName 供应商名称
     * @param storeName    门店名称
     * @return 打印是否成功
     */
    boolean printTraceabilityLabel(String traceCode, String materialName,
                                   String batchNo, String expiryDate,
                                   String supplierName, String storeName);

    /**
     * 批量打印标签
     *
     * @param labels 标签列表，每个标签包含：
     *               - traceCode: 追溯码
     *               - materialName: 物料名称
     *               - batchNo: 批次号
     *               - expiryDate: 到期日期
     *               - supplierName: 供应商名称
     *               - storeName: 门店名称
     * @return 打印是否成功
     */
    boolean batchPrintLabels(List<Map<String, Object>> labels);

    /**
     * 测试打印
     *
     * @return 打印是否成功
     */
    boolean testPrint();

    /**
     * 设置打印机配置
     *
     * @param labelWidth  标签宽度（mm）
     * @param labelHeight 标签高度（mm）
     * @param gapSize     标签间隙（mm）
     * @param speed       打印速度（1-10）
     * @param density     打印浓度（1-15）
     * @return 设置是否成功
     */
    boolean setPrinterConfig(int labelWidth, int labelHeight, int gapSize, int speed, int density);

    /**
     * 获取打印机状态
     *
     * @return 打印机状态信息
     */
    Map<String, Object> getPrinterStatus();

    /**
     * 设置打印机名称
     *
     * @param printerName 打印机名称
     */
    void setPrinterName(String printerName);

    /**
     * 打印自定义标签（使用 TSPL 指令）
     *
     * @param tsplData TSPL 指令字符串
     * @return 打印是否成功
     */
    boolean printCustomLabel(String tsplData);
}
