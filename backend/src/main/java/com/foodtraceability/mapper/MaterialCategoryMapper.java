package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商品分类 Mapper 接口
 * 提供基础 CRUD（继承 BaseMapper）+ 树形/聚合查询
 */
@Mapper
public interface MaterialCategoryMapper extends BaseMapper<MaterialCategory> {

    /**
     * 统计每个分类下的商品数量（仅统计未删除的商品档案）
     * @param categoryIds 分类ID列表
     * @return Map<categoryId, materialCount>
     */
    @Select({
        "<script>",
        "SELECT category_id AS categoryId, COUNT(*) AS materialCount",
        "FROM material_archives",
        "WHERE deleted = 0",
        "  AND category_id IN",
        "  <foreach collection='categoryIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "GROUP BY category_id",
        "</script>"
    })
    List<Map<String, Object>> countMaterialsByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    /**
     * 查询某个分类下子分类数量（仅未删除）
     * @param parentId 父分类ID
     * @return 子分类数量
     */
    @Select("SELECT COUNT(*) FROM material_categories WHERE deleted = 0 AND parent_id = #{parentId}")
    int countChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 查询被商品档案引用的分类ID列表
     * @param categoryIds 待校验的分类ID列表
     * @return 被引用的分类ID列表
     */
    @Select({
        "<script>",
        "SELECT DISTINCT category_id FROM material_archives",
        "WHERE deleted = 0",
        "  AND category_id IN",
        "  <foreach collection='categoryIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<Long> findReferencedCategoryIds(@Param("categoryIds") List<Long> categoryIds);
}
