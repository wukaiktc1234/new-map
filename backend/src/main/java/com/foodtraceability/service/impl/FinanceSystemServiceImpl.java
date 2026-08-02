package com.foodtraceability.service.impl;

import com.foodtraceability.entity.HealthCertificateExpense;
import com.foodtraceability.entity.finance.FinanceRecord;
import com.foodtraceability.service.FinanceSystemService;
import com.foodtraceability.service.finance.FinanceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 财务系统服务实现类
 * <p>
 * 用于处理健康证报销数据同步至财务系统。审核通过的健康证报销数据
 * 会创建对应的财务收支流水记录，实现健康证→财务中心的链路连通。
 * </p>
 */
@Service
public class FinanceSystemServiceImpl implements FinanceSystemService {

    private static final Logger log = LoggerFactory.getLogger(FinanceSystemServiceImpl.class);

    /** 收支类型：支出 */
    private static final int RECORD_TYPE_EXPENSE = 2;
    /** 收支类别：其他支出（健康证报销归入此类） */
    private static final int RECORD_CATEGORY_OTHER_EXPENSE = 205;
    /** 对方类型：员工 */
    private static final int COUNTERPARTY_TYPE_EMPLOYEE = 3;
    /** 审批状态：已审批 */
    private static final int APPROVAL_STATUS_APPROVED = 1;

    private final FinanceRecordService financeRecordService;

    public FinanceSystemServiceImpl(FinanceRecordService financeRecordService) {
        this.financeRecordService = financeRecordService;
    }

    @Override
    public boolean syncHealthCertificateExpenseToFinance(HealthCertificateExpense expense) {
        if (expense == null) {
            log.warn("健康证报销数据同步至财务系统失败：报销数据为空");
            return false;
        }
        if (expense.getAmount() == null) {
            log.warn("健康证报销数据同步至财务系统失败：报销金额为空，报销ID: {}", expense.getId());
            return false;
        }
        try {
            // 创建财务收支流水记录
            FinanceRecord record = new FinanceRecord();
            record.setRecordNo(generateRecordNo());
            record.setRecordType(RECORD_TYPE_EXPENSE);
            record.setRecordCategory(RECORD_CATEGORY_OTHER_EXPENSE);
            // 金额由元转换为分（Long）
            long amountInCents = Math.round(expense.getAmount() * 100.0);
            record.setAmount(amountInCents);
            record.setCounterpartyName(expense.getEmployeeName());
            record.setCounterpartyType(COUNTERPARTY_TYPE_EMPLOYEE);
            // 业务发生日期取审批日期，若为空则取当前日期
            LocalDate businessDate = expense.getApproveDate() != null ? expense.getApproveDate() : LocalDate.now();
            record.setBusinessDate(businessDate);
            record.setRecordDate(LocalDate.now());
            record.setApprovalStatus(APPROVAL_STATUS_APPROVED);
            // 备注：记录关联的健康证ID与报销事由，便于追溯
            String remark = String.format("健康证报销 - 健康证ID: %s, 报销事由: %s",
                    expense.getHealthCertificateId(),
                    expense.getReason() != null ? expense.getReason() : "");
            record.setRemark(remark);

            boolean saved = financeRecordService.save(record);
            if (saved) {
                log.info("健康证报销数据同步至财务系统成功，报销ID: {}, 财务记录ID: {}, 金额(分): {}",
                        expense.getId(), record.getRecordId(), amountInCents);
            } else {
                log.error("健康证报销数据同步至财务系统失败：保存财务记录失败，报销ID: {}", expense.getId());
            }
            return saved;
        } catch (Exception e) {
            log.error("健康证报销数据同步至财务系统失败，报销ID: {}, 错误信息: {}",
                    expense.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getExpenseStatusFromFinance(String expenseId) {
        try {
            // 占位实现：外部财务系统对接待实现，当前返回 null 表示占位
            log.warn("外部财务系统对接待实现 - 从财务系统获取报销状态占位返回 null，报销ID: {}",
                    expenseId);
            return null;
        } catch (Exception e) {
            log.error("从财务系统获取健康证报销状态失败，报销ID: {}, 错误信息: {}", expenseId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean updateExpenseStatus(String expenseId, String status) {
        try {
            // 占位实现：外部财务系统对接待实现，当前返回 true 表示占位成功
            log.warn("外部财务系统对接待实现 - 更新报销状态占位成功，报销ID: {}, 状态: {}",
                    expenseId, status);
            return true;
        } catch (Exception e) {
            log.error("更新健康证报销状态失败，报销ID: {}, 状态: {}, 错误信息: {}", expenseId, status, e.getMessage());
            return false;
        }
    }

    /**
     * 生成财务记录编号
     * 格式：HC + yyyyMMdd + 6位时间毫秒后缀
     * @return 财务记录编号
     */
    private String generateRecordNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = String.format("%06d", System.currentTimeMillis() % 1000000);
        return "HC" + datePart + suffix;
    }
}
