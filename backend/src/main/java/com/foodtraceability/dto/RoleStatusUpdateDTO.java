package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 角色状态更新DTO
 * 用于切换角色启用/禁用状态的请求参数
 */
@Schema(description = "角色状态更新DTO")
public class RoleStatusUpdateDTO {

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态值（1-启用，0-禁用，或active/inactive）", example = "1")
    private Object status;

    public RoleStatusUpdateDTO() {
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        this.status = status;
    }
}
