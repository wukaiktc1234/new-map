package com.foodtraceability.service.impl.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.marketing.RechargePlanCreateDTO;
import com.foodtraceability.dto.marketing.RechargePlanQueryDTO;
import com.foodtraceability.dto.marketing.RechargePlanUpdateDTO;
import com.foodtraceability.dto.marketing.RechargePlanVO;
import com.foodtraceability.entity.marketing.RechargePlan;
import com.foodtraceability.mapper.marketing.RechargePlanMapper;
import com.foodtraceability.service.marketing.RechargePlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 充值方案服务实现
 * 金额以分为单位存储，VO层以元为单位字符串返回
 *
 * 状态机：active ↔ inactive（可双向切换）
 */
@Service
public class RechargePlanServiceImpl
        extends ServiceImpl<RechargePlanMapper, RechargePlan>
        implements RechargePlanService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<RechargePlanVO> getPlanList(RechargePlanQueryDTO queryDTO) {
        LambdaQueryWrapper<RechargePlan> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getPlanName() != null && !queryDTO.getPlanName().isEmpty()) {
            wrapper.like(RechargePlan::getPlanName, queryDTO.getPlanName());
        }
        if (queryDTO.getPlanType() != null && !queryDTO.getPlanType().isEmpty()) {
            wrapper.eq(RechargePlan::getPlanType, queryDTO.getPlanType());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(RechargePlan::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByAsc(RechargePlan::getSortOrder);
        wrapper.orderByDesc(RechargePlan::getCreateTime);

        List<RechargePlan> plans = list(wrapper);
        return plans.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public RechargePlanVO getPlanById(String planId) {
        RechargePlan plan = getById(planId);
        if (plan == null) {
            return null;
        }
        return convertToVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargePlanVO createPlan(RechargePlanCreateDTO createDTO) {
        RechargePlan plan = new RechargePlan();
        plan.setPlanName(createDTO.getPlanName());
        plan.setPlanType(createDTO.getPlanType());
        plan.setRechargeAmount(parseYuanToFen(createDTO.getRechargeAmount()));
        plan.setBonusAmount(parseYuanToFen(createDTO.getBonusAmount()));
        plan.setBonusRate(calcBonusRate(createDTO.getRechargeAmount(), createDTO.getBonusAmount()));
        plan.setBonusPoints(createDTO.getBonusPoints());
        plan.setBonusType(createDTO.getBonusType() != null ? createDTO.getBonusType() : RechargePlan.BONUS_BALANCE);
        plan.setValidityDays(createDTO.getValidityDays() != null ? createDTO.getValidityDays() : 0);
        plan.setIsRecommended(createDTO.getIsRecommended() != null ? createDTO.getIsRecommended() : false);
        plan.setSortOrder(createDTO.getSortOrder() != null ? createDTO.getSortOrder() : 0);
        plan.setStatus(createDTO.getStatus() != null ? createDTO.getStatus() : RechargePlan.STATUS_ACTIVE);
        plan.setDescription(createDTO.getDescription());
        plan.setPromotionId(createDTO.getPromotionId());
        plan.setStartTime(parseDateTime(createDTO.getStartTime()));
        plan.setEndTime(parseDateTime(createDTO.getEndTime()));
        plan.setTargetAudience(createDTO.getTargetAudience());
        save(plan);
        return convertToVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargePlanVO updatePlan(String planId, RechargePlanUpdateDTO updateDTO) {
        RechargePlan plan = getById(planId);
        if (plan == null) {
            throw new RuntimeException("充值方案不存在：" + planId);
        }
        if (updateDTO.getPlanName() != null) plan.setPlanName(updateDTO.getPlanName());
        if (updateDTO.getPlanType() != null) plan.setPlanType(updateDTO.getPlanType());
        if (updateDTO.getRechargeAmount() != null) {
            plan.setRechargeAmount(parseYuanToFen(updateDTO.getRechargeAmount()));
            plan.setBonusRate(calcBonusRate(updateDTO.getRechargeAmount(), updateDTO.getBonusAmount()));
        }
        if (updateDTO.getBonusAmount() != null) {
            plan.setBonusAmount(parseYuanToFen(updateDTO.getBonusAmount()));
            if (updateDTO.getRechargeAmount() != null) {
                plan.setBonusRate(calcBonusRate(updateDTO.getRechargeAmount(), updateDTO.getBonusAmount()));
            }
        }
        if (updateDTO.getBonusPoints() != null) plan.setBonusPoints(updateDTO.getBonusPoints());
        if (updateDTO.getBonusType() != null) plan.setBonusType(updateDTO.getBonusType());
        if (updateDTO.getValidityDays() != null) plan.setValidityDays(updateDTO.getValidityDays());
        if (updateDTO.getIsRecommended() != null) plan.setIsRecommended(updateDTO.getIsRecommended());
        if (updateDTO.getSortOrder() != null) plan.setSortOrder(updateDTO.getSortOrder());
        if (updateDTO.getStatus() != null) plan.setStatus(updateDTO.getStatus());
        if (updateDTO.getDescription() != null) plan.setDescription(updateDTO.getDescription());
        if (updateDTO.getPromotionId() != null) plan.setPromotionId(updateDTO.getPromotionId());
        if (updateDTO.getStartTime() != null) plan.setStartTime(parseDateTime(updateDTO.getStartTime()));
        if (updateDTO.getEndTime() != null) plan.setEndTime(parseDateTime(updateDTO.getEndTime()));
        if (updateDTO.getTargetAudience() != null) plan.setTargetAudience(updateDTO.getTargetAudience());
        updateById(plan);
        return convertToVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(String planId) {
        RechargePlan plan = getById(planId);
        if (plan == null) {
            throw new RuntimeException("充值方案不存在：" + planId);
        }
        removeById(planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(String planId) {
        RechargePlan plan = getById(planId);
        if (plan == null) {
            throw new RuntimeException("充值方案不存在：" + planId);
        }
        String newStatus = RechargePlan.STATUS_ACTIVE.equals(plan.getStatus())
                ? RechargePlan.STATUS_INACTIVE
                : RechargePlan.STATUS_ACTIVE;
        plan.setStatus(newStatus);
        updateById(plan);
    }

    // ==================== 辅助方法 ====================

    /** 元（字符串）转分（Long），空值返回0 */
    private Long parseYuanToFen(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            return 0L;
        }
        try {
            double value = Double.parseDouble(yuan);
            return Math.round(value * 100);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 计算赠送比例（百分比，整数） */
    private Integer calcBonusRate(String rechargeAmount, String bonusAmount) {
        if (rechargeAmount == null || bonusAmount == null) return 0;
        try {
            double recharge = Double.parseDouble(rechargeAmount);
            double bonus = Double.parseDouble(bonusAmount);
            if (recharge <= 0) return 0;
            return (int) Math.round((bonus / recharge) * 100);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 解析日期字符串为 LocalDateTime，空值返回 null */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /** Entity → VO 转换 */
    private RechargePlanVO convertToVO(RechargePlan plan) {
        RechargePlanVO vo = new RechargePlanVO();
        vo.setPlanId(plan.getPlanId());
        vo.setPlanName(plan.getPlanName());
        vo.setPlanType(plan.getPlanType());
        vo.setRechargeAmount(formatFenToYuan(plan.getRechargeAmount()));
        vo.setBonusAmount(formatFenToYuan(plan.getBonusAmount()));
        vo.setBonusRate(plan.getBonusRate());
        vo.setBonusPoints(plan.getBonusPoints());
        vo.setBonusType(plan.getBonusType());
        vo.setValidityDays(plan.getValidityDays());
        vo.setIsRecommended(plan.getIsRecommended());
        vo.setSortOrder(plan.getSortOrder());
        vo.setStatus(plan.getStatus());
        vo.setDescription(plan.getDescription());
        vo.setPromotionId(plan.getPromotionId());
        vo.setStartTime(plan.getStartTime() != null ? plan.getStartTime().format(DATE_FORMATTER) : null);
        vo.setEndTime(plan.getEndTime() != null ? plan.getEndTime().format(DATE_FORMATTER) : null);
        vo.setTargetAudience(plan.getTargetAudience());
        vo.setCreatedAt(plan.getCreateTime() != null ? plan.getCreateTime().format(DATE_FORMATTER) : null);
        vo.setUpdatedAt(plan.getUpdateTime() != null ? plan.getUpdateTime().format(DATE_FORMATTER) : null);
        return vo;
    }

    /** 分（Long）转元（字符串，保留两位小数） */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }
}
