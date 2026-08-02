package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RechargeFinanceLogQueryDTO;
import com.foodtraceability.dto.marketing.RechargeFinanceLogVO;
import com.foodtraceability.service.marketing.RechargeFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 储值财务流水控制器
 * 对应前端 API 路径 /v1/recharge-finance
 *
 * 端点说明：
 * 1. GET  /v1/recharge-finance         - 分页查询财务流水
 * 2. POST /v1/recharge-finance/export  - 导出财务流水
 *
 * 流水类型：recharge-充值 consume-消费 refund-退款 bonus_expire-赠送过期 bonus_grant-赠送发放
 */
@RestController
@RequestMapping("/v1/recharge-finance")
@Tag(name = "储值财务流水", description = "财务流水查询、导出")
public class RechargeFinanceController {

    private final RechargeFinanceService rechargeFinanceService;

    public RechargeFinanceController(RechargeFinanceService rechargeFinanceService) {
        this.rechargeFinanceService = rechargeFinanceService;
    }

    /**
     * 分页查询财务流水
     */
    @GetMapping
    @Operation(summary = "分页查询财务流水")
    public Result<PageResult<RechargeFinanceLogVO>> getFinanceLogPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "流水类型: recharge/consume/refund/bonus_expire/bonus_grant") @RequestParam(required = false) String financeType,
            @Parameter(description = "开始时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String endTime) {
        try {
            RechargeFinanceLogQueryDTO queryDTO = new RechargeFinanceLogQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setFinanceType(financeType);
            queryDTO.setStartTime(startTime);
            queryDTO.setEndTime(endTime);
            PageResult<RechargeFinanceLogVO> result = rechargeFinanceService.getFinanceLogPage(queryDTO);
            return Result.success(result, "查询财务流水成功");
        } catch (Exception e) {
            return Result.error(500, "查询财务流水失败：" + e.getMessage());
        }
    }

    /**
     * 导出财务流水
     */
    @PostMapping("/export")
    @Operation(summary = "导出财务流水")
    public Result<Void> exportFinanceLogs(
            @Parameter(description = "开始时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（YYYY-MM-DD HH:mm:ss）") @RequestParam(required = false) String endTime) {
        try {
            RechargeFinanceLogQueryDTO queryDTO = new RechargeFinanceLogQueryDTO();
            queryDTO.setStartTime(startTime);
            queryDTO.setEndTime(endTime);
            rechargeFinanceService.exportFinanceLogs(queryDTO);
            return Result.success(null, "导出任务已创建");
        } catch (Exception e) {
            return Result.error(500, "导出财务流水失败：" + e.getMessage());
        }
    }
}
