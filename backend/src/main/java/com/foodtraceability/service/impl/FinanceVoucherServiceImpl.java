package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.FinanceVoucherDTO;
import com.foodtraceability.entity.FinanceVoucher;
import com.foodtraceability.service.FinanceVoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 财务凭证Service Stub实现 (空实现)
 * @deprecated 已废弃，仅用于兼容旧代码编译
 */
@Deprecated
@Service("legacyFinanceVoucherServiceImpl")
public class FinanceVoucherServiceImpl implements FinanceVoucherService {

    private static final Logger log = LoggerFactory.getLogger(FinanceVoucherServiceImpl.class);

    @Override
    public IPage<FinanceVoucher> getFinanceVoucherPage(Page<FinanceVoucher> page, String voucherNo,
                                                       String voucherType, String businessType, String status,
                                                       Date startDate, Date endDate) {
        log.warn("[STUB] FinanceVoucherService.getFinanceVoucherPage() called - no-op");
        return null;
    }

    @Override
    public FinanceVoucher createFinanceVoucher(FinanceVoucher voucher) {
        log.warn("[STUB] FinanceVoucherService.createFinanceVoucher() called - no-op");
        return null;
    }

    @Override
    public FinanceVoucherDTO createFinanceVoucherWithDetails(FinanceVoucherDTO voucherDTO) {
        log.warn("[STUB] FinanceVoucherService.createFinanceVoucherWithDetails() called - no-op");
        return null;
    }

    @Override
    public FinanceVoucherDTO getFinanceVoucherDTOById(Long id) {
        log.warn("[STUB] FinanceVoucherService.getFinanceVoucherDTOById() called - no-op");
        return null;
    }

    @Override
    public FinanceVoucher updateFinanceVoucher(Long id, FinanceVoucher voucher) {
        log.warn("[STUB] FinanceVoucherService.updateFinanceVoucher() called - no-op");
        return null;
    }

    @Override
    public FinanceVoucherDTO updateFinanceVoucherWithDetails(Long id, FinanceVoucherDTO voucherDTO) {
        log.warn("[STUB] FinanceVoucherService.updateFinanceVoucherWithDetails() called - no-op");
        return null;
    }

    @Override
    public void deleteFinanceVoucher(Long id) {
        log.warn("[STUB] FinanceVoucherService.deleteFinanceVoucher() called - no-op");
    }

    @Override
    public Boolean approveVoucher(Long id) {
        log.warn("[STUB] FinanceVoucherService.approveVoucher() called - no-op");
        return false;
    }

    @Override
    public Boolean rejectVoucher(Long id, String remark) {
        log.warn("[STUB] FinanceVoucherService.rejectVoucher() called - no-op");
        return false;
    }
}
