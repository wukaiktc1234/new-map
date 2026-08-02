package com.foodtraceability.service.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.purchase.PurchasePlanCreateDTO;
import com.foodtraceability.dto.purchase.PurchasePlanQueryDTO;
import com.foodtraceability.dto.purchase.PurchasePlanUpdateDTO;
import com.foodtraceability.dto.purchase.PurchasePlanVO;

/**
 * 采购计划服务接口
 *
 * <p>状态流转：
 * <ul>
 *   <li>draft(0) → pending(1)：submit</li>
 *   <li>pending(1) → approved(2)：approve</li>
 *   <li>pending(1) → rejected(5)：reject</li>
 *   <li>approved(2) → executing(3)：executePlan</li>
 *   <li>executing(3) → completed(4)：由 markPlanCompletedIfNeeded 触发（采购订单全部入库后回写）</li>
 * </ul>
 * </p>
 */
public interface PurchasePlanService {

    /**
     * 分页查询采购计划列表（不含明细）
     */
    IPage<PurchasePlanVO> getPlanPage(PurchasePlanQueryDTO queryDTO);

    /**
     * 查询采购计划详情（含明细列表）
     */
    PurchasePlanVO getPlanDetail(Long id);

    /**
     * 创建采购计划（状态=0 草稿）
     */
    PurchasePlanVO createPlan(PurchasePlanCreateDTO createDTO);

    /**
     * 更新采购计划（仅 status=0 草稿状态下可更新）
     */
    PurchasePlanVO updatePlan(Long id, PurchasePlanUpdateDTO updateDTO);

    /**
     * 删除采购计划（仅 status=0 草稿 或 status=5 已拒绝 可删除）
     */
    void deletePlan(Long id);

    /**
     * 提交审批（status: 0 → 1）
     */
    void submitPlan(Long id);

    /**
     * 审批通过（status: 1 → 2）
     */
    void approvePlan(Long id, String approveBy);

    /**
     * 审批拒绝（status: 1 → 5）
     */
    void rejectPlan(Long id, String reason);

    /**
     * 开始执行（status: 2 → 3）
     */
    void executePlan(Long id);

    /**
     * 采购订单入库完成后回写计划状态（executing(3) → completed(4)）
     *
     * <p>由 PurchaseStockinServiceImpl.confirmStockin 在订单全部入库后调用：
     * 检查该计划关联的所有采购订单是否都已完成（order_status=4），
     * 若是则将计划状态更新为已完成(4)；否则保持执行中。</p>
     *
     * <p>幂等性：若计划已不在执行中状态，直接返回不处理。</p>
     *
     * @param planId 采购计划ID
     */
    void markPlanCompletedIfNeeded(Long planId);

    /**
     * 从库存预警自动生成采购计划（草稿状态）
     *
     * @return 新建的计划（含明细）
     */
    com.foodtraceability.dto.purchase.PurchasePlanVO generateFromStock();

    /**
     * 导出采购计划 Excel
     *
     * @param queryDTO 查询条件
     * @param response HTTP 响应
     */
    void exportPlans(com.foodtraceability.dto.purchase.PurchasePlanQueryDTO queryDTO, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 根据采购计划生成采购订单（草稿状态，幂等：已生成过则返回既有订单）
     *
     * @param planId 计划ID
     * @return 采购订单
     */
    com.foodtraceability.entity.PurchaseOrder generateOrder(Long planId);
}
