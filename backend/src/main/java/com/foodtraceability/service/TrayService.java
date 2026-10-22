package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Tray;
import com.foodtraceability.entity.KitchenOrder;

import java.util.List;

public interface TrayService {

    Tray findByTrayCode(String trayCode);

    List<Tray> findIdleTrays(int limit);

    /** 按门店查询空闲托盘（KDS 端使用，避免跨店误选） */
    List<Tray> findIdleTraysByStore(Long storeId, int limit);

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
    /**
     * 手动流转托盘状态（兜底 / 测试轨，与扫码自动轨并存）。
     *
     * <p>2026-10-23 设计：后端原本只有"扫码驱动"的分步入口（scan-kitchen-in / scan-kitchen-out / scan-serve），
     * 没有扫码设备时无法推进状态，测试与现场兜底都走不通。此入口允许人工推进状态，
     * 但强制要求填写理由，并写入 {@code tray_scan_record}（scan_type=MANUAL）留痕。</p>
     *
     * <p><b>允许的目标状态</b>：making / ready / cleaning / idle。
     * <b>不允许</b>直接置 served —— 该跃迁承载扣料与追溯码生成，必须走 {@link #scanServe}。</p>
     *
     * @param trayCode      托盘码
     * @param targetStatus  目标状态
     * @param reason        操作理由（必填，写入审计）
     * @param operatorId    操作人ID（可空）
     * @param operatorName  操作人姓名（可空）
     */
    Result<Tray> manualTransition(String trayCode, String targetStatus, String reason,
                                  String operatorId, String operatorName);
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
