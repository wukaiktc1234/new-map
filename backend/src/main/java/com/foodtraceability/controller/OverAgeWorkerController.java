package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.hr.OverAgeWorkerCreateDTO;
import com.foodtraceability.dto.hr.OverAgeWorkerUpdateDTO;
import com.foodtraceability.dto.hr.OverAgeWorkerVO;
import com.foodtraceability.entity.OverAgeWorker;
import com.foodtraceability.service.OverAgeWorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 超龄劳动者 Controller
 * 处理超龄劳动者相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/over-age-workers")
@Tag(name = "超龄劳动者管理", description = "超龄劳动者相关接口")
public class OverAgeWorkerController {

    private final OverAgeWorkerService overAgeWorkerService;

    public OverAgeWorkerController(OverAgeWorkerService overAgeWorkerService) {
        this.overAgeWorkerService = overAgeWorkerService;
    }

    /**
     * 分页查询超龄劳动者列表
     * @param page 页码
     * @param size 每页条数
     * @param keyword 关键字搜索
     * @param healthCheckResult 健康体检结果筛选
     * @return 分页结果
     */
    @Operation(summary = "分页查询超龄劳动者列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('*')")
    public Result<Page<OverAgeWorkerVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String healthCheckResult) {
        IPage<OverAgeWorker> entityPage = overAgeWorkerService.getList(page, size, keyword, healthCheckResult);
        Page<OverAgeWorkerVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return Result.success(voPage);
    }

    /**
     * 根据ID获取超龄劳动者详情
     * @param id 主键ID
     * @return 超龄劳动者详情
     */
    @Operation(summary = "获取超龄劳动者详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('*')")
    public Result<OverAgeWorkerVO> getById(@PathVariable Long id) {
        OverAgeWorker entity = overAgeWorkerService.getById(id);
        if (entity == null) {
            return Result.error("超龄劳动者记录不存在");
        }
        return Result.success(convertToVO(entity));
    }

    /**
     * 创建超龄劳动者记录
     * @param createDTO 创建参数
     * @return 创建结果
     */
    @Operation(summary = "创建超龄劳动者记录")
    @PostMapping
    @PreAuthorize("hasAuthority('*')")
    public Result<OverAgeWorkerVO> create(@Valid @RequestBody OverAgeWorkerCreateDTO createDTO) {
        OverAgeWorker entity = new OverAgeWorker();
        entity.setEmployeeId(createDTO.getEmployeeId());
        entity.setEmployeeName(createDTO.getEmployeeName());
        entity.setRetirementDate(createDTO.getRetirementDate());
        entity.setReemploymentDate(createDTO.getReemploymentDate());
        entity.setAgreementNo(createDTO.getAgreementNo());
        entity.setAgreementStartDate(createDTO.getAgreementStartDate());
        entity.setAgreementEndDate(createDTO.getAgreementEndDate());
        entity.setWorkInjuryInsuranceNo(createDTO.getWorkInjuryInsuranceNo());
        entity.setWorkInjuryInsuranceExpiry(createDTO.getWorkInjuryInsuranceExpiry());
        entity.setHealthCheckExpiry(createDTO.getHealthCheckExpiry());
        entity.setHealthCheckResult(createDTO.getHealthCheckResult());
        entity.setRestrictedPositions(createDTO.getRestrictedPositions());
        entity.setRemark(createDTO.getRemark());

        OverAgeWorker created = overAgeWorkerService.create(entity);
        return Result.success(convertToVO(created));
    }

    /**
     * 更新超龄劳动者记录
     * @param id 主键ID
     * @param updateDTO 更新参数
     * @return 更新结果
     */
    @Operation(summary = "更新超龄劳动者记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('*')")
    public Result<OverAgeWorkerVO> update(@PathVariable Long id, @RequestBody OverAgeWorkerUpdateDTO updateDTO) {
        OverAgeWorker existing = overAgeWorkerService.getById(id);
        if (existing == null) {
            return Result.error("超龄劳动者记录不存在");
        }

        OverAgeWorker entity = new OverAgeWorker();
        entity.setEmployeeId(updateDTO.getEmployeeId());
        entity.setEmployeeName(updateDTO.getEmployeeName());
        entity.setRetirementDate(updateDTO.getRetirementDate());
        entity.setReemploymentDate(updateDTO.getReemploymentDate());
        entity.setAgreementNo(updateDTO.getAgreementNo());
        entity.setAgreementStartDate(updateDTO.getAgreementStartDate());
        entity.setAgreementEndDate(updateDTO.getAgreementEndDate());
        entity.setWorkInjuryInsuranceNo(updateDTO.getWorkInjuryInsuranceNo());
        entity.setWorkInjuryInsuranceExpiry(updateDTO.getWorkInjuryInsuranceExpiry());
        entity.setHealthCheckExpiry(updateDTO.getHealthCheckExpiry());
        entity.setHealthCheckResult(updateDTO.getHealthCheckResult());
        entity.setRestrictedPositions(updateDTO.getRestrictedPositions());
        entity.setRemark(updateDTO.getRemark());

        OverAgeWorker updated = overAgeWorkerService.update(id, entity);
        return Result.success(convertToVO(updated));
    }

    /**
     * 删除超龄劳动者记录
     * @param id 主键ID
     * @return 删除结果
     */
    @Operation(summary = "删除超龄劳动者记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('*')")
    public Result<String> delete(@PathVariable Long id) {
        boolean result = overAgeWorkerService.delete(id);
        return result ? Result.success("删除成功") : Result.error("删除失败，记录不存在");
    }

    /**
     * 获取合规预警列表
     * @return 合规预警列表
     */
    @Operation(summary = "获取合规预警列表")
    @GetMapping("/compliance-warnings")
    @PreAuthorize("hasAuthority('*')")
    public Result<List<OverAgeWorkerVO>> getComplianceWarnings() {
        List<OverAgeWorker> warnings = overAgeWorkerService.getComplianceWarnings();
        List<OverAgeWorkerVO> voList = warnings.stream().map(this::convertToVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 将实体转换为VO
     * @param entity 实体对象
     * @return VO对象
     */
    private OverAgeWorkerVO convertToVO(OverAgeWorker entity) {
        OverAgeWorkerVO vo = new OverAgeWorkerVO();
        vo.setId(entity.getId());
        vo.setEmployeeId(entity.getEmployeeId());
        vo.setEmployeeName(entity.getEmployeeName());
        vo.setRetirementDate(entity.getRetirementDate());
        vo.setReemploymentDate(entity.getReemploymentDate());
        vo.setAgreementNo(entity.getAgreementNo());
        vo.setAgreementStartDate(entity.getAgreementStartDate());
        vo.setAgreementEndDate(entity.getAgreementEndDate());
        vo.setWorkInjuryInsuranceNo(entity.getWorkInjuryInsuranceNo());
        vo.setWorkInjuryInsuranceExpiry(entity.getWorkInjuryInsuranceExpiry());
        vo.setHealthCheckExpiry(entity.getHealthCheckExpiry());
        vo.setHealthCheckResult(entity.getHealthCheckResult());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        vo.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);

        // JSON数组字符串转String[]
        if (entity.getRestrictedPositions() != null && !entity.getRestrictedPositions().isEmpty()) {
            try {
                String json = entity.getRestrictedPositions();
                // 去除方括号 [] 和空格，按逗号分割
                String trimmed = json.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    trimmed = trimmed.substring(1, trimmed.length() - 1);
                }
                vo.setRestrictedPositions(trimmed.split("\\s*,\\s*"));
            } catch (Exception e) {
                vo.setRestrictedPositions(new String[]{entity.getRestrictedPositions()});
            }
        } else {
            vo.setRestrictedPositions(new String[0]);
        }

        return vo;
    }
}
