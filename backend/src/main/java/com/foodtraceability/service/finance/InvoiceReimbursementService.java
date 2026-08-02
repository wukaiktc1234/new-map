package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.BudgetVO;
import com.foodtraceability.dto.finance.InvoiceReimbursementApproveDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementPayDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementStatsVO;
import com.foodtraceability.dto.finance.InvoiceReimbursementUpdateDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementVO;
import com.foodtraceability.dto.finance.ReimbursementCreateDTO;
import com.foodtraceability.dto.finance.ReimbursementQueryDTO;

import java.util.List;

/**
 * 发票报销Service接口
 *
 * <p>Sprint 3.1 P0 F-001：报销单 CRUD + 审批 + 付款 + 状态机管理 + 统计/预算信息查询。</p>
 *
 * <p>状态机（5 状态，spec/后端/前端三方统一）：</p>
 * <ul>
 *   <li>草稿(0) → 已审批(1)：approve(approved=true)</li>
 *   <li>草稿(0) → 已拒绝(4)：approve(approved=false)</li>
 *   <li>草稿(0) / 已审批(1) → 已取消(3)：cancel()</li>
 *   <li>已审批(1) → 已付款(2)：pay()</li>
 *   <li>已取消(3) / 已拒绝(4) 为终态</li>
 * </ul>
 *
 * <p>所有写操作由实现类加 {@code @Transactional(rollbackFor = Exception.class)}。</p>
 */
public interface InvoiceReimbursementService {

    /**
     * 创建报销申请
     *
     * <p>状态：初始为 0（草稿）；自动生成报销单号 {@code RE + yyyyMMdd + 4位序号}；
     * 同步保存 items 明细，并写入一条 submit 审批记录。</p>
     *
     * @param dto 创建DTO（含申请信息 + items 明细列表）
     * @return 报销单VO（含明细 + 审批记录）
     */
    InvoiceReimbursementVO create(ReimbursementCreateDTO dto);

    /**
     * 分页查询报销单列表
     *
     * @param query 查询条件（含报销单号/申请人/部门/状态/付款状态/报销类型/起止日期/分页参数）
     * @return 分页结果，每条记录为简化的 VO（不含明细/审批记录）
     */
    IPage<InvoiceReimbursementVO> queryPage(ReimbursementQueryDTO query);

    /**
     * 根据ID查询报销详情（含明细 + 审批记录）
     *
     * @param reimbursementId 报销单ID
     * @return 报销单VO（含完整明细和审批记录）
     */
    InvoiceReimbursementVO getById(Long reimbursementId);

    /**
     * 更新报销单（仅草稿状态可更新）
     *
     * @param dto 更新DTO（含 reimbursementId + 可更新字段 + 新的 items 明细）
     * @return 更新后的报销单VO
     */
    InvoiceReimbursementVO update(InvoiceReimbursementUpdateDTO dto);

    /**
     * 审批报销申请
     *
     * <p>状态流转：</p>
     * <ul>
     *   <li>approved=true：草稿(0) → 已审批(1)，approvedAmount 必填且 &gt; 0；审批通过后准备发布
     *       {@code InvoiceReimbursementApprovedEvent}（事件发布延后到 T-042，本方法仅预留接口）</li>
     *   <li>approved=false：草稿(0) → 已拒绝(4)，remark 必填作为拒绝原因（已拒绝为终态）</li>
     * </ul>
     *
     * @param reimbursementId 报销单ID
     * @param approverId      审批人ID（由 Controller 从 SecurityContext 获取并传入）
     * @param approverName    审批人姓名（冗余字段，便于展示）
     * @param dto             审批DTO（含 approved/remark/approvedAmount）
     * @return 审批后的报销单VO
     */
    InvoiceReimbursementVO approve(Long reimbursementId, Long approverId, String approverName,
                                   InvoiceReimbursementApproveDTO dto);

    /**
     * 取消报销申请（仅草稿/已审批状态可取消）
     *
     * @param reimbursementId 报销单ID
     * @param cancelReason    取消原因（写入 reject_reason 字段）
     * @return 取消后的报销单VO
     */
    InvoiceReimbursementVO cancel(Long reimbursementId, String cancelReason);

    /**
     * 标记报销单已付款
     *
     * <p>状态流转：已审批(1) → 已付款(2)；同时设置 paymentStatus=1（已付款）。</p>
     *
     * @param reimbursementId 报销单ID
     * @param dto             付款DTO（含 paymentVoucherNo/paymentDate）
     * @return 付款后的报销单VO
     */
    InvoiceReimbursementVO pay(Long reimbursementId, InvoiceReimbursementPayDTO dto);

    /**
     * 获取报销统计数据
     *
     * <p>按 query 条件聚合统计：各状态报销单数量、总金额、已审批金额、已付款金额等。</p>
     *
     * @param query 查询条件（含起止日期/部门/状态过滤）
     * @return 报销统计VO（专用于报销场景，与财务概览 VO 解耦）
     */
    InvoiceReimbursementStatsVO getStats(ReimbursementQueryDTO query);

    /**
     * 获取预算信息（用于报销页面的预算执行率展示）
     *
     * @param departmentId 部门ID（可空，查询全部部门）
     * @param budgetYear   预算年度（可空，默认当前年）
     * @return 预算VO列表
     */
    List<BudgetVO> getBudgetInfo(Long departmentId, Integer budgetYear);
}
