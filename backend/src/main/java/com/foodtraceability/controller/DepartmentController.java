package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DepartmentBasicInfo;
import com.foodtraceability.dto.DepartmentDTO;
import com.foodtraceability.entity.Department;
import com.foodtraceability.service.DepartmentDataService;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.utils.DepartmentConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 部门管理控制器
 */
@Tag(name = "部门管理", description = "部门相关接口")
@RestController
@RequestMapping("/v1/departments")
public class DepartmentController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;
    private final DepartmentDataService departmentDataService;

    public DepartmentController(DepartmentService departmentService, DepartmentDataService departmentDataService) {
        this.departmentService = departmentService;
        this.departmentDataService = departmentDataService;
    }

    /**
     * 获取部门树结构
     */
    @Operation(summary = "获取部门树结构")
    @GetMapping("/tree")
    public Result<List<DepartmentDTO>> getDepartmentTree() {
        List<Department> tree = departmentService.getDepartmentTree();
        List<DepartmentDTO> dtoTree = DepartmentConverter.toDTOTree(tree);
        return Result.success(dtoTree);
    }

    /**
     * 调试：获取原始部门数据（用于检查字符编码问题）
     */
    @Operation(summary = "调试：获取原始部门数据（用于检查字符编码问题）")
    @GetMapping("/debug/raw")
    @PreAuthorize("hasAnyRole('admin')")
    public Result<List<Department>> getRawDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return Result.success(departments);
    }

    /**
     * 获取部门列表（分页）
     */
    @Operation(summary = "获取部门列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<List<Department>> getDepartments(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            if (departments == null || departments.isEmpty()) {
                return Result.success(java.util.Collections.emptyList());
            }
            int start = (current - 1) * size;
            if (start < 0) {
                start = 0;
            }
            if (start >= departments.size()) {
                return Result.success(java.util.Collections.emptyList());
            }
            int end = Math.min(start + size, departments.size());
            List<Department> pageData = departments.subList(start, end);
            return Result.success(pageData);
        } catch (Exception e) {
            // 容错：表不存在或字段不匹配时返回空列表，避免阻塞页面加载
            log.warn("获取部门列表失败，返回空列表：{}", e.getMessage());
            return Result.success(java.util.Collections.emptyList());
        }
    }

    /**
     * 获取部门详情
     */
    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Department> getDepartment(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        if (department == null) {
            return Result.error(404, "部门不存在");
        }
        return Result.success(department);
    }

    /**
     * 创建部门
     */
    @Operation(summary = "创建部门")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Department> createDepartment(@Valid @RequestBody Department department) {
        Department created = departmentService.createDepartment(department);
        return Result.success(created);
    }

    /**
     * 更新部门
     */
    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Department> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDTO departmentDTO) {
        Department department = new Department();
        department.setDepartmentId(id);
        department.setDepartmentName(departmentDTO.getName());
        department.setDepartmentCode(departmentDTO.getCode());
        department.setParentId(departmentDTO.getParentId());
        department.setLevel(departmentDTO.getLevel());
        department.setManagerName(departmentDTO.getManager());
        department.setDescription(departmentDTO.getRemark());
        department.setSortOrder(departmentDTO.getSort());
        // 同步组织类型 type 字段（company/department/store/team/office/group/warehouse）
        if (departmentDTO.getType() != null && !departmentDTO.getType().trim().isEmpty()) {
            department.setType(departmentDTO.getType());
        }
        // 仅在显式传入 status 时更新状态，避免前端未传导致部门被误禁用
        if (departmentDTO.getStatus() != null && !departmentDTO.getStatus().trim().isEmpty()) {
            Integer statusValue = "active".equals(departmentDTO.getStatus()) ? 1 : 0;
            department.setStatus(statusValue);
        }
        Department updated = departmentService.updateDepartment(id, department);
        return Result.success(updated);
    }

    /**
     * 删除部门
     */
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }

    /**
     * 更新部门状态
     */
    @Operation(summary = "更新部门状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> updateDepartmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Integer statusValue = "active".equals(status) ? 1 : 0;
        departmentService.updateDepartmentStatus(id, statusValue);
        return Result.success();
    }

    /**
     * 更新部门状态（级联）
     */
    @Operation(summary = "更新部门状态（级联）")
    @PutMapping("/{id}/status/cascade")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> updateDepartmentStatusWithCascade(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Integer statusValue = "active".equals(status) ? 1 : 0;
        departmentService.updateDepartmentStatusWithCascade(id, statusValue);
        return Result.success();
    }

    /**
     * 更新部门排序
     */
    @Operation(summary = "更新部门排序")
    @PutMapping("/{id}/sort")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> updateDepartmentSortOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer sortOrder = body.get("sortOrder");
        if (sortOrder == null) {
            sortOrder = body.get("sort");
        }
        departmentService.updateDepartmentSortOrder(id, sortOrder);
        return Result.success();
    }

    /**
     * 获取子部门（懒加载）
     */
    @Operation(summary = "获取子部门（懒加载）")
    @GetMapping("/{parentId}/children")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<DepartmentDTO>> getChildrenDepartments(@PathVariable Long parentId) {
        List<DepartmentDTO> children = departmentService.getChildrenDepartments(parentId);
        return Result.success(children);
    }

    /**
     * 调试：检查JVM编码设置
     */
    @Operation(summary = "调试：检查JVM编码设置")
    @GetMapping("/debug/encoding")
    @PreAuthorize("hasAnyRole('admin')")
    public Result<Map<String, String>> checkEncoding() {
        Map<String, String> encodingInfo = new HashMap<>();
        encodingInfo.put("file.encoding", System.getProperty("file.encoding"));
        encodingInfo.put("sun.jnu.encoding", System.getProperty("sun.jnu.encoding"));
        encodingInfo.put("defaultCharset", java.nio.charset.Charset.defaultCharset().name());
        encodingInfo.put("testChinese", "测试中文");
        return Result.success(encodingInfo);
    }

    /**
     * 获取部门员工数量统计
     */
    @Operation(summary = "获取部门员工数量统计")
    @GetMapping("/employee-counts")
    public Result<Map<String, Integer>> getDepartmentEmployeeCounts() {
        Map<String, Integer> counts = departmentService.getDepartmentEmployeeCounts();
        return Result.success(counts);
    }

    /**
     * 清空所有部门数据（危险操作，仅管理员可执行）
     */
    @Operation(summary = "清空所有部门数据（危险操作）")
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> clearAllDepartments() {
        departmentService.clearAllDepartments();
        return Result.success();
    }

    /**
     * 批量获取部门基本信息
     */
    @Operation(summary = "批量获取部门基本信息", description = "根据部门ID列表批量获取部门基本信息")
    @PostMapping("/batch/basic-info")
    public Result<Map<Long, DepartmentBasicInfo>> batchGetDepartmentBasicInfo(
            @Parameter(description = "部门ID列表") @RequestBody List<String> departmentIds) {
        try {
            List<Long> longIds = departmentIds.stream()
                    .map(id -> {
                        try {
                            return Long.parseLong(id);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            Map<Long, DepartmentBasicInfo> result = departmentDataService.batchGetDepartmentBasicInfo(longIds);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("批量获取部门基本信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个部门基本信息
     */
    @Operation(summary = "获取部门基本信息", description = "根据部门ID获取部门基本信息")
    @GetMapping("/basic-info/{departmentId}")
    public Result<DepartmentBasicInfo> getDepartmentBasicInfo(
            @Parameter(description = "部门ID") @PathVariable Long departmentId) {
        try {
            DepartmentBasicInfo result = departmentDataService.getDepartmentBasicInfo(departmentId);
            if (result == null) {
                return Result.error("部门不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取部门基本信息失败: " + e.getMessage());
        }
    }
}
