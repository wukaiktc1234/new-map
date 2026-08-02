package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户分配门店DTO
 * 用于将门店分配给用户的请求参数
 */
@Schema(description = "用户分配门店DTO")
public class UserAssignStoreDTO {

    @NotBlank(message = "门店ID不能为空")
    @Schema(description = "门店ID", example = "1234567890")
    private Long storeId;

    public UserAssignStoreDTO() {
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
