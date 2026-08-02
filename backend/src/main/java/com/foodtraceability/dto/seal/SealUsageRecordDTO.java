package com.foodtraceability.dto.seal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 印章使用记录创建DTO
 */
@Schema(description = "印章使用记录创建请求")
public class SealUsageRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "印章ID", required = true)
    @NotBlank(message = "印章ID不能为空")
    private String sealId;

    @Schema(description = "业务类型：hr_contract-人事合同 purchase_contract-采购合同 electronic_contract-电子合同", required = true)
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "业务ID", required = true)
    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    @Schema(description = "业务编号")
    private String businessNo;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public String getSealId() {
        return sealId;
    }

    public void setSealId(String sealId) {
        this.sealId = sealId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
