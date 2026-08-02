package com.foodtraceability.entity.seal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 印章使用记录实体类
 * 记录每次印章使用的业务信息，用于审计追溯
 *
 * <p>businessType 枚举值（与前端 SealScene 保持一致）：
 * hr_contract-人事合同 / purchase_contract-采购合同 / electronic_contract-电子合同
 */
@TableName("seal_usage_logs")
public class SealUsageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    private String logId;

    /** 印章ID */
    @TableField("seal_id")
    private String sealId;

    /** 印章名称(冗余,便于展示) */
    @TableField("seal_name")
    private String sealName;

    /** 业务类型(hr_contract/purchase_contract/electronic_contract) */
    @TableField("business_type")
    private String businessType;

    /** 业务ID(合同ID) */
    @TableField("business_id")
    private String businessId;

    /** 业务编号(合同编号,便于展示) */
    @TableField("business_no")
    private String businessNo;

    /** 操作人 */
    @TableField("operator")
    private String operator;

    /** 操作时间 */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /** IP地址 */
    @TableField("ip_address")
    private String ipAddress;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 逻辑删除标记(0-未删除 1-已删除) */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 使用场景常量 ====================

    /** 人事合同 */
    public static final String SCENE_HR_CONTRACT = "hr_contract";
    /** 采购合同 */
    public static final String SCENE_PURCHASE_CONTRACT = "purchase_contract";
    /** 电子合同 */
    public static final String SCENE_ELECTRONIC_CONTRACT = "electronic_contract";

    // ==================== Getter & Setter ====================

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getSealId() {
        return sealId;
    }

    public void setSealId(String sealId) {
        this.sealId = sealId;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
