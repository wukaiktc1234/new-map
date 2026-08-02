package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.FinanceApprovalVO;
import com.foodtraceability.entity.finance.FinanceApproval;
import java.util.List;

/**
 * 财务审批Service (已废弃)
 * @deprecated 已废弃，仅用于兼容旧代码
 */
@Deprecated
public interface FinanceApprovalService {
    @Deprecated
    boolean createApproval(FinanceApproval approval);
    @Deprecated
    boolean updateApprovalStatus(Long approvalId, String status, Long approverId, String comment);
    @Deprecated
    List<FinanceApproval> getByBusinessIdAndType(Long businessId, String businessType);
    @Deprecated
    List<FinanceApproval> getByApplicantId(Long applicantId);
    @Deprecated
    List<FinanceApproval> getPendingApprovalsByApproverId(Long approverId);
    @Deprecated
    FinanceApproval getApprovalDetail(Long approvalId);

    /**
     * 分页查询财务审批列表，关联展示发票报销信息（F-033 跨层联动）
     *
     * <p>对于 businessType=INVOICE_REIMBURSEMENT 的审批记录，批量关联查询
     * InvoiceReimbursement 信息（reimbursementNo/totalAmount/departmentName），
     * 避免逐条查询导致 N+1 性能问题。</p>
     *
     * @param current      当前页码（从 1 开始）
     * @param size         每页条数
     * @param businessType 业务类型过滤（可空，查询全部类型）
     * @return 分页结果，每条记录为 FinanceApprovalVO（含报销关联字段）
     */
    IPage<FinanceApprovalVO> queryPageWithReimbursement(int current, int size, String businessType);
}
