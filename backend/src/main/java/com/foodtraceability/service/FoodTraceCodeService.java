package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.FoodTraceCodeGenerateDTO;
import com.foodtraceability.entity.FoodTraceCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 食品追溯码服务接口
 */
public interface FoodTraceCodeService extends IService<FoodTraceCode> {

    /**
     * 生成食品追溯码
     */
    FoodTraceCode generate(FoodTraceCodeGenerateDTO dto);

    /**
     * 根据追溯码查询
     */
    FoodTraceCode getByTraceCode(String traceCode);

    /**
     * 根据订单ID查询
     */
    List<FoodTraceCode> getByOrderId(String orderId);

    /**
     * 根据后厨订单ID查询
     */
    List<FoodTraceCode> getByKitchenOrderId(Long kitchenOrderId);

    /**
     * 更新制作状态
     */
    boolean updateMakeStatus(String traceCodeId, String makeStatus);

    /**
     * 开始制作
     */
    boolean startMake(String traceCodeId, Long chefId, String chefName);

    /**
     * 完成制作
     */
    boolean completeMake(String traceCodeId);

    /**
     * 出餐
     */
    boolean serve(String traceCode);

    /**
     * 打印追溯码标签
     */
    boolean printLabel(String traceCodeId, Long printerId);

    /**
     * 批量打印
     */
    boolean batchPrint(List<String> traceCodeIds, Long printerId);

    /**
     * 计算订单成本
     */
    BigDecimal calculateOrderCost(String orderId);

    /**
     * 生成二维码
     */
    String generateQrCode(String traceCodeId);

    /**
     * 消费者查询追溯信息
     */
    Object getTraceInfo(String traceCode);

    /**
     * 统计各状态数量
     */
    int countByStatus(String status);

    /**
     * 为订单生成食品追溯码
     */
    List<FoodTraceCode> generateForOrder(String kitchenOrderId);
}
