package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门实体类
 * 用于管理系统中的部门信息，支持层级关系
 */
@TableName("departments")
@Schema(description = "部门实体")
public class Department {
    /**
     * 部门ID
     * 主键列名为 department_id（遵循项目规范 {table}_id 格式，
     * 与 Flyway 迁移 V1.0.0.100__init_postgresql.sql 中的建表语句一致）
     */
    @TableId(type = IdType.AUTO, value = "department_id")
    @Schema(description = "部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;
    /**
     * 部门名称
     */
    @TableField("dept_name")
    @Schema(description = "部门名称", example = "生产部")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    private String departmentName;
    /**
     * 部门编码（唯一）
     */
    @TableField("dept_code")
    @Schema(description = "部门编码（唯一）", example = "DEP001")
    @Pattern(regexp = "^[A-Z0-9_&\\-]{2,20}$", message = "部门编码格式不正确，应为2-20位大写字母、数字、下划线或连字符")
    private String departmentCode;
    /**
     * 父部门ID
     */
    @TableField("parent_id")
    @Schema(description = "父部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    /**
     * 部门层级
     * <p>
     * 注意：移除 @NotNull 校验，由 Service 层根据 parentId 自动计算 level
     * （DepartmentServiceImpl.createDepartment 中处理）。
     */
    @TableField("level")
    @Schema(description = "部门层级", example = "1")
    private Integer level;
    /**
     * 所属门店ID
     */
    @TableField("store_id")
    @Schema(description = "所属门店ID", example = "STORE001")
    @Size(max = 32, message = "门店ID长度不能超过32个字符")
    private String storeId;
    /**
     * 子部门列表（非数据库字段）
     */
    @TableField(exist = false)
    @Schema(description = "子部门列表")
    private List<Department> children;
    /**
     * 部门路径（用于快速查询层级关系）
     */
    @TableField(exist = false)
    @Schema(description = "部门路径", example = "/1234567890/")
    private String path;
    /**
     * 部门状态（1: 正常, 0: 禁用）
     * <p>
     * 注意：移除 @NotNull 校验，因为前端表单未显式传入时由 Service 层
     * 设置默认值 1（DepartmentServiceImpl.createDepartment 中处理）。
     * 校验应在 Service 层兜底，避免请求绑定阶段误判。
     */
    @TableField("status")
    @Schema(description = "部门状态（1: 正常, 0: 禁用）", example = "1")
    private Integer status;
    /**
     * 部门负责人ID
     */
    @TableField(exist = false)
    @Schema(description = "部门负责人ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long managerId;
    /**
     * 部门负责人姓名
     */
    @TableField("manager_name")
    @Schema(description = "部门负责人姓名", example = "张三")
    @Size(max = 50, message = "负责人姓名长度不能超过50个字符")
    private String managerName;
    /**
     * 所属公司ID
     */
    @TableField(exist = false)
    @Schema(description = "所属公司ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long companyId;
    /**
     * 部门描述
     */
    @TableField("description")
    @Schema(description = "部门描述", example = "负责生产管理")
    @Size(max = 500, message = "部门描述长度不能超过500个字符")
    private String description;
    /**
     * 排序号
     */
    @TableField("sort_order")
    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;
    /**
     * 员工数量
     */
    @TableField("employee_count")
    @Schema(description = "员工数量", example = "5")
    private Integer employeeCount;
    /**
     * 部门基准薪资（用于薪资计算）
     */
    @TableField("base_salary")
    @Schema(description = "部门基准薪资", example = "10000.00")
    private BigDecimal baseSalary;
    /**
     * 组织类型
     * 取值范围：company（公司）/ department（部门）/ store（门店）/ team（团队）/ office（办事处）/ group（小组）
     * 用于前端树形展示与门店类型识别
     */
    @TableField("type")
    @Schema(description = "组织类型：company/department/store/team/office/group", example = "department")
    @Size(max = 50, message = "组织类型长度不能超过50个字符")
    private String type;
    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2024-01-01 00:00:00")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2024-01-01 00:00:00")
    private LocalDateTime updatedAt;
    /**
     * 创建人ID
     */
    @TableField("created_by")
    @Schema(description = "创建人ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createdBy;
    /**
     * 更新人ID
     */
    @TableField("updated_by")
    @Schema(description = "更新人ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updatedBy;

    public Department() {
    }

    /**
     * 部门ID
     */
    public Long getDepartmentId() {
        return this.departmentId;
    }

    /**
     * 部门名称
     */
    public String getDepartmentName() {
        return this.departmentName;
    }

    /**
     * 部门编码（唯一）
     */
    public String getDepartmentCode() {
        return this.departmentCode;
    }

    /**
     * 父部门ID
     */
    public Long getParentId() {
        return this.parentId;
    }

    /**
     * 部门层级
     */
    public Integer getLevel() {
        return this.level;
    }

    /**
     * 所属门店ID
     */
    public String getStoreId() {
        return this.storeId;
    }

    /**
     * 子部门列表（非数据库字段）
     */
    public List<Department> getChildren() {
        return this.children;
    }

    /**
     * 部门路径（用于快速查询层级关系）
     */
    public String getPath() {
        return this.path;
    }

    /**
     * 部门状态（active: 正常, inactive: 禁用）
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 部门负责人ID
     */
    public Long getManagerId() {
        return this.managerId;
    }

    /**
     * 部门负责人姓名
     */
    public String getManagerName() {
        return this.managerName;
    }

    /**
     * 所属公司ID
     */
    public Long getCompanyId() {
        return this.companyId;
    }

    /**
     * 部门描述
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 排序号
     */
    public Integer getSortOrder() {
        return this.sortOrder;
    }

    /**
     * 员工数量
     */
    public Integer getEmployeeCount() {
        return this.employeeCount;
    }

    /**
     * 部门基准薪资（用于薪资计算）
     */
    public BigDecimal getBaseSalary() {
        return this.baseSalary;
    }

    /**
     * 组织类型
     */
    public String getType() {
        return this.type;
    }

    /**
     * 逻辑删除标记
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 创建人ID
     */
    public Long getCreatedBy() {
        return this.createdBy;
    }

    /**
     * 更新人ID
     */
    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    /**
     * 部门ID
     */
    public void setDepartmentId(final Long departmentId) {
        this.departmentId = departmentId;
    }

    /**
     * 部门名称
     */
    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    /**
     * 部门编码（唯一）
     */
    public void setDepartmentCode(final String departmentCode) {
        this.departmentCode = departmentCode;
    }

    /**
     * 父部门ID
     */
    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 部门层级
     */
    public void setLevel(final Integer level) {
        this.level = level;
    }

    /**
     * 所属门店ID
     */
    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    /**
     * 子部门列表（非数据库字段）
     */
    public void setChildren(final List<Department> children) {
        this.children = children;
    }

    /**
     * 部门路径（用于快速查询层级关系）
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * 部门状态（active: 正常, inactive: 禁用）
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 部门负责人ID
     */
    public void setManagerId(final Long managerId) {
        this.managerId = managerId;
    }

    /**
     * 部门负责人姓名
     */
    public void setManagerName(final String managerName) {
        this.managerName = managerName;
    }

    /**
     * 所属公司ID
     */
    public void setCompanyId(final Long companyId) {
        this.companyId = companyId;
    }

    /**
     * 部门描述
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 排序号
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 员工数量
     */
    public void setEmployeeCount(final Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    /**
     * 部门基准薪资（用于薪资计算）
     */
    public void setBaseSalary(final BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    /**
     * 组织类型
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * 逻辑删除标记
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 更新时间
     */
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 创建人ID
     */
    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 更新人ID
     */
    public void setUpdatedBy(final Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Department)) return false;
        final Department other = (Department) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$managerId = this.getManagerId();
        final java.lang.Object other$managerId = other.getManagerId();
        if (this$managerId == null ? other$managerId != null : !this$managerId.equals(other$managerId)) return false;
        final java.lang.Object this$companyId = this.getCompanyId();
        final java.lang.Object other$companyId = other.getCompanyId();
        if (this$companyId == null ? other$companyId != null : !this$companyId.equals(other$companyId)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$employeeCount = this.getEmployeeCount();
        final java.lang.Object other$employeeCount = other.getEmployeeCount();
        if (this$employeeCount == null ? other$employeeCount != null : !this$employeeCount.equals(other$employeeCount)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$departmentCode = this.getDepartmentCode();
        final java.lang.Object other$departmentCode = other.getDepartmentCode();
        if (this$departmentCode == null ? other$departmentCode != null : !this$departmentCode.equals(other$departmentCode)) return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children)) return false;
        final java.lang.Object this$path = this.getPath();
        final java.lang.Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final java.lang.Object this$managerName = this.getManagerName();
        final java.lang.Object other$managerName = other.getManagerName();
        if (this$managerName == null ? other$managerName != null : !this$managerName.equals(other$managerName)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$baseSalary = this.getBaseSalary();
        final java.lang.Object other$baseSalary = other.getBaseSalary();
        if (this$baseSalary == null ? other$baseSalary != null : !this$baseSalary.equals(other$baseSalary)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Department;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $managerId = this.getManagerId();
        result = result * PRIME + ($managerId == null ? 43 : $managerId.hashCode());
        final java.lang.Object $companyId = this.getCompanyId();
        result = result * PRIME + ($companyId == null ? 43 : $companyId.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $employeeCount = this.getEmployeeCount();
        result = result * PRIME + ($employeeCount == null ? 43 : $employeeCount.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $departmentCode = this.getDepartmentCode();
        result = result * PRIME + ($departmentCode == null ? 43 : $departmentCode.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        final java.lang.Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final java.lang.Object $managerName = this.getManagerName();
        result = result * PRIME + ($managerName == null ? 43 : $managerName.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $baseSalary = this.getBaseSalary();
        result = result * PRIME + ($baseSalary == null ? 43 : $baseSalary.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Department(departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", departmentCode=" + this.getDepartmentCode() + ", parentId=" + this.getParentId() + ", level=" + this.getLevel() + ", children=" + this.getChildren() + ", path=" + this.getPath() + ", status=" + this.getStatus() + ", managerId=" + this.getManagerId() + ", managerName=" + this.getManagerName() + ", companyId=" + this.getCompanyId() + ", description=" + this.getDescription() + ", sortOrder=" + this.getSortOrder() + ", employeeCount=" + this.getEmployeeCount() + ", baseSalary=" + this.getBaseSalary() + ", deleted=" + this.getDeleted() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
