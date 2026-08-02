package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.marketing.RechargePlanCreateDTO;
import com.foodtraceability.dto.marketing.RechargePlanQueryDTO;
import com.foodtraceability.dto.marketing.RechargePlanUpdateDTO;
import com.foodtraceability.dto.marketing.RechargePlanVO;

import java.util.List;

/**
 * 充值方案服务接口
 * 提供充值方案的增删改查、状态切换等能力
 *
 * 状态：active-启用 inactive-停用
 */
public interface RechargePlanService {

    /**
     * 查询充值方案列表（不分页，返回全部匹配项）
     * 支持按方案名称、类型、状态筛选
     *
     * @param queryDTO 查询条件
     * @return 方案列表
     */
    List<RechargePlanVO> getPlanList(RechargePlanQueryDTO queryDTO);

    /**
     * 根据ID获取充值方案详情
     *
     * @param planId 方案ID
     * @return 方案VO，不存在返回null
     */
    RechargePlanVO getPlanById(String planId);

    /**
     * 创建充值方案
     * 金额由元转分存储
     *
     * @param createDTO 创建DTO
     * @return 创建后的方案VO
     */
    RechargePlanVO createPlan(RechargePlanCreateDTO createDTO);

    /**
     * 更新充值方案
     *
     * @param planId 方案ID
     * @param updateDTO 更新DTO
     * @return 更新后的方案VO
     */
    RechargePlanVO updatePlan(String planId, RechargePlanUpdateDTO updateDTO);

    /**
     * 删除充值方案（逻辑删除）
     *
     * @param planId 方案ID
     */
    void deletePlan(String planId);

    /**
     * 切换方案状态：active ↔ inactive
     *
     * @param planId 方案ID
     */
    void toggleStatus(String planId);
}
