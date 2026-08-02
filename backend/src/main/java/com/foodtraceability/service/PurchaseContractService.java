package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.PurchaseContract;

public interface PurchaseContractService extends IService<PurchaseContract> {
    
    Page<PurchaseContract> getPurchaseContractPage(Page<PurchaseContract> page, String contractNo, Long supplierId, String status, String startDate, String endDate);
    
    PurchaseContract createPurchaseContract(PurchaseContract purchaseContract);
    
    PurchaseContract updatePurchaseContract(Long id, PurchaseContract purchaseContract);
    
    void deletePurchaseContract(Long id);
    
    PurchaseContract getPurchaseContractById(Long id);
    
    PurchaseContract signPurchaseContract(Long id, String signatory);
    
    PurchaseContract terminatePurchaseContract(Long id, String reason);
}
