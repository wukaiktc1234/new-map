package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.ReceiptCreateDTO;
import com.foodtraceability.dto.finance.ReceiptVO;
import com.foodtraceability.service.finance.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收款单Controller
 * 提供应收账款收款登记、收款历史查询等功能
 */
@Tag(name = "收款管理", description = "应收账款收款登记、四账联动、收款历史查询")
@RestController
@RequestMapping("/v1/finance/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @Operation(summary = "登记收款", description = "登记向客户的收款，触发四账联动：应收账款+银行账户+资金流水+会计凭证")
    @PostMapping
    public Result<ReceiptVO> registerReceipt(@Valid @RequestBody ReceiptCreateDTO dto) {
        ReceiptVO vo = receiptService.registerReceipt(dto);
        return Result.success(vo);
    }

    @Operation(summary = "获取收款单详情", description = "根据收款单ID获取详情")
    @GetMapping("/{receiptId}")
    public Result<ReceiptVO> getDetail(@PathVariable Long receiptId) {
        ReceiptVO vo = receiptService.getDetail(receiptId);
        return Result.success(vo);
    }

    @Operation(summary = "查询收款历史", description = "查询某笔应收账款的所有收款记录")
    @GetMapping("/by-receivable/{receivableId}")
    public Result<List<ReceiptVO>> getReceiptHistory(@PathVariable Long receivableId) {
        List<ReceiptVO> list = receiptService.getReceiptHistoryByReceivableId(receivableId);
        return Result.success(list);
    }

    @Operation(summary = "更新收款单", description = "更新收款单的可编辑字段（金额/方式/银行账户/日期/备注）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:receipt:edit')")
    public Result<ReceiptVO> update(@PathVariable Long id, @Valid @RequestBody ReceiptCreateDTO dto) {
        ReceiptVO vo = receiptService.update(id, dto);
        return Result.success(vo);
    }

    @Operation(summary = "删除收款单", description = "逻辑删除收款单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:receipt:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        receiptService.removeById(id);
        return Result.success();
    }
}
