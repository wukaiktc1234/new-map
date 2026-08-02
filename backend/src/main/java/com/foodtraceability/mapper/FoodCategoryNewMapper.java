package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.FoodCategoryNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 菜品分类Mapper接口
 */
@Mapper
public interface FoodCategoryNewMapper extends BaseMapper<FoodCategoryNew> {

    /**
     * 查询所有启用的顶级分类
     */
    @Select("SELECT * FROM food_categories WHERE parent_id = 0 AND status = 1 AND deleted = 0 ORDER BY sort_order ASC")
    java.util.List<FoodCategoryNew> selectTopCategories();

    /**
     * 根据父级ID查询子分类
     */
    @Select("SELECT * FROM food_categories WHERE parent_id = #{parentId} AND status = 1 AND deleted = 0 ORDER BY sort_order ASC")
    java.util.List<FoodCategoryNew> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 检查分类下是否有菜品
     */
    @Select("SELECT COUNT(*) FROM foods WHERE category_id = #{categoryId} AND deleted = 0")
    long countFoodsByCategory(@Param("categoryId") Long categoryId);
}
