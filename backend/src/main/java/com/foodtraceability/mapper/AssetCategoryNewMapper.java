package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AssetCategoryNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产分类Mapper接口
 */
@Mapper
public interface AssetCategoryNewMapper extends BaseMapper<AssetCategoryNew> {

    /**
     * 根据父级ID查询子分类列表
     * @param parentId 父级ID
     * @return 子分类列表
     */
    @Select("SELECT * FROM asset_categories WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<AssetCategoryNew> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有启用的顶级分类
     * @return 顶级分类列表
     */
    @Select("SELECT * FROM asset_categories WHERE parent_id = 0 AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<AssetCategoryNew> selectTopLevelCategories();

    /**
     * 检查分类编码是否已存在
     * @param categoryCode 分类编码
     * @param excludeId 排除的分类ID（创建时传 null，更新时传当前分类ID）
     * @return 存在的数量
     *
     * 实现说明：使用 MyBatis 动态 SQL <if> 标签而非 `#{excludeId} IS NULL`，
     * 避免 PostgreSQL JDBC 驱动在 excludeId 为 null 时无法推断参数数据类型
     * （报错：无法确定参数 $N 的数据类型）。
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM asset_categories WHERE category_code = #{categoryCode} AND deleted = 0" +
            "<if test='excludeId != null'> AND category_id != #{excludeId}</if>" +
            "</script>")
    int countByCode(@Param("categoryCode") String categoryCode, @Param("excludeId") Long excludeId);
}
