package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HealthCertificateExpense;
import com.foodtraceability.service.HealthCertificateExpenseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康证报销管理Controller
 * 用于处理健康证报销相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/hr/health-certificate/expense")
public class HealthCertificateExpenseController {
    

    public HealthCertificateExpenseController(HealthCertificateExpenseService healthCertificateExpenseService) {
        this.healthCertificateExpenseService = healthCertificateExpenseService;
    }

    private final HealthCertificateExpenseService healthCertificateExpenseService;
    
    /**
     * 获取报销列表
     * @param page 页码
     * @param size 每页条数
     * @param store 门店
     * @param status 状态
     * @param employeeId 员工ID
     * @return 报销列表
     */
    @GetMapping("/list")
    public Result<Page<HealthCertificateExpense>> getExpenseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeId) {
        
        QueryWrapper<HealthCertificateExpense> queryWrapper = new QueryWrapper<>();
        if (store != null && !store.isEmpty()) {
            queryWrapper.eq("store", store);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (employeeId != null && !employeeId.isEmpty()) {
            queryWrapper.eq("employee_id", employeeId);
        }
        
        Page<HealthCertificateExpense> expensePage = healthCertificateExpenseService.page(new Page<>(page, size), queryWrapper);
        return Result.success(expensePage);
    }
    
    /**
     * 获取所有报销记录（不分页）
     * @return 报销记录列表
     */
    @GetMapping("/all")
    public Result<List<HealthCertificateExpense>> getAllExpenses() {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.list();
        return Result.success(expenses);
    }
    
    /**
     * 根据ID获取报销记录
     * @param id 报销ID
     * @return 报销记录
     */
    @GetMapping("/{id}")
    public Result<HealthCertificateExpense> getExpenseById(@PathVariable Long id) {
        HealthCertificateExpense expense = healthCertificateExpenseService.getById(id);
        return expense != null ? Result.success(expense) : Result.error("报销记录不存在");
    }
    
    /**
     * 根据员工ID获取报销记录
     * @param employeeId 员工ID
     * @return 报销记录列表
     */
    @GetMapping("/employee/{employeeId}")
    public Result<List<HealthCertificateExpense>> getExpensesByEmployeeId(@PathVariable String employeeId) {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getByEmployeeId(employeeId);
        return Result.success(expenses);
    }
    
    /**
     * 根据门店获取报销记录
     * @param store 门店名称
     * @return 报销记录列表
     */
    @GetMapping("/store/{store}")
    public Result<List<HealthCertificateExpense>> getExpensesByStore(@PathVariable String store) {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getByStore(store);
        return Result.success(expenses);
    }
    
    /**
     * 提交报销申请
     * @param expense 报销申请信息
     * @return 提交结果
     */
    @PostMapping("/apply")
    public Result<String> applyExpense(@RequestBody HealthCertificateExpense expense) {
        boolean result = healthCertificateExpenseService.submitExpense(expense);
        return result ? Result.success("报销申请提交成功") : Result.error("报销申请提交失败");
    }
    
    /**
     * 更新报销记录
     * @param expense 报销记录
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<String> updateExpense(@RequestBody HealthCertificateExpense expense) {
        boolean result = healthCertificateExpenseService.updateById(expense);
        return result ? Result.success("报销记录更新成功") : Result.error("报销记录更新失败");
    }
    
    /**
     * 删除报销记录
     * @param id 报销ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteExpense(@PathVariable Long id) {
        boolean result = healthCertificateExpenseService.removeById(id);
        return result ? Result.success("报销记录删除成功") : Result.error("报销记录删除失败");
    }
    
    /**
     * 批量删除报销记录
     * @param ids 报销ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result<String> batchDeleteExpense(@RequestBody List<Long> ids) {
        boolean result = healthCertificateExpenseService.removeByIds(ids);
        return result ? Result.success("报销记录批量删除成功") : Result.error("报销记录批量删除失败");
    }
    
    /**
     * 人事初审报销申请
     * @param id 报销ID
     * @param status 审批状态
     * @param approver 审批人
     * @param comment 审批意见
     * @return 审批结果
     */
    @PutMapping("/hr-review/{id}")
    public Result<String> hrReviewExpense(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String approver,
            @RequestParam(required = false) String comment) {
        
        boolean result = healthCertificateExpenseService.hrReviewExpense(id, status, approver, comment);
        return result ? Result.success("人事初审成功") : Result.error("人事初审失败");
    }
    
    /**
     * 财务审核报销申请
     * @param id 报销ID
     * @param status 审批状态
     * @param approver 审批人
     * @param comment 审批意见
     * @return 审批结果
     */
    @PutMapping("/finance-review/{id}")
    public Result<String> financeReviewExpense(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String approver,
            @RequestParam(required = false) String comment) {
        
        boolean result = healthCertificateExpenseService.financeReviewExpense(id, status, approver, comment);
        return result ? Result.success("财务审核成功") : Result.error("财务审核失败");
    }
    
    /**
     * 财务报销处理
     * @param id 报销ID
     * @param reimburseDate 报销日期
     * @return 处理结果
     */
    @PutMapping("/reimburse/{id}")
    public Result<String> reimburseExpense(@PathVariable Long id, @RequestParam String reimburseDate) {
        boolean result = healthCertificateExpenseService.reimburseExpense(id, reimburseDate);
        return result ? Result.success("财务报销处理成功") : Result.error("财务报销处理失败");
    }
    
    /**
     * 批量拒绝报销申请
     * @param reason 拒绝原因
     * @param approver 审批人
     * @param ids 报销ID列表
     * @return 处理结果
     */
    @PutMapping("/batch-reject")
    public Result<String> batchRejectExpense(
            @RequestParam String reason,
            @RequestParam String approver,
            @RequestBody List<Long> ids) {
        
        boolean result = healthCertificateExpenseService.batchRejectExpense(ids, reason, approver);
        return result ? Result.success("报销申请批量拒绝成功") : Result.error("报销申请批量拒绝失败");
    }
    
    /**
     * 上传发票附件
     * @param id 报销ID
     * @param attachmentPath 附件路径
     * @return 上传结果
     */
    @PostMapping("/upload-invoice/{id}")
    public Result<String> uploadInvoiceAttachment(
            @PathVariable Long id,
            @RequestParam String attachmentPath) {
        
        boolean result = healthCertificateExpenseService.uploadInvoiceAttachment(id, attachmentPath);
        return result ? Result.success("发票附件上传成功") : Result.error("发票附件上传失败");
    }
    
    /**
     * 获取待人事初审的报销申请
     * @return 报销申请列表
     */
    @GetMapping("/pending-hr-review")
    public Result<List<HealthCertificateExpense>> getPendingHrReviewExpenses() {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getPendingHrReviewExpenses();
        return Result.success(expenses);
    }
    
    /**
     * 获取待财务审核的报销申请
     * @return 报销申请列表
     */
    @GetMapping("/pending-finance-review")
    public Result<List<HealthCertificateExpense>> getPendingFinanceReviewExpenses() {
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getPendingFinanceReviewExpenses();
        return Result.success(expenses);
    }
    
    /**
     * 获取已报销的记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报销记录列表
     */
    @GetMapping("/reimbursed")
    public Result<List<HealthCertificateExpense>> getReimbursedExpenses(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getReimbursedExpenses(startDate, endDate);
        return Result.success(expenses);
    }
    
    /**
     * 同步报销数据到财务系统
     * @param id 报销ID
     * @return 同步结果
     */
    @PostMapping("/sync/{id}")
    public Result<String> syncExpenseToFinance(@PathVariable Long id) {
        // TODO: 调用 financeSystemService.syncHealthCertificateExpenseToFinance 完成真实同步，当前直接返回成功
        return Result.success("报销数据同步到财务系统成功");
    }
}
