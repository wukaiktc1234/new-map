package com.foodtraceability.service.finance;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 税务计算服务接口
 * <p>
 * 提供增值税、所得税的自动计算，纳税申报表生成等功能。
 * 基于门店的采购和销售数据进行税费计算。
 * </p>
 */
public interface TaxCalculationService {

    /**
     * 计算增值税
     * <p>
     * 增值税 = 销项税 - 进项税
     * 销项税基于当期销售收入计算，进项税基于当期采购支出计算。
     * </p>
     *
     * @param storeId 门店ID
     * @param year    年份，如 "2026"
     * @param month   月份，如 "06"
     * @return 增值税计算结果，包含销项税、进项税、应纳增值税等明细
     */
    Map<String, Object> calculateVAT(Long storeId, String year, String month);

    /**
     * 计算所得税
     * <p>
     * 所得税 = 利润 × 税率
     * 利润基于当期收入与成本费用计算。
     * </p>
     *
     * @param storeId 门店ID
     * @param year    年份，如 "2026"
     * @param month   月份，如 "06"
     * @return 所得税计算结果，包含收入、成本、利润、税率、应纳所得税等明细
     */
    Map<String, Object> calculateIncomeTax(Long storeId, String year, String month);

    /**
     * 生成纳税申报表
     * <p>
     * 根据税种和期间生成结构化的纳税申报表数据。
     * </p>
     *
     * @param storeId 门店ID
     * @param taxType 税种类型：VAT（增值税）、INCOME_TAX（所得税）
     * @param period  纳税期间，格式：YYYYMM
     * @return 纳税申报表数据
     */
    Map<String, Object> generateTaxReturn(Long storeId, String taxType, String period);

    /**
     * 获取纳税申报表详情
     *
     * @param returnId 申报表ID
     * @return 申报表详情数据
     */
    Map<String, Object> getTaxReturnDetail(Long returnId);

    /**
     * 获取纳税申报表列表
     *
     * @param storeId 门店ID（可选，null 表示查询所有门店）
     * @param taxType 税种类型（可选，null 表示查询所有税种）
     * @return 申报表列表数据，包含 records 和 total 字段
     */
    Map<String, Object> getTaxReturnList(Long storeId, String taxType);
}
