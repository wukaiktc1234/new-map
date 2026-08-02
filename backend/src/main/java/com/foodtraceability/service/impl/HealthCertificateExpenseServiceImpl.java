package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.HealthCertificateExpense;
import com.foodtraceability.mapper.HealthCertificateExpenseMapper;
import com.foodtraceability.service.FinanceSystemService;
import com.foodtraceability.service.HealthCertificateExpenseService;
import com.foodtraceability.service.HealthCertificateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康证报销管理Service实现类
 * 用于健康证报销相关的业务逻辑处理
 */
@Service
public class HealthCertificateExpenseServiceImpl extends ServiceImpl<HealthCertificateExpenseMapper, HealthCertificateExpense> implements HealthCertificateExpenseService {
    

    public HealthCertificateExpenseServiceImpl(HealthCertificateService healthCertificateService, ObjectMapper objectMapper, FinanceSystemService financeSystemService) {
        this.healthCertificateService = healthCertificateService;
        this.objectMapper = objectMapper;
        this.financeSystemService = financeSystemService;
    }

    private final HealthCertificateService healthCertificateService;
    
    private final ObjectMapper objectMapper;
    
    private final FinanceSystemService financeSystemService;
    
    @Override
    public List<HealthCertificateExpense> getByHealthCertificateId(String healthCertificateId) {
        return this.baseMapper.selectList(new QueryWrapper<HealthCertificateExpense>().eq("health_certificate_id", healthCertificateId));
    }
    
    @Override
    public List<HealthCertificateExpense> getByEmployeeId(String employeeId) {
        return this.baseMapper.selectList(new QueryWrapper<HealthCertificateExpense>().eq("employee_id", employeeId));
    }
    
    @Override
    public List<HealthCertificateExpense> getByStore(String store) {
        return this.baseMapper.selectList(new QueryWrapper<HealthCertificateExpense>().eq("store", store));
    }
    
    @Override
    public boolean updateStatus(Long id, String status) {
        HealthCertificateExpense expense = new HealthCertificateExpense();
        expense.setId(id);
        expense.setStatus(status);
        return this.updateById(expense);
    }
    
    @Override
    public boolean submitExpense(HealthCertificateExpense expense) {
        // 设置默认值
        expense.setStatus("pending_hr_review");
        expense.setApplyDate(LocalDate.now());
        expense.setCreateTime(LocalDate.now());
        expense.setUpdateTime(LocalDate.now());
        
        // 初始化审批历史
        try {
            ArrayNode historyArray = objectMapper.createArrayNode();
            ObjectNode submitNode = objectMapper.createObjectNode();
            submitNode.put("id", "SUB" + System.currentTimeMillis());
            submitNode.put("approver", expense.getCreator());
            submitNode.put("approverName", expense.getCreator());
            submitNode.put("approvalDate", LocalDate.now().toString());
            submitNode.put("approvalStatus", "submitted");
            submitNode.put("comment", "报销申请已提交");
            historyArray.add(submitNode);
            expense.setApprovalHistory(historyArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        boolean result = this.save(expense);
        
        // 同步至财务系统
        if (result) {
            financeSystemService.syncHealthCertificateExpenseToFinance(expense);
        }
        
        return result;
    }
    
    @Override
    public boolean hrReviewExpense(Long id, String status, String approver, String comment) {
        boolean result = processApproval(id, status, approver, comment, "pending_hr_review", "pending_finance_review");

        // 同步至财务系统
        if (result) {
            HealthCertificateExpense expense = this.getById(id);
            if (expense != null) {
                financeSystemService.syncHealthCertificateExpenseToFinance(expense);
            }
        }

        return result;
    }

    @Override
    public boolean financeReviewExpense(Long id, String status, String approver, String comment) {
        boolean result = processApproval(id, status, approver, comment, "pending_finance_review", "approved");

        // 同步至财务系统
        if (result) {
            HealthCertificateExpense expense = this.getById(id);
            if (expense != null) {
                financeSystemService.syncHealthCertificateExpenseToFinance(expense);
            }
        }

        return result;
    }

    @Override
    public boolean reimburseExpense(Long id, String reimburseDate) {
        // 获取报销记录
        HealthCertificateExpense expense = this.getById(id);
        if (expense == null) {
            return false;
        }
        
        // 更新报销状态为已报销
        expense.setStatus("reimbursed");
        expense.setReimburseDate(LocalDate.parse(reimburseDate));
        expense.setUpdateTime(LocalDate.now());
        
        // 更新审批历史
        updateApprovalHistory(expense, "reimbursed", "财务", "费用已支付");
        
        // 更新健康证的报销状态
        healthCertificateService.updateExpenseStatus(expense.getHealthCertificateId(), "reimbursed");
        
        boolean result = this.updateById(expense);
        
        // 同步至财务系统
        if (result) {
            financeSystemService.syncHealthCertificateExpenseToFinance(expense);
        }
        
        return result;
    }
    
    @Override
    public boolean batchRejectExpense(List<Long> ids, String reason, String approver) {
        // 批量拒绝报销申请
        for (Long id : ids) {
            this.processApproval(id, "rejected", approver, reason, null, null);
        }
        return true;
    }
    
    @Override
    public boolean uploadInvoiceAttachment(Long id, String attachmentPath) {
        HealthCertificateExpense expense = this.getById(id);
        if (expense == null) {
            return false;
        }
        
        // 获取现有附件路径
        String existingAttachments = expense.getInvoiceAttachments();
        String newAttachments;
        
        if (existingAttachments == null || existingAttachments.isEmpty()) {
            newAttachments = attachmentPath;
        } else {
            newAttachments = existingAttachments + "," + attachmentPath;
        }
        
        expense.setInvoiceAttachments(newAttachments);
        expense.setUpdateTime(LocalDate.now());
        
        return this.updateById(expense);
    }
    
    @Override
    public List<HealthCertificateExpense> getPendingHrReviewExpenses() {
        return this.baseMapper.selectList(new QueryWrapper<HealthCertificateExpense>().eq("status", "pending_hr_review"));
    }
    
    @Override
    public List<HealthCertificateExpense> getPendingFinanceReviewExpenses() {
        return this.baseMapper.selectList(new QueryWrapper<HealthCertificateExpense>().eq("status", "pending_finance_review"));
    }
    
    @Override
    public List<HealthCertificateExpense> getReimbursedExpenses(String startDate, String endDate) {
        QueryWrapper<HealthCertificateExpense> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "reimbursed");
        
        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("reimburse_date", startDate);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("reimburse_date", endDate);
        }
        
        return this.baseMapper.selectList(queryWrapper);
    }
    
    /**
     * 通用审批处理方法
     * @param id 报销ID
     * @param status 审批状态
     * @param approver 审批人
     * @param comment 审批意见
     * @param expectedCurrentStatus 预期当前状态
     * @param nextStatus 下一状态
     * @return 审批结果
     */
    private boolean processApproval(Long id, String status, String approver, String comment, String expectedCurrentStatus, String nextStatus) {
        // 获取报销记录
        HealthCertificateExpense expense = this.getById(id);
        if (expense == null) {
            return false;
        }
        
        // 验证当前状态是否符合预期
        if (expectedCurrentStatus != null && !expectedCurrentStatus.equals(expense.getStatus())) {
            return false;
        }
        
        // 更新报销状态
        if ("approved".equals(status) && nextStatus != null) {
            expense.setStatus(nextStatus);
        } else {
            expense.setStatus(status);
        }
        
        expense.setApproveDate(LocalDate.now());
        expense.setUpdateTime(LocalDate.now());
        
        // 更新审批历史
        updateApprovalHistory(expense, status, approver, comment);
        
        // 如果审批拒绝，更新拒绝原因
        if ("rejected".equals(status)) {
            expense.setRejectReason(comment);
            healthCertificateService.updateExpenseStatus(expense.getHealthCertificateId(), "rejected");
        }
        
        return this.updateById(expense);
    }
    
    /**
     * 更新审批历史
     * @param expense 报销记录
     * @param status 审批状态
     * @param approver 审批人
     * @param comment 审批意见
     */
    private void updateApprovalHistory(HealthCertificateExpense expense, String status, String approver, String comment) {
        String approvalHistory = expense.getApprovalHistory();
        try {
            ArrayNode historyArray;
            if (approvalHistory == null || approvalHistory.isEmpty()) {
                historyArray = objectMapper.createArrayNode();
            } else {
                historyArray = (ArrayNode) objectMapper.readTree(approvalHistory);
            }
            
            // 添加新的审批记录
            ObjectNode approvalNode = objectMapper.createObjectNode();
            approvalNode.put("id", "APP" + System.currentTimeMillis());
            approvalNode.put("approver", approver);
            approvalNode.put("approverName", approver); // 这里可以根据用户ID获取真实姓名
            approvalNode.put("approvalDate", LocalDate.now().toString());
            approvalNode.put("approvalStatus", status);
            approvalNode.put("comment", comment);
            
            historyArray.add(approvalNode);
            expense.setApprovalHistory(historyArray.toString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
