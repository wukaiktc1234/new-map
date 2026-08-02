package com.foodtraceability.service.impl.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RefundApproveDTO;
import com.foodtraceability.dto.marketing.RefundCreateDTO;
import com.foodtraceability.dto.marketing.RefundQueryDTO;
import com.foodtraceability.dto.marketing.RefundVO;
import com.foodtraceability.entity.marketing.RechargeFinanceLog;
import com.foodtraceability.entity.marketing.RechargeRecord;
import com.foodtraceability.entity.marketing.RefundRequest;
import com.foodtraceability.mapper.marketing.RechargeFinanceLogMapper;
import com.foodtraceability.mapper.marketing.RechargeRecordMapper;
import com.foodtraceability.mapper.marketing.RefundRequestMapper;
import com.foodtraceability.service.marketing.RefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 退款申请服务实现
 * 退款审批流程：申请 → 审批 → 执行
 *
 * 状态机：
 * - pending → approved / rejected（审批）
 * - approved → executed（执行退款）
 * - pending → cancelled（取消）
 *
 * 退款仅退本金，赠送按配置处理（清零或按比例扣减）
 * 业务编号格式：rf-{yyyyMMddHHmmss}{4位随机数}
 */
@Service
public class RefundServiceImpl implements RefundService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BUSINESS_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RefundRequestMapper refundRequestMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final RechargeFinanceLogMapper rechargeFinanceLogMapper;

    public RefundServiceImpl(RefundRequestMapper refundRequestMapper,
                             RechargeRecordMapper rechargeRecordMapper,
                             RechargeFinanceLogMapper rechargeFinanceLogMapper) {
        this.refundRequestMapper = refundRequestMapper;
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.rechargeFinanceLogMapper = rechargeFinanceLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO createRefund(RefundCreateDTO createDTO) {
        // 1. 校验充值记录存在且支付成功
        RechargeRecord record = rechargeRecordMapper.selectById(createDTO.getRechargeRecordId());
        if (record == null) {
            throw new RuntimeException("充值记录不存在：" + createDTO.getRechargeRecordId());
        }
        if (!RechargeRecord.PAYMENT_SUCCESS.equals(record.getPaymentStatus())
                && !RechargeRecord.PAYMENT_PARTIAL_REFUNDED.equals(record.getPaymentStatus())) {
            throw new RuntimeException("充值记录状态不允许退款：" + record.getPaymentStatus());
        }
        // 校验是否已有进行中的退款申请
        if (RechargeRecord.REFUND_PENDING.equals(record.getRefundStatus())
                || RechargeRecord.REFUND_APPROVED.equals(record.getRefundStatus())) {
            throw new RuntimeException("该充值记录已有进行中的退款申请");
        }

        // 2. 计算可退本金（充值本金 - 已退本金）
        Long refundablePrincipal = record.getPrincipalAmount() - (record.getRefundAmount() != null ? record.getRefundAmount() : 0L);
        Long requestedAmount = parseYuanToFen(createDTO.getRequestedAmount());
        if (requestedAmount <= 0) {
            throw new RuntimeException("退款金额必须大于0");
        }
        if (requestedAmount > refundablePrincipal) {
            throw new RuntimeException("退款金额超过可退本金：" + formatFenToYuan(refundablePrincipal) + "元");
        }

        // 3. 创建退款申请
        RefundRequest refund = new RefundRequest();
        refund.setRechargeRecordId(record.getRecordId());
        refund.setRechargeRecordNo(record.getRecordNo());
        refund.setMemberId(record.getMemberId());
        refund.setMemberName(record.getMemberName());
        refund.setMemberPhone(record.getMemberPhone());
        refund.setRequestedAmount(requestedAmount);
        refund.setRefundablePrincipal(refundablePrincipal);
        refund.setActualRefundAmount(0L);
        refund.setBonusHandling(RefundRequest.BONUS_CLEAR);
        refund.setStatus(RefundRequest.STATUS_PENDING);
        refund.setApplicant("当前用户");
        refund.setApplyTime(LocalDateTime.now());
        refund.setRefundReason(createDTO.getRefundReason());
        refund.setRefundMethod(record.getPaymentMethod());
        refundRequestMapper.insert(refund);

        // 4. 更新充值记录 refund_status 为 pending
        record.setRefundStatus(RechargeRecord.REFUND_PENDING);
        record.setRefundReason(createDTO.getRefundReason());
        rechargeRecordMapper.updateById(record);

        return convertToVO(refund);
    }

    @Override
    public PageResult<RefundVO> getRefundPage(RefundQueryDTO queryDTO) {
        LambdaQueryWrapper<RefundRequest> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(RefundRequest::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(RefundRequest::getApplyTime);

        Page<RefundRequest> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );
        Page<RefundRequest> resultPage = refundRequestMapper.selectPage(page, wrapper);

        List<RefundVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(resultPage.getTotal(), records, resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(String refundId, RefundApproveDTO approveDTO) {
        RefundRequest refund = refundRequestMapper.selectById(refundId);
        if (refund == null) {
            throw new RuntimeException("退款申请不存在：" + refundId);
        }
        if (!RefundRequest.STATUS_PENDING.equals(refund.getStatus())) {
            throw new RuntimeException("退款申请状态不允许审批：" + refund.getStatus());
        }

        boolean approved = Boolean.TRUE.equals(approveDTO.getApproved());
        if (approved) {
            // 同意退款
            refund.setStatus(RefundRequest.STATUS_APPROVED);
            refund.setApprover("当前用户");
            refund.setApproveTime(LocalDateTime.now());
            refund.setApproveComment(approveDTO.getComment());
        } else {
            // 拒绝退款
            refund.setStatus(RefundRequest.STATUS_REJECTED);
            refund.setApprover("当前用户");
            refund.setApproveTime(LocalDateTime.now());
            refund.setApproveComment(approveDTO.getComment());
            // 同步更新充值记录 refund_status 为 rejected
            RechargeRecord record = rechargeRecordMapper.selectById(refund.getRechargeRecordId());
            if (record != null) {
                record.setRefundStatus(RechargeRecord.REFUND_REJECTED);
                rechargeRecordMapper.updateById(record);
            }
        }
        refundRequestMapper.updateById(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeRefund(String refundId) {
        RefundRequest refund = refundRequestMapper.selectById(refundId);
        if (refund == null) {
            throw new RuntimeException("退款申请不存在：" + refundId);
        }
        if (!RefundRequest.STATUS_APPROVED.equals(refund.getStatus())) {
            throw new RuntimeException("仅已批准的退款申请可执行，当前状态：" + refund.getStatus());
        }

        // 1. 执行退款：更新充值记录 refund_amount 和 refund_status
        RechargeRecord record = rechargeRecordMapper.selectById(refund.getRechargeRecordId());
        if (record == null) {
            throw new RuntimeException("关联充值记录不存在：" + refund.getRechargeRecordId());
        }
        Long newRefundAmount = (record.getRefundAmount() != null ? record.getRefundAmount() : 0L) + refund.getRequestedAmount();
        record.setRefundAmount(newRefundAmount);
        record.setRefundTime(LocalDateTime.now());
        record.setRefundApprover(refund.getApprover());
        // 判断是否全额退款
        if (newRefundAmount >= record.getPrincipalAmount()) {
            record.setPaymentStatus(RechargeRecord.PAYMENT_REFUNDED);
            record.setRefundStatus(RechargeRecord.REFUND_REFUNDED);
        } else {
            record.setPaymentStatus(RechargeRecord.PAYMENT_PARTIAL_REFUNDED);
            record.setRefundStatus(RechargeRecord.REFUND_REFUNDED);
        }
        rechargeRecordMapper.updateById(record);

        // 2. 更新退款申请状态为 executed
        refund.setStatus(RefundRequest.STATUS_EXECUTED);
        refund.setActualRefundAmount(refund.getRequestedAmount());
        refund.setExecutor("当前用户");
        refund.setExecuteTime(LocalDateTime.now());
        refundRequestMapper.updateById(refund);

        // 3. 写入财务流水（退款类型）
        RechargeFinanceLog financeLog = new RechargeFinanceLog();
        financeLog.setRecordNo(record.getRecordNo());
        financeLog.setMemberId(record.getMemberId());
        financeLog.setMemberName(record.getMemberName());
        financeLog.setFinanceType(RechargeFinanceLog.TYPE_REFUND);
        financeLog.setPrincipalChange(-refund.getRequestedAmount());
        // 赠送处理：默认清零，按比例扣减时按退款比例计算
        Long bonusChange = RefundRequest.BONUS_PROPORTIONAL.equals(refund.getBonusHandling())
                ? -calcProportionalBonus(record, refund.getRequestedAmount())
                : 0L;
        financeLog.setBonusChange(bonusChange);
        financeLog.setRevenueChange(-refund.getRequestedAmount());
        Long balanceAfter = (record.getPrincipalAmount() - record.getRefundAmount()) + record.getBonusAmount();
        financeLog.setBalanceAfter(balanceAfter);
        financeLog.setPrincipalAfter(record.getPrincipalAmount() - record.getRefundAmount());
        financeLog.setBonusAfter(record.getBonusAmount());
        financeLog.setRemark("退款" + formatFenToYuan(refund.getRequestedAmount()) + "元");
        rechargeFinanceLogMapper.insert(financeLog);
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算按比例扣减的赠送金额
     * 退款比例 = 退款金额 / 充值本金
     * 扣减赠送 = 充值赠送金额 × 退款比例
     */
    private Long calcProportionalBonus(RechargeRecord record, Long refundAmount) {
        if (record.getPrincipalAmount() == null || record.getPrincipalAmount() <= 0) {
            return 0L;
        }
        if (record.getBonusAmount() == null || record.getBonusAmount() <= 0) {
            return 0L;
        }
        double ratio = (double) refundAmount / record.getPrincipalAmount();
        return Math.round(record.getBonusAmount() * ratio);
    }

    /** 元（字符串）转分（Long） */
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

    /** 分（Long）转元（字符串，保留两位小数） */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }

    /** Entity → VO 转换 */
    private RefundVO convertToVO(RefundRequest refund) {
        RefundVO vo = new RefundVO();
        vo.setRefundId(refund.getRefundId());
        vo.setRechargeRecordId(refund.getRechargeRecordId());
        vo.setRechargeRecordNo(refund.getRechargeRecordNo());
        vo.setMemberId(refund.getMemberId());
        vo.setMemberName(refund.getMemberName());
        vo.setMemberPhone(refund.getMemberPhone());
        vo.setRequestedAmount(formatFenToYuan(refund.getRequestedAmount()));
        vo.setRefundablePrincipal(formatFenToYuan(refund.getRefundablePrincipal()));
        vo.setActualRefundAmount(formatFenToYuan(refund.getActualRefundAmount()));
        vo.setBonusHandling(refund.getBonusHandling());
        vo.setStatus(refund.getStatus());
        vo.setApplicant(refund.getApplicant());
        vo.setApplyTime(refund.getApplyTime() != null ? refund.getApplyTime().format(DATE_FORMATTER) : null);
        vo.setApprover(refund.getApprover());
        vo.setApproveTime(refund.getApproveTime() != null ? refund.getApproveTime().format(DATE_FORMATTER) : null);
        vo.setApproveComment(refund.getApproveComment());
        vo.setExecutor(refund.getExecutor());
        vo.setExecuteTime(refund.getExecuteTime() != null ? refund.getExecuteTime().format(DATE_FORMATTER) : null);
        vo.setRefundReason(refund.getRefundReason());
        vo.setRefundMethod(refund.getRefundMethod());
        vo.setRemark(refund.getRemark());
        return vo;
    }
}
