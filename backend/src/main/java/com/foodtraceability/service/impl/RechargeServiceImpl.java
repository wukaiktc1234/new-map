package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.marketing.RechargeCreateDTO;
import com.foodtraceability.entity.RechargePlan;
import com.foodtraceability.entity.RechargeRecord;
import com.foodtraceability.mapper.MarketingMemberMapper;
import com.foodtraceability.mapper.RechargePlanBaseMapper;
import com.foodtraceability.mapper.RechargeRecordBaseMapper;
import com.foodtraceability.service.MarketingMemberService;
import com.foodtraceability.service.PointsService;
import com.foodtraceability.service.RechargeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值服务实现
 */
@Service
public class RechargeServiceImpl extends ServiceImpl<RechargeRecordBaseMapper, RechargeRecord>
        implements RechargeService {

    private static final Logger logger = LoggerFactory.getLogger(RechargeServiceImpl.class);

    /** 修复说明：将@Autowired字段注入改为构造函数注入，符合项目规范 */
    private final RechargeRecordBaseMapper rechargeRecordMapper;
    private final RechargePlanBaseMapper rechargePlanMapper;
    private final MarketingMemberService memberService;
    private final PointsService pointsService;
    private final MarketingMemberMapper marketingMemberMapper;

    /**
     * 构造函数注入
     * 符合项目规范：禁止使用@Autowired字段注入
     */
    public RechargeServiceImpl(RechargeRecordBaseMapper rechargeRecordMapper,
                               RechargePlanBaseMapper rechargePlanMapper,
                               MarketingMemberService memberService,
                               PointsService pointsService,
                               MarketingMemberMapper marketingMemberMapper) {
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.rechargePlanMapper = rechargePlanMapper;
        this.memberService = memberService;
        this.pointsService = pointsService;
        this.marketingMemberMapper = marketingMemberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecord createRecharge(RechargeCreateDTO createDTO) {
        // 验证会员存在
        if (memberService.getById(createDTO.getMemberId()) == null) {
            throw new RuntimeException("会员不存在");
        }

        // 确定充值金额和赠送规则
        Long rechargeAmount = createDTO.getRechargeAmount();
        Long bonusAmount = 0L;
        Integer bonusPoints = 0;

        // 如果选择了充值方案，使用方案配置
        if (createDTO.getPlanId() != null) {
            RechargePlan plan = rechargePlanMapper.selectById(createDTO.getPlanId());
            if (plan == null || plan.getStatus() != 1) {
                throw new RuntimeException("充值方案不存在或已停用");
            }
            rechargeAmount = plan.getRechargeAmount();
            bonusAmount = plan.getBonusAmount() != null ? plan.getBonusAmount() : 0L;
            bonusPoints = plan.getBonusPoints() != null ? plan.getBonusPoints() : 0;
        } else {
            // 自定义金额，无赠送
            if (rechargeAmount == null || rechargeAmount <= 0) {
                throw new RuntimeException("请选择充值方案或输入有效金额");
            }
        }

        // 创建充值记录
        RechargeRecord record = new RechargeRecord();
        record.setRecordNo(generateRecordNo());
        record.setMemberId(createDTO.getMemberId());
        record.setPlanId(createDTO.getPlanId());
        record.setRechargeAmount(rechargeAmount);
        record.setBonusAmount(bonusAmount);
        record.setBonusPoints(bonusPoints);
        record.setPaymentMethod(createDTO.getPaymentMethod());
        record.setPaymentStatus(0); // 待支付
        record.setTransactionNo(createDTO.getTransactionNo());
        record.setOperateUserId(createDTO.getOperateUserId());
        record.setOperateUserName(createDTO.getOperateUserName());
        record.setRemark(createDTO.getRemark());

        save(record);
        logger.info("创建充值记录: recordNo={}, memberId={}, amount={}分",
                record.getRecordNo(), createDTO.getMemberId(), rechargeAmount);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecord paymentSuccess(String recordNo, String transactionNo) {
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getRecordNo, recordNo);
        RechargeRecord record = getOne(wrapper);

        if (record == null) {
            throw new RuntimeException("充值记录不存在: " + recordNo);
        }
        if (record.getPaymentStatus() == 1) {
            throw new RuntimeException("该订单已支付完成");
        }
        if (record.getPaymentStatus() == 2) {
            throw new RuntimeException("该订单已退款");
        }

        // 更新支付状态
        record.setPaymentStatus(1); // 已支付
        record.setTransactionNo(transactionNo);
        record.setPaidTime(LocalDateTime.now());
        updateById(record);

        // 入账余额（实充+赠送）
        long totalAdd = record.getRechargeAmount() + record.getBonusAmount();
        marketingMemberMapper.addBalance(record.getMemberId(), totalAdd);

        // 赠送积分
        if (record.getBonusPoints() != null && record.getBonusPoints() > 0) {
            pointsService.grantActivityPoints(
                    record.getMemberId(),
                    record.getBonusPoints(),
                    "REC" + record.getRecordId()
            );
        }

        logger.info("充值入账成功: recordNo={}, memberId={}, amount={}分, bonus={}分",
                recordNo, record.getMemberId(), record.getRechargeAmount(), record.getBonusAmount());

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecord refund(Long recordId, Long refundAmount, String reason) {
        RechargeRecord record = getById(recordId);
        if (record == null) {
            throw new RuntimeException("充值记录不存在");
        }
        if (record.getPaymentStatus() != 1) {
            throw new RuntimeException("只有已支付的记录可以退款");
        }

        // 扣减会员余额
        marketingMemberMapper.addBalance(record.getMemberId(), -refundAmount);

        // 更新退款信息
        record.setRefundAmount(refundAmount);
        record.setRefundTime(LocalDateTime.now());
        record.setPaymentStatus(2); // 已退款
        record.setRemark((record.getRemark() != null ? record.getRemark() + "; " : "") +
                        "退款原因: " + reason);
        updateById(record);

        logger.info("充值退款成功: recordId={}, refundAmount={}分", recordId, refundAmount);
        return record;
    }

    @Override
    public List<RechargeRecord> getMemberRecords(Long memberId, int current, int size) {
        int offset = (current - 1) * size;
        return rechargeRecordMapper.selectByMemberId(memberId, offset, size);
    }

    @Override
    public List<RechargePlan> getAvailablePlans() {
        LambdaQueryWrapper<RechargePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargePlan::getStatus, 1)
               .orderByAsc(RechargePlan::getSortOrder)
               .orderByDesc(RechargePlan::getIsRecommended);
        return rechargePlanMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getRechargeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayAmount", rechargeRecordMapper.sumTodayRecharge());
        stats.put("totalRecords", count(new LambdaQueryWrapper<RechargeRecord>()
                .eq(RechargeRecord::getPaymentStatus, 1)));
        stats.put("availablePlans", getAvailablePlans().size());
        return stats;
    }

    /** 生成充值流水号 */
    private String generateRecordNo() {
        return "RC" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}
