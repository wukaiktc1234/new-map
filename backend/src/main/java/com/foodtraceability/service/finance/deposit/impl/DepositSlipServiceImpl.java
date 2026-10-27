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
    /** N19-2④：退款单 → 班次归属 */
    private final com.foodtraceability.mapper.finance.recon.RefundShiftAttributionMapper refundShiftAttributionMapper;

    /** N19-2④：班次边界容差（分钟），距 start/end ≤ N 的退款不自动计入任何班次，默认 10 */
    @org.springframework.beans.factory.annotation.Value("${finance.cash.shift-boundary-minutes:10}")
    private int shiftBoundaryMinutes;
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
                                  com.foodtraceability.mapper.finance.recon.RefundShiftAttributionMapper refundShiftAttributionMapper,
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
        this.refundShiftAttributionMapper = refundShiftAttributionMapper;
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
        //   → **N19-3a③ 起不再日级兜底**：不归属任何班次，逐笔审计 RECON_REFUND_TIME_MISSING 待回填。
        // ==================================================================
        RefundComponent component = sumShiftRefundComponent(shift, storeId, true, LocalDateTime.now());   // generate：as-of=now
        slip.setRefundComponentFen(component.fen);
        // 守卫（N19-2②）：退款构成超过应交额时**照常写入**（允许为负），但必须打点
        if (component.fen > expected) {
            log.warn("[METRIC] refund_component_exceeds_expected slipNo={} expected={} refundComponent={}",
                    slip.getSlipNo(), expected, component.fen);
        }
        slip.setActualAmount(0L);
        // AC-21 口径：difference_amount = actual_amount − expected_amount；
        // DRAFT 阶段 actual=0，故为 −expected（未收款即全额未交，符号与后续收款一致）
        slip.setDifferenceAmount(0L - expected);
        // 差异1（备用金）：reserve_fen = opening_cash 元 → 分（MoneyYuanToFen 唯一换算，禁止裸 ×100）
        //   cash_counted_fen = actual − reserve（generate 阶段 actual=0 → −reserve）
        long reserveFen = 0L;
        if (shift.getOpeningCash() != null) {
            Long converted = com.foodtraceability.service.finance.recon.importing.MoneyYuanToFen
                    .toFen(shift.getOpeningCash());
            reserveFen = converted == null ? 0L : converted;
        }
        slip.setReserveFen(reserveFen);
        slip.setCashCountedFen(0L - reserveFen);
        // N19-2②：difference_net = actual − (expected − refund_component)
        //   generate 阶段 actual=0 → difference_net = refund_component − expected
        // 差异1：generate 阶段的 difference_net 也必须按「计现额」口径（cashCouned − (expected − rcf)）
        slip.setDifferenceNetFen((0L - reserveFen) - (expected - component.fen));
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
        // 差异1（备用金）：允许 body 传 reserveFen 覆盖快照；缺省用单据上的 reserve_fen
        long reserveFen = slip.getReserveFen() == null ? 0L : slip.getReserveFen();
        if (dto.getReserveFen() != null) {
            if (dto.getReserveFen() < 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "reserveFen 不能为负：" + dto.getReserveFen());
            }
            reserveFen = dto.getReserveFen();
        }
        long cashCounted = received - reserveFen;
        long refundComponent = slip.getRefundComponentFen() == null ? 0L : slip.getRefundComponentFen();
        // difference_amount 口径不变（actual − expected）；difference_net 口径改为「计现额 − (应交 − 退款构成)」
        long differenceNet = cashCounted - (expected - refundComponent);

        LocalDateTime now = LocalDateTime.now();
        depositSlipMapper.update(null, new LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, slipId)
                .set(DepositSlip::getStatus, ST_RECEIVED)
                .set(DepositSlip::getActualAmount, received)
                .set(DepositSlip::getDifferenceAmount, difference)   // actual − expected（口径不变）
                .set(DepositSlip::getReserveFen, reserveFen)
                .set(DepositSlip::getCashCountedFen, cashCounted)
                // 差异1：difference_net = 计现额 − (expected − refund_component)
                .set(DepositSlip::getDifferenceNetFen, differenceNet)
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
        if (dto == null) {
            dto = new DepositToBankDTO();
        }
        // 差异3：bankTransactionId **可选**
        //   有值 → 老链路（校验存在性/账户门店/金额一致，E3-8/E3-9 全走）
        //   为空 → 人工汇总模式：仍可存行（现金户→本店银行户），但 deposit_bank_txn_id 留 NULL，审计 matched=false
        BankTransaction txn = null;
        BankAccount bankAccount;
        boolean txnMatched = false;
        if (dto.getBankTransactionId() != null) {
            txn = bankTransactionMapper.selectById(dto.getBankTransactionId());
            if (txn == null || Integer.valueOf(1).equals(txn.getDeleted())) {
                throw new BusinessException(ErrorCode.NOT_FOUND,
                        "存行流水不存在：bankTransactionId=" + dto.getBankTransactionId());
            }
            bankAccount = bankAccountMapper.selectById(txn.getAccountId());
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
            txnMatched = true;
        } else {
            // 人工汇总：优先用单据上已有的银行户，其次本店启用的银行账户（account_type=1）
            bankAccount = slip.getBankAccountId() == null ? null : bankAccountMapper.selectById(slip.getBankAccountId());
            if (bankAccount == null || Integer.valueOf(1).equals(bankAccount.getDeleted())) {
                List<BankAccount> banks = bankAccountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getStoreId, slip.getStoreId())
                        .eq(BankAccount::getAccountType, 1)
                        .eq(BankAccount::getStatus, 1)
                        .eq(BankAccount::getDeleted, 0)
                        .orderByAsc(BankAccount::getAccountId));
                bankAccount = (banks == null || banks.isEmpty()) ? null : banks.get(0);
            }
            if (bankAccount == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "未指定 bankTransactionId 且门店没有启用的银行账户，无法存行：storeId=" + slip.getStoreId());
            }
            log.info("[METRIC] deposit_slip_manual_mode slipNo={} bankAccountId={}", slip.getSlipNo(), bankAccount.getAccountId());
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
                .set(DepositSlip::getDepositBankTxnId, txnMatched ? txn.getTransactionId() : null)
                .set(DepositSlip::getDepositRefNo, dto.getDepositRefNo() != null ? dto.getDepositRefNo()
                        : (txnMatched ? txn.getExternalTxnId() : null))
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
                        + ",\"bankTransactionId\":" + (txnMatched ? txn.getTransactionId() : "null")
                        + ",\"matched\":" + txnMatched
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
        /** N19-3a③：refund_time 为 NULL 的现金退款笔数（不归属任何班次，待回填） */
        private final int missingRefundTimeCount;

        private RefundComponent(long fen, int missingRefundTimeCount) {
            this.fen = fen;
            this.missingRefundTimeCount = missingRefundTimeCount;
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
     *       refund_time 为 NULL → **不归属任何班次**（不计入），返回 missingRefundTimeCount 供详情展示</li>
     * </ul>
     */
    private RefundComponent sumShiftRefundComponent(PosShift shift, long storeId, boolean auditPending,
                                                   java.time.LocalDateTime asOf) {
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

        // N19-2④：人工归属 **优先于** 窗口规则 —— 一笔退款被归属到某班次后，
        // 只在该班次计入；其它班次即使窗口覆盖也不再计入（否则迁出侧会残留，正是 A39 抓的缺陷）。
        java.util.Map<Long, Long> attributionByRefund = new java.util.HashMap<>();
        List<com.foodtraceability.entity.finance.recon.RefundShiftAttribution> attributions =
                refundShiftAttributionMapper.selectList(new LambdaQueryWrapper<com.foodtraceability.entity.finance.recon.RefundShiftAttribution>());
        if (attributions != null) {
            for (com.foodtraceability.entity.finance.recon.RefundShiftAttribution a : attributions) {
                if (a.getRefundRecordId() != null) {
                    attributionByRefund.put(a.getRefundRecordId(), a.getShiftId());
                }
            }
        }

        java.time.LocalDateTime boundaryFrom = shift.getStartTime().plusMinutes(shiftBoundaryMinutes);
        java.time.LocalDateTime boundaryTo = shift.getEndTime().minusMinutes(shiftBoundaryMinutes);
        long fen = 0L;
        int missingRefundTime = 0;
        java.util.List<FundFlow> missingRefundTimeFlows = new java.util.ArrayList<>();
        java.util.List<Long> boundaryPending = new java.util.ArrayList<>();
        if (flows != null) {
            for (FundFlow flow : flows) {
                long amount = flow.getAmount() == null ? 0L : flow.getAmount();
                Long refundRecordId = flow.getRefundRecordId();
                Long attributedShift = refundRecordId == null ? null : attributionByRefund.get(refundRecordId);
                // (b) 人工归属到本班次 → 计入（与 A39 的 OR 语义一致）
                if (attributedShift != null && attributedShift.equals(shift.getShiftId())) {
                    fen += amount;
                    continue;
                }
                // (a) 否则按窗口规则：窗口内且不在边界带才计入
                if (flow.getRefundTime() != null) {
                    LocalDateTime rt = LocalDateTime.ofInstant(flow.getRefundTime().toInstant(), java.time.ZoneId.systemDefault());
                    if (rt.isBefore(shift.getStartTime()) || rt.isAfter(shift.getEndTime())) {
                        continue;   // 不在窗口
                    }
                    // ② 落在班次边界容差内 → 不计入任何班次，进待确认清单
                    if (rt.isBefore(boundaryFrom) || rt.isAfter(boundaryTo)) {
                        boundaryPending.add(refundRecordId);
                        continue;
                    }
                    fen += amount;
                } else {
                    // N19-3a③：refund_time 为 NULL → **不归属任何班次**（不计入 refund_component），
                    // 逐笔落审计 RECON_REFUND_TIME_MISSING 待回填；不再按 business_date 日级兜底。
                    missingRefundTime++;
                    missingRefundTimeFlows.add(flow);
                }
            }
        }
        log.info("[METRIC] refund_component_computed shiftId={} cashAccountId={} refundComponentFen={} flows={} attributed={} boundaryPending={}",
                shift.getShiftId(), cashAccountId, fen, flows == null ? 0 : flows.size(), attributionByRefund.size(), boundaryPending.size());
        log.info("[METRIC] refund_component_missing_refund_time count={}", missingRefundTime);

        // N19-2④：边界退回落审计（待人工确认归属），失败不影响出单
        for (Long pendingRefundId : auditPending ? boundaryPending : java.util.Collections.<Long>emptyList()) {
            try {
                auditShiftBoundary(shift.getShiftId(),
                        "退款落在班次边界容差内，未计入任何班次：shiftId=" + shift.getShiftId()
                                + " refundRecordId=" + pendingRefundId + " 容差=" + shiftBoundaryMinutes + " 分钟",
                        "{\"shiftId\":" + shift.getShiftId() + ",\"refundRecordId\":" + (pendingRefundId == null ? "null" : pendingRefundId)
                                + ",\"boundaryMinutes\":" + shiftBoundaryMinutes + ",\"state\":\"PENDING\"}");
            } catch (Exception e) {
                log.warn("[METRIC] audit_write_failed operationType=RECON_SHIFT_BOUNDARY_PENDING shiftId={} err={}",
                        shift.getShiftId(), e.getMessage());
            }
        }
        for (FundFlow missingFlow : auditPending ? missingRefundTimeFlows : java.util.Collections.<FundFlow>emptyList()) {
            try {
                auditRecon("RECON_REFUND_TIME_MISSING", shift.getShiftId(),
                        "退款缺少发生时刻（refund_time IS NULL），不归属任何班次：flowNo="
                                + missingFlow.getFlowNo() + " accountId=" + missingFlow.getAccountId()
                                + " businessDate=" + missingFlow.getBusinessDate() + " 金额=" + missingFlow.getAmount() + " 分",
                        "{\"flowNo\":\"" + missingFlow.getFlowNo() + "\",\"accountId\":" + missingFlow.getAccountId()
                                + ",\"businessDate\":\"" + missingFlow.getBusinessDate() + "\",\"amount\":" + missingFlow.getAmount() + "}");
            } catch (Exception e) {
                log.warn("[METRIC] audit_write_failed operationType=RECON_REFUND_TIME_MISSING flowNo={} err={}",
                        missingFlow.getFlowNo(), e.getMessage());
            }
        }
        return new RefundComponent(fen, missingRefundTime);
    }

    /**
     * N19-2②：净差异 = 实收 − (应交 − 退款构成)。
     *
     * <p>退款构成（refund_component_fen）是「本次存行里本就不该交的退款部分」，
     * 因此先从应交额里扣掉，再与实收比较。允许为负（多退少缴），不做截断。</p>
     */
    private static long differenceNetOf(long actualFen, long expectedFen, long refundComponentFen) {
        return actualFen - (expectedFen - refundComponentFen);
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

    /** N19-2④：班次边界待确认审计（失败只 WARN，不影响出单） */
    /** N19-2④ / N19-3a③：对账类审计（失败只 WARN，不影响出单） */
    private void auditRecon(String operationType, Long targetId, String desc, String dataJson) {
        Long operatorId = currentOperatorId();
        boolean ok = financeAuditLogService.log(operatorId, currentOperatorName(), operationType,
                "FINANCE_DEPOSIT_SLIP", targetId, "POS_SHIFT", desc, dataJson, null);
        if (!ok) {
            log.warn("[METRIC] audit_write_failed operationType={} targetId={}", operationType, targetId);
        }
    }

    private void auditShiftBoundary(Long shiftId, String desc, String dataJson) {
        Long operatorId = currentOperatorId();
        boolean ok = financeAuditLogService.log(operatorId, currentOperatorName(), "RECON_SHIFT_BOUNDARY_PENDING",
                "FINANCE_DEPOSIT_SLIP", shiftId, "POS_SHIFT", desc, dataJson, null);
        if (!ok) {
            log.warn("[METRIC] audit_write_failed operationType=RECON_SHIFT_BOUNDARY_PENDING shiftId={}", shiftId);
        }
    }

    /**
     * N19-2④：人工确认退款 → 班次归属。
     *
     * <p>幂等/覆盖：refund_shift_attribution.refund_record_id 唯一。首次确认 → 插入 + 审计 CONFIRMED；
     * 同班次重复确认 → 幂等返回；改到别的班次 → 乐观锁 CAS 覆盖 + 审计 CHANGED（old/new）；
     * CAS 失败（并发）→ 409「该退款已被确认，请刷新」。</p>
     *
     * <p>确认后立即重算该缴款单的 refund_component_fen 与 difference_net_fen（复用同一套 helper）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositSlipVO confirmAttribution(Long slipId, Long refundRecordId, Long shiftId, String reason) {
        if (refundRecordId == null || shiftId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "refundRecordId 与 shiftId 均不能为空");
        }
        DepositSlip slip = requireSlip(slipId);
        PosShift shift = posShiftMapper.selectById(shiftId);
        if (shift == null || Integer.valueOf(1).equals(shift.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "班次不存在：shiftId=" + shiftId);
        }
        Long operatorId = currentOperatorId();

        com.foodtraceability.entity.finance.recon.RefundShiftAttribution existing =
                refundShiftAttributionMapper.selectOne(new LambdaQueryWrapper<com.foodtraceability.entity.finance.recon.RefundShiftAttribution>()
                        .eq(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getRefundRecordId, refundRecordId)
                        .last("LIMIT 1"));
        Long oldShiftId = existing == null ? null : existing.getShiftId();
        Long oldSlipId = existing == null ? null : existing.getSlipId();
        boolean changed = false;
        if (existing == null) {
            com.foodtraceability.entity.finance.recon.RefundShiftAttribution row =
                    new com.foodtraceability.entity.finance.recon.RefundShiftAttribution();
            row.setRefundRecordId(refundRecordId);
            row.setShiftId(shiftId);
            row.setSlipId(slipId);
            row.setOperatorId(operatorId);
            row.setReason(reason);
            row.setConfirmedAt(java.time.OffsetDateTime.now());
            row.setUpdateTime(java.time.OffsetDateTime.now());
            row.setVersion(0);
            refundShiftAttributionMapper.insert(row);
            audit(slip, "REFUND_SHIFT_ATTRIBUTION_CONFIRMED",
                    "退款归属确认：refundRecordId=" + refundRecordId + " -> shiftId=" + shiftId + " 原因=" + reason,
                    "{\"refundRecordId\":" + refundRecordId + ",\"shiftId\":" + shiftId + ",\"slipId\":" + slipId + "}");
        } else if (shiftId.equals(existing.getShiftId())) {
            log.info("[METRIC] refund_attribution_idempotent refundRecordId={} shiftId={}", refundRecordId, shiftId);
        } else {
            int oldVersion = existing.getVersion() == null ? 0 : existing.getVersion();
            int updated = refundShiftAttributionMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.foodtraceability.entity.finance.recon.RefundShiftAttribution>()
                            .eq(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getId, existing.getId())
                            .eq(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getVersion, oldVersion)
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getShiftId, shiftId)
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getSlipId, slipId)
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getOperatorId, operatorId)
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getReason, reason)
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getConfirmedAt, java.time.OffsetDateTime.now())
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getUpdateTime, java.time.OffsetDateTime.now())
                            .set(com.foodtraceability.entity.finance.recon.RefundShiftAttribution::getVersion, oldVersion + 1));
            if (updated == 0) {
                throw new ConflictException("该退款已被确认，请刷新");
            }
            changed = true;
            audit(slip, "REFUND_SHIFT_ATTRIBUTION_CHANGED",
                    "退款归属变更：refundRecordId=" + refundRecordId + " oldShiftId=" + oldShiftId
                            + " -> newShiftId=" + shiftId + " 原因=" + reason,
                    "{\"refundRecordId\":" + refundRecordId + ",\"oldShiftId\":" + oldShiftId
                            + ",\"newShiftId\":" + shiftId + ",\"slipId\":" + slipId + "}");
        }

        // 金额一律「按公式重算」，不做增量：CONFIRMED 重算目标班次；CHANGED 旧/新两侧都重算
        recomputeSlipByShift(shiftId);
        if (changed && oldShiftId != null && !oldShiftId.equals(shiftId)) {
            recomputeSlipByShift(oldShiftId);
        }
        if (oldSlipId != null && !oldSlipId.equals(slipId)) {
            recomputeSlipRefundComponent(oldSlipId);
        }
        log.info("[METRIC] refund_attribution_done refundRecordId={} newShiftId={} oldShiftId={} changed={}",
                refundRecordId, shiftId, oldShiftId, changed);
        return detail(slipId);
    }

    /** 按班次找缴款单并重算（一班一单，TD-33） */
    private void recomputeSlipByShift(Long shiftId) {
        if (shiftId == null) {
            return;
        }
        DepositSlip target = depositSlipMapper.selectOne(new LambdaQueryWrapper<DepositSlip>()
                .eq(DepositSlip::getShiftId, shiftId)
                .eq(DepositSlip::getDeleted, 0)
                .last("LIMIT 1"));
        if (target == null) {
            log.info("[METRIC] refund_component_recompute_skipped reason=no_slip_for_shift shiftId={}", shiftId);
            return;
        }
        recomputeSlipRefundComponent(target.getSlipId());
    }

    /**
     * 重算某张缴款单的退款构成与净差异（唯一公式，与巡检 A39 同口径）：
     * refund_component = Σ 同门店现金户的现金退款流水，且
     *     (a) refund_time ∈ [shift.start + boundary, shift.end - boundary]
     *     或 (b) 该 refund_record_id 的归属记录 shift_id = 本缴款单的 shift_id
     *     （refund_time 为空的历史行 → 退化为 business_date 落在班次日期区间）
     * difference_net = actual - (expected - refund_component)
     */
    private void recomputeSlipRefundComponent(Long targetSlipId) {
        DepositSlip target = depositSlipMapper.selectById(targetSlipId);
        if (target == null || target.getShiftId() == null) {
            return;
        }
        PosShift shift = posShiftMapper.selectById(target.getShiftId());
        if (shift == null) {
            log.info("[METRIC] refund_component_recompute_skipped reason=no_shift slipId={} shiftId={}",
                    targetSlipId, target.getShiftId());
            return;
        }
        RefundComponent component = sumShiftRefundComponent(shift, target.getStoreId(), false, target.getCreateTime());   // 重算：as-of=create_time（A39 同口径）
        long expected = target.getExpectedAmount() == null ? 0L : target.getExpectedAmount();
        long actual = target.getActualAmount() == null ? 0L : target.getActualAmount();
        long differenceNet = differenceNetOf(actual, expected, component.fen);
        depositSlipMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DepositSlip>()
                .eq(DepositSlip::getSlipId, targetSlipId)
                .set(DepositSlip::getRefundComponentFen, component.fen)
                .set(DepositSlip::getDifferenceNetFen, differenceNet)
                .set(DepositSlip::getUpdateTime, LocalDateTime.now()));
        log.info("[METRIC] refund_component_recomputed slipId={} shiftId={} refundComponent={} expected={} actual={} differenceNet={}",
                targetSlipId, target.getShiftId(), component.fen, expected, actual, differenceNet);
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

    /**
    /** 差异3：某营业日的存行汇总（人工汇总模式）：当天 DEPOSITED 缴款单 + 绑/未绑流水拆分 + 银行进账。 */
    @Override
    @Transactional(readOnly = true)
    public com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO dailySummary(java.time.LocalDate date) {
        com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO vo =
                new com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO();
        java.time.LocalDate day = date == null ? java.time.LocalDate.now() : date;
        List<DepositSlip> slips = depositSlipMapper.selectList(new LambdaQueryWrapper<DepositSlip>()
                .eq(DepositSlip::getDeleted, 0)
                .eq(DepositSlip::getStatus, ST_DEPOSITED)
                .eq(DepositSlip::getBusinessDate, day)
                .orderByAsc(DepositSlip::getSlipId));
        java.util.Set<Long> bankAccountIds = new java.util.LinkedHashSet<>();
        long totalCashCounted = 0L;
        long matched = 0L;
        long unmatched = 0L;
        int unmatchedCount = 0;
        if (slips != null) {
            for (DepositSlip s : slips) {
                com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO.Row row =
                        new com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO.Row();
                row.slipId = s.getSlipId();
                row.slipNo = s.getSlipNo();
                row.storeId = s.getStoreId();
                row.actualAmount = s.getActualAmount() == null ? 0L : s.getActualAmount();
                row.reserveFen = s.getReserveFen() == null ? 0L : s.getReserveFen();
                row.cashCountedFen = s.getCashCountedFen() == null
                        ? (row.actualAmount - row.reserveFen) : s.getCashCountedFen();
                row.depositBankTxnId = s.getDepositBankTxnId();
                vo.depositedSlips.add(row);

                totalCashCounted += row.cashCountedFen;
                if (row.depositBankTxnId == null) {
                    unmatched += row.cashCountedFen;
                    unmatchedCount++;          // 差异3：未绑流水必须显式计数，不得静默
                } else {
                    matched += row.cashCountedFen;
                }
                if (s.getBankAccountId() != null) {
                    bankAccountIds.add(s.getBankAccountId());
                }
            }
        }
        vo.totalCashCountedFen = totalCashCounted;
        vo.matchedFen = matched;
        vo.unmatchedFen = unmatched;
        vo.unmatchedCount = unmatchedCount;

        // 银行进账：当日这些银行户的 bank_transactions 方向=1 合计
        long bankInflow = 0L;
        if (!bankAccountIds.isEmpty()) {
            List<com.foodtraceability.entity.finance.recon.BankTransaction> bankTxns =
                    bankTransactionMapper.selectList(new LambdaQueryWrapper<com.foodtraceability.entity.finance.recon.BankTransaction>()
                            .in(com.foodtraceability.entity.finance.recon.BankTransaction::getAccountId, bankAccountIds)
                            .eq(com.foodtraceability.entity.finance.recon.BankTransaction::getDirection, 1)
                            .eq(com.foodtraceability.entity.finance.recon.BankTransaction::getDeleted, 0)
                            .eq(com.foodtraceability.entity.finance.recon.BankTransaction::getBusinessDate, day));
            if (bankTxns != null) {
                for (com.foodtraceability.entity.finance.recon.BankTransaction t : bankTxns) {
                    bankInflow += t.getAmount() == null ? 0L : t.getAmount();
                }
            }
        }
        vo.bankInflowFen = bankInflow;
        log.info("[METRIC] deposit_daily_summary day={} slips={} totalCashCounted={} matched={} unmatched={} unmatchedCount={} bankInflow={}",
                day, vo.depositedSlips.size(), totalCashCounted, matched, unmatched, unmatchedCount, bankInflow);
        return vo;
    }


    /**
     * N19-3b：本单「待确认归属」数据源。
     *
     * <p>pendingRefunds = 本单班次边界带内（距 start/end ≤ boundary 分钟）且尚无 attribution 的现金退款；
     * candidateShifts = 同一门店的上一 / 本 / 下一班次（缺则少给）；
     * missingRefundTimeCount = 本单窗口内 refund_time 为 NULL 的退款笔数。</p>
     * <p>空数据返回空数组（code=0），不抛 404。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO pendingRefunds(Long slipId) {
        com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO vo =
                new com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO();
        DepositSlip slip = depositSlipMapper.selectById(slipId);
        if (slip == null || slip.getShiftId() == null) {
            return vo;
        }
        PosShift shift = posShiftMapper.selectById(slip.getShiftId());
        if (shift == null || shift.getStartTime() == null || shift.getEndTime() == null) {
            return vo;
        }
        Long storeId = slip.getStoreId();
        List<BankAccount> cashAccounts = bankAccountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getStoreId, storeId)
                .eq(BankAccount::getAccountType, 2)
                .eq(BankAccount::getStatus, 1)
                .eq(BankAccount::getDeleted, 0));
        if (cashAccounts != null && !cashAccounts.isEmpty()) {
            java.util.List<Long> accountIds = new ArrayList<>();
            for (BankAccount a : cashAccounts) {
                accountIds.add(a.getAccountId());
            }
            java.time.LocalDateTime boundaryFrom = shift.getStartTime().plusMinutes(shiftBoundaryMinutes);
            java.time.LocalDateTime boundaryTo = shift.getEndTime().minusMinutes(shiftBoundaryMinutes);
            List<FundFlow> flows = fundFlowMapper.selectList(new LambdaQueryWrapper<FundFlow>()
                    .in(FundFlow::getAccountId, accountIds)
                    .eq(FundFlow::getFlowCategory, 2)
                    .eq(FundFlow::getFlowDirection, 2)
                    .eq(FundFlow::getStatus, 2)
                    .eq(FundFlow::getDeleted, 0)
                    .isNotNull(FundFlow::getRefundTime)
                    .ge(FundFlow::getBusinessDate, shift.getStartTime().toLocalDate().minusDays(1))
                    .le(FundFlow::getBusinessDate, shift.getEndTime().toLocalDate().plusDays(1)));
            java.util.Set<Long> attributed = new java.util.HashSet<>();
            List<com.foodtraceability.entity.finance.recon.RefundShiftAttribution> attrs =
                    refundShiftAttributionMapper.selectList(new LambdaQueryWrapper<com.foodtraceability.entity.finance.recon.RefundShiftAttribution>());
            if (attrs != null) {
                for (com.foodtraceability.entity.finance.recon.RefundShiftAttribution a : attrs) {
                    if (a.getRefundRecordId() != null) {
                        attributed.add(a.getRefundRecordId());
                    }
                }
            }
            if (flows != null) {
                for (FundFlow flow : flows) {
                    LocalDateTime rt = LocalDateTime.ofInstant(flow.getRefundTime().toInstant(), java.time.ZoneId.systemDefault());
                    if (rt.isBefore(shift.getStartTime()) || rt.isAfter(shift.getEndTime())) {
                        continue;
                    }
                    boolean inBoundary = rt.isBefore(boundaryFrom) || rt.isAfter(boundaryTo);
                    if (!inBoundary) {
                        continue;   // 非边界带：已自动计入，无需人工确认
                    }
                    if (flow.getRefundRecordId() != null && attributed.contains(flow.getRefundRecordId())) {
                        continue;   // 已有归属记录
                    }
                    com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO.PendingRefund pr =
                            new com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO.PendingRefund();
                    pr.refundRecordId = flow.getRefundRecordId();
                    pr.flowNo = flow.getFlowNo();
                    pr.amountFen = flow.getAmount();
                    pr.refundTime = rt;
                    pr.orderId = flow.getOrderId();
                    vo.pendingRefunds.add(pr);
                }
            }
        }

        // 候选班次：本班次前后 7 天内的班次，用「时间窗内订单所属门店」判定同门店；
        // 无订单的班次无法判定门店（resolveShiftStores 会抛 BusinessException）→ 退化为「同一收银员」，
        // 避免整个接口因为一个无订单班次 400。
        List<PosShift> sameStore = new ArrayList<>();
        List<PosShift> allShifts = posShiftMapper.selectList(new LambdaQueryWrapper<PosShift>()
                .eq(PosShift::getDeleted, 0)
                .isNotNull(PosShift::getStartTime)
                .ge(PosShift::getStartTime, shift.getStartTime().minusDays(7))
                .le(PosShift::getStartTime, shift.getStartTime().plusDays(7))
                .orderByAsc(PosShift::getStartTime));
        if (allShifts != null) {
            for (PosShift ps : allShifts) {
                boolean sameStoreShift;
                try {
                    sameStoreShift = resolveShiftStores(ps).contains(storeId);
                } catch (Exception ignore) {
                    sameStoreShift = ps.getEmployeeId() != null && ps.getEmployeeId().equals(shift.getEmployeeId());
                }
                if (sameStoreShift) {
                    sameStore.add(ps);
                }
            }
        }
        int idx = -1;
        for (int i = 0; i < sameStore.size(); i++) {
            if (sameStore.get(i).getShiftId().equals(shift.getShiftId())) {
                idx = i;
                break;
            }
        }
        java.util.List<PosShift> picked = new ArrayList<>();
        if (idx >= 0) {
            if (idx > 0) {
                picked.add(sameStore.get(idx - 1));
            }
            picked.add(sameStore.get(idx));
            if (idx + 1 < sameStore.size()) {
                picked.add(sameStore.get(idx + 1));
            }
        } else {
            picked.add(shift);
        }
        for (PosShift ps : picked) {
            com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO.CandidateShift cs =
                    new com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO.CandidateShift();
            cs.shiftId = ps.getShiftId();
            cs.startTime = ps.getStartTime();
            cs.endTime = ps.getEndTime();
            cs.label = (ps.getShiftType() == null ? "班次" : ps.getShiftType()) + " #" + ps.getShiftId();
            vo.candidateShifts.add(cs);
        }

        vo.missingRefundTimeCount = countMissingRefundTime(storeId, shift);
        return vo;
    }

    /** N19-3b：stale 计数（本单窗口内「生成之后才发生」的退款笔数，与 A39 stale 同义） */
    private Integer countStaleRefunds(DepositSlip slip, PosShift shift) {
        if (slip == null || shift == null || slip.getCreateTime() == null || shift.getStartTime() == null) {
            return 0;
        }
        List<BankAccount> cashAccounts = bankAccountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getStoreId, slip.getStoreId())
                .eq(BankAccount::getAccountType, 2)
                .eq(BankAccount::getStatus, 1)
                .eq(BankAccount::getDeleted, 0));
        if (cashAccounts == null || cashAccounts.isEmpty()) {
            return 0;
        }
        java.util.List<Long> accountIds = new ArrayList<>();
        for (BankAccount a : cashAccounts) {
            accountIds.add(a.getAccountId());
        }
        List<FundFlow> flows = fundFlowMapper.selectList(new LambdaQueryWrapper<FundFlow>()
                .in(FundFlow::getAccountId, accountIds)
                .eq(FundFlow::getFlowCategory, 2)
                .eq(FundFlow::getFlowDirection, 2)
                .eq(FundFlow::getStatus, 2)
                .eq(FundFlow::getDeleted, 0)
                .isNotNull(FundFlow::getRefundTime));
        int stale = 0;
        if (flows != null) {
            for (FundFlow f : flows) {
                LocalDateTime rt = LocalDateTime.ofInstant(f.getRefundTime().toInstant(), java.time.ZoneId.systemDefault());
                if (rt.isAfter(slip.getCreateTime())
                        && !rt.isBefore(shift.getStartTime().plusMinutes(shiftBoundaryMinutes))
                        && !rt.isAfter(shift.getEndTime().minusMinutes(shiftBoundaryMinutes))) {
                    stale++;
                }
            }
        }
        return stale;
    }

    /** N19-3b：still_open 计数（班次窗口未结束的 N19-2 缴款单数，与 A39 still_open 同义） */
    private Integer countStillOpenSlips() {
        int open = 0;
        List<DepositSlip> slips = depositSlipMapper.selectList(new LambdaQueryWrapper<DepositSlip>()
                .eq(DepositSlip::getDeleted, 0)
                .isNotNull(DepositSlip::getDifferenceNetFen));
        if (slips != null) {
            LocalDateTime now = LocalDateTime.now();
            for (DepositSlip s2 : slips) {
                PosShift ps = s2.getShiftId() == null ? null : posShiftMapper.selectById(s2.getShiftId());
                if (ps == null || ps.getEndTime() == null || !ps.getEndTime().isBefore(now)) {
                    open++;
                }
            }
        }
        return open;
    }

    /** N19-3b：legacy_null 计数（difference_net_fen 为 NULL 的历史缴款单数） */
    private Integer countLegacyNullSlips() {
        Long n = depositSlipMapper.selectCount(new LambdaQueryWrapper<DepositSlip>()
                .eq(DepositSlip::getDeleted, 0)
                .isNull(DepositSlip::getDifferenceNetFen));
        return n == null ? 0 : n.intValue();
    }


    /** N19-3a③ / N19-3b：统计**本单班次窗口内** refund_time 为 NULL 的退款笔数（shift 为空则 0） */
    private Integer countMissingRefundTime(Long storeId, PosShift shift) {
        if (storeId == null || shift == null || shift.getStartTime() == null || shift.getEndTime() == null) {
            return 0;
        }
        List<BankAccount> cashAccounts = bankAccountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getStoreId, storeId)
                .eq(BankAccount::getAccountType, 2)
                .eq(BankAccount::getStatus, 1)
                .eq(BankAccount::getDeleted, 0));
        if (cashAccounts == null || cashAccounts.isEmpty()) {
            return 0;
        }
        java.util.List<Long> accountIds = new ArrayList<>();
        for (BankAccount a : cashAccounts) {
            accountIds.add(a.getAccountId());
        }
        Long n = fundFlowMapper.selectCount(new LambdaQueryWrapper<FundFlow>()
                .in(FundFlow::getAccountId, accountIds)
                .eq(FundFlow::getFlowCategory, 2)
                .eq(FundFlow::getFlowDirection, 2)
                .eq(FundFlow::getStatus, 2)
                .eq(FundFlow::getDeleted, 0)
                .isNull(FundFlow::getRefundTime)
                .ge(FundFlow::getBusinessDate, shift.getStartTime().toLocalDate())
                .le(FundFlow::getBusinessDate, shift.getEndTime().toLocalDate()));
        return n == null ? 0 : n.intValue();
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
        vo.setRefundComponentFen(slip.getRefundComponentFen());
        vo.setReserveFen(slip.getReserveFen());
        vo.setCashCountedFen(slip.getCashCountedFen());
        vo.setDifferenceNetFen(slip.getDifferenceNetFen());
        PosShift voShift = slip.getShiftId() == null ? null : posShiftMapper.selectById(slip.getShiftId());
        vo.setMissingRefundTimeCount(countMissingRefundTime(slip.getStoreId(), voShift));
        // N19-3b：四类计数全部由后端给（A39 同口径），前端不得自造
        vo.setStaleCount(countStaleRefunds(slip, voShift));
        vo.setStillOpenCount(countStillOpenSlips());
        vo.setLegacyNullCount(countLegacyNullSlips());
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
