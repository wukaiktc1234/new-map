package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PosEndShiftDTO;
import com.foodtraceability.dto.PosLoginDTO;
import com.foodtraceability.dto.PosStartShiftDTO;
import com.foodtraceability.entity.PosShift;
import com.foodtraceability.service.PosAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * POS认证控制器
 * 提供收银端的登录认证和班次管理API
 */
@RestController
@RequestMapping("/v1/pos/auth")
@Tag(name = "POS认证API", description = "收银端登录认证和班次管理接口")
public class PosAuthController {

    private static final Logger logger = LoggerFactory.getLogger(PosAuthController.class);

    private final PosAuthService posAuthService;

    public PosAuthController(PosAuthService posAuthService) {
        this.posAuthService = posAuthService;
    }

    /**
     * POS收银端登录
     */
    @PostMapping("/login")
    @Operation(summary = "POS收银端登录", description = "员工通过工号和密码登录POS系统")
    public Result<Map<String, Object>> login(@Valid @RequestBody PosLoginDTO params) {
        try {
            Map<String, Object> result = posAuthService.login(params.getEmployeeId(), params.getPassword(), params.getTerminalId());
            return Result.success(result, "登录成功");
        } catch (Exception e) {
            logger.error("POS登录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前活跃班次
     */
    @GetMapping("/current-shift")
    @Operation(summary = "获取当前班次", description = "获取指定终端当前进行中的班次信息")
    public Result<PosShift> getCurrentShift(@RequestParam String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) {
            return Result.error(400, "终端ID不能为空");
        }

        PosShift shift = posAuthService.getCurrentShift(terminalId);
        if (shift == null) {
            return Result.success(null, "没有进行中的班次");
        }
        return Result.success(shift);
    }

    /**
     * 开始新班次
     */
    @PostMapping("/shift/start")
    @Operation(summary = "开始新班次", description = "在指定终端开始一个新的工作班次")
    public Result<PosShift> startShift(@Valid @RequestBody PosStartShiftDTO params) {
        try {
            PosShift shift = posAuthService.startShift(
                    params.getTerminalId(), params.getEmployeeId(), params.getEmployeeName(),
                    params.getShiftType(), params.getOpeningCash());
            return Result.success(shift, "班次开始成功");
        } catch (Exception e) {
            logger.error("开始班次失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 结束当前班次
     */
    @PostMapping("/shift/end")
    @Operation(summary = "结束当前班次", description = "结束指定终端的当前班次，并统计订单数据")
    public Result<Map<String, Object>> endShift(@Valid @RequestBody PosEndShiftDTO params) {
        try {
            Map<String, Object> result = posAuthService.endShift(params.getTerminalId(), params.getRemark());
            return Result.success(result, "班次结束成功");
        } catch (Exception e) {
            logger.error("结束班次失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前班次统计摘要
     */
    @GetMapping("/shift/summary")
    @Operation(summary = "获取班次摘要", description = "获取当前班次的实时统计数据")
    public Result<Map<String, Object>> getShiftSummary(@RequestParam String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) {
            return Result.error(400, "终端ID不能为空");
        }

        Map<String, Object> summary = posAuthService.getShiftSummary(terminalId);
        return Result.success(summary);
    }

    /**
     * 获取今日所有班次记录
     */
    @GetMapping("/shift/today")
    @Operation(summary = "获取今日班次", description = "获取指定终端今天的所有班次记录")
    public Result<List<PosShift>> getTodayShifts(@RequestParam String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) {
            return Result.error(400, "终端ID不能为空");
        }

        List<PosShift> shifts = posAuthService.getTodayShifts(terminalId);
        return Result.success(shifts);
    }
}
