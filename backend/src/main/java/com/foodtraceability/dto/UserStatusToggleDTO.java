package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 用户状态切换DTO
 * 用于启用/禁用用户的请求参数
 */
@Schema(description = "用户状态切换DTO")
public class UserStatusToggleDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 1, message = "状态值不能大于1")
    @Schema(description = "用户状态（1-正常，0-禁用）", example = "1")
    private Integer status;

    public UserStatusToggleDTO() {
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
