package com.foodtraceability.service.finance.impl;

import com.foodtraceability.service.finance.TaxCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 税务计算服务实现
 * <p>
 * 提供基于模拟数据的税费计算和纳税申报表生成。
 * 后续对接 PostgreSQL 后，将从采购表、销售表等获取真实业务数据。
 * </p>
 */
@Service
public class TaxCalculationServiceImpl implements TaxCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationServiceImpl.class);

    /** 增值税标准税率 13% */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.13");

    /** 企业所得税标准税率 25% */
    private static final BigDecimal INCOME_TAX_RATE = new BigDecimal("0.25");

    /** 小规模纳税人增值税征收率 3% */
    private static final BigDecimal SMALL_SCALE_VAT_RATE = new BigDecimal("0.03");

    @Override
    public Map<String, Object> calculateVAT(Long storeId, String year, String month) {
        log.info("计算增值税 - 门店ID: {}, 期间: {}-{}", storeId, year, month);

        // 模拟数据：实际应从采购单和销售单中汇总
        // 销项税 = 销售收入 × 税率
        BigDecimal salesRevenue = new BigDecimal("158000.00");
        BigDecimal outputTax = salesRevenue.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);

        // 进项税 = 采购支出 × 税率
        BigDecimal purchaseAmount = new BigDecimal("89500.00");
        BigDecimal inputTax = purchaseAmount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);

        // 应纳增值税 = 销项税 - 进项税
        BigDecimal vatPayable = outputTax.subtract(inputTax).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storeId", storeId);
        result.put("year", year);
        result.put("month", month);
        result.put("taxType", "VAT");
        result.put("taxTypeName", "增值税");
        result.put("salesRevenue", salesRevenue);
        result.put("outputTaxRate", VAT_RATE);
        result.put("outputTax", outputTax);
        result.put("purchaseAmount", purchaseAmount);
        result.put("inputTaxRate", VAT_RATE);
        result.put("inputTax", inputTax);
        result.put("vatPayable", vatPayable);
        result.put("calculationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        log.info("增值税计算完成 - 销项税: {}, 进项税: {}, 应纳增值税: {}", outputTax, inputTax, vatPayable);
        return result;
    }

    @Override
    public Map<String, Object> calculateIncomeTax(Long storeId, String year, String month) {
        log.info("计算所得税 - 门店ID: {}, 期间: {}-{}", storeId, year, month);

        // 模拟数据：实际应从财务数据中汇总
        BigDecimal totalRevenue = new BigDecimal("158000.00");
        BigDecimal totalCost = new BigDecimal("89500.00");
        BigDecimal operatingExpenses = new BigDecimal("28500.00");

        // 利润 = 收入 - 成本 - 费用
        BigDecimal profit = totalRevenue.subtract(totalCost).subtract(operatingExpenses)
                .setScale(2, RoundingMode.HALF_UP);

        // 应纳所得税 = 利润 × 税率
        BigDecimal incomeTaxPayable = profit.multiply(INCOME_TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storeId", storeId);
        result.put("year", year);
        result.put("month", month);
        result.put("taxType", "INCOME_TAX");
        result.put("taxTypeName", "企业所得税");
        result.put("totalRevenue", totalRevenue);
        result.put("totalCost", totalCost);
        result.put("operatingExpenses", operatingExpenses);
        result.put("profit", profit);
        result.put("taxRate", INCOME_TAX_RATE);
        result.put("incomeTaxPayable", incomeTaxPayable);
        result.put("calculationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        log.info("所得税计算完成 - 利润: {}, 税率: {}, 应纳所得税: {}", profit, INCOME_TAX_RATE, incomeTaxPayable);
        return result;
    }

    @Override
    public Map<String, Object> generateTaxReturn(Long storeId, String taxType, String period) {
        log.info("生成纳税申报表 - 门店ID: {}, 税种: {}, 期间: {}", storeId, taxType, period);

        // 根据税种计算税款
        String year = period.substring(0, 4);
        String month = period.substring(4, 6);
        Map<String, Object> taxCalculation;

        if ("VAT".equals(taxType)) {
            taxCalculation = calculateVAT(storeId, year, month);
        } else if ("INCOME_TAX".equals(taxType)) {
            taxCalculation = calculateIncomeTax(storeId, year, month);
        } else {
            throw new IllegalArgumentException("不支持的税种类型: " + taxType);
        }

        // 生成申报表编号
        String returnNo = "TAX-" + period + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> taxReturn = new LinkedHashMap<>();
        taxReturn.put("returnId", new Random().nextLong(100000));
        taxReturn.put("returnNo", returnNo);
        taxReturn.put("storeId", storeId);
        taxReturn.put("taxType", taxType);
        taxReturn.put("period", period);
        taxReturn.put("status", "DRAFT");
        taxReturn.put("statusName", "草稿");
        taxReturn.put("taxCalculation", taxCalculation);
        taxReturn.put("totalTaxPayable", taxCalculation.get("vatPayable") != null
                ? taxCalculation.get("vatPayable")
                : taxCalculation.get("incomeTaxPayable"));
        taxReturn.put("generateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        taxReturn.put("submissionStatus", "UNSUBMITTED");
        taxReturn.put("submissionStatusName", "未提交");

        log.info("纳税申报表生成完成 - 编号: {}, 税种: {}, 期间: {}", returnNo, taxType, period);
        return taxReturn;
    }

    @Override
    public Map<String, Object> getTaxReturnDetail(Long returnId) {
        log.info("获取纳税申报表详情 - 申报表ID: {}", returnId);

        // 模拟数据：实际应从数据库查询
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("returnId", returnId);
        detail.put("returnNo", "TAX-202606-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        detail.put("storeId", 1L);
        detail.put("storeName", "示范门店");
        detail.put("taxType", "VAT");
        detail.put("taxTypeName", "增值税");
        detail.put("period", "202606");
        detail.put("status", "DRAFT");
        detail.put("statusName", "草稿");

        // 申报表明细
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("salesRevenue", new BigDecimal("158000.00"));
        details.put("outputTaxRate", "13%");
        details.put("outputTax", new BigDecimal("20540.00"));
        details.put("purchaseAmount", new BigDecimal("89500.00"));
        details.put("inputTaxRate", "13%");
        details.put("inputTax", new BigDecimal("11635.00"));
        details.put("vatPayable", new BigDecimal("8905.00"));
        detail.put("details", details);

        detail.put("totalTaxPayable", new BigDecimal("8905.00"));
        detail.put("generateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        detail.put("submissionStatus", "UNSUBMITTED");
        detail.put("submissionStatusName", "未提交");

        return detail;
    }

    @Override
    public Map<String, Object> getTaxReturnList(Long storeId, String taxType) {
        log.info("获取纳税申报表列表 - 门店ID: {}, 税种: {}", storeId, taxType);
        // TODO: 对接真实数据源（tax_records 表）后，按 storeId 和 taxType 查询申报表记录
        // 当前返回空列表，避免返回硬编码 mock 数据
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", Collections.emptyList());
        result.put("total", 0);
        return result;
    }
}
