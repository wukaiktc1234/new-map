package com.foodtraceability.service.finance;

import com.foodtraceability.dto.finance.FinanceVoucherCreateDTO;
import com.foodtraceability.dto.finance.FinanceVoucherVO;

/**
 * 自动凭证生成Service接口
 * 根据业务单据自动生成记账凭证，支持幂等性
 */
public interface AutoVoucherService {

    /**
     * 采购入库自动生成凭证
     * 借:库存商品 贷:应付账款
     * @param purchaseOrderId 采购单ID
     * @param amount 金额（分）
     * @param supplierId 供应商ID
     * @return 凭证VO
     */
    FinanceVoucherVO generatePurchaseVoucher(Long purchaseOrderId, Long amount, Long supplierId);

    /**
     * 销售确认自动生成凭证
     * 借:应收/银行 贷:主营业务收入+销项税
     * @param salesOrderId 销售订单ID
     * @param amount 销售金额（分）
     * @param taxAmount 税额（分）
     * @param paymentMethod 支付方式
     * @return 凭证VO
     */
    FinanceVoucherVO generateSalesVoucher(Long salesOrderId, Long amount, Long taxAmount, Integer paymentMethod);

    /**
     * 成本结转自动生成凭证
     * 借:主营业务成本 贷:库存商品
     * @param period 结转期间
     * @param amount 成本金额（分）
     * @return 凭证VO
     */
    FinanceVoucherVO generateCostTransferVoucher(String period, Long amount);

    /**
     * 工资计提自动生成凭证
     * 借:管理费用-工资 贷:应付职工薪酬
     * @param salaryRecordId 工资记录ID
     * @param amount 工资总额（分）
     * @return 凭证VO
     */
    FinanceVoucherVO generateSalaryVoucher(Long salaryRecordId, Long amount);

    /**
     * 折旧计提自动生成凭证
     * 借:管理费用-折旧 贷:累计折旧
     * @param period 计提期间
     * @param amount 折旧金额（分）
     * @return 凭证VO
     */
    FinanceVoucherVO generateDepreciationVoucher(String period, Long amount);

    /**
     * 门店日结完成自动生成凭证（F-008 联动）
     *
     * <p>Sprint 3.1 P0 T-020：方法签名声明。事件类
     * {@code StoreDailySettlementCompletedEvent} 在阶段5 T-035 定义，
     * 此处使用全限定名避免提前 import 导致编译失败。
     * 实现 T-037 在阶段5完成。</p>
     *
     * <p>凭证规则（E03）：
     * 借：银行存款/库存现金；贷：主营业务收入 + 应交税费-销项税额；
     * 借：主营业务成本；贷：库存商品/原材料</p>
     *
     * @param event 门店日结完成事件
     * @return 凭证VO
     */
    FinanceVoucherVO generateStoreSettlementVoucher(
            com.foodtraceability.event.StoreDailySettlementCompletedEvent event);

    /**
     * 发票报销审批通过自动生成凭证（F-011 联动）
     *
     * <p>Sprint 3.1 P0 T-020：方法签名声明。事件类
     * {@code InvoiceReimbursementApprovedEvent} 在阶段5 T-040 定义，
     * 此处使用全限定名避免提前 import 导致编译失败。
     * 实现 T-043 在阶段5完成。</p>
     *
     * <p>凭证规则：借：管理费用-报销；贷：库存现金/银行存款 或 其他应付款。
     * 报销类型 → 科目编码映射（差旅费→5502XX，招待费→5503XX 等）。</p>
     *
     * @param event 发票报销审批通过事件
     * @return 凭证VO
     */
    FinanceVoucherVO generateReimbursementVoucher(
            com.foodtraceability.event.InvoiceReimbursementApprovedEvent event);

    /**
     * 检查是否已存在相同来源的凭证（幂等检查）
     * @param sourceType 来源类型
     * @param sourceId 来源单据ID
     * @return 是否已存在
     */
    boolean existsBySource(Integer sourceType, Long sourceId);
}
