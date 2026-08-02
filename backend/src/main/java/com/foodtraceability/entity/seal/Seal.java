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
 * 印章实体类
 * 存储印章基本信息、授权使用人和授权使用场景
 *
 * <p>枚举值（与前端 types/seal.ts 保持一致）：
 * <ul>
 *   <li>sealType: official-公章 / finance-财务专用章 / contract-合同专用章 / legal-法人章 / custom-自定义</li>
 *   <li>status: active-启用 / inactive-停用 / revoked-作废</li>
 *   <li>authorizedScenes: hr_contract-人事合同 / purchase_contract-采购合同 / electronic_contract-电子合同</li>
 * </ul>
 *
 * <p>JSON 字段说明：
 * authorizedUsers / authorizedScenes 使用 String 存储 JSON 数组字符串，
 * 序列化/反序列化由 Service 层负责（如 ["U001","U002"]）。
 */
@TableName("seals")
public class Seal implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 印章ID */
    @TableId(value = "seal_id", type = IdType.ASSIGN_ID)
    private String sealId;

    /** 印章名称 */
    @TableField("seal_name")
    private String sealName;

    /** 印章类型(official/finance/contract/legal/custom) */
    @TableField("seal_type")
    private String sealType;

    /** 印章图片(Base64/data URI) */
    @TableField("seal_image")
    private String sealImage;

    /** 状态(active/inactive/revoked) */
    @TableField("status")
    private String status;

    /** 保管人 */
    @TableField("keeper")
    private String keeper;

    /** 授权使用人ID列表(JSON数组字符串,如 ["U001","U002"]) */
    @TableField("authorized_users")
    private String authorizedUsers;

    /** 授权使用场景列表(JSON数组字符串,如 ["hr_contract","purchase_contract"]) */
    @TableField("authorized_scenes")
    private String authorizedScenes;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

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

    // ==================== 状态常量 ====================

    /** 启用 */
    public static final String STATUS_ACTIVE = "active";
    /** 停用 */
    public static final String STATUS_INACTIVE = "inactive";
    /** 作废 */
    public static final String STATUS_REVOKED = "revoked";

    // ==================== 印章类型常量 ====================

    /** 公章 */
    public static final String TYPE_OFFICIAL = "official";
    /** 财务专用章 */
    public static final String TYPE_FINANCE = "finance";
    /** 合同专用章 */
    public static final String TYPE_CONTRACT = "contract";
    /** 法人章 */
    public static final String TYPE_LEGAL = "legal";
    /** 自定义 */
    public static final String TYPE_CUSTOM = "custom";

    // ==================== 使用场景常量 ====================

    /** 人事合同 */
    public static final String SCENE_HR_CONTRACT = "hr_contract";
    /** 采购合同 */
    public static final String SCENE_PURCHASE_CONTRACT = "purchase_contract";
    /** 电子合同 */
    public static final String SCENE_ELECTRONIC_CONTRACT = "electronic_contract";

    // ==================== Getter & Setter ====================

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

    public String getSealType() {
        return sealType;
    }

    public void setSealType(String sealType) {
        this.sealType = sealType;
    }

    public String getSealImage() {
        return sealImage;
    }

    public void setSealImage(String sealImage) {
        this.sealImage = sealImage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKeeper() {
        return keeper;
    }

    public void setKeeper(String keeper) {
        this.keeper = keeper;
    }

    public String getAuthorizedUsers() {
        return authorizedUsers;
    }

    public void setAuthorizedUsers(String authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    public String getAuthorizedScenes() {
        return authorizedScenes;
    }

    public void setAuthorizedScenes(String authorizedScenes) {
        this.authorizedScenes = authorizedScenes;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
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
