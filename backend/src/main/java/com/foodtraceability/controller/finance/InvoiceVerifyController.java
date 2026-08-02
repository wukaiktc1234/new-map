package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InvoiceVerifyRequestDTO;
import com.foodtraceability.dto.InvoiceVerifyResultDTO;
import com.foodtraceability.service.InvoiceVerifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 发票验真控制器
 *
 * <p>Sprint 3.1 P0 F-003：发票验真接口。</p>
 *
 * <p>路径前缀：/v1/finance/invoice-verify（原 /api/v1/invoice/verify）</p>
 */
@RestController
@RequestMapping("/v1/finance/invoice-verify")
@Tag(name = "发票验真", description = "发票真伪验证接口")
public class InvoiceVerifyController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvoiceVerifyController.class);
    private final InvoiceVerifyService invoiceVerifyService;

    @PostMapping
    @Operation(summary = "发票验真", description = "验证发票真伪，返回发票详细信息")
    @PreAuthorize("hasAuthority('finance:invoice:verify')")
    public Result<InvoiceVerifyResultDTO> verify(@RequestBody InvoiceVerifyRequestDTO request) {
        log.info("[发票验真] 收到验真请求: 发票代码={}, 发票号码={}", request.getInvoiceCode(), request.getInvoiceNo());
        if (!invoiceVerifyService.isAvailable()) {
            return Result.error("发票验真服务不可用");
        }
        if (request.getInvoiceNo() == null || request.getInvoiceNo().isEmpty()) {
            return Result.error("发票号码不能为空");
        }
        if (request.getIssueDate() == null || request.getIssueDate().isEmpty()) {
            return Result.error("开票日期不能为空");
        }
        InvoiceVerifyResultDTO result = invoiceVerifyService.verify(request);
        if (result.isSuccess()) {
            log.info("[发票验真] 验真成功: 发票号码={}, 销方={}", result.getInvoiceNo(), result.getSellerName());
            return Result.success(result);
        } else {
            log.warn("[发票验真] 验真失败: {}", result.getMessage());
            return Result.error(result.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取验真服务状态", description = "检查验真服务是否可用")
    @PreAuthorize("hasAuthority('finance:invoice:verify')")
    public Result<Object> getStatus() {
        return Result.success(new Object() {
            public final boolean available = invoiceVerifyService.isAvailable();
            public final String serviceName = invoiceVerifyService.getServiceName();
        });
    }

    public InvoiceVerifyController(final InvoiceVerifyService invoiceVerifyService) {
        this.invoiceVerifyService = invoiceVerifyService;
    }
}
