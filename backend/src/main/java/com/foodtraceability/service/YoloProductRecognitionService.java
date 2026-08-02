package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.YoloRecognitionResultDTO;

/**
 * YOLO 商品识别服务接口（POS 端多商品快速结算）
 * <p>
 * 用于 POS 收银场景：摄像头拍摄收银台商品，YOLO 模型识别后返回商品列表，
 * 收银员确认后一键加入购物车，提升多商品场景的结算效率。
 * </p>
 * <p>
 * 与 {@link YoloVerificationService} 的区别：
 * <ul>
 *   <li>YoloVerificationService：托盘出餐验证（单餐品/少餐品是否存在的二分类）</li>
 *   <li>YoloProductRecognitionService：商品多类目细粒度识别+计数，用于结算</li>
 * </ul>
 * </p>
 * <p>
 * 接入流程：
 * <ol>
 *   <li>训练 YOLO 模型（餐品检测+计数数据集，覆盖门店所有 SKU）</li>
 *   <li>部署推理服务（Triton/TF-Serving/ONNX Runtime，HTTP/gRPC 接口）</li>
 *   <li>替换默认 NoOp 实现为真实推理调用</li>
 * </ol>
 * </p>
 */
public interface YoloProductRecognitionService {

    /**
     * YOLO 服务是否启用
     */
    boolean isEnabled();

    /**
     * 识别图片/视频帧中的商品
     * <p>
     * 输入：摄像头拍摄的收银台图片（base64 或上传后的 URL）
     * 输出：识别到的商品列表（包含 SKU 匹配结果、数量、置信度）
     * </p>
     *
     * @param image 图片数据（base64 字符串或图片 URL）
     * @param storeId 门店 ID（用于 SKU 匹配范围限定）
     * @return 识别结果
     */
    Result<YoloRecognitionResultDTO> recognizeProducts(String image, String storeId);
}
