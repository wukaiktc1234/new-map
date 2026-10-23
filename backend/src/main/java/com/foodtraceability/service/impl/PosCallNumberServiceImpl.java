package com.foodtraceability.service.impl;

import com.foodtraceability.mapper.PosCallNumberMapper;
import com.foodtraceability.service.PosCallNumberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * POS 叫号号服务实现。
 *
 * <p><b>号段规则</b>：{@code ^[A-Z][0-9]{3,4}$}，前缀按订单类型区分
 * （0堂食→A、1外卖→B、2自提→C，其它→A），序号按
 * 「门店 + 自然日 + 前缀」每天从 1 自增，跨天自动从 1 重新开始。</p>
 *
 * <p><b>自增机制</b>：号段落在现有通用号段表 {@code order_sequence}
 * （键 {@code CALL-{storeId}-{yyyyMMdd}-{prefix}}），用单条
 * {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING} 原子取号：
 * PG 行锁串行化并发请求，既不依赖应用层 synchronized，也不会出现
 * 「SELECT MAX + 1」的经典竞态与重复号。</p>
 *
 * <p><b>幂等做法</b>：先读 {@code orders.call_number}，已有值直接返回；
 * 没有才取号，并用 {@code UPDATE ... WHERE call_number IS NULL} 做 CAS 绑定。
 * 并发下若被别的请求抢先绑定（UPDATE 影响 0 行），则读回对方的号返回，
 * 本次多消耗的序号直接丢弃——「宁可跳号，不可重号」。</p>
 *
 * <p><b>与状态机的关系</b>：本服务不修改订单状态，只追加叫号号字段，
 * 因此不触碰 {@code OrderLifecycleService} / {@code OrderNewServiceImpl}。</p>
 */
@Service
public class PosCallNumberServiceImpl implements PosCallNumberService {

    private static final Logger log = LoggerFactory.getLogger(PosCallNumberServiceImpl.class);

    /** 叫号号格式：1 位大写字母 + 3~4 位数字（A001 / B023 / C1000） */
    public static final Pattern CALL_NUMBER_PATTERN = Pattern.compile("^[A-Z][0-9]{3,4}$");

    /** 号段键前缀，与订单号号段（T/W + 日期）完全隔离，互不影响 */
    private static final String SEQUENCE_KEY_PREFIX = "CALL-";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 同一订单最多重试取号次数（含并发抢号后读回） */
    private static final int MAX_ALLOCATE_ATTEMPTS = 3;

    private final PosCallNumberMapper posCallNumberMapper;

    public PosCallNumberServiceImpl(PosCallNumberMapper posCallNumberMapper) {
        this.posCallNumberMapper = posCallNumberMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String ensureCallNumber(String orderId, Long storeId, Integer orderType) {
        return ensureCallNumber(orderId, storeId, orderType, LocalDate.now());
    }

    /**
     * 取号核心实现（显式指定业务日）。
     *
     * <p>业务日决定号段键：新单用「今天」，历史单补号用「下单当天」，
     * 这样补号不会把当天的号段往后推、也不会和历史同一编号撞车。</p>
     */
    private String ensureCallNumber(String orderId, Long storeId, Integer orderType, LocalDate businessDate) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return null;
        }
        // 1) 幂等：已取号直接返回，绝不二次取号
        String existing = posCallNumberMapper.selectOrderCallNumber(orderId);
        if (isValidCallNumber(existing)) {
            // 老单补历史：已取号但没有 call_record 行的顺手补上（幂等）
            ensureCallRecordRow(orderId, existing);
            return existing;
        }

        // 2) 号段键：门店 + 自然日 + 类型前缀（跨天自动从 1 重新开始）
        String prefix = resolvePrefix(orderType);
        LocalDate day = businessDate != null ? businessDate : LocalDate.now();
        String sequenceKey = SEQUENCE_KEY_PREFIX
                + (storeId != null ? storeId : 0L)
                + "-" + day.format(DAY_FORMATTER)
                + "-" + prefix;

        for (int attempt = 1; attempt <= MAX_ALLOCATE_ATTEMPTS; attempt++) {
            Integer sequence = posCallNumberMapper.nextSequenceValue(sequenceKey);
            if (sequence == null || sequence <= 0) {
                log.error("叫号号段分配失败: orderId={}, sequenceKey={}, sequence={}", orderId, sequenceKey, sequence);
                return null;
            }
            String candidate = formatCallNumber(prefix, sequence);

            // 3) CAS 绑定：只有「本订单还没号」时才写成功
            int bound;
            try {
                bound = posCallNumberMapper.bindOrderCallNumber(orderId, candidate);
            } catch (org.springframework.dao.DataAccessException ex) {
                // 唯一索引冲突（同一门店同一业务日同号）→ 换下一个序号继续
                log.warn("叫号号写入冲突，换号重试: orderId={}, candidate={}, error={}",
                        orderId, candidate, ex.getMessage());
                continue;
            }
            if (bound > 0) {
                log.info("POS 叫号号分配: orderId={}, callNumber={}, sequenceKey={}, sequence={}",
                        orderId, candidate, sequenceKey, sequence);
                // 取号即入历史（此前缺这一步，导致「下单有号、历史查不到」）
                ensureCallRecordRow(orderId, candidate);
                return candidate;
            }

            // 4) 并发/重复调用：别人已经绑定 → 读回既有号（本次序号丢弃，宁可跳号不重号）
            String raced = posCallNumberMapper.selectOrderCallNumber(orderId);
            if (isValidCallNumber(raced)) {
                log.info("POS 叫号号已存在（幂等返回）: orderId={}, callNumber={}", orderId, raced);
                return raced;
            }
            log.warn("叫号号绑定未命中（订单可能不存在或为空值）: orderId={}, attempt={}, candidate={}",
                    orderId, attempt, candidate);
        }

        String lastChance = posCallNumberMapper.selectOrderCallNumber(orderId);
        if (isValidCallNumber(lastChance)) {
            ensureCallRecordRow(orderId, lastChance);
            return lastChance;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String ensureCallNumber(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return null;
        }
        String existing = posCallNumberMapper.selectOrderCallNumber(orderId);
        if (isValidCallNumber(existing)) {
            return existing;
        }
        // orders 无该行（遗留单）→ 不取号，避免空耗号段
        if (posCallNumberMapper.countOrder(orderId) <= 0) {
            log.debug("订单不在 orders 新表，跳过叫号号分配: orderId={}", orderId);
            return null;
        }
        Long storeId = posCallNumberMapper.selectOrderStoreId(orderId);
        Integer orderType = posCallNumberMapper.selectOrderType(orderId);
        // 历史单按「下单当天」的号段补号，不占用今天的号段
        java.time.LocalDateTime createTime = posCallNumberMapper.selectOrderCreateTime(orderId);
        LocalDate businessDate = createTime != null ? createTime.toLocalDate() : LocalDate.now();
        return ensureCallNumber(orderId, storeId, orderType, businessDate);
    }

    /**
     * 取号成功后立即补建 {@code call_record} 历史行（幂等）。
     *
     * <p>此前只有叫号屏 sync / 后端同步任务才建历史行，导致「下单拿到号但历史查不到」；
     * 现在取号即入历史，配合 /call、/pickup 的时间戳与次数更新形成闭环。</p>
     */
    private void ensureCallRecordRow(String orderId, String callNumber) {
        if (!isValidCallNumber(callNumber)) {
            return;
        }
        try {
            posCallNumberMapper.insertCallRecordIfAbsent(orderId, callNumber);
        } catch (Exception e) {
            // 历史行补建失败不影响下单/取号主流程
            log.warn("补建叫号历史行失败（不影响取号）: orderId={}, callNumber={}, error={}",
                    orderId, callNumber, e.getMessage());
        }
    }

    @Override
    public String peekCallNumber(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return null;
        }
        String existing = posCallNumberMapper.selectOrderCallNumber(orderId);
        return isValidCallNumber(existing) ? existing : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToCallRecord(String orderId, String callNumber) {
        if (orderId == null || orderId.trim().isEmpty() || !isValidCallNumber(callNumber)) {
            return;
        }
        try {
            posCallNumberMapper.bindCallRecordCallNumber(orderId, callNumber);
        } catch (Exception e) {
            // 叫号屏同步失败不影响订单/叫号主流程
            log.warn("叫号号同步到 call_record 失败（不影响主流程）: orderId={}, callNumber={}, error={}",
                    orderId, callNumber, e.getMessage());
        }
    }

    /**
     * 订单类型 → 叫号号前缀：0堂食 A、1外卖 B、2自提 C，其它按堂食 A。
     */
    private String resolvePrefix(Integer orderType) {
        if (orderType == null) {
            return "A";
        }
        return switch (orderType) {
            case 1 -> "B";
            case 2 -> "C";
            default -> "A";
        };
    }

    /**
     * 序号 → 叫号号：3 位补零（A001…A999），超过 999 自然进位成 4 位（A1000…A9999）。
     */
    private String formatCallNumber(String prefix, int sequence) {
        if (sequence > 9999) {
            log.warn("叫号号段超过 4 位上限（单店单日取号超 9999）: prefix={}, sequence={}", prefix, sequence);
        }
        return prefix + String.format("%03d", sequence);
    }

    private boolean isValidCallNumber(String value) {
        return value != null && !value.trim().isEmpty() && CALL_NUMBER_PATTERN.matcher(value.trim()).matches();
    }
}
