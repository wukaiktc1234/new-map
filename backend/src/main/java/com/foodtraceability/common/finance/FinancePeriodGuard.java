package com.foodtraceability.common.finance;

import com.foodtraceability.common.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 期间封账守卫（P4-4，TD-39 硬拦截）。
 *
 * <p>设计依据：§4「期间封账后只读，补录须走反结账」+ RK-8。所有**写入资金数据**的入口
 * （存款单 deposit/verify、资金流水新增、勾稽冲正）都必须先调用 {@link #assertOpen(LocalDate)}，
 * 否则结账就只是「报表好看」，数据仍可被随意补录。</p>
 *
 * <p><b>为什么抽成一个组件</b>：三个 service 各写一遍 SQL 必然漂移（有人按 create_time、有人按
 * business_date），门禁就形同虚设。这里只有一个判定点。</p>
 *
 * <p>判定口径：<b>业务日期</b>（business_date）落在某期间 [start_date, end_date] 内且该期间
 * {@code is_closed = true} → 409；期间不存在 → 放行（历史数据早于首个期间时可写）。</p>
 */
@Component
public class FinancePeriodGuard {

    private static final Logger log = LoggerFactory.getLogger(FinancePeriodGuard.class);

    private final JdbcTemplate jdbc;

    public FinancePeriodGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 断言该业务日期所在期间未结账。
     *
     * @param businessDate 业务日期（必填；为 null 时放行并记 WARN，避免历史调用方被一刀切打挂）
     * @throws ConflictException 期间已结账（409）
     */
    public void assertOpen(LocalDate businessDate) {
        if (businessDate == null) {
            log.warn("[PERIOD_GUARD] businessDate 为空，跳过期间封账校验（调用方需自查）");
            return;
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT period_code, is_closed FROM accounting_periods "
                        + " WHERE deleted = 0 AND start_date <= ? AND end_date >= ? "
                        + " ORDER BY start_date DESC LIMIT 1",
                businessDate, businessDate);
        if (rows.isEmpty()) {
            return; // 没有覆盖该日期的期间 → 放行
        }
        Map<String, Object> p = rows.get(0);
        boolean closed = p.get("is_closed") != null && Boolean.TRUE.equals(p.get("is_closed"));
        if (closed) {
            String code = String.valueOf(p.get("period_code"));
            log.warn("[PERIOD_GUARD] 拒绝写入：期间 {} 已结账，businessDate={}", code, businessDate);
            throw new ConflictException("期间 " + code + " 已结账，禁止写入；如需补录请先反结账");
        }
    }
}
