package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.CameraService;
import com.foodtraceability.service.FoodTraceCodeService;
import com.foodtraceability.service.TrayService;
import com.foodtraceability.service.YoloVerificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 托盘服务实现（5 状态机：idle → bound → making → ready → served → idle）
 * <p>
 * 状态流转：
 * <ul>
 *   <li>bindOrder:    idle → bound（POS 绑定）</li>
 *   <li>scanKitchenIn:  bound → making（后厨一次扫码，含防抖 ≥3s）</li>
 *   <li>scanKitchenOut: making → ready（后厨二次扫码，含防抖 ≥5s + 摄像头 + YOLO）</li>
 *   <li>scanServe:    ready → served → idle（取餐确认，含防抖 ≥2s）</li>
 * </ul>
 * </p>
 */
@Service
public class TrayServiceImpl implements TrayService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrayServiceImpl.class);

    /** 扫码防抖阈值（秒）：bound → making */
    private static final long DEBOUNCE_KITCHEN_IN_SECONDS = 3L;
    /** 扫码防抖阈值（秒）：making → ready */
    private static final long DEBOUNCE_KITCHEN_OUT_SECONDS = 5L;
    /** 扫码防抖阈值（秒）：ready → served */
    private static final long DEBOUNCE_SERVE_SECONDS = 2L;

    private final TrayMapper trayMapper;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final CallRecordMapper callRecordMapper;
    private final FoodTraceCodeService foodTraceCodeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final CameraService cameraService;
    private final YoloVerificationService yoloVerificationService;
    private final TrayScanRecordMapper trayScanRecordMapper;

    public TrayServiceImpl(final TrayMapper trayMapper,
                           final KitchenOrderMapper kitchenOrderMapper,
                           final CallRecordMapper callRecordMapper,
                           final FoodTraceCodeService foodTraceCodeService,
                           final SimpMessagingTemplate messagingTemplate,
                           final ApplicationEventPublisher eventPublisher,
                           final CameraService cameraService,
                           final YoloVerificationService yoloVerificationService,
                           final TrayScanRecordMapper trayScanRecordMapper) {
        this.trayMapper = trayMapper;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.callRecordMapper = callRecordMapper;
        this.foodTraceCodeService = foodTraceCodeService;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.cameraService = cameraService;
        this.yoloVerificationService = yoloVerificationService;
        this.trayScanRecordMapper = trayScanRecordMapper;
    }

    @Override
    public Tray findByTrayCode(String trayCode) {
        return trayMapper.findByTrayCode(trayCode);
    }

    @Override
    public List<Tray> findIdleTrays(int limit) {
        return trayMapper.findIdleTrays(limit);
    }

    @Override
    public List<Tray> findAll() {
        return trayMapper.selectList(new LambdaQueryWrapper<Tray>().eq(Tray::getDeleted, 0).orderByAsc(Tray::getTrayCode));
    }

    @Override
    public Tray createTray(Tray tray) {
        if (tray.getTrayCode() == null || tray.getTrayCode().isEmpty()) {
            tray.setTrayCode("TRAY" + System.currentTimeMillis());
        }
        if (tray.getStatus() == null) {
            tray.setStatus("idle");
        }
        if (tray.getTrayType() == null) {
            tray.setTrayType("standard");
        }
        if (tray.getUseCount() == null) {
            tray.setUseCount(0);
        }
        trayMapper.insert(tray);
        return tray;
    }

    /**
     * POS 绑定订单：托盘 idle → bound（不直接进入 making）
     * <p>kitchenOrder 保持 pending 状态，等待后厨一次扫码才进入 making</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Tray> bindOrder(String trayCode, String orderId, Long kitchenOrderId) {
        log.info("绑定托盘（POS端）: trayCode={}, orderId={}, kitchenOrderId={}", trayCode, orderId, kitchenOrderId);
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在: " + trayCode);
        }
        if (!"idle".equals(tray.getStatus())) {
            return Result.error("托盘状态不允许绑定，当前状态: " + tray.getStatus());
        }
        KitchenOrder kitchenOrder = null;
        if (kitchenOrderId != null) {
            kitchenOrder = kitchenOrderMapper.selectById(kitchenOrderId);
        } else if (orderId != null) {
            kitchenOrder = kitchenOrderMapper.selectOne(new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, orderId).eq(KitchenOrder::getDeleted, 0));
        }
        if (kitchenOrder == null) {
            return Result.error("订单不存在");
        }
        // 托盘进入 bound 状态（不直接改 kitchenOrder 为 making）
        trayMapper.bindOrder(tray.getId(), kitchenOrder.getOrderId(), kitchenOrder.getId());
        kitchenOrder.setTrayId(tray.getId());
        kitchenOrder.setTrayCode(trayCode);
        kitchenOrder.setTrayBindTime(LocalDateTime.now());
        kitchenOrder.setBoundTime(LocalDateTime.now());
        // 不再直接改为 making，等待后厨一次扫码
        kitchenOrderMapper.updateById(kitchenOrder);
        Tray updatedTray = trayMapper.selectById(tray.getId());
        messagingTemplate.convertAndSend("/topic/tray/bind", updatedTray);
        messagingTemplate.convertAndSend("/topic/orders/status", kitchenOrder);
        log.info("托盘绑定成功: {} -> {}（等待后厨一次扫码进入 making）", trayCode, kitchenOrder.getOrderNumber());
        return Result.success(updatedTray);
    }

    /**
     * 后厨一次扫码：托盘 bound → making（含防抖 ≥3s）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Tray> scanKitchenIn(String trayCode, Long scanDeviceId, String scanDeviceCode,
                                       String operatorId, String operatorName) {
        log.info("后厨一次扫码: trayCode={}, scanDeviceId={}", trayCode, scanDeviceId);
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在: " + trayCode);
        }
        if (!"bound".equals(tray.getStatus())) {
            return Result.error("托盘状态不允许后厨一次扫码，当前状态: " + tray.getStatus() + "（需要 bound 状态）");
        }

        // 防抖判断
        LocalDateTime now = LocalDateTime.now();
        DebounceResult debounce = checkDebounce(tray, "KITCHEN_IN", DEBOUNCE_KITCHEN_IN_SECONDS, now);
        if (debounce.isDebounced()) {
            recordScan(tray, "KITCHEN_IN", "bound", "bound", now, debounce.prevScanTime(),
                    scanDeviceId, scanDeviceCode, operatorId, operatorName, null, null, null, true, "防抖忽略");
            log.info("后厨一次扫码被防抖忽略: trayCode={}, prevScanTime={}, 阈值={}s", trayCode, debounce.prevScanTime(), DEBOUNCE_KITCHEN_IN_SECONDS);
            return Result.error("扫码过于频繁，已忽略（防抖阈值 " + DEBOUNCE_KITCHEN_IN_SECONDS + "s）");
        }

        // 状态变更：bound → making
        trayMapper.updateToMaking(tray.getId(), scanDeviceId);
        // 更新 kitchenOrder 状态
        if (tray.getCurrentKitchenOrderId() != null) {
            KitchenOrder order = kitchenOrderMapper.selectById(tray.getCurrentKitchenOrderId());
            if (order != null) {
                order.setStatus("making");
                order.setMakingStartTime(now);
                if (order.getMakeStartTime() == null) {
                    order.setMakeStartTime(now);
                }
                kitchenOrderMapper.updateById(order);
                messagingTemplate.convertAndSend("/topic/orders/status", order);
            }
        }
        Tray updatedTray = trayMapper.selectById(tray.getId());
        recordScan(tray, "KITCHEN_IN", "bound", "making", now, debounce.prevScanTime(),
                scanDeviceId, scanDeviceCode, operatorId, operatorName, null, null, null, false, null);
        messagingTemplate.convertAndSend("/topic/tray/status", updatedTray);
        log.info("后厨一次扫码成功: trayCode={}, bound → making", trayCode);
        return Result.success(updatedTray);
    }

    /**
     * 后厨二次扫码：托盘 making → ready（含防抖 ≥5s + 摄像头 + YOLO）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Tray> scanKitchenOut(String trayCode, Long scanDeviceId, String scanDeviceCode,
                                        String operatorId, String operatorName) {
        log.info("后厨二次扫码: trayCode={}, scanDeviceId={}", trayCode, scanDeviceId);
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在: " + trayCode);
        }
        if (!"making".equals(tray.getStatus())) {
            return Result.error("托盘状态不允许后厨二次扫码，当前状态: " + tray.getStatus() + "（需要 making 状态）");
        }

        // 防抖判断
        LocalDateTime now = LocalDateTime.now();
        DebounceResult debounce = checkDebounce(tray, "KITCHEN_OUT", DEBOUNCE_KITCHEN_OUT_SECONDS, now);
        if (debounce.isDebounced()) {
            recordScan(tray, "KITCHEN_OUT", "making", "making", now, debounce.prevScanTime(),
                    scanDeviceId, scanDeviceCode, operatorId, operatorName, null, null, null, true, "防抖忽略");
            log.info("后厨二次扫码被防抖忽略: trayCode={}, prevScanTime={}, 阈值={}s", trayCode, debounce.prevScanTime(), DEBOUNCE_KITCHEN_OUT_SECONDS);
            return Result.error("扫码过于频繁，已忽略（防抖阈值 " + DEBOUNCE_KITCHEN_OUT_SECONDS + "s）");
        }

        // 联动摄像头拍照
        String snapshotUrl = null;
        try {
            String kitchenOrderIdStr = tray.getCurrentKitchenOrderId() != null ? String.valueOf(tray.getCurrentKitchenOrderId()) : null;
            Map<String, Object> cameraResult = cameraService.notifySnapshot(trayCode, kitchenOrderIdStr, "KITCHEN_OUT", tray.getStoreId());
            if (Boolean.TRUE.equals(cameraResult.get("success"))) {
                snapshotUrl = (String) cameraResult.get("snapshotUrl");
            }
        } catch (Exception e) {
            log.warn("摄像头联动失败（不阻塞业务）: trayCode={}, error={}", trayCode, e.getMessage());
        }

        // YOLO 识别（预留，未启用则跳过）
        String yoloResult = null;
        java.math.BigDecimal yoloConfidence = null;
        if (yoloVerificationService.isEnabled() && snapshotUrl != null) {
            try {
                Map<String, Object> yoloResultMap = yoloVerificationService.verifyTray(trayCode, snapshotUrl, tray.getStoreId());
                yoloResult = (String) yoloResultMap.get("result");
                Object confObj = yoloResultMap.get("confidence");
                if (confObj instanceof java.math.BigDecimal) {
                    yoloConfidence = (java.math.BigDecimal) confObj;
                } else if (confObj instanceof Number) {
                    yoloConfidence = java.math.BigDecimal.valueOf(((Number) confObj).doubleValue());
                }
            } catch (Exception e) {
                log.warn("YOLO 识别失败（不阻塞业务）: trayCode={}, error={}", trayCode, e.getMessage());
                yoloResult = "unknown";
            }
        }

        // 状态变更：making → ready
        trayMapper.updateToReady(tray.getId(), scanDeviceId, snapshotUrl);
        // 更新 kitchenOrder 状态
        if (tray.getCurrentKitchenOrderId() != null) {
            KitchenOrder order = kitchenOrderMapper.selectById(tray.getCurrentKitchenOrderId());
            if (order != null) {
                order.setStatus("ready");
                order.setReadyTime(now);
                order.setMakeCompleteTime(now);
                order.setLastCameraSnapshotUrl(snapshotUrl);
                kitchenOrderMapper.updateById(order);
                messagingTemplate.convertAndSend("/topic/orders/status", order);
            }
        }
        Tray updatedTray = trayMapper.selectById(tray.getId());
        recordScan(tray, "KITCHEN_OUT", "making", "ready", now, debounce.prevScanTime(),
                scanDeviceId, scanDeviceCode, operatorId, operatorName, snapshotUrl, yoloResult, yoloConfidence, false, null);
        messagingTemplate.convertAndSend("/topic/tray/status", updatedTray);
        log.info("后厨二次扫码成功: trayCode={}, making → ready, snapshotUrl={}, yolo={}", trayCode, snapshotUrl, yoloResult);
        return Result.success(updatedTray);
    }

    /**
     * 取餐口确认：托盘 ready → served → idle（含防抖 ≥2s）
     * <p>完成订单、生成追溯码、创建呼叫记录、发布订单完成事件</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<KitchenOrder> scanServe(String trayCode, String operatorId, String operatorName) {
        log.info("取餐口确认: trayCode={}", trayCode);
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在: " + trayCode);
        }
        if (!"ready".equals(tray.getStatus())) {
            return Result.error("托盘状态不允许取餐确认，当前状态: " + tray.getStatus() + "（需要 ready 状态）");
        }

        // 防抖判断
        LocalDateTime now = LocalDateTime.now();
        DebounceResult debounce = checkDebounce(tray, "SERVE", DEBOUNCE_SERVE_SECONDS, now);
        if (debounce.isDebounced()) {
            recordScan(tray, "SERVE", "ready", "ready", now, debounce.prevScanTime(),
                    null, null, operatorId, operatorName, null, null, null, true, "防抖忽略");
            log.info("取餐口确认被防抖忽略: trayCode={}, prevScanTime={}, 阈值={}s", trayCode, debounce.prevScanTime(), DEBOUNCE_SERVE_SECONDS);
            return Result.error("扫码过于频繁，已忽略（防抖阈值 " + DEBOUNCE_SERVE_SECONDS + "s）");
        }

        Long kitchenOrderId = tray.getCurrentKitchenOrderId();
        if (kitchenOrderId == null) {
            return Result.error("托盘未绑定后厨订单");
        }
        KitchenOrder order = kitchenOrderMapper.selectById(kitchenOrderId);
        if (order == null) {
            return Result.error("关联订单不存在");
        }

        // 状态变更：ready → served
        trayMapper.updateToServed(tray.getId(), null);
        order.setStatus("served");
        order.setServeTime(now);
        order.setServedTime(now);
        kitchenOrderMapper.updateById(order);

        // 记录扫码
        recordScan(tray, "SERVE", "ready", "served", now, debounce.prevScanTime(),
                null, null, operatorId, operatorName, null, null, null, false, null);

        // 生成食品追溯码
        try {
            foodTraceCodeService.generateForOrder(order.getKitchenOrderId());
            log.info("已生成食品追溯码: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.warn("生成食品追溯码失败: {}", e.getMessage());
        }

        // 创建待取餐呼叫记录
        createCallRecord(order);

        // 释放托盘：served → idle
        trayMapper.releaseTray(tray.getId());
        Tray updatedTray = trayMapper.selectById(tray.getId());
        messagingTemplate.convertAndSend("/topic/orders/status", order);
        messagingTemplate.convertAndSend("/topic/tray/status", updatedTray);

        // 发布订单完成事件
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setEventId("EVT" + System.currentTimeMillis());
        event.setOrderId(order.getOrderId());
        event.setOrderNumber(order.getOrderNumber());
        event.setOrderType(order.getOrderType());
        event.setStoreId(order.getStoreId());
        event.setStoreName(order.getStoreName());
        event.setTableNumber(order.getTableNumber());
        event.setCompleteTime(now);
        event.setActualAmount(order.getTotalAmount());
        event.setOrderAmount(order.getTotalAmount());
        eventPublisher.publishEvent(event);

        log.info("取餐确认成功: 托盘={}, 订单={}, 出餐时间={}", trayCode, order.getOrderNumber(), order.getServeTime());
        return Result.success(order);
    }

    /**
     * 防抖判断：检查上次同类型扫码距今是否超过阈值
     */
    private DebounceResult checkDebounce(Tray tray, String scanType, long thresholdSeconds, LocalDateTime now) {
        TrayScanRecord lastScan = trayScanRecordMapper.findLastEffectiveScan(tray.getId(), scanType);
        if (lastScan == null || lastScan.getScanTime() == null) {
            return new DebounceResult(false, null);
        }
        long elapsedSeconds = Duration.between(lastScan.getScanTime(), now).getSeconds();
        if (elapsedSeconds < thresholdSeconds) {
            return new DebounceResult(true, lastScan.getScanTime());
        }
        return new DebounceResult(false, lastScan.getScanTime());
    }

    /**
     * 记录扫码到 tray_scan_record 表（含防抖忽略的记录也写入留痕）
     */
    private void recordScan(Tray tray, String scanType, String fromStatus, String toStatus,
                            LocalDateTime scanTime, LocalDateTime prevScanTime,
                            Long scanDeviceId, String scanDeviceCode,
                            String operatorId, String operatorName,
                            String cameraSnapshotUrl, String yoloResult, java.math.BigDecimal yoloConfidence,
                            boolean isDebounced, String remark) {
        try {
            TrayScanRecord record = new TrayScanRecord();
            record.setTrayId(tray.getId());
            record.setTrayCode(tray.getTrayCode());
            record.setKitchenOrderId(tray.getCurrentKitchenOrderId() != null ? String.valueOf(tray.getCurrentKitchenOrderId()) : null);
            record.setScanDeviceId(scanDeviceId);
            record.setScanDeviceCode(scanDeviceCode);
            record.setScanType(scanType);
            record.setFromStatus(fromStatus);
            record.setToStatus(toStatus);
            record.setScanTime(scanTime);
            record.setPrevScanTime(prevScanTime);
            record.setIsDebounced(isDebounced ? 1 : 0);
            record.setCameraSnapshotUrl(cameraSnapshotUrl);
            record.setYoloResult(yoloResult);
            record.setYoloConfidence(yoloConfidence);
            record.setOperatorId(operatorId != null && !operatorId.isEmpty() ? Long.valueOf(operatorId) : null);
            record.setOperatorName(operatorName);
            record.setStoreId(tray.getStoreId());
            record.setRemark(remark);
            trayScanRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("记录扫码日志失败（不阻塞业务）: trayCode={}, scanType={}, error={}", tray.getTrayCode(), scanType, e.getMessage());
        }
    }

    private void createCallRecord(KitchenOrder order) {
        try {
            CallRecord existing = callRecordMapper.findByOrderId(order.getOrderId());
            if (existing != null) {
                return;
            }
            CallRecord record = new CallRecord();
            record.setOrderId(order.getOrderId());
            record.setOrderNumber(order.getOrderNumber());
            record.setTableNumber(order.getTableNumber());
            record.setOrderType(getOrderTypeName(order.getOrderType()));
            record.setItemCount(order.getTotalDishes() != null ? order.getTotalDishes() : 0);
            record.setStatus("pending");
            record.setCallCount(0);
            record.setCreateTime(LocalDateTime.now());
            callRecordMapper.insert(record);
            messagingTemplate.convertAndSend("/topic/call-number/new", record);
            log.info("已创建待取餐记录: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.warn("创建待取餐记录失败: {}", e.getMessage());
        }
    }

    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return "堂食";
        return switch (orderType) {
            case 0 -> "堂食";
            case 1 -> "外卖";
            case 2 -> "自提";
            default -> "堂食";
        };
    }

    @Override
    public Tray releaseTray(String trayCode) {
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return null;
        }
        trayMapper.releaseTray(tray.getId());
        return trayMapper.selectById(tray.getId());
    }

    @Override
    public Tray startCleaning(String trayCode) {
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return null;
        }
        trayMapper.startCleaning(tray.getId());
        return trayMapper.selectById(tray.getId());
    }

    @Override
    public Tray finishCleaning(String trayCode) {
        Tray tray = trayMapper.findByTrayCode(trayCode);
        if (tray == null) {
            return null;
        }
        trayMapper.finishCleaning(tray.getId());
        return trayMapper.selectById(tray.getId());
    }

    @Override
    public int countByStatus(String status) {
        return trayMapper.countByStatus(status);
    }

    @Override
    public void batchCreateTrays(int count, String prefix, String trayType, Long storeId, String storeName) {
        if (prefix == null || prefix.isEmpty()) {
            prefix = "TRAY";
        }
        if (trayType == null || trayType.isEmpty()) {
            trayType = "standard";
        }
        String finalPrefix = prefix;
        String finalTrayType = trayType;
        for (int i = 1; i <= count; i++) {
            Tray tray = new Tray();
            tray.setTrayCode(String.format("%s%03d", finalPrefix, i));
            tray.setTrayName(i + "号托盘");
            tray.setTrayType(finalTrayType);
            tray.setStatus("idle");
            tray.setStoreId(storeId);
            tray.setStoreName(storeName);
            tray.setUseCount(0);
            trayMapper.insert(tray);
        }
        log.info("批量创建托盘: {} 个, 前缀: {}, 类型: {}", count, prefix, trayType);
    }

    /**
     * 防抖判断结果
     */
    private record DebounceResult(boolean isDebounced, LocalDateTime prevScanTime) {
    }
}
