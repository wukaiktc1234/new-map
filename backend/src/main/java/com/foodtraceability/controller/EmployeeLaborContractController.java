package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.ContractDocumentDTO;
import com.foodtraceability.dto.ContractDocumentEditDTO;
import com.foodtraceability.dto.ContractExtendDTO;
import com.foodtraceability.dto.ContractRejectDTO;
import com.foodtraceability.dto.ContractSignDTO;
import com.foodtraceability.dto.ContractSignRequestDTO;
import com.foodtraceability.dto.ContractTerminateDTO;
import com.foodtraceability.dto.EmployeeLaborContractCreateDTO;
import com.foodtraceability.dto.EmployeeLaborContractUpdateDTO;
import com.foodtraceability.dto.SignatureStatusDTO;
import com.foodtraceability.dto.VerifyCodeResponseDTO;
import com.foodtraceability.entity.ContractDocument;
import com.foodtraceability.entity.ElectronicSignature;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.EmployeeLaborContractMapper;
import com.foodtraceability.service.ContractDocumentService;
import com.foodtraceability.service.ElectronicSignatureService;
import com.foodtraceability.service.EmployeeLaborContractService;
import com.foodtraceability.service.OnboardingArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 员工劳动合同控制器
 *
 * 【法律合规】劳动合同前置签署流程
 * 入职流程：审批通过 → 创建合同 → 员工签署 → 生成邀请码 → 注册入职
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/v1/contracts")
@Tag(name = "员工劳动合同管理", description = "员工劳动合同相关接口")
public class EmployeeLaborContractController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeLaborContractController.class);

    public EmployeeLaborContractController(EmployeeLaborContractService contractService, OnboardingArchiveService archiveService, EmployeeLaborContractMapper contractMapper, ContractDocumentService contractDocumentService, ElectronicSignatureService signatureService) {
        this.contractService = contractService;
        this.archiveService = archiveService;
        this.contractMapper = contractMapper;
        this.contractDocumentService = contractDocumentService;
        this.signatureService = signatureService;
    }

    private final EmployeeLaborContractService contractService;
    private final OnboardingArchiveService archiveService;
    private final EmployeeLaborContractMapper contractMapper;
    private final ContractDocumentService contractDocumentService;
    private final ElectronicSignatureService signatureService;

    @GetMapping("/list")
    @Operation(summary = "获取合同列表", description = "分页获取劳动合同列表，支持按员工ID筛选")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Map<String, Object>> listContracts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "员工ID") @RequestParam(required = false) String employeeId) {
        log.info("获取合同列表，页码：{}，每页数量：{}，状态：{}，员工ID：{}", page, size, status, employeeId);
        try {
            Page<EmployeeLaborContract> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<EmployeeLaborContract> queryWrapper = new LambdaQueryWrapper<>();
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq(EmployeeLaborContract::getStatus, status);
            }
            if (employeeId != null && !employeeId.isEmpty()) {
                queryWrapper.eq(EmployeeLaborContract::getEmployeeId, employeeId);
            }
            queryWrapper.orderByDesc(EmployeeLaborContract::getCreateTime);
            IPage<EmployeeLaborContract> pageResult = contractMapper.selectPage(pageParam, queryWrapper);
            Map<String, Object> result = new HashMap<>();
            result.put("records", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            return Result.success(result);
        } catch (Exception e) {
            // 容错：表不存在或字段不匹配时返回空列表，避免阻塞页面加载
            log.warn("获取合同列表失败，返回空列表：{}", e.getMessage());
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("records", java.util.Collections.emptyList());
            emptyResult.put("total", 0L);
            return Result.success(emptyResult);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取合同统计", description = "获取合同各状态数量统计")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Map<String, Integer>> getStatistics() {
        log.info("获取合同统计");
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("pending", contractMapper.countByStatus(EmployeeLaborContract.STATUS_PENDING));
        statistics.put("signed", contractMapper.countByStatus(EmployeeLaborContract.STATUS_SIGNED));
        statistics.put("active", contractMapper.countByStatus(EmployeeLaborContract.STATUS_ACTIVE));
        statistics.put("expired", contractMapper.countByStatus(EmployeeLaborContract.STATUS_EXPIRED));
        return Result.success(statistics);
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待签署合同列表", description = "获取所有待签署的劳动合同")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<List<EmployeeLaborContract>> getPendingContracts() {
        List<EmployeeLaborContract> contracts = contractService.getPendingContracts();
        return Result.success(contracts);
    }

    // ==================== 合同CRUD管理 ====================

    @PostMapping
    @Operation(summary = "创建劳动合同", description = "手动创建劳动合同（不通过入职档案流程）")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<EmployeeLaborContract> createContract(@Valid @RequestBody EmployeeLaborContractCreateDTO dto) {
        if (dto == null) {
            return Result.error("请求体不能为空");
        }
        log.info("创建劳动合同，员工姓名：{}", dto.getEmployeeName());
        try {
            EmployeeLaborContract contract = contractService.createContract(dto);
            if (contract == null) {
                return Result.error("创建劳动合同失败");
            }
            return Result.success(contract);
        } catch (IllegalArgumentException e) {
            log.warn("创建劳动合同参数错误：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建劳动合同失败", e);
            return Result.error("创建劳动合同失败");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新劳动合同", description = "更新劳动合同信息（不含状态变更）")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<EmployeeLaborContract> updateContract(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Valid @RequestBody EmployeeLaborContractUpdateDTO dto) {
        if (dto == null) {
            return Result.error("请求体不能为空");
        }
        log.info("更新劳动合同，合同ID：{}", id);
        try {
            EmployeeLaborContract contract = contractService.updateContract(id, dto);
            if (contract == null) {
                return Result.error(404, "合同不存在");
            }
            return Result.success(contract);
        } catch (IllegalArgumentException e) {
            log.warn("更新劳动合同参数错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新劳动合同失败，合同ID：{}", id, e);
            return Result.error("更新劳动合同失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除劳动合同", description = "逻辑删除劳动合同")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> deleteContract(@Parameter(description = "合同ID") @PathVariable Long id) {
        log.info("删除劳动合同，合同ID：{}", id);
        try {
            boolean success = contractService.deleteContract(id);
            if (!success) {
                return Result.error(404, "合同不存在或删除失败");
            }
            return Result.success();
        } catch (Exception e) {
            log.error("删除劳动合同失败，合同ID：{}", id, e);
            return Result.error("删除劳动合同失败");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取合同详情", description = "根据合同ID获取详情")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<EmployeeLaborContract> getContractById(@Parameter(description = "合同ID") @PathVariable Long id) {
        EmployeeLaborContract contract = contractService.getById(id);
        if (contract == null) {
            return Result.error(404, "合同不存在");
        }
        return Result.success(contract);
    }

    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "根据档案ID获取合同", description = "根据入职档案ID获取对应的劳动合同")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<EmployeeLaborContract> getContractByArchiveId(@Parameter(description = "档案ID") @PathVariable Long archiveId) {
        EmployeeLaborContract contract = contractService.getByArchiveId(archiveId);
        if (contract == null) {
            return Result.error(404, "该档案暂无合同");
        }
        return Result.success(contract);
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "签署合同", description = "员工签署劳动合同")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> signContract(@Parameter(description = "合同ID") @PathVariable Long id, @RequestBody(required = false) ContractSignDTO dto) {
        String signDate = dto != null ? dto.getSignDate() : null;
        boolean success = contractService.signContract(id, signDate);
        if (!success) {
            return Result.error("签署合同失败");
        }
        EmployeeLaborContract contract = contractService.getById(id);
        if (contract != null && contract.getArchiveId() != null) {
            OnboardingArchive archive = archiveService.getById(contract.getArchiveId());
            if (archive != null) {
                archive.setStatus(OnboardingArchive.STATUS_CONTRACT_SIGNED);
                archiveService.updateById(archive);
                log.info("合同签署成功，档案状态更新为CONTRACT_SIGNED，档案ID：{}", archive.getId());
            }
        }
        return Result.success();
    }

    @PostMapping("/{id}/extend")
    @Operation(summary = "延长合同期限", description = "延长劳动合同期限")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> extendContract(@Parameter(description = "合同ID") @PathVariable Long id, @RequestBody(required = false) ContractExtendDTO dto) {
        int months = dto != null && dto.getMonths() != null ? dto.getMonths() : 12;
        boolean success = contractService.extendContract(id, months);
        if (!success) {
            return Result.error("延长合同期限失败");
        }
        log.info("延长合同期限成功，合同ID：{}，延长月数：{}", id, months);
        return Result.success();
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止合同", description = "终止劳动合同")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> terminateContract(@Parameter(description = "合同ID") @PathVariable Long id, @RequestBody ContractTerminateDTO dto) {
        if (dto == null || dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            return Result.error("请提供终止原因");
        }
        boolean success = contractService.terminateContract(id, dto.getReason());
        if (!success) {
            return Result.error("终止合同失败");
        }
        log.info("终止合同成功，合同ID：{}，原因：{}", id, dto.getReason());
        return Result.success();
    }

    // ==================== 合同正文管理 ====================

    @GetMapping("/{id}/document")
    @Operation(summary = "获取合同当前正文", description = "根据合同ID获取当前版本的合同正文")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<ContractDocumentDTO> getCurrentDocument(@Parameter(description = "合同ID") @PathVariable Long id) {
        log.info("获取合同当前正文，合同ID：{}", id);
        ContractDocument document = contractDocumentService.getCurrentDocument(id);
        if (document == null) {
            return Result.error(404, "该合同暂无正文");
        }
        return Result.success(ContractDocumentDTO.fromEntity(document));
    }

    @GetMapping("/{id}/document/history")
    @Operation(summary = "获取版本历史", description = "获取合同正文的所有版本历史，按版本号倒序")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<ContractDocumentDTO>> getDocumentHistory(@Parameter(description = "合同ID") @PathVariable Long id) {
        log.info("获取合同正文版本历史，合同ID：{}", id);
        List<ContractDocument> history = contractDocumentService.getDocumentHistory(id);
        List<ContractDocumentDTO> dtoList = history.stream()
                .map(ContractDocumentDTO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @GetMapping("/{id}/document/{version}")
    @Operation(summary = "获取指定版本", description = "根据合同ID和版本号获取指定版本的合同正文")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<ContractDocumentDTO> getDocumentByVersion(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Parameter(description = "版本号") @PathVariable Integer version) {
        log.info("获取合同正文指定版本，合同ID：{}，版本：{}", id, version);
        ContractDocument document = contractDocumentService.getDocumentByVersion(id, version);
        if (document == null) {
            return Result.error(404, "指定版本的合同正文不存在");
        }
        return Result.success(ContractDocumentDTO.fromEntity(document));
    }

    @PostMapping("/{id}/document")
    @Operation(summary = "保存合同正文", description = "保存合同正文，创建新版本，旧版本自动归档")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<ContractDocumentDTO> saveDocument(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Valid @RequestBody ContractDocumentEditDTO dto) {
        if (dto == null) {
            return Result.error("请求体不能为空");
        }
        String operator = getCurrentOperator();
        log.info("保存合同正文，合同ID：{}，操作人：{}", id, operator);
        try {
            ContractDocument document = contractDocumentService.saveDocument(
                    id, dto.getHtmlContent(), dto.getEditRemark(), operator);
            return Result.success(ContractDocumentDTO.fromEntity(document));
        } catch (IllegalArgumentException e) {
            log.warn("保存合同正文参数错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("保存合同正文失败，合同ID：{}", id, e);
            return Result.error("保存合同正文失败");
        }
    }

    // ==================== 合同签署流程管理 ====================

    @GetMapping("/{id}/signatures")
    @Operation(summary = "获取合同签署记录列表", description = "根据合同ID获取所有签署记录")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<ElectronicSignature>> getSignatures(@Parameter(description = "合同ID") @PathVariable Long id) {
        log.info("获取合同签署记录列表，合同ID：{}", id);
        List<ElectronicSignature> signatures = signatureService.getSignaturesByContractId(id);
        return Result.success(signatures);
    }

    @GetMapping("/{id}/signature-status")
    @Operation(summary = "获取合同签署状态看板", description = "获取合同签署进度看板数据，包含各方签署详情")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<SignatureStatusDTO> getSignatureStatus(@Parameter(description = "合同ID") @PathVariable Long id) {
        log.info("获取合同签署状态看板，合同ID：{}", id);
        SignatureStatusDTO status = signatureService.getSignatureStatus(id);
        if (status == null) {
            return Result.error(404, "合同不存在");
        }
        return Result.success(status);
    }

    @PostMapping("/{id}/init-signatures")
    @Operation(summary = "初始化合同签署记录", description = "创建多方签署记录（公司方+员工方），状态为待签")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<List<ElectronicSignature>> initSignatures(@Parameter(description = "合同ID") @PathVariable Long id) {
        String operator = getCurrentOperator();
        log.info("初始化合同签署记录，合同ID：{}，操作人：{}", id, operator);
        try {
            List<ElectronicSignature> signatures = signatureService.initSignatures(id, operator);
            return Result.success(signatures);
        } catch (IllegalArgumentException e) {
            log.warn("初始化签署记录参数错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("初始化签署记录失败，合同ID：{}", id, e);
            return Result.error("初始化签署记录失败");
        }
    }

    @PostMapping("/{id}/sign-with-signature")
    @Operation(summary = "签署合同（带签名数据）", description = "签署方提交签名数据和验证码完成签署，全部签署完成后自动更新合同状态")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> signWithSignature(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Valid @RequestBody ContractSignRequestDTO dto) {
        if (dto == null) {
            return Result.error("请求体不能为空");
        }
        String operator = getCurrentOperator();
        log.info("签署合同（带签名数据），合同ID：{}，签署人类型：{}，操作人：{}",
                id, dto.getSignerType(), operator);
        try {
            boolean success = signatureService.signContract(
                    id, dto.getSignerType(), dto.getSignatureData(),
                    dto.getSignIp(), dto.getSignDevice(), dto.getVerifyCode(), operator);
            if (!success) {
                return Result.error("签署合同失败");
            }
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.warn("签署合同参数错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("签署合同状态错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(422, e.getMessage());
        } catch (Exception e) {
            log.error("签署合同失败，合同ID：{}", id, e);
            return Result.error("签署合同失败");
        }
    }

    @PostMapping("/{id}/reject-signature")
    @Operation(summary = "拒绝签署", description = "签署方拒绝签署合同，需提供拒绝原因")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> rejectSignature(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Valid @RequestBody ContractRejectDTO dto) {
        if (dto == null) {
            return Result.error("请求体不能为空");
        }
        String operator = getCurrentOperator();
        log.info("拒绝签署，合同ID：{}，签署人类型：{}，操作人：{}", id, dto.getSignerType(), operator);
        try {
            boolean success = signatureService.rejectSignature(
                    id, dto.getSignerType(), dto.getReason(), operator);
            if (!success) {
                return Result.error("拒绝签署失败");
            }
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.warn("拒绝签署参数错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("拒绝签署状态错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(422, e.getMessage());
        } catch (Exception e) {
            log.error("拒绝签署失败，合同ID：{}", id, e);
            return Result.error("拒绝签署失败");
        }
    }

    @PostMapping("/{id}/send-verify-code")
    @Operation(summary = "发送验证码", description = "向指定签署方发送验证码，实际项目应通过短信/邮件发送")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<VerifyCodeResponseDTO> sendVerifyCode(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Parameter(description = "签署人类型: employee-员工, company-公司") @RequestParam String signerType) {
        log.info("发送验证码，合同ID：{}，签署人类型：{}", id, signerType);
        try {
            VerifyCodeResponseDTO response = signatureService.sendVerifyCode(id, signerType);
            return Result.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("发送验证码参数错误，合同ID：{}，原因：{}", id, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码失败，合同ID：{}", id, e);
            return Result.error("发送验证码失败");
        }
    }

    /**
     * 获取当前操作人用户名
     *
     * @return 当前操作人用户名，获取失败返回 "system"
     */
    private String getCurrentOperator() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前操作人失败：{}", e.getMessage());
        }
        return "system";
    }
}
