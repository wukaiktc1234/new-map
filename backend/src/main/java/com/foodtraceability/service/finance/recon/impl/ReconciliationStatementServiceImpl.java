package com.foodtraceability.service.finance.recon.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.finance.recon.ReconciliationItemVO;
import com.foodtraceability.dto.finance.recon.ReconciliationStatementVO;
import com.foodtraceability.dto.finance.recon.StatementCreateRequest;
import com.foodtraceability.dto.finance.recon.StatementItemRequest;
import com.foodtraceability.dto.finance.recon.StatementQueryDTO;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.service.finance.recon.ReconciliationStatementService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 银行存款余额调节表（S3–S9）。
 *
 * <p><b>硬规则一（design §4.3）</b>：生成 / 重算 / 提交 / 审核 <b>不写 fund_flows、不改 bank_accounts.balance</b>。
 * 本类只对 reconciliation_statements / reconciliation_items 做写操作，对 fund_flows / bank_transactions / bank_accounts 只读。</p>
 *
 * <p><b>实现取舍</b>：使用 JdbcTemplate 直连 SQL（不依赖 MyBatis 实体），因为 §5.10 两表由 call-number 的
 * V20261024_004 创建；这样迁移落盘即可用，不与他的 entity/mapper 抢命名。字段名严格按 §5.10.1 / §5.10.2。</p>
 *
 * <p><b>四类未达账项（全文唯一口径）</b>：A=企业已收银行未收（银行侧加）、B=企业已付银行未付（银行侧减）、
 * C=银行已收企业未收（企业侧加）、D=银行已付企业未付（企业侧减）。恒等式：book + C − D = bank + A − B。</p>
 */
@Service
public class ReconciliationStatementServiceImpl implements ReconciliationStatementService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ReconciliationStatementServiceImpl.class);

    /** A7 非豁免口径（唯一来源）：补账流水的 balance_after 按设计写 0，必须排除，否则账面链被判断裂 */
    private static final String NON_EXEMPT =
            " AND COALESCE(ff.flow_no, '') NOT LIKE 'FFH%' AND COALESCE(ff.created_by, '') NOT LIKE 'system/backfill-%' ";

    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "A", "企业已收、银行未收",
            "B", "企业已付、银行未付",
            "C", "银行已收、企业未收",
            "D", "银行已付、企业未付");

    private final JdbcTemplate jdbc;

    public ReconciliationStatementServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================================================================
    // S3 生成 / 重算
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationStatementVO generate(StatementCreateRequest req) {
        if (req == null || req.getAccountId() == null) {
            throw err("accountId 必填");
        }
        if (req.getPeriodStart() == null || req.getPeriodEnd() == null) {
            throw err("periodStart / periodEnd 必填");
        }
        if (req.getPeriodEnd().isBefore(req.getPeriodStart())) {
            throw err("periodEnd 不能早于 periodStart");
        }
        if (req.getBankStatementBalanceFen() == null) {
            // 不默认 0：对账单余额必须由人对账单录入
            throw err("bankStatementBalanceFen（对账单余额，分）必填，不接受默认值");
        }
        Long accountId = req.getAccountId();
        LocalDate ps = req.getPeriodStart();
        LocalDate pe = req.getPeriodEnd();

        Map<String, Object> acc = oneOrNull(
                "SELECT account_id, store_id, account_name FROM bank_accounts WHERE account_id = ? AND deleted = 0", accountId);
        if (acc == null) {
            throw err("账户不存在或已删除: " + accountId);
        }
        Long storeId = acc.get("store_id") == null ? null : ((Number) acc.get("store_id")).longValue();

        // ---- book_balance：期间末最后一笔「非豁免」balance_after，取不到直接抛错（不默认 0）----
        Long bookBalance = lastBookBalance(accountId, pe);
        if (bookBalance == null) {
            throw err("账户 " + accountId + " 在 " + pe + " 及之前没有任何非豁免资金流水，"
                    + "无法确定企业账面余额（拒绝默认 0；请先补记流水或调整期间）");
        }

        // ---- 已有同账户同期间表的处置（重算规则）----
        Map<String, Object> existing = oneOrNull(
                "SELECT statement_id, status FROM reconciliation_statements "
                        + " WHERE account_id = ? AND period_start = ? AND period_end = ? AND deleted = 0", accountId, ps, pe);
        Long statementId = null;
        if (existing != null) {
            String status = String.valueOf(existing.get("status"));
            long sid = ((Number) existing.get("statement_id")).longValue();
            if ("SUBMITTED".equals(status)) {
                throw err("期间 " + ps + "~" + pe + " 的调节表（ID=" + sid + "）已提交（SUBMITTED），"
                        + "不允许重算；请先撤回，或改用一个新期间");
            }
            if ("APPROVED".equals(status)) {
                throw err("期间 " + ps + "~" + pe + " 的调节表（ID=" + sid + "）已审核锁定（APPROVED），"
                        + "不允许重算；如需更正请重开新期间（旧表保留存档）");
            }
            if (Boolean.FALSE.equals(req.getForceRecalc())) {
                return detail(sid);
            }
            statementId = sid;
            // DRAFT 原地重算：先删旧 items 再重插（同一事务内）
            jdbc.update("DELETE FROM reconciliation_items WHERE statement_id = ?", statementId);
        }

        // ---- 未达账项识别（人工指定优先，否则自动识别）----
        List<StatementItemRequest> items = (req.getItems() != null && !req.getItems().isEmpty())
                ? normalizeManualItems(req.getItems())
                : autoDetectItems(accountId, ps, pe, statementId);
        validateItems(items, ps, pe);

        // ---- 假平衡门禁（BL-6）：先识别「同额对侧候选」歧义，再算 is_balanced ----
        // 规则：**不自动二选一**。按金额分组，同一金额在本表内同时存在内部侧(A/B)与外部侧(C/D)条目时，
        // 该组内每一条都标记 AMBIGUOUS（复用 reason 前缀，不加列），并记下对侧候选 ref，交人工处置。
        int ambiguousCount = markAmbiguities(items);

        // ---- 四类合计与两个调节后余额 ----
        long a = sum(items, "A");
        long b = sum(items, "B");
        long c = sum(items, "C");
        long d = sum(items, "D");
        long bankBalance = req.getBankStatementBalanceFen();
        long adjustedBook = bookBalance + c - d;
        long adjustedBank = bankBalance + a - b;
        // is_balanced 前置条件（BL-6）：只要存在未解决的 AMBIGUOUS 歧义，即使两侧金额相等也判**不平衡**，
        // 否则就是「假平衡」——金额凑巧相等，但根本没确认哪笔对哪笔。
        boolean amountBalanced = adjustedBook == adjustedBank;
        // BL-6 收口：再并入 A19 口径的「同额两侧并存」判据 —— 即使 reason 前缀被重算冲掉，
        // 只要同一金额同时出现在 (A|B) 与 (C|D) 两侧，就不是真平衡（与 A19 同一判据，避免两套口径打架）。
        boolean sameAmountBothSides = hasSameAmountOnBothSides(items);
        boolean balanced = amountBalanced && ambiguousCount == 0 && !sameAmountBothSides;

        if (!balanced && (req.getRemark() == null || req.getRemark().isBlank())) {
            // 只允许存 DRAFT，但差异/歧义原因必须写清（V5 / V6 精神）
            if (ambiguousCount > 0) {
                throw err("存在 " + ambiguousCount + " 笔未解决的同额歧义（AMBIGUOUS），remark 必须填写处置说明");
            }
            throw err("调节后两侧余额不等（企业侧 " + adjustedBook + " 分 vs 银行侧 " + adjustedBank
                    + " 分，差 " + (adjustedBook - adjustedBank) + " 分），remark 必须填写差异原因");
        }

        Long userId = currentUserId();
        String userName = currentUserName();
        LocalDateTime now = LocalDateTime.now();

        if (statementId == null) {
            String statementNo = nextStatementNo(pe);
            jdbc.update("INSERT INTO reconciliation_statements "
                            + "(statement_no, account_id, store_id, period_start, period_end, book_balance, bank_balance,"
                            + " uncleared_in_credit, uncleared_in_debit, uncleared_out_credit, uncleared_out_debit,"
                            + " adjusted_book_balance, adjusted_bank_balance, is_balanced, status, remark,"
                            + " create_user_id, created_by, create_time, update_time, version, deleted) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)",
                    statementNo, accountId, storeId, ps, pe, bookBalance, bankBalance,
                    a, b, c, d, adjustedBook, adjustedBank, balanced, "DRAFT", req.getRemark(),
                    userId, userName, now, now);
            statementId = jdbc.queryForObject(
                    "SELECT statement_id FROM reconciliation_statements WHERE statement_no = ?", Long.class, statementNo);
        } else {
            jdbc.update("UPDATE reconciliation_statements SET book_balance = ?, bank_balance = ?,"
                            + " uncleared_in_credit = ?, uncleared_in_debit = ?, uncleared_out_credit = ?, uncleared_out_debit = ?,"
                            + " adjusted_book_balance = ?, adjusted_bank_balance = ?, is_balanced = ?, remark = ?,"
                            + " store_id = ?, update_time = ?, version = version + 1 "
                            + " WHERE statement_id = ?",
                    bookBalance, bankBalance, a, b, c, d, adjustedBook, adjustedBank, balanced, req.getRemark(),
                    storeId, now, statementId);
        }

        for (StatementItemRequest it : items) {
            jdbc.update("INSERT INTO reconciliation_items "
                            + "(statement_id, category, flow_id, transaction_id, amount, business_date, period_start, period_end,"
                            + " owner_user_id, owner_name, reason, cleared_flag, create_time, update_time, deleted) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,FALSE,?,?,0)",
                    statementId, it.getCategory(), it.getFlowId(), it.getTransactionId(), it.getAmount(),
                    it.getBusinessDate(), ps, pe,
                    it.getOwnerUserId() == null ? userId : it.getOwnerUserId(),
                    it.getOwnerUserId() == null ? userName : lookupUserName(it.getOwnerUserId()),
                    it.getReason(), now, now);
        }
        return detail(statementId);
    }

    // ==================================================================
    // S4 / S5
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public IPage<ReconciliationStatementVO> page(StatementQueryDTO q) {
        int current = q.getCurrent() == null || q.getCurrent() < 1 ? 1 : q.getCurrent();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : Math.min(q.getSize(), 200);
        StringBuilder where = new StringBuilder(" WHERE deleted = 0 ");
        List<Object> args = new ArrayList<>();
        if (q.getAccountId() != null) {
            where.append(" AND account_id = ? ");
            args.add(q.getAccountId());
        }
        if (q.getStatus() != null && !q.getStatus().isBlank()) {
            where.append(" AND status = ? ");
            args.add(q.getStatus());
        }
        if (q.getPeriodStart() != null) {
            where.append(" AND period_start >= ? ");
            args.add(q.getPeriodStart());
        }
        if (q.getPeriodEnd() != null) {
            where.append(" AND period_end <= ? ");
            args.add(q.getPeriodEnd());
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM reconciliation_statements" + where,
                Long.class, args.toArray());
        long totalVal = total == null ? 0 : total;

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((current - 1) * size);
        List<ReconciliationStatementVO> records = jdbc.query(
                "SELECT * FROM reconciliation_statements" + where
                        + " ORDER BY period_end DESC, statement_id DESC LIMIT ? OFFSET ?",
                STATEMENT_MAPPER, pageArgs.toArray());

        IPage<ReconciliationStatementVO> out = new Page<>(current, size, totalVal);
        out.setRecords(records);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationStatementVO detail(Long statementId) {
        List<ReconciliationStatementVO> list = jdbc.query(
                "SELECT * FROM reconciliation_statements WHERE statement_id = ? AND deleted = 0",
                STATEMENT_MAPPER, statementId);
        if (list.isEmpty()) {
            throw err("调节表不存在: " + statementId);
        }
        ReconciliationStatementVO vo = list.get(0);
        vo.items = jdbc.query(
                "SELECT item_id, category, flow_id, transaction_id, amount, business_date, period_start, period_end,"
                        + " owner_user_id, owner_name, reason, cleared_flag "
                        + " FROM reconciliation_items WHERE statement_id = ? AND deleted = 0 ORDER BY category, item_id",
                (rs, i) -> {
                    ReconciliationItemVO it = new ReconciliationItemVO();
                    it.itemId = rs.getLong("item_id");
                    it.category = rs.getString("category");
                    it.categoryName = CATEGORY_NAMES.getOrDefault(it.category, it.category);
                    it.flowId = rs.getObject("flow_id") == null ? null : rs.getLong("flow_id");
                    it.transactionId = rs.getObject("transaction_id") == null ? null : rs.getLong("transaction_id");
                    it.amount = rs.getLong("amount");
                    it.businessDate = rs.getObject("business_date") == null ? null : rs.getDate("business_date").toLocalDate();
                    it.periodStart = rs.getObject("period_start") == null ? null : rs.getDate("period_start").toLocalDate();
                    it.periodEnd = rs.getObject("period_end") == null ? null : rs.getDate("period_end").toLocalDate();
                    it.ownerUserId = rs.getObject("owner_user_id") == null ? null : rs.getLong("owner_user_id");
                    it.ownerName = rs.getString("owner_name");
                    it.reason = rs.getString("reason");
                    it.clearedFlag = rs.getBoolean("cleared_flag");
                    it.ambiguous = it.reason != null && it.reason.startsWith("AMBIGUOUS:");
                    return it;
                }, statementId);
        vo.ambiguousItemCount = (int) vo.items.stream()
                .filter(it -> Boolean.TRUE.equals(it.ambiguous)).count();
        return vo;
    }

    // ==================================================================
    // S6 提交（V5 硬门禁）
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationStatementVO submit(Long statementId) {
        ReconciliationStatementVO vo = detail(statementId);
        if (!"DRAFT".equals(vo.status)) {
            throw err("只有 DRAFT 状态可以提交，当前状态：" + vo.status);
        }
        // V5 硬门禁：调节后两侧必须相等 **且** 无未解决歧义，否则禁止 SUBMIT（不可配置关闭）
        int ambiguous = vo.ambiguousItemCount == null ? 0 : vo.ambiguousItemCount;
        if (ambiguous > 0) {
            // 文案必须报**原因分类**，不能只说「两侧不等」——金额可能是相等的（假平衡）
            throw err("V5 门禁失败：存在 " + ambiguous + " 笔未解决的同额歧义（AMBIGUOUS），请先处置后再提交"
                    + "（处置端点：POST /v1/finance/recon/statements/" + statementId + "/items/{itemId}/resolve-ambiguity）");
        }
        if (!Boolean.TRUE.equals(vo.isBalanced)) {
            long diff = (vo.adjustedBookBalance == null ? 0 : vo.adjustedBookBalance)
                    - (vo.adjustedBankBalance == null ? 0 : vo.adjustedBankBalance);
            throw err("V5 门禁失败：调节后两侧余额不等（差 " + diff
                    + " 分），不允许提交；请补齐未达账项或修正对账单余额后重算");
        }
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE reconciliation_statements SET status = 'SUBMITTED', submitted_by = ?, submitted_at = ?,"
                + " update_time = ?, version = version + 1 WHERE statement_id = ? AND status = 'DRAFT'",
                userId, now, now, statementId);
        return detail(statementId);
    }

    // ==================================================================
    // S7 审核（V8 四眼 + 角色门禁 + 复核 V5）
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationStatementVO approve(Long statementId) {
        ReconciliationStatementVO vo = detail(statementId);
        if (!"SUBMITTED".equals(vo.status)) {
            throw err("只有 SUBMITTED 状态可以审核，当前状态：" + vo.status);
        }
        Long userId = currentUserId();

        // ---- V8-a 四眼原则：提交人与审核人不得为同一人 ----
        if (vo.submittedBy != null && vo.submittedBy.equals(userId)) {
            throw err("V8 门禁失败：提交人与审核人不得为同一人（四眼原则，不可配置关闭）");
        }
        // ---- V8-b 角色门禁：审核人不得持有 ROLE_TELLER（出纳）----
        List<String> roles = userRoles(userId);
        if (roles.contains("ROLE_TELLER")) {
            throw err("V8 门禁失败：出纳（ROLE_TELLER）不得审核调节表");
        }
        // ---- 复核 V5：DRAFT/SUBMITTED 期间账面数据可能已变，必须重新核对 ----
        Long bookNow = lastBookBalance(vo.accountId, vo.periodEnd);
        if (bookNow == null) {
            throw err("复核 V5 失败：期间末已无可用账面余额（流水被清理？），拒绝审核");
        }
        if (!bookNow.equals(vo.bookBalance)) {
            throw err("复核 V5 失败：期间内账面余额已变化（生成时 " + vo.bookBalance
                    + " 分，现在 " + bookNow + " 分），请重算后再提交/审核");
        }
        long adjustedBook = vo.bookBalance + nz(vo.unclearedOutCredit) - nz(vo.unclearedOutDebit);
        long adjustedBank = vo.bankBalance + nz(vo.unclearedInCredit) - nz(vo.unclearedInDebit);
        if (adjustedBook != adjustedBank || !Boolean.TRUE.equals(vo.isBalanced)) {
            throw err("复核 V5 失败：调节后两侧余额不等（差 " + (adjustedBook - adjustedBank)
                    + " 分），拒绝审核");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE reconciliation_statements SET status = 'APPROVED', approved_by = ?, approved_at = ?,"
                + " update_time = ?, version = version + 1 WHERE statement_id = ? AND status = 'SUBMITTED'",
                userId, now, now, statementId);
        return detail(statementId);
    }

    // ==================================================================
    // S9 导出 CSV
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public String exportCsv(Long statementId) {
        ReconciliationStatementVO vo = detail(statementId);
        StringBuilder sb = new StringBuilder();
        sb.append("项目,金额(分)\n");
        sb.append("调节表号,").append(vo.statementNo).append('\n');
        sb.append("账户ID,").append(vo.accountId).append('\n');
        sb.append("期间,").append(vo.periodStart).append(" ~ ").append(vo.periodEnd).append('\n');
        sb.append("1 企业账面余额,").append(nz(vo.bookBalance)).append('\n');
        sb.append("2 加:银行已收企业未收(C),").append(nz(vo.unclearedOutCredit)).append('\n');
        sb.append("3 减:银行已付企业未付(D),").append(nz(vo.unclearedOutDebit)).append('\n');
        sb.append("4 加:企业已收银行未收(A),").append(nz(vo.unclearedInCredit)).append('\n');
        sb.append("5 减:企业已付银行未付(B),").append(nz(vo.unclearedInDebit)).append('\n');
        sb.append("企业调节后余额,").append(nz(vo.adjustedBookBalance)).append('\n');
        sb.append("对账单余额,").append(nz(vo.bankBalance)).append('\n');
        sb.append("银行调节后余额,").append(nz(vo.adjustedBankBalance)).append('\n');
        sb.append("差异,").append(nz(vo.adjustedBookBalance) - nz(vo.adjustedBankBalance)).append('\n');
        sb.append("状态,").append(vo.status).append('\n');
        sb.append("\n类别,名称,flow_id,transaction_id,金额(分),业务日,挂账人,原因\n");
        if (vo.items != null) {
            for (ReconciliationItemVO it : vo.items) {
                sb.append(it.category).append(',').append(it.categoryName).append(',')
                        .append(it.flowId == null ? "" : it.flowId).append(',')
                        .append(it.transactionId == null ? "" : it.transactionId).append(',')
                        .append(it.amount).append(',').append(it.businessDate == null ? "" : it.businessDate).append(',')
                        .append(it.ownerName == null ? "" : it.ownerName).append(',')
                        .append(it.reason == null ? "" : it.reason.replace(",", " ")).append('\n');
            }
        }
        return sb.toString();
    }

    // ==================================================================
    // 内部：未达账项识别与校验
    // ==================================================================

    /** 自动识别四类未达账项（A/B 来自 fund_flows 未匹配，C/D 来自 bank_transactions 未匹配） */
    private List<StatementItemRequest> autoDetectItems(Long accountId, LocalDate ps, LocalDate pe, Long excludeStatementId) {
        Long userId = currentUserId();
        String userName = currentUserName();
        List<StatementItemRequest> out = new ArrayList<>();

        // A / B：企业侧已记账、银行侧无对应（bank_transactions.matched_flow_id 为空）
        out.addAll(jdbc.query(
                "SELECT ff.flow_id, ff.amount, ff.business_date, ff.flow_direction FROM fund_flows ff "
                        + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.status = 2 "
                        + "   AND ff.business_date BETWEEN ? AND ? "
                        + "   AND NOT EXISTS (SELECT 1 FROM bank_transactions bt "
                        + "                    WHERE bt.matched_flow_id = ff.flow_id AND bt.deleted = 0) "
                        + " ORDER BY ff.business_date, ff.flow_id",
                (rs, i) -> {
                    StatementItemRequest it = new StatementItemRequest();
                    it.setCategory(rs.getInt("flow_direction") == 1 ? "A" : "B");
                    it.setFlowId(rs.getLong("flow_id"));
                    it.setAmount(rs.getLong("amount"));
                    it.setBusinessDate(rs.getDate("business_date").toLocalDate());
                    it.setOwnerUserId(userId);
                    it.setReason("自动识别：企业侧已" + (rs.getInt("flow_direction") == 1 ? "收款" : "付款")
                            + "，银行侧期间内无对应到账（系统挂账，待下期核销）");
                    return it;
                }, accountId, ps, pe));

        // C / D：银行侧已发生、企业侧无对应（matched_flow_id 为空）
        out.addAll(jdbc.query(
                "SELECT bt.transaction_id, bt.amount, bt.business_date, bt.direction, bt.needs_review FROM bank_transactions bt "
                        + " WHERE bt.account_id = ? AND bt.deleted = 0 "
                        + "   AND bt.business_date BETWEEN ? AND ? "
                        + "   AND bt.matched_flow_id IS NULL "
                        + " ORDER BY bt.business_date, bt.transaction_id",
                (rs, i) -> {
                    StatementItemRequest it = new StatementItemRequest();
                    it.setCategory(rs.getInt("direction") == 1 ? "C" : "D");
                    it.setTransactionId(rs.getLong("transaction_id"));
                    it.setAmount(rs.getLong("amount"));
                    it.setBusinessDate(rs.getDate("business_date").toLocalDate());
                    it.setOwnerUserId(userId);
                    String timeNote = "";
                    if (rs.getBoolean("needs_review")) {
                        // BL-6 §5：导入时源文件只有日期 → 该银行流水时间不可用，必须显式标注，避免 ±10 分钟窗静默失效
                        timeNote = "；【时间不可用】该银行流水导入时仅有日期无时刻（needs_review）";
                    }
                    it.setReason("自动识别：银行侧已" + (rs.getInt("direction") == 1 ? "入账" : "扣款")
                            + "，企业侧期间内无对应流水（系统挂账，待下期核销）" + timeNote);
                    return it;
                }, accountId, ps, pe));
        return out;
    }

    private List<StatementItemRequest> normalizeManualItems(List<StatementItemRequest> items) {
        List<StatementItemRequest> out = new ArrayList<>();
        for (StatementItemRequest it : items) {
            String cat = it.getCategory() == null ? "" : it.getCategory().toUpperCase();
            if (!CATEGORY_NAMES.containsKey(cat)) {
                throw err("未达账项类别只能是 A/B/C/D，收到：" + it.getCategory());
            }
            if (it.getAmount() == null || it.getAmount() <= 0) {
                throw err("未达账项金额必须为正数（分），方向由 category 决定");
            }
            if (("A".equals(cat) || "B".equals(cat)) && it.getFlowId() == null) {
                throw err(cat + " 类必须关联 fund_flows.flow_id");
            }
            if (("C".equals(cat) || "D".equals(cat)) && it.getTransactionId() == null) {
                throw err(cat + " 类必须关联 bank_transactions.transaction_id");
            }
            if (it.getReason() == null || it.getReason().isBlank()) {
                throw err("每条未达账项必须有原因（V6）");
            }
            it.setCategory(cat);
            out.add(it);
        }
        return out;
    }

    /**
     * V6 / V7 校验：
     * V6 每条必须有挂账人 + 原因 + 期间（挂账人缺省取当前操作者，原因必须显式给）；
     * V7 同一笔（flow_id / transaction_id）在同一期间只出现一次，且不得已被同期间其他调节表挂过。
     */
    /**
     * 标记同额歧义（BL-6，**不自动二选一**）。
     *
     * <p>规则：按金额分组；同一金额在本表内同时存在内部侧（A/B）与外部侧（C/D）条目时，
     * 该组内**每一条**都标 AMBIGUOUS，并在 reason 里写明对侧候选 ref（A/B 组给 transaction_id，
     * C/D 组给 flow_id），交人工用 resolve-ambiguity 端点处置。</p>
     *
     * <p>为什么用 reason 前缀而不是加列：§5.10.2 没有状态列，lead 明确要求「不加列」；前缀
     * {@code AMBIGUOUS:} 可被 SQL 直接 LIKE 出来，且人眼看报告也直观。</p>
     *
     * @return 歧义条目数
     */
    private int markAmbiguities(List<StatementItemRequest> items) {
        java.util.Map<Long, java.util.List<StatementItemRequest>> byAmount = new java.util.LinkedHashMap<>();
        for (StatementItemRequest it : items) {
            if (it.getAmount() == null) {
                continue;
            }
            byAmount.computeIfAbsent(it.getAmount(), k -> new java.util.ArrayList<>()).add(it);
        }
        int marked = 0;
        for (java.util.Map.Entry<Long, java.util.List<StatementItemRequest>> e : byAmount.entrySet()) {
            java.util.List<StatementItemRequest> group = e.getValue();
            java.util.List<StatementItemRequest> internal = new java.util.ArrayList<>();
            java.util.List<StatementItemRequest> external = new java.util.ArrayList<>();
            for (StatementItemRequest it : group) {
                if ("C".equals(it.getCategory()) || "D".equals(it.getCategory())) {
                    external.add(it);
                } else {
                    internal.add(it);
                }
            }
            // 只有「两侧都有同额条目」才叫歧义；单侧多笔不算（那是内部重复入账，另有 A4 管）
            if (internal.isEmpty() || external.isEmpty()) {
                continue;
            }
            String extRefs = external.stream()
                    .map(it -> it.getTransactionId() == null ? "-" : String.valueOf(it.getTransactionId()))
                    .distinct().collect(java.util.stream.Collectors.joining(","));
            String intRefs = internal.stream()
                    .map(it -> it.getFlowId() == null ? "-" : String.valueOf(it.getFlowId()))
                    .distinct().collect(java.util.stream.Collectors.joining(","));
            for (StatementItemRequest it : group) {
                if (it.getReason() != null && it.getReason().startsWith("AMBIGUOUS:")) {
                    continue;
                }
                String candidates = ("C".equals(it.getCategory()) || "D".equals(it.getCategory()))
                        ? "对侧同额候选 flow_id=" + intRefs
                        : "对侧同额候选 transaction_id=" + extRefs;
                it.setReason("AMBIGUOUS: 金额 " + it.getAmount() + " 分，" + candidates
                        + "；原挂账原因：" + (it.getReason() == null ? "-" : it.getReason()));
                marked++;
            }
        }
        return marked;
    }

    /**
     * 人工处置歧义（BL-6 §3）：**不自动选**，由人指定 decision + 对侧 targetRef。
     *
     * <p>校验（同一事务内）：
     * ① item 必须属于该 statement 且 reason 以 AMBIGUOUS: 开头；
     * ② decision 必须是对侧类别（A↔C、B↔D），不能选自己这一侧；
     * ③ targetRef 必须能在本表内找到同类别、同金额的对侧条目（A 对应 transaction_id，C 对应 flow_id）。</p>
     *
     * <p>处置后：条目的 AMBIGUOUS 前缀去掉、追加处置说明，并重算 statement 的 is_balanced
     * （歧义清零且金额相等 → 回到 true，可继续 submit）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationStatementVO resolveAmbiguity(Long statementId, Long itemId, String decision,
                                                      String targetRef, String reason) {
        if (decision == null || decision.isBlank()) {
            throw err("decision 必填（A/B/C/D，且必须是对侧类别）");
        }
        String dec = decision.trim().toUpperCase();
        if (!java.util.Set.of("A", "B", "C", "D").contains(dec)) {
            throw err("decision 只能是 A/B/C/D，收到：" + decision);
        }
        if (targetRef == null || targetRef.isBlank()) {
            throw err("targetRef 必填（对侧条目的 flow_id 或 transaction_id）");
        }
        if (reason == null || reason.isBlank()) {
            throw err("处置原因不能为空");
        }

        java.util.List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "SELECT item_id, statement_id, category, flow_id, transaction_id, amount, reason "
                        + "  FROM reconciliation_items WHERE item_id = ? AND deleted = 0 FOR UPDATE", itemId);
        if (rows.isEmpty()) {
            throw err("未达账项不存在：" + itemId);
        }
        java.util.Map<String, Object> item = rows.get(0);
        long sid = ((Number) item.get("statement_id")).longValue();
        if (sid != statementId.longValue()) {
            throw err("该 item 不属于调节表 " + statementId + "（实际属于 " + sid + "）");
        }
        String cat = String.valueOf(item.get("category"));
        String itemReason = item.get("reason") == null ? "" : String.valueOf(item.get("reason"));
        if (!itemReason.startsWith("AMBIGUOUS:")) {
            throw err("该 item 没有同额歧义标记（reason 不以 AMBIGUOUS: 开头），无需处置：" + cat + " #" + itemId);
        }
        String opposite = switch (cat) {
            case "A" -> "C";
            case "B" -> "D";
            case "C" -> "A";
            case "D" -> "B";
            default -> throw err("未知类别：" + cat);
        };
        if (!opposite.equals(dec)) {
            throw err("decision 必须是对侧类别 " + opposite + "（当前 item 类别 " + cat + "），收到：" + decision);
        }
        long amount = ((Number) item.get("amount")).longValue();
        // 对侧候选必须存在且同额
        // 对侧是 C/D（银行侧）时比对 transaction_id；对侧是 A/B（企业侧）时比对 flow_id
        Integer cand = jdbc.queryForObject(
                "SELECT count(*) FROM reconciliation_items WHERE statement_id = ? AND deleted = 0 "
                        + " AND category = ? AND amount = ? "
                        + " AND (CASE WHEN ? IN ('C','D') THEN transaction_id ELSE flow_id END)::text = ?",
                Integer.class, statementId, dec, amount, dec, targetRef.trim());
        if (cand == null || cand == 0) {
            throw err("对侧候选不存在或金额不一致：category=" + dec + "，amount=" + amount
                    + " 分，targetRef=" + targetRef);
        }

        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        String userName = currentUserName();
        String cleaned = itemReason.startsWith("AMBIGUOUS:") ? itemReason.substring("AMBIGUOUS:".length()).trim() : itemReason;
        String newReason = "已处置(AMBIGUITY->" + dec + ":" + targetRef.trim() + ")：原因=" + reason
                + "；" + cleaned;
        jdbc.update("UPDATE reconciliation_items SET reason = ?, update_time = ? WHERE item_id = ?",
                newReason, now, itemId);

        // 双事务校验：处置后重算歧义数与 is_balanced（同一事务内写入，失败整体回滚）
        reconcileStatementFlags(statementId, userId, userName, now);

        // 审计留痕
        try {
            jdbc.update("INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module,"
                            + " target_id, target_type, operation_desc, operation_data, create_time, created_by, version) "
                            + "VALUES (?,?,'RECON_AMBIGUITY_RESOLVED','finance',?,'RECON_ITEM',?,?,?,?,0)",
                    userId, userName, itemId,
                    "调节表 " + statementId + " 歧义处置：item=" + itemId + " 类别=" + cat + " → " + dec
                            + "，targetRef=" + targetRef + "，原因=" + reason,
                    "{\"statementId\":" + statementId + ",\"itemId\":" + itemId + ",\"decision\":\"" + dec
                            + "\",\"targetRef\":\"" + targetRef.trim() + "\",\"reason\":\""
                            + reason.replace("\"", "'") + "\"}",
                    now, userName);
        } catch (Exception e) {
            log.warn("歧义处置审计写入失败（不影响处置结果）: itemId={}, err={}", itemId, e.getMessage());
        }
        return detail(statementId);
    }

    /**
     * 同额两侧并存判据（**与 consistency_check A19 同口径**）：
     * 同一 statement 内，若某金额同时出现在内部侧 (A|B) 与外部侧 (C|D)，即为「形态假平衡」。
     *
     * <p>与 {@link #markAmbiguities} 的关系：markAmbiguities 依赖 reason 前缀（可被重算冲掉、
     * 也可能被人为改写），本方法只看数据形态，因此是**兜底**；两者都进 is_balanced 判定，
     * 任一为真即 false。A19 在 consistency_check 里按同样口径做持续巡检（>0 直接 FAIL）。</p>
     */
    private boolean hasSameAmountOnBothSides(List<StatementItemRequest> items) {
        java.util.Map<Long, int[]> byAmount = new java.util.HashMap<>();
        for (StatementItemRequest it : items) {
            if (it.getAmount() == null) {
                continue;
            }
            int[] sides = byAmount.computeIfAbsent(it.getAmount(), k -> new int[2]);
            if ("C".equals(it.getCategory()) || "D".equals(it.getCategory())) {
                sides[1]++;
            } else {
                sides[0]++;
            }
        }
        for (int[] sides : byAmount.values()) {
            if (sides[0] > 0 && sides[1] > 0) {
                return true;
            }
        }
        return false;
    }

    /** 重算 statement 的歧义数与 is_balanced（歧义清零 + 金额相等 才算平衡） */
    private void reconcileStatementFlags(Long statementId, Long userId, String userName, LocalDateTime now) {
        Integer ambiguous = jdbc.queryForObject(
                "SELECT count(*) FROM reconciliation_items WHERE statement_id = ? AND deleted = 0 "
                        + " AND reason LIKE 'AMBIGUOUS:%'", Integer.class, statementId);
        java.util.Map<String, Object> row = oneOrNull(
                "SELECT adjusted_book_balance, adjusted_bank_balance FROM reconciliation_statements WHERE statement_id = ?",
                statementId);
        if (row == null) {
            throw err("调节表不存在：" + statementId);
        }
        long adjBook = ((Number) row.get("adjusted_book_balance")).longValue();
        long adjBank = ((Number) row.get("adjusted_bank_balance")).longValue();
        boolean balanced = adjBook == adjBank && (ambiguous == null || ambiguous == 0);
        jdbc.update("UPDATE reconciliation_statements SET is_balanced = ?, update_time = ?, version = version + 1 "
                + " WHERE statement_id = ?", balanced, now, statementId);
        log.info("[RECON_AMBIGUITY] statement={} 剩余歧义={} 金额相等={} is_balanced={}",
                statementId, ambiguous, adjBook == adjBank, balanced);
    }

    private void validateItems(List<StatementItemRequest> items, LocalDate ps, LocalDate pe) {
        Set<String> seen = new HashSet<>();
        for (StatementItemRequest it : items) {
            if (it.getReason() == null || it.getReason().isBlank()) {
                throw err("每条未达账项必须有原因（V6）");
            }
            if (it.getOwnerUserId() == null) {
                it.setOwnerUserId(currentUserId());
            }
            String key = it.getFlowId() != null ? ("F" + it.getFlowId()) : ("T" + it.getTransactionId());
            if (!seen.add(key)) {
                throw err("V7 门禁失败：同一笔未达账项在本期内重复挂账（" + key + "）");
            }
            // 同期间已在别的调节表挂过 → 拒绝（不得跨表重复挂账）
            Integer dup = jdbc.queryForObject(
                    "SELECT count(*) FROM reconciliation_items WHERE deleted = 0 AND period_start = ? AND period_end = ?"
                            + " AND ((flow_id IS NOT NULL AND flow_id = ?) OR (transaction_id IS NOT NULL AND transaction_id = ?))"
                            + " AND statement_id IN (SELECT statement_id FROM reconciliation_statements"
                            + "   WHERE period_start = ? AND period_end = ? AND deleted = 0 AND status <> 'DRAFT')",
                    Integer.class, ps, pe, it.getFlowId(), it.getTransactionId(), ps, pe);
            if (dup != null && dup > 0) {
                throw err("V7 门禁失败：该笔未达账项已被同期间其它调节表挂账（" + key + "）");
            }
        }
    }

    // ==================================================================
    // 内部：通用
    // ==================================================================

    private final RowMapper<ReconciliationStatementVO> STATEMENT_MAPPER = (rs, i) -> {
        ReconciliationStatementVO vo = new ReconciliationStatementVO();
        vo.statementId = rs.getLong("statement_id");
        vo.statementNo = rs.getString("statement_no");
        vo.accountId = rs.getLong("account_id");
        vo.storeId = rs.getObject("store_id") == null ? null : rs.getLong("store_id");
        vo.periodStart = rs.getDate("period_start") == null ? null : rs.getDate("period_start").toLocalDate();
        vo.periodEnd = rs.getDate("period_end") == null ? null : rs.getDate("period_end").toLocalDate();
        vo.bookBalance = rs.getLong("book_balance");
        vo.bankBalance = rs.getLong("bank_balance");
        vo.unclearedInCredit = rs.getLong("uncleared_in_credit");
        vo.unclearedInDebit = rs.getLong("uncleared_in_debit");
        vo.unclearedOutCredit = rs.getLong("uncleared_out_credit");
        vo.unclearedOutDebit = rs.getLong("uncleared_out_debit");
        vo.adjustedBookBalance = rs.getLong("adjusted_book_balance");
        vo.adjustedBankBalance = rs.getLong("adjusted_bank_balance");
        vo.isBalanced = rs.getBoolean("is_balanced");
        vo.status = rs.getString("status");
        vo.submittedBy = rs.getObject("submitted_by") == null ? null : rs.getLong("submitted_by");
        vo.submittedAt = rs.getObject("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toLocalDateTime();
        vo.approvedBy = rs.getObject("approved_by") == null ? null : rs.getLong("approved_by");
        vo.approvedAt = rs.getObject("approved_at") == null ? null : rs.getTimestamp("approved_at").toLocalDateTime();
        vo.remark = rs.getString("remark");
        vo.createUserId = rs.getObject("create_user_id") == null ? null : rs.getLong("create_user_id");
        vo.createTime = rs.getObject("create_time") == null ? null : rs.getTimestamp("create_time").toLocalDateTime();
        return vo;
    };

    /** 期间末最后一笔非豁免 balance_after（A7 口径） */
    private Long lastBookBalance(Long accountId, LocalDate pe) {
        List<Long> list = jdbc.queryForList(
                "SELECT ff.balance_after FROM fund_flows ff "
                        + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.status = 2 AND ff.business_date <= ? "
                        + NON_EXEMPT
                        + " ORDER BY ff.business_date DESC, ff.flow_id DESC LIMIT 1",
                Long.class, accountId, pe);
        return list.isEmpty() ? null : list.get(0);
    }

    private String nextStatementNo(LocalDate periodEnd) {
        String prefix = "BS" + periodEnd.format(DateTimeFormatter.ofPattern("yyyyMM"));
        Long cnt = jdbc.queryForObject(
                "SELECT count(*) FROM reconciliation_statements WHERE statement_no LIKE ?",
                Long.class, prefix + "%");
        return prefix + String.format("%04d", (cnt == null ? 0 : cnt) + 1);
    }

    private List<String> userRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jdbc.queryForList(
                "SELECT DISTINCT r.role_id_str FROM user_roles ur "
                        + " JOIN roles r ON r.role_id_str = ur.role_id AND r.deleted = 0 "
                        + " WHERE ur.user_id = ? AND COALESCE(ur.deleted, 0) = 0",
                String.class, String.valueOf(userId));
    }

    private String lookupUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        List<String> names = jdbc.queryForList(
                "SELECT username FROM users WHERE user_id = ? AND deleted = 0", String.class, userId);
        return names.isEmpty() ? null : names.get(0);
    }

    private Map<String, Object> oneOrNull(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static long sum(List<StatementItemRequest> items, String category) {
        return items.stream().filter(Objects::nonNull)
                .filter(i -> category.equals(i.getCategory()))
                .mapToLong(i -> i.getAmount() == null ? 0 : i.getAmount())
                .sum();
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    /**
     * 业务异常助手：用 **common** 包的错误码构造器（code=400），
     * GlobalExceptionHandler 才会把 message 原样回给前端；否则会被当成 500「系统繁忙」把文案吞掉。
     */
    private static BusinessException err(String msg) {
        return new BusinessException(400, msg);
    }

    private Long currentUserId() {
        try {
            Long id = SecurityUtils.getCurrentUserId();
            return id == null ? 0L : id;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String currentUserName() {
        try {
            String n = SecurityUtils.getCurrentUsername();
            return n == null || n.isBlank() ? "system" : n;
        } catch (Exception e) {
            return "system";
        }
    }
}
