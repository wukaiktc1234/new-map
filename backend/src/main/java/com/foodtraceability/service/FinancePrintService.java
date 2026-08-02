package com.foodtraceability.service;

/**
 * 财务打印Service (已废弃)
 * @deprecated 已废弃，仅用于兼容旧代码
 */
@Deprecated
public interface FinancePrintService {
    @Deprecated void printVoucher(Long voucherId, String printerName);
    @Deprecated void printInvoice(Long invoiceId, String printerName);
    @Deprecated void printReport(Long reportId, String printerName);
    @Deprecated void printFinancialStatement(String reportType, String period, String printerName);
}
