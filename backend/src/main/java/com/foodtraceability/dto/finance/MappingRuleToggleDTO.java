package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 映射规则启用/禁用切换DTO
 */
public class MappingRuleToggleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否启用 */
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
