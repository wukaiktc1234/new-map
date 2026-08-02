package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.List;

/**
 * 会计科目实体类
 * @author example
 * @since 2025-12-06
 */
@TableName("accounting_subject")
public class AccountingSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科目ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 科目编码
     */
    @TableField("code")
    private String code;

    /**
     * 科目名称
     */
    @TableField("name")
    private String name;

    /**
     * 科目类别：资产、负债、所有者权益、成本、损益
     */
    @TableField("category")
    private String category;

    /**
     * 科目类型：一级科目、二级科目、三级科目
     */
    @TableField("type")
    private String type;

    /**
     * 父科目ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 余额方向：借、贷
     */
    @TableField("balance_direction")
    private String balanceDirection;

    /**
     * 状态：ACTIVE、INACTIVE
     */
    @TableField("status")
    private String status;

    /**
     * 科目描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 子科目列表（非数据库字段，用于树形结构）
     */
    @TableField(exist = false)
    private List<AccountingSubject> children;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getBalanceDirection() {
        return balanceDirection;
    }

    public void setBalanceDirection(String balanceDirection) {
        this.balanceDirection = balanceDirection;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public List<AccountingSubject> getChildren() {
        return children;
    }

    public void setChildren(List<AccountingSubject> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "AccountingSubject{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", category='" + category + '\'' +
            ", type='" + type + '\'' +
            ", parentId=" + parentId +
            ", balanceDirection='" + balanceDirection + '\'' +
            ", status='" + status + '\'' +
            ", description='" + description + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", createBy='" + createBy + '\'' +
            ", updateBy='" + updateBy + '\'' +
            ", children=" + (children != null ? children.size() : 0) +
            '}';
    }
}
