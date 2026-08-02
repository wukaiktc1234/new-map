package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.DeadLetterQueryDTO;
import com.foodtraceability.dto.DeadLetterVO;
import com.foodtraceability.entity.NotificationDeadLetter;

/**
 * 通知死信管理服务接口
 *
 * <p>提供死信记录的查询、保存、重试、删除、标记处理等功能。
 * 对应 spec NC-006（死信队列处理）和 plan.md ADR-003（死信处理策略）。</p>
 *
 * <p>事务边界：
 * <ul>
 *   <li>saveDeadLetter 使用 REQUIRES_NEW 独立事务，避免外层事务回滚影响死信持久化</li>
 *   <li>其他方法使用默认事务（REQUIRED）</li>
 * </ul></p>
 */
public interface NotificationDeadLetterService {

    /**
     * 分页查询死信列表。
     *
     * @param query 查询条件（channel / resolved / bizType / 时间范围 / 分页参数）
     * @return 死信 VO 分页结果
     */
    IPage<DeadLetterVO> getDeadLetterPage(DeadLetterQueryDTO query);

    /**
     * 根据 ID 查询死信详情（含处理人用户名）。
     *
     * @param deadLetterId 死信 ID
     * @return 死信 VO，不存在返回 null
     */
    DeadLetterVO getDeadLetterById(Long deadLetterId);

    /**
     * 保存死信记录（独立事务）。
     *
     * <p>由 NotificationMessageConsumer.onDeadLetter() 调用，
     * 在消息重试 3 次仍失败后持久化到 notification_dead_letter 表。</p>
     *
     * @param deadLetter 死信实体
     */
    void saveDeadLetter(NotificationDeadLetter deadLetter);

    /**
     * 重试死信消息（重新发送原始 msg_send_record）。
     *
     * @param deadLetterId 死信 ID
     * @param operatorId 操作人 ID
     * @return 是否成功触发重试
     */
    boolean retryDeadLetter(Long deadLetterId, Long operatorId);

    /**
     * 删除死信记录（逻辑删除）。
     *
     * @param deadLetterId 死信 ID
     * @return 是否删除成功
     */
    boolean deleteDeadLetter(Long deadLetterId);

    /**
     * 标记死信已处理。
     *
     * @param deadLetterId 死信 ID
     * @param operatorId 操作人 ID
     * @param remark 处理备注
     * @return 是否标记成功
     */
    boolean resolveDeadLetter(Long deadLetterId, Long operatorId, String remark);
}
