package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.ElectronicContract;
import com.foodtraceability.entity.seal.SealUsageLog;
import com.foodtraceability.service.ElectronicContractService;
import com.foodtraceability.service.seal.SealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/purchase/electronic-contracts")
@Tag(name = "电子合同管理", description = "电子合同创建、签署、取消等管理接口")
public class ElectronicContractController {

    private static final Logger log = LoggerFactory.getLogger(ElectronicContractController.class);

    private final ElectronicContractService electronicContractService;
    private final SealService sealService;

    public ElectronicContractController(ElectronicContractService electronicContractService,
                                        SealService sealService) {
        this.electronicContractService = electronicContractService;
        this.sealService = sealService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询电子合同")
    @PreAuthorize("hasAuthority('esign:view') or hasAuthority('*')")
    public Result<Page<ElectronicContract>> getElectronicContractPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<ElectronicContract> pageParam = new Page<>(page, pageSize);
        Page<ElectronicContract> result = electronicContractService.getElectronicContractPage(pageParam, contractNo, supplierId, status, startDate, endDate);
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "创建电子合同")
    @PreAuthorize("hasAuthority('esign:manage') or hasAuthority('*')")
    public Result<ElectronicContract> createElectronicContract(@RequestBody ElectronicContract electronicContract) {
        return Result.success(electronicContractService.createElectronicContract(electronicContract));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新电子合同")
    @PreAuthorize("hasAuthority('esign:manage') or hasAuthority('*')")
    public Result<ElectronicContract> updateElectronicContract(@PathVariable Long id, @RequestBody ElectronicContract electronicContract) {
        return Result.success(electronicContractService.updateElectronicContract(id, electronicContract));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取电子合同详情")
    @PreAuthorize("hasAuthority('esign:view') or hasAuthority('*')")
    public Result<ElectronicContract> getElectronicContractById(@PathVariable Long id) {
        return Result.success(electronicContractService.getElectronicContractById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除电子合同")
    @PreAuthorize("hasAuthority('esign:delete') or hasAuthority('*')")
    public Result<Void> deleteElectronicContract(@PathVariable Long id) {
        electronicContractService.deleteElectronicContract(id);
        return Result.success();
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "发送签署邀请")
    @PreAuthorize("hasAuthority('esign:manage') or hasAuthority('*')")
    public Result<ElectronicContract> sendForSign(@PathVariable Long id) {
        return Result.success(electronicContractService.sendForSign(id));
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "签署电子合同",
            description = "签署电子合同，可选择关联印章ID。若提供 sealId，签署成功后会自动记录印章使用日志。")
    @PreAuthorize("hasAuthority('esign:manage') or hasAuthority('*')")
    public Result<ElectronicContract> signElectronicContract(
            @PathVariable Long id,
            @RequestParam(required = false) String sealId,
            @RequestParam(required = false) String verifyCode,
            HttpServletRequest request) {
        ElectronicContract contract = electronicContractService.signElectronicContract(id, sealId);
        // 签署成功后记录印章使用（仅当提供了印章ID时）
        if (sealId != null && !sealId.trim().isEmpty()) {
            try {
                Map<String, Object> usageData = new HashMap<>();
                usageData.put("sealId", sealId);
                usageData.put("businessType", "electronic_contract");
                usageData.put("businessId", String.valueOf(id));
                usageData.put("businessNo", contract.getContractNo());
                usageData.put("operator", contract.getUpdatedBy() != null
                        ? String.valueOf(contract.getUpdatedBy()) : "");
                usageData.put("remark", verifyCode != null && !verifyCode.isEmpty()
                        ? "验证码:" + verifyCode : null);
                SealUsageLog usageLog = sealService.recordUsage(usageData, getClientIpAddress(request));
                log.info("电子合同[{}]签署成功，印章使用记录已保存: logId={}",
                        contract.getContractNo(), usageLog != null ? usageLog.getLogId() : "null");
            } catch (Exception e) {
                // 印章使用记录失败不阻断签署流程，仅记录日志
                log.warn("电子合同[{}]签署成功，但印章使用记录写入失败: {}",
                        contract.getContractNo(), e.getMessage());
            }
        }
        return Result.success(contract);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消电子合同")
    @PreAuthorize("hasAuthority('esign:manage') or hasAuthority('*')")
    public Result<ElectronicContract> cancelElectronicContract(
            @PathVariable Long id,
            @RequestParam String reason) {
        return Result.success(electronicContractService.cancelElectronicContract(id, reason));
    }
}
