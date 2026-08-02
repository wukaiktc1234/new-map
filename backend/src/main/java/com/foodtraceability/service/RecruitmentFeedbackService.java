package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.recruitment.RecruitmentFeedbackCreateDTO;
import com.foodtraceability.dto.recruitment.RecruitmentFeedbackVO;
import com.foodtraceability.dto.recruitment.RequirementResubmitDTO;
import com.foodtraceability.dto.recruitment.StoreEvaluationDTO;
import com.foodtraceability.entity.RecruitmentFeedback;

import java.util.List;

/**
 * 招聘反馈服务接口
 *
 * <p>对应 Ch3.2 招聘协同 4 个 API 端点，覆盖 5 类业务事件触发：</p>
 * <ul>
 *   <li>submitFeedback(approve) → recruitment.approved</li>
 *   <li>submitFeedback(feedback) → recruitment.feedback.given</li>
 *   <li>submitFeedback(reject) → recruitment.rejected</li>
 *   <li>submitStoreEvaluation → interview.store_evaluation.submitted</li>
 *   <li>resubmitRequirement → recruitment.requirement.resubmitted</li>
 * </ul>
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT + try-catch 隔离）、
 * ADR-008（不混淆 Feedback 与 Approval 概念，不修改 RecruitmentApproval 模块）。</p>
 */
public interface RecruitmentFeedbackService extends IService<RecruitmentFeedback> {

    /**
     * HR 提交反馈（approve/feedback/reject）。
     *
     * <p>校验需求状态 open→根据 feedbackType 流转：</p>
     * <ul>
     *   <li>approve → approved，发布 recruitment.approved 事件（接收人=门店店长）</li>
     *   <li>feedback → feedback_given，发布 recruitment.feedback.given 事件（接收人=门店店长）</li>
     *   <li>reject → rejected，发布 recruitment.rejected 事件（接收人=门店店长）</li>
     * </ul>
     *
     * @param dto        反馈参数
     * @param operatorId 操作人 ID（HR）
     * @return 已保存的反馈实体
     */
    RecruitmentFeedback submitFeedback(RecruitmentFeedbackCreateDTO dto, Long operatorId);

    /**
     * 查询需求的反馈历史。
     *
     * @param requirementId 招聘需求 ID
     * @return 反馈 VO 列表（按时间倒序）
     */
    List<RecruitmentFeedbackVO> getFeedbackHistory(String requirementId);

    /**
     * 门店提交面评。
     *
     * <p>查询面试记录→校验评分 1-5→更新 interview 5 字段
     * （storeInterviewerId/storeInterviewerName/storeEvaluation/storeEvaluationTime/storeEvaluationScore）→
     * 发布 interview.store_evaluation.submitted 事件（接收人=HR 招聘员）。</p>
     *
     * @param interviewId 面试 ID
     * @param dto         面评参数
     * @param operatorId  操作人 ID（门店面试官）
     * @return 是否提交成功
     */
    boolean submitStoreEvaluation(String interviewId, StoreEvaluationDTO dto, Long operatorId);

    /**
     * 门店重新提交需求（feedback_given → open）。
     *
     * <p>状态机校验 feedback_given→open→更新需求字段（description/requirements/salaryRange）→
     * 发布 recruitment.requirement.resubmitted 事件（接收人=HR 招聘员）。</p>
     *
     * @param requirementId 招聘需求 ID
     * @param dto           重新提交参数（字段均可选）
     * @param operatorId    操作人 ID（门店店长）
     * @return 是否重新提交成功
     */
    boolean resubmitRequirement(String requirementId, RequirementResubmitDTO dto, Long operatorId);
}
