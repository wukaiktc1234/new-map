package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.PosShift;
import com.foodtraceability.service.PosAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交班面板（Lead 2026-10-29）。
 *
 * <p><b>为什么补这个 controller：</b>前端 {@code frontend-pos/src/api/shiftExceptions.ts} 的 BASE 是
 * {@code /v1/pos/shift}，调 stats / exceptions —— 后端此前**整组缺失**（grep '/v1/pos/shift' = 0 处），
 * 导致交班面板数字永远空、按钮永远灰。</p>
 *
 * <p><b>口径同源硬要求：</b>stats 内部委托 {@link PosAuthService#getShiftStatsForPanel}，
 * 后者复用 endShift 用的**同一个** {@code countShiftOrders} 与同一套钱箱公式；
 * 严禁在此另写一套统计（否则前端看到的数字与交班落库数字对不上）。</p>
 */
@RestController
@RequestMapping("/v1/pos/shift")
@Tag(name = "POS-交班面板")
public class PosShiftController {

    private static final Logger log = LoggerFactory.getLogger(PosShiftController.class);

    private final PosAuthService posAuthService;

    public PosShiftController(PosAuthService posAuthService) {
        this.posAuthService = posAuthService;
    }

    /** stats：字段严格对齐前端 ShiftStats（frontend-pos/src/api/shiftExceptions.ts:21-40） */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('pos:shift:manage')")
    @Operation(summary = "交班面板统计", description = "与 endShift 同源；无进行中班次返回 data=null（不 404）")
    public Result<Map<String, Object>> stats(@RequestParam String terminalId) {
        try {
            assertTerminalBelongsToMe(terminalId);   // ★ ② 数据边界：别人的终端 → 403
            Map<String, Object> s = posAuthService.getShiftStatsForPanel(terminalId);
            return s == null ? Result.success(null, "没有进行中的班次") : Result.success(s);
        } catch (org.springframework.security.access.AccessDeniedException ade) {
            throw ade;   // ★ 越权必须冒泡成真 403，不能被下面的 catch(Exception) 吞成 500
        } catch (Exception e) {
            log.error("交班面板统计失败 terminalId={} err={}", terminalId, e.getMessage());
            return Result.error(500, "统计失败：" + e.getMessage());
        }
    }

    /** exceptions：tab=pending|processed（口径见 PosAuthServiceImpl#listShiftExceptions 注释） */
    @GetMapping("/exceptions")
    @PreAuthorize("hasAuthority('pos:shift:manage')")
    @Operation(summary = "交班面板异常单列表", description = "tab=pending|processed；processed 按 exception_ops 判定")
    public Result<List<Map<String, Object>>> exceptions(@RequestParam String terminalId,
                                                        @RequestParam(defaultValue = "pending") String tab) {
        try {
            assertTerminalBelongsToMe(terminalId);   // ★ ② 数据边界
            return Result.success(posAuthService.listShiftExceptions(terminalId, tab));
        } catch (org.springframework.security.access.AccessDeniedException ade) {
            throw ade;   // ★ 同上
        } catch (Exception e) {
            log.error("异常单列表失败 terminalId={} tab={} err={}", terminalId, tab, e.getMessage());
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /** 备用金只读 */
    @GetMapping("/opening-cash")
    @PreAuthorize("hasAuthority('pos:shift:manage')")
    @Operation(summary = "读取备用金", description = "只读当前班次 opening_cash")
    public Result<Map<String, Object>> getOpeningCash(@RequestParam String terminalId) {
        assertTerminalBelongsToMe(terminalId);   // ★ ② 数据边界
        PosShift shift = posAuthService.getMyCurrentShift(terminalId);
        if (shift == null) {
            return Result.success(null, "没有进行中的班次");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shiftId", shift.getShiftId());
        m.put("terminalId", shift.getTerminalId());
        m.put("openingCash", shift.getOpeningCash());
        return Result.success(m);
    }

    /**
     * 修改备用金：**501 未实现**（Lead 2026-10-29 裁定③）。
     * 属资金操作，需产品确认权限模型与审计要求，不臆造。
     */
    @PostMapping("/opening-cash")
    @PreAuthorize("hasAuthority('pos:shift:manage')")
    @Operation(summary = "修改备用金（未实现）", description = "501：资金操作，待产品确认"
            + "权限与审计要求")
    public ResponseEntity<Result<Void>> updateOpeningCash(@RequestParam String terminalId,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        log.warn("[METRIC] pos_shift_opening_cash_update_not_implemented terminalId={}", terminalId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Result.error(501, "修改备用金功能未实现：属资金操作，需产品确认权限模型与审计要求后再开放"));
    }

    /** 扫码收款：**501 未实现**（逻辑不明，不臆造） */
    @PostMapping("/{orderId}/scan")
    @PreAuthorize("hasAuthority('pos:order:pay')")
    @Operation(summary = "扫码收款（未实现）", description = "501：扫码逻辑待产品定义")
    public ResponseEntity<Result<Void>> scan(@PathVariable String orderId) {
        log.warn("[METRIC] pos_shift_scan_not_implemented orderId={}", orderId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Result.error(501, "扫码收款功能未实现：扫码合单/回调逻辑待产品定义，暂不开放"));
    }

    /**
     * ②（Lead 2026-10-30）：数据边界校验 —— 该终端上的活跃班次必须属于当前登录人。
     *
     * <p>修前实测：vcashier01 用别人的 terminalId 调 /stats → HTTP 200 + 别人的班次数据（越权读）。</p>
     *
     * <p>规则：「没权限」与「没数据」必须用**状态码**区分：
     * 无活跃班次 → 该终端没有数据（业务态，返回 null/空）；<b>有班次但不属于我 → 403</b>。</p>
     */
    private void assertTerminalBelongsToMe(String terminalId) {
        com.foodtraceability.entity.PosShift shift = posAuthService.getMyCurrentShift(terminalId);
        if (shift == null) {
            return;   // 该终端本来就没有进行中的班次 → 不是越权，交给业务返回 null
        }
        Long me = com.foodtraceability.utils.SecurityUtils.getCurrentUserId();
        String owner = shift.getEmployeeId();
        if (me == null || owner == null || !owner.equals(String.valueOf(me))) {
            log.warn("[METRIC] pos_shift_cross_terminal_denied terminalId={} me={} owner={}", terminalId, me, owner);
            throw new org.springframework.security.access.AccessDeniedException(
                    "该终端上的班次不属于当前用户，无权查看");
        }
    }
}
