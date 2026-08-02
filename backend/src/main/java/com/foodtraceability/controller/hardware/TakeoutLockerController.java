package com.foodtraceability.controller.hardware;

import com.foodtraceability.service.TakeoutLockerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 外卖取餐柜控制器
 * @author example
 * @since 2026-01-08
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             取餐柜设备能力应封装为独立的 DeviceDriver 实现类（如 LockerDriver），
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的格口分配和取餐逻辑应下沉为 DeviceDriver 实现类，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/takeout-locker")
@Tag(name = "取餐柜管理", description = "取餐柜设备连接、格口分配和取餐管理接口")
public class TakeoutLockerController {


    public TakeoutLockerController(TakeoutLockerService takeoutLockerService) {
        this.takeoutLockerService = takeoutLockerService;
    }

    private final TakeoutLockerService takeoutLockerService;

    /**
     * 连接取餐柜
     * @param lockerId 取餐柜ID
     * @return 连接结果
     */
    @PostMapping("/locker/{lockerId}/connect")
    @Operation(summary = "连接取餐柜")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> connectLocker(@PathVariable Long lockerId) {
        return takeoutLockerService.connectLocker(lockerId);
    }

    /**
     * 断开取餐柜连接
     * @param lockerId 取餐柜ID
     * @return 断开结果
     */
    @PostMapping("/locker/{lockerId}/disconnect")
    @Operation(summary = "断开取餐柜连接")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> disconnectLocker(@PathVariable Long lockerId) {
        return takeoutLockerService.disconnectLocker(lockerId);
    }

    /**
     * 获取取餐柜状态
     * @param lockerId 取餐柜ID
     * @return 取餐柜状态
     */
    @GetMapping("/locker/{lockerId}/status")
    @Operation(summary = "获取取餐柜状态")
    @PreAuthorize("hasAuthority('device:locker:view') or hasAuthority('*')")
    public Map<String, Object> getLockerStatus(@PathVariable Long lockerId) {
        return takeoutLockerService.getLockerStatus(lockerId);
    }

    /**
     * 分配格子
     * @param lockerId 取餐柜ID
     * @param orderId 订单ID
     * @param slotType 格子类型
     * @param putOperator 放入人
     * @param expectedPickupTime 预计取餐时间
     * @return 分配结果
     */
    @PostMapping("/assign-slot")
    @Operation(summary = "分配格口")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> assignSlot(@RequestParam Long lockerId,
                                         @RequestParam Long orderId,
                                         @RequestParam String slotType,
                                         @RequestParam String putOperator,
                                         @RequestParam String expectedPickupTime) {
        return takeoutLockerService.assignSlot(lockerId, orderId, slotType, 
                                             putOperator, expectedPickupTime);
    }

    /**
     * 放入外卖
     * @param slotId 格子ID
     * @param orderId 订单ID
     * @param putOperator 放入人
     * @return 放入结果
     */
    @PostMapping("/put-takeout")
    @Operation(summary = "存入外卖")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> putTakeout(@RequestParam Long slotId,
                                         @RequestParam Long orderId,
                                         @RequestParam String putOperator) {
        return takeoutLockerService.putTakeout(slotId, orderId, putOperator);
    }

    /**
     * 取餐
     * @param pickupCode 取餐码
     * @param pickupOperator 取餐人
     * @return 取餐结果
     */
    @PostMapping("/pickup-takeout")
    @Operation(summary = "取走外卖")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> pickupTakeout(@RequestParam String pickupCode,
                                           @RequestParam String pickupOperator) {
        return takeoutLockerService.pickupTakeout(pickupCode, pickupOperator);
    }

    /**
     * 批量获取取餐柜状态
     * @return 取餐柜状态列表
     */
    @GetMapping("/lockers/status")
    @Operation(summary = "批量获取取餐柜状态")
    @PreAuthorize("hasAuthority('device:locker:view') or hasAuthority('*')")
    public Map<String, Object> batchGetLockerStatus() {
        return takeoutLockerService.batchGetLockerStatus();
    }

    /**
     * 获取取餐柜格子状态
     * @param lockerId 取餐柜ID
     * @return 格子状态列表
     */
    @GetMapping("/locker/{lockerId}/slots/status")
    @Operation(summary = "获取格口状态")
    @PreAuthorize("hasAuthority('device:locker:view') or hasAuthority('*')")
    public Map<String, Object> getLockerSlotsStatus(@PathVariable Long lockerId) {
        return takeoutLockerService.getLockerSlotsStatus(lockerId);
    }

    /**
     * 释放格子
     * @param slotId 格子ID
     * @param operator 操作人
     * @return 释放结果
     */
    @PostMapping("/release-slot")
    @Operation(summary = "释放格口")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> releaseSlot(@RequestParam Long slotId,
                                         @RequestParam String operator) {
        return takeoutLockerService.releaseSlot(slotId, operator);
    }

    /**
     * 重置取餐柜今日使用计数
     * @param lockerId 取餐柜ID
     * @return 重置结果
     */
    @PostMapping("/locker/{lockerId}/reset-usage")
    @Operation(summary = "重置每日使用计数")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> resetDailyUsageCount(@PathVariable Long lockerId) {
        return takeoutLockerService.resetDailyUsageCount(lockerId);
    }

    /**
     * 测试取餐柜
     * @param lockerId 取餐柜ID
     * @return 测试结果
     */
    @PostMapping("/locker/{lockerId}/test")
    @Operation(summary = "测试取餐柜")
    @PreAuthorize("hasAuthority('device:locker:manage') or hasAuthority('*')")
    public Map<String, Object> testLocker(@PathVariable Long lockerId) {
        return takeoutLockerService.testLocker(lockerId);
    }
}
