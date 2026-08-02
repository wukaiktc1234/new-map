package com.foodtraceability.service.impl;

import com.foodtraceability.service.YoloVerificationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * YOLO 餐品识别服务默认实现（NoOp，未启用）
 * <p>当前为预留实现：返回 disabled 状态，不阻塞业务流程。</p>
 * <p>后续接入 YOLO 模型时，创建新的 YoloVerificationService 实现并通过 @Primary 或 @ConditionalOnProperty 替换。</p>
 */
@Service
public class NoOpYoloVerificationService implements YoloVerificationService {

    @Override
    public Map<String, Object> verifyTray(String trayCode, String snapshotUrl, Long storeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", false);
        result.put("result", "unknown");
        result.put("confidence", 0.0);
        result.put("verified", false);
        result.put("reason", "YOLO 服务未启用，使用人工识别结果");
        return result;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
