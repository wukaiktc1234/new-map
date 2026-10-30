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
import org.springframework.http.ResponseEntity;
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
        // ★（Lead 2026-10-30 试点，裁定 (a)）：**删掉整段 try-catch**。
        //   controller 内 catch → return Result 会让 Spring 认为「正常返回」→ HTTP 恒定 200，
        //   全局处理器的 400/403/404 映射**根本收不到**（实测：POS 登录密码错 = HTTP 200 + code=400）。
        //   现在让 BusinessException 冒泡，由 GlobalExceptionHandler 映射成真 HTTP 状态码。
        Map<String, Object> result = posAuthService.login(params.getEmployeeId(), params.getPassword(), params.getTerminalId());
        return Result.success(result, "登录成功");
    }

    /**
     * 获取当前活跃班次
     */
    /**
     * ② POS「上一班备用金」默认值：当前登录人所属门店最近一条已结束班次。
     *    无历史 / 找不到门店 → code=0 data=null（不 404）。
     */
    @GetMapping("/shift/last")
    @Operation(summary = "上一班（备用金默认值）", description = "返回当前登录人所属门店最近一条已结束班次的 shiftId/terminalId/openingCash/startTime/endTime；无历史返回 data=null")
    public Result<Map<String, Object>> lastShift() {
        Map<String, Object> last = posAuthService.lastEndedShiftOfMyStore();
        if (last == null) {
            return Result.success(null, "没有历史班次");
        }
        return Result.success(last);
    }

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
        // ★（Lead 2026-10-30 试点 #2）：**删掉整段 try-catch**，同 login。
        //   修前 catch 内只有 log + return，会让「403 不得为他人开班」变成 HTTP 200 + code=403（两套表达）。
        //   现在 BusinessException 冒泡 → GlobalExceptionHandler 映射 403 → 真 HTTP 403。
        //   前端核对（Lead 要求）：PosShiftDialog 走 posApi.startShift → 拦截器 httpStatus===403 分支
        //   reject({message: errData?.message || '无权限执行该操作（403）', status:403, code:…})
        //   → StartShiftDialog catch 取 error.message → toast 仍显示**服务端原文**，不依赖 code 判定。
        PosShift shift = posAuthService.startShift(
                params.getTerminalId(), params.getEmployeeId(), params.getEmployeeName(),
                params.getShiftType(), params.getOpeningCash());
        return Result.success(shift, "班次开始成功");
    }

    /**
     * GET /v1/pos/auth/me —— 刷新时恢复前端能力快照（Lead 2026-10-29）。
     *
     * <p>前端已拔除 localStorage 权限，刷新会丢内存快照；本接口让刷新后**不再重新登录**，
     * 同时保持「权限不落本地存储」的安全要求。</p>
     *
     * <p>鉴权：Bearer token。**未带/失效 token 返回真实 HTTP 401**（不是 200+业务码）。</p>
     */
    @GetMapping("/me")
    @Operation(summary = "当前登录人能力快照", description = "刷新时恢复前端权限快照；权限来源与登录同源（UserPermissionCache）")
    public ResponseEntity<Result<Map<String, Object>>> me(@RequestParam(required = false) String terminalId) {
        if (com.foodtraceability.utils.SecurityUtils.getCurrentUserId() == null) {
            return ResponseEntity.status(401).body(Result.error(401, "未登录或登录已过期"));
        }
        try {
            return ResponseEntity.ok(Result.success(posAuthService.getCurrentUserInfo(terminalId), "ok"));
        } catch (com.foodtraceability.exception.BusinessException be) {
            if (be.getCode() == 401) {
                return ResponseEntity.status(401).body(Result.error(401, be.getMessage()));
            }
            return ResponseEntity.badRequest().body(Result.error(be.getCode(), be.getMessage()));
        } catch (Exception e) {
            logger.error("获取当前登录人失败: {}", e.getMessage());
            return ResponseEntity.status(500).body(Result.error(500, "获取当前登录人失败"));
        }
    }

    /**
     * 结束当前班次
     */
    @PostMapping("/shift/end")
    @Operation(summary = "结束当前班次", description = "结束指定终端的当前班次，并统计订单数据")
    public ResponseEntity<Result<Map<String, Object>>> endShift(@Valid @RequestBody PosEndShiftDTO params) {
        try {
            Map<String, Object> result = posAuthService.endShift(params.getTerminalId(), params.getRemark());
            return ResponseEntity.ok(Result.success(result, "班次结束成功"));
        } catch (com.foodtraceability.exception.BusinessException be) {
            logger.warn("结束班次被拒: code={}, msg={}", be.getCode(), be.getMessage());
            // ⑤ 交班闸（Lead 2026-10-29）：存在待处理异常单 → **真 HTTP 409**（不是 200+业务码）
            if (be.getCode() == 40902) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                        .body(Result.error(be.getCode(), be.getMessage()));
            }
            return ResponseEntity.badRequest().body(Result.error(be.getCode(), be.getMessage()));
        } catch (Exception e) {
            logger.error("结束班次失败: {}", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(e.getMessage()));
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
