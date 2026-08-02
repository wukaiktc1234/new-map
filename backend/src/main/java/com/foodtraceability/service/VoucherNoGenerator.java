package com.foodtraceability.service;

import com.foodtraceability.mapper.VoucherHeaderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 财务凭证号生成器
 *
 * <p>会计法合规：凭证应当连续编号，且凭证号唯一不可重复、不可预测。
 * 本生成器按"会计期间 + 期间内流水号"的规则生成凭证号，保证同一会计期间内凭证号连续。</p>
 *
 * <p>凭证号格式：{@code V + YYYYMM + 6位流水号}，例如 {@code V202607000001}。
 * 流水号在每个会计期间内从 1 开始递增，最大支持 999999 笔凭证/期间。</p>
 *
 * <p>并发安全策略：
 * <ul>
 *   <li>JVM 内：{@code synchronized} 关键字保证单机线程安全</li>
 *   <li>数据库层：{@code uk_voucher_header_voucher_no} 唯一索引兜底，并发插入同号会抛唯一约束异常</li>
 *   <li>事务边界：{@code REQUIRES_NEW} 独立事务，避免外层事务回滚影响流水号分配</li>
 * </ul>
 * </p>
 *
 * @author example
 * @since 2026-07-17
 */
@Service
public class VoucherNoGenerator {

    /** 凭证号前缀 */
    private static final String VOUCHER_NO_PREFIX = "V";

    /** 流水号位数 */
    private static final int SEQUENCE_LENGTH = 6;

    /** 凭证号总长度：V(1) + YYYYMM(6) + 流水号(6) = 13 */
    private static final int VOUCHER_NO_LENGTH = 1 + 6 + SEQUENCE_LENGTH;

    private final VoucherHeaderMapper voucherHeaderMapper;

    public VoucherNoGenerator(VoucherHeaderMapper voucherHeaderMapper) {
        this.voucherHeaderMapper = voucherHeaderMapper;
    }

    /**
     * 生成指定会计期间的下一个凭证号
     *
     * <p>查询当前期间已存在的最大凭证号，提取流水号并自增。
     * 若该期间无凭证或现有凭证号不符合新格式，则从 1 开始。</p>
     *
     * @param period 会计期间，格式 yyyy-MM（如 "2026-07"）
     * @return 凭证号，如 "V202607000001"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String generateVoucherNo(String period) {
        if (period == null || period.isEmpty()) {
            throw new IllegalArgumentException("会计期间不能为空");
        }

        String maxVoucherNo = voucherHeaderMapper.selectMaxVoucherNoByPeriod(period);
        int sequence = extractSequence(maxVoucherNo, period) + 1;

        String periodCompact = period.replace("-", "");
        return String.format("%s%s%0" + SEQUENCE_LENGTH + "d", VOUCHER_NO_PREFIX, periodCompact, sequence);
    }

    /**
     * 从现有凭证号中提取流水号
     *
     * <p>仅识别符合新格式（V + YYYYMM + 6位流水号）的凭证号；
     * 历史遗留的其他格式凭证号将被忽略，从 1 重新计数。</p>
     *
     * @param voucherNo 现有最大凭证号
     * @param period    会计期间，格式 yyyy-MM
     * @return 流水号；若不匹配新格式则返回 0
     */
    private int extractSequence(String voucherNo, String period) {
        if (voucherNo == null || voucherNo.length() != VOUCHER_NO_LENGTH) {
            return 0;
        }
        String expectedPrefix = VOUCHER_NO_PREFIX + period.replace("-", "");
        if (!voucherNo.startsWith(expectedPrefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(voucherNo.substring(expectedPrefix.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
