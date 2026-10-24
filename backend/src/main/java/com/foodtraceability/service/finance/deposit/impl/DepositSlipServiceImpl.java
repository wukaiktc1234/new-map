package com.foodtraceability.service.finance.deposit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.common.finance.AtomicBalanceUpdater;
import com.foodtraceability.dto.finance.deposit.DepositSlipQueryDTO;
import com.foodtraceability.dto.finance.deposit.DepositSlipVO;
import com.foodtraceability.dto.finance.deposit.DepositToBankDTO;
import com.foodtraceability.dto.finance.deposit.GenerateSlipDTO;
import com.foodtraceability.dto.finance.deposit.GenerateSlipResultVO;
import com.foodtraceability.dto.finance.deposit.ReceiveSlipDTO;
import com.foodtraceability.dto.finance.deposit.RejectSlipDTO;
import com.foodtraceability.dto.finance.deposit.ReverseSlipDTO;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.entity.PosShift;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.entity.finance.recon.BankTransaction;
import com.foodtraceability.entity.finance.recon.DepositSlip;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.mapper.PosShiftMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.mapper.finance.recon.BankTransactionMapper;
import com.foodtraceability.mapper.finance.recon.DepositSlipMapper;
import com.foodtraceability.service.finance.FinanceAuditLogService;
import com.foodtraceability.service.finance.deposit.DepositSlipService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 现金缴款单实现（P2-A · SL1–SL11）。
 *
 * <p><b>五条红线（实现约束，逐条对应）</b></p>
 * <ol>
 *   <li><b>三段式</b>：支付 SALE 不动；{@link #receive} 不写任何 fund_flows；只有 {@link #deposit}
 *       生成<b>恰好 2 条</b>内部转账（现金户出 flow_direction=2、银行户入 flow_direction=1，
 *       两者 flow_category=5、金额均 = actual_amount）。</li>
 *   <li><b>幂等键写在 remark</b>：内部转账 {@code [depositSlip=<slip_no>]}；冲正反向流水
 *       {@code [reconReverseOf=<原 flow_no>]}。重复 deposit/reverse 一律 400，流水条数不变。</li>
 *   <li><b>冲正只增反向流水</b>：原流水置 {@code status=3}，禁止物理删除/改历史金额；
 *       反向流水 balance_after 取原子更新后的余额，不手填。</li>
 *   <li><b>余额只走乐观锁</b>：统一经 {@link AtomicBalanceUpdater}
 *       （UPDATE ... SET balance = balance + delta, version = version + 1 WHERE version = expected），
 *       绝不调用 {@code BankAccountServiceImpl.updateBalance}（裸 SET 绝对值）。</li>
 *   <li><b>短款不改 closing_cash</b>：应交额以 order_payment_records（payment_method=1）重算为权威，
 *       pos_shifts.closing_cash 只作展示。</li>
 * </ol>
 *
 * <p><b>其他硬约束</b>：E3-8 存行金额 = bank_transactions.amount；E3-9 存行账户 store_id = 单据 store_id；
 * 四眼（receive 人 ≠ verify 人）。异常统一 {@link BusinessException}（code=400/404）。</p>
 *
 * <p><b>审计（AC-29）</b>：submit / receive / deposit / verify / close / reject / reverse 七个动作各写一条
 * finance_audit_logs（operator_id、operation_type、target_id=slip_id、target_type=DEPOSIT_SLIP、
 * operation_desc 含 slip_no + 关键金额 + 前后状态、operation_data 为 JSON）。
 * 注：finance_audit_logs.target_id 是 bigint，无法直接存 slip_no（DS…），故单号写在 desc/data 里。</p>
 *
 * <p><b>权限码说明</b>：SL9 reject 暂挂 {@code finance:deposit-slip:close} —— permissions 表没有 reject 独立码，
 * 按 Owner「不新增权限码」约束沿用 close，待 Owner 决定是否新增独立码。</p>
 */
@Service
public class DepositSlipServiceImpl implements DepositSlipService {

    private static final Logger log = LoggerFactory.getLogger(DepositSlipServiceImpl.class);

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final String ST_DRAFT = "DRAFT";
    public static final String ST_SUBMITTED = "SUBMITTED";
    public static final String ST_RECEIVED = "RECEIVED";
    public static final String ST_DEPOSITED = "DEPOSITED";
    public static final String ST_VERIFIED = "VERIFIED";
    public static final String ST_CLOSED = "CLOSED";
    public static final String ST_REJECTED = "REJECTED";
    public static final String ST_REVERSED = "REVERSED";

    /** 内部转账分类（design §5.7：flow_category=5） */
    private static final int FLOW_CATEGORY_INTERNAL_TRANSFER = 5;
    private static final int DIRECTION_IN = 1;
    private static final int DIRECTION_OUT = 2;
    private static final int FLOW_STATUS_DONE = 2;
    private static final int FLOW_STATUS_CANCELLED = 3;

    private final DepositSlipMapper depositSlipMapper;
    private final PosShiftMapper posShiftMapper;
    private final OrderNewMapper orderNewMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordMapper;
    private final BankAccountMapper bankAccountMapper;
    private final BankTransactionMapper bankTransactionMapper;
    private final FundFlowMapper fundFlowMapper;
    private final AtomicBalanceUpdater atomicBalanceUpdater;
    private final FinanceAuditLogService financeAuditLogService;

    public DepositSlipServiceImpl(DepositSlipMapper depositSlipMapper,
                                  PosShiftMapper posShiftMapper,
                                  OrderNewMapper orderNewMapper,
                                  OrderPaymentRecordNewMapper orderPaymentRecordMapper,
                                  BankAccountMapper bankAccountMapper,
                                  BankTransactionMapper bankTransactionMapper,
                                  FundFlowMapper fundFlowMapper,
                                  AtomicBalanceUpdater atomicBalanceUpdater,
                                  FinanceAuditLogService financeAuditLogService) {
        this.depositSlipMapper = depositSlipMapper;
        this.posShiftMapper = posShiftMapper;
        this.orderNewMapper = orderNewMapper;
        this.orderPaymentRecordMapper = orderPaymentRecordMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.bankTransactionMapper = bankTransactionMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.atomicBalanceUpdater = atomicBalanceUpdater;
        this.financeAuditLogService = financeAuditLogService;
    }

    // ==================== SL1 生成 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateSlipResultVO generate(GenerateSlipDTO dto) {
        if (dto == null || dto.getShiftId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "shiftId 不能为空");
        }
        PosShift shift = posShiftMapper.selectById(dto.getShiftId());
        if (shift == null || Integer.valueOf(1).equals(shift.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "班次不存在：shiftId=" + dto.getShiftId());
        }

        DepositSlip existing = findByShiftId(dto.getShiftId());
        if (existing != null) {
            log.info("[METRIC] deposit_slip_reused shiftId={} slipNo={}", dto.getShiftId(), existing.getSlipNo());
            return new GenerateSlipResultVO(true, toVO(existing));
        }

        long storeId = dto.getStoreId() != null ? dto.getStoreId() : resolveStoreId(shift);
        long expected = sumCashPayments(shift, storeId);

        DepositSlip slip = new DepositSlip();
        slip.setSlipNo(nextSlipNo());
        slip.setStoreId(storeId);
        slip.setShiftId(shift.getShiftId());
        slip.setCashierEmployeeId(parseEmployeeId(shift.getEmployeeId()));
        slip.setCashierName(shift.getEmployeeName());
        // TD-03：应交额以 order_payment_records 现金支付重算为权威；closing_cash 只展示，不参与计算
        slip.setExpectedAmount(expected);
        slip.setActualAmount(0L);
        slip.setDifferenceAmount(expected);
        slip.setStatus(ST_DRAFT);
        Long operatorId = currentOperatorId();
        slip.setCreateUserId(operatorId == null ? 0L : operatorId);
        slip.setCreatedBy(currentOperatorName());
        slip.setCreateTime(LocalDateTime.now());
        slip.setUpdateTime(LocalDateTime.now());
        slip.setVersion(0);
        slip.setDeleted(0);
        depositSlipMapper.insert(slip);
        log.info("[METRIC] deposit_slip_generated slipNo={} shiftId={} storeId={} expected={}分 closingCash={}",
                slip.getSlipNo(), shift.getShiftId(), storeId, expected, shift.getClosingCash());
        return new GenerateSlipResultVO(false, toVO(slip));
    }

    // ==================== SL2 / SL3 / SL11 ====================

    @Override
    public IPage<DepositSlipVO> list(DepositSlipQueryDTO query) {
        DepositSlipQueryDTO condition = query == null ? new DepositSlipQueryDTO() : query;
        LambdaQueryWrapper<DepositSlip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepositSlip::getDeleted, 0);
        if (condition.getStoreId() != null) {
            wrapper.eq(DepositSlip::getStoreId, condition.getStoreId());
        }
        if (condition.getShiftId() != null) {
            wrapper.eq(DepositSlip::getShiftId, condition.getShiftId());
        }
        if (condition.getStatus() != null && !condition.getStatus().isBlank()) {
            wrapper.eq(DepositSlip::getStatus, condition.getStatus().trim());
        }
        if (condition.getSlipNo() != null && !condition.getSlipNo().isBlank()) {
            wrapper.like(DepositSlip::getSlipNo, condition.getSlipNo().trim());
        }
        wrapper.orderByDesc(DepositSlip::getSlipId);
        Page<DepositSlip> page = new Page<>(normalizeCurrent(condition.getCurrent()), normalizeSize(condition.getSize()));
        return depositSlipMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public DepositSlipVO detail(Long slipId) {
        return toVO(requireSlip(slipId));
    }

    @Override
    public String exportCsv(DepositSlipQueryDTO query) {
        DepositSlipQueryDTO condition = query == null ? new DepositSlipQueryDTO() : query;
        condition.setCurrent(1L);
        condition.setSize(5000L);
        List<DepositSlipVO> rows = list(condition).getRecords();
        StringBuilder sb = new StringBuilder();
        sb.append("slipNo,storeId,shiftId,status,expectedAmount,actualAmount,differenceAmount,")
          .append("cashAccountId,bankAccountId,depositBankTxnId,submittedAt,receivedAt,depositedAt,verifiedAt,closedAt\n");
        for (DepositSlipVO vo : rows) {
            sb.append(csv(vo.getSlipNo())).append(',')
              .append(vo.getStoreId()).append(',')
              .append(vo.getShiftId()).append(',')
              .append(vo.getStatus()).append(',')
              .append(vo.getExpectedAmount()).append(',')
              .append(vo.getActualAmount()).append(',')
              .append(vo.getDifferenceAmount()).append(',')
              .append(vo.getCashAccountId()).append(',')
              .append(vo.getBankAccountId()).append(',')
              .append(vo.getDepositBankTxnId()).append(',')
              .append(vo.getSubmittedAt()).append(',')
              .append(vo.getReceivedAt()).append(',')
              .append(vo.getDepositedAt()).append(',')
              .append(vo.getVerifiedAt()).append(',')
              .append(vo.getClosedAt()).append('\n');
        }
        return sb.toString();
    }

    // ==================== SL4 提交 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO submit(Long slipId) {
        DepositSlip slip = requireSlip(slipId);
        requireStatus(slip, "提交", ST_DRAFT);
        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_SUBMITTED)
                .set(DepositSlip::getSubmittedAt, now)
                .set(DepositSlip::getUpdateTime, now));
        audit(slip, "DEPOSIT_SLIP_SUBMIT",
                "缴款单提交：slipNo=" + slip.getSlipNo() + " 应交=" + slip.getExpectedAmount()
                        + "分 DRAFT→SUBMITTED",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"DRAFT\",\"statusAfter\":\"SUBMITTED\""
                        + ",\"expectedAmount\":" + slip.getExpectedAmount() + "}");
        log.info("[METRIC] deposit_slip_submitted slipNo={} operator={}", slip.getSlipNo(), currentOperatorId());
        return detail(slipId);
    }

    // ==================== SL5 收款（不生成流水） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO receive(Long slipId, ReceiveSlipDTO dto) {
        DepositSlip slip = requireSlip(slipId);
        requireStatus(slip, "收款", ST_SUBMITTED);
        if (dto == null || dto.getReceivedAmountFen() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "receivedAmountFen 不能为空");
        }
        long received = dto.getReceivedAmountFen();
        if (received < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实收金额不能为负：receivedAmountFen=" + received);
        }
        Long operatorId = requireOperatorId("收款");
        long expected = slip.getExpectedAmount() == null ? 0L : slip.getExpectedAmount();
        long difference = expected - received; // 正=短款，负=长款

        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_RECEIVED)
                .set(DepositSlip::getActualAmount, received)
                .set(DepositSlip::getDifferenceAmount, difference)
                .set(DepositSlip::getReceivedAt, now)
                .set(DepositSlip::getCollectorUserId, operatorId)
                .set(DepositSlip::getCollectorName, currentOperatorName())
                .set(DepositSlip::getRemark, mergeRemark(slip.getRemark(), dto.getRemark()))
                .set(DepositSlip::getUpdateTime, now));
        // 红线 1/5：本步绝不生成 fund_flows；短款只落在单据 difference_amount，不改 pos_shifts.closing_cash
        audit(slip, "DEPOSIT_SLIP_RECEIVE",
                "缴款单收款：slipNo=" + slip.getSlipNo() + " 应交=" + expected + "分 实收=" + received
                        + "分 短款=" + difference + "分 SUBMITTED→RECEIVED（不生成流水）",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"SUBMITTED\",\"statusAfter\":\"RECEIVED\""
                        + ",\"expectedAmount\":" + expected + ",\"actualAmount\":" + received
                        + ",\"differenceAmount\":" + difference + "}");
        log.info("[METRIC] deposit_slip_received slipNo={} expected={} actual={} difference={} collector={}",
                slip.getSlipNo(), expected, received, difference, operatorId);
        return detail(slipId);
    }

    // ==================== SL6 存行（生成恰好 2 条内部转账） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO deposit(Long slipId, DepositToBankDTO dto) {
        DepositSlip slip = requireSlip(slipId);
        requireStatus(slip, "存行", ST_RECEIVED);
        if (dto == null || dto.getBankTransactionId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bankTransactionId 不能为空");
        }
        // 红线 2：幂等键 [depositSlip=<slip_no>] 已存在 → 400，且流水条数不变
        if (existsSlipTransferFlow(slip.getSlipNo())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该缴款单已入账（幂等键 [depositSlip=" + slip.getSlipNo() + "] 已存在），不可重复存行");
        }

        long amount = slip.getActualAmount() == null ? 0L : slip.getActualAmount();
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "实收金额必须大于 0 才能存行：slipNo=" + slip.getSlipNo() + " actualAmount=" + amount);
        }

        BankTransaction txn = bankTransactionMapper.selectById(dto.getBankTransactionId());
        if (txn == null || Integer.valueOf(1).equals(txn.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "存行流水不存在：bankTransactionId=" + dto.getBankTransactionId());
        }
        BankAccount bankAccount = bankAccountMapper.selectById(txn.getAccountId());
        if (bankAccount == null || Integer.valueOf(1).equals(bankAccount.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "存行流水对应账户不存在：accountId=" + txn.getAccountId());
        }
        if (bankAccount.getStatus() == null || bankAccount.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存行账户已停用：accountId=" + bankAccount.getAccountId());
        }
        // E3-9：存行账户的门店必须等于单据门店
        if (bankAccount.getStoreId() == null || !bankAccount.getStoreId().equals(slip.getStoreId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "E3-9 校验失败：存行账户门店与缴款单门店不一致（slip.storeId=" + slip.getStoreId()
                            + ", account.storeId=" + bankAccount.getStoreId() + "）");
        }
        // E3-8：存行金额必须等于关联外部流水金额
        if (txn.getAmount() == null || txn.getAmount() != amount) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "E3-8 校验失败：存行金额与缴款单实收不一致（bank_transactions.amount=" + txn.getAmount()
                            + ", actualAmount=" + amount + "）");
        }

        BankAccount cashAccount = requireCashAccount(slip.getStoreId());
        if (cashAccount.getAccountId().equals(bankAccount.getAccountId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "现金账户与存行账户是同一个账户，无法做内部转账：accountId=" + cashAccount.getAccountId());
        }

        LocalDate businessDate = businessDateOf(slip);
        Long operatorId = currentOperatorId();
        String operatorName = currentOperatorName();
        String remark = "缴款单存行：" + slip.getSlipNo() + "（现金户→" + bankAccount.getAccountName()
                + "）[depositSlip=" + slip.getSlipNo() + "]";

        // 现金户出
        AtomicBalanceUpdater.BalanceSnapshot cashSnap = atomicBalanceUpdater.addBalance(cashAccount.getAccountId(), -amount);
        String cashFlowNo = insertFlow(cashAccount.getAccountId(), DIRECTION_OUT, amount, cashSnap.getBalanceAfter(),
                businessDate, remark, operatorId, operatorName);
        // 银行户入
        AtomicBalanceUpdater.BalanceSnapshot bankSnap = atomicBalanceUpdater.addBalance(bankAccount.getAccountId(), amount);
        String bankFlowNo = insertFlow(bankAccount.getAccountId(), DIRECTION_IN, amount, bankSnap.getBalanceAfter(),
                businessDate, remark, operatorId, operatorName);

        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_DEPOSITED)
                .set(DepositSlip::getCashAccountId, cashAccount.getAccountId())
                .set(DepositSlip::getBankAccountId, bankAccount.getAccountId())
                .set(DepositSlip::getDepositBankTxnId, txn.getTransactionId())
                .set(DepositSlip::getDepositRefNo, dto.getDepositRefNo() != null
                        ? dto.getDepositRefNo() : txn.getExternalTxnId())
                .set(DepositSlip::getDepositedAt, now)
                .set(DepositSlip::getRemark, mergeRemark(slip.getRemark(), dto.getRemark()))
                .set(DepositSlip::getUpdateTime, now));
        audit(slip, "DEPOSIT_SLIP_DEPOSIT",
                "缴款单存行：slipNo=" + slip.getSlipNo() + " 金额=" + amount + "分 现金户=" + cashAccount.getAccountId()
                        + "(余额" + cashSnap.getBalanceBefore() + "→" + cashSnap.getBalanceAfter()
                        + ") 银行户=" + bankAccount.getAccountId()
                        + "(余额" + bankSnap.getBalanceBefore() + "→" + bankSnap.getBalanceAfter()
                        + ") 流水=" + cashFlowNo + "/" + bankFlowNo + " RECEIVED→DEPOSITED",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"RECEIVED\",\"statusAfter\":\"DEPOSITED\""
                        + ",\"amount\":" + amount + ",\"cashAccountId\":" + cashAccount.getAccountId()
                        + ",\"bankAccountId\":" + bankAccount.getAccountId()
                        + ",\"bankTransactionId\":" + txn.getTransactionId()
                        + ",\"flowNos\":[\"" + cashFlowNo + "\",\"" + bankFlowNo + "\"]}");
        log.info("[METRIC] deposit_slip_deposited slipNo={} amount={} cash={}({}→v{}) bank={}({}→v{})",
                slip.getSlipNo(), amount,
                cashAccount.getAccountId(), cashSnap.getBalanceAfter(), cashSnap.getVersionAfter(),
                bankAccount.getAccountId(), bankSnap.getBalanceAfter(), bankSnap.getVersionAfter());
        return detail(slipId);
    }

    // ==================== SL7 复核（四眼） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO verify(Long slipId) {
        DepositSlip slip = requireSlip(slipId);
        requireStatus(slip, "复核", ST_DEPOSITED);
        Long verifier = requireOperatorId("复核");
        if (slip.getCollectorUserId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缴款单缺少收款人，无法做四眼校验：slipNo=" + slip.getSlipNo());
        }
        if (slip.getCollectorUserId().equals(verifier)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "四眼校验失败：复核人不能与收款人相同（userId=" + verifier + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_VERIFIED)
                .set(DepositSlip::getVerifiedAt, now)
                .set(DepositSlip::getUpdateTime, now));
        audit(slip, "DEPOSIT_SLIP_VERIFY",
                "缴款单复核：slipNo=" + slip.getSlipNo() + " 复核人=" + verifier
                        + " 收款人=" + slip.getCollectorUserId() + " DEPOSITED→VERIFIED（四眼校验通过）",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"DEPOSITED\",\"statusAfter\":\"VERIFIED\""
                        + ",\"verifier\":" + verifier + ",\"collector\":" + slip.getCollectorUserId() + "}");
        log.info("[METRIC] deposit_slip_verified slipNo={} verifier={} collector={}",
                slip.getSlipNo(), verifier, slip.getCollectorUserId());
        return detail(slipId);
    }

    // ==================== SL8 关闭 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO close(Long slipId) {
        DepositSlip slip = requireSlip(slipId);
        requireStatus(slip, "关闭", ST_VERIFIED);
        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_CLOSED)
                .set(DepositSlip::getClosedAt, now)
                .set(DepositSlip::getUpdateTime, now));
        audit(slip, "DEPOSIT_SLIP_CLOSE",
                "缴款单关闭：slipNo=" + slip.getSlipNo() + " VERIFIED→CLOSED",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"VERIFIED\",\"statusAfter\":\"CLOSED\"}");
        log.info("[METRIC] deposit_slip_closed slipNo={} operator={}", slip.getSlipNo(), currentOperatorId());
        return detail(slipId);
    }

    // ==================== SL9 驳回 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO reject(Long slipId, RejectSlipDTO dto) {
        DepositSlip slip = requireSlip(slipId);
        if (!ST_SUBMITTED.equals(slip.getStatus()) && !ST_RECEIVED.equals(slip.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "仅 SUBMITTED/RECEIVED 可驳回，当前 status=" + slip.getStatus() + "（slipNo=" + slip.getSlipNo() + "）");
        }
        if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回原因不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_REJECTED)
                .set(DepositSlip::getRemark, mergeRemark(slip.getRemark(), "[reject] " + dto.getReason().trim()))
                .set(DepositSlip::getUpdateTime, now));
        audit(slip, "DEPOSIT_SLIP_REJECT",
                "缴款单驳回：slipNo=" + slip.getSlipNo() + " 原因=" + dto.getReason().trim()
                        + " " + slip.getStatus() + "→REJECTED",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"" + slip.getStatus()
                        + "\",\"statusAfter\":\"REJECTED\",\"reason\":\"" + dto.getReason().trim() + "\"}");
        log.info("[METRIC] deposit_slip_rejected slipNo={} reason={}", slip.getSlipNo(), dto.getReason());
        return detail(slipId);
    }

    // ==================== SL10 冲正（只增反向流水） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO reverse(Long slipId, ReverseSlipDTO dto) {
        DepositSlip slip = requireSlip(slipId);
        if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "冲正原因不能为空");
        }
        if (ST_REVERSED.equals(slip.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该缴款单已冲正，不可重复冲正：slipNo=" + slip.getSlipNo());
        }
        if (!ST_DEPOSITED.equals(slip.getStatus()) && !ST_VERIFIED.equals(slip.getStatus())
                && !ST_CLOSED.equals(slip.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前状态没有可冲正的流水：status=" + slip.getStatus() + "（slipNo=" + slip.getSlipNo() + "）");
        }

        List<FundFlow> originals = findSlipTransferFlows(slip.getSlipNo());
        if (originals.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "未找到该缴款单的内部转账流水，无法冲正：slipNo=" + slip.getSlipNo());
        }
        for (FundFlow original : originals) {
            if (!Integer.valueOf(FLOW_STATUS_DONE).equals(original.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "原流水状态不是已完成，不能冲正：flowNo=" + original.getFlowNo() + " status=" + original.getStatus());
            }
            if (existsReverseFlow(original.getFlowNo())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "该流水已冲正（幂等键 [reconReverseOf=" + original.getFlowNo() + "] 已存在）");
            }
        }

        LocalDate businessDate = LocalDate.now(ZONE);
        Long operatorId = currentOperatorId();
        String operatorName = currentOperatorName();
        List<String> reversedFlowNos = new ArrayList<>();
        List<String> originalFlowNos = new ArrayList<>();
        for (FundFlow original : originals) {
            long amount = original.getAmount() == null ? 0L : original.getAmount();
            int reverseDirection = original.getFlowDirection() != null && original.getFlowDirection() == DIRECTION_IN
                    ? DIRECTION_OUT : DIRECTION_IN;
            long delta = reverseDirection == DIRECTION_IN ? amount : -amount;
            AtomicBalanceUpdater.BalanceSnapshot snap = atomicBalanceUpdater.addBalance(original.getAccountId(), delta);
            String remark = "缴款单冲正：" + slip.getSlipNo() + " 原流水 " + original.getFlowNo()
                    + " [reconReverseOf=" + original.getFlowNo() + "]";
            reversedFlowNos.add(insertFlow(original.getAccountId(), reverseDirection, amount, snap.getBalanceAfter(),
                    businessDate, remark, operatorId, operatorName));
            originalFlowNos.add(original.getFlowNo());
            // 红线 3：原流水只置 status=3，不删除、不改金额
            fundFlowMapper.update(null, new LambdaUpdateWrapper<FundFlow>()
                    .eq(FundFlow::getFlowId, original.getFlowId())
                    .set(FundFlow::getStatus, FLOW_STATUS_CANCELLED)
                    .set(FundFlow::getUpdateTime, LocalDateTime.now()));
        }

        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_REVERSED)
                .set(DepositSlip::getRemark, mergeRemark(slip.getRemark(), "[reverse] " + dto.getReason().trim()))
                .set(DepositSlip::getUpdateTime, now));
        audit(slip, "DEPOSIT_SLIP_REVERSE",
                "缴款单冲正：slipNo=" + slip.getSlipNo() + " 原因=" + dto.getReason().trim()
                        + " 原流水=" + originalFlowNos + "（置 status=3） 反向流水=" + reversedFlowNos
                        + " " + slip.getStatus() + "→REVERSED",
                "{\"slipNo\":\"" + slip.getSlipNo() + "\",\"statusBefore\":\"" + slip.getStatus()
                        + "\",\"statusAfter\":\"REVERSED\",\"reason\":\"" + dto.getReason().trim()
                        + "\",\"originalFlowNos\":\"" + originalFlowNos + "\",\"reverseFlowNos\":\""
                        + reversedFlowNos + "\"}");
        log.info("[METRIC] deposit_slip_reversed slipNo={} flows={}", slip.getSlipNo(), originals.size());
        return detail(slipId);
    }

    // ==================== 内部工具 ====================

    private DepositSlip requireSlip(Long slipId) {
        if (slipId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "slipId 不能为空");
        }
        DepositSlip slip = depositSlipMapper.selectById(slipId);
        if (slip == null || Integer.valueOf(1).equals(slip.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "缴款单不存在：slipId=" + slipId);
        }
        return slip;
    }

    private void requireStatus(DepositSlip slip, String action, String expected) {
        if (!expected.equals(slip.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前状态不允许" + action + "：slipNo=" + slip.getSlipNo()
                            + " status=" + slip.getStatus() + "（要求 " + expected + "）");
        }
    }

    private DepositSlip findByShiftId(Long shiftId) {
        List<DepositSlip> rows = depositSlipMapper.selectList(new LambdaQueryWrapper<DepositSlip>()
                .eq(DepositSlip::getShiftId, shiftId)
                .eq(DepositSlip::getDeleted, 0)
                .orderByDesc(DepositSlip::getSlipId)
                .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /** 门店推导：班次时间窗内订单的门店必须唯一，否则 400（不猜、不给默认值） */
    private long resolveStoreId(PosShift shift) {
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "班次缺少起止时间，无法推导门店：shiftId=" + shift.getShiftId());
        }
        List<OrderNew> orders = orderNewMapper.selectList(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getDeleted, 0)
                .ge(OrderNew::getCreateTime, shift.getStartTime())
                .le(OrderNew::getCreateTime, shift.getEndTime())
                .select(OrderNew::getStoreId));
        Set<Long> stores = new LinkedHashSet<>();
        if (orders != null) {
            for (OrderNew order : orders) {
                if (order.getStoreId() != null) {
                    stores.add(order.getStoreId());
                }
            }
        }
        if (stores.size() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "班次无法唯一归属门店（命中 " + stores.size() + " 个：" + stores
                            + "），请在请求里显式传 storeId，或修正班次数据。shiftId=" + shift.getShiftId());
        }
        return stores.iterator().next();
    }

    /** TD-03：应交额 = 该班次该门店 order_payment_records 中现金支付（payment_method=1）的合计（分） */
    private long sumCashPayments(PosShift shift, long storeId) {
        List<OrderNew> orders = orderNewMapper.selectList(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getDeleted, 0)
                .eq(OrderNew::getStoreId, storeId)
                .ge(OrderNew::getCreateTime, shift.getStartTime())
                .le(OrderNew::getCreateTime, shift.getEndTime())
                .select(OrderNew::getOrderId));
        if (orders == null || orders.isEmpty()) {
            return 0L;
        }
        List<String> orderIds = new ArrayList<>();
        for (OrderNew order : orders) {
            if (order.getOrderId() != null) {
                orderIds.add(order.getOrderId());
            }
        }
        if (orderIds.isEmpty()) {
            return 0L;
        }
        List<OrderPaymentRecordNew> payments = orderPaymentRecordMapper.selectList(
                new LambdaQueryWrapper<OrderPaymentRecordNew>()
                        .in(OrderPaymentRecordNew::getOrderId, orderIds)
                        .eq(OrderPaymentRecordNew::getPaymentMethod, 1)
                        .eq(OrderPaymentRecordNew::getDeleted, 0));
        long sum = 0L;
        if (payments != null) {
            for (OrderPaymentRecordNew payment : payments) {
                sum += payment.getPaymentAmount() == null ? 0L : payment.getPaymentAmount();
            }
        }
        return sum;
    }

    /** 门店现金账户必须唯一（account_type=2、启用、未删除） */
    private BankAccount requireCashAccount(Long storeId) {
        List<BankAccount> accounts = bankAccountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getStoreId, storeId)
                .eq(BankAccount::getAccountType, 2)
                .eq(BankAccount::getStatus, 1)
                .eq(BankAccount::getDeleted, 0));
        if (accounts == null || accounts.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店缺少启用的现金账户：storeId=" + storeId);
        }
        if (accounts.size() > 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "门店存在多个现金账户，无法确定出账账户：storeId=" + storeId + " count=" + accounts.size());
        }
        return accounts.get(0);
    }

    private boolean existsSlipTransferFlow(String slipNo) {
        Long count = fundFlowMapper.selectCount(new LambdaQueryWrapper<FundFlow>()
                .like(FundFlow::getRemark, "[depositSlip=" + slipNo + "]"));
        return count != null && count > 0;
    }

    private List<FundFlow> findSlipTransferFlows(String slipNo) {
        return fundFlowMapper.selectList(new LambdaQueryWrapper<FundFlow>()
                .like(FundFlow::getRemark, "[depositSlip=" + slipNo + "]")
                .orderByAsc(FundFlow::getFlowId));
    }

    private boolean existsReverseFlow(String originalFlowNo) {
        Long count = fundFlowMapper.selectCount(new LambdaQueryWrapper<FundFlow>()
                .like(FundFlow::getRemark, "[reconReverseOf=" + originalFlowNo + "]"));
        return count != null && count > 0;
    }

    private String insertFlow(Long accountId, int direction, long amount, long balanceAfter,
                              LocalDate businessDate, String remark, Long operatorId, String operatorName) {
        FundFlow flow = new FundFlow();
        flow.setFlowNo(nextFlowNo());
        flow.setAccountId(accountId);
        flow.setFlowDirection(direction);
        flow.setFlowCategory(FLOW_CATEGORY_INTERNAL_TRANSFER);
        flow.setAmount(amount);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessDate(businessDate);
        flow.setRemark(remark);
        flow.setCreateUserId(operatorId == null ? 0L : operatorId);
        flow.setCreatedBy(operatorName);
        flow.setStatus(FLOW_STATUS_DONE);
        flow.setDeleted(0);
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        fundFlowMapper.insert(flow);
        return flow.getFlowNo();
    }

    /** AC-29：写财务审计（与业务同事务，写不进去就整体回滚，绝不静默吞掉） */
    private void audit(DepositSlip slip, String operationType, String desc, String dataJson) {
        Long operatorId = currentOperatorId();
        boolean ok = financeAuditLogService.log(operatorId, currentOperatorName(), operationType,
                "FINANCE_DEPOSIT_SLIP", slip.getSlipId(), "DEPOSIT_SLIP", desc, dataJson, null);
        if (!ok) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "审计日志写入失败：slipNo=" + slip.getSlipNo() + " op=" + operationType);
        }
    }

    /** 缴款单号：DS + yyyyMMdd + 4 位序号（复用 order_sequence 号段，避免并发撞号） */
    private String nextSlipNo() {
        String datePart = LocalDate.now(ZONE).format(DAY);
        String key = "SLIP" + datePart;
        String prefix = "DS" + datePart;
        // 共用 order_sequence 号段：GREATEST(当前号段, 当天 MAX) + 1 RETURNING（单条语句，非「查 MAX+1」）
        Integer seq = depositSlipMapper.nextSlipSequenceWithFloor(key, prefix);
        if (seq == null || seq <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "缴款单号段分配失败：key=" + key);
        }
        return String.format("%s%04d", prefix, seq);
    }

    /** 流水号：FF + yyyyMMdd + 4 位序号（与 FundFlowServiceImpl 同号段规则） */
    private String nextFlowNo() {
        String datePart = LocalDate.now(ZONE).format(DAY);
        String key = "FLOW" + datePart;
        String prefix = "FF" + datePart;
        // 共用 order_sequence 号段：GREATEST(当前号段, 当天 MAX) + 1 RETURNING（单条语句，非「查 MAX+1」）
        Integer seq = fundFlowMapper.nextFlowSequenceWithFloor(key, prefix);
        if (seq == null || seq <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "流水号段分配失败：key=" + key);
        }
        return String.format("%s%04d", prefix, seq);
    }

    private LocalDate businessDateOf(DepositSlip slip) {
        if (slip.getShiftId() == null) {
            return LocalDate.now(ZONE);
        }
        PosShift shift = posShiftMapper.selectById(slip.getShiftId());
        if (shift == null || shift.getStartTime() == null) {
            return LocalDate.now(ZONE);
        }
        return shift.getStartTime().toLocalDate();
    }

    private Long parseEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(employeeId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long currentOperatorId() {
        return SecurityUtils.getCurrentUserId();
    }

    private Long requireOperatorId(String action) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别操作人（未登录上下文），不允许执行" + action);
        }
        return operatorId;
    }

    private String currentOperatorName() {
        String username = SecurityUtils.getCurrentUsername();
        return username == null || username.isBlank() ? "system" : username;
    }

    private String mergeRemark(String base, String extra) {
        if (extra == null || extra.isBlank()) {
            return base;
        }
        return base == null || base.isBlank() ? extra.trim() : base + " | " + extra.trim();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 5000L);
    }

    private DepositSlipVO toVO(DepositSlip slip) {
        DepositSlipVO vo = new DepositSlipVO();
        vo.setSlipId(slip.getSlipId());
        vo.setSlipNo(slip.getSlipNo());
        vo.setStoreId(slip.getStoreId());
        vo.setShiftId(slip.getShiftId());
        vo.setCashierEmployeeId(slip.getCashierEmployeeId());
        vo.setCashierName(slip.getCashierName());
        vo.setCollectorUserId(slip.getCollectorUserId());
        vo.setCollectorName(slip.getCollectorName());
        vo.setExpectedAmount(slip.getExpectedAmount());
        vo.setActualAmount(slip.getActualAmount());
        vo.setDifferenceAmount(slip.getDifferenceAmount());
        vo.setCashAccountId(slip.getCashAccountId());
        vo.setBankAccountId(slip.getBankAccountId());
        vo.setDepositBankTxnId(slip.getDepositBankTxnId());
        vo.setDepositRefNo(slip.getDepositRefNo());
        vo.setStatus(slip.getStatus());
        vo.setSubmittedAt(slip.getSubmittedAt());
        vo.setReceivedAt(slip.getReceivedAt());
        vo.setDepositedAt(slip.getDepositedAt());
        vo.setVerifiedAt(slip.getVerifiedAt());
        vo.setClosedAt(slip.getClosedAt());
        vo.setReopenOf(slip.getReopenOf());
        vo.setRemark(slip.getRemark());
        vo.setCreateUserId(slip.getCreateUserId());
        vo.setCreatedBy(slip.getCreatedBy());
        vo.setCreateTime(slip.getCreateTime());
        vo.setUpdateTime(slip.getUpdateTime());
        return vo;
    }
}
