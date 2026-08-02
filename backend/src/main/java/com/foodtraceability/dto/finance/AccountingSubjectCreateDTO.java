package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 会计科目创建DTO
 */
public class AccountingSubjectCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 科目编码 */
    @NotBlank(message = "科目编码不能为空")
    @Size(max = 20, message = "科目编码长度不能超过20位")
    private String subjectCode;

    /** 科目名称 */
    @NotBlank(message = "科目名称不能为空")
    @Size(max = 100, message = "科目名称长度不能超过100位")
    private String subjectName;

    /** 父科目ID */
    private Long parentId;

    /** 科目类型：1资产 2负债 3权益 4成本 5损益 */
    @NotNull(message = "科目类型不能为空")
    @Min(value = 1, message = "科目类型无效")
    @Max(value = 5, message = "科目类型无效")
    private Integer subjectType;

    /** 余额方向：1借方 2贷方 */
    @NotNull(message = "余额方向不能为空")
    @Min(value = 1, message = "余额方向无效")
    @Max(value = 2, message = "余额方向无效")
    private Integer direction;

    /** 是否叶子节点 */
    private Boolean isLeaf;

    /** 状态：1启用 0停用 */
    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(Integer subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
