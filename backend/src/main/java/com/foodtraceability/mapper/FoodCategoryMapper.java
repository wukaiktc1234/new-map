package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.FoodCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FoodCategoryMapper extends BaseMapper<FoodCategory> {

    /**
     * 查询所有分类
     * @param params 查询参数
     * @return 分类列表
     */
    List<FoodCategory> selectAllCategories(@Param("params") Map<String, Object> params);

    /**
     * 根据父分类查询子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<FoodCategory> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询分类树
     * @return 分类树结构
     */
    List<FoodCategory> selectCategoryTree();

    /**
     * 更新分类排序
     * @param id 分类ID
     * @param sortOrder 排序值
     * @return 影响行数
     */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    /**
     * 查询分类下的菜品数量
     * @param categoryId 分类ID
     * @return 菜品数量
     */
    Long selectFoodCountByCategory(@Param("categoryId") Long categoryId);
}
