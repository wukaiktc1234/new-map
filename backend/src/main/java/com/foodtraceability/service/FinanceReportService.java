package com.foodtraceability.service;

import com.foodtraceability.dto.FinanceReportDTO;
import com.foodtraceability.dto.FinanceReportGenerateRequest;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.finance.FinanceReport;
import java.util.List;

/**
 * 财务报表Service (已废弃)
 * @deprecated 已废弃，仅用于兼容旧代码
 */
@Deprecated
public interface FinanceReportService {
    @Deprecated FinanceReport generateReport(String reportType, String reportPeriod, Long generateBy);
    @Deprecated FinanceReport getReportById(Long reportId);
    @Deprecated PageResult<FinanceReport> queryReports(Object query);
    @Deprecated FinanceReportDTO getReportData(Long reportId);
    @Deprecated FinanceReportDTO getReportDataWithDetails(Long reportId);
    @Deprecated List<Object> getReportDetails(Long reportId);
    @Deprecated void saveReportDetails(Long reportId, List<Object> details);
    @Deprecated boolean deleteReport(Long reportId);
}
