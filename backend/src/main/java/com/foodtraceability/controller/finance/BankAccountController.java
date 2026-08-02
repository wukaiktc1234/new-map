package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 银行账户控制器
 * 管理银行账户信息，支持余额管理与账号脱敏
 */
@Tag(name = "银行账户管理", description = "银行账户的增删改查及余额管理")
@RestController
@RequestMapping("/v1/finance/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @Operation(summary = "创建银行账户", description = "新增银行账户信息，账号将自动脱敏存储")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:bank:create')")
    public Result<BankAccountVO> create(@Valid @RequestBody BankAccountCreateDTO dto) {
        return Result.success(bankAccountService.create(dto));
    }

    @Operation(summary = "更新银行账户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:bank:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody BankAccountUpdateDTO dto) {
        return Result.success(bankAccountService.update(id, dto));
    }

    @Operation(summary = "删除银行账户", description = "逻辑删除指定银行账户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:bank:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(bankAccountService.delete(id));
    }

    @Operation(summary = "获取银行账户详情", description = "获取银行账户详情，账号自动脱敏")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:bank:query')")
    public Result<BankAccountVO> getDetail(@PathVariable Long id) {
        return Result.success(bankAccountService.getDetail(id));
    }

    @Operation(summary = "分页查询银行账户", description = "支持按账户类型等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:bank:query')")
    public Result<IPage<BankAccountVO>> getPage(BankAccountQueryDTO query) {
        return Result.success(bankAccountService.getPage(query));
    }

    @Operation(summary = "更新账户余额", description = "更新银行账户余额，单位为分")
    @PutMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('finance:bank:update')")
    public Result<Boolean> updateBalance(@PathVariable Long id, @RequestParam Long newBalance) {
        return Result.success(bankAccountService.updateBalance(id, newBalance));
    }
}
