package com.foodtraceability.service;

import com.foodtraceability.dto.FinanceVoucherDTO;
import com.foodtraceability.entity.FinanceVoucher;

import java.util.Date;

/**
 * 财务凭证Service接口 (已废弃，请使用com.foodtraceability.service.finance.VoucherService)
 * @deprecated 此接口已废弃，将在后续版本移除
 */
@Deprecated
public interface FinanceVoucherService {

    /**
     * 分页查询财务凭证
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    com.baomidou.mybatisplus.core.metadata.IPage<FinanceVoucher> getFinanceVoucherPage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<FinanceVoucher> page,
            String voucherNo, String voucherType, String businessType, String status,
            Date startDate, Date endDate);

    /**
     * 创建财务凭证
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    FinanceVoucher createFinanceVoucher(FinanceVoucher voucher);

    /**
     * 创建财务凭证（包含明细）
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    FinanceVoucherDTO createFinanceVoucherWithDetails(FinanceVoucherDTO voucherDTO);

    /**
     * 获取财务凭证详情
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    FinanceVoucherDTO getFinanceVoucherDTOById(Long id);

    /**
     * 更新财务凭证
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    FinanceVoucher updateFinanceVoucher(Long id, FinanceVoucher voucher);

    /**
     * 更新财务凭证（包含明细）
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    FinanceVoucherDTO updateFinanceVoucherWithDetails(Long id, FinanceVoucherDTO voucherDTO);

    /**
     * 删除财务凭证
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    void deleteFinanceVoucher(Long id);

    /**
     * 审核财务凭证
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    Boolean approveVoucher(Long id);

    /**
     * 拒绝财务凭证
     * @deprecated 请使用新的finance包中的VoucherService
     */
    @Deprecated
    Boolean rejectVoucher(Long id, String remark);
}
