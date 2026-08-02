package com.foodtraceability.service;

import java.util.Map;

/**
 * YOLO 餐品识别服务接口（预留）
 * <p>当前为预留接口，后期接入 YOLO 模型辅助识别是否有餐品可出餐。</p>
 * <p>接入流程：
 * <ul>
 *   <li>1. 训练或加载 YOLO 模型（餐品检测数据集）</li>
 *   <li>2. 部署推理服务（Triton/TF-Serving/ONNX Runtime）</li>
 *   <li>3. 替换默认 NoOp 实现为真实推理调用</li>
 * </ul>
 * </p>
 */
public interface YoloVerificationService {

    /**
     * 识别托盘是否有餐品
     * @param trayCode 托盘码
     * @param snapshotUrl 摄像头拍照URL
     * @param storeId 门店ID
     * @return Map 含 enabled(是否启用)、result(has_food/no_food/unknown)、confidence(0-1)、verified(布尔)
     */
    Map<String, Object> verifyTray(String trayCode, String snapshotUrl, Long storeId);

    /**
     * YOLO 服务是否启用
     */
    boolean isEnabled();
}
