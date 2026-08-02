package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.ElectronicVoucher;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 电子凭证服务接口
 */
public interface ElectronicVoucherService {
    
    /**
     * 上传电子凭证
     */
    ElectronicVoucherUploadResultDTO upload(MultipartFile file);
    
    /**
     * 批量上传电子凭证
     */
    BatchUploadResultDTO batchUpload(MultipartFile[] files);
    
    /**
     * 分页查询电子凭证
     */
    IPage<ElectronicVoucherVO> queryPage(ElectronicVoucherQueryDTO query);
    
    /**
     * 获取电子凭证详情
     */
    ElectronicVoucherDetailVO getDetailById(Long id);
    
    /**
     * 验签
     */
    SignatureVerifyResultDTO verifySignature(Long id);
    
    /**
     * 发票验真
     */
    InvoiceVerifyResultDTO verifyInvoice(Long id);
    
    /**
     * 生成记账凭证
     */
    Long generateVoucher(Long id, VoucherGenerateRequestDTO request);
    
    /**
     * 删除电子凭证
     */
    void delete(Long id);
    
    /**
     * 检查文件是否重复
     */
    boolean checkDuplicate(String fileHash);
    
    /**
     * 下载源文件
     */
    Resource downloadFile(Long id);
    
    /**
     * 获取源文件内容（用于预览）
     */
    String getSourceFileContent(Long id);
    
    /**
     * 批量验签
     */
    BatchVerifyResultDTO batchVerifySignature(java.util.List<Long> ids);
    
    /**
     * 批量验真
     */
    BatchVerifyResultDTO batchVerifyInvoice(java.util.List<Long> ids);
    
    /**
     * 测试二维码提取
     */
    java.util.Map<String, Object> testQrCodeExtraction(MultipartFile file);
    
    /**
     * 带进度的上传电子凭证
     */
    void uploadWithProgress(MultipartFile file, String uploadId);
    
    /**
     * 重新解析电子凭证
     */
    java.util.Map<String, Object> reparse(Long id);
    
    /**
     * 获取工具包状态
     */
    java.util.Map<String, Object> getToolkitStatus();
}
