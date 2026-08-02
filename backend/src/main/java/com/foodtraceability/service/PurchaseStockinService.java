package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PurchaseQualityCheckDTO;
import com.foodtraceability.dto.PurchaseStockinCreateDTO;
import com.foodtraceability.entity.PurchaseStockin;

/**
 * 采购入库单服务接口
 * 管理采购商品的入库验收流程：创建入库单 -> 质检 -> 确认入库
 */
public interface PurchaseStockinService extends IService<PurchaseStockin> {

    /**
     * 分页查询入库单
     * @param current 当前页码
     * @param size 每页条数
     * @param orderId 关联的订单ID（可选）
     * @param status 入库状态（可选）
     * @param stockinNo 入库单号（可选）
     * @param orderNo 采购订单号（可选）
     * @param supplierName 供应商名称（可选）
     * @return 分页结果
     */
    Page<PurchaseStockin> getStockinPage(int current, int size, Long orderId, Integer status,
                                         String stockinNo, String orderNo, String supplierName);

    /**
     * 获取入库单详情（含明细）
     * @param stockinId 入库单主键ID
     * @return 入库单（含items）
     */
    PurchaseStockin getStockinDetail(Long stockinId);

    /**
     * 创建入库单
     * @param createDTO 创建数据
     * @return 创建后的入库单
     */
    PurchaseStockin createStockin(PurchaseStockinCreateDTO createDTO);

    /**
     * 质检操作
     * @param stockinId 入库单主键ID
     * @param qualityCheckDTO 质检信息
     * @return 更新后的入库单
     */
    PurchaseStockin qualityCheck(Long stockinId, PurchaseQualityCheckDTO qualityCheckDTO);

    /**
     * 确认入库（质检通过后执行）
     * @param stockinId 入库单主键ID
     * @return 更新后的入库单
     */
    PurchaseStockin confirmStockin(Long stockinId);

    /**
     * 删除入库单并还原订单明细已收货数量
     * @param stockinId 入库单主键ID
     * @return 是否删除成功
     */
    boolean deleteStockin(Long stockinId);

    /**
     * 作废已入库的入库单
     *
     * <p>作废非撤销：原入库记录保留，仅变更状态为"已作废"，并联动回滚库存、
     * 采购订单已收货数量及未付款的应付账款，确保数据可溯源。</p>
     *
     * @param stockinId 入库单主键ID
     * @param remark 作废备注
     * @param voidBy 作废人ID
     * @return 作废后的入库单
     */
    PurchaseStockin voidStockin(Long stockinId, String remark, Long voidBy);
}
