package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.purchase.PurchaseSettlementCreateDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementQueryDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementUpdateDTO;
import com.foodtraceability.dto.finance.PaymentCreateDTO;
import com.foodtraceability.entity.PurchaseSettlement;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.mapper.PurchaseSettlementMapper;
import com.foodtraceability.service.PurchaseSettlementService;
import com.foodtraceability.service.finance.BankAccountService;
import com.foodtraceability.service.finance.PayableService;
import com.foodtraceability.service.finance.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 采购结算单服务实现类
 * 管理供应商应付账款的结算流程：创建 -> 部分付款 -> 财务审核 -> 完成
 *
 * <p>状态编码：
 * <ul>
 *   <li>0 - 待结算（pending）</li>
 *   <li>1 - 部分结算（partial）</li>
 *   <li>2 - 财务审核中（finance_reviewing）</li>
 *   <li>3 - 已完成（completed）</li>
 *   <li>4 - 已逾期（overdue）</li>
 * </ul>
 *
 * <p>发票状态：0未开票 1已开票 2已收票</p>
 */
@Service
public class PurchaseSettlementServiceImpl
        extends ServiceImpl<PurchaseSettlementMapper, PurchaseSettlement>
        implements PurchaseSettlementService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseSettlementServiceImpl.class);

    /** 结算状态常量 */
    private static final int STATUS_PENDING = 0;            // 待结算
    private static final int STATUS_PARTIAL = 1;            // 部分结算
    private static final int STATUS_FINANCE_REVIEWING = 2;  // 财务审核中
    private static final int STATUS_COMPLETED = 3;          // 已完成
    private static final int STATUS_OVERDUE = 4;            // 已逾期

    /** 发票状态常量 */
    private static final int INVOICE_NONE = 0;              // 未开票
    private static final int INVOICE_ISSUED = 1;           // 已开票
    private static final int INVOICE_RECEIVED = 2;         // 已收票

    /** 结算操作类型 */
    private static final String ACTION_PAY = "pay";
    private static final String ACTION_COMPLETE = "complete";

    private final PurchaseSettlementMapper purchaseSettlementMapper;
    private final PayableService payableService;
    private final PaymentService paymentService;
    private final BankAccountService bankAccountService;

    public PurchaseSettlementServiceImpl(PurchaseSettlementMapper purchaseSettlementMapper,
                                         PayableService payableService,
                                         PaymentService paymentService,
                                         BankAccountService bankAccountService) {
        this.purchaseSettlementMapper = purchaseSettlementMapper;
        this.payableService = payableService;
        this.paymentService = paymentService;
        this.bankAccountService = bankAccountService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseSettlement> getSettlementPage(PurchaseSettlementQueryDTO queryDTO) {
        long current = queryDTO.getCurrent() != null ? queryDTO.getCurrent() : 1L;
        long size = queryDTO.getSize() != null ? queryDTO.getSize() : 10L;
        Page<PurchaseSettlement> page = new Page<>(current, size);

        LambdaQueryWrapper<PurchaseSettlement> wrapper = new LambdaQueryWrapper<>();

        // 精确字段筛选
        if (queryDTO.getStatus() != null) {
            wrapper.eq(PurchaseSettlement::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getInvoiceStatus() != null) {
            wrapper.eq(PurchaseSettlement::getInvoiceStatus, queryDTO.getInvoiceStatus());
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(PurchaseSettlement::getSupplierId, queryDTO.getSupplierId());
        }
        if (StringUtils.hasText(queryDTO.getSettlementNo())) {
            wrapper.like(PurchaseSettlement::getSettlementNo, queryDTO.getSettlementNo());
        }
        if (StringUtils.hasText(queryDTO.getOrderNo())) {
            wrapper.like(PurchaseSettlement::getOrderNo, queryDTO.getOrderNo());
        }
        if (StringUtils.hasText(queryDTO.getSupplierName())) {
            wrapper.like(PurchaseSettlement::getSupplierName, queryDTO.getSupplierName());
        }

        // 到期日期范围筛选
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(PurchaseSettlement::getDueDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(PurchaseSettlement::getDueDate, queryDTO.getEndDate());
        }

        // 关键词模糊匹配（结算单编号/订单编号/供应商名称）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword();
            wrapper.and(w -> w.like(PurchaseSettlement::getSettlementNo, kw)
                    .or().like(PurchaseSettlement::getOrderNo, kw)
                    .or().like(PurchaseSettlement::getSupplierName, kw));
        }

        wrapper.orderByDesc(PurchaseSettlement::getCreateTime);
        return purchaseSettlementMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseSettlement getSettlementDetail(Long settlementId) {
        PurchaseSettlement settlement = purchaseSettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购结算单不存在");
        }
        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSettlement createSettlement(PurchaseSettlementCreateDTO createDTO) {
        // 构建结算单实体
        PurchaseSettlement settlement = new PurchaseSettlement();
        settlement.setSettlementNo(generateSettlementNo());
        settlement.setOrderId(createDTO.getOrderId());
        settlement.setOrderNo(createDTO.getOrderNo());
        settlement.setSupplierId(createDTO.getSupplierId());
        settlement.setSupplierName(createDTO.getSupplierName());
        settlement.setTotalAmount(createDTO.getTotalAmount());
        // 初始化付款金额：已付 0，未付 = 总金额
        settlement.setPaidAmount(0L);
        settlement.setUnpaidAmount(createDTO.getTotalAmount());
        settlement.setDueDate(createDTO.getDueDate());
        settlement.setStatus(STATUS_PENDING);
        settlement.setPaymentMethod(createDTO.getPaymentMethod());
        settlement.setInvoiceNo(null);
        settlement.setInvoiceStatus(INVOICE_NONE);
        settlement.setRemark(createDTO.getRemark());

        purchaseSettlementMapper.insert(settlement);
        log.info("创建采购结算单成功，编号：{}", settlement.getSettlementNo());

        return getSettlementDetail(settlement.getSettlementId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSettlement updateSettlement(Long settlementId, PurchaseSettlementUpdateDTO updateDTO) {
        PurchaseSettlement settlement = getSettlementDetail(settlementId);

        // 仅待结算/部分结算状态可更新
        if (settlement.getStatus() != STATUS_PENDING && settlement.getStatus() != STATUS_PARTIAL) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "只有待结算或部分结算状态的结算单可以修改");
        }

        // 更新可选字段
        if (updateDTO.getTotalAmount() != null) {
            long oldTotal = settlement.getTotalAmount() != null ? settlement.getTotalAmount() : 0L;
            long paid = settlement.getPaidAmount() != null ? settlement.getPaidAmount() : 0L;
            settlement.setTotalAmount(updateDTO.getTotalAmount());
            // 重新计算未付金额（保持已付金额不变）
            long newUnpaid = updateDTO.getTotalAmount() - paid;
            settlement.setUnpaidAmount(Math.max(0L, newUnpaid));
            // 若已付金额 > 0 且未付为 0，状态变更为部分结算
            if (paid > 0 && newUnpaid <= 0 && settlement.getStatus() == STATUS_PENDING) {
                settlement.setStatus(STATUS_PARTIAL);
            }
        }
        if (updateDTO.getDueDate() != null) {
            settlement.setDueDate(updateDTO.getDueDate());
        }
        if (updateDTO.getPaymentMethod() != null) {
            settlement.setPaymentMethod(updateDTO.getPaymentMethod());
        }
        if (updateDTO.getRemark() != null) {
            settlement.setRemark(updateDTO.getRemark());
        }

        purchaseSettlementMapper.updateById(settlement);
        log.info("更新采购结算单成功，ID：{}", settlementId);

        return getSettlementDetail(settlementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSettlement(Long settlementId) {
        PurchaseSettlement settlement = getSettlementDetail(settlementId);

        // 仅待结算状态的结算单可删除
        if (settlement.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "只有待结算状态的结算单可以删除");
        }

        // 逻辑删除（MyBatis-Plus @TableLogic 自动处理）
        purchaseSettlementMapper.deleteById(settlementId);
        log.info("删除采购结算单成功，ID：{}", settlementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSettlement settle(Long settlementId, String action, String voucherNo) {
        PurchaseSettlement settlement = getSettlementDetail(settlementId);

        if (ACTION_PAY.equals(action)) {
            return executePay(settlement, voucherNo);
        } else if (ACTION_COMPLETE.equals(action)) {
            return executeComplete(settlement);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的操作类型：" + action + "，仅支持 pay/complete");
        }
    }

    /**
     * 执行付款操作
     * <p>记录全额付款，paidAmount = totalAmount，unpaidAmount = 0。
     * 若当前状态为 finance_reviewing 则流转到 completed，否则流转到 finance_reviewing。</p>
     */
    private PurchaseSettlement executePay(PurchaseSettlement settlement, String voucherNo) {
        Integer currentStatus = settlement.getStatus();
        // 仅待结算/部分结算/财务审核中状态可执行付款
        if (currentStatus != STATUS_PENDING
                && currentStatus != STATUS_PARTIAL
                && currentStatus != STATUS_FINANCE_REVIEWING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "当前状态不允许执行付款操作");
        }

        // 记录全额付款
        settlement.setPaidAmount(settlement.getTotalAmount());
        settlement.setUnpaidAmount(0L);

        // 状态流转：财务审核中 → 已完成；其他 → 财务审核中（付款完成等待审核）
        if (currentStatus == STATUS_FINANCE_REVIEWING) {
            settlement.setStatus(STATUS_COMPLETED);
        } else {
            settlement.setStatus(STATUS_FINANCE_REVIEWING);
        }

        // 凭证号记录到备注（避免实体类无 voucherNo 字段）
        if (StringUtils.hasText(voucherNo)) {
            String existingRemark = settlement.getRemark();
            String voucherRecord = "付款凭证号：" + voucherNo;
            settlement.setRemark(StringUtils.hasText(existingRemark)
                    ? existingRemark + " | " + voucherRecord
                    : voucherRecord);
        }

        purchaseSettlementMapper.updateById(settlement);
        log.info("采购结算单付款成功，ID：{}，凭证号：{}", settlement.getSettlementId(), voucherNo);

        // F5：结算付款联动应付/付款单（消除结算与应付双轨不一致）
        linkPaymentToPayables(settlement);

        return getSettlementDetail(settlement.getSettlementId());
    }

    /**
     * F5：采购结算付款后联动关联应付账款。
     * 优先走正式付款单（registerPayment 四账联动：应付+流水+凭证+采购订单回写）；
     * 无可用银行账户时降级为应付直确（confirmPayment）。
     * 联动失败仅记录日志，不影响结算单主流程。
     */
    private void linkPaymentToPayables(PurchaseSettlement settlement) {
        try {
            if (settlement.getOrderNo() == null || settlement.getTotalAmount() == null) {
                return;
            }
            List<Payable> payables = payableService.lambdaQuery()
                    .eq(Payable::getOrderNo, settlement.getOrderNo())
                    .eq(Payable::getDeleted, 0)
                    .list();
            if (payables.isEmpty()) {
                log.info("F5 结算无关联应付账款，跳过联动：settlementNo={}", settlement.getSettlementNo());
                return;
            }

            long totalToPay = settlement.getTotalAmount();
            Long defaultAccountId = getDefaultBankAccountId();
            for (Payable payable : payables) {
                if (totalToPay <= 0) {
                    break;
                }
                long balance = payable.getBalanceAmount() != null ? payable.getBalanceAmount() : 0L;
                if (balance <= 0) {
                    continue;
                }
                long payNow = Math.min(balance, totalToPay);
                try {
                    if (defaultAccountId != null) {
                        PaymentCreateDTO dto = new PaymentCreateDTO();
                        dto.setPayableId(payable.getPayableId());
                        dto.setPaymentAmount(payNow);
                        dto.setPaymentMethod(settlement.getPaymentMethod() != null
                                ? settlement.getPaymentMethod() : "bank_transfer");
                        dto.setBankAccountId(defaultAccountId);
                        dto.setPaymentDate(LocalDate.now());
                        dto.setRemark("采购结算单支付 - " + settlement.getSettlementNo());
                        paymentService.registerPayment(dto);
                    } else {
                        payableService.confirmPayment(payable.getPayableId(), payNow);
                    }
                } catch (Exception e) {
                    log.error("F5 付款单联动失败，降级应付直确：payableId={}, 错误={}",
                            payable.getPayableId(), e.getMessage());
                    try {
                        payableService.confirmPayment(payable.getPayableId(), payNow);
                    } catch (Exception e2) {
                        log.error("F5 应付直确也失败：payableId={}, 错误={}",
                                payable.getPayableId(), e2.getMessage());
                    }
                }
                totalToPay -= payNow;
            }
        } catch (Exception e) {
            log.error("F5 结算联动应付失败：settlementId={}, 错误={}",
                    settlement.getSettlementId(), e.getMessage());
        }
    }

    /** 取默认银行账户（第一个启用的），无则返回 null */
    private Long getDefaultBankAccountId() {
        try {
            List<BankAccount> accounts = bankAccountService.list();
            for (BankAccount account : accounts) {
                if (account.getStatus() == null || account.getStatus() == 1) {
                    return account.getAccountId();
                }
            }
        } catch (Exception e) {
            log.warn("F5 查询默认银行账户失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 执行完成操作
     * <p>强制流转到已完成状态（仅财务审核中/部分结算可调用）。</p>
     */
    private PurchaseSettlement executeComplete(PurchaseSettlement settlement) {
        Integer currentStatus = settlement.getStatus();
        // 仅部分结算/财务审核中状态可标记完成
        if (currentStatus != STATUS_PARTIAL && currentStatus != STATUS_FINANCE_REVIEWING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "只有部分结算或财务审核中状态的结算单可以标记完成");
        }

        settlement.setStatus(STATUS_COMPLETED);
        // 若有未付金额，标记完成时自动结清
        if (settlement.getUnpaidAmount() != null && settlement.getUnpaidAmount() > 0L) {
            settlement.setPaidAmount(settlement.getTotalAmount());
            settlement.setUnpaidAmount(0L);
        }

        purchaseSettlementMapper.updateById(settlement);
        log.info("采购结算单标记完成成功，ID：{}", settlement.getSettlementId());

        return getSettlementDetail(settlement.getSettlementId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSettlement applyInvoice(Long settlementId) {
        PurchaseSettlement settlement = getSettlementDetail(settlementId);

        // 仅未开票状态可申请发票
        if (settlement.getInvoiceStatus() != null && settlement.getInvoiceStatus() != INVOICE_NONE) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "只有未开票状态的结算单可以申请发票");
        }

        settlement.setInvoiceStatus(INVOICE_ISSUED);
        purchaseSettlementMapper.updateById(settlement);
        log.info("采购结算单申请发票成功，ID：{}", settlementId);

        return getSettlementDetail(settlementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseSettlement receiveInvoice(Long settlementId, String invoiceNo) {
        if (!StringUtils.hasText(invoiceNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发票号不能为空");
        }

        PurchaseSettlement settlement = getSettlementDetail(settlementId);

        // 仅已开票状态可确认收票
        if (settlement.getInvoiceStatus() == null
                || settlement.getInvoiceStatus() != INVOICE_ISSUED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "只有已开票状态的结算单可以确认收票");
        }

        settlement.setInvoiceNo(invoiceNo);
        settlement.setInvoiceStatus(INVOICE_RECEIVED);
        purchaseSettlementMapper.updateById(settlement);
        log.info("采购结算单确认收票成功，ID：{}，发票号：{}", settlementId, invoiceNo);

        return getSettlementDetail(settlementId);
    }

    /**
     * 生成结算单编号
     * 格式：STL + 年月日 + 4位序号（如 STL20260629001）
     */
    private String generateSettlementNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "STL" + dateStr;

        LambdaQueryWrapper<PurchaseSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseSettlement::getSettlementNo, prefix);
        wrapper.orderByDesc(PurchaseSettlement::getSettlementNo);
        wrapper.last("LIMIT 1");
        PurchaseSettlement lastSettlement = purchaseSettlementMapper.selectOne(wrapper);

        int seq = 1;
        if (lastSettlement != null && lastSettlement.getSettlementNo() != null) {
            String lastNo = lastSettlement.getSettlementNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%04d", seq);
    }
}
