package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.OrderRequestDTO;
import com.foodtraceability.dto.OrderResultDTO;
import com.foodtraceability.dto.TableOrderDTO;

/**
 * POS订单创建服务接口
 * 负责订单创建相关的业务逻辑
 */
public interface PosOrderCreateService {

    /**
     * 创建订单
     * 包含幂等性检查、库存校验、订单号生成、金额计算、订单项创建、后厨订单、事件发布等完整流程
     * @param request 订单请求DTO
     * @return 订单结果
     */
    Result<OrderResultDTO> createOrder(OrderRequestDTO request);

    /**
     * 创建桌台订单（扫码点餐）
     * @param request 桌台订单请求DTO
     * @return 订单结果
     */
    Result<OrderResultDTO> createTableOrder(TableOrderDTO request);
}
