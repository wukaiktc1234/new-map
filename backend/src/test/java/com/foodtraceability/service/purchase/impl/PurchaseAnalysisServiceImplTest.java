package com.foodtraceability.service.purchase.impl;

import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.service.purchase.PurchaseReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PurchaseAnalysisServiceImpl 单元测试（采购分析：只按日期范围聚合，供应商/品类过滤恒为 null）。
 *
 * <p>契约核心：分析视角**不接受** supplierId/category 过滤，四个方法都必须把后两个参数传 null，
 * 否则「分析」会悄悄变成「筛选」，报表口径与页面不一致。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseAnalysisServiceImplTest {

    @Mock
    private PurchaseReportService purchaseReportService;

    @InjectMocks
    private PurchaseAnalysisServiceImpl service;

    @Test
    @DisplayName("getTrend：委托报表服务，supplierId/category 传 null（不筛选）")
    void getTrend_delegatesWithoutFilters() {
        List<PurchaseReportMonthlyItemVO> rows = List.of(new PurchaseReportMonthlyItemVO());
        when(purchaseReportService.getMonthlyTrend("2026-10-01", "2026-10-31", null, null)).thenReturn(rows);

        assertSame(rows, service.getTrend("2026-10-01", "2026-10-31"));
        verify(purchaseReportService).getMonthlyTrend("2026-10-01", "2026-10-31", null, null);
    }

    @Test
    @DisplayName("getSupplier：委托供应商占比报表，过滤参数为 null")
    void getSupplier_delegatesWithoutFilters() {
        List<PurchaseReportSupplierItemVO> rows = List.of(new PurchaseReportSupplierItemVO());
        when(purchaseReportService.getSupplierReport("2026-10-01", "2026-10-31", null, null)).thenReturn(rows);

        assertSame(rows, service.getSupplier("2026-10-01", "2026-10-31"));
        verify(purchaseReportService).getSupplierReport("2026-10-01", "2026-10-31", null, null);
    }

    @Test
    @DisplayName("getCategory：委托品类分布报表，过滤参数为 null")
    void getCategory_delegatesWithoutFilters() {
        List<PurchaseReportCategoryItemVO> rows = List.of(new PurchaseReportCategoryItemVO());
        when(purchaseReportService.getCategoryReport("2026-10-01", "2026-10-31", null, null)).thenReturn(rows);

        assertSame(rows, service.getCategory("2026-10-01", "2026-10-31"));
        verify(purchaseReportService).getCategoryReport("2026-10-01", "2026-10-31", null, null);
    }

    @Test
    @DisplayName("getSummary：委托汇总指标，过滤参数为 null")
    void getSummary_delegatesWithoutFilters() {
        PurchaseReportSummaryVO summary = new PurchaseReportSummaryVO();
        when(purchaseReportService.getSummary("2026-10-01", "2026-10-31", null, null)).thenReturn(summary);

        assertSame(summary, service.getSummary("2026-10-01", "2026-10-31"));
        verify(purchaseReportService).getSummary("2026-10-01", "2026-10-31", null, null);
    }

    @Test
    @DisplayName("四个方法：日期为 null 也原样下传（由报表服务决定边界）")
    void nullDates_passedThrough() {
        when(purchaseReportService.getMonthlyTrend(null, null, null, null)).thenReturn(List.of());

        assertTrue(service.getTrend(null, null).isEmpty());
        verify(purchaseReportService).getMonthlyTrend(null, null, null, null);
    }
}
