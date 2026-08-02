package com.foodtraceability.dto.seal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 印章VO（视图对象）
 * 用于返回给前端的印章详细信息
 */
@Schema(description = "印章视图对象")
public class SealVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "印章ID")
    private String sealId;

    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "印章类型：official-公章 finance-财务专用章 contract-合同专用章 legal-法人章 custom-自定义")
    private String sealType;

    @Schema(description = "印章类型名称")
    private String sealTypeName;

    @Schema(description = "印章图片URL")
    private String sealImageUrl;

    @Schema(description = "状态：active-启用 inactive-停用 revoked-作废")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "保管人")
    private String keeper;

    @Schema(description = "授权使用人ID列表")
    private List<String> authorizedUsers;

    @Schema(description = "授权使用场景列表")
    private List<String> authorizedScenes;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

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

    public String getSealTypeName() {
        return sealTypeName;
    }

    public void setSealTypeName(String sealTypeName) {
        this.sealTypeName = sealTypeName;
    }

    public String getSealImageUrl() {
        return sealImageUrl;
    }

    public void setSealImageUrl(String sealImageUrl) {
        this.sealImageUrl = sealImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getKeeper() {
        return keeper;
    }

    public void setKeeper(String keeper) {
        this.keeper = keeper;
    }

    public List<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public void setAuthorizedUsers(List<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    public List<String> getAuthorizedScenes() {
        return authorizedScenes;
    }

    public void setAuthorizedScenes(List<String> authorizedScenes) {
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
