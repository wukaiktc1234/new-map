package com.foodtraceability.service.impl;

import com.foodtraceability.entity.SalaryRecord;
import com.foodtraceability.entity.SalaryRule;
import com.foodtraceability.entity.User;
import com.foodtraceability.event.SalaryPaidEvent;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.SalaryRecordService;
import com.foodtraceability.service.SalaryRuleService;
import com.foodtraceability.service.SalaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 薪资管理服务实现类
 */
@Service
public class SalaryServiceImpl implements SalaryService {

    private static final Logger log = LoggerFactory.getLogger(SalaryServiceImpl.class);

    private final UserMapper userMapper;
    private final SalaryRecordService salaryRecordService;
    private final SalaryRuleService salaryRuleService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public SalaryServiceImpl(UserMapper userMapper,
                              SalaryRecordService salaryRecordService,
                              SalaryRuleService salaryRuleService,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.userMapper = userMapper;
        this.salaryRecordService = salaryRecordService;
        this.salaryRuleService = salaryRuleService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByStatus(String status) {
        return userMapper.findByStatus(status);
    }

    @Override
    public BigDecimal getRuleAmount(List<?> rules, String ruleType, BigDecimal defaultAmount) {
        if (rules == null || rules.isEmpty()) {
            return defaultAmount;
        }
        for (Object rule : rules) {
            if (rule instanceof SalaryRule salaryRule) {
                if (ruleType.equals(salaryRule.getRuleType()) && "ACTIVE".equals(salaryRule.getStatus())) {
                    return salaryRule.getAmount() != null ? salaryRule.getAmount() : defaultAmount;
                }
            }
        }
        return defaultAmount;
    }

    @Override
    public BigDecimal calculateTax(BigDecimal totalEarnings, BigDecimal insurance) {
        BigDecimal threshold = new BigDecimal(5000);
        BigDecimal taxableIncome = totalEarnings.subtract(insurance).subtract(threshold);
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (taxableIncome.compareTo(new BigDecimal(3000)) <= 0) {
            return taxableIncome.multiply(new BigDecimal("0.03"));
        } else if (taxableIncome.compareTo(new BigDecimal(12000)) <= 0) {
            return taxableIncome.multiply(new BigDecimal("0.1")).subtract(new BigDecimal("210"));
        } else if (taxableIncome.compareTo(new BigDecimal(25000)) <= 0) {
            return taxableIncome.multiply(new BigDecimal("0.2")).subtract(new BigDecimal("1410"));
        } else {
            return taxableIncome.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("2660"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateSalary(String year, String month) {
        // 查询所有已激活的用户
        List<User> activeUsers = findUsersByStatus("active");
        if (activeUsers == null || activeUsers.isEmpty()) {
            return 0;
        }
        // 获取所有活跃的薪资规则
        List<SalaryRule> activeRules = salaryRuleService.list();
        // 拼接薪资月份，月份补零
        String salaryMonth = year + "-" + (month.length() == 1 ? "0" + month : month);
        int generatedCount = 0;
        for (User user : activeUsers) {
            SalaryRecord record = new SalaryRecord();
            record.setEmployeeId(user.getId());
            record.setEmployeeName(user.getFullName());
            record.setDepartment(user.getDepartment());
            record.setPosition(user.getRoleNames() != null ? user.getRoleNames() : "员工");
            record.setSalaryMonth(salaryMonth);
            record.setStatus("待确认");
            // 根据规则计算各项薪资
            BigDecimal basicSalary = getRuleAmount(activeRules, "basicSalary", new BigDecimal(5000));
            BigDecimal performanceBonus = getRuleAmount(activeRules, "performanceBonus", new BigDecimal(1000));
            BigDecimal overtimePay = getRuleAmount(activeRules, "overtimePay", BigDecimal.ZERO);
            BigDecimal allowance = getRuleAmount(activeRules, "allowance", new BigDecimal(500));
            BigDecimal totalEarnings = basicSalary.add(performanceBonus).add(overtimePay).add(allowance);
            BigDecimal insurance = getRuleAmount(activeRules, "insurance", new BigDecimal(800));
            BigDecimal tax = calculateTax(totalEarnings, insurance);
            BigDecimal otherDeductions = BigDecimal.ZERO;
            BigDecimal finalSalary = totalEarnings.subtract(insurance).subtract(tax).subtract(otherDeductions);
            record.setBasicSalary(basicSalary);
            record.setPerformanceBonus(performanceBonus);
            record.setOvertimePay(overtimePay);
            record.setAllowance(allowance);
            record.setTotalEarnings(totalEarnings);
            record.setInsurance(insurance);
            record.setTax(tax);
            record.setOtherDeductions(otherDeductions);
            record.setFinalSalary(finalSalary);
            if (salaryRecordService.save(record)) {
                generatedCount++;
                // 发布薪资发放事件，触发财务凭证生成与人工成本记录
                // 监听器：SalaryPaidEventListener（@TransactionalEventListener AFTER_COMMIT + @Async）
                publishSalaryPaidEvent(record);
            }
        }
        return generatedCount;
    }

    /**
     * 发布薪资发放事件
     * 触发 SalaryPaidEventListener 联动生成薪资凭证和记录人工成本
     * 异常隔离，发布失败不影响主事务
     */
    private void publishSalaryPaidEvent(SalaryRecord record) {
        try {
            SalaryPaidEvent event = new SalaryPaidEvent();
            event.setSalaryRecordId(record.getId());
            event.setEmployeeId(record.getEmployeeId());
            event.setEmployeeName(record.getEmployeeName());
            event.setEmployeeCode(record.getEmployeeNumber());
            event.setDepartmentName(record.getDepartment());
            event.setPositionName(record.getPosition());
            event.setBaseSalary(record.getBasicSalary());
            event.setBonus(record.getPerformanceBonus());
            // 扣款合计 = 社保 + 个税 + 其他扣款
            BigDecimal deduction = BigDecimal.ZERO;
            if (record.getInsurance() != null) {
                deduction = deduction.add(record.getInsurance());
            }
            if (record.getTax() != null) {
                deduction = deduction.add(record.getTax());
            }
            if (record.getOtherDeductions() != null) {
                deduction = deduction.add(record.getOtherDeductions());
            }
            event.setDeduction(deduction);
            event.setActualSalary(record.getFinalSalary());
            event.setPaymentPeriod(record.getSalaryMonth());
            event.setPaymentTime(LocalDateTime.now());
            applicationEventPublisher.publishEvent(event);
            log.info("已发布薪资发放事件: salaryRecordId={}, employeeId={}",
                    record.getId(), record.getEmployeeId());
        } catch (Exception e) {
            log.error("发布薪资发放事件失败: salaryRecordId={}, 错误={}",
                    record.getId(), e.getMessage(), e);
        }
    }
}
