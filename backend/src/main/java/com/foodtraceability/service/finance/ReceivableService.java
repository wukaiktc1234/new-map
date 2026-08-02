package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.Receivable;

import java.util.Date;

/**
 * 应收账款Service接口
 * 管理客户欠款，支持账龄分析和催款提醒
 */
public interface ReceivableService extends IService<Receivable> {

    /**
     * 创建应收账款
     */
    ReceivableVO create(ReceivableCreateDTO dto);

    /**
     * 更新应收账款
     */
    boolean update(ReceivableUpdateDTO dto);

    /**
     * 删除应收账款（逻辑删除）
     */
    boolean deleteReceivable(Long receivableId);

    /**
     * 获取应收账款详情
     */
    ReceivableVO getDetail(Long receivableId);

    /**
     * 分页查询应收账款
     */
    IPage<ReceivableVO> getPage(ReceivableQueryDTO query);

    /**
     * 确认收款
     */
    boolean confirmPayment(Long receivableId, Long amount);

    /** 核销坏账 */
    boolean writeOff(Long receivableId);

    /**
     * 为销售订单创建应收账款（F-010 联动 / T-039）
     *
     * <p>Sprint 3.1 P0：销售订单确认出库后由
     * {@code SalesOrderServiceImpl.confirmDelivery} 同事务调用此方法生成应收账款记录。</p>
     *
     * <p>幂等性：通过确定性的 {@code receivableNo}（"AR" + 0填充的 orderId）实现，
     * 同一订单重复调用不会产生重复应收记录。</p>
     *
     * <p>注：由于数据库暂停（ADR-006），receivables 表暂无 source_type/source_id 字段，
     * 此方法使用 receivable_no 的确定性生成实现幂等，待数据库恢复后可迁移至 source 字段方案。</p>
     *
     * @param orderId       销售订单ID（幂等键）
     * @param customerId    客户ID
     * @param customerName  客户名称
     * @param amount        应收金额（单位：分）
     * @param orderDate     订单日期（用于计算到期日 = 订单日 + 30天）
     * @return 应收账款VO；若已存在则返回已有记录
     */
    ReceivableVO createForOrder(Long orderId, Long customerId, String customerName,
                                Long amount, Date orderDate);

    /**
     * 为核心订单（OrderNew，订单ID为字符串）创建应收账款（F4 联动）
     *
     * <p>按订单编号（order_code）确定性生成应收编号（"AR"+orderNo）实现幂等，
     * 同一订单重复调用不产生重复应收。仅用于订单完成时未付清的欠款。</p>
     *
     * @param orderNo       订单编号（幂等键）
     * @param customerId    客户ID（可为空=散客）
     * @param customerName  客户名称
     * @param amount        应收金额（单位：分，=未付金额）
     * @param orderDate     订单日期（用于计算到期日 = 订单日 + 30天）
     * @return 应收账款VO；若已存在则返回已有记录
     */
    ReceivableVO createForOrderByNo(String orderNo, Long customerId, String customerName,
                                    Long amount, Date orderDate);
}
