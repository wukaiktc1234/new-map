package com.foodtraceability.service;

import com.foodtraceability.dto.product.CategoryCreateDTO;
import com.foodtraceability.dto.product.CategoryUpdateDTO;
import com.foodtraceability.dto.product.CategoryVO;
import java.util.List;

/**
 * 分类服务接口
 * 管理菜品分类的树形结构和层级关系
 */
public interface CategoryService {

    /**
     * 创建分类
     * @param dto 创建请求DTO
     * @return 分类视图对象
     */
    CategoryVO create(CategoryCreateDTO dto);

    /**
     * 更新分类信息
     * @param categoryId 分类ID
     * @param dto 更新请求DTO
     * @return 分类视图对象
     */
    CategoryVO update(Long categoryId, CategoryUpdateDTO dto);

    /**
     * 根据ID获取分类详情
     * @param categoryId 分类ID
     * @return 分类视图对象
     */
    CategoryVO getById(Long categoryId);

    /**
     * 获取分类树形结构（全部）
     * @return 分类树列表
     */
    List<CategoryVO> getTree();

    /**
     * 获取启用的分类树形结构
     * @return 启用的分类树列表
     */
    List<CategoryVO> getEnabledTree();

    /**
     * 获取所有分类列表（扁平）
     * @return 分类列表
     */
    List<CategoryVO> listAll();

    /**
     * 获取子分类列表
     * @param parentId 父级分类ID
     * @return 子分类列表
     */
    List<CategoryVO> listChildren(Long parentId);

    /**
     * 更新分类状态
     * @param categoryId 分类ID
     * @param status 目标状态 1启用 2停用
     */
    void updateStatus(Long categoryId, Integer status);

    /**
     * 排序分类
     * @param categoryId 分类ID
     * @param newSortOrder 新排序值
     */
    void updateSortOrder(Long categoryId, Integer newSortOrder);

    /**
     * 移动分类（更改父级）
     * @param categoryId 分类ID
     * @param newParentId 新父级分类ID
     */
    void moveCategory(Long categoryId, Long newParentId);

    /**
     * 删除分类（逻辑删除）
     * @param categoryId 分类ID
     */
    void delete(Long categoryId);
}
