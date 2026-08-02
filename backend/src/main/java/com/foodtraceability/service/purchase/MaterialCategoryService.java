package com.foodtraceability.service.purchase;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.purchase.MaterialCategoryCreateDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryQueryDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryUpdateDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryVO;

import java.util.List;

/**
 * 商品分类服务接口
 */
public interface MaterialCategoryService {

    /**
     * 分页查询商品分类列表
     * @param queryDTO 查询参数
     * @return 分页结果（含 parentName + materialCount）
     */
    PageResult<MaterialCategoryVO> getCategoryPage(MaterialCategoryQueryDTO queryDTO);

    /**
     * 根据 ID 获取商品分类详情
     * @param categoryId 分类ID
     * @return 分类 VO（含 parentName + materialCount），不存在返回 null
     */
    MaterialCategoryVO getCategoryById(Long categoryId);

    /**
     * 创建商品分类
     * @param createDTO 创建数据
     * @return 创建后的分类 VO
     */
    MaterialCategoryVO createCategory(MaterialCategoryCreateDTO createDTO);

    /**
     * 更新商品分类（部分更新）
     * @param categoryId 分类ID
     * @param updateDTO 更新数据
     * @return 更新后的分类 VO
     */
    MaterialCategoryVO updateCategory(Long categoryId, MaterialCategoryUpdateDTO updateDTO);

    /**
     * 删除商品分类（逻辑删除）
     * 删除前校验：有子分类或被商品档案引用时禁止删除
     * @param categoryId 分类ID
     * @throws IllegalArgumentException 存在子分类或被引用
     */
    void deleteCategory(Long categoryId) throws IllegalArgumentException;

    /**
     * 更新分类状态（启用/停用）
     * @param categoryId 分类ID
     * @param status 目标状态：1启用 0停用
     * @throws IllegalArgumentException 状态值无效或分类不存在
     */
    void updateStatus(Long categoryId, Integer status) throws IllegalArgumentException;

    /**
     * 获取所有启用的分类列表（用于下拉选项）
     * @return 启用状态分类列表
     */
    List<MaterialCategoryVO> getEnabledList();

    /**
     * 获取所有分类列表（包含禁用）
     * @return 全部分类列表
     */
    List<MaterialCategoryVO> getAllList();
}
