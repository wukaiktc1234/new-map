package com.foodtraceability.service;

import com.foodtraceability.dto.PreMakeRequestDTO;

import java.util.List;
import java.util.Map;

/**
 * 提前制作管理服务接口
 */
public interface PreMakeService {

    /**
     * 提前制作：生成食品追溯码
     * @param request 提前制作请求
     * @return 制作结果
     */
    Map<String, Object> preMakeFood(PreMakeRequestDTO request);

    /**
     * 扫描食品码出餐（匹配待出餐订单）
     * @param traceCode 追溯码
     * @param orderNumber 订单号（可选）
     * @param operatorId 操作人ID（可选）
     * @param operatorName 操作人姓名（可选）
     * @return 出餐结果
     */
    Map<String, Object> scanServe(String traceCode, String orderNumber, String operatorId, String operatorName);

    /**
     * 获取已提前制作待出餐的食品列表
     * @return 食品汇总列表
     */
    List<Map<String, Object>> getReadyFoods();
}
