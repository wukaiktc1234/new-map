package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.marketing.PromotionCreateDTO;
import com.foodtraceability.dto.marketing.PromotionUpdateDTO;
import com.foodtraceability.entity.MarketingPromotion;
import com.foodtraceability.mapper.MarketingPromotionMapper;
import com.foodtraceability.service.PromotionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 促销活动服务实现
 */
@Service
public class PromotionServiceImpl extends ServiceImpl<MarketingPromotionMapper, MarketingPromotion>
        implements PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionServiceImpl.class);

    private final MarketingPromotionMapper promotionMapper;

    public PromotionServiceImpl(MarketingPromotionMapper promotionMapper) {
        this.promotionMapper = promotionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingPromotion createPromotion(PromotionCreateDTO createDTO) {
        // 检查编码唯一性
        MarketingPromotion existing = lambdaQuery()
                .eq(MarketingPromotion::getPromotionCode, createDTO.getPromotionCode())
                .one();
        if (existing != null) {
            throw new RuntimeException("活动编码已存在: " + createDTO.getPromotionCode());
        }

        // 时间校验
        if (createDTO.getStartTime().isAfter(createDTO.getEndTime())) {
            throw new RuntimeException("开始时间不能晚于结束时间");
        }

        MarketingPromotion promotion = new MarketingPromotion();
        promotion.setPromotionName(createDTO.getPromotionName());
        promotion.setPromotionCode(createDTO.getPromotionCode());
        promotion.setPromotionType(createDTO.getPromotionType());
        promotion.setDiscountRule(createDTO.getDiscountRule());
        promotion.setStartTime(createDTO.getStartTime());
        promotion.setEndTime(createDTO.getEndTime());
        promotion.setTargetAudience(createDTO.getTargetAudience() != null ? createDTO.getTargetAudience() : 1);
        promotion.setTargetLevelIds(createDTO.getTargetLevelIds());
        promotion.setParticipationCondition(createDTO.getParticipationCondition());
        promotion.setBudgetTotal(createDTO.getBudgetTotal());
        promotion.setBudgetUsed(0L);
        promotion.setParticipantCount(0);
        promotion.setSuccessCount(0);
        promotion.setSalesAmount(0L);
        promotion.setDiscountAmount(0L);
        promotion.setStatus(1); // 草稿状态
        promotion.setDescription(createDTO.getDescription());

        save(promotion);
        logger.info("创建促销活动: promotionId={}, name={}", promotion.getPromotionId(), promotion.getPromotionName());
        return promotion;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingPromotion updatePromotion(Long promotionId, PromotionUpdateDTO updateDTO) {
        MarketingPromotion promotion = getById(promotionId);
        if (promotion == null) {
            throw new RuntimeException("活动不存在");
        }
        if (promotion.getStatus() == 3 || promotion.getStatus() == 5) {
            throw new RuntimeException("已结束或已作废的活动不能修改");
        }

        if (updateDTO.getPromotionName() != null) {
            promotion.setPromotionName(updateDTO.getPromotionName());
        }
        if (updateDTO.getDiscountRule() != null) {
            promotion.setDiscountRule(updateDTO.getDiscountRule());
        }
        if (updateDTO.getStartTime() != null) {
            promotion.setStartTime(updateDTO.getStartTime());
        }
        if (updateDTO.getEndTime() != null) {
            promotion.setEndTime(updateDTO.getEndTime());
        }
        if (updateDTO.getTargetAudience() != null) {
            promotion.setTargetAudience(updateDTO.getTargetAudience());
        }
        if (updateDTO.getBudgetTotal() != null) {
            promotion.setBudgetTotal(updateDTO.getBudgetTotal());
        }
        if (updateDTO.getStatus() != null) {
            promotion.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getDescription() != null) {
            promotion.setDescription(updateDTO.getDescription());
        }

        updateById(promotion);
        return promotion;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPromotion(Long promotionId) {
        MarketingPromotion promotion = getById(promotionId);
        if (promotion == null) {
            throw new RuntimeException("活动不存在");
        }
        if (promotion.getStatus() != 1) {
            throw new RuntimeException("只有草稿状态的活动可以启动");
        }
        promotion.setStatus(2); // 进行中
        updateById(promotion);
        logger.info("启动促销活动: promotionId={}", promotionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pausePromotion(Long promotionId) {
        updateStatus(promotionId, 2, 4, "暂停");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endPromotion(Long promotionId) {
        updateStatus(promotionId, 2, 3, "结束");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPromotion(Long promotionId) {
        MarketingPromotion promotion = getById(promotionId);
        if (promotion == null) {
            throw new RuntimeException("活动不存在");
        }
        if (promotion.getStatus() == 2) {
            throw new RuntimeException("进行中的活动不能作废，请先暂停");
        }
        promotion.setStatus(5); // 已作废
        updateById(promotion);
    }

    @Override
    public List<MarketingPromotion> getActivePromotions() {
        return promotionMapper.selectActivePromotions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordParticipation(Long promotionId, Long salesAmount, Long discountAmount) {
        MarketingPromotion promotion = getById(promotionId);
        if (promotion == null) return;

        promotion.setParticipantCount(promotion.getParticipantCount() + 1);
        promotion.setSuccessCount(promotion.getSuccessCount() + 1);
        if (salesAmount != null) {
            promotion.setSalesAmount(promotion.getSalesAmount() + salesAmount);
        }
        if (discountAmount != null) {
            promotion.setDiscountAmount(promotion.getDiscountAmount() + discountAmount);
        }
        if (discountAmount != null) {
            promotion.setBudgetUsed(promotion.getBudgetUsed() + discountAmount);
        }
        updateById(promotion);
    }

    @Override
    public Map<String, Object> getEffectSummary() {
        return promotionMapper.selectEffectSummary();
    }

    /** 更新状态（带校验） */
    private void updateStatus(Long promotionId, int expectedStatus, int targetStatus, String action) {
        MarketingPromotion promotion = getById(promotionId);
        if (promotion == null) {
            throw new RuntimeException("活动不存在");
        }
        if (promotion.getStatus() != expectedStatus) {
            throw new RuntimeException("当前状态不允许" + action + "操作");
        }
        promotion.setStatus(targetStatus);
        updateById(promotion);
        logger.info("{}促销活动: promotionId={}, status={}->{}", action, promotionId, expectedStatus, targetStatus);
    }
}
