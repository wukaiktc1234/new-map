package com.foodtraceability.service.finance.deposit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ConflictException;
import com.foodtraceability.common.finance.FinancePeriodGuard;
import com.foodtraceability.common.sequence.SequenceAllocator;
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
import org.springframework.dao.DuplicateKeyException;
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
    /** P4-1 BL-2：全项目唯一取号实现 */
    private final SequenceAllocator sequenceAllocator;
    /** P4-4 TD-39：期间封账硬拦截（deposit / verify 都要过） */
    private final FinancePeriodGuard financePeriodGuard;

    public DepositSlipServiceImpl(DepositSlipMapper depositSlipMapper,
                                  PosShiftMapper posShiftMapper,
                                  OrderNewMapper orderNewMapper,
                                  OrderPaymentRecordNewMapper orderPaymentRecordMapper,
                                  BankAccountMapper bankAccountMapper,
                                  BankTransactionMapper bankTransactionMapper,
                                  FundFlowMapper fundFlowMapper,
                                  AtomicBalanceUpdater atomicBalanceUpdater,
                                  FinanceAuditLogService financeAuditLogService,
                                  SequenceAllocator sequenceAllocator,
                                  FinancePeriodGuard financePeriodGuard) {
        this.depositSlipMapper = depositSlipMapper;
        this.posShiftMapper = posShiftMapper;
        this.orderNewMapper = orderNewMapper;
        this.orderPaymentRecordMapper = orderPaymentRecordMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.bankTransactionMapper = bankTransactionMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.atomicBalanceUpdater = atomicBalanceUpdater;
        this.financeAuditLogService = financeAuditLogService;
        this.sequenceAllocator = sequenceAllocator;
        this.financePeriodGuard = financePeriodGuard;
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

        // P3 前置：门店归属校验先于幂等命中执行（请求带了错误 storeId 时，即便单据已存在也应 400，
        // 否则调用方会拿到一张「不属于该门店」的既有单据，掩盖数据问题）
        Set<Long> shiftStores = resolveShiftStores(shift);
        if (dto.getStoreId() != null && !shiftStores.contains(dto.getStoreId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "storeId 与班次实际门店不一致：shiftId=" + shift.getShiftId()
                            + "，请求 storeId=" + dto.getStoreId()
                            + "，班次时间窗内实际门店集合=" + shiftStores);
        }

        // 门店解析与校验同样先于幂等命中：缺省推导不唯一时必须 400，不能因为「单据已存在」就放过
        long storeId;
        if (dto.getStoreId() != null) {
            storeId = dto.getStoreId();
        } else {
            if (shiftStores.size() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "班次无法唯一归属门店（命中 " + shiftStores.size() + " 个：" + shiftStores
                                + "），请显式传 storeId（必须是其中之一），或修正班次数据。shiftId=" + shift.getShiftId());
            }
            storeId = shiftStores.iterator().next();
        }

        DepositSlip existing = findByShiftId(dto.getShiftId());
        if (existing != null) {
            log.info("[METRIC] deposit_slip_reused shiftId={} slipNo={}", dto.getShiftId(), existing.getSlipNo());
            return new GenerateSlipResultVO(true, toVO(existing));
        }
        long expected = sumCashPayments(shift, storeId);
        if (expected <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "班次在该门店没有现金支付（payment_method=1）可交，拒绝生成金额为 0 的缴款单：shiftId="
                            + shift.getShiftId() + "，storeId=" + storeId);
        }

        DepositSlip slip = new DepositSlip();
        slip.setSlipNo(nextSlipNo());
        slip.setStoreId(storeId);
        slip.setShiftId(shift.getShiftId());
        // TD-36：业务日期 = 所属班次 pos_shifts.end_time 的日期（交班日），不是生成日/提交日；
        // 跨天班次（22:00→次日 02:00）取次日，即交班当天。
        slip.setBusinessDate(shift.getEndTime() != null ? shift.getEndTime().toLocalDate() : null);
        slip.setCashierEmployeeId(parseEmployeeId(shift.getEmployeeId()));
        slip.setCashierName(shift.getEmployeeName());
        // TD-03：应交额以 order_payment_records 现金支付重算为权威；closing_cash 只展示，不参与计算
        slip.setExpectedAmount(expected);
        // ==================================================================
        // N19-2②：班次内「退款构成」金额（只写 refund_component_fen，**不动 expected/difference**）
        //   口径：Σ 该门店现金户在班次窗口内的现金退款流水（flow_category=2 且 direction=2）
        //   时间判定：refund_time 有值 → 落在 [shift.start, shift.end]；refund_time 为空的历史行
        //   → 退化为「business_date 的日期落在班次日期区间」，并单独计数 fallbackByDate 便于回填。
        // ==================================================================
        RefundComponent component = sumShiftRefundComponent(shift, storeId);
        slip.setRefundComponentFen(component.fen);
        slip.setActualAmount(0L);
        // AC-21 口径：difference_amount = actual_amount − expected_amount；
        // DRAFT 阶段 actual=0，故为 −expected（未收款即全额未交，符号与后续收款一致）
        slip.setDifferenceAmount(0L - expected);
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
        // AC-21（design §5.7 / 第 1005 行）：difference_amount = actual − expected
        //   > 0 → 长款（待查溢余）；< 0 → 短款（待查损溢）；= 0 → 平账
        long difference = received - expected;

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
                        + "分 差额=" + difference + "分（" + differenceLabel(difference)
                        + "）SUBMITTED→RECEIVED（不生成流水）",
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
        // TD-39 / P4-4：期间封账后只读 —— 存行（写 2 条内部转账 + 改余额）必须先过期间门禁
        financePeriodGuard.assertOpen(businessDateOfSlip(slipId));
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

        // BL-4（P3 收口 / 专项审计 §1）：现金户（转出方）实时余额下限校验，统一由
        // AtomicBalanceUpdater#addBalanceGuarded 负责（负 delta 时要求 当前余额 + delta >= 0，
        // 不足抛 ConflictException(409)，文案全项目一套）。此处在写任何流水之前，失败即整体回滚：
        // 余额不变、无流水写入、单据状态不变。A15 巡检（现金户余额非负）保留为事后第二道防线。

        // ===== 并发闸门（审计 §5.2）：先抢单据状态，再动钱 =====
        // 条件更新 RECEIVED → DEPOSITED，且把本次存行的账户/流水/时间一起落库；
        // 影响行数 ≠ 1 说明另一个请求已把状态推进走 → 立刻 409 退出，绝不进入余额与流水。
        // 该语句在事务内、且在 addBalanceGuarded / insertFlow **之前**：
        // 第二个并发事务会阻塞在这一行的行锁上，等第一个提交后匹配 0 行 → 409（幂等键 [depositSlip=]
        // 只能做事后兜底，挡不住同时进来的两个请求，故不能依赖它做并发控制）。
        // 不使用 synchronized/本地锁：多实例部署下无效，DB 条件更新才是唯一有效的仲裁点。
        LocalDateTime now = LocalDateTime.now();
        int guarded = depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .eq(DepositSlip::getStatus, ST_RECEIVED)
                .set(DepositSlip::getStatus, ST_DEPOSITED)
                .set(DepositSlip::getCashAccountId, cashAccount.getAccountId())
                .set(DepositSlip::getBankAccountId, bankAccount.getAccountId())
                .set(DepositSlip::getDepositBankTxnId, txn.getTransactionId())
                .set(DepositSlip::getDepositRefNo, dto.getDepositRefNo() != null
                        ? dto.getDepositRefNo() : txn.getExternalTxnId())
                .set(DepositSlip::getDepositedAt, now)
                .set(DepositSlip::getRemark, mergeRemark(slip.getRemark(), dto.getRemark()))
                .set(DepositSlip::getUpdateTime, now));
        if (guarded != 1) {
            DepositSlip latest = depositSlipMapper.selectById(slipId);
            String actualStatus = latest == null ? "UNKNOWN" : latest.getStatus();
            log.warn("[METRIC] deposit_slip_concurrent_rejected slipNo={} currentStatus={}",
                    slip.getSlipNo(), actualStatus);
            throw new ConflictException("缴款单状态已变更，本次存行未执行（并发或已处理）：slipNo="
                    + slip.getSlipNo() + "，当前状态=" + actualStatus + "，要求状态=" + ST_RECEIVED);
        }

        LocalDate businessDate = businessDateOf(slip);
        Long operatorId = currentOperatorId();
        String operatorName = currentOperatorName();
        String remark = "缴款单存行：" + slip.getSlipNo() + "（现金户→" + bankAccount.getAccountName()
                + "）[depositSlip=" + slip.getSlipNo() + "]";

        // 现金户出
        AtomicBalanceUpdater.BalanceSnapshot cashSnap =
                atomicBalanceUpdater.addBalanceGuarded(cashAccount.getAccountId(), -amount, "存行", null);
        String cashFlowNo = insertFlow(cashAccount.getAccountId(), DIRECTION_OUT, amount, cashSnap.getBalanceAfter(),
                businessDate, remark, operatorId, operatorName);
        // 银行户入
        AtomicBalanceUpdater.BalanceSnapshot bankSnap = atomicBalanceUpdater.addBalance(bankAccount.getAccountId(), amount);
        String bankFlowNo = insertFlow(bankAccount.getAccountId(), DIRECTION_IN, amount, bankSnap.getBalanceAfter(),
                businessDate, remark, operatorId, operatorName);

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
        // TD-39 / P4-4：复核同样属于封账后的写路径
        financePeriodGuard.assertOpen(businessDateOfSlip(slipId));
        DepositSlip slip = requireSlip(slipId);
        requireStatus(slip, "复核", ST_DEPOSITED);
        Long verifier = requireOperatorId("复核");
        if (slip.getCollectorUserId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缴款单缺少收款人，无法做四眼校验：slipNo=" + slip.getSlipNo());
        }
        if (slip.getCollectorUserId().equals(verifier)) {
            // BL-3：四眼（V8）硬门禁属业务违规 → HTTP 400 + 原文
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "四眼校验失败：复核人不能与收款人相同（userId=" + verifier + "）", 400);
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

        // (a) 单据状态：条件更新（并发仲裁点）——第二个并发请求会在此阻塞，等前一个提交后
        //     匹配 0 行（status 已变 REVERSED），从而拿到 409，而不是「先 SELECT 后无条件 UPDATE」。
        LocalDateTime now = LocalDateTime.now();
        int guarded = depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .in(DepositSlip::getStatus, ST_DEPOSITED, ST_VERIFIED, ST_CLOSED)
                .set(DepositSlip::getStatus, ST_REVERSED)
                .set(DepositSlip::getRemark, mergeRemark(slip.getRemark(), "[reverse] " + dto.getReason().trim()))
                .set(DepositSlip::getUpdateTime, now));
        if (guarded != 1) {
            throw new ConflictException(
                    "缴款单已被并发冲正或状态已变更，本次冲正未执行：slipNo=" + slip.getSlipNo()
                            + "（要求当前状态 ∈ DEPOSITED/VERIFIED/CLOSED）");
        }

        LocalDate businessDate = LocalDate.now(ZONE);
        Long operatorId = currentOperatorId();
        String operatorName = currentOperatorName();
        List<String> reversedFlowNos = new ArrayList<>();
        List<String> originalFlowNos = new ArrayList<>();
        try {
            for (FundFlow original : originals) {
                // (b) 原流水：条件更新 status 2→3，影响行数 ≠ 1 说明已被并发改过 → 抛错整体回滚
                int flowGuarded = fundFlowMapper.update(null, new LambdaUpdateWrapper<FundFlow>()
                        .eq(FundFlow::getFlowId, original.getFlowId())
                        .eq(FundFlow::getStatus, FLOW_STATUS_DONE)
                        .set(FundFlow::getStatus, FLOW_STATUS_CANCELLED)
                        .set(FundFlow::getUpdateTime, LocalDateTime.now()));
                if (flowGuarded != 1) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "原流水状态已被并发修改，冲正终止：flowNo=" + original.getFlowNo());
                }
                long amount = original.getAmount() == null ? 0L : original.getAmount();
                int reverseDirection = original.getFlowDirection() != null && original.getFlowDirection() == DIRECTION_IN
                        ? DIRECTION_OUT : DIRECTION_IN;
                long delta = reverseDirection == DIRECTION_IN ? amount : -amount;
                // 专项审计 §1：冲正的反向流水可能从某账户转出（delta<0），必须做余额下限校验，
                // 失败抛 409 并整体回滚（原流水不会被置 status=3、不新增反向流水）。
                AtomicBalanceUpdater.BalanceSnapshot snap = atomicBalanceUpdater
                        .addBalanceGuarded(original.getAccountId(), delta, "冲正", original.getFlowNo());
                String remark = "缴款单冲正：" + slip.getSlipNo() + " 原流水 " + original.getFlowNo()
                        + " [reconReverseOf=" + original.getFlowNo() + "]";
                reversedFlowNos.add(insertFlow(original.getAccountId(), reverseDirection, amount, snap.getBalanceAfter(),
                        businessDate, remark, operatorId, operatorName));
                originalFlowNos.add(original.getFlowNo());
            }
        } catch (DuplicateKeyException e) {
            // (c) 兜底：uk_fund_flows_recon_reverse 唯一索引拦下重复冲正 → 业务 400，不是 500
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "冲正重复被唯一索引拦截（[reconReverseOf=] 已存在）：slipNo=" + slip.getSlipNo());
        }
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
            // BL-3：状态机冲突属业务冲突 → HTTP 409 + 原文
            throw new ConflictException(
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

    /**
     * 班次的门店集合（P3 前置口径，供 SL1 做一致性校验）。
     *
     * <p>集合 = 「班次时间窗内、且存在 order_payment_records（deleted=0）的订单」的 orders.store_id 去重。</p>
     * <ul>
     *   <li>集合为空（班次内没有带支付记录的订单）→ 400，绝不静默生成 0 额单据；</li>
     *   <li>集合多值 → 由调用方决定：显式 storeId 时校验必须命中，缺省时 400 并列候选（不默认取第一个）。</li>
     * </ul>
     */
    private Set<Long> resolveShiftStores(PosShift shift) {
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "班次缺少起止时间，无法确定门店范围：shiftId=" + shift.getShiftId());
        }
        List<OrderNew> orders = orderNewMapper.selectList(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getDeleted, 0)
                .ge(OrderNew::getCreateTime, shift.getStartTime())
                .le(OrderNew::getCreateTime, shift.getEndTime())
                .select(OrderNew::getOrderId, OrderNew::getStoreId));
        if (orders == null || orders.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "班次时间窗内没有订单，无法确定门店：shiftId=" + shift.getShiftId());
        }
        List<String> orderIds = new ArrayList<>();
        for (OrderNew order : orders) {
            if (order.getOrderId() != null) {
                orderIds.add(order.getOrderId());
            }
        }
        Set<String> paidOrderIds = new java.util.HashSet<>();
        if (!orderIds.isEmpty()) {
            List<OrderPaymentRecordNew> payments = orderPaymentRecordMapper.selectList(
                    new LambdaQueryWrapper<OrderPaymentRecordNew>()
                            .in(OrderPaymentRecordNew::getOrderId, orderIds)
                            .eq(OrderPaymentRecordNew::getDeleted, 0)
                            .select(OrderPaymentRecordNew::getOrderId));
            if (payments != null) {
                for (OrderPaymentRecordNew payment : payments) {
                    if (payment.getOrderId() != null) {
                        paidOrderIds.add(payment.getOrderId());
                    }
                }
            }
        }
        Set<Long> stores = new LinkedHashSet<>();
        for (OrderNew order : orders) {
            if (order.getStoreId() != null && paidOrderIds.contains(order.getOrderId())) {
                stores.add(order.getStoreId());
            }
        }
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "班次时间窗内没有带支付记录的订单，无法生成缴款单（拒绝生成 0 额单据）：shiftId=" + shift.getShiftId());
        }
        return stores;
    }

    /** TD-03：应交额 = 该班次该门店 order_payment_records 中现金支付（payment_method=1）的合计（分） */
    /** N19-2：退款构成计算结果 */
    private static final class RefundComponent {
        private final long fen;
        private final int fallbackByDate;

        private RefundComponent(long fen, int fallbackByDate) {
            this.fen = fen;
            this.fallbackByDate = fallbackByDate;
        }
    }

    /**
     * 计算班次窗口内的「退款构成」金额（分）。
     *
     * <p>口径（N19-2 第二步①，暂不含人工归属表）：</p>
     * <ul>
     *   <li>账户：该门店的现金户（account_type=2 / status=1 / deleted=0）；无现金户 → 0（不抛，避免影响出单）</li>
     *   <li>流水：flow_category=2（退款）且 flow_direction=2（出账）且 status=2 且 deleted=0</li>
     *   <li>时间：refund_time 有值 → 必须落在 [shift.start_time, shift.end_time]；
     *       refund_time 为 NULL（历史 36 条）→ 退化为 business_date 的**日期**落在班次日期区间，计入 fallbackByDate</li>
     * </ul>
     */
    private RefundComponent sumShiftRefundComponent(PosShift shift, long storeId) {
        List<BankAccount> cashAccounts = bankAccountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getStoreId, storeId)
                .eq(BankAccount::getAccountType, 2)
                .eq(BankAccount::getStatus, 1)
                .eq(BankAccount::getDeleted, 0));
        if (cashAccounts == null || cashAccounts.isEmpty() || shift.getStartTime() == null || shift.getEndTime() == null) {
            log.info("[METRIC] refund_component_fallback_by_date count=0 (no cash account or shift window missing)");
            return new RefundComponent(0L, 0);
        }
        Long cashAccountId = cashAccounts.get(0).getAccountId();
        List<FundFlow> flows = fundFlowMapper.selectList(new LambdaQueryWrapper<FundFlow>()
                .eq(FundFlow::getAccountId, cashAccountId)
                .eq(FundFlow::getFlowCategory, 2)
                .eq(FundFlow::getFlowDirection, 2)
                .eq(FundFlow::getStatus, 2)
                .eq(FundFlow::getDeleted, 0)
                .ge(FundFlow::getBusinessDate, shift.getStartTime().toLocalDate().minusDays(1))
                .le(FundFlow::getBusinessDate, shift.getEndTime().toLocalDate().plusDays(1)));
        long fen = 0L;
        int fallbackByDate = 0;
        if (flows != null) {
            for (FundFlow flow : flows) {
                long amount = flow.getAmount() == null ? 0L : flow.getAmount();
                if (flow.getRefundTime() != null) {
                    LocalDateTime rt = LocalDateTime.ofInstant(flow.getRefundTime().toInstant(), java.time.ZoneId.systemDefault());
                    if (!rt.isBefore(shift.getStartTime()) && !rt.isAfter(shift.getEndTime())) {
                        fen += amount;
                    }
                } else if (flow.getBusinessDate() != null) {
                    // 历史行无 refund_time：按「天」判断是否落在班次日期区间
                    LocalDate d = flow.getBusinessDate();
                    LocalDate sd = shift.getStartTime().toLocalDate();
                    LocalDate ed = shift.getEndTime().toLocalDate();
                    if (!d.isBefore(sd) && !d.isAfter(ed)) {
                        fen += amount;
                        fallbackByDate++;
                    }
                }
            }
        }
        log.info("[METRIC] refund_component_computed shiftId={} cashAccountId={} refundComponentFen={} flows={}",
                shift.getShiftId(), cashAccountId, fen, flows == null ? 0 : flows.size());
        log.info("[METRIC] refund_component_fallback_by_date count={}", fallbackByDate);
        return new RefundComponent(fen, fallbackByDate);
    }

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

    /**
     * 差额方向标签（AC-21）：difference = actual − expected。
     * 长款（>0）= 实收多于应交，挂「待查溢余」；短款（<0）= 实收少于应交，挂「待查损溢」；0=平账。
     */
    private String differenceLabel(long difference) {
        if (difference > 0) {
            return "长款·待查溢余";
        }
        if (difference < 0) {
            return "短款·待查损溢";
        }
        return "平账";
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
    /** 取缴款单业务日期（门禁用；单不存在时返回 null → 守卫放行，由后续逻辑报「不存在」） */
    private java.time.LocalDate businessDateOfSlip(Long slipId) {
        try {
            com.foodtraceability.entity.finance.recon.DepositSlip slip = depositSlipMapper.selectById(slipId);
            return slip == null ? null : slip.getBusinessDate();
        } catch (Exception e) {
            return null;
        }
    }

    private String nextSlipNo() {
        String datePart = LocalDate.now(ZONE).format(DAY);
        String key = "SLIP" + datePart;
        String prefix = "DS" + datePart;
        // 共用 order_sequence 号段：GREATEST(当前号段, 当天 MAX) + 1 RETURNING（单条语句，非「查 MAX+1」）
        Integer seq = sequenceAllocator.next(key, prefix, "deposit_slips", "slip_no");
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
        Integer seq = sequenceAllocator.next(key, prefix, "fund_flows", "flow_no");
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
        vo.setBusinessDate(slip.getBusinessDate());
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
