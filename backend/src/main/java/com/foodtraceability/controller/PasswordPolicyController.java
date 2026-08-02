package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PasswordPolicyDTO;
import com.foodtraceability.entity.PasswordPolicy;
import com.foodtraceability.service.PasswordPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密码策略控制器
 */
@RestController
@RequestMapping("/v1/password-policies")
@Tag(name = "密码策略管理", description = "系统密码安全策略配置")
public class PasswordPolicyController {

    private final PasswordPolicyService passwordPolicyService;

    public PasswordPolicyController(PasswordPolicyService passwordPolicyService) {
        this.passwordPolicyService = passwordPolicyService;
    }

    @GetMapping
    @Operation(summary = "查询密码策略列表")
    @PreAuthorize("hasAuthority('system:policy:view') or hasAuthority('*')")
    public Result<List<PasswordPolicy>> list() {
        List<PasswordPolicy> policies = passwordPolicyService.list();
        return Result.success(policies);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认密码策略")
    @PreAuthorize("hasAuthority('system:policy:view') or hasAuthority('*')")
    public Result<PasswordPolicy> getDefault() {
        PasswordPolicy policy = passwordPolicyService.getDefaultPolicy();
        return Result.success(policy);
    }

    @PostMapping
    @Operation(summary = "创建密码策略")
    @PreAuthorize("hasAuthority('system:policy:create') or hasAuthority('*')")
    public Result<PasswordPolicy> create(@Valid @RequestBody PasswordPolicyDTO dto) {
        PasswordPolicy policy = passwordPolicyService.createFromDTO(dto);
        passwordPolicyService.save(policy);
        return Result.success(policy);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新密码策略")
    @PreAuthorize("hasAuthority('system:policy:update') or hasAuthority('*')")
    public Result<PasswordPolicy> update(@PathVariable Long id, @Valid @RequestBody PasswordPolicyDTO dto) {
        PasswordPolicy existing = passwordPolicyService.getById(id);
        if (existing == null) {
            return Result.error("策略不存在");
        }
        PasswordPolicy policy = passwordPolicyService.createFromDTO(dto);
        policy.setPolicyId(id);
        policy.setVersion(existing.getVersion());
        passwordPolicyService.updateById(policy);
        return Result.success(policy);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认策略")
    @PreAuthorize("hasAuthority('system:policy:update') or hasAuthority('*')")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        boolean success = passwordPolicyService.setDefaultPolicy(id);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除密码策略")
    @PreAuthorize("hasAuthority('system:policy:delete') or hasAuthority('*')")
    public Result<Void> delete(@PathVariable Long id) {
        PasswordPolicy existing = passwordPolicyService.getById(id);
        if (existing == null) {
            return Result.error("策略不存在");
        }
        passwordPolicyService.removeById(id);
        return Result.success();
    }
}
