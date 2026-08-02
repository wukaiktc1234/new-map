package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.FinanceVoucherCreateDTO;
import com.foodtraceability.dto.finance.FinanceVoucherVO;
import com.foodtraceability.dto.finance.FundFlowCreateDTO;
import com.foodtraceability.dto.finance.FundFlowVO;
import com.foodtraceability.dto.finance.PaymentCreateDTO;
import com.foodtraceability.dto.finance.PaymentVO;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.Payment;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.PaymentMapper;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.service.finance.PaymentService;
import com.foodtraceability.service.finance.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 付款单Service实现
 * 核心能力：四账联动（应付账款+银行账户+资金流水+会计凭证）
 *
 * 付款流程（一个事务内）：
 * 1. 校验应付账款和银行账户
 * 2. 创建付款单记录
 * 3. 更新应付账款余额和状态
 * 4. 生成付款会计凭证（借：应付账款 / 贷：银行存款）
 * 5. 创建资金流水（自动联动银行账户余额扣减）
 * 6. 回写付款单的凭证ID和流水ID
 */
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment>
        implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    /** 应付账款科目编码 */
    private static final String SUBJECT_CODE_PAYABLE = "2202";
    /** 银行存款科目编码 */
    private static final String SUBJECT_CODE_BANK = "1002";
    /** 资金流水分类：采购付款退回 */
    private static final int FLOW_CATEGORY_PAYMENT_RETURN = 7;

    private final PayableMapper payableMapper;
    private final BankAccountMapper bankAccountMapper;
    private final AccountingSubjectMapper subjectMapper;
    private final FundFlowService fundFlowService;
    private final VoucherService voucherService;
    private final PurchaseOrderMapper purchaseOrderMapper;

    public PaymentServiceImpl(PayableMapper payableMapper,
                              BankAccountMapper bankAccountMapper,
                              AccountingSubjectMapper subjectMapper,
                              FundFlowService fundFlowService,
                              VoucherService voucherService,
                              PurchaseOrderMapper purchaseOrderMapper) {
        this.payableMapper = payableMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.subjectMapper = subjectMapper;
        this.fundFlowService = fundFlowService;
        this.voucherService = voucherService;
        this.purchaseOrderMapper = purchaseOrderMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO registerPayment(PaymentCreateDTO dto) {
        log.info("登记付款，应付ID：{}，金额：{}", dto.getPayableId(), dto.getPaymentAmount());

        // ========== 1. 校验应付账款 ==========
        Payable payable = payableMapper.selectById(dto.getPayableId());
        if (payable == null) {
            throw new BusinessException("应付账款不存在");
        }
        if (payable.getStatus() != null && payable.getStatus() == 3) {
            throw new BusinessException("该应付账款已付清，无需再次付款");
        }
        if (dto.getPaymentAmount() > payable.getBalanceAmount()) {
            throw new BusinessException("付款金额（" + dto.getPaymentAmount() + "）不能大于未付余额（" + payable.getBalanceAmount() + "）");
        }

        // ========== 2. 校验银行账户 ==========
        BankAccount bankAccount = bankAccountMapper.selectById(dto.getBankAccountId());
        if (bankAccount == null) {
            throw new BusinessException("银行账户不存在");
        }
        if (bankAccount.getStatus() == null || bankAccount.getStatus() != 1) {
            throw new BusinessException("银行账户已停用");
        }
        long currentBalance = bankAccount.getBalance() != null ? bankAccount.getBalance() : 0L;
        if (dto.getPaymentAmount() > currentBalance) {
            throw new BusinessException("银行账户余额不足（当前余额：" + currentBalance + "，付款金额：" + dto.getPaymentAmount() + "）");
        }

        // ========== 3. 创建付款单 ==========
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setPayableId(payable.getPayableId());
        payment.setPayableNo(payable.getPayableNo());
        payment.setStockinId(payable.getStockinId());
        payment.setStockinNo(payable.getStockinNo());
        payment.setOrderNo(payable.getOrderNo());
        payment.setSupplierId(payable.getSupplierId());
        payment.setSupplierName(payable.getSupplierName());
        payment.setPaymentAmount(dto.getPaymentAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setBankAccountId(bankAccount.getAccountId());
        payment.setBankAccountName(bankAccount.getAccountName());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setStatus(1); // 已确认
        payment.setRemark(dto.getRemark());
        baseMapper.insert(payment);
        log.info("付款单已创建：{}", payment.getPaymentNo());

        // ========== 4. 更新应付账款余额和状态 ==========
        long newPaidAmount = payable.getPaidAmount() + dto.getPaymentAmount();
        long newBalanceAmount = payable.getBalanceAmount() - dto.getPaymentAmount();
        payable.setPaidAmount(newPaidAmount);
        payable.setBalanceAmount(newBalanceAmount);
        if (newBalanceAmount <= 0) {
            payable.setStatus(3); // 已付清
        } else {
            payable.setStatus(2); // 部分支付
        }
        payableMapper.updateById(payable);
        log.info("应付账款已更新：{}，已付：{}，余额：{}", payable.getPayableNo(), newPaidAmount, newBalanceAmount);

        // ========== 4.1 回写采购订单付款状态（LK-FINANCE-02） ==========
        syncPurchaseOrderPaymentStatus(payable.getPurchaseOrderId());

        // ========== 5. 生成付款会计凭证 ==========
        FinanceVoucherCreateDTO voucherDTO = buildPaymentVoucherDTO(payment, payable, bankAccount);
        FinanceVoucherVO voucherVO = voucherService.create(voucherDTO);
        log.info("付款凭证已生成：{}", voucherVO.getVoucherNo());

        // ========== 6. 创建资金流水（自动联动银行账户余额扣减） ==========
        FundFlowCreateDTO flowDTO = new FundFlowCreateDTO();
        flowDTO.setAccountId(bankAccount.getAccountId());
        flowDTO.setFlowDirection(2); // 支出
        flowDTO.setFlowCategory(2); // 采购付款
        flowDTO.setAmount(dto.getPaymentAmount());
        flowDTO.setCounterpartyName(payable.getSupplierName());
        flowDTO.setBusinessDate(dto.getPaymentDate());
        flowDTO.setVoucherId(voucherVO.getVoucherId());
        flowDTO.setRemark("付款单：" + payment.getPaymentNo() + "，应付：" + payable.getPayableNo());
        FundFlowVO flowVO = fundFlowService.create(flowDTO);
        log.info("资金流水已创建：{}", flowVO.getFlowNo());

        // ========== 7. 回写付款单的凭证ID、流水ID及对应编号 ==========
        payment.setVoucherId(voucherVO.getVoucherId());
        payment.setVoucherNo(voucherVO.getVoucherNo());
        payment.setFundFlowId(flowVO.getFlowId());
        payment.setFundFlowNo(flowVO.getFlowNo());
        baseMapper.updateById(payment);

        return convertToVO(payment, voucherVO, flowVO);
    }

    @Override
    public List<PaymentVO> getPaymentHistoryByPayableId(Long payableId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPayableId, payableId)
               .orderByDesc(Payment::getPaymentDate);
        List<Payment> list = baseMapper.selectList(wrapper);
        List<PaymentVO> result = new ArrayList<>();
        for (Payment p : list) {
            result.add(convertToVO(p, null, null));
        }
        return result;
    }

    @Override
    public PaymentVO getDetail(Long paymentId) {
        Payment payment = baseMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException("付款单不存在");
        }
        return convertToVO(payment, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO update(Long paymentId, PaymentCreateDTO dto) {
        log.info("更新付款单，ID：{}", paymentId);
        Payment payment = baseMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException("付款单不存在");
        }
        if (payment.getStatus() != null && payment.getStatus() == 1) {
            throw new BusinessException("已确认的付款单不可修改，请先作废后重新登记");
        }
        // 仅更新可编辑字段，不涉及四账联动重新生成
        payment.setPaymentAmount(dto.getPaymentAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setBankAccountId(dto.getBankAccountId());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setRemark(dto.getRemark());
        baseMapper.updateById(payment);
        return getDetail(paymentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO voidPayment(Long paymentId, String remark) {
        log.info("作废付款单，ID：{}，备注：{}", paymentId, remark);

        // ========== 1. 校验付款单 ==========
        Payment payment = baseMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException("付款单不存在");
        }
        if (payment.getStatus() == null || payment.getStatus() != 1) {
            throw new BusinessException("仅已确认的付款单可作废");
        }

        // ========== 2. 校验关联数据 ==========
        Payable payable = payableMapper.selectById(payment.getPayableId());
        if (payable == null) {
            throw new BusinessException("关联应付账款不存在");
        }
        BankAccount bankAccount = bankAccountMapper.selectById(payment.getBankAccountId());
        if (bankAccount == null) {
            throw new BusinessException("付款银行账户不存在");
        }

        Long paymentAmount = payment.getPaymentAmount();

        // ========== 3. 更新付款单状态为已作废 ==========
        payment.setStatus(2);
        payment.setVoidTime(LocalDateTime.now());
        payment.setVoidRemark(remark);
        baseMapper.updateById(payment);

        // ========== 4. 回滚应付账款 ==========
        long newPaidAmount = payable.getPaidAmount() - paymentAmount;
        long newBalanceAmount = payable.getBalanceAmount() + paymentAmount;
        payable.setPaidAmount(newPaidAmount);
        payable.setBalanceAmount(newBalanceAmount);
        // 回滚后若余额大于等于原金额则回到待付，否则为部分支付；已付清状态不会恢复
        if (newBalanceAmount >= payable.getOriginalAmount()) {
            payable.setStatus(1);
        } else {
            payable.setStatus(2);
        }
        payableMapper.updateById(payable);
        log.info("应付账款已回滚：{}，已付：{}，余额：{}，状态：{}",
                payable.getPayableNo(), newPaidAmount, newBalanceAmount, payable.getStatus());

        // ========== 4.1 回写采购订单付款状态（LK-FINANCE-02） ==========
        syncPurchaseOrderPaymentStatus(payable.getPurchaseOrderId());

        // ========== 5. 回滚银行账户余额 ==========
        long currentBalance = bankAccount.getBalance() != null ? bankAccount.getBalance() : 0L;
        bankAccount.setBalance(currentBalance + paymentAmount);
        bankAccountMapper.updateById(bankAccount);
        log.info("银行账户余额已回滚：{}，当前余额：{}", bankAccount.getAccountName(), bankAccount.getBalance());

        // ========== 6. 生成红冲会计凭证（借：银行存款 / 贷：应付账款） ==========
        FinanceVoucherCreateDTO voucherDTO = buildVoidVoucherDTO(payment, payable, bankAccount);
        FinanceVoucherVO voucherVO = voucherService.create(voucherDTO);
        log.info("红冲凭证已生成：{}", voucherVO.getVoucherNo());

        // ========== 7. 创建反向资金流水（收入-采购付款退回） ==========
        FundFlowCreateDTO flowDTO = new FundFlowCreateDTO();
        flowDTO.setAccountId(bankAccount.getAccountId());
        flowDTO.setFlowDirection(1); // 收入
        flowDTO.setFlowCategory(FLOW_CATEGORY_PAYMENT_RETURN); // 采购付款退回
        flowDTO.setAmount(paymentAmount);
        flowDTO.setCounterpartyName(payable.getSupplierName());
        flowDTO.setBusinessDate(payment.getPaymentDate());
        flowDTO.setVoucherId(voucherVO.getVoucherId());
        flowDTO.setRemark("作废付款单退回：" + payment.getPaymentNo() + "，应付：" + payable.getPayableNo());
        FundFlowVO flowVO = fundFlowService.create(flowDTO);
        log.info("反向资金流水已创建：{}", flowVO.getFlowNo());

        // ========== 8. 回写新凭证ID和流水ID到付款单 ==========
        payment.setVoidVoucherId(voucherVO.getVoucherId());
        payment.setVoidVoucherNo(voucherVO.getVoucherNo());
        payment.setVoidFundFlowId(flowVO.getFlowId());
        payment.setVoidFundFlowNo(flowVO.getFlowNo());
        baseMapper.updateById(payment);

        return convertToVO(payment, voucherVO, flowVO);
    }

    // ============================================================
    // 私有方法
    // ============================================================

    /**
     * 构建付款凭证DTO
     * 借：应付账款（2202）
     * 贷：银行存款（1002）
     */
    private FinanceVoucherCreateDTO buildPaymentVoucherDTO(Payment payment, Payable payable, BankAccount bankAccount) {
        // 查询科目ID
        AccountingSubject payableSubject = getSubjectByCode(SUBJECT_CODE_PAYABLE);
        AccountingSubject bankSubject = getSubjectByCode(SUBJECT_CODE_BANK);

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(payment.getPaymentDate());
        dto.setVoucherType(5); // 付款
        dto.setSourceType(5); // 付款
        dto.setSourceId(payment.getPaymentId());
        dto.setReferenceNo(payment.getPaymentNo());
        dto.setAttachmentCount(1);
        dto.setRemark("支付供应商：" + payable.getSupplierName() + "，付款单：" + payment.getPaymentNo());

        // 借方分录：应付账款
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSubjectId(payableSubject.getSubjectId());
        debitItem.setSummary("支付" + payable.getSupplierName() + "货款");
        debitItem.setDebitAmount(payment.getPaymentAmount());
        debitItem.setCreditAmount(0L);
        debitItem.setSortOrder(1);

        // 贷方分录：银行存款
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSubjectId(bankSubject.getSubjectId());
        creditItem.setSummary("付款账户：" + bankAccount.getAccountName());
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(payment.getPaymentAmount());
        creditItem.setSortOrder(2);

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();
        details.add(debitItem);
        details.add(creditItem);
        dto.setDetails(details);

        return dto;
    }

    /**
     * 构建红冲凭证DTO
     * 借：银行存款（1002）
     * 贷：应付账款（2202）
     */
    private FinanceVoucherCreateDTO buildVoidVoucherDTO(Payment payment, Payable payable, BankAccount bankAccount) {
        AccountingSubject payableSubject = getSubjectByCode(SUBJECT_CODE_PAYABLE);
        AccountingSubject bankSubject = getSubjectByCode(SUBJECT_CODE_BANK);

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(payment.getPaymentDate());
        dto.setVoucherType(5); // 付款
        dto.setSourceType(5); // 付款
        dto.setSourceId(payment.getPaymentId());
        dto.setReferenceNo(payment.getPaymentNo());
        dto.setAttachmentCount(1);
        dto.setRemark("作废红冲：付款单 " + payment.getPaymentNo() + "，供应商：" + payable.getSupplierName());

        // 借方分录：银行存款
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSubjectId(bankSubject.getSubjectId());
        debitItem.setSummary("收回付款账户：" + bankAccount.getAccountName());
        debitItem.setDebitAmount(payment.getPaymentAmount());
        debitItem.setCreditAmount(0L);
        debitItem.setSortOrder(1);

        // 贷方分录：应付账款
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSubjectId(payableSubject.getSubjectId());
        creditItem.setSummary("冲回" + payable.getSupplierName() + "货款");
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(payment.getPaymentAmount());
        creditItem.setSortOrder(2);

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();
        details.add(debitItem);
        details.add(creditItem);
        dto.setDetails(details);

        return dto;
    }

    /**
     * 回写采购订单付款状态与已付金额（LK-FINANCE-02）
     *
     * <p>业务规则：一个采购订单可能对应多张采购入库单，每张入库单生成一笔应付账款。
     * 采购订单的 paidAmount = 该订单下所有有效（未逻辑删除）应付账款的 paidAmount 之和；
     * paymentStatus：0=未付（paidAmount=0）/ 1=部分支付（0<paidAmount<finalAmount）/ 2=已支付（paidAmount>=finalAmount）。</p>
     *
     * <p>幂等性：本方法每次根据当前应付账款汇总值重算并写入，重复调用结果一致。
     * 已作废应付账款通过 @TableLogic 逻辑删除自动排除（deleted=1 不参与查询）。</p>
     *
     * @param purchaseOrderId 关联的采购订单ID；为 null 时跳过（非采购付款场景）
     */
    private void syncPurchaseOrderPaymentStatus(Long purchaseOrderId) {
        if (purchaseOrderId == null) {
            return;
        }
        PurchaseOrder order = purchaseOrderMapper.selectById(purchaseOrderId);
        if (order == null) {
            log.warn("回写采购订单付款状态失败：订单不存在，orderId={}", purchaseOrderId);
            return;
        }

        // 汇总该订单下所有有效应付账款的已付金额（已作废的通过 @TableLogic 自动排除）
        LambdaQueryWrapper<Payable> payableWrapper = new LambdaQueryWrapper<>();
        payableWrapper.eq(Payable::getPurchaseOrderId, purchaseOrderId);
        List<Payable> payables = payableMapper.selectList(payableWrapper);

        long totalPaid = 0L;
        for (Payable p : payables) {
            if (p.getPaidAmount() != null) {
                totalPaid += p.getPaidAmount();
            }
        }

        Long finalAmount = order.getFinalAmount() != null ? order.getFinalAmount()
                : order.getTotalAmount() != null ? order.getTotalAmount() : 0L;

        int newPaymentStatus;
        if (totalPaid <= 0L) {
            newPaymentStatus = 0; // 未付
        } else if (finalAmount > 0L && totalPaid >= finalAmount) {
            newPaymentStatus = 2; // 已支付
        } else {
            newPaymentStatus = 1; // 部分支付
        }

        order.setPaidAmount(totalPaid);
        order.setPaymentStatus(newPaymentStatus);
        purchaseOrderMapper.updateById(order);
        log.info("采购订单付款状态已回写：orderId={}, paidAmount={}, paymentStatus={}",
                purchaseOrderId, totalPaid, newPaymentStatus);
    }

    /**
     * 根据科目编码查询科目
     */
    private AccountingSubject getSubjectByCode(String subjectCode) {
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingSubject::getSubjectCode, subjectCode);
        AccountingSubject subject = subjectMapper.selectOne(wrapper);
        if (subject == null) {
            throw new BusinessException("会计科目不存在：" + subjectCode);
        }
        return subject;
    }

    /**
     * 生成付款单号：FK + yyyyMMdd + 4位序号
     */
    private String generatePaymentNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Payment::getPaymentNo, "FK" + datePart)
               .orderByDesc(Payment::getPaymentId).last("LIMIT 1");
        Payment last = this.getOne(wrapper, false);
        int seq = 1;
        if (last != null && last.getPaymentNo() != null) {
            String no = last.getPaymentNo();
            seq = Integer.parseInt(no.substring(no.length() - 4)) + 1;
        }
        return String.format("FK%s%04d", datePart, seq);
    }

    /**
     * 实体转VO
     */
    private PaymentVO convertToVO(Payment payment, FinanceVoucherVO voucherVO, FundFlowVO flowVO) {
        PaymentVO vo = new PaymentVO();
        vo.setPaymentId(payment.getPaymentId());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setPayableId(payment.getPayableId());
        vo.setPayableNo(payment.getPayableNo());
        vo.setStockinId(payment.getStockinId());
        vo.setStockinNo(payment.getStockinNo());
        vo.setOrderNo(payment.getOrderNo());
        vo.setSupplierId(payment.getSupplierId());
        vo.setSupplierName(payment.getSupplierName());
        vo.setPaymentAmount(payment.getPaymentAmount());
        vo.setPaymentAmountDisplay(formatFenToYuan(payment.getPaymentAmount()));
        vo.setPaymentMethod(payment.getPaymentMethod());
        vo.setPaymentMethodName(getPaymentMethodName(payment.getPaymentMethod()));
        vo.setBankAccountId(payment.getBankAccountId());
        vo.setBankAccountName(payment.getBankAccountName());
        vo.setPaymentDate(payment.getPaymentDate());
        vo.setFundFlowId(payment.getFundFlowId());
        vo.setVoucherId(payment.getVoucherId());
        vo.setStatus(payment.getStatus());
        vo.setStatusName(payment.getStatus() != null && payment.getStatus() == 1 ? "已确认" : "已作废");
        vo.setRemark(payment.getRemark());
        vo.setVoidTime(payment.getVoidTime());
        vo.setVoidRemark(payment.getVoidRemark());
        vo.setVoidVoucherId(payment.getVoidVoucherId());
        vo.setVoidVoucherNo(payment.getVoidVoucherNo());
        vo.setVoidFundFlowId(payment.getVoidFundFlowId());
        vo.setVoidFundFlowNo(payment.getVoidFundFlowNo());
        vo.setCreateUserId(payment.getCreateUserId());
        vo.setCreateUserName(payment.getCreateUserName());
        vo.setCreateTime(payment.getCreateTime());

        // 优先使用付款单上冗余保存的编号，确保历史查询无需再查凭证/流水
        vo.setVoucherNo(payment.getVoucherNo());
        vo.setFundFlowNo(payment.getFundFlowNo());
        if (voucherVO != null && vo.getVoucherNo() == null) {
            vo.setVoucherNo(voucherVO.getVoucherNo());
        }
        if (flowVO != null && vo.getFundFlowNo() == null) {
            vo.setFundFlowNo(flowVO.getFlowNo());
        }
        return vo;
    }

    /**
     * 分转元（保留2位小数）
     */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }

    /**
     * 获取付款方式名称
     */
    private String getPaymentMethodName(String method) {
        if (method == null) return "未知";
        switch (method) {
            case "bank_transfer": return "银行转账";
            case "cash": return "现金";
            case "check": return "支票";
            default: return method;
        }
    }
}
