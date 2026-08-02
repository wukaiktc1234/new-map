package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.FinanceInvoice;
import java.util.List;

public interface FinanceInvoiceService extends IService<FinanceInvoice> {
    
    List<FinanceInvoice> getInvoicesByBusinessId(Long businessId);
    
    List<FinanceInvoice> getInvoicesByBusinessType(String businessType);
    
    List<FinanceInvoice> getInvoicesByDateRange(String startDate, String endDate);
    
    FinanceInvoice createInvoice(FinanceInvoice invoice);
    
    void updateInvoiceStatus(Long id, String status);
}
