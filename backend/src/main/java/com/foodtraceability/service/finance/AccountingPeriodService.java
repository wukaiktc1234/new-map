package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.AccountingPeriod;

/**
 * 会计期间Service接口
 * 管理会计期间的生命周期，包括创建、结账、反结账、试算平衡、损益结转等
 */
public interface AccountingPeriodService extends IService<AccountingPeriod> {

    /**
     * 创建会计期间
     * @param dto 创建DTO
     * @return 期间VO
     */
    AccountingPeriodVO create(AccountingPeriodCreateDTO dto);

    /**
     * 获取期间详情
     * @param periodId 期间ID
     * @return 期间VO
     */
    AccountingPeriodVO getDetail(Long periodId);

    /**
     * 分页查询会计期间
     * @param current 当前页码
     * @param size 每页大小
     * @param periodType 期间类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    IPage<AccountingPeriodVO> getPage(int current, int size, Integer periodType, Integer status);

    /**
     * 结账
     * @param periodId 期间ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean closePeriod(Long periodId, Long operatorId);

    /**
     * 反结账（重新开放期间）
     * @param periodId 期间ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean reopenPeriod(Long periodId, Long operatorId);

    /**
     * 试算平衡
     * @param periodId 期间ID
     * @return 试算平衡结果
     */
    TrialBalanceResultVO trialBalance(Long periodId);

    /**
     * 获取结账检查清单
     * @param periodId 期间ID
     * @return 检查清单
     */
    ClosingChecklistVO getClosingChecklist(Long periodId);

    /**
     * 损益结转
     * @param periodId 期间ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean profitTransfer(Long periodId, Long operatorId);

    /**
     * 获取当前会计期间
     * @return 当前期间
     */
    AccountingPeriod getCurrentPeriod();
}
