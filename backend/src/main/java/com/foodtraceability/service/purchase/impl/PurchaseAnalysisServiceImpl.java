package com.foodtraceability.service.purchase.impl;

import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;
import com.foodtraceability.service.purchase.PurchaseAnalysisService;
import com.foodtraceability.service.purchase.PurchaseReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 采购数据分析服务实现
 *
 * <p>本实现聚焦于"分析视角"，仅按日期范围聚合，不接受 supplierId/category 过滤。
 * 内部委托 {@link PurchaseReportService} 完成实际聚合计算，避免重复实现。</p>
 *
 * <p>设计考量：
 * <ul>
 *   <li>复用报表 Service 的聚合逻辑（DRY 原则）</li>
 *   <li>独立 Service 便于后续扩展不同的分析维度（如按品类细分、按地区分析等）</li>
 *   <li>独立 Controller 路径 /v1/purchase/analysis/* 与报表 /v1/purchase/reports/* 隔离</li>
 * </ul>
 * </p>
 */
@Service
public class PurchaseAnalysisServiceImpl implements PurchaseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseAnalysisServiceImpl.class);

    private final PurchaseReportService purchaseReportService;

    public PurchaseAnalysisServiceImpl(PurchaseReportService purchaseReportService) {
        this.purchaseReportService = purchaseReportService;
    }

    @Override
    public List<PurchaseReportMonthlyItemVO> getTrend(String startDate, String endDate) {
        log.debug("采购趋势分析：startDate={}, endDate={}", startDate, endDate);
        // 委托报表服务，supplierId/category 传 null 表示不限定
        return purchaseReportService.getMonthlyTrend(startDate, endDate, null, null);
    }

    @Override
    public List<PurchaseReportSupplierItemVO> getSupplier(String startDate, String endDate) {
        log.debug("供应商采购占比分析：startDate={}, endDate={}", startDate, endDate);
        return purchaseReportService.getSupplierReport(startDate, endDate, null, null);
    }

    @Override
    public List<PurchaseReportCategoryItemVO> getCategory(String startDate, String endDate) {
        log.debug("品类采购分布分析：startDate={}, endDate={}", startDate, endDate);
        return purchaseReportService.getCategoryReport(startDate, endDate, null, null);
    }

    @Override
    public PurchaseReportSummaryVO getSummary(String startDate, String endDate) {
        log.debug("采购综合汇总指标：startDate={}, endDate={}", startDate, endDate);
        return purchaseReportService.getSummary(startDate, endDate, null, null);
    }
}
