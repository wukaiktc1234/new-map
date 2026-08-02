package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.UserPermissionOverride;

import java.util.List;

/**
 * 用户权限覆盖 Service 接口
 */
public interface UserPermissionOverrideService extends IService<UserPermissionOverride> {

    /**
     * 分页查询用户权限覆盖
     *
     * @param page           分页参数
     * @param userId         用户ID（可空）
     * @param userName       用户姓名（可空，模糊匹配）
     * @param permissionCode 权限码（可空，模糊匹配）
     * @param domainCode     业务域（可空）
     * @param overrideType   覆盖类型（可空）
     * @param status         状态（可空）
     * @return 分页结果
     */
    IPage<UserPermissionOverride> getPage(Page<UserPermissionOverride> page, String userId, String userName,
                                          String permissionCode, String domainCode, String overrideType, String status);

    /**
     * 根据ID查询
     */
    UserPermissionOverride getById(Long id);

    /**
     * 根据用户ID查询所有覆盖
     */
    List<UserPermissionOverride> getByUserId(String userId);

    /**
     * 创建覆盖（默认状态 PENDING）
     */
    UserPermissionOverride create(UserPermissionOverride override);

    /**
     * 更新覆盖
     */
    UserPermissionOverride update(Long id, UserPermissionOverride override);

    /**
     * 逻辑删除
     */
    boolean delete(Long id);

    /**
     * 审批覆盖
     *
     * @param id            覆盖ID
     * @param approverId    审批人ID
     * @param approverName  审批人姓名
     * @param approved      true-通过（ACTIVE），false-拒绝（REJECTED）
     */
    UserPermissionOverride approve(Long id, String approverId, String approverName, boolean approved);

    /**
     * 撤销覆盖（状态置为 REVOKED）
     */
    UserPermissionOverride revoke(Long id);

    /**
     * 刷新过期状态：将所有已过期但状态仍为 ACTIVE 的记录改为 EXPIRED
     *
     * @return 本次刷新的条数
     */
    boolean refreshExpired();
}
