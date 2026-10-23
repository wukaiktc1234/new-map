package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.BankPaymentRecordConfirmDTO;
import com.foodtraceability.dto.finance.BankPaymentRecordFillDTO;
import com.foodtraceability.dto.finance.BankPaymentRecordVO;
import com.foodtraceability.entity.finance.BankPaymentRecord;
import com.foodtraceability.service.finance.BankPaymentRecordService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 银行打款记录Controller
 * 提供银行打款记录的查询、回填、确认功能
 */
@Tag(name = "银行打款记录管理", description = "银行打款记录的查询、回填银行信息、财务确认")
@RestController
@RequestMapping("/v1/finance/bank-payment-records")
public class BankPaymentRecordController {

    private final BankPaymentRecordService bankPaymentRecordService;

    public BankPaymentRecordController(BankPaymentRecordService bankPaymentRecordService) {
        this.bankPaymentRecordService = bankPaymentRecordService;
    }

    @Operation(summary = "查询付款单的银行记录", description = "根据付款单ID查询关联的银行打款记录列表")
    @GetMapping("/by-payment/{paymentId}")
    @PreAuthorize("hasAuthority('finance:payment:query')")
    public Result<List<BankPaymentRecordVO>> getByPaymentId(@PathVariable Long paymentId) {
        List<BankPaymentRecord> records = bankPaymentRecordService.getByPaymentId(paymentId);
        List<BankPaymentRecordVO> vos = records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @Operation(summary = "回填银行记录", description = "回填银行流水号、交易时间、凭证等信息")
    @PostMapping("/{recordId}/fill-bank-info")
    @PreAuthorize("hasAuthority('finance:payment:edit')")
    public Result<BankPaymentRecordVO> fillBankInfo(
            @PathVariable Long recordId,
            @Valid @RequestBody BankPaymentRecordFillDTO dto) {
        BankPaymentRecord record = bankPaymentRecordService.fillBankInfo(
                recordId,
                dto.getBankTransactionNo(),
                dto.getBankTransactionTime(),
                dto.getVoucherType(),
                dto.getVoucherUrl());
        return Result.success(convertToVO(record));
    }

    @Operation(summary = "确认银行记录", description = "财务确认银行打款记录，结果为success或fail")
    @PostMapping("/{recordId}/confirm")
    @PreAuthorize("hasAuthority('finance:payment:confirm')")
    public Result<BankPaymentRecordVO> confirm(
            @PathVariable Long recordId,
            @Valid @RequestBody BankPaymentRecordConfirmDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        BankPaymentRecord record = bankPaymentRecordService.confirm(
                recordId,
                dto.getConfirmResult(),
                userId,
                userName,
                dto.getConfirmRemark());
        return Result.success(convertToVO(record));
    }

    /**
     * 实体转VO
     */
    private BankPaymentRecordVO convertToVO(BankPaymentRecord record) {
        BankPaymentRecordVO vo = new BankPaymentRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setPaymentId(record.getPaymentId());
        vo.setBankTransactionNo(record.getBankTransactionNo());
        vo.setBankTransactionTime(record.getBankTransactionTime());
        vo.setVoucherType(record.getVoucherType());
        vo.setVoucherUrl(record.getVoucherUrl());
        vo.setStatus(record.getStatus());
        vo.setConfirmResult(record.getConfirmResult());
        vo.setConfirmRemark(record.getConfirmRemark());
        vo.setConfirmUserName(record.getConfirmUserName());
        vo.setConfirmTime(record.getConfirmTime());
        vo.setCreateUserName(record.getCreateUserName());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
