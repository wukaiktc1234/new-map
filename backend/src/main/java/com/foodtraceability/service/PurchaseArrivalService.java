package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PurchaseArrivalCloseDTO;
import com.foodtraceability.dto.PurchaseArrivalCreateDTO;
import com.foodtraceability.dto.PurchaseArrivalQueryDTO;
import com.foodtraceability.dto.PurchaseArrivalUpdateDTO;
import com.foodtraceability.entity.PurchaseArrival;

import java.util.List;

/**
 * 采购到货单服务接口
 * 仅负责到货登记，不处理库存和财务
 */
public interface PurchaseArrivalService extends IService<PurchaseArrival> {

    /**
     * 分页查询采购到货单
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<PurchaseArrival> getArrivalPage(PurchaseArrivalQueryDTO queryDTO);

    /**
     * 根据ID获取到货单详情（含明细）
     * @param arrivalId 到货单ID
     * @return 到货单详情
     */
    PurchaseArrival getArrivalDetail(Long arrivalId);

    /**
     * 根据采购订单创建到货单
     * 按采购订单明细的收货地点自动分组，每组生成一张到货单
     * @param createDTO 创建参数
     * @return 生成的到货单列表
     */
    List<PurchaseArrival> createArrivalsFromOrder(PurchaseArrivalCreateDTO createDTO);

    /**
     * 更新到货单物流信息
     * 仅允许更新 PENDING 状态的到货单
     * @param arrivalId 到货单ID
     * @param updateDTO 更新参数
     * @return 更新后的到货单
     */
    PurchaseArrival updateArrival(Long arrivalId, PurchaseArrivalUpdateDTO updateDTO);

    /**
     * 手动关闭 PENDING 状态的到货单
     * @param arrivalId 到货单ID
     * @param closeDTO 关闭参数
     * @return 关闭后的到货单
     */
    PurchaseArrival closeArrival(Long arrivalId, PurchaseArrivalCloseDTO closeDTO);

    /** 到货单质检（1通过/2失败） */
    PurchaseArrival qualityCheck(Long arrivalId, Integer result, String remark);

    /** 到货单确认入库：加库存（仓库+门店/unit_cost）+ 应付 + 回写订单实收 + 追溯码事件 */
    PurchaseArrival confirmArrival(Long arrivalId);

    /**
     * 获取超期待收货的到货单列表
     * @return 超期待收货到货单列表
     */
    List<PurchaseArrival> getOverdueArrivals();

    /**
     * 自动关闭超期待收货的到货单
     * @return 关闭的到货单数量
     */
    int autoCloseOverdueArrivals();
}
