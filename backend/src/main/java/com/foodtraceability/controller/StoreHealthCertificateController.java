package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HealthCertificate;
import com.foodtraceability.entity.HealthCertificateExpense;
import com.foodtraceability.service.HealthCertificateExpenseService;
import com.foodtraceability.service.HealthCertificateService;
import com.foodtraceability.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店端健康证管理Controller
 * 用于处理门店端健康证相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/store/health-certificate")
public class StoreHealthCertificateController {
    

    public StoreHealthCertificateController(HealthCertificateService healthCertificateService, HealthCertificateExpenseService healthCertificateExpenseService, OperationLogService operationLogService) {
        this.healthCertificateService = healthCertificateService;
        this.healthCertificateExpenseService = healthCertificateExpenseService;
        this.operationLogService = operationLogService;
    }

    private final HealthCertificateService healthCertificateService;
    
    private final HealthCertificateExpenseService healthCertificateExpenseService;
    
    private final OperationLogService operationLogService;
    
    
    
    /**
     * 获取门店健康证列表
     * @param page 页码
     * @param size 每页条数
     * @param store 门店
     * @param status 状态
     * @return 健康证列表
     */
    @PreAuthorize("hasAnyRole('store_manager', 'employee', 'admin', 'admin')")
    @GetMapping("/list")
    public Result<Page<HealthCertificate>> getStoreHealthCertificateList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String status) {
        
        QueryWrapper<HealthCertificate> queryWrapper = new QueryWrapper<>();
        if (store != null && !store.isEmpty()) {
            queryWrapper.eq("store", store);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        Page<HealthCertificate> healthCertificatePage = healthCertificateService.page(new Page<>(page, size), queryWrapper);
        return Result.success(healthCertificatePage);
    }
    
    /**
     * 根据ID获取健康证信息
     * @param id 健康证ID
     * @return 健康证信息
     */
    @PreAuthorize("hasAnyRole('store_manager', 'store_employee', 'admin', 'admin')")
    @GetMapping("/{id}")
    public Result<HealthCertificate> getStoreHealthCertificateById(@PathVariable String id) {
        HealthCertificate healthCertificate = healthCertificateService.getById(id);
        return healthCertificate != null ? Result.success(healthCertificate) : Result.error("健康证不存在");
    }
    
    /**
     * 根据员工ID获取健康证信息
     * @param employeeId 员工ID
     * @return 健康证信息
     */
    @PreAuthorize("hasAnyRole('store_manager', 'employee', 'admin', 'admin')")
    @GetMapping("/employee/{employeeId}")
    public Result<HealthCertificate> getStoreHealthCertificateByEmployeeId(@PathVariable String employeeId) {
        HealthCertificate healthCertificate = healthCertificateService.getByEmployeeId(employeeId);
        return healthCertificate != null ? Result.success(healthCertificate) : Result.error("健康证不存在");
    }
    
    /**
     * 新增健康证（门店端）
     * @param healthCertificate 健康证信息
     * @return 新增结果
     */
    @PreAuthorize("hasAnyRole('store_manager') or hasAuthority('hr:health-certificate:manage')")
    @PostMapping("/add")
    public Result<String> addStoreHealthCertificate(@RequestBody HealthCertificate healthCertificate, HttpServletRequest request) {
        boolean result = healthCertificateService.save(healthCertificate);
        return result ? Result.success("健康证添加成功") : Result.error("健康证添加失败");
    }

    /**
     * 更新健康证（门店端）
     * @param healthCertificate 健康证信息
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('store_manager') or hasAuthority('hr:health-certificate:manage')")
    @PutMapping("/update")
    public Result<String> updateStoreHealthCertificate(@RequestBody HealthCertificate healthCertificate, HttpServletRequest request) {
        boolean result = healthCertificateService.updateById(healthCertificate);
        return result ? Result.success("健康证更新成功") : Result.error("健康证更新失败");
    }

    /**
     * 删除健康证（门店端）
     * @param id 健康证ID
     * @return 删除结果
     */
    @PreAuthorize("hasAnyRole('store_manager') or hasAuthority('hr:health-certificate:delete')")
    @DeleteMapping("/{id}")
    public Result<String> deleteStoreHealthCertificate(@PathVariable String id, HttpServletRequest request) {
        boolean result = healthCertificateService.removeById(id);
        return result ? Result.success("健康证删除成功") : Result.error("健康证删除失败");
    }
    
    /**
     * 获取门店健康证报销记录
     * @param healthCertificateId 健康证ID
     * @return 报销记录列表
     */
    @PreAuthorize("hasAnyRole('store_manager', 'employee', 'admin', 'admin')")
    @GetMapping("/expense/{healthCertificateId}")
    public Result<List<HealthCertificateExpense>> getStoreHealthCertificateExpenses(@PathVariable String healthCertificateId) {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getByHealthCertificateId(healthCertificateId);
        return Result.success(expenses);
    }
    
    /**
     * 新增健康证报销记录（门店端）
     * @param expense 报销记录
     * @return 新增结果
     */
    @PreAuthorize("hasAnyRole('store_manager') or hasAuthority('hr:health-certificate:manage')")
    @PostMapping("/expense/add")
    public Result<String> addStoreHealthCertificateExpense(@RequestBody HealthCertificateExpense expense) {
        // 检查健康证是否已通过审核
        boolean canApply = healthCertificateService.canApplyForExpense(expense.getHealthCertificateId());
        if (!canApply) {
            return Result.error("只有通过审核的健康证才能申请报销");
        }

        boolean result = healthCertificateExpenseService.save(expense);
        return result ? Result.success("健康证报销记录添加成功") : Result.error("健康证报销记录添加失败");
    }

    /**
     * 分页查询所有费用报销记录
     * 支持按状态筛选，供费用管理页面使用
     *
     * @param page 页码
     * @param size 每页条数
     * @param status 报销状态（可选）
     * @return 分页费用记录
     */
    @PreAuthorize("hasAnyRole('store_manager', 'admin', 'admin')")
    @GetMapping("/expense/all")
    public Result<Page<HealthCertificateExpense>> getAllExpenseRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        QueryWrapper<HealthCertificateExpense> queryWrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("create_time");
        Page<HealthCertificateExpense> result = healthCertificateExpenseService.page(
                new Page<>(page, size), queryWrapper);
        return Result.success(result);
    }

    /**
     * 更新费用报销状态
     * 用于审批流程：人事初审 → 财务审核 → 报销处理
     *
     * @param id 报销记录ID
     * @param body 包含 status 和 approver 的请求体
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('store_manager', 'admin', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PutMapping("/expense/{id}/status")
    public Result<String> updateExpenseStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.trim().isEmpty()) {
            return Result.error("报销状态不能为空");
        }
        boolean result = healthCertificateExpenseService.updateStatus(id, status);
        return result ? Result.success("报销状态更新成功") : Result.error("报销状态更新失败");
    }
}