package com.foodtraceability.service;

import com.foodtraceability.dto.SignatureVerifyResultDTO;
import com.foodtraceability.entity.ElectronicVoucher;

/**
 * 电子凭证签名验证服务接口
 * 
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 
 * 支持验证：
 * 1. XML-DSig签名（XML文件）
 * 2. PDF数字签名（PDF文件）
 * 3. OFD电子签章（OFD文件）
 */
public interface SignatureVerifyService {
    
    /**
     * 验证电子凭证数字签名
     * 
     * @param voucher 电子凭证
     * @return 验签结果
     */
    SignatureVerifyResultDTO verifySignature(ElectronicVoucher voucher);
    
    /**
     * 从PDF字节验证签名
     * 
     * @param pdfContent PDF文件字节内容
     * @return 验签结果
     */
    SignatureVerifyResultDTO verifyPdfSignatureFromBytes(byte[] pdfContent);
}
