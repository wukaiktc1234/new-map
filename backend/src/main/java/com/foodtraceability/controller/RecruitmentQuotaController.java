package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.recruitment.QuotaAdjustmentDTO;
import com.foodtraceability.dto.recruitment.QuotaAdjustmentReviewDTO;
import com.foodtraceability.dto.recruitment.QuotaRejectDTO;
import com.foodtraceability.dto.recruitment.QuotaValidateResult;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaCreateDTO;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaQueryDTO;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaVO;
import com.foodtraceability.entity.RecruitmentQuota;
import com.foodtraceability.service.RecruitmentQuotaService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 招聘名额控制器
 *
 * <p>对应 Ch3.1 招聘名额系统 10 个 API 端点，覆盖 8 类业务事件触发：</p>
 * <ul>
 *   <li>issueQuota → recruitment.quota.issued</li>
 *   <li>confirmQuota → recruitment.quota.confirmed</li>
 *   <li>rejectQuota → recruitment.quota.rejected</li>
 *   <li>requestAdjustment → recruitment.quota.adjustment_requested</li>
 *   <li>approveAdjustment → recruitment.quota.adjustment_result</li>
 *   <li>closeQuota → recruitment.quota.closed</li>
 *   <li>incrementUsedCount(80%) → recruitment.quota.exhausting（Sprint 3 调用）</li>
 *   <li>incrementUsedCount(100%) → recruitment.quota.exhausted（Sprint 3 调用）</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/recruitment-quotas")
@Tag(name = "招聘名额", description = "招聘名额下发、确认、追加、关闭等接口")
@Validated
public class RecruitmentQuotaController {

    private final RecruitmentQuotaService quotaService;

    /**
     * 构造函数注入
     *
     * @param quotaService 招聘名额服务
     */
    public RecruitmentQuotaController(RecruitmentQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /**
     * HR 创建并下发招聘名额到门店
     *
     * <p>发布 recruitment.quota.issued 事件（接收人=门店店长，渠道=SITE_MSG+EMAIL）。</p>
     *
     * @param dto 名额下发参数
     * @return 已下发的名额实体
     */
    @Operation(summary = "HR 创建并下发招聘名额", description = "HR 按年度/季度向门店下发招聘名额")
    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public Result<RecruitmentQuota> issueQuota(@Valid @RequestBody RecruitmentQuotaCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        RecruitmentQuota quota = quotaService.issueQuota(dto, operatorId);
        return Result.success(quota);
    }

    /**
     * 查询招聘名额详情
     *
     * @param quotaId 名额 ID
     * @return 名额实体
     */
    @Operation(summary = "查询招聘名额详情")
    @GetMapping("/{quotaId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER', 'STORE_MANAGER')")
    public Result<RecruitmentQuota> getQuotaById(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId) {
        RecruitmentQuota quota = quotaService.getQuotaById(quotaId);
        return Result.success(quota);
    }

    /**
     * 分页查询招聘名额列表
     *
     * @param query 查询参数（page/size/year/quarter/storeId/positionId/status）
     * @return 名额 VO 分页结果
     */
    @Operation(summary = "分页查询招聘名额列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER', 'STORE_MANAGER')")
    public Result<IPage<RecruitmentQuotaVO>> listQuotas(@Valid RecruitmentQuotaQueryDTO query) {
        IPage<RecruitmentQuotaVO> page = quotaService.listQuotas(query);
        return Result.success(page);
    }

    /**
     * 门店店长确认接收名额（issued → active）
     *
     * <p>发布 recruitment.quota.confirmed 事件（接收人=下发 HR，渠道=SITE_MSG）。</p>
     *
     * @param quotaId 名额 ID
     * @return 是否确认成功
     */
    @Operation(summary = "门店确认接收名额")
    @PutMapping("/{quotaId}/confirm")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public Result<Boolean> confirmQuota(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = quotaService.confirmQuota(quotaId, operatorId);
        return Result.success(success);
    }

    /**
     * 门店店长拒绝接收名额（issued → rejected）
     *
     * <p>发布 recruitment.quota.rejected 事件（接收人=下发 HR，渠道=SITE_MSG）。</p>
     *
     * @param quotaId 名额 ID
     * @param dto     拒绝原因
     * @return 是否拒绝成功
     */
    @Operation(summary = "门店拒绝接收名额")
    @PutMapping("/{quotaId}/reject")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public Result<Boolean> rejectQuota(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId,
            @Valid @RequestBody QuotaRejectDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = quotaService.rejectQuota(quotaId, dto, operatorId);
        return Result.success(success);
    }

    /**
     * 门店店长申请追加名额（active → adjustment_requested）
     *
     * <p>发布 recruitment.quota.adjustment_requested 事件（接收人=下发 HR，渠道=SITE_MSG+EMAIL）。</p>
     *
     * @param quotaId 名额 ID
     * @param dto     追加申请参数
     * @return 是否申请成功
     */
    @Operation(summary = "门店申请追加名额")
    @PutMapping("/{quotaId}/request-adjustment")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public Result<Boolean> requestAdjustment(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId,
            @Valid @RequestBody QuotaAdjustmentDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = quotaService.requestAdjustment(quotaId, dto, operatorId);
        return Result.success(success);
    }

    /**
     * HR 审核门店的追加申请（adjustment_requested → active/issued）
     *
     * <p>发布 recruitment.quota.adjustment_result 事件（接收人=门店店长，渠道=SITE_MSG）。</p>
     *
     * @param quotaId 名额 ID
     * @param dto     审核结果
     * @return 是否审核成功
     */
    @Operation(summary = "HR 审核名额追加申请")
    @PutMapping("/{quotaId}/approve-adjustment")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public Result<Boolean> approveAdjustment(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId,
            @Valid @RequestBody QuotaAdjustmentReviewDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = quotaService.approveAdjustment(quotaId, dto, operatorId);
        return Result.success(success);
    }

    /**
     * HR 手动关闭名额（→ closed）
     *
     * <p>发布 recruitment.quota.closed 事件（接收人=门店店长，渠道=SITE_MSG）。</p>
     *
     * @param quotaId 名额 ID
     * @return 是否关闭成功
     */
    @Operation(summary = "HR 关闭招聘名额")
    @PutMapping("/{quotaId}/close")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public Result<Boolean> closeQuota(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = quotaService.closeQuota(quotaId, operatorId);
        return Result.success(success);
    }

    /**
     * 按门店查询所有可用名额（status=active）
     *
     * @param storeId 门店 ID
     * @return 可用名额 VO 列表
     */
    @Operation(summary = "按门店查询可用名额")
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'STORE_MANAGER')")
    public Result<List<RecruitmentQuotaVO>> getAvailableQuotasByStore(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId) {
        List<RecruitmentQuotaVO> quotas = quotaService.getAvailableQuotasByStore(storeId);
        return Result.success(quotas);
    }

    /**
     * 校验名额可用性（提报招聘需求时调用）
     *
     * @param quotaId 名额 ID
     * @param count   本次需要的名额数（默认 1）
     * @return 校验结果（valid + remaining + errorCode）
     */
    @Operation(summary = "校验名额可用性")
    @PostMapping("/{quotaId}/validate")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER', 'STORE_MANAGER')")
    public Result<QuotaValidateResult> validateQuota(
            @Parameter(description = "名额 ID", required = true) @PathVariable Long quotaId,
            @Parameter(description = "本次需要的名额数（默认 1）")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "名额数必须大于0") int count) {
        QuotaValidateResult result = quotaService.validateQuota(quotaId, count);
        return Result.success(result);
    }
}
