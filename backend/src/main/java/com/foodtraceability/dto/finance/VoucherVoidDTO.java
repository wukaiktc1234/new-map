package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 凭证作废请求DTO
 */
public class VoucherVoidDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 作废原因 */
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
