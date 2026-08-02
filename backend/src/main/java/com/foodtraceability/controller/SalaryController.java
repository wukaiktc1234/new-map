package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.SalaryAdjustment;
import com.foodtraceability.entity.SalaryRecord;
import com.foodtraceability.entity.SalaryRule;
import com.foodtraceability.entity.User;
import com.foodtraceability.service.SalaryAdjustmentService;
import com.foodtraceability.service.SalaryRecordService;
import com.foodtraceability.service.SalaryRuleService;
import com.foodtraceability.service.SalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资管理Controller
 * 处理薪资相关的API请求
 */
@RestController
@RequestMapping("/v1/salary")
@Tag(name = "薪资管理", description = "薪资相关接口")
public class SalaryController {

    private static final Logger log = LoggerFactory.getLogger(SalaryController.class);

    private final SalaryRecordService salaryRecordService;
    private final SalaryRuleService salaryRuleService;
    private final SalaryAdjustmentService salaryAdjustmentService;
    private final SalaryService salaryService;

    public SalaryController(SalaryRecordService salaryRecordService,
                             SalaryRuleService salaryRuleService,
                             SalaryAdjustmentService salaryAdjustmentService,
                             SalaryService salaryService) {
        this.salaryRecordService = salaryRecordService;
        this.salaryRuleService = salaryRuleService;
        this.salaryAdjustmentService = salaryAdjustmentService;
        this.salaryService = salaryService;
    }

    @Operation(summary = "获取薪资记录列表")
    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<IPage<SalaryRecord>> getSalaryRecords(Page<SalaryRecord> page, @RequestParam(required = false) String employeeName, @RequestParam(required = false) String salaryMonth) {
        IPage<SalaryRecord> salaryRecords = salaryRecordService.page(page);
        return Result.success(salaryRecords);
    }

    @Operation(summary = "获取薪资记录详情")
    @GetMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<SalaryRecord> getSalaryRecordById(@PathVariable Long id) {
        SalaryRecord salaryRecord = salaryRecordService.getById(id);
        if (salaryRecord == null) {
            return Result.error("记录不存在");
        }
        return Result.success(salaryRecord);
    }

    @Operation(summary = "创建薪资记录")
    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<SalaryRecord> createSalaryRecord(@Valid @RequestBody SalaryRecord salaryRecord) {
        salaryRecordService.save(salaryRecord);
        return Result.success(salaryRecord);
    }

    @Operation(summary = "更新薪资记录")
    @PutMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<SalaryRecord> updateSalaryRecord(@PathVariable Long id, @Valid @RequestBody SalaryRecord salaryRecord) {
        salaryRecord.setId(id);
        salaryRecordService.updateById(salaryRecord);
        return Result.success(salaryRecord);
    }

    @Operation(summary = "删除薪资记录")
    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> deleteSalaryRecord(@PathVariable Long id) {
        salaryRecordService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取薪资规则列表")
    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<List<SalaryRule>> getSalaryRules() {
        List<SalaryRule> salaryRules = salaryRuleService.list();
        return Result.success(salaryRules);
    }

    @Operation(summary = "获取薪资规则详情")
    @GetMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<SalaryRule> getSalaryRuleById(@PathVariable Long id) {
        SalaryRule salaryRule = salaryRuleService.getById(id);
        if (salaryRule == null) {
            return Result.error("记录不存在");
        }
        return Result.success(salaryRule);
    }

    @Operation(summary = "创建薪资规则")
    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<SalaryRule> createSalaryRule(@Valid @RequestBody SalaryRule salaryRule) {
        salaryRuleService.save(salaryRule);
        return Result.success(salaryRule);
    }

    @Operation(summary = "更新薪资规则")
    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<SalaryRule> updateSalaryRule(@PathVariable Long id, @Valid @RequestBody SalaryRule salaryRule) {
        salaryRule.setId(id);
        salaryRuleService.updateById(salaryRule);
        return Result.success(salaryRule);
    }

    @Operation(summary = "删除薪资规则")
    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> deleteSalaryRule(@PathVariable Long id) {
        salaryRuleService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取薪资调整列表")
    @GetMapping("/adjustments")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<List<SalaryAdjustment>> getSalaryAdjustments() {
        List<SalaryAdjustment> salaryAdjustments = salaryAdjustmentService.list();
        return Result.success(salaryAdjustments);
    }

    @Operation(summary = "获取薪资调整详情")
    @GetMapping("/adjustments/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<SalaryAdjustment> getSalaryAdjustmentById(@PathVariable Long id) {
        SalaryAdjustment salaryAdjustment = salaryAdjustmentService.getById(id);
        if (salaryAdjustment == null) {
            return Result.error("记录不存在");
        }
        return Result.success(salaryAdjustment);
    }

    @Operation(summary = "创建薪资调整")
    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<SalaryAdjustment> createSalaryAdjustment(@Valid @RequestBody SalaryAdjustment salaryAdjustment) {
        salaryAdjustment.setAdjustmentAmount(salaryAdjustment.getNewSalary().subtract(salaryAdjustment.getOldSalary()));
        salaryAdjustmentService.save(salaryAdjustment);
        return Result.success(salaryAdjustment);
    }

    @Operation(summary = "更新薪资调整")
    @PutMapping("/adjustments/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<SalaryAdjustment> updateSalaryAdjustment(@PathVariable Long id, @Valid @RequestBody SalaryAdjustment salaryAdjustment) {
        salaryAdjustment.setId(id);
        salaryAdjustment.setAdjustmentAmount(salaryAdjustment.getNewSalary().subtract(salaryAdjustment.getOldSalary()));
        salaryAdjustmentService.updateById(salaryAdjustment);
        return Result.success(salaryAdjustment);
    }

    @Operation(summary = "删除薪资调整")
    @DeleteMapping("/adjustments/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> deleteSalaryAdjustment(@PathVariable Long id) {
        salaryAdjustmentService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "生成薪资记录")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> generateSalary(@RequestParam String year, @RequestParam String month) {
        List<User> activeUsers = salaryService.findUsersByStatus("active");
        if (activeUsers == null || activeUsers.isEmpty()) {
            return Result.success(0);
        }
        List<SalaryRule> activeRules = salaryRuleService.list();
        int generatedCount = 0;
        String salaryMonth = year + "-" + (month.length() == 1 ? "0" + month : month);
        for (User user : activeUsers) {
            SalaryRecord salaryRecord = new SalaryRecord();
            salaryRecord.setEmployeeId(user.getId());
            salaryRecord.setEmployeeName(user.getFullName());
            salaryRecord.setDepartment(user.getDepartment());
            salaryRecord.setPosition(user.getRoleNames() != null ? user.getRoleNames() : "员工");
            salaryRecord.setSalaryMonth(salaryMonth);
            salaryRecord.setStatus("待确认");
            BigDecimal basicSalary = salaryService.getRuleAmount(activeRules, "basicSalary", new BigDecimal(5000));
            BigDecimal performanceBonus = salaryService.getRuleAmount(activeRules, "performanceBonus", new BigDecimal(1000));
            BigDecimal overtimePay = salaryService.getRuleAmount(activeRules, "overtimePay", BigDecimal.ZERO);
            BigDecimal allowance = salaryService.getRuleAmount(activeRules, "allowance", new BigDecimal(500));
            BigDecimal totalEarnings = basicSalary.add(performanceBonus).add(overtimePay).add(allowance);
            BigDecimal insurance = salaryService.getRuleAmount(activeRules, "insurance", new BigDecimal(800));
            BigDecimal tax = salaryService.calculateTax(totalEarnings, insurance);
            BigDecimal otherDeductions = BigDecimal.ZERO;
            BigDecimal finalSalary = totalEarnings.subtract(insurance).subtract(tax).subtract(otherDeductions);
            salaryRecord.setBasicSalary(basicSalary);
            salaryRecord.setPerformanceBonus(performanceBonus);
            salaryRecord.setOvertimePay(overtimePay);
            salaryRecord.setAllowance(allowance);
            salaryRecord.setTotalEarnings(totalEarnings);
            salaryRecord.setInsurance(insurance);
            salaryRecord.setTax(tax);
            salaryRecord.setOtherDeductions(otherDeductions);
            salaryRecord.setFinalSalary(finalSalary);
            if (salaryRecordService.save(salaryRecord)) {
                generatedCount++;
            }
        }
        log.info("生成薪资记录完成，共生成{}条记录", generatedCount);
        return Result.success(generatedCount);
    }

    @Operation(summary = "确认薪资发放")
    @PostMapping("/confirm/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<SalaryRecord> confirmSalary(@PathVariable Long id) {
        SalaryRecord salaryRecord = salaryRecordService.getById(id);
        if (salaryRecord == null) {
            return Result.error("记录不存在");
        }
        salaryRecord.setStatus("已确认");
        salaryRecordService.updateById(salaryRecord);
        log.info("薪资确认完成，记录ID：{}", id);
        return Result.success(salaryRecord);
    }
}
