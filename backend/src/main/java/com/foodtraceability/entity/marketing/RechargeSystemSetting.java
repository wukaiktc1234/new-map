package com.foodtraceability.entity.marketing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 储值系统设置实体类
 * 单行配置，config_json 存储完整的 RechargeSystemSettings JSON
 */
@TableName("recharge_system_settings")
public class RechargeSystemSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设置ID */
    @TableId(value = "setting_id", type = IdType.ASSIGN_ID)
    private String settingId;

    /** 配置JSON（完整储值系统设置） */
    @TableField("config_json")
    private String configJson;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 单行配置固定ID */
    public static final String DEFAULT_SETTING_ID = "RECHARGE_SETTINGS_001";

    // ==================== Getter & Setter ====================

    public String getSettingId() { return settingId; }
    public void setSettingId(String settingId) { this.settingId = settingId; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
