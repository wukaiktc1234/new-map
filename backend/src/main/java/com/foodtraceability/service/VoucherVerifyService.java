package com.foodtraceability.service;

import com.foodtraceability.dto.BatchVerifyResultDTO;
import com.foodtraceability.dto.InvoiceVerifyResultDTO;
import com.foodtraceability.dto.SignatureVerifyResultDTO;
import com.foodtraceability.entity.ElectronicInvoice;
import com.foodtraceability.entity.ElectronicVoucher;

/**
 * 电子凭证验签验真服务接口
 */
public interface VoucherVerifyService {
    
    /**
     * 验证电子凭证数字签名
     * @param voucher 电子凭证
     * @return 验签结果
     */
    SignatureVerifyResultDTO verifySignature(ElectronicVoucher voucher);
    
    /**
     * 发票验真
     * @param invoice 发票信息
     * @return 验真结果
     */
    InvoiceVerifyResultDTO verifyInvoice(ElectronicInvoice invoice);
    
    /**
     * 批量验签
     * @param voucherIds 凭证ID列表
     * @return 验签结果数量统计
     */
    BatchVerifyResultDTO batchVerifySignature(java.util.List<Long> voucherIds);
    
    /**
     * 批量验真
     * @param voucherIds 凭证ID列表
     * @return 验真结果数量统计
     */
    BatchVerifyResultDTO batchVerifyInvoice(java.util.List<Long> voucherIds);
}
