package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.product.ComboCreateDTO;
import com.foodtraceability.dto.product.ComboQueryDTO;
import com.foodtraceability.dto.product.ComboUpdateDTO;
import com.foodtraceability.dto.product.ComboVO;
import java.util.List;

/**
 * 套餐服务接口
 * 管理套餐的完整业务逻辑，包括成本计算和组成管理
 */
public interface ComboService {

    /**
     * 创建套餐（含明细）
     * @param dto 创建请求DTO（含明细列表）
     * @return 套餐视图对象
     */
    ComboVO create(ComboCreateDTO dto);

    /**
     * 更新套餐信息（含明细全量更新）
     * @param comboId 套餐ID
     * @param dto 更新请求DTO
     * @return 套餐视图对象
     */
    ComboVO update(Long comboId, ComboUpdateDTO dto);

    /**
     * 根据ID获取套餐详情（含明细）
     * @param comboId 套餐ID
     * @return 套餐视图对象
     */
    ComboVO getById(Long comboId);

    /**
     * 分页查询套餐列表
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Page<ComboVO> queryPage(ComboQueryDTO queryDto);

    /**
     * 查询在售套餐列表
     * @return 在售套餐列表
     */
    List<ComboVO> listOnSale();

    /**
     * 计算套餐成本价
     * @param comboId 套餐ID
     * @return 成本价（分）
     */
    Long calculateCost(Long comboId);

    /**
     * 更新套餐状态
     * @param comboId 套餐ID
     * @param status 目标状态 1在售 2停售
     */
    void updateStatus(Long comboId, Integer status);

    /**
     * 逻辑删除套餐
     * @param comboId 套餐ID
     */
    void delete(Long comboId);
}
