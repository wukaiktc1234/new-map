package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.product.PricingCreateDTO;
import com.foodtraceability.dto.product.PricingQueryDTO;
import com.foodtraceability.dto.product.PricingVO;
import java.util.List;

/**
 * 产品定价服务接口
 * 管理产品价格策略和历史记录
 */
public interface PricingService {

    /**
     * 创建定价记录（调价）
     * @param dto 定价请求DTO
     * @return 定价视图对象
     */
    PricingVO create(PricingCreateDTO dto);

    /**
     * 批量调价
     * @param productType 产品类型 FOOD/COMBO
     * @param pricingList 调价列表
     * @return 定价视图对象列表
     */
    List<PricingVO> batchPricing(String productType, List<PricingCreateDTO> pricingList);

    /**
     * 分页查询定价历史记录
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Page<PricingVO> queryHistory(PricingQueryDTO queryDto);

    /**
     * 获取产品的当前价格
     * @param productType 产品类型
     * @param productId 产品ID
     * @return 当前售价（分）
     */
    Long getCurrentPrice(String productType, Long productId);

    /**
     * 获取产品的价格变动历史
     * @param productType 产品类型
     * @param productId 产品ID
     * @param limit 数量限制
     * @return 价格变动记录列表
     */
    List<PricingVO> getPriceHistory(String productType, Long productId, int limit);
}
