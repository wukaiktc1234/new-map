package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.VoucherHeader;
import com.foodtraceability.mapper.VoucherHeaderMapper;
import com.foodtraceability.service.VoucherHeaderService;
import com.foodtraceability.service.VoucherNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 凭证头服务实现
 *
 * <p>会计法合规要点：
 * <ul>
 *   <li>凭证号由 {@link VoucherNoGenerator} 统一生成，保证连续性与唯一性</li>
 *   <li>已审核凭证不可直接修改/删除，必须通过红字凭证冲销（{@link #createReverseVoucher}）</li>
 *   <li>实体层通过 {@code @TableLogic} + {@code @Version} 防止物理删除与并发篡改</li>
 * </ul>
 * </p>
 *
 * @author example
 * @since 2025-12-07
 */
@Service
public class VoucherHeaderServiceImpl extends ServiceImpl<VoucherHeaderMapper, VoucherHeader> implements VoucherHeaderService {

    /** 会计期间格式 */
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 凭证审核状态：已审核 */
    private static final String REVIEW_STATUS_APPROVED = "APPROVED";

    /** 凭证审核状态：未审核 */
    private static final String REVIEW_STATUS_PENDING = "PENDING";

    private final VoucherNoGenerator voucherNoGenerator;

    public VoucherHeaderServiceImpl(VoucherNoGenerator voucherNoGenerator) {
        this.voucherNoGenerator = voucherNoGenerator;
    }

    @Override
    public List<VoucherHeader> getByStatus(String status) {
        // TODO: 实现按状态查询逻辑
        return Collections.emptyList();
    }

    @Override
    public List<VoucherHeader> getByPeriod(String period) {
        // TODO: 实现按会计期间查询逻辑
        return Collections.emptyList();
    }

    @Override
    public List<VoucherHeader> getByDateRange(String startDate, String endDate) {
        // TODO: 实现按日期范围查询逻辑
        return Collections.emptyList();
    }

    /**
     * 生成凭证号
     *
     * <p>使用当前日期对应的会计期间生成下一个凭证号。
     * 凭证号格式：V + YYYYMM + 6位流水号（如 V202607000001）。</p>
     *
     * @return 凭证号
     */
    @Override
    public String generateVoucherNumber() {
        String period = LocalDate.now().format(PERIOD_FORMATTER);
        return voucherNoGenerator.generateVoucherNo(period);
    }

    @Override
    public boolean submitVoucher(Long id) {
        // TODO: 实现提交凭证逻辑
        return false;
    }

    @Override
    public boolean approveVoucher(Long id) {
        // TODO: 实现审核凭证逻辑
        return false;
    }

    @Override
    public boolean postVoucher(Long id) {
        // TODO: 实现过账凭证逻辑
        return false;
    }

    @Override
    public boolean cancelVoucher(Long id) {
        // TODO: 实现取消凭证逻辑
        return false;
    }

    /**
     * 创建红字冲销凭证
     *
     * <p>会计法合规：已审核凭证不得直接修改或删除，必须通过创建红字凭证进行冲销。
     * 红字凭证的借贷金额取原凭证的负数，关联原凭证ID，并记录冲销原因。</p>
     *
     * <p>校验规则：
     * <ul>
     *   <li>原凭证必须存在</li>
     *   <li>原凭证必须已审核（草稿凭证可直接作废，无需红冲）</li>
     *   <li>原凭证不能已被红冲（避免重复冲销）</li>
     *   <li>原凭证本身不能是红字凭证（不允许对红字凭证再红冲）</li>
     *   <li>冲销原因不能为空</li>
     * </ul>
     * </p>
     *
     * @param originalVoucherId 原凭证ID
     * @param reason            冲销原因
     * @return 生成的红字凭证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoucherHeader createReverseVoucher(Long originalVoucherId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_HEADER_REASON_REQUIRED, "红冲原因不能为空");
        }

        VoucherHeader original = this.getById(originalVoucherId);
        if (original == null) {
            throw new BusinessException(ErrorCode.VOUCHER_HEADER_NOT_FOUND, "原凭证不存在：" + originalVoucherId);
        }

        if (Boolean.TRUE.equals(original.getIsReversed())) {
            throw new BusinessException(ErrorCode.VOUCHER_HEADER_ALREADY_REVERSED, "该凭证是红字凭证，不允许再次红冲");
        }

        if (original.getReverseOriginalId() != null) {
            throw new BusinessException(ErrorCode.VOUCHER_HEADER_ALREADY_REVERSED, "原凭证已被红冲，不允许重复冲销");
        }

        if (!REVIEW_STATUS_APPROVED.equals(original.getReviewStatus())) {
            throw new BusinessException(ErrorCode.VOUCHER_HEADER_NOT_APPROVED,
                "仅已审核凭证可红冲，当前状态：" + original.getReviewStatus());
        }

        // 生成红字凭证号（与原凭证同期）
        String reverseVoucherNo = voucherNoGenerator.generateVoucherNo(original.getPeriod());

        VoucherHeader reverse = new VoucherHeader();
        reverse.setVoucherNo(reverseVoucherNo);
        reverse.setVoucherDate(LocalDate.now());
        reverse.setSummary("红冲凭证：" + (original.getSummary() == null ? "" : original.getSummary()));
        reverse.setPeriod(original.getPeriod());
        // 红字金额：借贷方均取负数
        reverse.setTotalDebit(negate(original.getTotalDebit()));
        reverse.setTotalCredit(negate(original.getTotalCredit()));
        reverse.setStatus(original.getStatus());
        reverse.setSourceType(original.getSourceType());
        reverse.setSourceBusinessId(original.getSourceBusinessId());
        reverse.setSourceBusinessNo(original.getSourceBusinessNo());
        reverse.setAutoGenerated(original.getAutoGenerated());
        reverse.setReviewStatus(REVIEW_STATUS_PENDING);
        reverse.setIsReversed(Boolean.TRUE);
        reverse.setReverseOriginalId(original.getId());
        reverse.setReverseReason(reason);
        // deleted/version 由 MyBatis-Plus 自动初始化
        reverse.setDeleted(0);
        reverse.setVersion(0);
        this.save(reverse);

        // 标记原凭证为已红冲（触发乐观锁校验）
        original.setIsReversed(Boolean.TRUE);
        this.updateById(original);

        return reverse;
    }

    /**
     * 对金额取负（红字）。null 视为 0。
     *
     * @param amount 原金额
     * @return 负金额
     */
    private BigDecimal negate(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.negate();
    }
}
