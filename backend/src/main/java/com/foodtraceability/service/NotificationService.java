package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.MsgSendDTO;
import com.foodtraceability.dto.MsgTemplateCreateDTO;
import com.foodtraceability.dto.NotificationSettingUpdateDTO;
import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.MsgTemplate;
import com.foodtraceability.entity.NotificationSettingEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息通知服务接口
 * 提供模板管理、消息发送、设置管理、统计仪表盘等功能
 */
public interface NotificationService {

    // ==================== 模板管理 ====================

    /**
     * 分页查询模板列表
     *
     * @param page 分页参数
     * @param templateCode 模板编码(模糊)
     * @param templateName 模板名称(模糊)
     * @param templateType 模板类型
     * @param status 状态
     * @param channel 发送渠道
     * @return 分页结果
     */
    IPage<MsgTemplate> getTemplatePage(Page<MsgTemplate> page, String templateCode,
                                       String templateName, Integer templateType,
                                       Integer status, String channel);

    /**
     * 根据ID获取模板详情
     *
     * @param templateId 模板ID
     * @return 模板实体
     */
    MsgTemplate getTemplateById(Long templateId);

    /**
     * 根据编码获取模板
     *
     * @param templateCode 模板编码
     * @return 模板实体
     */
    MsgTemplate getTemplateByCode(String templateCode);

    /**
     * 创建新模板
     *
     * @param dto 创建请求DTO
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 创建的模板实体
     */
    MsgTemplate createTemplate(MsgTemplateCreateDTO dto, Long userId, String username);

    /**
     * 更新模板
     *
     * @param templateId 模板ID
     * @param dto 更新数据（复用CreateDTO）
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean updateTemplate(Long templateId, MsgTemplateCreateDTO dto, Long userId, String username);

    /**
     * 删除模板（逻辑删除）
     *
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean deleteTemplate(Long templateId);

    /**
     * 启用模板
     *
     * @param templateId 模板ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean enableTemplate(Long templateId, Long userId, String username);

    /**
     * 禁用模板
     *
     * @param templateId 模板ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean disableTemplate(Long templateId, Long userId, String username);

    /**
     * 预览模板渲染效果
     *
     * @param templateCode 模板编码
     * @param variables 变量数据
     * @return 渲染结果Map(subject, content)
     */
    Map<String, String> previewTemplate(String templateCode, Map<String, Object> variables);

    // ==================== 消息发送 ====================

    /**
     * 发送单条消息
     *
     * @param dto 发送请求DTO
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 发送记录ID
     */
    Long sendMessage(MsgSendDTO dto, Long userId, String username);

    /**
     * 批量发送消息
     *
     * @param dto 发送请求DTO（包含recipients列表）
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 发送记录ID列表
     */
    List<Long> sendBatchMessage(MsgSendDTO dto, Long userId, String username);

    /**
     * 重试失败的消息
     *
     * @param recordId 记录ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean retryMessage(Long recordId, Long userId, String username);

    /**
     * 分页查询发送记录
     *
     * @param page 分页参数
     * @param recipient 收件人(模糊)
     * @param channel 渠道
     * @param sendStatus 状态
     * @param bizType 业务类型
     * @param triggerType 触发类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    IPage<MsgSendRecord> getRecordPage(Page<MsgSendRecord> page, String recipient,
                                       String channel, Integer sendStatus,
                                       String bizType, Integer triggerType,
                                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取发送记录详情
     *
     * @param recordId 记录ID
     * @return 发送记录实体
     */
    MsgSendRecord getRecordById(Long recordId);

    // ==================== 设置管理 ====================

    /**
     * 获取所有通知设置（按分组）
     *
     * @param settingGroup 设置分组（可选）
     * @return 设置列表
     */
    List<NotificationSettingEntity> getSettings(String settingGroup);

    /**
     * 获取单个设置值
     *
     * @param settingKey 设置键
     * @return 设置值
     */
    String getSettingValue(String settingKey);

    /**
     * 更新或创建设置
     *
     * @param dto 更新请求DTO
     * @return 是否成功
     */
    boolean updateSetting(NotificationSettingUpdateDTO dto);

    /**
     * 批量更新设置
     *
     * @param settings 设置列表
     * @return 是否成功
     */
    boolean batchUpdateSettings(List<NotificationSettingUpdateDTO> settings);

    // ==================== 统计仪表盘 ====================

    /**
     * 获取发送统计数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据Map
     */
    Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取仪表盘概览数据
     *
     * @return 仪表盘数据Map
     */
    Map<String, Object> getDashboard();

    /**
     * 获取渠道列表
     *
     * @return 渠道列表
     */
    List<String> getChannelList();

    /**
     * 获取指定模板的变量定义
     *
     * @param templateCode 模板编码
     * @return 变量列表
     */
    List<String> getTemplateVariables(String templateCode);

    // ==================== 业务通知方法 ====================

    /**
     * 发送自动审核通过通知
     *
     * @param userId   用户ID
     * @param recordId 记录ID
     * @param bizType  业务类型
     */
    void sendAutoApproveNotification(Long userId, Long recordId, String bizType);

    /**
     * 发送审核拒绝通知
     *
     * @param userId      用户ID
     * @param recordId    记录ID
     * @param bizType     业务类型
     * @param auditRemark 审核备注
     */
    void sendRejectAuditNotification(Long userId, Long recordId, String bizType, String auditRemark);

    /**
     * 发送审核通过通知
     *
     * @param userId   用户ID
     * @param recordId 记录ID
     * @param bizType  业务类型
     */
    void sendApproveAuditNotification(Long userId, Long recordId, String bizType);
}
