package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.SalaryRecord;
import com.foodtraceability.service.SalaryRecordService;
import com.foodtraceability.service.SalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 薪资批次Controller
 * 处理薪资批次生成和管理相关的API请求
 */
@RestController
@RequestMapping("/v1/salary/batches")
@Tag(name = "薪资批次管理", description = "薪资批次相关接口")
public class SalaryBatchController {

    private static final Logger log = LoggerFactory.getLogger(SalaryBatchController.class);

    private final SalaryRecordService salaryRecordService;
    private final SalaryService salaryService;

    public SalaryBatchController(SalaryRecordService salaryRecordService,
                                  SalaryService salaryService) {
        this.salaryRecordService = salaryRecordService;
        this.salaryService = salaryService;
    }

    /**
     * 生成指定年月的薪资批次
     * @param year 年份
     * @param month 月份
     * @return 生成的记录数
     */
    @Operation(summary = "生成薪资批次")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<Integer> generateBatch(@RequestParam String year, @RequestParam String month) {
        int count = salaryService.generateSalary(year, month);
        log.info("薪资批次生成完成，年月：{}-{}，共生成{}条记录", year, month, count);
        return Result.success(count);
    }

    /**
     * 分页查询薪资批次列表
     * @param page 分页参数
     * @return 薪资批次分页列表
     */
    @Operation(summary = "获取薪资批次列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<IPage<SalaryRecord>> listBatches(Page<SalaryRecord> page) {
        try {
            IPage<SalaryRecord> records = salaryRecordService.page(page);
            return Result.success(records);
        } catch (Exception e) {
            // 容错：表不存在或字段不匹配时返回空页，避免阻塞页面加载
            log.warn("获取薪资批次列表失败，返回空页：{}", e.getMessage());
            Page<SalaryRecord> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(java.util.Collections.emptyList());
            return Result.success(emptyPage);
        }
    }

    /**
     * 根据ID查询薪资批次详情
     * @param id 批次ID
     * @return 薪资批次详情
     */
    @Operation(summary = "获取薪资批次详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<SalaryRecord> getBatchById(@PathVariable Long id) {
        SalaryRecord record = salaryRecordService.getById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        return Result.success(record);
    }

    /**
     * 确认指定ID的薪资批次
     * @param id 批次ID
     * @return 更新后的薪资批次
     */
    @Operation(summary = "确认薪资批次")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<SalaryRecord> confirmBatch(@PathVariable Long id) {
        SalaryRecord record = salaryRecordService.getById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        record.setStatus("已确认");
        salaryRecordService.updateById(record);
        log.info("薪资批次确认完成，记录ID：{}", id);
        return Result.success(record);
    }

    /**
     * 删除指定ID的薪资批次
     * @param id 批次ID
     * @return 无内容响应
     */
    @Operation(summary = "删除薪资批次")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        salaryRecordService.removeById(id);
        log.info("薪资批次删除完成，记录ID：{}", id);
        return Result.success();
    }
}
