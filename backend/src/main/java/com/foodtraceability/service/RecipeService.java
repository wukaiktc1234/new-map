package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.product.RecipeCreateDTO;
import com.foodtraceability.dto.product.RecipeQueryDTO;
import com.foodtraceability.dto.product.RecipeVO;
import java.util.List;

/**
 * 配方/BOM服务接口
 * 管理菜品的原料配方和成本核算
 */
public interface RecipeService {

    /**
     * 添加配方原料
     * @param dto 创建请求DTO
     * @return 配方视图对象
     */
    RecipeVO create(RecipeCreateDTO dto);

    /**
     * 批量添加配方原料
     * @param foodId 菜品ID
     * @param dtos 原料列表
     * @return 配方视图对象列表
     */
    List<RecipeVO> batchCreate(Long foodId, List<RecipeCreateDTO> dtos);

    /**
     * 删除配方原料
     * @param recipeId 配方ID
     */
    void delete(Long recipeId);

    /**
     * 根据菜品ID获取配方列表
     * @param foodId 菜品ID
     * @return 配方列表
     */
    List<RecipeVO> listByFoodId(Long foodId);

    /**
     * 分页查询配方列表
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Page<RecipeVO> queryPage(RecipeQueryDTO queryDto);

    /**
     * 计算菜品总成本
     * @param foodId 菜品ID
     * @return 成本价（分）
     */
    Long calculateFoodCost(Long foodId);

    /**
     * 更新配方原料单价（当原料价格变动时）
     * @param materialId 原料ID
     * @param newUnitCost 新单价（分）
     */
    void updateMaterialCost(Long materialId, Long newUnitCost);
}
