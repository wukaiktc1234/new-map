package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HR模块占位接口控制器
 *
 * 提供前端页面所需但后端尚未实现完整业务逻辑的 HR 相关接口的占位实现。
 * 这些接口返回空数据或默认值，确保前端页面不会因 404 错误而无法加载。
 *
 * 后续应逐步替换为完整的业务实现。
 *
 * 路径前缀：/v1/hr
 *
 * 占位接口列表：
 * 1. GET /v1/hr/organizations/tree       - 组织架构树
 * 2. GET /v1/hr/config/sick-pay          - 病假扣薪配置
 * 3. GET /v1/hr/approvals                - HR审批列表
 * 4. GET /v1/hr/invitation-codes         - 邀请码列表
 */
@RestController
@RequestMapping("/v1/hr")
@Tag(name = "HR模块占位接口", description = "HR模块尚未实现完整业务逻辑的接口占位")
public class HrStubController {

    /**
     * 获取组织架构树
     * 对应前端调用：GET /v1/hr/organizations/tree
     * 当前为占位实现，返回空数组
     */
    @GetMapping("/organizations/tree")
    @Operation(summary = "获取组织架构树", description = "返回组织架构树结构（占位接口，返回空数据）")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<Map<String, Object>>> getOrganizationTree() {
        return Result.success(new ArrayList<>());
    }

    /**
     * 获取病假扣薪配置
     * 对应前端调用：GET /v1/hr/config/sick-pay
     * 当前为占位实现，返回默认配置
     */
    @GetMapping("/config/sick-pay")
    @Operation(summary = "获取病假扣薪配置", description = "返回病假扣薪配置（占位接口，返回默认配置）")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Map<String, Object>> getSickPayConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("id", "default");
        config.put("name", "默认病假扣薪配置");

        Map<String, Object> defaultRegion = new HashMap<>();
        defaultRegion.put("regionCode", "DEFAULT");
        defaultRegion.put("regionName", "默认地区");
        defaultRegion.put("dailyBaseRate", 100);
        defaultRegion.put("deductionRatePerDay", 50);
        defaultRegion.put("maxDeductionDays", 30);
        defaultRegion.put("minSickPay", 0);
        defaultRegion.put("enabled", true);

        config.put("defaultRegion", defaultRegion);
        config.put("regions", new ArrayList<>());
        config.put("enabled", true);
        config.put("remark", "占位配置，待业务实现");
        return Result.success(config);
    }

    /**
     * 获取扣薪规则配置
     * 对应前端调用：GET /v1/hr/config/deduction
     * 当前为占位实现，返回默认配置
     */
    @GetMapping("/config/deduction")
    @Operation(summary = "获取扣薪规则配置", description = "返回扣薪规则配置（占位接口，返回默认配置）")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Map<String, Object>> getDeductionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("id", "deduction_default");
        config.put("name", "扣薪规则配置（默认）");
        config.put("standardWorkHours", 8);
        config.put("standardWorkDays", 21.75);
        config.put("fullAttendanceBonus", 200);
        config.put("rules", new ArrayList<>());
        return Result.success(config);
    }

    /**
     * 获取加班调休配置
     * 对应前端调用：GET /v1/hr/config/overtime
     * 当前为占位实现，返回默认配置
     */
    @GetMapping("/config/overtime")
    @Operation(summary = "获取加班调休配置", description = "返回加班调休配置（占位接口，返回默认配置）")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Map<String, Object>> getOvertimeConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("id", "overtime_default");
        config.put("name", "加班调休配置（默认）");
        config.put("dailyOvertimeLimit", 3);
        config.put("monthlyOvertimeLimit", 36);
        config.put("approvalRequired", true);
        config.put("rules", new ArrayList<>());
        return Result.success(config);
    }

    /**
     * 获取个税计算配置
     * 对应前端调用：GET /v1/hr/config/tax
     * 当前为占位实现，返回默认配置
     */
    @GetMapping("/config/tax")
    @Operation(summary = "获取个税计算配置", description = "返回个税计算配置（占位接口，返回默认配置）")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Map<String, Object>> getTaxConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("id", "tax_default");
        config.put("name", "个税计算配置（默认）");
        config.put("taxThreshold", 5000);
        config.put("taxBrackets", new ArrayList<>());
        config.put("specialDeductions", new ArrayList<>());
        config.put("insurancePreTax", true);
        return Result.success(config);
    }

    /**
     * 获取HR审批列表
     * 对应前端调用：GET /v1/hr/approvals
     * 当前为占位实现，返回空分页结果
     */
    @GetMapping("/approvals")
    @Operation(summary = "获取HR审批列表", description = "分页返回HR审批记录（占位接口，返回空数据）")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getApprovals(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", new ArrayList<>());
        result.put("total", 0);
        result.put("current", page);
        result.put("size", pageSize);
        result.put("pages", 0);
        return Result.success(result);
    }

    /**
     * 获取邀请码列表
     * 对应前端调用：GET /v1/hr/invitation-codes
     * 当前为占位实现，返回空分页结果
     */
    @GetMapping("/invitation-codes")
    @Operation(summary = "获取邀请码列表", description = "分页返回邀请码列表（占位接口，返回空数据）")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Map<String, Object>> getInvitationCodes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", new ArrayList<>());
        result.put("total", 0);
        result.put("current", page);
        result.put("size", pageSize);
        result.put("pages", 0);
        return Result.success(result);
    }
}
