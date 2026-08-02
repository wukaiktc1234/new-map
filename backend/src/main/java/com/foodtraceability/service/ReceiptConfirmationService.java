package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.ReceiptConfirmationCreateDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.entity.ReceiptConfirmation;

/**
 * 收货确认单服务接口
 * 门店/仓库共用，负责实物确认、库存增加、应付生成
 */
public interface ReceiptConfirmationService extends IService<ReceiptConfirmation> {

    /**
     * 创建收货确认单
     * @param createDTO 创建参数
     * @return 创建后的收货确认单详情
     */
    ReceiptConfirmation createConfirmation(ReceiptConfirmationCreateDTO createDTO);

    /**
     * 分页查询收货确认单
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<ReceiptConfirmation> getConfirmationPage(ReceiptConfirmationQueryDTO queryDTO);

    /**
     * 根据ID获取收货确认单详情（含明细）
     * @param confirmationId 确认单ID
     * @return 确认单详情
     */
    ReceiptConfirmation getConfirmationDetail(Long confirmationId);
}
