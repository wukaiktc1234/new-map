package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会计科目VO
 */
public class AccountingSubjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 科目ID */
    private Long subjectId;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 父科目ID */
    private Long parentId;

    /** 父科目名称 */
    private String parentName;

    /** 科目类型：1资产 2负债 3权益 4成本 5损益 */
    private Integer subjectType;

    /** 科目类型名称 */
    private String subjectTypeName;

    /** 余额方向：1借方 2贷方 */
    private Integer direction;

    /** 方向名称 */
    private String directionName;

    /** 是否叶子节点 */
    private Boolean isLeaf;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 子科目列表 */
    private List<AccountingSubjectVO> children;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Integer getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(Integer subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectTypeName() {
        return subjectTypeName;
    }

    public void setSubjectTypeName(String subjectTypeName) {
        this.subjectTypeName = subjectTypeName;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    public String getDirectionName() {
        return directionName;
    }

    public void setDirectionName(String directionName) {
        this.directionName = directionName;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<AccountingSubjectVO> getChildren() {
        return children;
    }

    public void setChildren(List<AccountingSubjectVO> children) {
        this.children = children;
    }
}
