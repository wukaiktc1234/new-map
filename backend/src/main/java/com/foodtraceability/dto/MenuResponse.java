package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 菜单响应DTO
 */
@Schema(description = "菜单响应")
public class MenuResponse {
    
    @Schema(description = "菜单列表")
    private List<MenuItem> menus;
    
    /**
     * 菜单项内部类
     */
    @Schema(description = "菜单项")
    public static class MenuItem {
        
        @Schema(description = "菜单ID", example = "1234567890")
        private String id;
        
        @Schema(description = "菜单名称", example = "产品管理")
        private String name;
        
        @Schema(description = "菜单路径", example = "/product/list")
        private String path;
        
        @Schema(description = "组件路径", example = "@/views/ProductManagement.vue")
        private String component;
        
        @Schema(description = "重定向路径", example = "/product/list")
        private String redirect;
        
        @Schema(description = "菜单元数据")
        private MenuMeta meta;
        
        @Schema(description = "子菜单列表")
        private List<MenuItem> children;
        
        @Schema(description = "权限标识", example = "product:manage")
        private String permission;
        
        @Schema(description = "排序", example = "1")
        private Integer sort;
        
        @Schema(description = "父菜单ID", example = "0")
        private String parentId;
        
        // 兼容旧字段，同时支持meta字段
        @Schema(hidden = true)
        private String icon;
        
        @Schema(hidden = true)
        private Integer menuType;
        
        @Schema(hidden = true)
        private String status;
        
        @Schema(hidden = true)
        private Boolean hidden;
        
        @Schema(hidden = true)
        private Boolean keepAlive;
        
        // Getter and Setter methods for MenuItem
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
            // 设置meta的title
            if (meta == null) {
                meta = new MenuMeta();
            }
            meta.setTitle(name);
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getComponent() {
            return component;
        }
        
        public void setComponent(String component) {
            this.component = component;
        }
        
        public String getRedirect() {
            return redirect;
        }
        
        public void setRedirect(String redirect) {
            this.redirect = redirect;
        }
        
        public MenuMeta getMeta() {
            return meta;
        }
        
        public void setMeta(MenuMeta meta) {
            this.meta = meta;
        }
        
        public List<MenuItem> getChildren() {
            return children;
        }
        
        public void setChildren(List<MenuItem> children) {
            this.children = children;
        }
        
        public String getPermission() {
            return permission;
        }
        
        public void setPermission(String permission) {
            this.permission = permission;
        }
        
        public Integer getSort() {
            return sort;
        }
        
        public void setSort(Integer sort) {
            this.sort = sort;
        }
        
        public String getParentId() {
            return parentId;
        }
        
        public void setParentId(String parentId) {
            this.parentId = parentId;
        }
        
        // 兼容旧字段的getter和setter方法
        public String getIcon() {
            return icon;
        }
        
        public void setIcon(String icon) {
            this.icon = icon;
            // 同时更新meta字段
            if (meta == null) {
                meta = new MenuMeta();
            }
            meta.setIcon(icon);
        }
        
        public Integer getMenuType() {
            return menuType;
        }
        
        public void setMenuType(Integer menuType) {
            this.menuType = menuType;
            // 同时更新meta字段
            if (meta == null) {
                meta = new MenuMeta();
            }
            meta.setMenuType(menuType);
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public Boolean getHidden() {
            return hidden;
        }
        
        public void setHidden(Boolean hidden) {
            this.hidden = hidden;
            // 同时更新meta字段
            if (meta == null) {
                meta = new MenuMeta();
            }
            meta.setHidden(hidden);
        }
        
        public Boolean getKeepAlive() {
            return keepAlive;
        }
        
        public void setKeepAlive(Boolean keepAlive) {
            this.keepAlive = keepAlive;
            // 同时更新meta字段
            if (meta == null) {
                meta = new MenuMeta();
            }
            meta.setKeepAlive(keepAlive);
        }
    }
    
    // Getter and Setter methods for MenuResponse
    public List<MenuItem> getMenus() {
        return menus;
    }
    
    public void setMenus(List<MenuItem> menus) {
        this.menus = menus;
    }
}
