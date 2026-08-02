package com.foodtraceability.entity;

/**
 * 角色统计实体类
 */
public class RoleCount {
    private String role;
    private Long count;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}