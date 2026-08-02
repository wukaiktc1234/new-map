package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.Payable;

import java.time.LocalDate;

/**
 * 应付账款Service接口
 * 管理供应商欠款，支持账期管理和付款跟踪
 */
public interface PayableService extends IService<Payable> {

    /**
     * 创建应付账款
     */
    PayableVO create(PayableCreateDTO dto);

    /**
     * 更新应付账款
     */
    boolean update(PayableUpdateDTO dto);

    /**
     * 删除应付账款（逻辑删除）
     */
    boolean deletePayable(Long payableId);

    /**
     * 获取应付账款详情
     */
    PayableVO getDetail(Long payableId);

    /**
     * 分页查询应付账款
     */
    IPage<PayableVO> getPage(PayableQueryDTO query);

    /** 确认付款 */
    boolean confirmPayment(Long payableId, Long amount);

    /**
     * 为采购入库单创建应付账款（F-009 联动 / T-038）
     *
     * <p>Sprint 3.1 P0：采购入库确认后由 {@code PurchaseStockinServiceImpl.confirmStockin}
     * 同事务调用此方法生成应付账款记录。</p>
     *
     * <p>幂等性：通过确定性的 {@code payableNo}（"AP" + 0填充的 stockinId）实现，
     * 同一入库单重复调用不会产生重复应付记录。</p>
     *
     * <p>注：由于数据库暂停（ADR-006），payables 表暂无 source_type/source_id 字段，
     * 此方法使用 payable_no 的确定性生成实现幂等，待数据库恢复后可迁移至 source 字段方案。</p>
     *
     * @param stockinId     入库单ID（幂等键）
     * @param supplierId    供应商ID
     * @param supplierName  供应商名称
     * @param orderId       关联采购订单ID
     * @param amount        应付金额（单位：分）
     * @param stockinDate   入库日期（用于计算到期日 = 入库日 + 30天）
     * @return 应付账款VO；若已存在则返回已有记录
     */
    PayableVO createForStockin(Long stockinId, String stockinNo, Long supplierId, String supplierName,
                               Long orderId, String orderNo, Long amount, LocalDate stockinDate);

    /**
     * 为收货确认单创建应付账款
     *
     * <p>采购收货确认后生成应付账款记录，按收货确认单维度独立入账，支持部分收货。</p>
     *
     * <p>幂等性：通过确定性的 {@code payableNo}（"AP" + 0填充的 confirmationId）实现。</p>
     *
     * @param confirmationId 收货确认单ID（幂等键）
     * @param confirmationNo 收货确认单编号
     * @param supplierId     供应商ID
     * @param supplierName   供应商名称
     * @param orderId        关联采购订单ID
     * @param orderNo        采购订单号
     * @param amount         应付金额（单位：分）
     * @param confirmDate    确认日期（用于计算到期日 = 确认日 + 30天）
     * @return 应付账款VO；若已存在则返回已有记录
     */
    PayableVO createForReceiptConfirmation(Long confirmationId, String confirmationNo, Long supplierId, String supplierName,
                                           Long orderId, String orderNo, Long amount, LocalDate confirmDate);

    /**
     * 根据入库单ID作废未付款的应付账款
     *
     * <p>仅当关联应付账款处于待付/逾期状态时可作废；若已部分或全部付款，
     * 抛出业务异常，避免影响已发生的付款流水与凭证。</p>
     *
     * @param stockinId 入库单ID
     * @return 是否作废成功（无关联应付账款也返回 true）
     */
    boolean voidPayableByStockinId(Long stockinId);

    /**
     * 创建红字应付单（采购退货）
     *
     * <p>金额为负，关联原正向应付单，用于退货后的应付余额实时扣减。</p>
     *
     * @param returnId          退货单ID
     * @param originalPayableId 原正向应付单ID
     * @param supplierId        供应商ID
     * @param supplierName      供应商名称
     * @param orderId           采购订单ID
     * @param orderNo           采购订单号
     * @param stockinId         入库单ID
     * @param stockinNo         入库单号
     * @param amount            退货金额（单位：分，正数）
     * @param returnDate        退货日期
     * @param initialStatus     红字单初始状态（1-待付/3-已付清）
     * @return 红字应付单VO
     */
    PayableVO createRedPayableForReturn(Long returnId, Long originalPayableId,
                                        Long supplierId, String supplierName,
                                        Long orderId, String orderNo,
                                        Long stockinId, String stockinNo,
                                        Long amount, LocalDate returnDate,
                                        Integer initialStatus);
}
