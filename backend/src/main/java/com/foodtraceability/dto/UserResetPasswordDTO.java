package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户重置密码DTO
 * 用于管理员重置用户密码的请求参数
 */
@Schema(description = "用户重置密码DTO")
public class UserResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", example = "NewPass123!")
    private String newPassword;

    public UserResetPasswordDTO() {
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
