package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.YoloRecognitionResultDTO;
import com.foodtraceability.service.YoloProductRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * YOLO 商品识别控制器（POS 端辅助收银）
 * <p>
 * 用于 POS 收银场景：摄像头拍摄收银台商品，调用 YOLO 模型识别，
 * 返回商品列表供收银员确认后一键加入购物车，提升多商品结算效率。
 * </p>
 * <p>
 * S1 修复：类级别 @PreAuthorize 统一要求 POS 收银角色。
 * </p>
 */
@RestController
@RequestMapping("/v1/pos/yolo")
@Tag(name = "YOLO 辅助收银API", description = "YOLO 商品识别接口")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_POS_OPERATOR', '*')")
public class YoloProductRecognitionController {

    private final YoloProductRecognitionService recognitionService;

    public YoloProductRecognitionController(YoloProductRecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    /**
     * 识别商品（base64 图片）
     * <p>
     * 前端将摄像头拍摄的图片转为 base64 后传入。
     * 服务未启用时返回 disabled 状态，前端应提示并禁用相关功能。
     * </p>
     *
     * @param body 请求体，含 base64 图片和门店ID
     * @return 识别结果
     */
    @PostMapping("/recognize")
    @Operation(summary = "识别商品（base64 图片）")
    public Result<YoloRecognitionResultDTO> recognize(@RequestBody YoloRecognizeRequest body) {
        return recognitionService.recognizeProducts(body.getImage(), body.getStoreId());
    }

    /**
     * 查询 YOLO 服务状态
     */
    @GetMapping("/status")
    @Operation(summary = "查询 YOLO 服务状态")
    public Result<YoloStatusResponse> getStatus() {
        YoloStatusResponse status = new YoloStatusResponse();
        status.setEnabled(recognitionService.isEnabled());
        return Result.success(status);
    }

    /** 识别请求体 */
    public static class YoloRecognizeRequest {
        private String image;
        private String storeId;

        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }
    }

    /** 服务状态响应 */
    public static class YoloStatusResponse {
        private boolean enabled;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
