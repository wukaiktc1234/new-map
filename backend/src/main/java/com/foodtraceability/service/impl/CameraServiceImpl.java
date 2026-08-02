package com.foodtraceability.service.impl;

import com.foodtraceability.service.CameraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 摄像头服务实现（桩实现）
 * <p>当前为桩实现：拍照URL使用占位符，仅记录日志。后续接入真实摄像头SDK时替换 notifySnapshot 内部逻辑即可。</p>
 * <p>预留接入点：
 * <ul>
 *   <li>HTTP/IP 摄像头：调用 ONVIF 协议或厂商 SDK 拉取快照</li>
 *   <li>USB 摄像头：通过 OpenCV 抓帧</li>
 *   <li>图片存储：上传到 OSS/MinIO 后返回可访问URL</li>
 * </ul>
 * </p>
 */
@Service
public class CameraServiceImpl implements CameraService {

    private static final Logger logger = LoggerFactory.getLogger(CameraServiceImpl.class);
    private static final DateTimeFormatter URL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    public Map<String, Object> notifySnapshot(String trayCode, String kitchenOrderId, String scanType, Long storeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // TODO: 后续接入真实摄像头SDK时替换以下桩逻辑
            // 1. 通过 storeId 找到对应门店的摄像头设备
            // 2. 调用摄像头SDK抓拍
            // 3. 上传图片到 OSS/MinIO
            // 4. 返回可访问的URL
            String snapshotUrl = buildStubSnapshotUrl(trayCode, scanType, storeId);
            LocalDateTime snapshotTime = LocalDateTime.now();

            logger.info("摄像头拍照（桩实现）：trayCode={}, kitchenOrderId={}, scanType={}, storeId={}, url={}, time={}",
                    trayCode, kitchenOrderId, scanType, storeId, snapshotUrl, snapshotTime);

            result.put("success", true);
            result.put("snapshotUrl", snapshotUrl);
            result.put("snapshotTime", snapshotTime.toString());
            result.put("stub", true);
        } catch (Exception e) {
            logger.error("摄像头拍照失败：trayCode={}, scanType={}, error={}", trayCode, scanType, e.getMessage(), e);
            result.put("success", false);
            result.put("snapshotUrl", null);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getSnapshotHistory(String trayCode, String kitchenOrderId, int limit) {
        Map<String, Object> result = new HashMap<>();
        // TODO: 后续接入真实存储后从 tray_scan_record 表查询 camera_snapshot_url 字段
        result.put("items", java.util.Collections.emptyList());
        result.put("total", 0);
        result.put("note", "桩实现，未接入真实摄像头SDK");
        return result;
    }

    /**
     * 构造桩拍照URL（仅用于开发期占位）
     */
    private String buildStubSnapshotUrl(String trayCode, String scanType, Long storeId) {
        String timestamp = LocalDateTime.now().format(URL_FORMATTER);
        return String.format("/camera/snapshots/stub/%s/%s/%s_%s.jpg",
                storeId != null ? storeId : "0", trayCode, scanType, timestamp);
    }
}
