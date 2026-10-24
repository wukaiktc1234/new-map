package com.foodtraceability.service.finance.recon.importing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 银行/微信/支付宝 CSV 解析器（P0-B，**只解析，不落库**）。
 *
 * <p>职责边界（design §1.2 / §1.3 / §1.4）：</p>
 * <ul>
 *   <li>编码自适应（{@link CsvEncoding}）；表头可出现在前 1–5 行；列名走别名映射；</li>
 *   <li>金额一律走 {@link MoneyYuanToFen}（元→分唯一实现）；行级错误给 errorCode，不阻断其他行；</li>
 *   <li>方向判定：微信/支付宝按「收/支」列或金额正负；银行按借贷标志/借贷金额列或正负；判不出 → DIRECTION_UNKNOWN；</li>
 *   <li>幂等分流：批内精确键重复 → DUPLICATE（跳过）；退化键（时间+金额+对方账号）重复 → NEEDS_REVIEW（落库待人工）；</li>
 *   <li>必需列缺失 → 整批 PARSED_FAILED（不产出任何行）。</li>
 * </ul>
 *
 * <p>本类不依赖 MyBatis/Spring，方便先用样例文件自测；落库由 P1 的 ImportService 负责。</p>
 */
@org.springframework.stereotype.Component
public final class CsvImportParser {

    /** 来源类型（与 design §1.1 一致） */
    public enum SourceType { WECHAT, ALIPAY, BANK }

    /** 行状态（与 import_rows.status 对齐） */
    public enum RowStatus { OK, DUPLICATE, NEEDS_REVIEW, ERROR }

    /** 批次状态（与 import_batches.status 对齐） */
    public enum BatchStatus { PARSED_OK, PARSED_FAILED }

    /** 去重依据（与 import_rows.dedupe_basis 对齐） */
    public enum DedupeBasis { EXTERNAL_ID, COMPOSITE }

    // ---- 行级错误码 ----
    public static final String ERR_MISSING_TXN_TIME = "MISSING_TXN_TIME";
    public static final String ERR_MISSING_EXTERNAL_ID = "MISSING_EXTERNAL_ID";
    public static final String ERR_TXN_TIME_INVALID = "TXN_TIME_INVALID";
    public static final String ERR_FUTURE_TIME = "FUTURE_TIME";
    public static final String ERR_DIRECTION_UNKNOWN = "DIRECTION_UNKNOWN";
    public static final String ERR_FILE_HEADER = "HEADER_MISSING";
    public static final String ERR_ENCODING = "ENCODING_UNSUPPORTED";

    // ---- 内部语义 → 列别名（design §1.1 + 附录 A）----
    private static final Map<String, String[]> ALIASES = new LinkedHashMap<>();

    static {
        // txn_time
        ALIASES.put("txn_time", new String[]{"交易时间", "交易日期时间", "成交时间", "完成时间", "创建时间", "交易日期", "时间", "txn_time"});
        // external_txn_id
        ALIASES.put("external_txn_id", new String[]{"微信订单号", "支付宝交易号", "交易流水号", "交易单号", "流水号", "交易号", "external_txn_id"});
        // merchant_order_no
        ALIASES.put("merchant_order_no", new String[]{"商户订单号", "商家订单号", "商户单号", "merchant_order_no"});
        // amount（元）
        ALIASES.put("amount", new String[]{"应结订单金额", "订单金额（元）", "订单金额", "商家实收（元）", "发生额", "交易金额", "金额（元）", "金额", "amount"});
        // fee（元）
        ALIASES.put("fee", new String[]{"手续费", "服务费（元）", "服务费", "渠道费", "fee"});
        // direction（文本）
        ALIASES.put("direction", new String[]{"收/支", "收支", "借贷标志", "交易类型", "方向", "direction"});
        // 银行借贷分列
        ALIASES.put("debit", new String[]{"借方金额", "借方发生额", "debit"});
        ALIASES.put("credit", new String[]{"贷方金额", "贷方发生额", "credit"});
        // summary
        ALIASES.put("summary", new String[]{"商品名称", "摘要", "用途", "备注", "摘要/用途", "summary"});
        // counterparty
        ALIASES.put("counterparty_account", new String[]{"对方账号", "对方账户", "对手账号", "counterparty_account"});
        ALIASES.put("counterparty_name", new String[]{"对方户名", "对方名称", "counterparty_name"});
    }

    /** 每行解析结果 */
    public record ParsedRow(int rowNo, String externalTxnId, String merchantOrderNo, LocalDateTime txnTime,
                            Long amountFen, Long feeFen, Integer direction, String summary,
                            String counterpartyAccount, DedupeBasis dedupeBasis,
                            RowStatus status, String errorCode, String errorMessage, String rawHash) {
    }

    /** 整批结果 */
    public record BatchResult(BatchStatus status, String encodingUsed, int totalRows, int okRows,
                              int dupRows, int reviewRows, int errorRows, String errorSummary,
                              List<ParsedRow> rows) {
        public int committedRows() {
            return okRows + reviewRows;
        }
    }

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
    };

    /**
     * 解析一份 CSV 字节流。
     *
     * @param bytes      文件字节（编码由本类判定）
     * @param sourceType 来源（决定方向判定策略）
     * @param knownKeys  库内已存在的键（用于「与库内去重」；批内去重由本类自己维护）
     */
    public BatchResult parse(byte[] bytes, SourceType sourceType, Set<String> knownKeys) {
        CsvEncoding.Decoded decoded = CsvEncoding.decode(bytes);
        List<ParsedRow> rows = new ArrayList<>();
        if ("UNKNOWN".equals(decoded.encodingUsed())) {
            return new BatchResult(BatchStatus.PARSED_FAILED, decoded.encodingUsed(), 0, 0, 0, 0, 0,
                    ERR_ENCODING + ": 文件既不是 UTF-8 也不是 GB18030", List.of());
        }
        List<String> lines = new ArrayList<>();
        for (String l : decoded.text().split("\\r\\n|\\n|\\r", -1)) {
            if (!l.isBlank()) {
                lines.add(l);
            }
        }
        // 表头可出现在前 1–5 行
        int headerIdx = -1;
        Map<String, Integer> header = null;
        for (int i = 0; i < Math.min(5, lines.size()); i++) {
            Map<String, Integer> h = mapHeader(splitCsv(lines.get(i)));
            // 银行导出用「借方金额/贷方金额」分列代替「发生额/金额」，两者任一即视为有金额列
            boolean hasAmountCol = h.containsKey("amount") || h.containsKey("debit") || h.containsKey("credit");
            if (h.containsKey("txn_time") && hasAmountCol) {
                headerIdx = i;
                header = h;
                break;
            }
        }
        if (header == null) {
            return new BatchResult(BatchStatus.PARSED_FAILED, decoded.encodingUsed(), 0, 0, 0, 0, 0,
                    ERR_FILE_HEADER + ": 前 5 行内找不到同时包含「交易时间」与「金额」的必需列", List.of());
        }
        // 必需列：txn_time / (amount 或 借贷分列) / external_txn_id（design §1.3 缺任一 → 整批 FAILED）
        if (!header.containsKey("amount") && !header.containsKey("debit") && !header.containsKey("credit")) {
            return new BatchResult(BatchStatus.PARSED_FAILED, decoded.encodingUsed(), 0, 0, 0, 0, 0,
                    ERR_FILE_HEADER + ": 缺少必需列「金额/发生额/借方金额/贷方金额」", List.of());
        }
        if (!header.containsKey("external_txn_id")) {
            return new BatchResult(BatchStatus.PARSED_FAILED, decoded.encodingUsed(), 0, 0, 0, 0, 0,
                    ERR_FILE_HEADER + ": 缺少必需列「交易流水号/微信订单号/支付宝交易号」", List.of());
        }

        Set<String> seenExact = new HashSet<>();
        Set<String> seenComposite = new HashSet<>();
        Set<String> known = knownKeys == null ? Set.of() : knownKeys;
        int ok = 0, dup = 0, review = 0, err = 0;
        StringBuilder errSummary = new StringBuilder();

        int headerCols = splitCsv(lines.get(headerIdx)).length;
        for (int i = headerIdx + 1; i < lines.size(); i++) {
            String[] cells = splitCsv(lines.get(i));
            int rowNo = i - headerIdx; // 数据行号：1 起
            String rawHash = sha256(lines.get(i));
            ParsedRow row;
            if (cells.length > headerCols) {
                // 列数比表头多 → 一律判 AMOUNT_THOUSANDS_SEP（**不是**普通格式错误，必须专门拦）。
                // 场景示例（千分位逗号未加引号，被 CSV 切成两列）：
                //   原始行: 2026-10-24,09:00:00,E1,1,234.56,,1000.00,A,,千分位,转出
                //   表头  : 交易日期,交易时间,交易流水号,借方金额,贷方金额,余额,对方户名,对方账号,摘要,交易类型  (10 列)
                //   实际  : 切出 11 列 → 「借方金额」拿到 "1"（1 元），"234.56" 挤进「贷方金额」，
                //           若照列位硬取，会把 1234.56 元的付款记成「1 元出 + 234.56 元入」，账直接失真。
                // 因此这里不猜金额、不猜方向，直接行级 ERROR 交人工；导出方给金额加引号（"1,234.56"）后
                // 本分支不会触发，而由 MoneyYuanToFen 按千分位规则正常拒绝。
                row = errorRow(rowNo, cell(cells, header, "external_txn_id"), cell(cells, header, "merchant_order_no"),
                        null, null, null, cell(cells, header, "summary"), cell(cells, header, "counterparty_account"),
                        rawHash, MoneyYuanToFen.ERR_THOUSANDS_SEP,
                        "列数与表头不一致（" + cells.length + " > " + headerCols + "），疑似金额含千分位逗号");
            } else {
                row = parseRow(rowNo, cells, header, sourceType, rawHash);
            }
            if (row.status() == RowStatus.ERROR) {
                err++;
                if (errSummary.length() < 500) {
                    errSummary.append("row").append(rowNo).append('=').append(row.errorCode()).append(' ');
                }
                rows.add(row);
                continue;
            }
            // 去重分流（design §1.4）
            boolean exactHit = row.externalTxnId() != null && !row.externalTxnId().isBlank();
            String exactKey = exactHit ? (sourceType.name() + "|" + row.externalTxnId()) : null;
            String compositeKey = sourceType.name() + "|" + row.txnTime() + "|" + row.amountFen() + "|"
                    + (row.counterpartyAccount() == null ? "" : row.counterpartyAccount());

            if (exactHit && (seenExact.contains(exactKey) || known.contains(exactKey))) {
                dup++;
                rows.add(withStatus(row, RowStatus.DUPLICATE, null));
            } else if (!exactHit && (seenComposite.contains(compositeKey) || known.contains(compositeKey))) {
                review++;
                rows.add(withStatus(row, RowStatus.NEEDS_REVIEW, null));
            } else {
                ok++;
                rows.add(withStatus(row, RowStatus.OK, null));
            }
            if (exactHit) {
                seenExact.add(exactKey);
            }
            seenComposite.add(compositeKey);
        }

        // 行级失败不阻断（design §1.2 硬规则 4）：只要文件级检查（编码/表头/必需列）通过，批次一律 PARSED_OK，
        // 行级错误体现在 error_rows 与 import_rows.status=ERROR 上，由 commit 阶段决定 COMMITTED / PARTIAL。
        BatchStatus status = BatchStatus.PARSED_OK;
        return new BatchResult(status, decoded.encodingUsed(), rows.size(), ok, dup, review, err,
                errSummary.toString().trim(), rows);
    }

    private ParsedRow withStatus(ParsedRow r, RowStatus s, String msg) {
        return new ParsedRow(r.rowNo(), r.externalTxnId(), r.merchantOrderNo(), r.txnTime(), r.amountFen(),
                r.feeFen(), r.direction(), r.summary(), r.counterpartyAccount(), r.dedupeBasis(), s,
                r.errorCode(), msg == null ? r.errorMessage() : msg, r.rawHash());
    }

    private ParsedRow parseRow(int rowNo, String[] cells, Map<String, Integer> header, SourceType type, String rawHash) {
        String external = cell(cells, header, "external_txn_id");
        String merchantNo = cell(cells, header, "merchant_order_no");
        String timeRaw = cell(cells, header, "txn_time");
        String amountRaw = cell(cells, header, "amount");
        String feeRaw = cell(cells, header, "fee");
        String dirRaw = cell(cells, header, "direction");
        String summary = cell(cells, header, "summary");
        String cpAccount = cell(cells, header, "counterparty_account");

        // 银行借贷分列：借方=出账(direction=2)，贷方=进账(direction=1)
        String debitRaw = cell(cells, header, "debit");
        String creditRaw = cell(cells, header, "credit");

        if (timeRaw == null || timeRaw.isBlank()) {
            return errorRow(rowNo, external, merchantNo, null, null, null, summary, cpAccount, rawHash, ERR_MISSING_TXN_TIME, "交易时间缺失");
        }
        LocalDateTime txnTime = parseTime(timeRaw);
        if (txnTime == null) {
            return errorRow(rowNo, external, merchantNo, null, null, null, summary, cpAccount, rawHash, ERR_TXN_TIME_INVALID, "时间格式无法识别: " + timeRaw);
        }
        if (txnTime.isAfter(LocalDateTime.now().plusDays(1))) {
            return errorRow(rowNo, external, merchantNo, txnTime, null, null, summary, cpAccount, rawHash, ERR_FUTURE_TIME, "未来时间: " + timeRaw);
        }
        // 金额：优先「发生额/金额」；银行若只有借贷分列，取非空那列
        // 注意：debit/credit 的「是否有值」独立判定——两者同时有值属于方向歧义，必须行级报错，不能取其一糊过去
        boolean hasDebitCell = debitRaw != null && !debitRaw.isBlank();
        boolean hasCreditCell = creditRaw != null && !creditRaw.isBlank();
        String amountCell = amountRaw;
        boolean debit = hasDebitCell && !hasCreditCell;
        boolean credit = hasCreditCell && !hasDebitCell;
        if ((amountCell == null || amountCell.isBlank())) {
            if (hasCreditCell) {
                amountCell = creditRaw;
            } else if (hasDebitCell) {
                amountCell = debitRaw;
            }
        }
        MoneyYuanToFen.Result amount = MoneyYuanToFen.toFen(amountCell);
        if (!amount.ok()) {
            return errorRow(rowNo, external, merchantNo, txnTime, null, null, summary, cpAccount, rawHash, amount.errorCode(), "金额无法换算: " + amountCell);
        }
        Long fee = null;
        if (feeRaw != null && !feeRaw.isBlank()) {
            MoneyYuanToFen.Result f = MoneyYuanToFen.toFen(feeRaw);
            if (f.ok()) {
                fee = f.fen();
            }
        }
        Integer direction = resolveDirection(type, dirRaw, amountCell, hasDebitCell, hasCreditCell);
        if (direction == null) {
            return errorRow(rowNo, external, merchantNo, txnTime, amount.fen(), fee, summary, cpAccount, rawHash, ERR_DIRECTION_UNKNOWN, "无法判定收/支: " + dirRaw);
        }
        DedupeBasis basis = (external != null && !external.isBlank()) ? DedupeBasis.EXTERNAL_ID : DedupeBasis.COMPOSITE;
        return new ParsedRow(rowNo, external, merchantNo, txnTime, amount.fen(), fee, direction, summary,
                cpAccount, basis, RowStatus.OK, null, null, rawHash);
    }

    /**
     * 方向判定优先级：借贷分列同时有值 → 无法判定（DIRECTION_UNKNOWN）；
     * 其次文本列（收/支、借贷标志、交易类型）；再次借贷分列；
     * 最后：银行按金额正负，微信/支付宝按「渠道对账单默认收款」兜底（金额非负=收入，负数=支出）。
     */
    private Integer resolveDirection(SourceType type, String dirRaw, String amountCell, boolean debit, boolean credit) {
        // 借方、贷方同时有值 = 方向歧义，绝不猜
        if (debit && credit) {
            return null;
        }
        if (dirRaw != null && !dirRaw.isBlank()) {
            String d = dirRaw.trim();
            if (d.contains("收入") || d.equals("收") || d.contains("进账") || d.contains("转入") || d.contains("贷")) {
                return 1;
            }
            if (d.contains("支出") || d.equals("支") || d.contains("出账") || d.contains("转出") || d.contains("借")) {
                return 2;
            }
            if (d.contains("退款") || d.contains("REFUND")) {
                return 1; // 退款视为资金回流，方向仍为收入侧（design §1.3）
            }
        }
        if (credit) {
            return 1;
        }
        if (debit) {
            return 2;
        }
        if (amountCell != null) {
            String t = amountCell.replace("¥", "").replace("￥", "").replace(",", "").trim();
            if (t.startsWith("-")) {
                return 2;
            }
            if (t.startsWith("+")) {
                return 1;
            }
        }
        // 微信/支付宝对账单：商户视角默认「收款」，金额恒正且无收/支列 → 收入；
        // 负数已在上面判为支出；银行对账单没有借贷标志/正负号时**不猜**（保持 DIRECTION_UNKNOWN）。
        if (type == SourceType.WECHAT || type == SourceType.ALIPAY) {
            return 1;
        }
        return null;
    }

    private ParsedRow errorRow(int rowNo, String ext, String merchant, LocalDateTime t, Long amount, Long fee,
                               String summary, String cpAccount, String rawHash, String code, String msg) {
        return new ParsedRow(rowNo, ext, merchant, t, amount, fee, null, summary, cpAccount,
                DedupeBasis.COMPOSITE, RowStatus.ERROR, code, msg, rawHash);
    }

    private LocalDateTime parseTime(String raw) {
        String s = raw.trim().replace('T', ' ');
        for (DateTimeFormatter f : TIME_FORMATS) {
            try {
                if (f.toString().contains("yyyyMMddHHmmss")) {
                    return LocalDateTime.parse(s, f);
                }
                return LocalDateTime.parse(s, f);
            } catch (Exception ignored) {
                // try next
            }
        }
        // 只有日期：按 00:00 解释
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Integer> mapHeader(String[] cells) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < cells.length; i++) {
            String name = cells[i] == null ? "" : cells[i].trim().replace("\uFEFF", "");
            if (name.isEmpty()) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String[]> e : ALIASES.entrySet()) {
                if (map.containsKey(e.getKey())) {
                    continue;
                }
                for (String alias : e.getValue()) {
                    if (name.equals(alias) || lower.equals(alias.toLowerCase(Locale.ROOT))) {
                        map.put(e.getKey(), i);
                        break;
                    }
                }
            }
        }
        return map;
    }

    private String cell(String[] cells, Map<String, Integer> header, String field) {
        Integer idx = header.get(field);
        if (idx == null || idx >= cells.length) {
            return null;
        }
        String v = cells[idx];
        return v == null ? null : v.trim();
    }

    /** 极简 CSV 切分：支持双引号包裹与 "" 转义（渠道导出的标准形态）。public 供 ImportService/自检复用。 */
    public static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuote = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
