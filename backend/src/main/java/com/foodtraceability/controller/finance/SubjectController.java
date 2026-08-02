package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.service.finance.AccountingSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计科目控制器
 */
@Tag(name = "会计科目管理", description = "会计科目的增删改查和树形结构展示")
@RestController
@RequestMapping("/v1/finance/subjects")
public class SubjectController {

    private final AccountingSubjectService accountingSubjectService;

    public SubjectController(AccountingSubjectService accountingSubjectService) {
        this.accountingSubjectService = accountingSubjectService;
    }

    @Operation(summary = "获取科目树", description = "获取完整的会计科目树形结构，用于凭证录入选择科目")
    @GetMapping("/tree")
    public Result<List<AccountingSubjectVO>> getTree() {
        return Result.success(accountingSubjectService.getSubjectTree());
    }

    @Operation(summary = "获取叶子节点科目列表", description = "用于凭证录入的下拉选择")
    @GetMapping("/leaves")
    public Result<List<AccountingSubjectVO>> getLeafSubjects() {
        List<AccountingSubject> leaves = accountingSubjectService.getLeafSubjects();
        // 转换为 VO 列表
        List<AccountingSubjectVO> voList = new java.util.ArrayList<>();
        for (AccountingSubject subject : leaves) {
            AccountingSubjectVO vo = new AccountingSubjectVO();
            vo.setSubjectId(subject.getSubjectId());
            vo.setSubjectCode(subject.getSubjectCode());
            vo.setSubjectName(subject.getSubjectName());
            vo.setSubjectType(subject.getSubjectType());
            vo.setDirection(subject.getDirection());
            vo.setStatus(subject.getStatus());
            vo.setParentId(subject.getParentId());
            vo.setIsLeaf(subject.getIsLeaf());
            vo.setRemark(subject.getRemark());
            voList.add(vo);
        }
        return Result.success(voList);
    }

    @Operation(summary = "创建会计科目")
    @PostMapping
    public Result<AccountingSubjectVO> create(@Valid @RequestBody AccountingSubjectCreateDTO dto) {
        AccountingSubject entity = accountingSubjectService.create(dto);
        AccountingSubjectVO vo = new AccountingSubjectVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        return Result.success(vo);
    }

    @Operation(summary = "更新会计科目")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody AccountingSubjectUpdateDTO dto) {
        dto.setSubjectId(id);
        return Result.success(accountingSubjectService.update(dto));
    }

    @Operation(summary = "获取科目详情")
    @GetMapping("/{id}")
    public Result<AccountingSubjectVO> getDetail(@PathVariable Long id) {
        return Result.success(accountingSubjectService.getDetail(id));
    }

    @Operation(summary = "分页查询科目列表")
    @GetMapping
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<AccountingSubjectVO>> getPage(AccountingSubjectQueryDTO query) {
        return Result.success(accountingSubjectService.getPage(query));
    }

    @Operation(summary = "启用/禁用科目")
    @PatchMapping("/{id}/status")
    public Result<Boolean> toggleStatus(@PathVariable Long id) {
        return Result.success(accountingSubjectService.toggleStatus(id));
    }
}
