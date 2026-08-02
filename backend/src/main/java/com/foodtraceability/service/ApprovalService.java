package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.ApprovalRecordWithArchiveDTO;
import com.foodtraceability.entity.ApprovalRecord;

import java.util.List;

/**
 * 审批服务接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
public interface ApprovalService extends IService<ApprovalRecord> {

    /**
     * 创建审批记录
     *
     * @param archiveId 档案ID
     * @param stepOrder 步骤序号
     * @param stepName  步骤名称
     * @param reviewType 审查类型
     * @return 审批记录
     */
    ApprovalRecord createApprovalRecord(Long archiveId, Integer stepOrder, String stepName, String reviewType);

    /**
     * 审批通过
     *
     * @param recordId   记录ID
     * @param reviewerId 审批人ID
     * @param comment    审批意见
     * @return 是否成功
     */
    boolean approve(Long recordId, Long reviewerId, String comment);

    /**
     * 审批拒绝
     *
     * @param recordId   记录ID
     * @param reviewerId 审批人ID
     * @param comment    拒绝原因
     * @return 是否成功
     */
    boolean reject(Long recordId, Long reviewerId, String comment);

    /**
     * 根据档案ID查询审批记录
     *
     * @param archiveId 档案ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> getApprovalsByArchiveId(Long archiveId);

    /**
     * 获取待审批列表（根据审批人）
     *
     * @param reviewerId 审批人ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> getPendingApprovals(Long reviewerId);

    /**
     * 获取待审批列表（带档案信息）
     *
     * @param reviewerId 审批人ID
     * @return 审批记录与档案关联DTO列表
     */
    List<ApprovalRecordWithArchiveDTO> getPendingApprovalsWitArchive(Long reviewerId);

    /**
     * 获取档案的最新审批记录
     *
     * @param archiveId 档案ID
     * @return 最新审批记录
     */
    ApprovalRecord getLatestApproval(Long archiveId);

    /**
     * 检查审批流程是否完成
     *
     * @param archiveId 档案ID
     * @return 是否完成
     */
    boolean isApprovalProcessComplete(Long archiveId);

    /**
     * 根据职位级别获取审批流程
     *
     * @param positionLevel 职位级别
     * @return 审批步骤列表
     */
    List<ApprovalStep> getApprovalFlowByPositionLevel(String positionLevel);

    /**
     * 启动审批流程
     *
     * @param archiveId 档案ID
     * @param hrId      HR ID
     * @return 是否成功
     */
    boolean startApprovalProcess(Long archiveId, Long hrId);

    /**
     * 获取审批历史（已完成的审批记录，带档案信息）
     *
     * @return 审批记录与档案关联DTO列表
     */
    List<ApprovalRecordWithArchiveDTO> getApprovalHistory();

    /**
     * 审批步骤内部类
     */
    class ApprovalStep {
        private Integer order;
        private String name;
        private String type;

        public ApprovalStep(Integer order, String name, String type) {
            this.order = order;
            this.name = name;
            this.type = type;
        }

        public Integer getOrder() { return order; }
        public String getName() { return name; }
        public String getType() { return type; }
    }
}
