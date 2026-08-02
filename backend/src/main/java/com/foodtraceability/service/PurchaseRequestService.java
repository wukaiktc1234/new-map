package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PurchaseRequestCreateDTO;
import com.foodtraceability.dto.PurchaseRequestDTO;
import com.foodtraceability.dto.PurchaseRequestUpdateDTO;
import com.foodtraceability.entity.PurchaseRequest;

import java.util.List;

public interface PurchaseRequestService extends IService<PurchaseRequest> {
    
    IPage<PurchaseRequestDTO> getPage(int page, int size, String requestNo, String status,
                                       String departmentId, String applicantId,
                                       String departmentName, String applicantName,
                                       String startDate, String endDate);
    
    PurchaseRequestDTO getById(String requestId);
    
    PurchaseRequestDTO create(PurchaseRequestCreateDTO dto);
    
    PurchaseRequestDTO update(PurchaseRequestUpdateDTO dto);
    
    void delete(String requestId);
    
    void submit(String requestId);
    
    void approve(String requestId, String status, String remark);

    /** 重置驳回次数（管理员解锁被限制的申请） */
    void resetRejectCount(String requestId);
    
    PurchaseRequestDTO generateOrder(String requestId);
    
    List<PurchaseRequestDTO> getByStatus(String status);
    
    int countByStatus(String status);
}
