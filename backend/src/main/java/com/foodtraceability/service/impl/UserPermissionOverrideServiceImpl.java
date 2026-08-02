package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.UserPermissionOverride;
import com.foodtraceability.mapper.UserPermissionOverrideMapper;
import com.foodtraceability.service.UserPermissionOverrideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户权限覆盖 Service 实现
 */
@Service
public class UserPermissionOverrideServiceImpl
        extends ServiceImpl<UserPermissionOverrideMapper, UserPermissionOverride>
        implements UserPermissionOverrideService {

    private static final Logger log = LoggerFactory.getLogger(UserPermissionOverrideServiceImpl.class);

    /** 覆盖状态常量 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Override
    public IPage<UserPermissionOverride> getPage(Page<UserPermissionOverride> page, String userId, String userName,
                                                  String permissionCode, String domainCode, String overrideType, String status) {
        LambdaQueryWrapper<UserPermissionOverride> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userId)) {
            wrapper.eq(UserPermissionOverride::getUserId, userId);
        }
        if (StringUtils.hasText(userName)) {
            wrapper.like(UserPermissionOverride::getUserName, userName);
        }
        if (StringUtils.hasText(permissionCode)) {
            wrapper.like(UserPermissionOverride::getPermissionCode, permissionCode);
        }
        if (StringUtils.hasText(domainCode)) {
            wrapper.eq(UserPermissionOverride::getDomainCode, domainCode);
        }
        if (StringUtils.hasText(overrideType)) {
            wrapper.eq(UserPermissionOverride::getOverrideType, overrideType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserPermissionOverride::getStatus, status);
        }
        wrapper.orderByDesc(UserPermissionOverride::getCreatedAt);
        return this.page(page, wrapper);
    }

    @Override
    public UserPermissionOverride getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<UserPermissionOverride> getByUserId(String userId) {
        LambdaQueryWrapper<UserPermissionOverride> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPermissionOverride::getUserId, userId)
               .orderByDesc(UserPermissionOverride::getCreatedAt);
        return this.list(wrapper);
    }

    @Override
    public UserPermissionOverride create(UserPermissionOverride override) {
        // 新建覆盖默认进入待审批状态
        override.setStatus(STATUS_PENDING);
        LocalDateTime now = LocalDateTime.now();
        override.setCreatedAt(now);
        override.setUpdatedAt(now);
        if (override.getDeleted() == null) {
            override.setDeleted(0);
        }
        this.save(override);
        log.info("创建用户权限覆盖: userId={}, permissionCode={}, type={}",
                 override.getUserId(), override.getPermissionCode(), override.getOverrideType());
        return override;
    }

    @Override
    public UserPermissionOverride update(Long id, UserPermissionOverride override) {
        UserPermissionOverride existing = super.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("覆盖记录不存在: id=" + id);
        }
        override.setId(id);
        // 不允许通过 update 修改状态、审批信息、创建信息
        override.setStatus(existing.getStatus());
        override.setApproveBy(existing.getApproveBy());
        override.setApproveName(existing.getApproveName());
        override.setApproveTime(existing.getApproveTime());
        override.setCreatedBy(existing.getCreatedBy());
        override.setCreatedByName(existing.getCreatedByName());
        override.setCreatedAt(existing.getCreatedAt());
        override.setUpdatedAt(LocalDateTime.now());
        this.updateById(override);
        return super.getById(id);
    }

    @Override
    public boolean delete(Long id) {
        return this.removeById(id);
    }

    @Override
    public UserPermissionOverride approve(Long id, String approverId, String approverName, boolean approved) {
        UserPermissionOverride existing = super.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("覆盖记录不存在: id=" + id);
        }
        // 仅 PENDING 状态可审批
        if (!STATUS_PENDING.equals(existing.getStatus())) {
            throw new IllegalStateException("仅待审批状态可审批，当前状态: " + existing.getStatus());
        }
        existing.setStatus(approved ? STATUS_ACTIVE : STATUS_REJECTED);
        existing.setApproveBy(approverId);
        existing.setApproveName(approverName);
        existing.setApproveTime(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
        log.info("审批用户权限覆盖: id={}, approved={}, approver={}", id, approved, approverName);
        return existing;
    }

    @Override
    public UserPermissionOverride revoke(Long id) {
        UserPermissionOverride existing = super.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("覆盖记录不存在: id=" + id);
        }
        // 仅 ACTIVE 状态可撤销
        if (!STATUS_ACTIVE.equals(existing.getStatus())) {
            throw new IllegalStateException("仅生效中状态可撤销，当前状态: " + existing.getStatus());
        }
        existing.setStatus(STATUS_REVOKED);
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
        log.info("撤销用户权限覆盖: id={}", id);
        return existing;
    }

    @Override
    public boolean refreshExpired() {
        // 将所有过期但状态仍为 ACTIVE 的记录改为 EXPIRED
        LambdaUpdateWrapper<UserPermissionOverride> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserPermissionOverride::getStatus, STATUS_ACTIVE)
                     .isNotNull(UserPermissionOverride::getExpireTime)
                     .lt(UserPermissionOverride::getExpireTime, LocalDateTime.now())
                     .set(UserPermissionOverride::getStatus, STATUS_EXPIRED)
                     .set(UserPermissionOverride::getUpdatedAt, LocalDateTime.now());
        boolean ok = this.update(updateWrapper);
        log.info("刷新过期用户权限覆盖完成: success={}", ok);
        return ok;
    }
}
