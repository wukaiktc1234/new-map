package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.marketing.PromotionCreateDTO;
import com.foodtraceability.dto.marketing.PromotionUpdateDTO;
import com.foodtraceability.entity.MarketingPromotion;

import java.util.List;
import java.util.Map;

/**
 * 促销活动服务接口
 */
public interface PromotionService extends IService<MarketingPromotion> {

    /** 创建促销活动 */
    MarketingPromotion createPromotion(PromotionCreateDTO createDTO);

    /** 更新促销活动 */
    MarketingPromotion updatePromotion(Long promotionId, PromotionUpdateDTO updateDTO);

    /** 启动活动（草稿->进行中） */
    void startPromotion(Long promotionId);

    /** 暂停活动 */
    void pausePromotion(Long promotionId);

    /** 结束活动 */
    void endPromotion(Long promotionId);

    /** 作废活动 */
    void cancelPromotion(Long promotionId);

    /** 获取进行中的活动列表 */
    List<MarketingPromotion> getActivePromotions();

    /** 记录活动参与数据 */
    void recordParticipation(Long promotionId, Long salesAmount, Long discountAmount);

    /** 获取活动效果统计 */
    Map<String, Object> getEffectSummary();
}
