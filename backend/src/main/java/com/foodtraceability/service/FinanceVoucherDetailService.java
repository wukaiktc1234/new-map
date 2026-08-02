package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.FinanceVoucherDetail;
import com.foodtraceability.dto.FinanceVoucherDetailDTO;
import java.util.List;

public interface FinanceVoucherDetailService extends IService<FinanceVoucherDetail> {
    
    List<FinanceVoucherDetailDTO> getVoucherDetails(Long voucherId);
    
    void saveVoucherDetails(Long voucherId, List<FinanceVoucherDetailDTO> details);
    
    void deleteVoucherDetails(Long voucherId);
}
