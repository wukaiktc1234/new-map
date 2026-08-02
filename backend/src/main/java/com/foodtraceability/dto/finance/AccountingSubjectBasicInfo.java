package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 会计科目基本信息DTO（用于缓存和批量查询）
 * 仅包含跨模块共享的必要字段，避免传输完整实体
 */
@Schema(description = "会计科目基本信息")
public class AccountingSubjectBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 科目ID */
    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    /** 科目编码，如1001、1002、2202等 */
    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    /** 科目名称，如库存现金、银行存款、应付账款等 */
    @Schema(description = "科目名称", example = "库存现金")
    private String subjectName;

    /** 科目类型：1-资产类 2-负债类 3-所有者权益类 4-成本类 5-损益类 */
    @Schema(description = "科目类型: 1资产 2负债 3权益 4成本 5损益", example = "1")
    private Integer subjectType;

    /** 余额方向：1-借方余额 2-贷方余额 */
    @Schema(description = "余额方向: 1借方 2贷方", example = "1")
    private Integer balanceDirection;

    /** 父科目ID，一级科目为null */
    @Schema(description = "父科目ID")
    private Long parentId;

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(Integer subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getBalanceDirection() {
        return balanceDirection;
    }

    public void setBalanceDirection(Integer balanceDirection) {
        this.balanceDirection = balanceDirection;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "AccountingSubjectBasicInfo{" +
                "subjectId=" + subjectId +
                ", subjectCode='" + subjectCode + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", subjectType=" + subjectType +
                ", balanceDirection=" + balanceDirection +
                ", parentId=" + parentId +
                '}';
    }
}
