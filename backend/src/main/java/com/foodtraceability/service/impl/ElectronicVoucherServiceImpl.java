package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.*;
import com.foodtraceability.service.ElectronicVoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 电子凭证服务 - Stub实现（兼容性适配）
 * 
 * @deprecated 此类为临时兼容性实现，请使用 finance 子包中的新实现
 */
@Deprecated
@Service("legacyElectronicVoucherServiceImpl")
public class ElectronicVoucherServiceImpl implements ElectronicVoucherService {
    
    private static final Logger log = LoggerFactory.getLogger(ElectronicVoucherServiceImpl.class);

    @Override
    public ElectronicVoucherUploadResultDTO upload(MultipartFile file) {
        log.warn("[STUB] upload() called - returning null");
        return null;
    }

    @Override
    public BatchUploadResultDTO batchUpload(MultipartFile[] files) {
        log.warn("[STUB] batchUpload() called - returning null");
        return null;
    }

    @Override
    public IPage<ElectronicVoucherVO> queryPage(ElectronicVoucherQueryDTO query) {
        log.warn("[STUB] queryPage() called - returning null");
        return null;
    }

    @Override
    public ElectronicVoucherDetailVO getDetailById(Long id) {
        log.warn("[STUB] getDetailById({}) called - returning null", id);
        return null;
    }

    @Override
    public SignatureVerifyResultDTO verifySignature(Long id) {
        log.warn("[STUB] verifySignature({}) called - returning null", id);
        return null;
    }

    @Override
    public InvoiceVerifyResultDTO verifyInvoice(Long id) {
        log.warn("[STUB] verifyInvoice({}) called - returning null", id);
        return null;
    }

    @Override
    public Long generateVoucher(Long id, VoucherGenerateRequestDTO request) {
        log.warn("[STUB] generateVoucher({}, {}) called - returning null", id, request);
        return null;
    }

    @Override
    public void delete(Long id) {
        log.warn("[STUB] delete({}) called - no-op", id);
    }

    @Override
    public boolean checkDuplicate(String fileHash) {
        log.warn("[STUB] checkDuplicate({}) called - returning false", fileHash);
        return false;
    }

    @Override
    public Resource downloadFile(Long id) {
        log.warn("[STUB] downloadFile({}) called - returning null", id);
        return null;
    }

    @Override
    public String getSourceFileContent(Long id) {
        log.warn("[STUB] getSourceFileContent({}) called - returning null", id);
        return null;
    }

    @Override
    public BatchVerifyResultDTO batchVerifySignature(List<Long> ids) {
        log.warn("[STUB] batchVerifySignature({}) called - returning null", ids);
        return null;
    }

    @Override
    public BatchVerifyResultDTO batchVerifyInvoice(List<Long> ids) {
        log.warn("[STUB] batchVerifyInvoice({}) called - returning null", ids);
        return null;
    }

    @Override
    public Map<String, Object> testQrCodeExtraction(MultipartFile file) {
        log.warn("[STUB] testQrCodeExtraction() called - returning empty map");
        return Collections.emptyMap();
    }

    @Override
    public void uploadWithProgress(MultipartFile file, String uploadId) {
        log.warn("[STUB] uploadWithProgress({}, {}) called - no-op", file.getName(), uploadId);
    }

    @Override
    public Map<String, Object> reparse(Long id) {
        log.warn("[STUB] reparse({}) called - returning empty map", id);
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getToolkitStatus() {
        log.warn("[STUB] getToolkitStatus() called - returning empty map");
        return Collections.emptyMap();
    }
}
