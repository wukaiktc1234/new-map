package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.ElectronicContract;

public interface ElectronicContractService extends IService<ElectronicContract> {
    
    Page<ElectronicContract> getElectronicContractPage(Page<ElectronicContract> page, String contractNo, Long supplierId, String status, String startDate, String endDate);
    
    ElectronicContract createElectronicContract(ElectronicContract electronicContract);
    
    ElectronicContract updateElectronicContract(Long id, ElectronicContract electronicContract);
    
    void deleteElectronicContract(Long id);
    
    ElectronicContract getElectronicContractById(Long id);
    
    ElectronicContract sendForSign(Long id);

    /**
     * 签署电子合同
     * @param id 电子合同ID
     * @param sealId 签署使用的印章ID（关联 seals.seal_id，可为空）
     * @return 签署后的电子合同
     */
    ElectronicContract signElectronicContract(Long id, String sealId);

    ElectronicContract cancelElectronicContract(Long id, String reason);
}
