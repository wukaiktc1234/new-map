package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RefundApproveDTO;
import com.foodtraceability.dto.marketing.RefundCreateDTO;
import com.foodtraceability.dto.marketing.RefundQueryDTO;
import com.foodtraceability.dto.marketing.RefundVO;
import com.foodtraceability.service.marketing.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款申请管理控制器
 * 对应前端 API 路径 /v1/refunds
 *
 * 端点说明：
 * 1. GET    /v1/refunds                    - 分页查询退款申请
 * 2. POST   /v1/refunds                    - 创建退款申请
 * 3. PUT    /v1/refunds/{refundId}/approve - 审批退款申请
 * 4. POST   /v1/refunds/{refundId}/execute - 执行退款
 *
 * 退款审批流程：申请 → 审批 → 执行
 * 退款仅退本金，赠送按配置处理（清零或按比例扣减）
 */
@RestController
@RequestMapping("/v1/refunds")
@Tag(name = "退款申请管理", description = "退款申请的创建、审批、执行")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * 分页查询退款申请
     */
    @GetMapping
    @Operation(summary = "分页查询退款申请")
    public Result<PageResult<RefundVO>> getRefundPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "退款状态: pending/approved/rejected/executed/cancelled") @RequestParam(required = false) String status) {
        try {
            RefundQueryDTO queryDTO = new RefundQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setStatus(status);
            PageResult<RefundVO> result = refundService.getRefundPage(queryDTO);
            return Result.success(result, "查询退款申请成功");
        } catch (Exception e) {
            return Result.error(500, "查询退款申请失败：" + e.getMessage());
        }
    }

    /**
     * 创建退款申请
     */
    @PostMapping
    @Operation(summary = "创建退款申请")
    public Result<RefundVO> createRefund(@Valid @RequestBody RefundCreateDTO createDTO) {
        try {
            RefundVO vo = refundService.createRefund(createDTO);
            return Result.success(vo, "创建退款申请成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建退款申请失败：" + e.getMessage());
        }
    }

    /**
     * 审批退款申请（同意/拒绝）
     */
    @PutMapping("/{refundId}/approve")
    @Operation(summary = "审批退款申请")
    public Result<Void> approveRefund(
            @Parameter(description = "退款ID") @PathVariable("refundId") String refundId,
            @Valid @RequestBody RefundApproveDTO approveDTO) {
        try {
            refundService.approveRefund(refundId, approveDTO);
            return Result.success(null, "审批退款申请成功");
        } catch (RuntimeException e) {
            return Result.error(4002, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "审批退款申请失败：" + e.getMessage());
        }
    }

    /**
     * 执行退款
     */
    @PostMapping("/{refundId}/execute")
    @Operation(summary = "执行退款")
    public Result<Void> executeRefund(
            @Parameter(description = "退款ID") @PathVariable("refundId") String refundId) {
        try {
            refundService.executeRefund(refundId);
            return Result.success(null, "执行退款成功");
        } catch (RuntimeException e) {
            return Result.error(4003, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "执行退款失败：" + e.getMessage());
        }
    }
}
