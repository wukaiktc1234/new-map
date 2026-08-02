package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PurchaseReturnApproveDTO;
import com.foodtraceability.dto.PurchaseReturnCreateDTO;
import com.foodtraceability.dto.PurchaseReturnVO;
import com.foodtraceability.entity.PurchaseReturn;

/**
 * 采购退货单服务接口
 */
public interface PurchaseReturnService extends IService<PurchaseReturn> {

    /**
     * 分页查询采购退货单
     */
    Page<PurchaseReturnVO> getPurchaseReturnPage(int current, int size, String returnNo,
                                                 Long supplierId, String stockinNo,
                                                 String status, String startDate, String endDate);

    /**
     * 创建采购退货单
     */
    PurchaseReturnVO createPurchaseReturn(PurchaseReturnCreateDTO dto);

    /**
     * 更新采购退货单（仅待审批状态可修改）
     */
    PurchaseReturnVO updatePurchaseReturn(Long id, PurchaseReturnCreateDTO dto);

    /**
     * 删除采购退货单（逻辑删除）
     */
    void deletePurchaseReturn(Long id);

    /**
     * 获取采购退货单详情
     */
    PurchaseReturnVO getPurchaseReturnById(Long id);

    /**
     * 审批采购退货单
     */
    PurchaseReturnVO approvePurchaseReturn(Long id, PurchaseReturnApproveDTO dto);

    /**
     * 完成采购退货单（现金退款到账确认）
     */
    PurchaseReturnVO completePurchaseReturn(Long id);
}
