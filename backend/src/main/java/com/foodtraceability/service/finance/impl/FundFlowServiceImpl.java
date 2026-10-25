package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.finance.AtomicBalanceUpdater;
import com.foodtraceability.common.sequence.SequenceAllocator;
import com.foodtraceability.common.util.PosPaymentMethodResolver;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.OrderItemNew;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.OrderItemNewMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 资金流水Service实现
 * 流水号格式：FF + yyyyMMdd + 4位序号
 */
@Service
public class FundFlowServiceImpl extends ServiceImpl<FundFlowMapper, FundFlow>
        implements FundFlowService {

    private static final Logger log = LoggerFactory.getLogger(FundFlowServiceImpl.class);

    /** POS 销售入账备注解析：POS销售入账：<orderNumber>[orderId=<orderId>] */
    private static final Pattern POS_SALE_REMARK_PATTERN =
            Pattern.compile("POS销售入账[:：]\\s*([^\\[\\]]+?)\\s*\\[orderId=([^\\]]+)\\]");
    private static final DateTimeFormatter ORDER_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BankAccountMapper bankAccountMapper;
    private final FundFlowMapper fundFlowMapper;
    /** BL-4：负 delta 的余额扣减必须走「带下限校验」的共用实现（与 deposit/冲正/抹平同一套文案） */
    private final AtomicBalanceUpdater atomicBalanceUpdater;
    /** 详情接口透出 POS 订单摘要所需的订单侧 mapper（列表接口不查，避免 N+1） */
    private final OrderNewMapper orderNewMapper;
    private final OrderItemNewMapper orderItemNewMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;
    /**
     * BL-5 烧号账写入器（独立事务 Bean）。
     *
     * <p>**必须注入调用、不得在下方 catch 里直接写 mapper**：业务事务马上回滚，
     * 只有 REQUIRES_NEW 的独立事务才能把烧号记录留下（见 {@link FlowNoBurnLedgerRecorder}）。</p>
     */
    private final FlowNoBurnLedgerRecorder flowNoBurnLedgerRecorder;
    /** P4-1 BL-2：全项目唯一取号实现（原 Mapper 内取号 SQL 已删除） */
    private final SequenceAllocator sequenceAllocator;

    public FundFlowServiceImpl(BankAccountMapper bankAccountMapper, FundFlowMapper fundFlowMapper,
                               AtomicBalanceUpdater atomicBalanceUpdater,
                               OrderNewMapper orderNewMapper, OrderItemNewMapper orderItemNewMapper,
                               OrderPaymentRecordNewMapper orderPaymentRecordNewMapper,
                               FlowNoBurnLedgerRecorder flowNoBurnLedgerRecorder,
                               SequenceAllocator sequenceAllocator) {
        this.bankAccountMapper = bankAccountMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.atomicBalanceUpdater = atomicBalanceUpdater;
        this.sequenceAllocator = sequenceAllocator;
        this.orderNewMapper = orderNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.orderPaymentRecordNewMapper = orderPaymentRecordNewMapper;
        this.flowNoBurnLedgerRecorder = flowNoBurnLedgerRecorder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundFlowVO create(FundFlowCreateDTO dto) {
        // 校验银行账户存在且启用
        BankAccount account = bankAccountMapper.selectById(dto.getAccountId());
        if (account == null) {
            throw new BusinessException("银行账户不存在");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException("银行账户已停用");
        }

        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new BusinessException("资金流水金额必须大于0");
        }
        if (dto.getFlowDirection() == null || (dto.getFlowDirection() != 1 && dto.getFlowDirection() != 2)) {
            throw new BusinessException("资金流水方向非法（1=收入 2=支出）");
        }

        // 创建流水
        FundFlow entity = new FundFlow();
        BeanUtils.copyProperties(dto, entity);

        // ==================================================================
        // BL-5：取号 → 记账 → 插入 的边界处理
        //   ① 取号失败**不写**烧号账（号没被消耗，不算烧号）——见 generateFlowNoAllocation 的兜底分支；
        //   ② 取号成功之后，任何让本次流水无法落库的失败（唯一键冲突 / 乐观锁耗尽 / 业务校验失败）
        //      都要在 catch 里补一条烧号账，否则号段会凭空多一个洞，A25 对不上；
        //   ③ 写账走注入的 FlowNoBurnLedgerRecorder（REQUIRES_NEW 独立事务）：
        //      本方法 @Transactional(rollbackFor = Exception.class)，catch 之后仍会整体回滚，
        //      同事务写账会被一起回滚掉 —— 这是本条最关键的约束，改代码时不要把它内联成 mapper 调用。
        //   ④ 取号（order_sequence 行级自增）与 baseMapper.insert 处在**同一个事务**内：
        //      事务回滚时号段自增也会回滚，但因为号已对外可见（本次响应/日志），仍然按烧号记账；
        //      号段回滚反而使该号可被下次重用，账表中的这一行正是「可能被重用」的审计痕迹。
        // ==================================================================
        FlowNoAllocation allocation = generateFlowNoAllocation();
        entity.setFlowNo(allocation.getFlowNo());
        // 状态初值：默认 2=已完成。
        // 本系统资金流水全部由「钱已经动了」的路径创建（POS 销售入账 / 付款单 / 人工确认入账），
        // 不存在草稿态；历史数据已全量归一为 2（scripts/db-fix/2026-10-23-fund-flow-status-normalize.sql），
        // 新流水再写 1 会立刻造成口径分裂。调用方可显式覆盖。
        try {
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 2);

        // 审计合规：操作人必须留痕。优先用调用方显式传入的（POS 支付路径传当前收银员），
        // 其次取当前登录上下文；都取不到写 0 并 WARN —— 绝不写入 null。
        Long operatorId = dto.getCreateUserId() != null ? dto.getCreateUserId() : SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            operatorId = 0L;
            log.warn("[AUDIT] 资金流水缺少操作人上下文，create_user_id 兜底记 0: accountId={}, direction={}, amount={}分",
                    dto.getAccountId(), dto.getFlowDirection(), dto.getAmount());
        }
        entity.setCreateUserId(operatorId);

        // 操作人姓名同时落 created_by（varchar 列；与 create_user_id 双轨：
        // ID 便于关联查询，姓名便于人工核对，历史行也可读）。
        // 取值优先级：当前登录用户名 → DTO.createUserId → 解析出的 operatorId → "system"
        String createdBy = SecurityUtils.getCurrentUsername();
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = dto.getCreateUserId() != null
                    ? String.valueOf(dto.getCreateUserId())
                    : (operatorId > 0 ? String.valueOf(operatorId) : "system");
        }
        entity.setCreatedBy(createdBy);

        // 并发安全 + 余额下限（BL-4）：
        //  ① 原子性：数据库端 balance = balance + delta / version = version + 1，不做「查-算-写」；
        //  ② 下限校验：direction=2（退款/支出）时 delta 为负，余额不足必须**拒绝记账**，不允许把余额打成负数。
        //     统一走 AtomicBalanceUpdater#addBalanceGuarded —— 与 deposit 存行 / 冲正 / 夹具抹平共用同一实现与文案
        //     （同一次读既做下限校验又做乐观锁版本条件，无额外竞态窗口）；余额不足/乐观锁耗尽均抛 ConflictException(409)。
        //     **不要再自己拼条件更新或另写文案。**
        long delta = dto.getFlowDirection() == 1 ? dto.getAmount() : -dto.getAmount();
        String scene = dto.getFlowDirection() == 1 ? "POS销售入账" : "POS退款/支出";
        AtomicBalanceUpdater.BalanceSnapshot snapshot =
                atomicBalanceUpdater.addBalanceGuarded(account.getAccountId(), delta, scene, entity.getFlowNo());
        // balance_after 必须取原子更新返回的 afterBalance，不得手填（红线）
        entity.setBalanceAfter(snapshot.getBalanceAfter());

        baseMapper.insert(entity);
        } catch (RuntimeException ex) {
            // BL-5 ②③：号已消耗但流水没落库 —— 独立事务补烧号账，再把原异常原样抛出（不改调用方语义）
            try {
                // 兜底（时间戳号）路径 seqValue = null：号段没被消耗，按约束②不写烧号账
                if (allocation.getSeqValue() != null) {
                    flowNoBurnLedgerRecorder.recordBurn(allocation.getFlowNo(), allocation.getSeqKey(),
                            allocation.getSeqValue(), describeFailure(ex), allocation.getCreatedBy());
                }
            } catch (RuntimeException burnEx) {
                ex.addSuppressed(burnEx);
                log.error("[A25] 烧号账写入失败，需人工补账：flowNo={} seqKey={} seqValue={}",
                        allocation.getFlowNo(), allocation.getSeqKey(), allocation.getSeqValue(), burnEx);
            }
            throw ex;
        }

        return getDetail(entity.getFlowId());
    }

    /**
     * BL-5：把失败异常归类成烧号账 reason（决策用短标签 + 关键信息）。
     */
    private String describeFailure(RuntimeException ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        String rootMsg = root.getMessage() == null ? "" : root.getMessage();

        if (ex instanceof org.springframework.dao.DuplicateKeyException
                || root instanceof java.sql.SQLException) {
            String constraint = "unknown";
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(uk_fund_flows_flow_no|fund_flows_flow_no_key|\"[a-z0-9_]*flow[a-z0-9_]*\")")
                    .matcher(msg + " " + rootMsg);
            if (m.find()) {
                constraint = m.group(1).replace("\"", "");
            }
            return "duplicate-key: " + constraint;
        }
        if (ex instanceof com.foodtraceability.common.exception.ConflictException) {
            // 409 里混着两类：额度/状态类业务校验（余额不足）与版本竞争耗尽（乐观锁）
            if (msg.contains("乐观锁")) {
                return "optimistic-lock-exhausted: " + msg;
            }
            return "business-validation: " + msg;
        }
        if (ex instanceof com.foodtraceability.common.exception.BusinessException) {
            return "business-validation: " + msg;
        }
        return ex.getClass().getSimpleName() + ": " + msg;
    }

    @Override
    public FundFlowVO getDetail(Long flowId) {
        FundFlow entity = baseMapper.selectById(flowId);
        if (entity == null) {
            return null;
        }
        // 详情接口额外透出 POS 订单摘要（支付方式中文/状态中文/明细），
        // 前端详情弹窗无需再打订单/支付方式接口，也避免订单接口 403 降级后显示 "-"。
        return convertToVO(entity, true);
    }

    @Override
    public IPage<FundFlowVO> getPage(FundFlowQueryDTO query) {
        Page<FundFlow> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();

        if (query.getAccountId() != null) {
            wrapper.eq(FundFlow::getAccountId, query.getAccountId());
        }
        if (query.getFlowDirection() != null) {
            wrapper.eq(FundFlow::getFlowDirection, query.getFlowDirection());
        }
        if (query.getFlowCategory() != null) {
            wrapper.eq(FundFlow::getFlowCategory, query.getFlowCategory());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(FundFlow::getBusinessDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(FundFlow::getBusinessDate, query.getEndDate());
        }
        // keyword：流水号 / 对方名称 / 备注 / 业务订单ID。
        // 备注清洗后已含订单号（POS销售入账：T2026…），所以「按单号搜」命中 remark；
        // 再 OR order_id 兼容直接传订单ID。空串/空白忽略，值 trim。
        String keyword = query.getKeyword() != null ? query.getKeyword().trim() : null;
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(FundFlow::getFlowNo, keyword)
                    .or().like(FundFlow::getCounterpartyName, keyword)
                    .or().like(FundFlow::getRemark, keyword)
                    .or().like(FundFlow::getOrderId, keyword));
        }
        wrapper.orderByDesc(FundFlow::getBusinessDate)
               .orderByDesc(FundFlow::getFlowId);

        IPage<FundFlow> entityPage = baseMapper.selectPage(page, wrapper);

        // 批量查询关联的银行账户名称
        Map<Long, String> accountNameMap = new HashMap<>();
        List<Long> accountIds = entityPage.getRecords().stream()
                .map(FundFlow::getAccountId)
                .distinct()
                .collect(Collectors.toList());
        if (!accountIds.isEmpty()) {
            // 只取账户 id + 名称两列：本接口不需要账户明细字段，
            // 避免因 bank_accounts 其他列的脏数据（如 account_type 历史 varchar 值）
            // 让整页列表 500（traceId=680bd8650713）。
            List<BankAccount> accounts = bankAccountMapper.selectList(
                    new LambdaQueryWrapper<BankAccount>()
                            .select(BankAccount::getAccountId, BankAccount::getAccountName)
                            .in(BankAccount::getAccountId, accountIds));
            for (BankAccount acc : accounts) {
                accountNameMap.put(acc.getAccountId(), acc.getAccountName());
            }
        }

        Page<FundFlowVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<FundFlowVO> voList = entityPage.getRecords().stream()
                .map(e -> {
                    FundFlowVO vo = convertToVO(e);
                    vo.setAccountName(accountNameMap.get(e.getAccountId()));
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<FundFlowVO> result = (IPage<FundFlowVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public Map<String, Object> getStatistics(Long accountId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundFlow::getAccountId, accountId);
        if (startDate != null) {
            wrapper.ge(FundFlow::getBusinessDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(FundFlow::getBusinessDate, endDate);
        }

        List<FundFlow> flows = baseMapper.selectList(wrapper);

        long totalIncome = 0L;
        long totalExpense = 0L;
        for (FundFlow flow : flows) {
            if (flow.getFlowDirection() != null && flow.getFlowDirection() == 1) {
                totalIncome += flow.getAmount() != null ? flow.getAmount() : 0L;
            } else if (flow.getFlowDirection() != null && flow.getFlowDirection() == 2) {
                totalExpense += flow.getAmount() != null ? flow.getAmount() : 0L;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netAmount", totalIncome - totalExpense);
        result.put("count", flows.size());
        return result;
    }

    /**
     * 生成流水号：FF + yyyyMMdd + 4 位序号。
     *
     * <p>并发安全：序号来自 order_sequence 号段表的原子自增（键 {@code FLOWyyyyMMdd}），
     * 不再「查最后一条 +1」——高并发支付时原实现会拿到相同号并撞唯一键，导致流水写不进去。</p>
     */
    private String generateFlowNo() {
        return generateFlowNoAllocation().getFlowNo();
    }

    /**
     * 取号并带回号段信息（BL-5）：返回流水号 + 号段键 + 被消耗的序号，
     * 供 {@link #create} 在「取号成功但落库失败」时写烧号账用。
     *
     * <p>兜底分支（号段异常 → 时间戳号）的 {@code seqValue} 为 {@code null}：
     * 该路径**没有消耗号段**，按 Owner 约束「取号本身失败不写烧号账」，
     * 调用方只在 {@code seqValue != null} 的语义下记账。</p>
     */
    private FlowNoAllocation generateFlowNoAllocation() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequenceKey = "FLOW" + datePart;
        String prefix = "FF" + datePart;
        try {
            // BL-2-b / P4-1：改用带「下限兜底」的统一取号器（SequenceAllocator#next）：
            // 单条语句 = GREATEST(号段当前值, 当天已用最大序号) + 1 并 RETURNING。
            //
            // 为什么同事务内连续取 N 个号也不会重号（三条理由，缺一不可）：
            //  ① 号源是号段表 sequence_value **自身的行级锁自增**，不是「查 MAX + 1」——
            //     并发事务在这里排队天然串行；号段落后只会被抬升，不会被压低。
            //  ② 每次调用都真实执行本语句：注解上 flushCache=TRUE（清一级缓存）、useCache=false（跳二级缓存），
            //     否则 MyBatis 会把同事务内第二次同参调用当缓存命中并返回同一个号 → 撞
            //     fund_flows_flow_no_key（P2-A 缺陷 1 即此）。
            //  ③ GREATEST(..., 当天 MAX) 只做**下限兜底**：防的是「号段被调低 / 历史直写 SQL 越过号段」
            //     造成的落后（如号段 200 而当天已用到 232）；事务内它最多等于上一号，
            //     不会撞上本事务刚取走的下一个值。
            Integer seq = sequenceAllocator.next(sequenceKey, prefix, "fund_flows", "flow_no");
            if (seq != null && seq > 0) {
                // 号段真实消耗：seqValue 非空 → 后续若落库失败必须写烧号账
                return new FlowNoAllocation(String.format("%s%04d", prefix, seq), sequenceKey,
                        seq.longValue(), com.foodtraceability.utils.SecurityUtils.getCurrentUsername());
            }
        } catch (Exception e) {
            log.warn("流水号段分配失败，降级为时间戳流水号: {}", e.getMessage());
        }
        // 兜底：号段表异常时用时间戳保证唯一（仍满足 FF 前缀，仅序号段不再是 4 位顺序号）
        // 该路径没有消耗号段 → seqValue = null（不写烧号账）
        return new FlowNoAllocation(prefix + String.format("%04d", System.currentTimeMillis() % 10000),
                sequenceKey, null, com.foodtraceability.utils.SecurityUtils.getCurrentUsername());
    }

    /**
     * 取号结果（BL-5）：流水号 + 号段键 + 被消耗序号（兜底路径为 null）。
     */
    private static final class FlowNoAllocation {
        private final String flowNo;
        private final String seqKey;
        private final Long seqValue;
        private final String createdBy;

        FlowNoAllocation(String flowNo, String seqKey, Long seqValue, String createdBy) {
            this.flowNo = flowNo;
            this.seqKey = seqKey;
            this.seqValue = seqValue;
            this.createdBy = createdBy;
        }

        String getFlowNo() { return flowNo; }
        String getSeqKey() { return seqKey; }
        Long getSeqValue() { return seqValue; }
        String getCreatedBy() { return createdBy; }
    }

    /**
     * 实体转VO（不含 POS 订单摘要，列表接口用）
     */
    private FundFlowVO convertToVO(FundFlow entity) {
        return convertToVO(entity, false);
    }

    /**
     * 实体转VO
     *
     * @param withPosOrder 是否附带 POS 订单摘要（仅详情接口传 true；列表接口不查，避免 N+1）
     */
    private FundFlowVO convertToVO(FundFlow entity, boolean withPosOrder) {
        FundFlowVO vo = new FundFlowVO();
        BeanUtils.copyProperties(entity, vo);
        // 状态兜底：历史行可能是 0/NULL（无对应状态），统一按「待处理」呈现，避免前端显示「未知」
        if (vo.getStatus() == null || vo.getStatus() < 1 || vo.getStatus() > 3) {
            vo.setStatus(1);
        }
        vo.setStatusName(getStatusName(vo.getStatus()));
        // 操作人展示名：BeanUtils 已复制同名字段，这里再兜底一次（历史行 created_by 为空时前端显示 '-'）
        if (vo.getCreatedBy() == null && entity.getCreatedBy() != null) {
            vo.setCreatedBy(entity.getCreatedBy());
        }
        if (withPosOrder) {
            vo.setPosOrder(buildPosOrderSummary(entity));
        }
        return vo;
    }

    // ==================== POS 订单摘要（详情接口） ====================

    /**
     * 从流水备注解析 POS 订单并组装摘要。
     *
     * <p>备注口径由 {@code PosSaleFinanceRecorder.buildSaleRemark} 统一生成：
     * {@code POS销售入账：<orderNumber>}（历史行可能仍带 {@code [orderId=...]} 技术串）。
     * 解析不到（非 POS 入账流水）返回 null，
     * 不影响既有详情接口的其它字段。</p>
     *
     * <p><b>订单定位优先用 {@code fund_flows.order_id} 列</b>（2026-10-24 新增，备注清洗后不再带
     * {@code [orderId=...]} 技术串）；仅当 order_id 为空的历史行，才回退解析旧备注。</p>
     *
     * <p>返回体<b>不含 orderId</b>：前端要求隐藏内部ID，订单号即可满足展示与追溯。</p>
     */
    private FundFlowVO.PosOrderSummary buildPosOrderSummary(FundFlow entity) {
        if (entity == null) {
            return null;
        }
        String remark = entity.getRemark();
        try {
            String orderNumber = null;
            // 新口径：order_id 列直接承载订单关联
            String orderId = trimToNull(entity.getOrderId());
            // 兼容历史行：order_id 为空时才回退解析旧备注里的技术串
            if (orderId == null && remark != null) {
                Matcher matcher = POS_SALE_REMARK_PATTERN.matcher(remark);
                if (matcher.find()) {
                    orderNumber = trimToNull(matcher.group(1));
                    orderId = trimToNull(matcher.group(2));
                }
            }
            if ((orderNumber == null || orderNumber.isEmpty()) && (orderId == null || orderId.isEmpty())) {
                return null;
            }

            OrderNew order = findOrder(orderId, orderNumber);
            if (order == null) {
                return null;
            }

            FundFlowVO.PosOrderSummary summary = new FundFlowVO.PosOrderSummary();
            summary.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderCode());
            summary.setOrderTime(order.getCreateTime() != null
                    ? order.getCreateTime().format(ORDER_TIME_FORMATTER) : null);
            summary.setOrderStatusCode(order.getOrderStatus());
            summary.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
            summary.setPaymentMethodName(resolvePaymentMethodName(order));
            summary.setOrderAmount(centsToYuan(order.getFinalAmount() != null
                    ? order.getFinalAmount() : order.getTotalAmount()));
            summary.setItems(loadOrderItems(order.getOrderId()));
            return summary;
        } catch (Exception e) {
            // 摘要只是详情增强：任何异常都不能让详情接口失败
            log.warn("组装 POS 订单摘要失败（详情接口降级返回空摘要）: orderId={}, remark={}, error={}",
                    entity.getOrderId(), remark, e.getMessage());
            return null;
        }
    }

    /** 先按 orderId 精确查，查不到再按 orderNumber 兜底（历史备注可能只有订单号） */
    private OrderNew findOrder(String orderId, String orderNumber) {
        if (orderId != null && !orderId.isEmpty()) {
            OrderNew byId = orderNewMapper.selectById(orderId);
            if (byId != null) {
                return byId;
            }
        }
        if (orderNumber != null && !orderNumber.isEmpty()) {
            return orderNewMapper.selectOne(new LambdaQueryWrapper<OrderNew>()
                    .and(w -> w.eq(OrderNew::getOrderNumber, orderNumber)
                            .or().eq(OrderNew::getOrderCode, orderNumber))
                    .last("LIMIT 1"));
        }
        return null;
    }

    /**
     * 支付方式中文（与 POS 详情/列表共用同一解析器 {@link PosPaymentMethodResolver}）：
     * 首选 order_payment_records 最近一条；无记录时回退 orders.payment_method。
     * <b>两列已是同一套编码</b>（1现金/2微信/3支付宝/4银行卡/5余额，2026-10-24 单轨化），不再做旧编码映射；
     * 两者都取不到才返回「（系统未记录流水，请查单）」。
     */
    private String resolvePaymentMethodName(OrderNew order) {
        if (order == null) {
            return PosPaymentMethodResolver.UNRECORDED;
        }
        Integer recordCode = null;
        String orderId = order.getOrderId();
        if (orderId != null && !orderId.isEmpty()) {
            List<OrderPaymentRecordNew> records = orderPaymentRecordNewMapper.selectByOrderId(orderId);
            if (records != null && !records.isEmpty()) {
                // mapper 按 payment_time ASC，取最后一条（最近一次支付）
                recordCode = records.get(records.size() - 1).getPaymentMethod();
            }
        }
        return PosPaymentMethodResolver.resolve(recordCode, order.getPaymentMethod());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 订单状态中文（canonical orders.order_status：0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款） */
    private String getOrderStatusName(Integer orderStatus) {
        if (orderStatus == null) {
            return "待确认";
        }
        return switch (orderStatus) {
            case 1 -> "已确认";
            case 2 -> "已完成";
            case 3 -> "已取消";
            case 4 -> "部分退款";
            case 5 -> "全额退款";
            default -> "待确认";
        };
    }

    /** 订单明细（元）：单价/小计由「分」转「元」 */
    private List<FundFlowVO.PosOrderItem> loadOrderItems(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderItemNew> items = orderItemNewMapper.selectList(
                new LambdaQueryWrapper<OrderItemNew>().eq(OrderItemNew::getOrderId, orderId));
        List<FundFlowVO.PosOrderItem> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (OrderItemNew item : items) {
            FundFlowVO.PosOrderItem row = new FundFlowVO.PosOrderItem();
            row.setName(item.getProductName());
            row.setQuantity(item.getQuantity());
            row.setPrice(centsToYuan(item.getUnitPrice()));
            row.setAmount(item.getAmount() != null
                    ? centsToYuan(item.getAmount())
                    : centsToYuan(item.getUnitPrice() != null && item.getQuantity() != null
                            ? item.getUnitPrice() * item.getQuantity() : null));
            result.add(row);
        }
        return result;
    }

    /** 分 → 元（保留 2 位） */
    private BigDecimal centsToYuan(Long cents) {
        if (cents == null) {
            return null;
        }
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 流水状态中文名：1-待处理 2-已完成 3-已取消
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "待处理";
        }
        return switch (status) {
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "待处理";
        };
    }
}
