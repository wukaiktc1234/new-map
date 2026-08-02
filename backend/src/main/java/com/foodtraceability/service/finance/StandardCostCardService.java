package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.StandardCostCard;

/**
 * 标准成本卡Service接口
 * 管理菜品的标准成本，包括标准成本、损耗系数及预警状态，支持成本控制与分析
 */
public interface StandardCostCardService extends IService<StandardCostCard> {

    /**
     * 创建标准成本卡
     * @param dto 创建DTO
     * @return 成本卡VO
     */
    StandardCostCardVO create(StandardCostCardCreateDTO dto);

    /**
     * 更新标准成本卡
     * @param cardId 成本卡ID
     * @param dto 更新DTO
     * @return 是否成功
     */
    boolean update(Long cardId, StandardCostCardUpdateDTO dto);

    /**
     * 删除标准成本卡（逻辑删除）
     * @param cardId 成本卡ID
     * @return 是否成功
     */
    boolean delete(Long cardId);

    /**
     * 获取成本卡详情
     * @param cardId 成本卡ID
     * @return 成本卡VO
     */
    StandardCostCardVO getDetail(Long cardId);

    /**
     * 分页查询标准成本卡
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<StandardCostCardVO> getPage(StandardCostCardQueryDTO query);

    /**
     * 更新预警状态
     * @param cardId 成本卡ID
     * @param warningStatus 预警状态：0-正常 1-预警 2-异常
     * @return 是否成功
     */
    boolean updateWarningStatus(Long cardId, Integer warningStatus);

    /**
     * 按菜品ID查询唯一成本卡
     * @param dishId 菜品ID
     * @return 成本卡VO
     */
    StandardCostCardVO getByDishId(Long dishId);
}
