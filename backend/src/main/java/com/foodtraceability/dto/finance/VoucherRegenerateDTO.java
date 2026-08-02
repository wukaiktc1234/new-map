package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 凭证重新生成请求DTO
 */
public class VoucherRegenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源事件ID */
    private Long sourceEventId;

    public Long getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(Long sourceEventId) {
        this.sourceEventId = sourceEventId;
    }
}
