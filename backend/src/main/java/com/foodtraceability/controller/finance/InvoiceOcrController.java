package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InvoiceOcrResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * 发票OCR模拟控制器
 *
 * <p>提供发票图片OCR识别的模拟入口：上传发票图片后返回固定模拟识别结果，
 * 用于M5财务联动发票录入环节的前端联调，不依赖真实OCR引擎。</p>
 */
@Tag(name = "发票OCR模拟", description = "模拟发票图片OCR识别结果")
@RestController
@RequestMapping("/v1/finance/invoices")
public class InvoiceOcrController {

    private static final String MOCK_ENGINE = "MockOCR";

    /**
     * 模拟识别发票图片
     *
     * @param file 发票图片或PDF（本接口仅做占位，实际返回固定模拟数据）
     * @return 模拟OCR识别结果
     */
    @Operation(summary = "模拟发票OCR识别", description = "上传发票文件，返回模拟OCR识别结果")
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('finance:invoice:create')")
    public Result<InvoiceOcrResultDTO> recognize(@RequestParam("file") MultipartFile file) {
        InvoiceOcrResultDTO result = InvoiceOcrResultDTO.success(MOCK_ENGINE);
        result.setInvoiceCode("044002100211");
        result.setInvoiceNo("12345678");
        result.setInvoiceType("vat_special");
        result.setInvoiceTypeName("增值税专用发票");
        result.setIssueDate("2026-07-20");
        result.setCheckCode("123456");
        result.setBuyerName("测试公司A");
        result.setBuyerTaxNo("91110000123456789X");
        result.setBuyerAddressPhone("北京市朝阳区测试路1号 010-88880001");
        result.setBuyerBankAccount("中国工商银行测试支行 1100000012345678901");
        result.setSellerName("鲜蔬源农产品有限公司");
        result.setSellerTaxNo("91110000987654321Y");
        result.setSellerAddressPhone("北京市海淀区农产品路2号 010-66660002");
        result.setSellerBankAccount("中国农业银行测试支行 1100000098765432109");
        result.setAmountWithoutTax(new BigDecimal("100.00"));
        result.setTaxRate(new BigDecimal("0.13"));
        result.setTaxAmount(new BigDecimal("13.00"));
        result.setTotalAmount(new BigDecimal("113.00"));
        result.setPayee("张三");
        result.setChecker("李四");
        result.setIssuer("王五");
        result.setRemarks("模拟OCR识别结果，仅用于测试");
        result.setConfidence(0.98);
        result.setProcessingTimeMs(120.0);
        return Result.success(result);
    }

    /**
     * 获取模拟OCR服务状态
     *
     * @return 服务状态
     */
    @Operation(summary = "获取模拟OCR服务状态", description = "返回模拟OCR引擎是否可用")
    @GetMapping("/ocr/status")
    @PreAuthorize("hasAuthority('finance:invoice:view')")
    public Result<Object> status() {
        return Result.success(new Object() {
            public final boolean available = true;
            public final String engineName = MOCK_ENGINE;
        });
    }
}
