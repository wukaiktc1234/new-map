package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;

/**
 * 会计科目实体类
 * 用于管理餐饮企业的会计科目体系，包括资产、负债、权益、成本、损益五大类
 */
@TableName("accounting_subjects")
public class AccountingSubject extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 科目ID */
    @TableId(type = IdType.AUTO)
    private Long subjectId;

    /** 科目编码，如1001、1002、2202等 */
    private String subjectCode;

    /** 科目名称，如库存现金、银行存款、应付账款等 */
    private String subjectName;

    /** 父科目ID，一级科目为null */
    private Long parentId;

    /**
     * 科目类型
     * 1-资产类 2-负债类 3-所有者权益类 4-成本类 5-损益类
     */
    private Integer subjectType;

    /**
     * 余额方向
     * 1-借方余额 2-贷方余额
     */
    private Integer direction;

    /** 是否叶子节点（是否有子科目） */
    private Boolean isLeaf;

    /**
     * 状态
     * 1-启用 0-停用
     */
    private Integer status;

    /** 备注 */
    private String remark;

    /**
     * 科目余额（单位：分）
     *
     * <p>Sprint 3.1 P0 T-021：凭证过账时按 ADR-005 规则更新。
     * 借方科目余额 = 借方发生额 - 贷方发生额；
     * 贷方科目余额 = 贷方发生额 - 借方发生额。</p>
     */
    private Long balance;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

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

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "AccountingSubject{" +
                "subjectId=" + subjectId +
                ", subjectCode='" + subjectCode + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", parentId=" + parentId +
                ", subjectType=" + subjectType +
                ", direction=" + direction +
                ", isLeaf=" + isLeaf +
                ", status=" + status +
                ", balance=" + balance +
                '}';
    }
}
