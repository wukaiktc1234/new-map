package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 会计科目查询DTO
 */
public class AccountingSubjectQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 科目编码（模糊查询） */
    private String subjectCode;

    /** 科目名称（模糊查询） */
    private String subjectName;

    /** 父科目ID */
    private Long parentId;

    /** 科目类型：1资产 2负债 3权益 4成本 5损益 */
    private Integer subjectType;

    /** 余额方向：1借方 2贷方 */
    private Integer direction;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 是否只查叶子节点 */
    private Boolean leafOnly;

    /** 当前页码 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 20;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getLeafOnly() {
        return leafOnly;
    }

    public void setLeafOnly(Boolean leafOnly) {
        this.leafOnly = leafOnly;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
