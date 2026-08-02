package com.foodtraceability.service.impl;

import com.foodtraceability.dto.FinanceReportDTO;
import com.foodtraceability.dto.FinanceReportGenerateRequest;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.finance.FinanceReport;
import com.foodtraceability.service.FinanceReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Deprecated
@Service("legacyFinanceReportServiceImpl")
public class FinanceReportServiceImpl implements FinanceReportService {
    private static final Logger log = LoggerFactory.getLogger(FinanceReportServiceImpl.class);

    @Override public FinanceReport generateReport(String rt, String rp, Long g) { log.warn("[STUB] no-op"); return null; }
    @Override public FinanceReport getReportById(Long id) { return null; }
    @Override public PageResult<FinanceReport> queryReports(Object q) { return null; }
    @Override public FinanceReportDTO getReportData(Long id) { return null; }
    @Override public FinanceReportDTO getReportDataWithDetails(Long id) { return null; }
    @Override public List<Object> getReportDetails(Long id) { return Collections.emptyList(); }
    @Override public void saveReportDetails(Long id, List<Object> d) {}
    @Override public boolean deleteReport(Long id) { return false; }
}
