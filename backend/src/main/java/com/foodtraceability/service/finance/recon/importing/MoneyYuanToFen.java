package com.foodtraceability.service.finance.recon.importing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * 金额换算的**唯一实现**（P0-B 硬规则：元 → 分只允许在这里做）。
 *
 * <p>规则（design §1.3）：</p>
 * <ul>
 *   <li>CSV 里的元值 × 100 后**必须是整数**（分），用 {@link BigDecimal}，禁止 double/float；</li>
 *   <li>出现千分位逗号、全角数字、3 位及以上小数 → 行级 ERROR（返回 errorCode，不给金额）；</li>
 *   <li>入参允许带货币符号（¥/￥/$）与首尾空格，其余字符一律拒绝。</li>
 * </ul>
 *
 * <p>任何新代码要做元↔分换算都必须调用本类，不得自行 *100 / /100。</p>
 */
public final class MoneyYuanToFen {

    /** 行级错误码（与 import_rows.error_code 对齐） */
    public static final String ERR_EMPTY = "AMOUNT_EMPTY";
    public static final String ERR_THOUSANDS_SEP = "AMOUNT_THOUSANDS_SEP";
    public static final String ERR_NON_ASCII_DIGIT = "AMOUNT_NON_ASCII_DIGIT";
    public static final String ERR_DECIMAL_INVALID = "AMOUNT_DECIMAL_INVALID";
    public static final String ERR_NOT_NUMERIC = "AMOUNT_NOT_NUMERIC";

    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d{1,2})?$");

    private MoneyYuanToFen() {
    }

    /**
     * 元 → 分（BigDecimal 版；DB numeric(10,2) 直接换算，禁止调用方自行 ×100）。
     *
     * @param yuan 元值（可空 → 返回 null）
     * @return 分；元值小数位超过 2 位时抛 IllegalArgumentException（数据问题不得静默四舍五入）
     */
    public static Long toFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        BigDecimal fen = yuan.movePointRight(2);
        if (fen.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("元值小数位超过 2 位，无法精确换算为分: " + yuan.toPlainString());
        }
        return fen.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    /** 换算结果：成功给 fen，失败给 errorCode（两者互斥） */
    public record Result(Long fen, String errorCode) {
        public boolean ok() {
            return fen != null;
        }
    }

    /**
     * 元 → 分（精确）。
     *
     * @param raw 原始单元格文本（如 "1,234.50" 会被拒绝，"1234.50" → 123450）
     */
    public static Result toFen(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Result(null, ERR_EMPTY);
        }
        String s = raw.trim();
        // 去掉常见货币符号与不可见空白
        s = s.replace("¥", "").replace("￥", "").replace("$", "").replace("\u00A0", "").trim();
        if (s.isEmpty()) {
            return new Result(null, ERR_EMPTY);
        }
        // 千分位（半角/全角逗号）一律拒绝：说明上游未清洗，静默去逗号会掩盖格式问题
        if (s.indexOf(',') >= 0 || s.indexOf('，') >= 0) {
            return new Result(null, ERR_THOUSANDS_SEP);
        }
        // 全角数字/全角小数点
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 0x7F) {
                return new Result(null, ERR_NON_ASCII_DIGIT);
            }
        }
        if (!NUMERIC.matcher(s).matches()) {
            // 有 3 位以上小数，或根本不是数字
            if (s.matches("^-?\\d+\\.\\d{3,}$")) {
                return new Result(null, ERR_DECIMAL_INVALID);
            }
            return new Result(null, ERR_NOT_NUMERIC);
        }
        BigDecimal yuan = new BigDecimal(s);
        BigDecimal fen = yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP);
        // 复核：2 位以内小数的元值 ×100 必然整数；这里再断言一次，防将来有人放宽正则
        if (fen.compareTo(yuan.movePointRight(2)) != 0) {
            return new Result(null, ERR_DECIMAL_INVALID);
        }
        return new Result(fen.longValueExact(), null);
    }

    /** 分 → 元（仅用于展示/测试断言，不得用于入库） */
    public static BigDecimal toYuan(long fen) {
        return BigDecimal.valueOf(fen).movePointLeft(2);
    }
}
