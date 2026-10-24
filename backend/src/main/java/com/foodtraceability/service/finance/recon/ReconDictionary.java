package com.foodtraceability.service.finance.recon;

import java.util.Locale;
import java.util.Set;

/**
 * 对账字典与口径常量（P1-B）。
 *
 * <p>状态 / 类型 / 决议都是**枚举 code**（设计 §2.3–2.5），中文名由前端字典渲染，不进库。</p>
 *
 * <p>差异状态新增值 {@link #DIFF_PENDING_P2_REVERSE}（PENDING_P2_REVERSE）：
 * 仅用于 {@code recon_differences.status}，表示「冲正决议已记录、执行留给 P2」；
 * 本阶段不生成任何反向流水、不写 fund_flows（TD-28 留 P2）。</p>
 */
public final class ReconDictionary {

    private ReconDictionary() {
    }

    /* ==================== run 状态 ==================== */
    public static final String RUN_RUNNING = "RUNNING";
    public static final String RUN_FINALIZED = "FINALIZED";
    public static final String RUN_FAILED = "FAILED";

    /* ==================== 主体侧 ==================== */
    public static final String SIDE_INTERNAL = "INTERNAL";
    public static final String SIDE_EXTERNAL = "EXTERNAL";

    /* ==================== 主体状态（§2.3） ==================== */
    public static final String ITEM_UNMATCHED = "UNMATCHED";
    public static final String ITEM_MATCHED = "MATCHED";
    public static final String ITEM_AUTO_MATCHED = "AUTO_MATCHED";
    public static final String ITEM_MANUAL_MATCHED = "MANUAL_MATCHED";
    public static final String ITEM_EXCEPTION = "EXCEPTION";

    /* ==================== 匹配级别 ==================== */
    public static final String LEVEL_L1 = "L1";
    public static final String LEVEL_L2 = "L2";
    public static final String LEVEL_MANUAL = "MANUAL";

    /* ==================== 差异类型（§2.4） ==================== */
    public static final String DIFF_AMOUNT = "AMOUNT_DIFF";
    public static final String DIFF_TIME = "TIME_DIFF";
    public static final String DIFF_ONE_SIDED_INTERNAL = "ONE_SIDED_INTERNAL";
    public static final String DIFF_ONE_SIDED_EXTERNAL = "ONE_SIDED_EXTERNAL";
    public static final String DIFF_DUPLICATE = "DUPLICATE";
    public static final String DIFF_CHANNEL_FEE = "CHANNEL_FEE";
    public static final String DIFF_AMBIGUOUS = "AMBIGUOUS";

    /* ==================== 差异状态 ==================== */
    public static final String DIFF_OPEN = "OPEN";
    public static final String DIFF_ASSIGNED = "ASSIGNED";
    public static final String DIFF_RESOLVING = "RESOLVING";
    public static final String DIFF_CLOSED = "CLOSED";
    public static final String DIFF_EXEMPT = "EXEMPT";
    /**
     * 冲正决议已记录，等 P2 执行反向流水（本阶段不生成任何流水）。
     *
     * <p><b>口径说明</b>：设计稿写的是 {@code PENDING_P2_REVERSE}（18 字符），但
     * {@code recon_differences.status} 是 {@code varchar(16)}（§5.6 结构被 P1 范围锁冻结，不得改列宽），
     * 18 字符会直接报 {@code value too long}。因此落库值取等长语义的 {@code PENDING_P2_REV}（14 字符），
     * 权威标记仍由 {@code resolution='REVERSE'} 承担；该状态只在差异表内使用，不扩散到其它实体。</p>
     */
    public static final String DIFF_PENDING_P2_REVERSE = "PENDING_P2_REV";

    /* ==================== 差异决议 ==================== */
    public static final String RES_WRITE_OFF = "WRITE_OFF";
    public static final String RES_EXEMPT = "EXEMPT";
    public static final String RES_REVERSE = "REVERSE";


    private static final Set<String> RESOLUTIONS = Set.of(RES_WRITE_OFF, RES_EXEMPT, RES_REVERSE);

    /** 被外部流水号前缀污染的场景：WX:4200001 / ALI:xxx / CASH:xxx 之类 */
    private static final String[] REF_PREFIXES = {"WX:", "WX_", "ALI:", "ALI_", "CASH:", "CSH:", "PAY:", "PAY_"};

    /** 是否合法决议 */
    public static boolean isValidResolution(String resolution) {
        return resolution != null && RESOLUTIONS.contains(resolution);
    }

    /**
     * 外部流水号去前缀归一（L1 精确匹配用）。
     * 例：{@code WX:4200001} → {@code 4200001}；已是裸号则原样返回（大写化 + 去空白）。
     */
    public static String normalizeRef(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return v;
        }
        String upper = v.toUpperCase(Locale.ROOT);
        for (String prefix : REF_PREFIXES) {
            if (upper.startsWith(prefix)) {
                return v.substring(prefix.length()).trim();
            }
        }
        return v;
    }
}
