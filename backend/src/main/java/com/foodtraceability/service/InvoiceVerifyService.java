package com.foodtraceability.service;

import com.foodtraceability.dto.InvoiceVerifyRequestDTO;
import com.foodtraceability.dto.InvoiceVerifyResultDTO;

/**
 * 发票验真服务接口
 */
public interface InvoiceVerifyService {
    
    /**
     * 验真发票
     * @param request 验真请求
     * @return 验真结果
     */
    InvoiceVerifyResultDTO verify(InvoiceVerifyRequestDTO request);
    
    /**
     * 检查验真服务是否可用
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取服务名称
     * @return 服务名称
     */
    String getServiceName();
}
