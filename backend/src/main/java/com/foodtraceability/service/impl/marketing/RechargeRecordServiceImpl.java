package com.foodtraceability.service.impl.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RechargeRecordQueryDTO;
import com.foodtraceability.dto.marketing.RechargeRecordVO;
import com.foodtraceability.dto.marketing.RechargeRequestDTO;
import com.foodtraceability.entity.MarketingMember;
import com.foodtraceability.entity.MemberLevel;
import com.foodtraceability.entity.marketing.BonusBalanceDetail;
import com.foodtraceability.entity.marketing.RechargeFinanceLog;
import com.foodtraceability.entity.marketing.RechargePlan;
import com.foodtraceability.entity.marketing.RechargeRecord;
import com.foodtraceability.event.MemberLevelUpgradedEvent;
import com.foodtraceability.mapper.MarketingMemberMapper;
import com.foodtraceability.mapper.marketing.BonusBalanceDetailMapper;
import com.foodtraceability.mapper.marketing.RechargeFinanceLogMapper;
import com.foodtraceability.mapper.marketing.RechargePlanMapper;
import com.foodtraceability.mapper.marketing.RechargeRecordMapper;
import com.foodtraceability.service.MemberLevelService;
import com.foodtraceability.service.marketing.RechargeRecordService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 充值记录服务实现
 * 会员充值流程：方案校验 → 生成流水号 → 创建充值记录 → 更新会员余额 → 发放赠送（写赠送明细）→ 写入财务流水 → 触发等级升级
 *
 * 业务编号格式：rc-{yyyyMMddHHmmss}{4位随机数}
 * 金额单位：后端/数据库使用分(Long)，前端使用元(String)
 */
@Service
public class RechargeRecordServiceImpl implements RechargeRecordService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BUSINESS_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RechargeRecordMapper rechargeRecordMapper;
    private final RechargePlanMapper rechargePlanMapper;
    private final BonusBalanceDetailMapper bonusBalanceDetailMapper;
    private final RechargeFinanceLogMapper rechargeFinanceLogMapper;
    private final MarketingMemberMapper marketingMemberMapper;
    private final MemberLevelService memberLevelService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RechargeRecordServiceImpl(RechargeRecordMapper rechargeRecordMapper,
                                     RechargePlanMapper rechargePlanMapper,
                                     BonusBalanceDetailMapper bonusBalanceDetailMapper,
                                     RechargeFinanceLogMapper rechargeFinanceLogMapper,
                                     MarketingMemberMapper marketingMemberMapper,
                                     MemberLevelService memberLevelService,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.rechargePlanMapper = rechargePlanMapper;
        this.bonusBalanceDetailMapper = bonusBalanceDetailMapper;
        this.rechargeFinanceLogMapper = rechargeFinanceLogMapper;
        this.marketingMemberMapper = marketingMemberMapper;
        this.memberLevelService = memberLevelService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public PageResult<RechargeRecordVO> getRecordPage(RechargeRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<RechargeRecord> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(RechargeRecord::getPaymentTime);

        Page<RechargeRecord> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );
        Page<RechargeRecord> resultPage = rechargeRecordMapper.selectPage(page, wrapper);

        List<RechargeRecordVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(resultPage.getTotal(), records, resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public RechargeRecordVO getRecordById(String recordId) {
        RechargeRecord record = rechargeRecordMapper.selectById(recordId);
        if (record == null) {
            return null;
        }
        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecordVO recharge(RechargeRequestDTO requestDTO) {
        // 1. 校验充值方案存在且启用
        RechargePlan plan = rechargePlanMapper.selectById(requestDTO.getPlanId());
        if (plan == null) {
            throw new RuntimeException("充值方案不存在：" + requestDTO.getPlanId());
        }
        if (!RechargePlan.STATUS_ACTIVE.equals(plan.getStatus())) {
            throw new RuntimeException("充值方案已停用：" + plan.getPlanName());
        }

        // 2. 创建充值记录
        RechargeRecord record = new RechargeRecord();
        record.setRecordNo(generateRecordNo());
        record.setMemberId(requestDTO.getMemberId());
        // 会员姓名/手机号由调用方维护或后续通过关联查询补全，此处置空
        record.setMemberName("");
        record.setMemberPhone("");
        record.setPlanId(plan.getPlanId());
        record.setPlanName(plan.getPlanName());
        record.setRechargeAmount(plan.getRechargeAmount());
        record.setPrincipalAmount(plan.getRechargeAmount());
        record.setBonusAmount(plan.getBonusAmount() != null ? plan.getBonusAmount() : 0L);
        record.setBonusPoints(plan.getBonusPoints() != null ? plan.getBonusPoints() : 0);
        record.setPaymentMethod(requestDTO.getPaymentMethod());
        record.setPaymentStatus(RechargeRecord.PAYMENT_SUCCESS);
        record.setPaymentTime(LocalDateTime.now());
        record.setTransactionNo(generateTransactionNo(requestDTO.getPaymentMethod()));
        // 赠送过期时间：validityDays > 0 时按天数计算，-1 永久，0 跟随系统默认（暂按180天）
        record.setBonusExpireTime(calcBonusExpireTime(plan.getValidityDays()));
        record.setRefundStatus(RechargeRecord.REFUND_NONE);
        record.setRefundAmount(0L);
        rechargeRecordMapper.insert(record);

        // 3. 更新会员余额：本金+赠送全部入账
        Long memberId = Long.parseLong(record.getMemberId());
        Long rechargeAmount = record.getRechargeAmount();
        Long bonusAmount = record.getBonusAmount() != null ? record.getBonusAmount() : 0L;

        // 修复 DF-028：查询充值前会员余额，用于计算变动后总余额
        MarketingMember member = marketingMemberMapper.selectById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在：" + memberId);
        }
        Long oldBalance = member.getBalance() != null ? member.getBalance() : 0L;

        // 修复 DF-029：校验 addBalance 返回值，避免会员不存在或已删除时静默失败
        int updatedRows = marketingMemberMapper.addBalance(memberId, rechargeAmount + bonusAmount);
        if (updatedRows == 0) {
            throw new RuntimeException("会员余额更新失败，会员ID：" + memberId + " 可能不存在或已删除");
        }

        // 4. 发放赠送：写入赠送余额明细
        if (bonusAmount > 0) {
            BonusBalanceDetail bonusDetail = new BonusBalanceDetail();
            bonusDetail.setMemberId(record.getMemberId());
            bonusDetail.setRechargeRecordId(record.getRecordId());
            bonusDetail.setBonusAmount(bonusAmount);
            bonusDetail.setRemainingAmount(bonusAmount);
            bonusDetail.setExpireTime(record.getBonusExpireTime());
            bonusDetail.setStatus(BonusBalanceDetail.STATUS_ACTIVE);
            bonusBalanceDetailMapper.insert(bonusDetail);
        }

        // 修复 DF-028：计算变动后的总余额、本金余额、赠送余额
        // balanceAfter = 充值前总余额 + 本次充值本金 + 本次赠送金额
        // bonusAfter  = 当前有效赠送余额总和（含本次新增）
        // principalAfter = balanceAfter - bonusAfter
        Long newBalance = oldBalance + rechargeAmount + bonusAmount;
        Long activeBonusRemaining = bonusBalanceDetailMapper.sumActiveBonusRemaining(record.getMemberId());
        Long newBonusAfter = activeBonusRemaining != null ? activeBonusRemaining : 0L;
        Long newPrincipalAfter = newBalance - newBonusAfter;

        // 5. 写入财务流水（充值类型）
        RechargeFinanceLog financeLog = new RechargeFinanceLog();
        financeLog.setRecordNo(record.getRecordNo());
        financeLog.setMemberId(record.getMemberId());
        financeLog.setMemberName(record.getMemberName());
        financeLog.setFinanceType(RechargeFinanceLog.TYPE_RECHARGE);
        financeLog.setPrincipalChange(record.getPrincipalAmount());
        financeLog.setBonusChange(bonusAmount);
        financeLog.setRevenueChange(record.getPrincipalAmount());
        financeLog.setBalanceAfter(newBalance);
        financeLog.setPrincipalAfter(newPrincipalAfter);
        financeLog.setBonusAfter(newBonusAfter);
        financeLog.setRemark("充值" + formatFenToYuan(rechargeAmount) + "元，赠送" + formatFenToYuan(bonusAmount) + "元");
        rechargeFinanceLogMapper.insert(financeLog);

        // 修复 DF-027：触发会员等级升级（只升不降）
        triggerLevelUpgrade(member, memberId, rechargeAmount, bonusAmount);

        return convertToVO(record);
    }

    /**
     * 触发会员等级升级（只升不降）
     * 基于会员累计充值金额判断是否达到更高等级门槛，达到则升级并发布事件。
     * 幂等性：等级未变化时不执行更新；目标等级不高于当前等级时不执行降级。
     *
     * @param member 充值前查询到的会员快照
     * @param memberId 会员ID
     * @param rechargeAmount 本次充值本金（分）
     * @param bonusAmount 本次赠送金额（分）
     */
    private void triggerLevelUpgrade(MarketingMember member, Long memberId, Long rechargeAmount, Long bonusAmount) {
        Long oldLevelId = member.getMemberLevelId();
        Integer currentPoints = member.getPoints() != null ? member.getPoints() : 0;
        // 累计充值金额作为等级判定依据（addBalance 已将 rechargeAmount+bonusAmount 累加到 total_recharge）
        Long newTotalRecharge = (member.getTotalRecharge() != null ? member.getTotalRecharge() : 0L)
                                + rechargeAmount + bonusAmount;

        Long targetLevelId = memberLevelService.calculateUpgradeLevel(currentPoints, newTotalRecharge);
        if (targetLevelId == null || targetLevelId.equals(oldLevelId)) {
            return;
        }

        // 只升不降：校验目标等级 sortOrder 严格高于当前等级
        if (!isLevelUpgrade(oldLevelId, targetLevelId)) {
            return;
        }

        marketingMemberMapper.updateMemberLevel(memberId, targetLevelId);
        applicationEventPublisher.publishEvent(
                new MemberLevelUpgradedEvent(memberId, oldLevelId, targetLevelId));
    }

    /**
     * 判断目标等级是否为升级（目标 sortOrder 严格高于当前等级）
     * 当前等级为 null 时视为首次定级，任意目标等级均为升级
     */
    private boolean isLevelUpgrade(Long oldLevelId, Long targetLevelId) {
        if (oldLevelId == null) {
            return true;
        }
        MemberLevel currentLevel = memberLevelService.getById(oldLevelId);
        MemberLevel targetLevel = memberLevelService.getById(targetLevelId);
        if (currentLevel == null || targetLevel == null) {
            return false;
        }
        int currentSort = currentLevel.getSortOrder() != null ? currentLevel.getSortOrder() : 0;
        int targetSort = targetLevel.getSortOrder() != null ? targetLevel.getSortOrder() : 0;
        return targetSort > currentSort;
    }

    @Override
    public void exportRecords(RechargeRecordQueryDTO queryDTO) {
        // 导出占位实现：实际可接入异步导出任务
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecordVO updateRecord(String recordId, RechargeRecord record) {
        // 1. 校验记录是否存在
        RechargeRecord existing = rechargeRecordMapper.selectById(recordId);
        if (existing == null) {
            throw new RuntimeException("充值记录不存在：" + recordId);
        }

        // 2. 仅更新业务可变字段：会员信息、支付方式、支付状态、退款相关字段
        // 金额、流水号等财务核心字段禁止变更，保证账务一致性
        if (record.getMemberName() != null) {
            existing.setMemberName(record.getMemberName());
        }
        if (record.getMemberPhone() != null) {
            existing.setMemberPhone(record.getMemberPhone());
        }
        if (record.getPaymentMethod() != null) {
            existing.setPaymentMethod(record.getPaymentMethod());
        }
        if (record.getPaymentStatus() != null) {
            existing.setPaymentStatus(record.getPaymentStatus());
        }
        if (record.getRefundStatus() != null) {
            existing.setRefundStatus(record.getRefundStatus());
        }
        if (record.getRefundAmount() != null) {
            existing.setRefundAmount(record.getRefundAmount());
        }
        if (record.getRefundTime() != null) {
            existing.setRefundTime(record.getRefundTime());
        }
        if (record.getRefundReason() != null) {
            existing.setRefundReason(record.getRefundReason());
        }
        if (record.getRefundApprover() != null) {
            existing.setRefundApprover(record.getRefundApprover());
        }

        rechargeRecordMapper.updateById(existing);
        return convertToVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRecord(String recordId) {
        RechargeRecord existing = rechargeRecordMapper.selectById(recordId);
        if (existing == null) {
            return false;
        }
        // 安全约束：已支付成功的记录禁止删除，需走退款流程
        if (RechargeRecord.PAYMENT_SUCCESS.equals(existing.getPaymentStatus())) {
            throw new RuntimeException("已支付成功的充值记录不可删除，请走退款流程");
        }
        // @TableLogic 自动处理逻辑删除
        return rechargeRecordMapper.deleteById(recordId) > 0;
    }

    // ==================== 辅助方法 ====================

    /** 构建查询条件 */
    private LambdaQueryWrapper<RechargeRecord> buildQueryWrapper(RechargeRecordQueryDTO queryDTO) {
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getMemberPhone() != null && !queryDTO.getMemberPhone().isEmpty()) {
            wrapper.like(RechargeRecord::getMemberPhone, queryDTO.getMemberPhone());
        }
        if (queryDTO.getMemberId() != null && !queryDTO.getMemberId().isEmpty()) {
            wrapper.eq(RechargeRecord::getMemberId, queryDTO.getMemberId());
        }
        if (queryDTO.getPlanId() != null && !queryDTO.getPlanId().isEmpty()) {
            wrapper.eq(RechargeRecord::getPlanId, queryDTO.getPlanId());
        }
        if (queryDTO.getPaymentMethod() != null && !queryDTO.getPaymentMethod().isEmpty()) {
            wrapper.eq(RechargeRecord::getPaymentMethod, queryDTO.getPaymentMethod());
        }
        if (queryDTO.getPaymentStatus() != null && !queryDTO.getPaymentStatus().isEmpty()) {
            wrapper.eq(RechargeRecord::getPaymentStatus, queryDTO.getPaymentStatus());
        }
        if (queryDTO.getRefundStatus() != null && !queryDTO.getRefundStatus().isEmpty()) {
            wrapper.eq(RechargeRecord::getRefundStatus, queryDTO.getRefundStatus());
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            LocalDateTime startTime = parseDateTime(queryDTO.getStartTime());
            if (startTime != null) {
                wrapper.ge(RechargeRecord::getPaymentTime, startTime);
            }
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            LocalDateTime endTime = parseDateTime(queryDTO.getEndTime());
            if (endTime != null) {
                wrapper.le(RechargeRecord::getPaymentTime, endTime);
            }
        }
        return wrapper;
    }

    /** 生成充值流水号：rc-{yyyyMMddHHmmss}{4位随机数} */
    private String generateRecordNo() {
        return "rc-" + LocalDateTime.now().format(BUSINESS_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /** 生成第三方交易流水号 */
    private String generateTransactionNo(String paymentMethod) {
        String prefix = paymentMethod != null ? paymentMethod.toUpperCase() : "PAY";
        return prefix + LocalDateTime.now().format(BUSINESS_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /** 计算赠送过期时间 */
    private LocalDateTime calcBonusExpireTime(Integer validityDays) {
        if (validityDays == null || validityDays == 0) {
            // 跟随系统默认：180天
            return LocalDateTime.now().plusDays(180);
        }
        if (validityDays == -1) {
            // 永久有效
            return null;
        }
        return LocalDateTime.now().plusDays(validityDays);
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
    private RechargeRecordVO convertToVO(RechargeRecord record) {
        RechargeRecordVO vo = new RechargeRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setRecordNo(record.getRecordNo());
        vo.setMemberId(record.getMemberId());
        vo.setMemberName(record.getMemberName());
        vo.setMemberPhone(record.getMemberPhone());
        vo.setPlanId(record.getPlanId());
        vo.setPlanName(record.getPlanName());
        vo.setRechargeAmount(formatFenToYuan(record.getRechargeAmount()));
        vo.setPrincipalAmount(formatFenToYuan(record.getPrincipalAmount()));
        vo.setBonusAmount(formatFenToYuan(record.getBonusAmount()));
        vo.setBonusPoints(record.getBonusPoints());
        vo.setPaymentMethod(record.getPaymentMethod());
        vo.setPaymentStatus(record.getPaymentStatus());
        vo.setPaymentTime(record.getPaymentTime() != null ? record.getPaymentTime().format(DATE_FORMATTER) : null);
        vo.setTransactionNo(record.getTransactionNo());
        vo.setBonusExpireTime(record.getBonusExpireTime() != null ? record.getBonusExpireTime().format(DATE_FORMATTER) : null);
        vo.setRefundStatus(record.getRefundStatus());
        vo.setRefundAmount(formatFenToYuan(record.getRefundAmount()));
        vo.setRefundTime(record.getRefundTime() != null ? record.getRefundTime().format(DATE_FORMATTER) : null);
        vo.setRefundReason(record.getRefundReason());
        vo.setRefundApprover(record.getRefundApprover());
        vo.setCreatedAt(record.getCreateTime() != null ? record.getCreateTime().format(DATE_FORMATTER) : null);
        vo.setUpdatedAt(record.getUpdateTime() != null ? record.getUpdateTime().format(DATE_FORMATTER) : null);
        return vo;
    }

    /** 分（Long）转元（字符串，保留两位小数） */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }
}
