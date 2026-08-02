package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.DeadLetterQueryDTO;
import com.foodtraceability.dto.DeadLetterVO;
import com.foodtraceability.entity.NotificationDeadLetter;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.NotificationDeadLetterMapper;
import com.foodtraceability.service.NotificationDeadLetterService;
import com.foodtraceability.service.NotificationService;
import com.foodtraceability.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知死信管理服务实现
 *
 * <p>对应 spec NC-006 / plan.md ADR-003 / plan.md 第 6.2.5 节。</p>
 *
 * <p>事务策略：
 * <ul>
 *   <li>saveDeadLetter 使用 REQUIRES_NEW 独立事务，避免外层消费者事务回滚影响死信持久化</li>
 *   <li>retryDeadLetter / resolveDeadLetter / deleteDeadLetter 使用默认 REQUIRED 事务</li>
 *   <li>查询方法不需要事务</li>
 * </ul></p>
 */
@Service
public class NotificationDeadLetterServiceImpl
        extends ServiceImpl<NotificationDeadLetterMapper, NotificationDeadLetter>
        implements NotificationDeadLetterService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDeadLetterServiceImpl.class);

    /** 处理状态：未处理 */
    private static final int RESOLVED_NO = 0;

    /** 处理状态：已处理 */
    private static final int RESOLVED_YES = 1;

    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 构造函数注入依赖。
     *
     * @param userService 用户服务（查询处理人用户名）
     * @param notificationService 通知服务（重试死信消息）
     */
    public NotificationDeadLetterServiceImpl(UserService userService,
                                              NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Override
    public IPage<DeadLetterVO> getDeadLetterPage(DeadLetterQueryDTO query) {
        Page<NotificationDeadLetter> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<NotificationDeadLetter> wrapper = new LambdaQueryWrapper<>();

        if (query.getChannel() != null && !query.getChannel().isEmpty()) {
            wrapper.eq(NotificationDeadLetter::getChannel, query.getChannel());
        }
        if (query.getResolved() != null) {
            wrapper.eq(NotificationDeadLetter::getResolved, query.getResolved());
        }
        if (query.getBizType() != null && !query.getBizType().isEmpty()) {
            wrapper.eq(NotificationDeadLetter::getBizType, query.getBizType());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(NotificationDeadLetter::getDeadLetterTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(NotificationDeadLetter::getDeadLetterTime, query.getEndTime());
        }
        wrapper.orderByDesc(NotificationDeadLetter::getDeadLetterTime);

        IPage<NotificationDeadLetter> entityPage = this.page(page, wrapper);
        List<NotificationDeadLetter> records = entityPage.getRecords();

        // FIX-017(M-3): 批量查询处理人用户名,避免 toVO 中逐个 getById 的 N+1 查询
        Map<Long, String> usernameMap = batchGetUsernames(records);

        return entityPage.convert(entity -> toVO(entity, usernameMap));
    }

    @Override
    public DeadLetterVO getDeadLetterById(Long deadLetterId) {
        NotificationDeadLetter entity = this.getById(deadLetterId);
        if (entity == null) {
            return null;
        }
        // 单条查询直接使用 resolveUsername
        Map<Long, String> usernameMap = new HashMap<>();
        if (entity.getResolvedBy() != null) {
            usernameMap.put(entity.getResolvedBy(), resolveUsername(entity.getResolvedBy()));
        }
        return toVO(entity, usernameMap);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveDeadLetter(NotificationDeadLetter deadLetter) {
        if (deadLetter.getDeadLetterTime() == null) {
            deadLetter.setDeadLetterTime(LocalDateTime.now());
        }
        if (deadLetter.getResolved() == null) {
            deadLetter.setResolved(RESOLVED_NO);
        }
        if (deadLetter.getSendStatus() == null) {
            deadLetter.setSendStatus(3); // FAILED
        }
        this.save(deadLetter);
        logger.info("死信记录已持久化: deadLetterId={}, originalRecordId={}, channel={}, reason={}",
                deadLetter.getDeadLetterId(), deadLetter.getOriginalRecordId(),
                deadLetter.getChannel(), deadLetter.getDeadLetterReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryDeadLetter(Long deadLetterId, Long operatorId) {
        // FIX-007(C5): CAS 占位,避免并发重试
        // UPDATE notification_dead_letter SET resolved=1, resolved_by=?, resolved_time=?, resolve_remark='RETRYING'
        // WHERE dead_letter_id=? AND resolved=0
        NotificationDeadLetter casUpdate = new NotificationDeadLetter();
        casUpdate.setResolved(RESOLVED_YES);
        casUpdate.setResolvedBy(operatorId);
        casUpdate.setResolvedTime(LocalDateTime.now());
        casUpdate.setResolveRemark("RETRYING");
        LambdaUpdateWrapper<NotificationDeadLetter> casWrapper = new LambdaUpdateWrapper<>();
        casWrapper.eq(NotificationDeadLetter::getDeadLetterId, deadLetterId)
                  .eq(NotificationDeadLetter::getResolved, RESOLVED_NO);
        boolean casSuccess = this.update(casUpdate, casWrapper);
        if (!casSuccess) {
            logger.warn("死信重试 CAS 占位失败(已被处理或不存在): deadLetterId={}, operatorId={}",
                    deadLetterId, operatorId);
            return false;
        }

        NotificationDeadLetter entity = this.getById(deadLetterId);
        if (entity == null || entity.getOriginalRecordId() == null) {
            // 极端情况:CAS 成功但再次查询失败,回滚占位
            rollbackRetryPlaceholder(deadLetterId);
            logger.warn("重试死信失败, 死信记录或原始记录ID缺失: deadLetterId={}", deadLetterId);
            return false;
        }

        String operatorName = resolveUsername(operatorId);
        boolean success;
        try {
            success = notificationService.retryMessage(
                    entity.getOriginalRecordId(), operatorId, operatorName);
        } catch (IllegalArgumentException e) {
            // FIX-012(B5): retryMessage 抛 IllegalArgumentException 时回滚占位,让用户可再次重试
            rollbackRetryPlaceholder(deadLetterId);
            logger.warn("死信重试参数非法: deadLetterId={}, error={}", deadLetterId, e.getMessage());
            return false;
        }

        if (success) {
            // FIX-007(C7): 重试成功,更新 resolve_remark 为 RETRY_SUCCESS(保持 resolved=1)
            updateRetryResultRemark(deadLetterId, "RETRY_SUCCESS");
            logger.info("死信重试已触发: deadLetterId={}, originalRecordId={}, operatorId={}",
                    deadLetterId, entity.getOriginalRecordId(), operatorId);
        } else {
            // FIX-007(C7): 重试失败,回滚占位(resolved=1→0),清空 resolved_by/time/remark,让用户可再次重试
            rollbackRetryPlaceholder(deadLetterId);
            logger.warn("死信重试失败, 已回滚占位: deadLetterId={}, originalRecordId={}",
                    deadLetterId, entity.getOriginalRecordId());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeadLetter(Long deadLetterId) {
        boolean success = this.removeById(deadLetterId);
        if (success) {
            logger.info("死信记录已删除: deadLetterId={}", deadLetterId);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resolveDeadLetter(Long deadLetterId, Long operatorId, String remark) {
        NotificationDeadLetter entity = this.getById(deadLetterId);
        if (entity == null) {
            logger.warn("标记死信处理失败, 死信记录不存在: deadLetterId={}", deadLetterId);
            return false;
        }
        // FIX-020(B4): 状态守卫,已处理的死信不允许重复标记
        if (entity.getResolved() != null && entity.getResolved() == RESOLVED_YES) {
            logger.warn("死信已处理, 不允许重复标记: deadLetterId={}, currentResolvedBy={}",
                    deadLetterId, entity.getResolvedBy());
            return false;
        }

        entity.setResolved(RESOLVED_YES);
        entity.setResolvedBy(operatorId);
        entity.setResolvedTime(LocalDateTime.now());
        entity.setResolveRemark(remark);

        boolean success = this.updateById(entity);
        if (success) {
            logger.info("死信已标记处理: deadLetterId={}, operatorId={}, remark={}",
                    deadLetterId, operatorId, remark);
        }
        return success;
    }

    /**
     * 实体转 VO。
     *
     * <p>FIX-017(M-3): usernameMap 由调用方批量查询后传入,避免 N+1。</p>
     *
     * @param entity 死信实体
     * @param usernameMap 用户 ID → 用户名 映射
     * @return 死信 VO
     */
    private DeadLetterVO toVO(NotificationDeadLetter entity, Map<Long, String> usernameMap) {
        if (entity == null) {
            return null;
        }
        DeadLetterVO vo = new DeadLetterVO();
        vo.setDeadLetterId(entity.getDeadLetterId());
        vo.setOriginalRecordId(entity.getOriginalRecordId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setRecipient(entity.getRecipient());
        vo.setRecipientType(entity.getRecipientType());
        vo.setSubject(entity.getSubject());
        vo.setContent(entity.getContent());
        vo.setChannel(entity.getChannel());
        vo.setSendStatus(entity.getSendStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setRetryCount(entity.getRetryCount());
        vo.setMaxRetry(entity.getMaxRetry());
        vo.setOriginalMessageId(entity.getOriginalMessageId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setDeadLetterReason(entity.getDeadLetterReason());
        vo.setDeadLetterTime(entity.getDeadLetterTime());
        vo.setResolved(entity.getResolved());
        vo.setResolvedBy(entity.getResolvedBy());
        vo.setResolvedByUsername(usernameMap != null ? usernameMap.get(entity.getResolvedBy()) : null);
        vo.setResolvedTime(entity.getResolvedTime());
        vo.setResolveRemark(entity.getResolveRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 批量查询用户名映射。
     *
     * <p>FIX-017(M-3): 收集所有 resolvedBy 用户 ID,一次 listByIds 查询全部用户名。</p>
     *
     * @param records 死信记录列表
     * @return 用户 ID → 用户名 映射
     */
    private Map<Long, String> batchGetUsernames(List<NotificationDeadLetter> records) {
        Map<Long, String> result = new HashMap<>();
        if (records == null || records.isEmpty()) {
            return result;
        }
        Set<Long> userIds = new HashSet<>();
        for (NotificationDeadLetter record : records) {
            if (record.getResolvedBy() != null) {
                userIds.add(record.getResolvedBy());
            }
        }
        if (userIds.isEmpty()) {
            return result;
        }
        try {
            List<User> users = userService.listByIds(new ArrayList<>(userIds));
            if (users != null) {
                for (User user : users) {
                    if (user != null && user.getId() != null) {
                        result.put(user.getId(), user.getUsername());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("批量查询用户名失败, 返回空映射: error={}", e.getMessage());
        }
        return result;
    }

    /**
     * 回滚重试占位状态。
     *
     * <p>FIX-007(C7): 重试失败时调用,将 resolved=1 回滚为 0,清空 resolved_by/time/remark,
     * 让用户可以再次发起重试。</p>
     *
     * @param deadLetterId 死信 ID
     */
    private void rollbackRetryPlaceholder(Long deadLetterId) {
        try {
            NotificationDeadLetter rollback = new NotificationDeadLetter();
            rollback.setResolved(RESOLVED_NO);
            rollback.setResolvedBy(null);
            rollback.setResolvedTime(null);
            rollback.setResolveRemark(null);
            LambdaUpdateWrapper<NotificationDeadLetter> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(NotificationDeadLetter::getDeadLetterId, deadLetterId)
                   .eq(NotificationDeadLetter::getResolved, RESOLVED_YES)
                   .eq(NotificationDeadLetter::getResolveRemark, "RETRYING");
            this.update(rollback, wrapper);
        } catch (Exception e) {
            logger.error("回滚重试占位失败: deadLetterId={}, error={}",
                    deadLetterId, e.getMessage(), e);
        }
    }

    /**
     * 更新重试结果备注（保持 resolved=1）。
     *
     * @param deadLetterId 死信 ID
     * @param remark 备注内容（RETRY_SUCCESS）
     */
    private void updateRetryResultRemark(Long deadLetterId, String remark) {
        try {
            NotificationDeadLetter update = new NotificationDeadLetter();
            update.setResolveRemark(remark);
            LambdaUpdateWrapper<NotificationDeadLetter> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(NotificationDeadLetter::getDeadLetterId, deadLetterId)
                   .eq(NotificationDeadLetter::getResolved, RESOLVED_YES);
            this.update(update, wrapper);
        } catch (Exception e) {
            logger.warn("更新重试结果备注失败(不影响主流程): deadLetterId={}, error={}",
                    deadLetterId, e.getMessage());
        }
    }

    /**
     * 解析用户 ID 对应的用户名。
     *
     * <p>用户 ID 为空或查询失败时返回 null，不影响主流程。</p>
     *
     * @param userId 用户 ID
     * @return 用户名，或 null
     */
    private String resolveUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            User user = userService.getById(userId);
            return user != null ? user.getUsername() : null;
        } catch (Exception e) {
            logger.warn("查询用户名失败, 忽略: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
}
