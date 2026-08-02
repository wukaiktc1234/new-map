package com.foodtraceability.service;

import java.util.Map;

/**
 * 摄像头服务接口
 * <p>托盘出餐状态变更时联动通知摄像头拍照留底</p>
 * <p>当前为桩实现（返回模拟URL），后续接入真实摄像头SDK时替换实现即可</p>
 */
public interface CameraService {

    /**
     * 通知摄像头拍照
     * @param trayCode 托盘码
     * @param kitchenOrderId 后厨订单ID（可空）
     * @param scanType 扫码类型：KITCHEN_IN/KITCHEN_OUT/SERVE
     * @param storeId 门店ID
     * @return Map 含 snapshotUrl、snapshotTime、success 字段
     */
    Map<String, Object> notifySnapshot(String trayCode, String kitchenOrderId, String scanType, Long storeId);

    /**
     * 查询摄像头拍照历史
     * @param trayCode 托盘码（可空）
     * @param kitchenOrderId 后厨订单ID（可空）
     * @param limit 返回条数
     */
    Map<String, Object> getSnapshotHistory(String trayCode, String kitchenOrderId, int limit);
}
