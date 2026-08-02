package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Tray;
import com.foodtraceability.entity.KitchenOrder;

import java.util.List;

public interface TrayService {

    Tray findByTrayCode(String trayCode);

    List<Tray> findIdleTrays(int limit);

    List<Tray> findAll();

    Tray createTray(Tray tray);

    /**
     * 绑定订单（POS端）：托盘 idle → bound，订单 pending → 保持（等待后厨扫码才进入 making）
     */
    Result<Tray> bindOrder(String trayCode, String orderId, Long kitchenOrderId);

    /**
     * 后厨一次扫码：托盘 bound → making（含防抖 ≥3s）
     * <p>触发条件：托盘经过后厨入口扫码设备</p>
     * <p>语义：后厨接过托盘，可以开始制作餐品</p>
     */
    Result<Tray> scanKitchenIn(String trayCode, Long scanDeviceId, String scanDeviceCode,
                                String operatorId, String operatorName);

    /**
     * 后厨二次扫码：托盘 making → ready（含防抖 ≥5s）
     * <p>触发条件：托盘经过后厨出口扫码设备</p>
     * <p>语义：制作完成，已放入托盘，推出窗口</p>
     * <p>联动：摄像头拍照留底 + YOLO 识别（如启用）</p>
     */
    Result<Tray> scanKitchenOut(String trayCode, Long scanDeviceId, String scanDeviceCode,
                                 String operatorId, String operatorName);

    /**
     * 取餐口确认：托盘 ready → served（含防抖 ≥2s）
     * <p>触发条件：顾客取餐后人工确认</p>
     * <p>完成后托盘释放回 idle 状态</p>
     */
    Result<KitchenOrder> scanServe(String trayCode, String operatorId, String operatorName);

    Tray releaseTray(String trayCode);

    Tray startCleaning(String trayCode);

    Tray finishCleaning(String trayCode);

    int countByStatus(String status);

    void batchCreateTrays(int count, String prefix, String trayType, Long storeId, String storeName);
}
