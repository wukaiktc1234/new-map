package com.foodtraceability.service.trace;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.trace.*;
import com.foodtraceability.entity.PurchaseLedger;

/**
 * 进货台账服务接口
 * 管理电子化进货台账，符合《食品安全法》要求
 */
public interface PurchaseLedgerService extends IService<PurchaseLedger> {

    /**
     * 创建进货台账
     */
    PurchaseLedgerVO create(PurchaseLedgerCreateDTO dto);

    /**
     * 分页查询台账列表
     */
    IPage<PurchaseLedgerVO> queryPage(PurchaseLedgerQueryDTO queryDTO);

    /**
     * 根据ID查询详情
     */
    PurchaseLedgerVO getDetailById(Long ledgerId);

    /**
     * 更新质检结果
     */
    boolean updateQualityResult(Long ledgerId, Integer result, Long inspectorId);
}
