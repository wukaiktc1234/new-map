package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 用户信息DTO
 * 用于登录响应中的用户信息数据传输
 */
@Schema(description = "用户信息DTO")
public class UserInfoDTO {

    @Schema(description = "用户ID", example = "1234567890")
    private String id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "用户姓名", example = "管理员")
    private String name;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "角色列表", example = "[\"admin\", \"user\"]")
    private List<String> roles;

    @Schema(description = "权限列表", example = "[\"user:manage\", \"product:view\"]")
    private List<String> permissions;

    public UserInfoDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
