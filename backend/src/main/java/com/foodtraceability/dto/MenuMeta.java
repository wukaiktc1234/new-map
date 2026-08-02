package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜单元数据DTO
 * 统一前后端菜单数据格式
 */
@Schema(description = "菜单元数据")
public class MenuMeta {
    
    @Schema(description = "菜单标题", example = "产品管理")
    private String title;
    
    @Schema(description = "菜单图标", example = "goods")
    private String icon;
    
    @Schema(description = "是否隐藏菜单", example = "false")
    private Boolean hidden;
    
    @Schema(description = "是否缓存页面", example = "false")
    private Boolean keepAlive;
    
    @Schema(description = "是否需要权限", example = "true")
    private Boolean requireAuth;
    
    @Schema(description = "菜单类型（0: 目录, 1: 菜单, 2: 按钮）", example = "1")
    private Integer menuType;
    
    @Schema(description = "角色列表")
    private String[] roles;
    
    @Schema(description = "权限列表")
    private String[] permissions;

    @Schema(description = "菜单分类", example = "system")
    private String category;

    // Getter and Setter methods
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public Boolean getHidden() {
        return hidden;
    }
    
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
    
    public Boolean getKeepAlive() {
        return keepAlive;
    }
    
    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public Boolean getRequireAuth() {
        return requireAuth;
    }
    
    public void setRequireAuth(Boolean requireAuth) {
        this.requireAuth = requireAuth;
    }
    
    public Integer getMenuType() {
        return menuType;
    }
    
    public void setMenuType(Integer menuType) {
        this.menuType = menuType;
    }
    
    public String[] getRoles() {
        return roles;
    }
    
    public void setRoles(String[] roles) {
        this.roles = roles;
    }
    
    public String[] getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}