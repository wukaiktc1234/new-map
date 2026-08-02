package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HealthCertificate;
import com.foodtraceability.entity.HealthCertificateExpense;
import com.foodtraceability.service.HealthCertificateExpenseService;
import com.foodtraceability.service.HealthCertificateService;
import com.foodtraceability.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康证管理Controller
 * 用于处理健康证相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/hr/health-certificate")
public class HealthCertificateController {
    

    public HealthCertificateController(HealthCertificateService healthCertificateService, HealthCertificateExpenseService healthCertificateExpenseService, OperationLogService operationLogService) {
        this.healthCertificateService = healthCertificateService;
        this.healthCertificateExpenseService = healthCertificateExpenseService;
        this.operationLogService = operationLogService;
    }

    private final HealthCertificateService healthCertificateService;
    
    private final HealthCertificateExpenseService healthCertificateExpenseService;
    
    private final OperationLogService operationLogService;
    
    
    
    /**
     * 获取健康证列表
     * @param page 页码
     * @param size 每页条数
     * @param store 门店
     * @param status 状态
     * @return 健康证列表
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:view')")
    @GetMapping("/list")
    public Result<Page<HealthCertificate>> getHealthCertificateList(
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
     * 健康证统计信息
     * @return 统计数据（总数、有效数、即将过期数、已过期数等）
     */
    @Operation(summary = "健康证统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('hr:health-certificate:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", healthCertificateService.count());
            // 按状态分布：valid=有效, expiring=即将过期, expired=已过期
            statistics.put("valid", healthCertificateService.count(new QueryWrapper<HealthCertificate>().eq("status", "valid")));
            statistics.put("expiring", healthCertificateService.count(new QueryWrapper<HealthCertificate>().eq("status", "expiring")));
            statistics.put("expired", healthCertificateService.count(new QueryWrapper<HealthCertificate>().eq("status", "expired")));
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(5001, "获取统计信息失败");
        }
    }
    
    /**
     * 获取所有健康证（不分页）
     * @return 健康证列表
     */
    @PreAuthorize("hasAnyRole('hr_director', 'admin') or hasAuthority('hr:health-certificate:view')")
    @GetMapping("/all")
    public Result<List<HealthCertificate>> getAllHealthCertificates() {
        List<HealthCertificate> healthCertificates = healthCertificateService.list();
        return Result.success(healthCertificates);
    }
    
    /**
     * 根据ID获取健康证信息
     * @param id 健康证ID
     * @return 健康证信息
     */
    @PreAuthorize("hasAnyRole('employee', 'store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:view')")
    @GetMapping("/{id}")
    public Result<HealthCertificate> getHealthCertificateById(@PathVariable String id) {
        HealthCertificate healthCertificate = healthCertificateService.getById(id);
        return healthCertificate != null ? Result.success(healthCertificate) : Result.error("健康证不存在");
    }
    
    /**
     * 根据员工ID获取健康证信息
     * @param employeeId 员工ID
     * @return 健康证信息
     */
    @PreAuthorize("hasAnyRole('employee', 'store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:view')")
    @GetMapping("/employee/{employeeId}")
    public Result<HealthCertificate> getHealthCertificateByEmployeeId(@PathVariable String employeeId) {
        HealthCertificate healthCertificate = healthCertificateService.getByEmployeeId(employeeId);
        return healthCertificate != null ? Result.success(healthCertificate) : Result.error("健康证不存在");
    }
    
    /**
     * 根据门店获取健康证列表
     * @param store 门店名称
     * @return 健康证列表
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:view')")
    @GetMapping("/store/{store}")
    public Result<List<HealthCertificate>> getHealthCertificateByStore(@PathVariable String store) {
        List<HealthCertificate> healthCertificates = healthCertificateService.getByStore(store);
        return Result.success(healthCertificates);
    }
    
    /**
     * 新增健康证
     * @param healthCertificate 健康证信息
     * @return 新增结果
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PostMapping("/add")
    public Result<String> addHealthCertificate(@RequestBody HealthCertificate healthCertificate, HttpServletRequest request) {
        boolean result = healthCertificateService.save(healthCertificate);
        return result ? Result.success("健康证添加成功") : Result.error("健康证添加失败");
    }
    
    /**
     * 更新健康证
     * @param healthCertificate 健康证信息
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PutMapping("/update")
    public Result<String> updateHealthCertificate(@RequestBody HealthCertificate healthCertificate, HttpServletRequest request) {
        boolean result = healthCertificateService.updateById(healthCertificate);
        return result ? Result.success("健康证更新成功") : Result.error("健康证更新失败");
    }

    /**
     * 删除健康证
     * @param id 健康证ID
     * @return 删除结果
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:delete')")
    @DeleteMapping("/{id}")
    public Result<String> deleteHealthCertificate(@PathVariable String id, HttpServletRequest request) {
        boolean result = healthCertificateService.removeById(id);
        return result ? Result.success("健康证删除成功") : Result.error("健康证删除失败");
    }
    
    /**
     * 批量删除健康证
     * @param ids 健康证ID列表
     * @return 删除结果
     */
    @PreAuthorize("hasAnyRole('hr_director', 'admin') or hasAuthority('hr:health-certificate:delete')")
    @DeleteMapping("/batch")
    public Result<String> batchDeleteHealthCertificate(@RequestBody List<String> ids) {
        boolean result = healthCertificateService.removeByIds(ids);
        return result ? Result.success("健康证批量删除成功") : Result.error("健康证批量删除失败");
    }
    
    /**
     * 更新健康证状态
     * @param id 健康证ID
     * @param status 状态
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PutMapping("/update-status/{id}")
    public Result<String> updateHealthCertificateStatus(@PathVariable String id, @RequestParam String status) {
        boolean result = healthCertificateService.updateStatus(id, status);
        return result ? Result.success("健康证状态更新成功") : Result.error("健康证状态更新失败");
    }
    
    /**
     * 批量更新健康证状态
     * @param status 状态
     * @param ids 健康证ID列表
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PutMapping("/batch-update-status")
    public Result<String> batchUpdateHealthCertificateStatus(@RequestParam String status, @RequestBody List<String> ids) {
        boolean result = healthCertificateService.batchUpdateStatus(ids, status);
        return result ? Result.success("健康证状态批量更新成功") : Result.error("健康证状态批量更新失败");
    }
    
    /**
     * 计算并更新所有健康证的剩余天数和状态
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PostMapping("/calculate-status")
    public Result<String> calculateAndUpdateStatus() {
        healthCertificateService.calculateAndUpdateStatus();
        return Result.success("健康证状态计算更新成功");
    }
    
    /**
     * 发送健康证到期提醒
     * @param days 提前天数
     * @return 发送结果
     */
    @PreAuthorize("hasAnyRole('hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PostMapping("/send-reminders")
    public Result<String> sendExpiryReminders(@RequestParam(defaultValue = "30") Integer days) {
        boolean result = healthCertificateService.sendExpiryReminders(days);
        return result ? Result.success("健康证到期提醒发送成功") : Result.error("健康证到期提醒发送失败");
    }
    
    /**
     * 获取健康证报销记录
     * @param healthCertificateId 健康证ID
     * @return 报销记录列表
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'finance_director', 'admin') or hasAuthority('hr:health-certificate:view')")
    @GetMapping("/expense/{healthCertificateId}")
    public Result<List<HealthCertificateExpense>> getHealthCertificateExpenses(@PathVariable String healthCertificateId) {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getByHealthCertificateId(healthCertificateId);
        return Result.success(expenses);
    }
    
    /**
     * 新增健康证报销记录
     * @param expense 报销记录
     * @return 新增结果
     */
    @PreAuthorize("hasAnyRole('store_manager', 'hr_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PostMapping("/expense/add")
    public Result<String> addHealthCertificateExpense(@RequestBody HealthCertificateExpense expense) {
        // 检查健康证是否已通过审核
        boolean canApply = healthCertificateService.canApplyForExpense(expense.getHealthCertificateId());
        if (!canApply) {
            return Result.error("只有通过审核的健康证才能申请报销");
        }
        
        boolean result = healthCertificateExpenseService.save(expense);
        return result ? Result.success("健康证报销记录添加成功") : Result.error("健康证报销记录添加失败");
    }
    
    /**
     * 更新健康证报销状态
     * @param id 健康证ID
     * @param expenseStatus 报销状态
     * @return 更新结果
     */
    @PreAuthorize("hasAnyRole('hr_director', 'finance_director', 'admin') or hasAuthority('hr:health-certificate:manage')")
    @PutMapping("/update-expense-status/{id}")
    public Result<String> updateHealthCertificateExpenseStatus(@PathVariable String id, @RequestParam String expenseStatus) {
        boolean result = healthCertificateService.updateExpenseStatus(id, expenseStatus);
        return result ? Result.success("健康证报销状态更新成功") : Result.error("健康证报销状态更新失败");
    }
}
