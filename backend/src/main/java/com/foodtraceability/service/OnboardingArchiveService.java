package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.OnboardingArchive;

import java.util.List;

/**
 * 入职档案服务接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
public interface OnboardingArchiveService extends IService<OnboardingArchive> {

    /**
     * 创建入职档案
     *
     * @param archive 档案信息
     * @return 创建后的档案
     */
    OnboardingArchive createArchive(OnboardingArchive archive);

    /**
     * 更新入职档案
     *
     * @param archive 档案信息
     * @return 更新后的档案
     */
    OnboardingArchive updateArchive(OnboardingArchive archive);

    /**
     * 根据状态查询档案列表
     *
     * @param status 状态
     * @return 档案列表
     */
    List<OnboardingArchive> getArchivesByStatus(String status);

    /**
     * 根据部门ID查询档案列表
     *
     * @param departmentId 部门ID
     * @return 档案列表
     */
    List<OnboardingArchive> getArchivesByDepartment(String departmentId);

    /**
     * 获取待HR审查的档案列表
     *
     * @return 档案列表
     */
    List<OnboardingArchive> getPendingHrReviewArchives();

    /**
     * 获取待实质审查的档案列表
     *
     * @return 档案列表
     */
    List<OnboardingArchive> getPendingSubstantiveReviewArchives();

    /**
     * 提交档案审批
     *
     * @param archiveId 档案ID
     * @param hrId      HR ID
     * @return 是否成功
     */
    boolean submitForApproval(Long archiveId, Long hrId);

    /**
     * 根据邀请码ID查询档案
     *
     * @param invitationCodeId 邀请码ID
     * @return 档案
     */
    OnboardingArchive getArchiveByInvitationCodeId(Long invitationCodeId);
}
