package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.recruitment.JobOfferCreateDTO;
import com.foodtraceability.dto.recruitment.JobOfferQueryDTO;
import com.foodtraceability.dto.recruitment.JobOfferVO;
import com.foodtraceability.dto.recruitment.OfferRejectDTO;
import com.foodtraceability.entity.JobOffer;
import com.foodtraceability.entity.OnboardingRecord;

/**
 * 录用 Offer 服务接口
 *
 * <p>对应 Ch3.3 录用 Offer 8 个 API 端点，覆盖 4 类业务事件触发：</p>
 * <ul>
 *   <li>sendOffer → offer.sent（候选人邮箱接收，EMAIL 渠道）</li>
 *   <li>acceptOffer → offer.accepted（HR 接收）</li>
 *   <li>rejectOffer → offer.rejected（HR 接收）</li>
 *   <li>withdrawOffer → offer.withdrawn（候选人邮箱接收，EMAIL 渠道）</li>
 * </ul>
 *
 * <p>createOffer 不发布事件（pending 状态），convertToOnboarding 不发布事件（onboarding.completed 留 Sprint 3）。</p>
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT + try-catch 隔离）、
 * plan.md 11.3 方案 A（候选人无账号时传 HR userId + variables 携带 candidateEmail）。</p>
 */
public interface JobOfferService extends IService<JobOffer> {

    /**
     * 创建 Offer（pending 状态，不发布事件）。
     *
     * @param dto        Offer 创建参数
     * @param operatorId 操作人 ID（HR）
     * @return 已创建的 Offer 实体
     */
    JobOffer createOffer(JobOfferCreateDTO dto, Long operatorId);

    /**
     * 查询 Offer 详情。
     *
     * @param offerId Offer ID
     * @return Offer 实体
     */
    JobOffer getOfferById(Long offerId);

    /**
     * 分页查询 Offer 列表。
     *
     * @param query 查询参数
     * @return Offer VO 分页结果
     */
    IPage<JobOfferVO> listOffers(JobOfferQueryDTO query);

    /**
     * 发送 Offer（pending → sent）。
     *
     * <p>状态机校验 pending→sent→写入 sendTime→发布 offer.sent 事件。</p>
     * <p>接收人特殊处理：候选人若已是系统用户则用 userId，否则传 HR userId + variables 携带 candidateEmail
     * （plan.md 11.3 方案 A，待澄清）。</p>
     *
     * @param offerId    Offer ID
     * @param operatorId 操作人 ID（HR）
     * @return 是否发送成功
     */
    boolean sendOffer(Long offerId, Long operatorId);

    /**
     * 候选人接受 Offer（sent → accepted）。
     *
     * <p>状态机校验 sent→accepted→写入 acceptTime→发布 offer.accepted 事件（接收人=HR）。</p>
     *
     * @param offerId    Offer ID
     * @param operatorId 操作人 ID（候选人，暂用 HR 代操作）
     * @return 是否接受成功
     */
    boolean acceptOffer(Long offerId, Long operatorId);

    /**
     * 候选人拒绝 Offer（sent → rejected）。
     *
     * <p>状态机校验 sent→rejected→写入 rejectTime/rejectReason→发布 offer.rejected 事件（接收人=HR）。</p>
     *
     * @param offerId    Offer ID
     * @param dto        拒绝原因（可选）
     * @param operatorId 操作人 ID（候选人，暂用 HR 代操作）
     * @return 是否拒绝成功
     */
    boolean rejectOffer(Long offerId, OfferRejectDTO dto, Long operatorId);

    /**
     * HR 撤回 Offer（sent/pending → withdrawn）。
     *
     * <p>状态机校验 sent/pending→withdrawn→发布 offer.withdrawn 事件（接收人=候选人邮箱）。</p>
     *
     * @param offerId    Offer ID
     * @param operatorId 操作人 ID（HR）
     * @return 是否撤回成功
     */
    boolean withdrawOffer(Long offerId, Long operatorId);

    /**
     * 转入职（accepted → onboarded）。
     *
     * <p>状态机校验 accepted→onboarded→同事务调用 onboardingRecordService 创建 OnboardingRecord
     * （含岗位/薪资/试用期/门店信息）→updateById Offer 状态。</p>
     * <p>不发布 onboarding.completed 事件（留 Sprint 3）。</p>
     *
     * @param offerId    Offer ID
     * @param operatorId 操作人 ID（HR）
     * @return 已创建的入职记录
     */
    OnboardingRecord convertToOnboarding(Long offerId, Long operatorId);
}
