package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.recruitment.QuotaAdjustmentDTO;
import com.foodtraceability.dto.recruitment.QuotaAdjustmentReviewDTO;
import com.foodtraceability.dto.recruitment.QuotaRejectDTO;
import com.foodtraceability.dto.recruitment.QuotaValidateResult;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaCreateDTO;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaQueryDTO;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaVO;
import com.foodtraceability.entity.RecruitmentQuota;

import java.util.List;

/**
 * 招聘名额服务接口
 *
 * <p>对应 Ch3.1 招聘名额系统 10 个 API 端点，覆盖 8 类业务事件触发：</p>
 * <ul>
 *   <li>issueQuota → recruitment.quota.issued</li>
 *   <li>confirmQuota → recruitment.quota.confirmed</li>
 *   <li>rejectQuota → recruitment.quota.rejected</li>
 *   <li>requestAdjustment → recruitment.quota.adjustment_requested</li>
 *   <li>approveAdjustment → recruitment.quota.adjustment_result</li>
 *   <li>closeQuota → recruitment.quota.closed</li>
 *   <li>incrementUsedCount(80%) → recruitment.quota.exhausting</li>
 *   <li>incrementUsedCount(100%) → recruitment.quota.exhausted</li>
 * </ul>
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT）、ADR-006（无缓存层直接 ServiceImpl）、
 * ADR-007（80% 实时检查 + 原子计数避免超卖）。</p>
 */
public interface RecruitmentQuotaService extends IService<RecruitmentQuota> {

    /**
     * HR 下发名额。
     *
     * <p>填充门店/岗位名称→状态设 issued→save→查询门店店长接收人→
     * 构造 variables→发布 recruitment.quota.issued 事件。</p>
     *
     * @param dto        名额下发参数
     * @param operatorId 操作人 ID（HR）
     * @return 已下发的名额实体
     */
    RecruitmentQuota issueQuota(RecruitmentQuotaCreateDTO dto, Long operatorId);

    /**
     * 查询名额详情。
     *
     * @param quotaId 名额 ID
     * @return 名额实体
     */
    RecruitmentQuota getQuotaById(Long quotaId);

    /**
     * 分页查询名额列表。
     *
     * @param query 查询参数
     * @return 名额 VO 分页结果
     */
    IPage<RecruitmentQuotaVO> listQuotas(RecruitmentQuotaQueryDTO query);

    /**
     * 门店确认名额（issued → active）。
     *
     * <p>状态机校验 issued→active→写入 confirmedBy/confirmedTime→
     * 发布 recruitment.quota.confirmed 事件。</p>
     *
     * @param quotaId    名额 ID
     * @param operatorId 操作人 ID（门店店长）
     * @return 是否确认成功
     */
    boolean confirmQuota(Long quotaId, Long operatorId);

    /**
     * 门店拒绝名额（issued → rejected）。
     *
     * <p>状态机校验 issued→rejected→写入备注→
     * 发布 recruitment.quota.rejected 事件。</p>
     *
     * @param quotaId    名额 ID
     * @param dto        拒绝原因
     * @param operatorId 操作人 ID（门店店长）
     * @return 是否拒绝成功
     */
    boolean rejectQuota(Long quotaId, QuotaRejectDTO dto, Long operatorId);

    /**
     * 门店申请名额追加（active → adjustment_requested）。
     *
     * <p>状态机校验 active→adjustment_requested→写入备注→
     * 发布 recruitment.quota.adjustment_requested 事件。</p>
     *
     * @param quotaId    名额 ID
     * @param dto        追加申请参数
     * @param operatorId 操作人 ID（门店店长）
     * @return 是否申请成功
     */
    boolean requestAdjustment(Long quotaId, QuotaAdjustmentDTO dto, Long operatorId);

    /**
     * HR 审核名额追加（adjustment_requested → active/issued）。
     *
     * <p>状态机校验 adjustment_requested→active/issued→approved=true 时 headcount+=additionalCount→
     * 发布 recruitment.quota.adjustment_result 事件。</p>
     *
     * @param quotaId    名额 ID
     * @param dto        审核结果
     * @param operatorId 操作人 ID（HR）
     * @return 是否审核成功
     */
    boolean approveAdjustment(Long quotaId, QuotaAdjustmentReviewDTO dto, Long operatorId);

    /**
     * HR 关闭名额（→ closed）。
     *
     * <p>状态机校验 issued/active/exhausted/adjustment_requested→closed→
     * 发布 recruitment.quota.closed 事件。</p>
     *
     * @param quotaId    名额 ID
     * @param operatorId 操作人 ID（HR）
     * @return 是否关闭成功
     */
    boolean closeQuota(Long quotaId, Long operatorId);

    /**
     * 按门店查询可用名额（status=active）。
     *
     * @param storeId 门店 ID
     * @return 可用名额 VO 列表
     */
    List<RecruitmentQuotaVO> getAvailableQuotasByStore(Long storeId);

    /**
     * 校验名额可用性（提报招聘需求时调用）。
     *
     * <p>查询名额→校验状态 active→校验 remaining=headcount-used_count>=count→
     * 返回 QuotaValidateResult。</p>
     *
     * @param quotaId 名额 ID
     * @param count   本次需要的名额数
     * @return 校验结果（valid + remaining + errorCode）
     */
    QuotaValidateResult validateQuota(Long quotaId, int count);

    /**
     * 名额使用计数 +1（入职成功时调用，Sprint 2 预留，Sprint 3 OnboardingRecord 调用）。
     *
     * <p>原子更新：UPDATE ... SET used_count = used_count + 1 WHERE used_count &lt; headcount。</p>
     * <p>80% 阈值检查：incrementUsedCount 后实时计算，达到首次 80% 触发 exhausting 事件。</p>
     * <p>100% 状态变更：达到 headcount 时 status → exhausted + 触发 exhausted 事件。</p>
     *
     * @param quotaId 名额 ID
     * @return true=计数成功, false=名额已用完
     */
    boolean incrementUsedCount(Long quotaId);
}
