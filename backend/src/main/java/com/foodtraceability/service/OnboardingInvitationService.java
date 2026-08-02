package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.InvitationSendRecord;

import java.util.List;

/**
 * 入职邀请码服务接口
 * 专门用于入职档案的邀请码管理
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
public interface OnboardingInvitationService extends IService<InvitationSendRecord> {

    /**
     * 生成邀请码
     *
     * @param archiveId 档案ID
     * @return 邀请码记录
     */
    InvitationSendRecord generateInvitationCode(Long archiveId);

    /**
     * 验证邀请码
     *
     * @param code  邀请码
     * @param email 邮箱
     * @param phone 手机号
     * @param name  姓名
     * @return 验证结果
     */
    boolean validateInvitationCode(String code, String email, String phone, String name);

    /**
     * 使用邀请码
     *
     * @param code   邀请码
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean useInvitationCode(String code, Long userId);

    /**
     * 撤销邀请码
     *
     * @param codeId 邀请码ID
     * @return 是否成功
     */
    boolean revokeInvitationCode(Long codeId);

    /**
     * 获取邀请码详情
     *
     * @param code 邀请码
     * @return 邀请码记录
     */
    InvitationSendRecord getInvitationCode(String code);

    /**
     * 根据档案ID获取邀请码
     *
     * @param archiveId 档案ID
     * @return 邀请码记录
     */
    InvitationSendRecord getInvitationCodeByArchiveId(Long archiveId);

    /**
     * 获取即将过期的邀请码列表
     *
     * @return 邀请码列表
     */
    List<InvitationSendRecord> getExpiringInvitationCodes();

    /**
     * 获取已过期的邀请码列表
     *
     * @return 邀请码列表
     */
    List<InvitationSendRecord> getExpiredInvitationCodes();

    /**
     * 延长邀请码有效期
     *
     * @param codeId   邀请码ID
     * @param addDays  增加的天数
     * @return 是否成功
     */
    boolean extendExpiration(Long codeId, int addDays);

    /**
     * 重新生成邀请码
     *
     * @param oldCodeId 旧邀请码ID
     * @return 新的邀请码记录
     */
    InvitationSendRecord regenerateInvitationCode(Long oldCodeId);
}
