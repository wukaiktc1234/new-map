package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.finance.FundFlowCreateDTO;
import com.foodtraceability.dto.finance.FundFlowVO;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * POS 支付成功后的财务副作用记录器：资金流水（收入/销售收款）+ 挂账核销。
 *
 * <p><b>为什么单独一个组件：</b>支付主流程（{@code PosOrderPaymentServiceImpl.payNewOrder}）本身是
 * {@code @Transactional}，如果直接在同一个事务里调用 {@code fundFlowService.create()} 并 catch 异常，
 * 一旦 create 抛错，异常穿过它的 @Transactional 代理会把<b>外层事务标记为 rollback-only</b>，
 * 结果是「入账失败」连带「支付回滚」——正好违反业务要求。因此这里用
 * {@link TransactionTemplate} 以 {@code PROPAGATION_REQUIRES_NEW} 开独立事务执行财务写入：
 * 财务侧失败只回滚自己，支付事务照常提交。</p>
 *
 * <p><b>幂等：</b>fund_flows 表没有 order_id 列，本类用确定性备注
 * {@code "POS销售入账：<orderNumber> [orderId=<orderId>]"} 作为幂等键，
 * 写入前先按 {@code direction=1 & category=1 & remark like '%[orderId=xxx]%'} 查重；
 * 已存在则跳过并 WARN，重复支付/重复回调不会产生第二条收入。</p>
 *
 * <p><b>账户映射：</b>现金/余额→现金类账户（account_type=2），微信/支付宝→名称含「微信」「支付宝」的账户，
 * 银行卡→银行账户（account_type=1）；找不到匹配账户时回退到第一个启用账户并 WARN，绝不因缺账户让支付失败。</p>
 */
@Service
public class PosSaleFinanceRecorder {

    private static final Logger log = LoggerFactory.getLogger(PosSaleFinanceRecorder.class);

    /** 资金流水方向：1=收入 */
    public static final int FLOW_DIRECTION_INCOME = 1;
    /** 资金流水分类：1=销售收款（SALE） */
    public static final int FLOW_CATEGORY_SALE = 1;

    /**
     * 幂等键片段：备注里带 orderId 的定界串。
     * 刻意使用 ASCII 方括号（[orderId=xxx]）而非常见的中文全角括号：
     * 运维/E2E 需要用 psql 等工具按该片段筛选流水，ASCII 定界符不会因客户端编码（GBK/UTF8）错位。
     */
    private static final String ORDER_ID_TOKEN_PREFIX = "[orderId=";
    private static final String ORDER_ID_TOKEN_SUFFIX = "]";

    private final FundFlowService fundFlowService;
    private final BankAccountMapper bankAccountMapper;
    private final FundFlowMapper fundFlowMapper;
    /** 应收账款服务：容器里没有该 bean 时降级为不核销（不阻塞支付） */
    private final ReceivableService receivableService;
    /** REQUIRES_NEW 事务模板：财务写入与支付主事务物理隔离 */
    private final TransactionTemplate requiresNewTx;

    public PosSaleFinanceRecorder(FundFlowService fundFlowService,
                                  BankAccountMapper bankAccountMapper,
                                  FundFlowMapper fundFlowMapper,
                                  @Nullable ReceivableService receivableService,
                                  PlatformTransactionManager transactionManager) {
        this.fundFlowService = fundFlowService;
        this.bankAccountMapper = bankAccountMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.receivableService = receivableService;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTx.setName("pos-sale-finance-new-tx");
    }

    /**
     * 支付成功后记录财务副作用（资金流水 + 挂账核销），任何异常都不向调用方抛出。
     *
     * @param orderId       订单ID（幂等键 + 备注追溯）
     * @param orderNumber   订单号（备注主键，E2E 断言依据）
     * @param amountFen     本次实收金额（分），与 order_payment_records.payment_amount 一致
     * @param paymentMethod 支付方式：1现金 2微信 3支付宝 4银行卡 5余额
     * @param storeId       门店ID（预留：账户若带门店归属可用于优先匹配）
     * @param customerName  顾客名称（流水对方名称）
     * @return 结果摘要（状态/流水号/账户/核销金额），供返回值与日志展示
     */
    public PosSaleFinanceOutcome recordAfterPayment(String orderId, String orderNumber, long amountFen,
                                                    Integer paymentMethod, Long storeId, String customerName) {
        if (orderId == null || orderId.isEmpty() || orderNumber == null || orderNumber.isEmpty()) {
            log.warn("[METRIC] pos_fund_flow_recorded result=skipped reason=missing_order_key orderId={} orderNumber={}",
                    orderId, orderNumber);
            return PosSaleFinanceOutcome.skipped("订单标识缺失");
        }
        if (amountFen <= 0) {
            log.warn("[METRIC] pos_fund_flow_recorded result=skipped reason=non_positive_amount orderId={} amountFen={}",
                    orderId, amountFen);
            return PosSaleFinanceOutcome.skipped("金额非正数");
        }

        // ===== 1. 资金流水（独立事务：失败只回滚自己，不影响支付） =====
        FundFlowWriteOutcome flow;
        try {
            FundFlowWriteOutcome first = requiresNewTx.execute(
                    status -> insertSaleFlow(orderId, orderNumber, amountFen, paymentMethod, customerName));
            flow = first != null ? first : FundFlowWriteOutcome.failed("事务未返回结果");
        } catch (DataIntegrityViolationException e) {
            // 流水号 FFyyyyMMddNNNN 由「查最后一条 + 1」生成，并发下有极小概率撞唯一键：重试一次
            log.warn("POS 资金流水写入冲突，重试一次: orderId={}, error={}", orderId, e.getMessage());
            try {
                FundFlowWriteOutcome retry = requiresNewTx.execute(
                        status -> insertSaleFlow(orderId, orderNumber, amountFen, paymentMethod, customerName));
                flow = retry != null ? retry : FundFlowWriteOutcome.failed("重试未返回结果");
            } catch (Exception e2) {
                flow = FundFlowWriteOutcome.failed(e2.getMessage());
                log.error("[METRIC] pos_fund_flow_recorded result=failed orderId={} amountFen={} error={}",
                        orderId, amountFen, e2.getMessage(), e2);
            }
        } catch (Exception e) {
            flow = FundFlowWriteOutcome.failed(e.getMessage());
            log.error("[METRIC] pos_fund_flow_recorded result=failed orderId={} orderNumber={} amountFen={} error={}",
                    orderId, orderNumber, amountFen, e.getMessage(), e);
        }

        // ===== 2. 挂账核销（有应收才核销；无应收属正常现结） =====
        long settledFen = 0L;
        try {
            settledFen = settleReceivableIfAny(orderNumber, amountFen);
        } catch (Exception e) {
            log.error("[METRIC] pos_receivable_settle result=failed orderNumber={} amountFen={} error={}",
                    orderNumber, amountFen, e.getMessage(), e);
        }

        return new PosSaleFinanceOutcome(flow.status, flow.flowNo, flow.accountId, settledFen, flow.note);
    }

    // ==================== 资金流水 ====================

    /** 在独立事务内执行：幂等查重 → 账户映射 → 调 FundFlowService.create（不写裸 SQL） */
    private FundFlowWriteOutcome insertSaleFlow(String orderId, String orderNumber, long amountFen,
                                                Integer paymentMethod, String customerName) {
        FundFlow existing = findExistingSaleFlow(orderId);
        if (existing != null) {
            log.warn("[METRIC] pos_fund_flow_recorded result=duplicate orderId={} orderNumber={} flowNo={} amountFen={}",
                    orderId, orderNumber, existing.getFlowNo(), existing.getAmount());
            return FundFlowWriteOutcome.duplicate(existing.getFlowNo(), existing.getAccountId());
        }

        AccountPick pick = resolveAccount(paymentMethod);
        if (pick == null) {
            log.error("[METRIC] pos_fund_flow_recorded result=no_account orderId={} orderNumber={} paymentMethod={}",
                    orderId, orderNumber, paymentMethod);
            return FundFlowWriteOutcome.noAccount();
        }
        if (!pick.matchedByKeyword) {
            log.warn("POS 入账账户按默认规则回退: orderId={}, paymentMethod={}, accountId={}, accountName={}, reason={}",
                    orderId, paymentMethod, pick.accountId, pick.accountName, pick.reason);
        }

        FundFlowCreateDTO dto = new FundFlowCreateDTO();
        dto.setAccountId(pick.accountId);
        dto.setFlowDirection(FLOW_DIRECTION_INCOME);
        dto.setFlowCategory(FLOW_CATEGORY_SALE);
        dto.setAmount(amountFen);
        dto.setBusinessDate(LocalDate.now());
        dto.setCounterpartyName(customerName != null && !customerName.isBlank() ? customerName : "散客");
        dto.setRemark(buildRemark(orderNumber, orderId));

        FundFlowVO vo = fundFlowService.create(dto);
        log.info("[METRIC] pos_fund_flow_recorded result=success orderId={} orderNumber={} amountFen={} flowNo={} accountId={} matchedBy={}",
                orderId, orderNumber, amountFen,
                vo != null ? vo.getFlowNo() : null, pick.accountId, pick.reason);
        return FundFlowWriteOutcome.recorded(vo != null ? vo.getFlowNo() : null, pick.accountId);
    }

    /** 幂等查重：方向=收入、分类=销售、备注内含本订单的 orderId 定界串 */
    private FundFlow findExistingSaleFlow(String orderId) {
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundFlow::getFlowDirection, FLOW_DIRECTION_INCOME)
               .eq(FundFlow::getFlowCategory, FLOW_CATEGORY_SALE)
               .like(FundFlow::getRemark, ORDER_ID_TOKEN_PREFIX + orderId + ORDER_ID_TOKEN_SUFFIX)
               .last("LIMIT 1");
        return fundFlowMapper.selectOne(wrapper);
    }

    /** 备注：POS销售入账：<orderNumber> [orderId=<orderId>]——前半段是业务可读口径，后半段是幂等键 */
    private String buildRemark(String orderNumber, String orderId) {
        return "POS销售入账：" + orderNumber + ORDER_ID_TOKEN_PREFIX + orderId + ORDER_ID_TOKEN_SUFFIX;
    }

    /**
     * 支付方式 → 资金账户：
     * 现金/余额取现金类账户；微信/支付宝取名称含对应关键字的商户账户；银行卡取银行账户；
     * 都不命中则回退到第一个启用账户（WARN）。
     */
    private AccountPick resolveAccount(Integer paymentMethod) {
        List<BankAccount> accounts = bankAccountMapper.selectList(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getStatus, 1)
                        .orderByAsc(BankAccount::getAccountId));
        if (accounts == null || accounts.isEmpty()) {
            return null;
        }

        int method = paymentMethod != null ? paymentMethod : 1;
        String keyword = switch (method) {
            case 2 -> "微信";
            case 3 -> "支付宝";
            case 1, 5 -> "现金";
            default -> null;
        };

        // 1) 关键字命中（微信/支付宝/现金 商户账户）
        if (keyword != null) {
            for (BankAccount account : accounts) {
                if (containsKeyword(account, keyword)) {
                    return new AccountPick(account.getAccountId(), account.getAccountName(), true, "keyword:" + keyword);
                }
            }
        }

        // 2) 账户类型命中：现金/余额 → 现金账户(2)；微信/支付宝/银行卡 → 银行账户(1)
        int preferType = (method == 1 || method == 5) ? 2 : 1;
        for (BankAccount account : accounts) {
            if (account.getAccountType() != null && account.getAccountType() == preferType) {
                return new AccountPick(account.getAccountId(), account.getAccountName(), false,
                        "accountType:" + preferType);
            }
        }

        // 3) 兜底：第一个启用账户
        BankAccount fallback = accounts.get(0);
        return new AccountPick(fallback.getAccountId(), fallback.getAccountName(), false, "fallback:firstActive");
    }

    private boolean containsKeyword(BankAccount account, String keyword) {
        return contains(account.getAccountName(), keyword)
                || contains(account.getBankName(), keyword)
                || contains(account.getRemark(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    // ==================== 挂账核销 ====================

    /**
     * 若该订单存在应收账款（应收编号 = "AR" + orderNumber，由 OrderCompletedEventListener 在
     * 「已完成但未付清」时生成），按本次实收金额核销；无应收=正常现结，直接跳过。
     *
     * @return 本次核销金额（分）
     */
    private long settleReceivableIfAny(String orderNumber, long amountFen) {
        if (receivableService == null) {
            log.debug("未装配 ReceivableService，跳过挂账核销: orderNumber={}", orderNumber);
            return 0L;
        }
        Receivable receivable = receivableService.lambdaQuery()
                .eq(Receivable::getReceivableNo, "AR" + orderNumber)
                .one();
        if (receivable == null) {
            log.debug("订单无挂账记录（现结），无需核销: orderNumber={}", orderNumber);
            return 0L;
        }
        Long balance = receivable.getBalanceAmount();
        if (balance == null || balance <= 0 || (receivable.getStatus() != null && receivable.getStatus() == 3)) {
            log.info("挂账已结清，跳过核销: orderNumber={}, receivableNo={}, status={}",
                    orderNumber, receivable.getReceivableNo(), receivable.getStatus());
            return 0L;
        }
        long apply = Math.min(amountFen, balance);
        if (apply <= 0) {
            return 0L;
        }
        Boolean ok = requiresNewTx.execute(status ->
                receivableService.confirmPayment(receivable.getReceivableId(), apply));
        boolean success = Boolean.TRUE.equals(ok);
        log.info("[METRIC] pos_receivable_settle result={} orderNumber={} receivableId={} applyFen={} balanceBeforeFen={}",
                success ? "success" : "skipped", orderNumber, receivable.getReceivableId(), apply, balance);
        return success ? apply : 0L;
    }

    // ==================== 内部结果类型 ====================

    /** 账户选择结果 */
    private static final class AccountPick {
        private final Long accountId;
        private final String accountName;
        private final boolean matchedByKeyword;
        private final String reason;

        private AccountPick(Long accountId, String accountName, boolean matchedByKeyword, String reason) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.matchedByKeyword = matchedByKeyword;
            this.reason = reason;
        }
    }

    /** 资金流水写入结果 */
    private static final class FundFlowWriteOutcome {
        private final String status;
        private final String flowNo;
        private final Long accountId;
        private final String note;

        private FundFlowWriteOutcome(String status, String flowNo, Long accountId, String note) {
            this.status = status;
            this.flowNo = flowNo;
            this.accountId = accountId;
            this.note = note;
        }

        static FundFlowWriteOutcome recorded(String flowNo, Long accountId) {
            return new FundFlowWriteOutcome("recorded", flowNo, accountId, null);
        }

        static FundFlowWriteOutcome duplicate(String flowNo, Long accountId) {
            return new FundFlowWriteOutcome("duplicate", flowNo, accountId, "已存在同订单收入流水");
        }

        static FundFlowWriteOutcome noAccount() {
            return new FundFlowWriteOutcome("no_account", null, null, "无可用资金账户");
        }

        static FundFlowWriteOutcome failed(String note) {
            return new FundFlowWriteOutcome("failed", null, null, note);
        }
    }

    /**
     * 对外可见的财务副作用结果：status ∈ recorded / duplicate / no_account / failed / skipped。
     */
    public static final class PosSaleFinanceOutcome {
        private final String fundFlowStatus;
        private final String fundFlowNo;
        private final Long accountId;
        private final long receivableSettledFen;
        private final String note;

        public PosSaleFinanceOutcome(String fundFlowStatus, String fundFlowNo, Long accountId,
                                     long receivableSettledFen, String note) {
            this.fundFlowStatus = fundFlowStatus;
            this.fundFlowNo = fundFlowNo;
            this.accountId = accountId;
            this.receivableSettledFen = receivableSettledFen;
            this.note = note;
        }

        static PosSaleFinanceOutcome skipped(String note) {
            return new PosSaleFinanceOutcome("skipped", null, null, 0L, note);
        }

        public String getFundFlowStatus() { return fundFlowStatus; }

        public String getFundFlowNo() { return fundFlowNo; }

        public Long getAccountId() { return accountId; }

        public long getReceivableSettledFen() { return receivableSettledFen; }

        public String getNote() { return note; }

        public boolean isRecorded() { return "recorded".equals(fundFlowStatus); }
    }
}
