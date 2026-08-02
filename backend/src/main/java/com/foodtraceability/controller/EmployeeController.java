package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.EmployeeBasicInfo;
import com.foodtraceability.dto.EmployeeCreateDTO;
import com.foodtraceability.dto.EmployeeTransferDTO;
import com.foodtraceability.dto.EmployeeUpdateDTO;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.service.EmployeeDataService;
import com.foodtraceability.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工管理控制器
 */
@Tag(name = "员工管理", description = "员工相关接口")
@RestController
@RequestMapping("/v1/employees")
public class EmployeeController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeController.class);

    public EmployeeController(EmployeeService employeeService,
                              EmployeeDataService employeeDataService) {
        this.employeeService = employeeService;
        this.employeeDataService = employeeDataService;
    }

    private final EmployeeService employeeService;
    private final EmployeeDataService employeeDataService;

    @Operation(summary = "获取员工列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Map<String, Object>> getEmployees(@RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = employeeService.getEmployees(current, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取员工列表失败", e);
            return Result.error("获取员工列表失败");
        }
    }

    @Operation(summary = "获取员工总数", description = "获取系统中的员工总数")
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('hr:employee:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getEmployeeCount() {
        try {
            long count = employeeService.count();
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取员工总数失败", e);
            return Result.error("获取员工总数失败");
        }
    }

    @Operation(summary = "员工统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('hr:employee:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", employeeService.count());
            // 按状态分布：1=在职(active), 0=离职(inactive), 2=试用(probation)
            Map<String, Object> statusDistribution = new HashMap<>();
            statusDistribution.put("active", employeeService.count(new QueryWrapper<Employee>().eq("status", 1)));
            statusDistribution.put("inactive", employeeService.count(new QueryWrapper<Employee>().eq("status", 0)));
            statusDistribution.put("probation", employeeService.count(new QueryWrapper<Employee>().eq("status", 2)));
            statistics.put("statusDistribution", statusDistribution);
            // 按性别分布
            Map<String, Object> genderDistribution = new HashMap<>();
            genderDistribution.put("male", employeeService.count(new QueryWrapper<Employee>().eq("gender", "male")));
            genderDistribution.put("female", employeeService.count(new QueryWrapper<Employee>().eq("gender", "female")));
            genderDistribution.put("other", employeeService.count(new QueryWrapper<Employee>().eq("gender", "other")));
            statistics.put("genderDistribution", genderDistribution);
            return Result.success(statistics);
        } catch (Exception e) {
            // 容错：数据库字段不匹配等异常时返回空统计，避免阻塞页面加载
            log.warn("获取员工统计信息失败，返回空统计：{}", e.getMessage());
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("total", 0L);
            Map<String, Object> emptyStatus = new HashMap<>();
            emptyStatus.put("active", 0L);
            emptyStatus.put("inactive", 0L);
            emptyStatus.put("probation", 0L);
            emptyStats.put("statusDistribution", emptyStatus);
            Map<String, Object> emptyGender = new HashMap<>();
            emptyGender.put("male", 0L);
            emptyGender.put("female", 0L);
            emptyGender.put("other", 0L);
            emptyStats.put("genderDistribution", emptyGender);
            return Result.success(emptyStats);
        }
    }

    @Operation(summary = "获取员工详情", description = "根据ID获取员工详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Employee> getEmployeeById(@Parameter(description = "员工ID") @PathVariable String id) {
        try {
            Employee employee = employeeService.getEmployeeDetail(id);
            if (employee == null) {
                return Result.error("员工不存在");
            }
            return Result.success(employee);
        } catch (Exception e) {
            log.error("获取员工详情失败", e);
            return Result.error("获取员工详情失败");
        }
    }

    @Operation(summary = "根据部门获取员工", description = "根据部门ID获取员工列表")
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<List<Employee>> getEmployeesByDepartmentId(@Parameter(description = "部门ID") @PathVariable Long departmentId) {
        try {
            List<Employee> employees = employeeService.getEmployeesByDepartmentId(departmentId);
            return Result.success(employees);
        } catch (Exception e) {
            log.error("获取部门员工列表失败", e);
            return Result.error("获取部门员工列表失败");
        }
    }

    @Operation(summary = "根据职位获取员工", description = "根据职位ID获取员工列表")
    @GetMapping("/position/{positionId}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<List<Employee>> getEmployeesByPositionId(@Parameter(description = "职位ID") @PathVariable Long positionId) {
        try {
            List<Employee> employees = employeeService.getEmployeesByPositionId(positionId);
            return Result.success(employees);
        } catch (Exception e) {
            log.error("获取职位员工列表失败", e);
            return Result.error("获取职位员工列表失败");
        }
    }

    @Operation(summary = "创建员工", description = "创建新员工")
    @PostMapping
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Employee> createEmployee(@Parameter(description = "员工信息") @Valid @RequestBody EmployeeCreateDTO employeeDTO) {
        try {
            Employee employee = employeeService.createEmployee(employeeDTO);
            return Result.success(employee);
        } catch (Exception e) {
            log.error("创建员工失败", e);
            return Result.error("创建员工失败，请联系管理员");
        }
    }

    @Operation(summary = "更新员工", description = "更新员工信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Employee> updateEmployee(@Parameter(description = "员工ID") @PathVariable String id, @Parameter(description = "员工信息") @Valid @RequestBody EmployeeUpdateDTO employeeDTO) {
        try {
            log.info("更新员工信息，员工ID：{}", id);
            Employee updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
            return Result.success(updatedEmployee);
        } catch (Exception e) {
            log.error("更新员工失败", e);
            return Result.error("更新员工失败，请联系管理员");
        }
    }

    @Operation(summary = "员工人事变动", description = "员工调岗、调部门等人事变动")
    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Employee> transferEmployee(@Parameter(description = "员工ID") @PathVariable String id, @Parameter(description = "人事变动信息") @Valid @RequestBody EmployeeTransferDTO transferDTO) {
        try {
            Employee employee = employeeService.transferEmployee(Long.valueOf(id), transferDTO.getNewDepartmentId(), transferDTO.getNewPositionId(), transferDTO.getTransferDate(), transferDTO.getReason());
            return Result.success(employee);
        } catch (Exception e) {
            log.error("员工人事变动失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "员工人事变动失败");
        }
    }

    @Operation(summary = "删除员工", description = "删除员工")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> deleteEmployee(@Parameter(description = "员工ID") @PathVariable String id) {
        try {
            boolean deleted = employeeService.deleteEmployee(id);
            if (deleted) {
                return Result.success();
            }
            return Result.error("删除员工失败");
        } catch (Exception e) {
            log.error("删除员工失败", e);
            return Result.error("删除员工失败");
        }
    }

    @Operation(summary = "批量更新员工部门", description = "批量更新员工所属部门")
    @PutMapping("/batch/department")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> batchUpdateDepartment(@Parameter(description = "员工ID列表") @RequestParam List<Long> employeeIds, @Parameter(description = "部门ID") @RequestParam Long departmentId) {
        try {
            boolean updated = employeeService.batchUpdateDepartment(employeeIds, departmentId);
            if (updated) {
                return Result.success();
            }
            return Result.error("批量更新员工部门失败");
        } catch (Exception e) {
            log.error("批量更新员工部门失败", e);
            return Result.error("批量更新员工部门失败");
        }
    }

    @Operation(summary = "批量更新员工职位", description = "批量更新员工所属职位")
    @PutMapping("/batch/position")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> batchUpdatePosition(@Parameter(description = "员工ID列表") @RequestParam List<Long> employeeIds, @Parameter(description = "职位ID") @RequestParam Long positionId) {
        try {
            boolean updated = employeeService.batchUpdatePosition(employeeIds, positionId);
            if (updated) {
                return Result.success();
            }
            return Result.error("批量更新员工职位失败");
        } catch (Exception e) {
            log.error("批量更新员工职位失败", e);
            return Result.error("批量更新员工职位失败");
        }
    }

    @Operation(summary = "获取部门员工数量", description = "获取指定部门的员工数量")
    @GetMapping("/count/department/{departmentId}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Integer> getEmployeeCountByDepartmentId(@Parameter(description = "部门ID") @PathVariable Long departmentId) {
        try {
            Integer count = employeeService.getEmployeeCountByDepartmentId(departmentId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取部门员工数量失败", e);
            return Result.error("获取部门员工数量失败");
        }
    }

    @Operation(summary = "获取职位员工数量", description = "获取指定职位的员工数量")
    @GetMapping("/count/position/{positionId}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Integer> getEmployeeCountByPositionId(@Parameter(description = "职位ID") @PathVariable Long positionId) {
        try {
            Integer count = employeeService.getEmployeeCountByPositionId(positionId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取职位员工数量失败", e);
            return Result.error("获取职位员工数量失败");
        }
    }

    @Operation(summary = "批量获取员工基本信息", description = "根据员工ID列表批量获取员工基本信息")
    @PostMapping("/batch/basic-info")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Map<String, EmployeeBasicInfo>> batchGetEmployeeBasicInfo(@Parameter(description = "员工ID列表") @RequestBody List<String> employeeIds) {
        try {
            Map<String, EmployeeBasicInfo> result = employeeDataService.batchGetEmployeeBasicInfo(employeeIds);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量获取员工基本信息失败", e);
            return Result.error("批量获取员工基本信息失败");
        }
    }

    @Operation(summary = "获取员工基本信息", description = "根据员工ID获取员工基本信息")
    @GetMapping("/basic-info/{employeeId}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<EmployeeBasicInfo> getEmployeeBasicInfo(@Parameter(description = "员工ID") @PathVariable String employeeId) {
        try {
            EmployeeBasicInfo result = employeeDataService.getEmployeeBasicInfo(employeeId);
            if (result == null) {
                return Result.error("员工不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取员工基本信息失败", e);
            return Result.error("获取员工基本信息失败");
        }
    }
}
