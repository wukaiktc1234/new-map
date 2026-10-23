package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TableName("finance_account")
public class FinanceAccount {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String accountCode;
    
    private String accountName;
    
    private String accountType;
    
    private Long parentId;
    
    private Integer level;
    
    private String direction;
    
    private Integer sort;
    
    private String status;
    
    private String remark;
    
    private Date createTime;
    
    private Date updateTime;

    /**
     * 子科目（树形结构用，非数据库字段）。
     * 不标 @TableField(exist = false) 会被 MyBatis-Plus 当成真实列，查询/插入直接报错。
     */
    @TableField(exist = false)
    private List<FinanceAccount> children = new ArrayList<>();

    public List<FinanceAccount> getChildren() {
        return children;
    }

    public void setChildren(List<FinanceAccount> children) {
        this.children = children;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAccountCode() {
        return accountCode;
    }
    
    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public Integer getSort() {
        return sort;
    }
    
    public void setSort(Integer sort) {
        this.sort = sort;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
