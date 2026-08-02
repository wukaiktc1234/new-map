package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.CouponReceiveDTO;
import com.foodtraceability.dto.marketing.CouponTemplateCreateDTO;
import com.foodtraceability.entity.CouponTemplate;
import com.foodtraceability.entity.MemberCoupon;
import com.foodtraceability.entity.MarketingMember;
import com.foodtraceability.mapper.CouponTemplateMapper;
import com.foodtraceability.mapper.MemberCouponMapper;
import com.foodtraceability.service.CouponTemplateService;
import com.foodtraceability.service.MarketingMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务实现
 */
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate>
        implements CouponTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(CouponTemplateServiceImpl.class);

    private final CouponTemplateMapper couponTemplateMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final MarketingMemberService memberService;

    public CouponTemplateServiceImpl(CouponTemplateMapper couponTemplateMapper,
                                     MemberCouponMapper memberCouponMapper,
                                     MarketingMemberService memberService) {
        this.couponTemplateMapper = couponTemplateMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.memberService = memberService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate createTemplate(CouponTemplateCreateDTO createDTO) {
        // 检查编码是否重复
        CouponTemplate existing = lambdaQuery()
                .eq(CouponTemplate::getTemplateCode, createDTO.getTemplateCode())
                .one();
        if (existing != null) {
            throw new RuntimeException("模板编码已存在: " + createDTO.getTemplateCode());
        }

        CouponTemplate template = new CouponTemplate();
        template.setTemplateName(createDTO.getTemplateName());
        template.setTemplateCode(createDTO.getTemplateCode());
        template.setCouponType(createDTO.getCouponType());
        template.setDiscountType(createDTO.getDiscountType());
        template.setDiscountValue(createDTO.getDiscountValue());
        template.setMinConsumption(createDTO.getMinConsumption() != null ? createDTO.getMinConsumption() : 0L);
        template.setMaxDiscount(createDTO.getMaxDiscount());
        template.setTotalQuantity(createDTO.getTotalQuantity() != null ? createDTO.getTotalQuantity() : -1);
        template.setPerPersonLimit(createDTO.getPerPersonLimit() != null ? createDTO.getPerPersonLimit() : 1);
        template.setValidStartDate(createDTO.getValidStartDate());
        template.setValidExpiryDate(createDTO.getValidExpiryDate());
        template.setValidDays(createDTO.getValidDays() != null ? createDTO.getValidDays() : 30);
        template.setApplicableScope(createDTO.getApplicableScope() != null ? createDTO.getApplicableScope() : 1);
        template.setScopeConfig(createDTO.getScopeConfig());
        template.setUsageRules(createDTO.getUsageRules());
        template.setStatus(1); // 默认启用
        template.setCreatedCount(0);
        template.setUsedCount(0);

        save(template);
        logger.info("创建优惠券模板: templateId={}, name={}", template.getTemplateId(), template.getTemplateName());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberCoupon receiveCoupon(CouponReceiveDTO receiveDTO) {
        // 1. 检查模板是否存在且有效
        CouponTemplate template = getById(receiveDTO.getTemplateId());
        if (template == null) {
            throw new RuntimeException("优惠券模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new RuntimeException("优惠券模板已停用");
        }
        if (template.getValidExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("优惠券领取已截止");
        }

        // 2. 检查会员是否存在
        MarketingMember member = memberService.getById(receiveDTO.getMemberId());
        if (member == null || member.getStatus() != 1) {
            throw new RuntimeException("会员不存在或状态异常");
        }

        // 3. 检查每人限领数量
        long receivedCount = memberCouponMapper.countByTemplateAndMember(
                receiveDTO.getTemplateId(), receiveDTO.getMemberId()
        );
        if (receivedCount >= template.getPerPersonLimit()) {
            throw new RuntimeException("已达领取上限，每限限领" + template.getPerPersonLimit() + "张");
        }

        // 4. 检查发放总量
        if (template.getTotalQuantity() > 0) {
            long totalReceived = memberCouponMapper.countByTemplateId(receiveDTO.getTemplateId());
            if (totalReceived >= template.getTotalQuantity()) {
                throw new RuntimeException("优惠券已被领完");
            }
        }

        // 5. 生成优惠券号码并保存
        MemberCoupon coupon = new MemberCoupon();
        coupon.setTemplateId(receiveDTO.getTemplateId());
        coupon.setMemberId(receiveDTO.getMemberId());
        coupon.setCouponNo(generateCouponNo());
        coupon.setStatus(0); // 未使用
        coupon.setReceiveTime(LocalDateTime.now());
        coupon.setExpiryTime(LocalDateTime.now().plusDays(
                template.getValidDays() != null ? template.getValidDays() : 30
        ));
        memberCouponMapper.insert(coupon);

        // 6. 更新模板统计
        template.setCreatedCount(template.getCreatedCount() + 1);
        updateById(template);

        logger.info("会员领券成功: memberId={}, templateId={}, couponNo={}",
                receiveDTO.getMemberId(), receiveDTO.getTemplateId(), coupon.getCouponNo());

        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useCoupon(Long couponId, Long orderId, String orderNo) {
        int affected = memberCouponMapper.useCoupon(couponId, orderId, orderNo);
        if (affected > 0) {
            // 更新模板使用计数
            MemberCoupon coupon = memberCouponMapper.selectById(couponId);
            if (coupon != null) {
                CouponTemplate template = getById(coupon.getTemplateId());
                if (template != null) {
                    template.setUsedCount(template.getUsedCount() + 1);
                    updateById(template);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public List<MemberCoupon> getAvailableCoupons(Long memberId) {
        return memberCouponMapper.selectAvailableByMemberId(memberId);
    }

    @Override
    public List<MemberCoupon> getMemberCoupons(Long memberId, Integer status) {
        // TODO: 根据状态筛选
        return memberCouponMapper.selectAvailableByMemberId(memberId);
    }

    @Override
    public int expireCoupons() {
        return memberCouponMapper.expireCoupons();
    }

    @Override
    public void voidCoupon(Long couponId) {
        int affected = memberCouponMapper.voidCoupon(couponId);
        if (affected == 0) {
            throw new RuntimeException("作废失败，优惠券可能不存在或状态异常");
        }
    }

    @Override
    public MemberCoupon getCouponById(Long couponId) {
        return memberCouponMapper.selectById(couponId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberCoupon updateCoupon(Long couponId, MemberCoupon coupon) {
        MemberCoupon existing = memberCouponMapper.selectById(couponId);
        if (existing == null) {
            throw new RuntimeException("优惠券不存在：" + couponId);
        }
        // 仅允许更新过期时间、状态等可变字段
        // 优惠券号码、模板ID、会员ID等核心字段不可变更
        if (coupon.getExpiryTime() != null) {
            existing.setExpiryTime(coupon.getExpiryTime());
        }
        if (coupon.getStatus() != null) {
            existing.setStatus(coupon.getStatus());
        }
        if (coupon.getOrderId() != null) {
            existing.setOrderId(coupon.getOrderId());
        }
        if (coupon.getOrderNo() != null) {
            existing.setOrderNo(coupon.getOrderNo());
        }
        if (coupon.getUseTime() != null) {
            existing.setUseTime(coupon.getUseTime());
        }
        memberCouponMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCoupon(Long couponId) {
        MemberCoupon existing = memberCouponMapper.selectById(couponId);
        if (existing == null) {
            return false;
        }
        // @TableLogic 自动处理逻辑删除
        return memberCouponMapper.deleteById(couponId) > 0;
    }

    @Override
    public PageResult<MemberCoupon> getCouponPage(Integer page, Integer size,
                                                   Long memberId, Long templateId, Integer status) {
        LambdaQueryWrapper<MemberCoupon> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(MemberCoupon::getMemberId, memberId);
        }
        if (templateId != null) {
            wrapper.eq(MemberCoupon::getTemplateId, templateId);
        }
        if (status != null) {
            wrapper.eq(MemberCoupon::getStatus, status);
        }
        wrapper.orderByDesc(MemberCoupon::getReceiveTime);

        Page<MemberCoupon> pageObj = new Page<>(
                page != null ? page : 1,
                size != null ? size : 10
        );
        Page<MemberCoupon> resultPage = memberCouponMapper.selectPage(pageObj, wrapper);

        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    /** 生成优惠券号码 */
    private String generateCouponNo() {
        return "CPN" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}
