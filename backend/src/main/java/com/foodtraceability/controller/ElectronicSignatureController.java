package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.ElectronicSignature;
import com.foodtraceability.mapper.ElectronicSignatureMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 旧版电子签章管理控制器（已弃用）
 *
 * <p><b>迁移说明（2026-06-30）：</b>
 * 该体系已被全新的 Seal 体系（{@link com.foodtraceability.controller.seal.SealController}）替代。
 * 新业务请使用 <b>/v1/seals</b> 系列接口：
 * <ul>
 *   <li>印章管理：GET/POST/PUT/DELETE /v1/seals（含分页、状态切换、授权查询）</li>
 *   <li>印章使用记录：GET/POST /v1/seals/usage-logs（审计追溯）</li>
 *   <li>采购电子合同签署已集成 SealService（见 ElectronicContractController.signElectronicContract）</li>
 * </ul>
 *
 * <p><b>为何保留：</b>
 * HR 合同模块（EmployeeLaborContractController）仍在使用 electronic_signature 表，
 * 在 HR 模块完成迁移前不得删除本控制器及其依赖的 Service/Mapper/Entity。
 *
 * <p><b>新旧体系差异：</b>
 * <ul>
 *   <li>旧体系：electronic_signature 表由 HrMigrationController 运行时创建（非 Flyway），仅记录签署动作</li>
 *   <li>新体系：seals + seal_usage_logs 两表由 Flyway 迁移管理，提供完整的印章生命周期与使用审计</li>
 * </ul>
 *
 * @see com.foodtraceability.controller.seal.SealController
 */
@Deprecated
@RestController
@RequestMapping("/v1/hr/signature")
@Tag(name = "电子签章管理", description = "电子签章和公司印章管理接口（已弃用，请使用 SealController）")
public class ElectronicSignatureController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElectronicSignatureController.class);

    public ElectronicSignatureController(ElectronicSignatureMapper signatureMapper) {
        this.signatureMapper = signatureMapper;
    }

    private final ElectronicSignatureMapper signatureMapper;
    @Value("${app.storage.path:./storage}")
    private String storagePath;
    private String companySealData = null;
    private Map<String, Object> companySealInfo = null;

    @GetMapping("/company-seal")
    @Operation(summary = "获取公司印章")
    @PreAuthorize("hasAuthority('signature:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getCompanySeal() {
        Map<String, Object> result = new HashMap<>();
        if (companySealData != null) {
            result.put("sealUrl", companySealData);
            result.put("uploadTime", companySealInfo.get("uploadTime"));
            result.put("uploadBy", companySealInfo.get("uploadBy"));
        }
        return Result.success(result);
    }

    @PostMapping("/company-seal")
    @Operation(summary = "上传公司印章")
    @PreAuthorize("hasAuthority('signature:manage') or hasAuthority('*')")
    public Result<Void> uploadCompanySeal(@RequestBody Map<String, String> request) {
        String sealData = request.get("sealData");
        if (sealData == null || sealData.isEmpty()) {
            return Result.error("印章数据不能为空");
        }
        companySealData = sealData;
        companySealInfo = new HashMap<>();
        companySealInfo.put("uploadTime", LocalDateTime.now());
        companySealInfo.put("uploadBy", "管理员");
        log.info("公司印章上传成功");
        return Result.success();
    }

    @DeleteMapping("/company-seal")
    @Operation(summary = "删除公司印章")
    @PreAuthorize("hasAuthority('signature:delete') or hasAuthority('*')")
    public Result<Void> deleteCompanySeal() {
        companySealData = null;
        companySealInfo = null;
        log.info("公司印章已删除");
        return Result.success();
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近签署记录")
    @PreAuthorize("hasAuthority('signature:view') or hasAuthority('*')")
    public Result<List<ElectronicSignature>> getRecentSignatures(@RequestParam(defaultValue = "10") int limit) {
        LambdaQueryWrapper<ElectronicSignature> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ElectronicSignature::getSignTime).last("LIMIT " + limit);
        List<ElectronicSignature> signatures = signatureMapper.selectList(wrapper);
        return Result.success(signatures);
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "获取合同签署记录")
    @PreAuthorize("hasAuthority('signature:view') or hasAuthority('*')")
    public Result<List<ElectronicSignature>> getContractSignatures(@PathVariable Long contractId) {
        LambdaQueryWrapper<ElectronicSignature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectronicSignature::getContractId, contractId).orderByAsc(ElectronicSignature::getSignTime);
        List<ElectronicSignature> signatures = signatureMapper.selectList(wrapper);
        return Result.success(signatures);
    }

    @PostMapping("/send-verify-code")
    @Operation(summary = "发送签署验证码")
    @PreAuthorize("hasAuthority('signature:manage') or hasAuthority('*')")
    public Result<Void> sendVerifyCode(@RequestBody Map<String, Long> request) {
        Long contractId = request.get("contractId");
        if (contractId == null) {
            return Result.error("合同ID不能为空");
        }
        log.info("发送签署验证码，合同ID: {}", contractId);
        return Result.success();
    }

    @PutMapping("/rules")
    @Operation(summary = "保存签章规则")
    @PreAuthorize("hasAuthority('signature:manage') or hasAuthority('*')")
    public Result<Void> saveSignatureRules(@RequestBody Map<String, Object> rules) {
        log.info("保存签章规则: {}", rules);
        return Result.success();
    }
}
