package com.foodtraceability.dto.seal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 印章使用记录VO（视图对象）
 * 用于返回给前端的印章使用记录信息
 */
@Schema(description = "印章使用记录视图对象")
public class SealUsageLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "印章ID")
    private String sealId;

    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "业务类型：hr_contract-人事合同 purchase_contract-采购合同 electronic_contract-电子合同")
    private String businessType;

    @Schema(description = "业务类型名称")
    private String businessTypeName;

    @Schema(description = "业务ID")
    private String businessId;

    @Schema(description = "业务编号")
    private String businessNo;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作人IP")
    private String operatorIp;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSealId() {
        return sealId;
    }

    public void setSealId(String sealId) {
        this.sealId = sealId;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessTypeName() {
        return businessTypeName;
    }

    public void setBusinessTypeName(String businessTypeName) {
        this.businessTypeName = businessTypeName;
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

    public String getOperatorIp() {
        return operatorIp;
    }

    public void setOperatorIp(String operatorIp) {
        this.operatorIp = operatorIp;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
