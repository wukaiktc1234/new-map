package com.foodtraceability.listener;

import com.foodtraceability.common.LogUtil;
import com.foodtraceability.event.*;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class HREventListener {
    private static final Logger log = LoggerFactory.getLogger(HREventListener.class);


    public HREventListener(PositionService positionService, com.foodtraceability.service.DepartmentService departmentService) {
        this.positionService = positionService;
        this.departmentService = departmentService;
    }

    private final PositionService positionService;

    private final com.foodtraceability.service.DepartmentService departmentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {
        try {
            log.info("处理员工入职事件: 员工ID={}, 部门ID={}, 职位ID={}", 
                event.getEmployeeId(), event.getDepartmentId(), event.getPositionId());

            if (event.getPositionId() != null && !event.getPositionId().isEmpty()) {
                try {
                    Long positionId = Long.parseLong(event.getPositionId());
                    positionService.incrementEmployeeCount(positionId);
                    log.info("职位员工数量已更新: 职位ID={}", positionId);
                } catch (NumberFormatException e) {
                    log.warn("职位ID格式错误: {}", event.getPositionId());
                }
            }

            if (event.getDepartmentId() != null && !event.getDepartmentId().isEmpty()) {
                try {
                    Long departmentId = Long.parseLong(event.getDepartmentId());
                    departmentService.incrementEmployeeCount(departmentId);
                    log.info("部门员工数量已更新: 部门ID={}", departmentId);
                } catch (NumberFormatException e) {
                    log.warn("部门ID格式错误: {}", event.getDepartmentId());
                }
            }
        } catch (Exception e) {
            log.error("处理员工入职事件失败", e);
            LogUtil.logApiError("EVENT", "EmployeeCreatedEvent", "处理员工入职事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "employeeId=" + event.getEmployeeId());
            throw new RuntimeException("处理员工入职事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleEmployeeTransferred(EmployeeTransferredEvent event) {
        try {
            log.info("处理员工调岗事件: 员工ID={}, 原部门={}, 新部门={}, 原职位={}, 新职位={}", 
                event.getEmployeeId(), event.getOldDepartmentId(), event.getNewDepartmentId(),
                event.getOldPositionId(), event.getNewPositionId());

            if (event.getOldPositionId() != null && !event.getOldPositionId().isEmpty()) {
                try {
                    Long oldPositionId = Long.parseLong(event.getOldPositionId());
                    positionService.decrementEmployeeCount(oldPositionId);
                    log.info("原职位员工数量已更新: 职位ID={}", oldPositionId);
                } catch (NumberFormatException e) {
                    log.warn("原职位ID格式错误: {}", event.getOldPositionId());
                }
            }

            if (event.getNewPositionId() != null && !event.getNewPositionId().isEmpty()) {
                try {
                    Long newPositionId = Long.parseLong(event.getNewPositionId());
                    positionService.incrementEmployeeCount(newPositionId);
                    log.info("新职位员工数量已更新: 职位ID={}", newPositionId);
                } catch (NumberFormatException e) {
                    log.warn("新职位ID格式错误: {}", event.getNewPositionId());
                }
            }

            if (event.getOldDepartmentId() != null && !event.getOldDepartmentId().isEmpty()) {
                try {
                    Long oldDepartmentId = Long.parseLong(event.getOldDepartmentId());
                    departmentService.decrementEmployeeCount(oldDepartmentId);
                    log.info("原部门员工数量已更新: 部门ID={}", oldDepartmentId);
                } catch (NumberFormatException e) {
                    log.warn("原部门ID格式错误: {}", event.getOldDepartmentId());
                }
            }

            if (event.getNewDepartmentId() != null && !event.getNewDepartmentId().isEmpty()) {
                try {
                    Long newDepartmentId = Long.parseLong(event.getNewDepartmentId());
                    departmentService.incrementEmployeeCount(newDepartmentId);
                    log.info("新部门员工数量已更新: 部门ID={}", newDepartmentId);
                } catch (NumberFormatException e) {
                    log.warn("新部门ID格式错误: {}", event.getNewDepartmentId());
                }
            }
        } catch (Exception e) {
            log.error("处理员工调岗事件失败", e);
            LogUtil.logApiError("EVENT", "EmployeeTransferredEvent", "处理员工调岗事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "employeeId=" + event.getEmployeeId());
            throw new RuntimeException("处理员工调岗事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleEmployeeResigned(EmployeeResignedEvent event) {
        try {
            log.info("处理员工离职事件: 员工ID={}, 部门ID={}, 职位ID={}", 
                event.getEmployeeId(), event.getDepartmentId(), event.getPositionId());

            if (event.getPositionId() != null && !event.getPositionId().isEmpty()) {
                try {
                    Long positionId = Long.parseLong(event.getPositionId());
                    positionService.decrementEmployeeCount(positionId);
                    log.info("职位员工数量已更新: 职位ID={}", positionId);
                } catch (NumberFormatException e) {
                    log.warn("职位ID格式错误: {}", event.getPositionId());
                }
            }

            if (event.getDepartmentId() != null && !event.getDepartmentId().isEmpty()) {
                try {
                    Long departmentId = Long.parseLong(event.getDepartmentId());
                    departmentService.decrementEmployeeCount(departmentId);
                    log.info("部门员工数量已更新: 部门ID={}", departmentId);
                } catch (NumberFormatException e) {
                    log.warn("部门ID格式错误: {}", event.getDepartmentId());
                }
            }
        } catch (Exception e) {
            log.error("处理员工离职事件失败", e);
            LogUtil.logApiError("EVENT", "EmployeeResignedEvent", "处理员工离职事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "employeeId=" + event.getEmployeeId());
            throw new RuntimeException("处理员工离职事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleEmployeeDeleted(EmployeeDeletedEvent event) {
        try {
            log.info("处理员工删除事件: 员工ID={}, 部门ID={}, 职位ID={}", 
                event.getEmployeeId(), event.getDepartmentId(), event.getPositionId());

            if (event.getPositionId() != null && !event.getPositionId().isEmpty()) {
                try {
                    Long positionId = Long.parseLong(event.getPositionId());
                    positionService.decrementEmployeeCount(positionId);
                    log.info("职位员工数量已更新: 职位ID={}", positionId);
                } catch (NumberFormatException e) {
                    log.warn("职位ID格式错误: {}", event.getPositionId());
                }
            }

            if (event.getDepartmentId() != null && !event.getDepartmentId().isEmpty()) {
                try {
                    Long departmentId = Long.parseLong(event.getDepartmentId());
                    departmentService.decrementEmployeeCount(departmentId);
                    log.info("部门员工数量已更新: 部门ID={}", departmentId);
                } catch (NumberFormatException e) {
                    log.warn("部门ID格式错误: {}", event.getDepartmentId());
                }
            }
        } catch (Exception e) {
            log.error("处理员工删除事件失败", e);
            LogUtil.logApiError("EVENT", "EmployeeDeletedEvent", "处理员工删除事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "employeeId=" + event.getEmployeeId());
            throw new RuntimeException("处理员工删除事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handlePositionCreated(PositionCreatedEvent event) {
        try {
            log.info("处理职位创建事件: 职位ID={}, 职位名称={}, 部门ID={}", 
                event.getPositionId(), event.getPositionName(), event.getDepartmentId());
        } catch (Exception e) {
            log.error("处理职位创建事件失败", e);
            LogUtil.logApiError("EVENT", "PositionCreatedEvent", "处理职位创建事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "positionId=" + event.getPositionId());
            throw new RuntimeException("处理职位创建事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handlePositionDeleted(PositionDeletedEvent event) {
        try {
            log.info("处理职位删除事件: 职位ID={}, 部门ID={}", 
                event.getPositionId(), event.getDepartmentId());
        } catch (Exception e) {
            log.error("处理职位删除事件失败", e);
            LogUtil.logApiError("EVENT", "PositionDeletedEvent", "处理职位删除事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "positionId=" + event.getPositionId());
            throw new RuntimeException("处理职位删除事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleDepartmentCreated(DepartmentCreatedEvent event) {
        try {
            log.info("处理部门创建事件: 部门ID={}, 部门名称={}, 父部门ID={}", 
                event.getDepartmentId(), event.getDepartmentName(), event.getParentId());
        } catch (Exception e) {
            log.error("处理部门创建事件失败", e);
            LogUtil.logApiError("EVENT", "DepartmentCreatedEvent", "处理部门创建事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "departmentId=" + event.getDepartmentId());
            throw new RuntimeException("处理部门创建事件失败: " + e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleDepartmentDeleted(DepartmentDeletedEvent event) {
        try {
            log.info("处理部门删除事件: 部门ID={}", event.getDepartmentId());
        } catch (Exception e) {
            log.error("处理部门删除事件失败", e);
            LogUtil.logApiError("EVENT", "DepartmentDeletedEvent", "处理部门删除事件", 
                e.getMessage(), LogUtil.generateRequestId(), 
                "departmentId=" + event.getDepartmentId());
            throw new RuntimeException("处理部门删除事件失败: " + e.getMessage(), e);
        }
    }
}
