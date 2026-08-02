package com.foodtraceability.service;

import com.foodtraceability.dto.product.AlternativeDishVO;
import com.foodtraceability.dto.product.BatchCheckRequestDTO;
import com.foodtraceability.dto.product.BatchCheckResultVO;
import com.foodtraceability.dto.product.BomCheckResultVO;
import com.foodtraceability.dto.product.BomCheckStatsVO;
import com.foodtraceability.dto.product.BomStockWarningConfigVO;

import java.util.List;
import java.util.Map;

/**
 * BOM 库存联动检查服务接口
 * 提供菜品 BOM 配方与库存的联动检查、预警配置、替代推荐等功能
 */
public interface BomCheckService {

    /**
     * 检查单个菜品的 BOM 库存
     * @param dishId 菜品ID
     * @param quantity 制作数量
     * @return 检查结果
     */
    BomCheckResultVO checkDish(String dishId, Integer quantity);

    /**
     * 批量检查多个菜品的 BOM 库存
     * @param request 批量检查请求
     * @return 批量检查结果
     */
    BatchCheckResultVO batchCheck(BatchCheckRequestDTO request);

    /**
     * 下单前预检查（识别阻塞和警告级问题）
     * @param dishIds 菜品ID列表
     * @return 预检查结果：ready 可制作列表、issues 问题列表
     */
    Map<String, Object> preOrderCheck(List<String> dishIds);

    /**
     * 获取 BOM 预警配置
     * @return 预警配置
     */
    BomStockWarningConfigVO getWarningConfig();

    /**
     * 保存 BOM 预警配置
     * @param config 预警配置
     * @return 保存后的配置
     */
    BomStockWarningConfigVO saveWarningConfig(BomStockWarningConfigVO config);

    /**
     * 获取替代菜品推荐
     * @param dishId 原菜品ID
     * @param limit 数量限制
     * @return 替代菜品列表
     */
    List<AlternativeDishVO> getAlternatives(String dishId, Integer limit);

    /**
     * 获取 BOM 检查统计数据
     * @param startDate 开始日期（可空，格式 yyyy-MM-dd）
     * @param endDate 结束日期（可空，格式 yyyy-MM-dd）
     * @return 统计数据
     */
    BomCheckStatsVO getStats(String startDate, String endDate);
}
