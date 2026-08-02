package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RechargeRecordQueryDTO;
import com.foodtraceability.dto.marketing.RechargeRecordVO;
import com.foodtraceability.dto.marketing.RechargeRequestDTO;
import com.foodtraceability.entity.marketing.RechargeRecord;
import com.foodtraceability.service.marketing.RechargeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 充值记录管理控制器
 * 对应前端 API 路径 /v1/recharge-records
 *
 * 端点说明：
 * 1. GET    /v1/recharge-records           - 分页查询充值记录
 * 2. GET    /v1/recharge-records/{id}       - 获取充值记录详情
 * 3. POST   /v1/recharge-records/recharge   - 会员充值（字面量路径优先于 {id}）
 * 4. POST   /v1/recharge-records/export     - 导出充值记录（字面量路径优先于 {id}）
 * 5. PUT    /v1/recharge-records/{id}       - 更新充值记录
 * 6. DELETE /v1/recharge-records/{id}       - 删除充值记录（逻辑删除）
 *
 * 路径匹配优先级：字面量路径（recharge/export）优先于 {id} 匹配
 */
@RestController
@RequestMapping("/v1/recharge-records")
@Tag(name = "充值记录管理", description = "会员充值、充值记录查询、导出")
public class RechargeRecordController {

    private final RechargeRecordService rechargeRecordService;

    public RechargeRecordController(RechargeRecordService rechargeRecordService) {
        this.rechargeRecordService = rechargeRecordService;
    }

    /**
     * 会员充值
     * 注意：字面量路径 /recharge 优先于 /{id} 匹配，确保不会被误识别为记录ID
     */
    @PostMapping("/recharge")
    @Operation(summary = "会员充值")
    @PreAuthorize("hasAuthority('member:recharge:manage') or hasAuthority('*')")
    public Result<RechargeRecordVO> recharge(@Valid @RequestBody RechargeRequestDTO requestDTO) {
        try {
            RechargeRecordVO vo = rechargeRecordService.recharge(requestDTO);
            return Result.success(vo, "充值成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "充值失败：" + e.getMessage());
        }
    }

    /**
     * 导出充值记录
     * 注意：字面量路径 /export 优先于 /{id} 匹配
     */
    @PostMapping("/export")
    @Operation(summary = "导出充值记录")
    @PreAuthorize("hasAuthority('member:recharge:export') or hasAuthority('*')")
    public Result<Void> exportRecords(
            @Parameter(description = "会员ID") @RequestParam(required = false) String memberId,
            @Parameter(description = "会员手机号（模糊匹配）") @RequestParam(required = false) String memberPhone,
            @Parameter(description = "充值方案ID") @RequestParam(required = false) String planId,
            @Parameter(description = "支付方式") @RequestParam(required = false) String paymentMethod,
            @Parameter(description = "支付状态") @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "退款状态") @RequestParam(required = false) String refundStatus,
            @Parameter(description = "开始时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String endTime) {
        try {
            RechargeRecordQueryDTO queryDTO = new RechargeRecordQueryDTO();
            queryDTO.setMemberId(memberId);
            queryDTO.setMemberPhone(memberPhone);
            queryDTO.setPlanId(planId);
            queryDTO.setPaymentMethod(paymentMethod);
            queryDTO.setPaymentStatus(paymentStatus);
            queryDTO.setRefundStatus(refundStatus);
            queryDTO.setStartTime(startTime);
            queryDTO.setEndTime(endTime);
            rechargeRecordService.exportRecords(queryDTO);
            return Result.success(null, "导出任务已创建");
        } catch (Exception e) {
            return Result.error(500, "导出充值记录失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询充值记录
     */
    @GetMapping
    @Operation(summary = "分页查询充值记录")
    @PreAuthorize("hasAuthority('member:recharge:view') or hasAuthority('*')")
    public Result<PageResult<RechargeRecordVO>> getRecordPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "会员ID") @RequestParam(required = false) String memberId,
            @Parameter(description = "会员手机号（模糊匹配）") @RequestParam(required = false) String memberPhone,
            @Parameter(description = "充值方案ID") @RequestParam(required = false) String planId,
            @Parameter(description = "支付方式: wechat/alipay/cash/bank_card/balance") @RequestParam(required = false) String paymentMethod,
            @Parameter(description = "支付状态: pending/success/failed/refunded/partial_refunded") @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "退款状态: none/pending/approved/rejected/refunded") @RequestParam(required = false) String refundStatus,
            @Parameter(description = "开始时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String endTime) {
        try {
            RechargeRecordQueryDTO queryDTO = new RechargeRecordQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setMemberId(memberId);
            queryDTO.setMemberPhone(memberPhone);
            queryDTO.setPlanId(planId);
            queryDTO.setPaymentMethod(paymentMethod);
            queryDTO.setPaymentStatus(paymentStatus);
            queryDTO.setRefundStatus(refundStatus);
            queryDTO.setStartTime(startTime);
            queryDTO.setEndTime(endTime);
            PageResult<RechargeRecordVO> result = rechargeRecordService.getRecordPage(queryDTO);
            return Result.success(result, "查询充值记录成功");
        } catch (Exception e) {
            return Result.error(500, "查询充值记录失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取充值记录详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取充值记录详情")
    @PreAuthorize("hasAuthority('member:recharge:view') or hasAuthority('*')")
    public Result<RechargeRecordVO> getRecordById(
            @Parameter(description = "记录ID") @PathVariable("id") String id) {
        try {
            RechargeRecordVO vo = rechargeRecordService.getRecordById(id);
            if (vo == null) {
                return Result.error(404, "充值记录不存在");
            }
            return Result.success(vo, "获取充值记录详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取充值记录详情失败：" + e.getMessage());
        }
    }

    /**
     * 更新充值记录
     * 仅允许更新业务可变字段（会员信息、支付方式、支付状态、退款相关字段）
     * 金额、流水号等财务核心字段不可变更
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新充值记录")
    @PreAuthorize("hasAuthority('member:recharge:manage') or hasAuthority('*')")
    public Result<RechargeRecordVO> updateRecord(
            @Parameter(description = "记录ID") @PathVariable("id") String id,
            @RequestBody RechargeRecord record) {
        try {
            RechargeRecordVO vo = rechargeRecordService.updateRecord(id, record);
            return Result.success(vo, "更新充值记录成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新充值记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除充值记录（逻辑删除）
     * 安全约束：已支付成功的记录禁止删除，需走退款流程
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除充值记录（逻辑删除）")
    @PreAuthorize("hasAuthority('member:recharge:manage') or hasAuthority('*')")
    public Result<Void> deleteRecord(
            @Parameter(description = "记录ID") @PathVariable("id") String id) {
        try {
            boolean success = rechargeRecordService.deleteRecord(id);
            if (!success) {
                return Result.error(404, "充值记录不存在");
            }
            return Result.success(null, "删除充值记录成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除充值记录失败：" + e.getMessage());
        }
    }
}
