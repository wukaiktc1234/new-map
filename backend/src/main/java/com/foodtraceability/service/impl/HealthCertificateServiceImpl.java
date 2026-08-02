package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.HealthCertificate;
import com.foodtraceability.event.HealthCertificateApprovedEvent;
import com.foodtraceability.event.HealthCertificateEvent;
import com.foodtraceability.mapper.HealthCertificateMapper;
import com.foodtraceability.service.HealthCertificateService;
import java.io.Serializable;
import com.foodtraceability.service.HrSystemService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class HealthCertificateServiceImpl extends ServiceImpl<HealthCertificateMapper, HealthCertificate> implements HealthCertificateService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HealthCertificateServiceImpl.class);

    public HealthCertificateServiceImpl(HrSystemService hrSystemService, ApplicationEventPublisher eventPublisher) {
        this.hrSystemService = hrSystemService;
        this.eventPublisher = eventPublisher;
    }

    private final HrSystemService hrSystemService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public HealthCertificate getByEmployeeId(String employeeId) {
        return this.baseMapper.selectOne(new QueryWrapper<HealthCertificate>().eq("employee_id", employeeId));
    }

    @Override
    public List<HealthCertificate> getByStore(String store) {
        return this.baseMapper.selectList(new QueryWrapper<HealthCertificate>().eq("store", store));
    }

    @Override
    public boolean updateStatus(String id, String status) {
        HealthCertificate healthCertificate = new HealthCertificate();
        healthCertificate.setId(id);
        healthCertificate.setStatus(status);
        return this.updateById(healthCertificate);
    }

    @Override
    public boolean batchUpdateStatus(List<String> ids, String status) {
        HealthCertificate healthCertificate = new HealthCertificate();
        healthCertificate.setStatus(status);
        return this.update(healthCertificate, new QueryWrapper<HealthCertificate>().in("id", ids));
    }

    @Override
    public boolean updateExpenseStatus(String id, String expenseStatus) {
        HealthCertificate healthCertificate = new HealthCertificate();
        healthCertificate.setId(id);
        healthCertificate.setExpenseStatus(expenseStatus);
        return this.updateById(healthCertificate);
    }

    @Override
    public void calculateAndUpdateStatus() {
        // 获取所有健康证
        List<HealthCertificate> healthCertificates = this.list();
        LocalDate today = LocalDate.now();
        for (HealthCertificate healthCertificate : healthCertificates) {
            // 计算剩余天数
            LocalDate expiryDate = healthCertificate.getExpiryDate();
            long daysBetween = ChronoUnit.DAYS.between(today, expiryDate);
            int expiryDays = (int) daysBetween;
            // 计算状态
            String status;
            if (expiryDays < 0) {
                status = "expired"; // 已过期
            } else if (expiryDays <= 30) {
                status = "expiring"; // 即将过期（30天内）
            } else {
                status = "valid"; // 有效
            }
            // 更新剩余天数和状态
            healthCertificate.setExpiryDays(expiryDays);
            healthCertificate.setStatus(status);
            healthCertificate.setOperationTime(today);
            this.updateById(healthCertificate);
        }
    }

    @Override
    public boolean submitForApproval(String id, String submittedBy) {
        HealthCertificate healthCertificate = this.getById(id);
        if (healthCertificate == null) {
            return false;
        }
        // 只有草稿状态可以提交审核
        if (!"draft".equals(healthCertificate.getApprovalStatus())) {
            return false;
        }
        healthCertificate.setApprovalStatus("pending");
        healthCertificate.setSubmittedBy(submittedBy);
        healthCertificate.setSubmissionDate(LocalDate.now());
        healthCertificate.setOperationTime(LocalDate.now());
        boolean result = this.updateById(healthCertificate);
        // 同步至HR系统
        if (result) {
            hrSystemService.syncHealthCertificateToHrSystem(healthCertificate);
        }
        return result;
    }

    @Override
    public boolean approveHealthCertificate(String id, String approvalStatus, String approvedBy, String rejectReason) {
        HealthCertificate healthCertificate = this.getById(id);
        if (healthCertificate == null) {
            return false;
        }
        // 只有待审核状态可以审核
        if (!"pending".equals(healthCertificate.getApprovalStatus())) {
            return false;
        }
        healthCertificate.setApprovalStatus(approvalStatus);
        healthCertificate.setApprovalBy(approvedBy);
        healthCertificate.setApprovalDate(LocalDate.now());
        healthCertificate.setOperationTime(LocalDate.now());
        if ("rejected".equals(approvalStatus)) {
            healthCertificate.setRejectReason(rejectReason);
        }
        boolean result = this.updateById(healthCertificate);
        // 同步至HR系统
        if (result) {
            hrSystemService.syncHealthCertificateApprovalResult(healthCertificate);
        }
        // 审核通过后发布事件，通知财务系统记录报销费用
        if (result && "approved".equals(approvalStatus)) {
            eventPublisher.publishEvent(new HealthCertificateApprovedEvent(this, healthCertificate));
        }
        return result;
    }

    @Override
    public boolean save(HealthCertificate entity) {
        boolean result = super.save(entity);
        // 发布创建事件
        if (result) {
            HealthCertificateEvent event = new HealthCertificateEvent(entity, HealthCertificateEvent.EventType.CREATE, entity.getOperator() != null ? entity.getOperator() : "system", "健康证创建");
            eventPublisher.publishEvent(event);
        }
        return result;
    }

    @Override
    public boolean updateById(HealthCertificate entity) {
        boolean result = super.updateById(entity);
        // 发布更新事件
        if (result) {
            HealthCertificateEvent event = new HealthCertificateEvent(entity, HealthCertificateEvent.EventType.UPDATE, entity.getOperator() != null ? entity.getOperator() : "system", "健康证更新");
            eventPublisher.publishEvent(event);
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        HealthCertificate entity = this.getById(id);
        boolean result = super.removeById(id);
        // 发布删除事件
        if (result && entity != null) {
            HealthCertificateEvent event = new HealthCertificateEvent(entity, HealthCertificateEvent.EventType.DELETE, "system", "健康证删除");
            eventPublisher.publishEvent(event);
        }
        return result;
    }

    @Override
    public boolean canApplyForExpense(String id) {
        HealthCertificate healthCertificate = this.getById(id);
        if (healthCertificate == null) {
            return false;
        }
        // 只有已通过审核的健康证才能申请报销
        return "approved".equals(healthCertificate.getApprovalStatus());
    }

    @Override
    public boolean sendExpiryReminders(int days) {
        // 获取今天的日期
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = today.plusDays(days);
        // 构建查询条件
        QueryWrapper<HealthCertificate> wrapper = new QueryWrapper<>();
        if (days == 0) {
            // 已过期的健康证：expiryDate <= today
            wrapper.le("expiry_date", today);
        } else {
            // 即将过期的健康证：today < expiryDate <= today + days
            wrapper.gt("expiry_date", today).le("expiry_date", expiryDate);
        }
        // 只查询有效状态的健康证
        wrapper.eq("status", "valid").or().eq("status", "expiring");
        List<HealthCertificate> healthCertificates = this.list(wrapper);
        log.info("发送健康证到期提醒：");
        log.info("提前天数：{}", days);
        log.info("提醒数量：{}", healthCertificates.size());
        return true;
    }
}
