package com.foodtraceability.service.impl;

import com.foodtraceability.dto.InvoiceVerifyResultDTO;
import com.foodtraceability.entity.ElectronicInvoice;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 模拟税务平台验真服务（仅开发环境生效）
 *
 * <p>用于在没有真实税务API账号时测试验真链路是否完整可用。</p>
 * <p>生产环境不会加载此 Service（通过 @Profile("dev") 限制）。</p>
 *
 * 模拟规则：
 * 1. 发票号码以"26"开头的全电发票 -> 返回验真通过
 * 2. 发票号码以"24"开头的增值税发票 -> 返回验真通过
 * 3. 发票号码包含"TEST"或"测试" -> 返回验真通过
 * 4. 其他情况 -> 根据金额判断（金额>0则通过）
 */
@Service
@Profile("dev")
public class MockTaxPlatformService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MockTaxPlatformService.class);
    private static final Random random = new Random();

    /**
     * 模拟税务平台验真
     */
    public InvoiceVerifyResultDTO mockVerify(ElectronicInvoice invoice) {
        log.info("[模拟验真] 开始模拟验真, 发票号码: {}", maskInvoiceNo(invoice.getInvoiceNo()));
        InvoiceVerifyResultDTO result = new InvoiceVerifyResultDTO();
        boolean shouldPass = determineVerifyResult(invoice);
        if (shouldPass) {
            result.setStatus(1);
            result.setStatusDescription("验真通过（模拟税务平台验证）");
            result.setSuccess(true);
            result.setMessage("发票信息验证通过");
            result.setVerifySource("模拟税务平台（测试模式）");
            result.setTaxPlatformVerified(true);
            fillMockInvoiceData(result, invoice);
            log.info("[模拟验真] 验真通过, 发票号码: {}", maskInvoiceNo(invoice.getInvoiceNo()));
        } else {
            result.setStatus(2);
            result.setStatusDescription("验真失败（模拟）");
            result.setSuccess(false);
            result.setMessage("模拟：发票信息不匹配或不存在");
            result.setVerifySource("模拟税务平台（测试模式）");
            result.setTaxPlatformVerified(false);
            log.warn("[模拟验真] 验真失败, 发票号码: {}", maskInvoiceNo(invoice.getInvoiceNo()));
        }
        result.setVerifyTime(LocalDateTime.now());
        result.setVerifyCount(random.nextInt(5) + 1);
        return result;
    }

    /**
     * 根据发票信息判断验真结果
     */
    private boolean determineVerifyResult(ElectronicInvoice invoice) {
        String invoiceNo = invoice.getInvoiceNo();
        if (invoiceNo == null || invoiceNo.isEmpty()) {
            return false;
        }
        if (invoiceNo.startsWith("26")) {
            log.info("[模拟验真] 检测到全电发票格式，返回验真通过");
            return true;
        }
        if (invoiceNo.startsWith("24")) {
            log.info("[模拟验真] 检测到增值税发票格式，返回验真通过");
            return true;
        }
        if (invoiceNo.toUpperCase().contains("TEST") || invoiceNo.contains("测试")) {
            log.info("[模拟验真] 检测到测试发票，返回验真通过");
            return true;
        }
        if (invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            log.info("[模拟验真] 发票金额有效，返回验真通过");
            return true;
        }
        return false;
    }

    /**
     * 填充模拟发票数据
     */
    private void fillMockInvoiceData(InvoiceVerifyResultDTO result, ElectronicInvoice invoice) {
        result.setInvoiceCode(invoice.getInvoiceCode());
        result.setInvoiceNo(invoice.getInvoiceNo());
        result.setIssueDate(invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : null);
        result.setBuyerName(invoice.getBuyerName() != null ? invoice.getBuyerName() : "模拟购买方");
        result.setBuyerTaxNo(invoice.getBuyerTaxNo() != null ? invoice.getBuyerTaxNo() : "91110000MA00XXXXXX");
        result.setSellerName(invoice.getSellerName() != null ? invoice.getSellerName() : "模拟销售方");
        result.setSellerTaxNo(invoice.getSellerTaxNo() != null ? invoice.getSellerTaxNo() : "91110000MA00YYYYYY");
        result.setTotalAmount(invoice.getTotalAmount());
        result.setTaxAmount(invoice.getTaxAmount());
        result.setAmountWithoutTax(invoice.getAmountWithoutTax());
        result.setCheckCode(invoice.getCheckCode());
        result.setInvoiceStatus(0);
        result.setInvoiceStatusDescription("正常");
        result.setInvalidFlag("N");
        result.setSellerAddressPhone("北京市朝阳区XX路XX号 010-12345678");
        result.setSellerBankAccount("中国工商银行北京分行 1234567890123456789");
        result.setBuyerAddressPhone(invoice.getBuyerAddress());
        result.setBuyerBankAccount(invoice.getBuyerBank());
        result.setRemark("模拟验真测试数据");
        result.setPayee("张三");
        result.setChecker("李四");
        String invoiceType = determineInvoiceType(invoice);
        result.setInvoiceType(invoiceType);
        result.setInvoiceTypeName(getInvoiceTypeName(invoiceType));
    }

    /**
     * 判断发票类型
     */
    private String determineInvoiceType(ElectronicInvoice invoice) {
        String invoiceNo = invoice.getInvoiceNo();
        if (invoiceNo != null && invoiceNo.startsWith("26")) {
            return "028";
        }
        if (invoiceNo != null && invoiceNo.startsWith("24")) {
            return "004";
        }
        return "007";
    }

    /**
     * 获取发票类型名称
     */
    private String getInvoiceTypeName(String type) {
        Map<String, String> typeNames = new HashMap<>();
        typeNames.put("004", "增值税专用发票");
        typeNames.put("007", "增值税普通发票");
        typeNames.put("028", "电子发票（增值税专用发票）");
        typeNames.put("010", "电子发票（增值税普通发票）");
        return typeNames.getOrDefault(type, "增值税发票");
    }

    /**
     * 脱敏发票号码
     */
    private String maskInvoiceNo(String invoiceNo) {
        if (invoiceNo == null || invoiceNo.length() <= 8) {
            return invoiceNo;
        }
        return invoiceNo.substring(0, 4) + "****" + invoiceNo.substring(invoiceNo.length() - 4);
    }
}
