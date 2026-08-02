package com.foodtraceability.dto.seal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

/**
 * 印章更新DTO
 * 用于接收更新印章的请求参数，所有字段可选
 */
@Schema(description = "印章更新请求")
public class SealUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "印章名称")
    @Size(max = 100, message = "印章名称长度不能超过100个字符")
    private String sealName;

    @Schema(description = "印章类型：official-公章 finance-财务专用章 contract-合同专用章 legal-法人章 custom-自定义")
    private String sealType;

    @Schema(description = "印章图片（Base64/data URI）")
    private String sealImage;

    @Schema(description = "保管人")
    @Size(max = 50, message = "保管人长度不能超过50个字符")
    private String keeper;

    @Schema(description = "授权使用人ID列表")
    private List<String> authorizedUsers;

    @Schema(description = "授权使用场景列表：hr_contract-人事合同 purchase_contract-采购合同 electronic_contract-电子合同")
    private List<String> authorizedScenes;

    @Schema(description = "状态：active-启用 inactive-停用 revoked-作废")
    private String status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    // ==================== Getter & Setter ====================

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
}
