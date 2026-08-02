package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 字典项明细实体类
 * 用于存储字典的具体选项值，如：启用/禁用、待处理/已完成等
 */
@TableName("sys_dict_item")
@Schema(description = "字典项明细实体")
public class SysDictItem {

    @TableId(type = IdType.AUTO)
    @Schema(description = "字典项ID")
    private Long itemId;

    @TableField("dict_id")
    @Schema(description = "关联的字典类型ID")
    private Long dictId;

    @TableField("item_label")
    @Schema(description = "字典项标签（显示文本）")
    private String itemLabel;

    @TableField("item_value")
    @Schema(description = "字典项值（实际值）")
    private String itemValue;

    @TableField("sort_order")
    @Schema(description = "排序序号")
    private Integer sortOrder;

    @TableField("css_class")
    @Schema(description = "CSS样式类")
    private String cssClass;

    @TableField("list_class")
    @Schema(description = "列表样式类")
    private String listClass;

    @TableField("color_type")
    @Schema(description = "颜色类型")
    private String colorType;

    @TableField("is_default")
    @Schema(description = "是否默认项：0-否，1-是")
    private Integer isDefault;

    @TableField("status")
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @TableField("remark")
    @Schema(description = "备注说明")
    private String remark;

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

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getListClass() {
        return listClass;
    }

    public void setListClass(String listClass) {
        this.listClass = listClass;
    }

    public String getColorType() {
        return colorType;
    }

    public void setColorType(String colorType) {
        this.colorType = colorType;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
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
