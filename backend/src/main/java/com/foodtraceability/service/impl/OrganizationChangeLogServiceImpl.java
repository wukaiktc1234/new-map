package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.OrganizationChangeLog;
import com.foodtraceability.mapper.OrganizationChangeLogMapper;
import com.foodtraceability.service.OrganizationChangeLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织架构变更日志服务实现类
 *
 * 审计日志写入与主业务事务隔离（REQUIRES_NEW），
 * 即使审计日志失败也不影响主业务事务的提交。
 */
@Service
public class OrganizationChangeLogServiceImpl extends ServiceImpl<OrganizationChangeLogMapper, OrganizationChangeLog> implements OrganizationChangeLogService {

    @Override
    public List<OrganizationChangeLog> getLogsByChangeType(Integer changeType) {
        QueryWrapper<OrganizationChangeLog> wrapper = new QueryWrapper<>();
        wrapper.eq("change_type", changeType);
        wrapper.orderByDesc("operate_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<OrganizationChangeLog> getLogsByObjectId(Long objectId) {
        QueryWrapper<OrganizationChangeLog> wrapper = new QueryWrapper<>();
        wrapper.eq("object_id", objectId);
        wrapper.orderByDesc("operate_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Long recordDepartmentChange(Long departmentId, String departmentName, String beforeChange, String afterChange, String changeReason, String operator) {
        return recordChangeLog(1, departmentId, departmentName, beforeChange, afterChange, changeReason, operator);
    }

    @Override
    public Long recordPositionChange(Long positionId, String positionName, String beforeChange, String afterChange, String changeReason, String operator) {
        return recordChangeLog(2, positionId, positionName, beforeChange, afterChange, changeReason, operator);
    }

    @Override
    public Long recordEmployeeChange(Long employeeId, String employeeName, String beforeChange, String afterChange, String changeReason, String operator) {
        return recordChangeLog(3, employeeId, employeeName, beforeChange, afterChange, changeReason, operator);
    }

    @Override
    public List<OrganizationChangeLog> getRecentLogs(Integer limit) {
        QueryWrapper<OrganizationChangeLog> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("operate_time");
        wrapper.last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Object getChangeStatistics() {
        // 获取变更类型统计
        QueryWrapper<OrganizationChangeLog> departmentWrapper = new QueryWrapper<>();
        departmentWrapper.eq("change_type", 1);
        int departmentChangeCount = Math.toIntExact(baseMapper.selectCount(departmentWrapper));

        QueryWrapper<OrganizationChangeLog> positionWrapper = new QueryWrapper<>();
        positionWrapper.eq("change_type", 2);
        int positionChangeCount = Math.toIntExact(baseMapper.selectCount(positionWrapper));

        QueryWrapper<OrganizationChangeLog> employeeWrapper = new QueryWrapper<>();
        employeeWrapper.eq("change_type", 3);
        int employeeChangeCount = Math.toIntExact(baseMapper.selectCount(employeeWrapper));

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalChanges", departmentChangeCount + positionChangeCount + employeeChangeCount);
        statistics.put("departmentChanges", departmentChangeCount);
        statistics.put("positionChanges", positionChangeCount);
        statistics.put("employeeChanges", employeeChangeCount);

        // 获取最近的变更记录
        List<OrganizationChangeLog> recentLogs = getRecentLogs(5);
        statistics.put("recentChanges", recentLogs);

        return statistics;
    }

    /**
     * 记录变更日志（核心方法，私有，仅在本类内部调用）
     *
     * 修复要点：
     * 1. 显式设置 operateTime，因为 PostgreSQL 的 NOT NULL 约束不接受 null，
     *    而 MyMetaObjectHandler 仅填充 createTime/updateTime，不处理 operateTime。
     * 2. REQUIRES_NEW 传播在公开方法上声明（logDepartmentCreate 等），
     *    因为 Spring AOP 同类内方法调用不经过代理，protected 方法上的注解不生效。
     */
    private Long recordChangeLog(Integer changeType, Long objectId, String objectName, String beforeChange, String afterChange, String changeReason, String operator) {
        OrganizationChangeLog log = new OrganizationChangeLog();
        log.setChangeType(changeType);
        log.setObjectId(objectId);
        log.setObjectName(objectName);
        log.setBeforeChange(beforeChange);
        log.setAfterChange(afterChange);
        log.setChangeReason(changeReason);
        log.setOperator(operator);
        log.setStatus(1); // 默认为成功
        // 显式设置操作时间（PostgreSQL NOT NULL 约束）
        log.setOperateTime(LocalDateTime.now());

        baseMapper.insert(log);
        return log.getId();
    }

    /**
     * 记录职位创建日志
     * REQUIRES_NEW：审计日志独立事务，失败不影响主业务事务提交
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void logPositionCreate(String positionId, String positionName, String departmentId, String operator, String ipAddress) {
        if (positionId != null) {
            try {
                Long posId = Long.parseLong(positionId);
                Long deptId = departmentId != null ? Long.parseLong(departmentId) : null;
                recordChangeLog(2, posId, positionName, "", "职位创建", "职位创建", operator);
            } catch (NumberFormatException e) {
                // 处理数字格式异常
                recordChangeLog(2, null, positionName, "", "职位创建", "职位创建", operator);
            }
        } else {
            // 当positionId为null时的处理
            recordChangeLog(2, null, positionName, "", "职位创建", "职位创建", operator);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void logPositionUpdate(String positionId, String beforeValue, String afterValue, String operator, String reason, String ipAddress) {
        if (positionId != null) {
            try {
                Long posId = Long.parseLong(positionId);
                recordChangeLog(2, posId, "", beforeValue, afterValue, reason, operator);
            } catch (NumberFormatException e) {
                // 处理数字格式异常
                recordChangeLog(2, null, "", beforeValue, afterValue, reason, operator);
            }
        } else {
            // 当positionId为null时的处理
            recordChangeLog(2, null, "", beforeValue, afterValue, reason, operator);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void logPositionDelete(String positionId, String beforeValue, String operator, String ipAddress) {
        if (positionId != null) {
            try {
                Long posId = Long.parseLong(positionId);
                recordChangeLog(2, posId, "", beforeValue, "职位删除", "职位删除", operator);
            } catch (NumberFormatException e) {
                // 处理数字格式异常
                recordChangeLog(2, null, "", beforeValue, "职位删除", "职位删除", operator);
            }
        } else {
            // 当positionId为null时的处理
            recordChangeLog(2, null, "", beforeValue, "职位删除", "职位删除", operator);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void logDepartmentCreate(String departmentId, String departmentName, String operator, String ipAddress) {
        if (departmentId != null) {
            try {
                Long deptId = Long.parseLong(departmentId);
                recordChangeLog(1, deptId, departmentName, "", "部门创建", "部门创建", operator);
            } catch (NumberFormatException e) {
                // 处理数字格式异常
                recordChangeLog(1, null, departmentName, "", "部门创建", "部门创建", operator);
            }
        } else {
            // 当departmentId为null时的处理
            recordChangeLog(1, null, departmentName, "", "部门创建", "部门创建", operator);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void logDepartmentUpdate(String departmentId, String beforeValue, String afterValue, String operator, String reason, String ipAddress) {
        if (departmentId != null) {
            try {
                Long deptId = Long.parseLong(departmentId);
                recordChangeLog(1, deptId, "", beforeValue, afterValue, reason, operator);
            } catch (NumberFormatException e) {
                // 处理数字格式异常
                recordChangeLog(1, null, "", beforeValue, afterValue, reason, operator);
            }
        } else {
            // 当departmentId为null时的处理
            recordChangeLog(1, null, "", beforeValue, afterValue, reason, operator);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void logDepartmentDelete(String departmentId, String beforeValue, String operator, String ipAddress) {
        if (departmentId != null) {
            try {
                Long deptId = Long.parseLong(departmentId);
                recordChangeLog(1, deptId, "", beforeValue, "部门删除", "部门删除", operator);
            } catch (NumberFormatException e) {
                // 处理数字格式异常
                recordChangeLog(1, null, "", beforeValue, "部门删除", "部门删除", operator);
            }
        } else {
            // 当departmentId为null时的处理
            recordChangeLog(1, null, "", beforeValue, "部门删除", "部门删除", operator);
        }
    }
}
