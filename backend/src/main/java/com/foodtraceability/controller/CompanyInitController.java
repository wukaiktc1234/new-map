package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.CompanyInitDTO;
import com.foodtraceability.dto.SystemInitStatusDTO;
import com.foodtraceability.service.SystemInitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公司初始化控制器
 * 提供公司初始化向导的后端接口
 */
@Tag(name = "公司初始化", description = "公司初始化向导接口")
@RestController
@RequestMapping("/v1/company-init")
public class CompanyInitController {

    private static final Logger log = LoggerFactory.getLogger(CompanyInitController.class);

    private final SystemInitService systemInitService;

    public CompanyInitController(SystemInitService systemInitService) {
        this.systemInitService = systemInitService;
    }

    /**
     * 查询当前初始化状态
     */
    @Operation(summary = "查询初始化状态")
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('system:init:view') or hasAuthority('*')")
    public Result<SystemInitStatusDTO> getInitStatus() {
        try {
            SystemInitStatusDTO status = systemInitService.getInitStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询初始化状态失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 判断系统是否已完成初始化
     */
    @Operation(summary = "判断系统是否已初始化")
    @GetMapping("/initialized")
    @PreAuthorize("hasAuthority('system:init:view') or hasAuthority('*')")
    public Result<Boolean> isInitialized() {
        try {
            boolean initialized = systemInitService.isSystemInitialized();
            return Result.success(initialized);
        } catch (Exception e) {
            log.error("判断初始化状态失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 执行公司初始化
     */
    @Operation(summary = "执行公司初始化")
    @PostMapping("/initialize")
    @PreAuthorize("hasAuthority('system:init:manage') or hasAuthority('*')")
    public Result<Boolean> initializeCompany(
            @Parameter(description = "公司初始化信息", required = true)
            @Valid @RequestBody CompanyInitDTO dto) {
        try {
            boolean success = systemInitService.initializeCompany(dto);
            return Result.success(success);
        } catch (Exception e) {
            log.error("公司初始化失败: {}", dto.getCompanyName(), e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "初始化失败");
        }
    }
}
