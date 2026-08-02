package com.foodtraceability.entity;

/**
 * 部门统计实体类
 */
public class DepartmentCount {
    private String department;
    private Long count;

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}