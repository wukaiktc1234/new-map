package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.recruitment.JobOfferCreateDTO;
import com.foodtraceability.dto.recruitment.JobOfferQueryDTO;
import com.foodtraceability.dto.recruitment.JobOfferVO;
import com.foodtraceability.dto.recruitment.OfferRejectDTO;
import com.foodtraceability.entity.JobOffer;
import com.foodtraceability.entity.OnboardingRecord;
import com.foodtraceability.service.JobOfferService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 录用 Offer 控制器
 *
 * <p>对应 Ch3.3 录用 Offer 8 个 API 端点，覆盖 4 类业务事件触发：</p>
 * <ul>
 *   <li>sendOffer → offer.sent（候选人邮箱接收，EMAIL 渠道）</li>
 *   <li>acceptOffer → offer.accepted（HR 接收，SITE_MSG 渠道）</li>
 *   <li>rejectOffer → offer.rejected（HR 接收，SITE_MSG 渠道）</li>
 *   <li>withdrawOffer → offer.withdrawn（候选人邮箱接收，EMAIL 渠道）</li>
 * </ul>
 *
 * <p>createOffer 不发布事件（pending 状态），convertToOnboarding 不发布事件
 * （onboarding.completed 留 Sprint 3）。</p>
 */
@RestController
@RequestMapping("/v1/job-offers")
@Tag(name = "录用Offer", description = "录用 Offer 创建、发送、接受、拒绝、撤回、转入职接口")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    /**
     * 构造函数注入
     *
     * @param jobOfferService 录用 Offer 服务
     */
    public JobOfferController(JobOfferService jobOfferService) {
        this.jobOfferService = jobOfferService;
    }

    /**
     * HR 创建录用 Offer（pending 状态，不发布事件）
     *
     * @param dto Offer 创建参数
     * @return 已创建的 Offer 实体
     */
    @Operation(summary = "创建录用 Offer", description = "HR 创建录用 Offer，状态为 pending，发送时才触发 offer.sent 事件")
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<JobOffer> createOffer(@Valid @RequestBody JobOfferCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        JobOffer offer = jobOfferService.createOffer(dto, operatorId);
        return Result.success(offer);
    }

    /**
     * 查询 Offer 详情
     *
     * @param offerId Offer ID
     * @return Offer 实体
     */
    @Operation(summary = "查询 Offer 详情")
    @GetMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<JobOffer> getOfferById(
            @Parameter(description = "Offer ID", required = true) @PathVariable Long offerId) {
        JobOffer offer = jobOfferService.getOfferById(offerId);
        return Result.success(offer);
    }

    /**
     * 分页查询 Offer 列表
     *
     * @param query 查询参数（page/size/requirementId/candidateName/status）
     * @return Offer VO 分页结果
     */
    @Operation(summary = "分页查询 Offer 列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<IPage<JobOfferVO>> listOffers(@Valid JobOfferQueryDTO query) {
        IPage<JobOfferVO> page = jobOfferService.listOffers(query);
        return Result.success(page);
    }

    /**
     * HR 发送 Offer 给候选人（pending → sent）
     *
     * <p>发布 offer.sent 事件（接收人=候选人邮箱，渠道=EMAIL）。</p>
     *
     * @param offerId Offer ID
     * @return 是否发送成功
     */
    @Operation(summary = "发送 Offer", description = "HR 发送 Offer 给候选人，状态 pending → sent")
    @PutMapping("/{offerId}/send")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<Boolean> sendOffer(
            @Parameter(description = "Offer ID", required = true) @PathVariable Long offerId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = jobOfferService.sendOffer(offerId, operatorId);
        return Result.success(success);
    }

    /**
     * 候选人接受 Offer（sent → accepted）
     *
     * <p>发布 offer.accepted 事件（接收人=HR，渠道=SITE_MSG）。</p>
     *
     * @param offerId Offer ID
     * @return 是否接受成功
     */
    @Operation(summary = "候选人接受 Offer", description = "候选人接受 Offer（暂由 HR 代操作），状态 sent → accepted")
    @PutMapping("/{offerId}/accept")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<Boolean> acceptOffer(
            @Parameter(description = "Offer ID", required = true) @PathVariable Long offerId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = jobOfferService.acceptOffer(offerId, operatorId);
        return Result.success(success);
    }

    /**
     * 候选人拒绝 Offer（sent → rejected）
     *
     * <p>发布 offer.rejected 事件（接收人=HR，渠道=SITE_MSG）。</p>
     *
     * @param offerId Offer ID
     * @param dto     拒绝原因（可选）
     * @return 是否拒绝成功
     */
    @Operation(summary = "候选人拒绝 Offer", description = "候选人拒绝 Offer（暂由 HR 代操作），状态 sent → rejected")
    @PutMapping("/{offerId}/reject")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<Boolean> rejectOffer(
            @Parameter(description = "Offer ID", required = true) @PathVariable Long offerId,
            @RequestBody(required = false) OfferRejectDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = jobOfferService.rejectOffer(offerId, dto, operatorId);
        return Result.success(success);
    }

    /**
     * HR 撤回 Offer（sent/pending → withdrawn）
     *
     * <p>发布 offer.withdrawn 事件（接收人=候选人邮箱，渠道=EMAIL）。</p>
     *
     * @param offerId Offer ID
     * @return 是否撤回成功
     */
    @Operation(summary = "HR 撤回 Offer", description = "HR 撤回 Offer，状态 sent/pending → withdrawn")
    @PutMapping("/{offerId}/withdraw")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<Boolean> withdrawOffer(
            @Parameter(description = "Offer ID", required = true) @PathVariable Long offerId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = jobOfferService.withdrawOffer(offerId, operatorId);
        return Result.success(success);
    }

    /**
     * 接受 Offer 后办理转入职（accepted → onboarded）
     *
     * <p>同事务调用 onboardingRecordService 创建 OnboardingRecord（含岗位/薪资/试用期/门店信息）
     * + 更新 Offer 状态为 onboarded。</p>
     *
     * <p>不发布 onboarding.completed 事件（留 Sprint 3）。</p>
     *
     * @param offerId Offer ID
     * @return 已创建的入职记录
     */
    @Operation(summary = "转入职", description = "接受 Offer 后办理转入职，生成 OnboardingRecord，状态 accepted → onboarded")
    @PostMapping("/{offerId}/convert-to-onboarding")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public Result<OnboardingRecord> convertToOnboarding(
            @Parameter(description = "Offer ID", required = true) @PathVariable Long offerId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        OnboardingRecord record = jobOfferService.convertToOnboarding(offerId, operatorId);
        return Result.success(record);
    }
}
