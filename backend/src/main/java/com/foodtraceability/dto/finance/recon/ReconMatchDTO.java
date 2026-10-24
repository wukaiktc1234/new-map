package com.foodtraceability.dto.finance.recon;

import jakarta.validation.constraints.NotBlank;

/**
 * R6 人工勾稽入参（§2.7 #8）。
 *
 * <p>externalRef = 同 run 内 EXTERNAL 主体的 external_ref；groupKey 用于 1:N/N:1 合并勾稽
 * （为空的 1:1 由服务端生成 group key）。</p>
 */
public class ReconMatchDTO {

    @NotBlank(message = "externalRef 不能为空")
    private String externalRef;

    private String groupKey;

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public String getGroupKey() { return groupKey; }
    public void setGroupKey(String groupKey) { this.groupKey = groupKey; }
}
