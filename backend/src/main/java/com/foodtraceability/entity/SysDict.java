package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 字典类型实体类
 * 用于存储字典分类信息，如：用户状态、订单状态等
 */
@TableName("sys_dict")
@Schema(description = "字典类型实体")
public class SysDict {

    @TableId(type = IdType.AUTO)
    @Schema(description = "字典ID")
    private Long dictId;

    @TableField("dict_name")
    @Schema(description = "字典名称")
    private String dictName;

    @TableField("dict_code")
    @Schema(description = "字典编码（唯一标识）")
    private String dictCode;

    @TableField("dict_group")
    @Schema(description = "字典分组")
    private String dictGroup;

    @TableField("description")
    @Schema(description = "字典描述")
    private String description;

    @TableField("status")
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @TableField("is_system")
    @Schema(description = "是否系统内置：0-否，1-是")
    private Integer isSystem;

    @TableField("sort_order")
    @Schema(description = "排序序号")
    private Integer sortOrder;

    @TableField("create_user_id")
    @Schema(description = "创建人ID")
    private String createUserId;

    @TableField("create_username")
    @Schema(description = "创建人姓名")
    private String createUsername;

    @TableField("update_user_id")
    @Schema(description = "更新人ID")
    private String updateUserId;

    @TableField("update_username")
    @Schema(description = "更新人姓名")
    private String updateUsername;

    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictGroup() {
        return dictGroup;
    }

    public void setDictGroup(String dictGroup) {
        this.dictGroup = dictGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Integer isSystem) {
        this.isSystem = isSystem;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateUsername() {
        return createUsername;
    }

    public void setCreateUsername(String createUsername) {
        this.createUsername = createUsername;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public String getUpdateUsername() {
        return updateUsername;
    }

    public void setUpdateUsername(String updateUsername) {
        this.updateUsername = updateUsername;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
}
