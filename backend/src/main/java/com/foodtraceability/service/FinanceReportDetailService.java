package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.finance.FinanceReportDetail;
import com.foodtraceability.dto.FinanceReportItemDTO;
import java.util.List;

public interface FinanceReportDetailService extends IService<FinanceReportDetail> {
    
    List<FinanceReportItemDTO> getReportDetails(Long reportId);
    
    List<FinanceReportItemDTO> getReportDetailsByType(Long reportId, String itemType);
    
    void saveReportDetails(Long reportId, List<FinanceReportItemDTO> details);
    
    void deleteReportDetails(Long reportId);
}
