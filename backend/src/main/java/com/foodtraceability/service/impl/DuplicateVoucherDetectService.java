package com.foodtraceability.service.impl;

import com.foodtraceability.entity.ElectronicInvoice;
import com.foodtraceability.entity.ElectronicVoucher;
import com.foodtraceability.mapper.ElectronicInvoiceMapper;
import com.foodtraceability.mapper.ElectronicVoucherMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/**
 * 电子凭证重复报销检测服务
 * 
 * 依据：
 * - 财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * - 财会〔2020〕6号《关于规范电子会计凭证报销入账归档的通知》
 * - 2026年财务合规要求
 * 
 * 检测维度：
 * 1. 文件哈希去重 - 检测完全相同的文件
 * 2. 发票号码去重 - 检测相同发票代码+发票号码
 * 3. 关键信息去重 - 检测金额、日期、销售方等关键信息组合
 * 4. 跨业务检测 - 检测同一发票是否已关联其他业务
 */
@Service
public class DuplicateVoucherDetectService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DuplicateVoucherDetectService.class);
    private final ElectronicVoucherMapper voucherMapper;
    private final ElectronicInvoiceMapper invoiceMapper;


    /**
     * 重复检测结果
     */
    public static class DuplicateCheckResult {
        private boolean isDuplicate = false;
        private String duplicateType;
        private String duplicateMessage;
        private List<DuplicateRecord> duplicateRecords = new ArrayList<>();

        public boolean isDuplicate() {
            return isDuplicate;
        }

        public void setDuplicate(boolean duplicate) {
            isDuplicate = duplicate;
        }

        public String getDuplicateType() {
            return duplicateType;
        }

        public void setDuplicateType(String duplicateType) {
            this.duplicateType = duplicateType;
        }

        public String getDuplicateMessage() {
            return duplicateMessage;
        }

        public void setDuplicateMessage(String duplicateMessage) {
            this.duplicateMessage = duplicateMessage;
        }

        public List<DuplicateRecord> getDuplicateRecords() {
            return duplicateRecords;
        }

        public void addDuplicateRecord(DuplicateRecord record) {
            duplicateRecords.add(record);
        }
    }


    /**
     * 重复记录详情
     */
    public static class DuplicateRecord {
        private Long voucherId;
        private String voucherNo;
        private String voucherType;
        private String sourceFileName;
        private BigDecimal totalAmount;
        private LocalDate issueDate;
        private String businessType;
        private Long businessId;
        private String status;
        private String createTime;

        public Long getVoucherId() {
            return voucherId;
        }

        public void setVoucherId(Long voucherId) {
            this.voucherId = voucherId;
        }

        public String getVoucherNo() {
            return voucherNo;
        }

        public void setVoucherNo(String voucherNo) {
            this.voucherNo = voucherNo;
        }

        public String getVoucherType() {
            return voucherType;
        }

        public void setVoucherType(String voucherType) {
            this.voucherType = voucherType;
        }

        public String getSourceFileName() {
            return sourceFileName;
        }

        public void setSourceFileName(String sourceFileName) {
            this.sourceFileName = sourceFileName;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public LocalDate getIssueDate() {
            return issueDate;
        }

        public void setIssueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
        }

        public String getBusinessType() {
            return businessType;
        }

        public void setBusinessType(String businessType) {
            this.businessType = businessType;
        }

        public Long getBusinessId() {
            return businessId;
        }

        public void setBusinessId(Long businessId) {
            this.businessId = businessId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }
    }

    /**
     * 全面重复检测
     * 
     * @param tenantId 租户ID
     * @param fileHash 文件哈希值（SHA256）
     * @param invoiceCode 发票代码（可选）
     * @param invoiceNo 发票号码（可选）
     * @param totalAmount 金额（可选）
     * @param issueDate 开票日期（可选）
     * @param sellerTaxNo 销售方税号（可选）
     * @return 检测结果
     */
    public DuplicateCheckResult checkDuplicate(Long tenantId, String fileHash, String invoiceCode, String invoiceNo, BigDecimal totalAmount, LocalDate issueDate, String sellerTaxNo) {
        log.info("[重复检测] 开始检测, 租户ID: {}, 发票号码: {}", tenantId, invoiceNo);
        DuplicateCheckResult result = new DuplicateCheckResult();
        DuplicateCheckResult hashResult = checkByFileHash(tenantId, fileHash);
        if (hashResult.isDuplicate()) {
            result.setDuplicate(true);
            result.setDuplicateType("FILE_HASH");
            result.setDuplicateMessage("检测到完全相同的文件已存在");
            result.getDuplicateRecords().addAll(hashResult.getDuplicateRecords());
            log.warn("[重复检测] 文件哈希重复: {}", fileHash);
            return result;
        }
        if (invoiceCode != null && invoiceNo != null) {
            DuplicateCheckResult invoiceResult = checkByInvoiceNo(tenantId, invoiceCode, invoiceNo);
            if (invoiceResult.isDuplicate()) {
                result.setDuplicate(true);
                result.setDuplicateType("INVOICE_NO");
                result.setDuplicateMessage("检测到相同发票号码已存在");
                result.getDuplicateRecords().addAll(invoiceResult.getDuplicateRecords());
                log.warn("[重复检测] 发票号码重复: {}{}", invoiceCode, invoiceNo);
                return result;
            }
        }
        if (totalAmount != null && issueDate != null && sellerTaxNo != null) {
            DuplicateCheckResult keyInfoResult = checkByKeyInfo(tenantId, totalAmount, issueDate, sellerTaxNo);
            if (keyInfoResult.isDuplicate()) {
                result.setDuplicate(true);
                result.setDuplicateType("KEY_INFO");
                result.setDuplicateMessage("检测到相似凭证已存在（相同金额、日期、销售方）");
                result.getDuplicateRecords().addAll(keyInfoResult.getDuplicateRecords());
                log.warn("[重复检测] 关键信息重复: 金额={}, 日期={}, 销售方={}", totalAmount, issueDate, sellerTaxNo);
                return result;
            }
        }
        log.info("[重复检测] 未发现重复");
        return result;
    }

    /**
     * 检测发票重复
     */
    public DuplicateCheckResult checkInvoiceDuplicate(Long tenantId, String invoiceCode, String invoiceNo, Long excludeVoucherId) {
        log.info("[重复检测] 检测发票重复, 发票代码: {}, 发票号码: {}", invoiceCode, invoiceNo);
        DuplicateCheckResult result = new DuplicateCheckResult();
        try {
            List<ElectronicInvoice> existingInvoices = invoiceMapper.findByCodeAndNo(tenantId, invoiceCode, invoiceNo);
            if (existingInvoices != null && !existingInvoices.isEmpty()) {
                for (ElectronicInvoice invoice : existingInvoices) {
                    if (excludeVoucherId != null && invoice.getVoucherId().equals(excludeVoucherId)) {
                        continue;
                    }
                    DuplicateRecord record = new DuplicateRecord();
                    record.setVoucherId(invoice.getVoucherId());
                    record.setVoucherNo(invoice.getInvoiceNo());
                    record.setVoucherType("invoice");
                    record.setTotalAmount(invoice.getTotalAmount());
                    record.setIssueDate(invoice.getIssueDate());
                    ElectronicVoucher voucher = voucherMapper.selectById(invoice.getVoucherId());
                    if (voucher != null) {
                        record.setBusinessType(voucher.getBusinessType());
                        record.setBusinessId(voucher.getBusinessId());
                        record.setStatus(getStatusText(voucher.getStatus()));
                    }
                    result.addDuplicateRecord(record);
                }
            }
            if (!result.getDuplicateRecords().isEmpty()) {
                result.setDuplicate(true);
                result.setDuplicateType("INVOICE_NO");
                result.setDuplicateMessage("发票号码 " + invoiceCode + invoiceNo + " 已存在");
            }
        } catch (Exception e) {
            log.error("[重复检测] 发票检测异常", e);
        }
        return result;
    }

    /**
     * 通过文件哈希检测
     */
    private DuplicateCheckResult checkByFileHash(Long tenantId, String fileHash) {
        DuplicateCheckResult result = new DuplicateCheckResult();
        if (fileHash == null || fileHash.isEmpty()) {
            return result;
        }
        try {
            ElectronicVoucher existingVoucher = voucherMapper.selectByFileHash(fileHash, tenantId);
            if (existingVoucher != null) {
                result.setDuplicate(true);
                DuplicateRecord record = new DuplicateRecord();
                record.setVoucherId(existingVoucher.getId());
                record.setVoucherNo(existingVoucher.getVoucherNo());
                record.setVoucherType(existingVoucher.getVoucherType());
                record.setTotalAmount(existingVoucher.getTotalAmount());
                record.setIssueDate(existingVoucher.getIssueDate());
                record.setBusinessType(existingVoucher.getBusinessType());
                record.setBusinessId(existingVoucher.getBusinessId());
                record.setStatus(getStatusText(existingVoucher.getStatus()));
                result.addDuplicateRecord(record);
            }
        } catch (Exception e) {
            log.error("[重复检测] 文件哈希检测异常", e);
        }
        return result;
    }

    /**
     * 通过发票号码检测
     */
    private DuplicateCheckResult checkByInvoiceNo(Long tenantId, String invoiceCode, String invoiceNo) {
        return checkInvoiceDuplicate(tenantId, invoiceCode, invoiceNo, null);
    }

    /**
     * 通过关键信息组合检测
     */
    private DuplicateCheckResult checkByKeyInfo(Long tenantId, BigDecimal totalAmount, LocalDate issueDate, String sellerTaxNo) {
        DuplicateCheckResult result = new DuplicateCheckResult();
        try {
            List<ElectronicVoucher> similarVouchers = voucherMapper.findSimilarVouchers(tenantId, totalAmount, issueDate);
            if (similarVouchers != null && !similarVouchers.isEmpty()) {
                for (ElectronicVoucher voucher : similarVouchers) {
                    if (voucher.getVoucherType().equals("invoice")) {
                        ElectronicInvoice invoice = invoiceMapper.selectByVoucherId(voucher.getId());
                        if (invoice != null && sellerTaxNo.equals(invoice.getSellerTaxNo())) {
                            DuplicateRecord record = new DuplicateRecord();
                            record.setVoucherId(voucher.getId());
                            record.setVoucherNo(voucher.getVoucherNo());
                            record.setVoucherType(voucher.getVoucherType());
                            record.setTotalAmount(voucher.getTotalAmount());
                            record.setIssueDate(voucher.getIssueDate());
                            record.setBusinessType(voucher.getBusinessType());
                            record.setBusinessId(voucher.getBusinessId());
                            record.setStatus(getStatusText(voucher.getStatus()));
                            result.addDuplicateRecord(record);
                        }
                    }
                }
            }
            if (!result.getDuplicateRecords().isEmpty()) {
                result.setDuplicate(true);
            }
        } catch (Exception e) {
            log.error("[重复检测] 关键信息检测异常", e);
        }
        return result;
    }

    /**
     * 计算文件哈希
     */
    public String calculateFileHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileContent);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(255 & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("[重复检测] 计算文件哈希失败", e);
            return null;
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已入账";
            case 2 -> "已归档";
            default -> "未知";
        };
    }

    public DuplicateVoucherDetectService(final ElectronicVoucherMapper voucherMapper, final ElectronicInvoiceMapper invoiceMapper) {
        this.voucherMapper = voucherMapper;
        this.invoiceMapper = invoiceMapper;
    }
}
