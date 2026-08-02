package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.TakeoutLocker;
import com.foodtraceability.entity.LockerSlot;
import java.util.Map;

/**
 * 外卖取餐柜服务接口
 * @author example
 * @since 2026-01-08
 */
public interface TakeoutLockerService extends IService<TakeoutLocker> {

    /**
     * 连接取餐柜
     * @param lockerId 取餐柜ID
     * @return 连接结果
     */
    Map<String, Object> connectLocker(Long lockerId);

    /**
     * 断开取餐柜连接
     * @param lockerId 取餐柜ID
     * @return 断开结果
     */
    Map<String, Object> disconnectLocker(Long lockerId);

    /**
     * 获取取餐柜状态
     * @param lockerId 取餐柜ID
     * @return 取餐柜状态
     */
    Map<String, Object> getLockerStatus(Long lockerId);

    /**
     * 分配格子
     * @param lockerId 取餐柜ID
     * @param orderId 订单ID
     * @param slotType 格子类型
     * @param putOperator 放入人
     * @param expectedPickupTime 预计取餐时间
     * @return 分配结果
     */
    Map<String, Object> assignSlot(Long lockerId, Long orderId, String slotType, 
                                  String putOperator, String expectedPickupTime);

    /**
     * 放入外卖
     * @param slotId 格子ID
     * @param orderId 订单ID
     * @param putOperator 放入人
     * @return 放入结果
     */
    Map<String, Object> putTakeout(Long slotId, Long orderId, String putOperator);

    /**
     * 取餐
     * @param pickupCode 取餐码
     * @param pickupOperator 取餐人
     * @return 取餐结果
     */
    Map<String, Object> pickupTakeout(String pickupCode, String pickupOperator);

    /**
     * 批量获取取餐柜状态
     * @return 取餐柜状态列表
     */
    Map<String, Object> batchGetLockerStatus();

    /**
     * 获取取餐柜格子状态
     * @param lockerId 取餐柜ID
     * @return 格子状态列表
     */
    Map<String, Object> getLockerSlotsStatus(Long lockerId);

    /**
     * 释放格子
     * @param slotId 格子ID
     * @param operator 操作人
     * @return 释放结果
     */
    Map<String, Object> releaseSlot(Long slotId, String operator);

    /**
     * 重置取餐柜今日使用计数
     * @param lockerId 取餐柜ID
     * @return 重置结果
     */
    Map<String, Object> resetDailyUsageCount(Long lockerId);

    /**
     * 测试取餐柜
     * @param lockerId 取餐柜ID
     * @return 测试结果
     */
    Map<String, Object> testLocker(Long lockerId);
}
