package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.TaxRecordDTO;
import com.foodtraceability.entity.TaxRecord;
import com.foodtraceability.service.TaxRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 税务记录Controller
 *
 * <p>Sprint 3.1 P0 F-003：税务记录管理。</p>
 *
 * <p>路径前缀：/v1/finance/tax-records（原 /tax-records）</p>
 *
 * <p>统一返回 Result&lt;T&gt; 格式，移除 ResponseEntity。</p>
 */
@Tag(name = "税务记录管理", description = "税务记录的CRUD和批量操作")
@RestController
@RequestMapping("/v1/finance/tax-records")
public class TaxRecordController {

    private final TaxRecordService taxRecordService;

    public TaxRecordController(TaxRecordService taxRecordService) {
        this.taxRecordService = taxRecordService;
    }

    /**
     * 分页查询税务记录
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param taxRecordDTO 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询税务记录", description = "支持按条件分页查询")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('finance:tax:view')")
    public Result<IPage<TaxRecord>> getTaxRecordPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            TaxRecordDTO taxRecordDTO) {
        Page<TaxRecord> page = new Page<>(pageNum, pageSize);
        IPage<TaxRecord> result = taxRecordService.getTaxRecordPage(page, taxRecordDTO);
        return Result.success(result);
    }

    /**
     * 根据ID获取税务记录详情
     *
     * @param id 税务记录ID
     * @return 税务记录详情
     */
    @Operation(summary = "获取税务记录详情", description = "根据ID查询税务记录")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:tax:view')")
    public Result<TaxRecord> getTaxRecordById(@PathVariable Long id) {
        TaxRecord taxRecord = taxRecordService.getTaxRecordById(id);
        return Result.success(taxRecord);
    }

    /**
     * 创建税务记录
     *
     * @param taxRecordDTO 税务记录DTO
     * @return 创建结果
     */
    @Operation(summary = "创建税务记录", description = "新增税务记录")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:tax:create')")
    public Result<Boolean> createTaxRecord(@RequestBody TaxRecordDTO taxRecordDTO) {
        boolean result = taxRecordService.createTaxRecord(taxRecordDTO);
        return Result.success(result);
    }

    /**
     * 更新税务记录
     *
     * @param id 税务记录ID
     * @param taxRecordDTO 税务记录DTO
     * @return 更新结果
     */
    @Operation(summary = "更新税务记录", description = "根据ID更新税务记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:tax:update')")
    public Result<Boolean> updateTaxRecord(@PathVariable Long id, @RequestBody TaxRecordDTO taxRecordDTO) {
        boolean result = taxRecordService.updateTaxRecord(id, taxRecordDTO);
        return Result.success(result);
    }

    /**
     * 删除税务记录
     *
     * @param id 税务记录ID
     * @return 删除结果
     */
    @Operation(summary = "删除税务记录", description = "根据ID删除税务记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:tax:delete')")
    public Result<Boolean> deleteTaxRecord(@PathVariable Long id) {
        boolean result = taxRecordService.deleteTaxRecord(id);
        return Result.success(result);
    }

    /**
     * 批量删除税务记录
     *
     * @param ids 税务记录ID列表
     * @return 删除结果
     */
    @Operation(summary = "批量删除税务记录", description = "批量删除税务记录")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('finance:tax:delete')")
    public Result<Boolean> batchDeleteTaxRecord(@RequestBody Long[] ids) {
        boolean result = taxRecordService.batchDeleteTaxRecord(ids);
        return Result.success(result);
    }
}
