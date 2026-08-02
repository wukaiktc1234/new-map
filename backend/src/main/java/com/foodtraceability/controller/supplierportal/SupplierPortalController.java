package com.foodtraceability.controller.supplierportal;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.supplierportal.SupplierSignLink;
import com.foodtraceability.service.supplierportal.SupplierPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 供应商签署门户控制器
 * 提供签署链接的管理端 CRUD 与供应商端 H5 签署流程接口
 *
 * <p>路径前缀：/v1/supplier-portal
 *
 * <p>接口分组：
 * <ul>
 *   <li>管理端：/page、/links、/{linkId}、/{linkId}/send、/{linkId}/revoke</li>
 *   <li>供应商端 H5：/h5/contract、/h5/view、/h5/verify、/h5/sign、/h5/reject</li>
 * </ul>
 *
 * <p>注意：字面量路径（/h5/*、/page、/links）需在 /{linkId} 之前定义，避免路径变量误匹配。
 */
@Tag(name = "供应商签署门户", description = "供应商签署链接管理及H5签署流程接口")
@RestController
@RequestMapping("/v1/supplier-portal")
public class SupplierPortalController {

    private static final Logger log = LoggerFactory.getLogger(SupplierPortalController.class);

    private final SupplierPortalService supplierPortalService;

    public SupplierPortalController(SupplierPortalService supplierPortalService) {
        this.supplierPortalService = supplierPortalService;
    }

    // ==================== 供应商端 H5 API（字面量路径优先定义） ====================

    /**
     * 供应商通过 token 获取合同信息
     * H5 页面通过链接中的 token 拉取合同详情，已过期的链接不返回
     */
    @Operation(summary = "供应商获取合同信息", description = "通过 token 获取签署链接及合同信息，已过期链接返回 null")
    @GetMapping("/h5/contract")
    public Result<SupplierSignLink> getContractByToken(
            @Parameter(description = "H5访问token") @RequestParam String token) {
        try {
            SupplierSignLink link = supplierPortalService.getContractByToken(token);
            return Result.success(link);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 供应商查看合同（更新状态为 viewed）
     * 仅 pending/sent 状态会流转为 viewed，记录首次查看时间
     */
    @Operation(summary = "供应商查看合同", description = "供应商打开合同详情，状态由 pending/sent 流转为 viewed")
    @PostMapping("/h5/view")
    public Result<SupplierSignLink> viewContract(@RequestBody Map<String, Object> body) {
        try {
            String token = body == null ? null : (String) body.get("token");
            SupplierSignLink link = supplierPortalService.viewContract(token);
            return Result.success(link);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送实名认证验证码
     * 校验签署链接有效性后，将验证码存入 Redis（10分钟过期）
     */
    @Operation(summary = "发送实名认证验证码", description = "校验签署链接后生成验证码存入 Redis，暂未对接真实短信网关")
    @PostMapping("/h5/send-code")
    public Result<Map<String, Object>> sendVerifyCode(@RequestBody Map<String, Object> body) {
        try {
            String token = getString(body, "token");
            String phone = getString(body, "phone");
            Map<String, Object> result = supplierPortalService.sendVerifyCode(token, phone);
            return Result.success(result, "验证码已发送");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 供应商实名认证
     * 校验验证码并返回脱敏后的认证信息（验证码必须先通过 /h5/send-code 发送）
     */
    @Operation(summary = "供应商实名认证", description = "校验验证码并返回脱敏后的实名认证信息")
    @PostMapping("/h5/verify")
    public Result<Map<String, Object>> verify(@RequestBody Map<String, Object> body) {
        try {
            String token = getString(body, "token");
            String realName = getString(body, "realName");
            String idCard = getString(body, "idCard");
            String phone = getString(body, "phone");
            String code = getString(body, "code");
            Map<String, Object> verification = supplierPortalService.verify(token, realName, idCard, phone, code);
            return Result.success(verification);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 供应商确认签署
     * 签署成功后状态置为 signed，记录签署时间与认证信息
     */
    @Operation(summary = "供应商确认签署", description = "供应商确认签署合同，状态变为 signed")
    @PostMapping("/h5/sign")
    @SuppressWarnings("unchecked")
    public Result<SupplierSignLink> signByToken(@RequestBody Map<String, Object> body) {
        try {
            String token = getString(body, "token");
            Object verificationObj = body == null ? null : body.get("verification");
            Map<String, Object> verification = verificationObj instanceof Map
                    ? (Map<String, Object>) verificationObj : null;
            SupplierSignLink link = supplierPortalService.signByToken(token, verification);
            return Result.success(link, "签署成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 供应商拒绝签署
     * 拒绝后状态置为 rejected，记录拒绝时间与原因
     */
    @Operation(summary = "供应商拒绝签署", description = "供应商拒绝签署合同，状态变为 rejected")
    @PostMapping("/h5/reject")
    public Result<SupplierSignLink> rejectByToken(@RequestBody Map<String, Object> body) {
        try {
            String token = getString(body, "token");
            String reason = getString(body, "reason");
            SupplierSignLink link = supplierPortalService.rejectByToken(token, reason);
            return Result.success(link, "已拒绝签署");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 管理端 API（字面量路径优先定义） ====================

    /**
     * 查询签署链接列表（分页+筛选）
     * 支持按状态、关键词筛选，按创建时间倒序返回
     */
    @Operation(summary = "查询签署链接列表", description = "分页查询签署链接，支持按状态、关键词筛选")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('purchase:supplier:manage') or hasAuthority('*')")
    public Result<PageResult<SupplierSignLink>> getList(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        try {
            PageResult<SupplierSignLink> result = supplierPortalService.getList(page, size, status, keyword);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成签署链接
     * 根据电子合同ID生成签署链接，初始状态为 pending
     */
    @Operation(summary = "生成签署链接", description = "根据电子合同ID生成供应商签署链接")
    @PostMapping("/links")
    @PreAuthorize("hasAuthority('purchase:supplier:manage') or hasAuthority('*')")
    public Result<SupplierSignLink> createLink(@RequestBody Map<String, Object> body) {
        try {
            String eContractId = getString(body, "eContractId");
            String contactPhone = getString(body, "contactPhone");
            String contactEmail = getString(body, "contactEmail");
            Integer expireDays = getInteger(body, "expireDays");
            String createBy = getCurrentOperator();
            SupplierSignLink link = supplierPortalService.createLink(eContractId, contactPhone,
                    contactEmail, expireDays, createBy);
            return Result.success(link, "签署链接生成成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取签署链接详情
     */
    @Operation(summary = "获取签署链接详情", description = "根据链接ID获取签署链接详细信息")
    @GetMapping("/{linkId}")
    @PreAuthorize("hasAuthority('purchase:supplier:manage') or hasAuthority('*')")
    public Result<SupplierSignLink> getById(
            @Parameter(description = "链接ID") @PathVariable String linkId) {
        try {
            SupplierSignLink link = supplierPortalService.getById(linkId);
            if (link == null) {
                return Result.error("签署链接不存在");
            }
            return Result.success(link);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送签署链接（短信/邮件）
     * 状态由 pending 流转为 sent，记录发送时间
     */
    @Operation(summary = "发送签署链接", description = "通过短信/邮件渠道发送签署链接给供应商")
    @PostMapping("/{linkId}/send")
    @PreAuthorize("hasAuthority('purchase:supplier:manage') or hasAuthority('*')")
    public Result<SupplierSignLink> sendLink(
            @Parameter(description = "链接ID") @PathVariable String linkId,
            @RequestBody Map<String, Object> body) {
        try {
            String channel = getString(body, "channel");
            SupplierSignLink link = supplierPortalService.sendLink(linkId, channel);
            return Result.success(link, "链接发送成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 作废签署链接
     * 状态置为 expired，已签署的链接不允许作废
     */
    @Operation(summary = "作废签署链接", description = "作废指定的签署链接，状态变为 expired")
    @PostMapping("/{linkId}/revoke")
    @PreAuthorize("hasAuthority('purchase:supplier:manage') or hasAuthority('*')")
    public Result<String> revokeLink(
            @Parameter(description = "链接ID") @PathVariable String linkId) {
        try {
            supplierPortalService.revokeLink(linkId);
            return Result.success("链接作废成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从 Map 中安全获取字符串值
     */
    private String getString(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 从 Map 中安全获取整数值
     */
    private Integer getInteger(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取当前操作人用户名
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
