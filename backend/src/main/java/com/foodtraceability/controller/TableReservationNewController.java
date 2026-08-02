package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.order.ReservationStatsVO;
import com.foodtraceability.entity.TableReservationNew;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.TableReservationNewService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 预约管理控制器
 *
 * <p>提供预约记录的增删改查及状态流转 API。</p>
 *
 * <h2>安全加固说明</h2>
 * <ul>
 *   <li>类级别 {@code @PreAuthorize} 限制访问角色为 ADMIN / MANAGER / HOST / CASHIER</li>
 *   <li>状态流转接口（confirm/arrive/cancel）的操作人 ID 从 SecurityContext 提取，
 *       禁止前端通过 {@code @RequestParam} 伪造操作人</li>
 *   <li>更新接口加 {@code @Valid} 触发 DTO 校验</li>
 *   <li>getById 在记录不存在时返回 {@link Result#error} 而非抛异常</li>
 *   <li>/stats 接入真实数据库统计查询</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/reservations")
@Tag(name = "预约管理", description = "桌台预约的增删改查及状态管理接口")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_HOST', 'ROLE_CASHIER')")
public class TableReservationNewController {

    private final TableReservationNewService tableReservationNewService;

    public TableReservationNewController(TableReservationNewService tableReservationNewService) {
        this.tableReservationNewService = tableReservationNewService;
    }

    /** 创建预约 */
    @PostMapping
    @Operation(summary = "创建预约", description = "新增一条桌台预约记录")
    public Result<TableReservationNew> create(@Valid @RequestBody TableReservationNew reservation) {
        return Result.success(tableReservationNewService.create(reservation));
    }

    /** 更新预约 */
    @PutMapping("/{reservationId}")
    @Operation(summary = "更新预约", description = "根据ID更新预约信息")
    public Result<TableReservationNew> update(
            @PathVariable Long reservationId,
            @Valid @RequestBody TableReservationNew reservation) {
        reservation.setReservationId(reservationId);
        return Result.success(tableReservationNewService.update(reservation));
    }

    /**
     * 预约统计
     * <p>字面量路径必须在 /{reservationId} 之前定义，避免路径变量冲突</p>
     */
    @GetMapping("/stats")
    @Operation(summary = "预约统计", description = "获取预约汇总统计数据（今日预约/待确认/已到店/取消率）")
    public Result<ReservationStatsVO> getStats() {
        try {
            return Result.success(tableReservationNewService.getStats(), "获取预约统计成功");
        } catch (Exception e) {
            return Result.error("获取预约统计失败：" + e.getMessage());
        }
    }

    /** 获取预约详情 */
    @GetMapping("/{reservationId}")
    @Operation(summary = "获取预约详情", description = "根据ID获取预约详细信息")
    public Result<TableReservationNew> getById(@PathVariable Long reservationId) {
        TableReservationNew reservation = tableReservationNewService.getById(reservationId);
        if (reservation == null) {
            return Result.error("预约记录不存在，ID: " + reservationId);
        }
        return Result.success(reservation);
    }

    /** 分页查询预约 */
    @GetMapping
    @Operation(summary = "分页查询预约", description = "支持按状态、关键词、日期范围筛选的分页查询")
    public Result<Page<TableReservationNew>> queryPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateEnd) {
        Page<TableReservationNew> pageParam = new Page<>(page, size);
        return Result.success(tableReservationNewService.queryPage(pageParam, status, keyword, date, dateEnd));
    }

    /** 查询指定日期的预约 */
    @GetMapping("/by-date/{date}")
    @Operation(summary = "按日期查询预约", description = "查询指定日期的所有待处理和已确认预约")
    public Result<java.util.List<TableReservationNew>> listByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(tableReservationNewService.listByDate(date));
    }

    /**
     * 确认预约
     * <p>操作人 ID 从 SecurityContext 提取，禁止前端通过 query 参数伪造。</p>
     */
    @PostMapping("/{reservationId}/confirm")
    @Operation(summary = "确认预约", description = "将待确认预约标记为已确认，操作人从登录态自动提取")
    public Result<Void> confirm(@PathVariable Long reservationId) {
        Long operatorId = requireCurrentOperatorId();
        if (operatorId == null) {
            return Result.error("未登录或登录态已失效，无法获取操作人信息");
        }
        tableReservationNewService.confirm(reservationId, operatorId);
        return Result.success();
    }

    /**
     * 标记到店
     * <p>操作人 ID 从 SecurityContext 提取。</p>
     */
    @PostMapping("/{reservationId}/arrive")
    @Operation(summary = "标记到店", description = "将已确认预约标记为已到店，操作人从登录态自动提取")
    public Result<Void> arrive(@PathVariable Long reservationId) {
        Long operatorId = requireCurrentOperatorId();
        if (operatorId == null) {
            return Result.error("未登录或登录态已失效，无法获取操作人信息");
        }
        tableReservationNewService.arrive(reservationId, operatorId);
        return Result.success();
    }

    /**
     * 取消预约
     * <p>操作人 ID 从 SecurityContext 提取；取消原因 reason 为可选参数。</p>
     */
    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "取消预约", description = "取消指定预约记录，操作人从登录态自动提取")
    public Result<Void> cancel(
            @PathVariable Long reservationId,
            @RequestParam(required = false, defaultValue = "") String reason) {
        Long operatorId = requireCurrentOperatorId();
        if (operatorId == null) {
            return Result.error("未登录或登录态已失效，无法获取操作人信息");
        }
        tableReservationNewService.cancel(reservationId, reason, operatorId);
        return Result.success();
    }

    /**
     * 从 SecurityContext 提取当前操作人 ID
     * <p>兼容 SecurityUser（JWT 链路）和 User（旧版链路），统一返回 Long 类型。</p>
     *
     * @return 当前操作人 ID；若未登录或 SecurityUser.userId 不可解析返回 null
     */
    private Long requireCurrentOperatorId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * 内部辅助：直接从 Principal 解析 SecurityUser（备用方法）
     * <p>当 SecurityUtils 行为不符合预期时，可直接调用此方法。
     * 已在 requireCurrentOperatorId 中通过 SecurityUtils 覆盖，此方法保留作为扩展点。</p>
     */
    @SuppressWarnings("unused")
    private SecurityUser extractSecurityUser() {
        return null; // 委托给 SecurityUtils，保留扩展点
    }
}
