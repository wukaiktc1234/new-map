package com.foodtraceability.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "部门DTO")
public class DepartmentDTO {
    @Schema(description = "部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @Schema(description = "部门名称", example = "生产部")
    private String name;
    @Schema(description = "部门层级", example = "1")
    private Integer level;
    @Schema(description = "员工数量", example = "10")
    private Integer employeeCount;
    @Schema(description = "部门负责人", example = "张三")
    private String manager;
    @Schema(description = "部门类型", example = "部门")
    private String type;
    @Schema(description = "部门状态", example = "active")
    private String status;
    @Schema(description = "子部门列表")
    private List<DepartmentDTO> children;
    @Schema(description = "是否有子节点", example = "true")
    private Boolean hasChildren;
    @Schema(description = "部门编码", example = "DEP001")
    private String code;
    @Schema(description = "父部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    @Schema(description = "排序号", example = "1")
    private Integer sort;
    @Schema(description = "部门描述", example = "负责生产管理")
    private String remark;
    @Schema(description = "部门基准薪资", example = "10000.00")
    private BigDecimal baseSalary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DepartmentDTO> getChildren() {
        return children;
    }

    public void setChildren(List<DepartmentDTO> children) {
        this.children = children;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public DepartmentDTO() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DepartmentDTO)) return false;
        final DepartmentDTO other = (DepartmentDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) return false;
        final java.lang.Object this$employeeCount = this.getEmployeeCount();
        final java.lang.Object other$employeeCount = other.getEmployeeCount();
        if (this$employeeCount == null ? other$employeeCount != null : !this$employeeCount.equals(other$employeeCount)) return false;
        final java.lang.Object this$hasChildren = this.getHasChildren();
        final java.lang.Object other$hasChildren = other.getHasChildren();
        if (this$hasChildren == null ? other$hasChildren != null : !this$hasChildren.equals(other$hasChildren)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$sort = this.getSort();
        final java.lang.Object other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !this$sort.equals(other$sort)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$manager = this.getManager();
        final java.lang.Object other$manager = other.getManager();
        if (this$manager == null ? other$manager != null : !this$manager.equals(other$manager)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children)) return false;
        final java.lang.Object this$code = this.getCode();
        final java.lang.Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$baseSalary = this.getBaseSalary();
        final java.lang.Object other$baseSalary = other.getBaseSalary();
        if (this$baseSalary == null ? other$baseSalary != null : !this$baseSalary.equals(other$baseSalary)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DepartmentDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        final java.lang.Object $employeeCount = this.getEmployeeCount();
        result = result * PRIME + ($employeeCount == null ? 43 : $employeeCount.hashCode());
        final java.lang.Object $hasChildren = this.getHasChildren();
        result = result * PRIME + ($hasChildren == null ? 43 : $hasChildren.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $sort = this.getSort();
        result = result * PRIME + ($sort == null ? 43 : $sort.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $manager = this.getManager();
        result = result * PRIME + ($manager == null ? 43 : $manager.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        final java.lang.Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $baseSalary = this.getBaseSalary();
        result = result * PRIME + ($baseSalary == null ? 43 : $baseSalary.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DepartmentDTO(id=" + this.getId() + ", name=" + this.getName() + ", level=" + this.getLevel() + ", employeeCount=" + this.getEmployeeCount() + ", manager=" + this.getManager() + ", type=" + this.getType() + ", status=" + this.getStatus() + ", children=" + this.getChildren() + ", hasChildren=" + this.getHasChildren() + ", code=" + this.getCode() + ", parentId=" + this.getParentId() + ", sort=" + this.getSort() + ", remark=" + this.getRemark() + ", baseSalary=" + this.getBaseSalary() + ")";
    }
}
