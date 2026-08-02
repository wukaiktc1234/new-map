package com.foodtraceability.service.impl;

import com.foodtraceability.service.FinancePrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Deprecated
@Service("legacyFinancePrintServiceImpl")
public class FinancePrintServiceImpl implements FinancePrintService {
    private static final Logger log = LoggerFactory.getLogger(FinancePrintServiceImpl.class);

    @Override public void printVoucher(Long id, String p) { log.warn("[STUB] no-op"); }
    @Override public void printInvoice(Long id, String p) { log.warn("[STUB] no-op"); }
    @Override public void printReport(Long id, String p) { log.warn("[STUB] no-op"); }
    @Override public void printFinancialStatement(String rt, String per, String p) { log.warn("[STUB] no-op"); }
}
