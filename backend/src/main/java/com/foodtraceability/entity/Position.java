package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 职位实体类
 * 用于管理系统中的职位信息
 */
@TableName("positions")
@Schema(description = "职位实体")
public class Position {

    /**
     * 职位ID
     * 数据库主键为 position_id（BIGINT GENERATED ALWAYS AS IDENTITY）
     */
    @TableId(type = IdType.AUTO, value = "position_id")
    @Schema(description = "职位ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 职位名称
     */
    @Schema(description = "职位名称", example = "经理")
    private String positionName;

    /**
     * 职位编码（唯一）
     */
    @Schema(description = "职位编码（唯一）", example = "POS001")
    private String positionCode;

    /**
     * 所属部门ID
     */
    @TableField("department_id")
    @Schema(description = "所属部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;

    /**
     * 所属部门名称
     */
    @TableField(exist = false)
    @Schema(description = "所属部门名称", example = "技术部")
    private String department;

    /**
     * 部门排序顺序
     */
    @TableField(exist = false)
    @Schema(description = "部门排序顺序", example = "1")
    private Integer departmentSortOrder;

    /**
     * 职位编码序号（从position_code中提取）
     */
    @TableField(exist = false)
    @Schema(description = "职位编码序号（从position_code中提取）", example = "1")
    private Integer positionCodeSeq;

    /**
     * 职位描述
     */
    @Schema(description = "职位描述", example = "负责部门管理工作")
    private String description;

    /**
     * 员工数量
     */
    @Schema(description = "员工数量", example = "5")
    private Integer employeeCount;

    /**
     * 编制人数
     */
    @TableField("head_count")
    @Schema(description = "编制人数", example = "3")
    private Integer headCount;

    /**
     * 职位状态（1: 启用, 0: 禁用）
     */
    @Schema(description = "职位状态（1: 启用, 0: 禁用）", example = "1")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @TableField("created_by")
    @Schema(description = "创建人", example = "system")
    private String createdBy;

    /**
     * 更新人
     */
    @TableField("updated_by")
    @Schema(description = "更新人", example = "admin")
    private String updatedBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getDepartmentSortOrder() {
        return departmentSortOrder;
    }

    public void setDepartmentSortOrder(Integer departmentSortOrder) {
        this.departmentSortOrder = departmentSortOrder;
    }

    public Integer getPositionCodeSeq() {
        return positionCodeSeq;
    }

    public void setPositionCodeSeq(Integer positionCodeSeq) {
        this.positionCodeSeq = positionCodeSeq;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Integer getHeadCount() {
        return headCount;
    }

    public void setHeadCount(Integer headCount) {
        this.headCount = headCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
