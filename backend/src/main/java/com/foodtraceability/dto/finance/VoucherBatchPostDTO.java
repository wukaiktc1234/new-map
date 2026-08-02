package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.util.List;

/**
 * 批量过账请求DTO
 */
public class VoucherBatchPostDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证ID列表 */
    private List<Long> voucherIds;

    public List<Long> getVoucherIds() {
        return voucherIds;
    }

    public void setVoucherIds(List<Long> voucherIds) {
        this.voucherIds = voucherIds;
    }
}
