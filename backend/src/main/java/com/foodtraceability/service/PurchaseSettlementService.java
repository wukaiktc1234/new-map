package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.purchase.PurchaseSettlementCreateDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementQueryDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementUpdateDTO;
import com.foodtraceability.entity.PurchaseSettlement;

/**
 * 采购结算单服务接口
 * 管理供应商应付账款的结算流程：创建 -> 部分付款 -> 财务审核 -> 完成
 *
 * <p>状态机：
 * <ul>
 *   <li>pending(0) → partial(1) → finance_reviewing(2) → completed(3)</li>
 *   <li>overdue(4)：到期日过后仍未完成则自动/手动标记为逾期</li>
 * </ul>
 *
 * <p>发票状态：未开票(0) → 已开票(1) → 已收票(2)</p>
 */
public interface PurchaseSettlementService extends IService<PurchaseSettlement> {

    /**
     * 分页查询采购结算单
     * @param queryDTO 查询参数（含分页）
     * @return 分页结果
     */
    Page<PurchaseSettlement> getSettlementPage(PurchaseSettlementQueryDTO queryDTO);

    /**
     * 获取结算单详情
     * @param settlementId 结算单主键ID
     * @return 结算单详情
     */
    PurchaseSettlement getSettlementDetail(Long settlementId);

    /**
     * 创建结算单
     * <p>settlementNo 由后端生成（STL+yyyyMMdd+4位序号），paidAmount 初始化为 0，
     * unpaidAmount 初始化为 totalAmount，status 初始化为 0（待结算），invoiceStatus 初始化为 0（未开票）。</p>
     * @param createDTO 创建数据
     * @return 创建后的结算单
     */
    PurchaseSettlement createSettlement(PurchaseSettlementCreateDTO createDTO);

    /**
     * 更新结算单（仅允许更新 totalAmount/dueDate/paymentMethod/remark）
     * @param settlementId 结算单主键ID
     * @param updateDTO 更新数据
     * @return 更新后的结算单
     */
    PurchaseSettlement updateSettlement(Long settlementId, PurchaseSettlementUpdateDTO updateDTO);

    /**
     * 删除结算单（逻辑删除）
     * @param settlementId 结算单主键ID
     */
    void deleteSettlement(Long settlementId);

    /**
     * 确认结算（付款/完成）
     * <p>action='pay'：记录全额付款。若当前状态为 finance_reviewing 则流转到 completed，
     * 否则流转到 finance_reviewing（付款完成等待财务审核）。</p>
     * <p>action='complete'：强制标记为已完成（仅 finance_reviewing/partial 可调用）。</p>
     * @param settlementId 结算单主键ID
     * @param action 操作类型：pay-付款，complete-完成
     * @param voucherNo 付款凭证号（pay 操作时必填）
     * @return 更新后的结算单
     */
    PurchaseSettlement settle(Long settlementId, String action, String voucherNo);

    /**
     * 申请发票（发票状态：未开票 → 已开票）
     * @param settlementId 结算单主键ID
     * @return 更新后的结算单
     */
    PurchaseSettlement applyInvoice(Long settlementId);

    /**
     * 确认收票（发票状态：已开票 → 已收票，并登记发票号）
     * @param settlementId 结算单主键ID
     * @param invoiceNo 发票号
     * @return 更新后的结算单
     */
    PurchaseSettlement receiveInvoice(Long settlementId, String invoiceNo);
}
