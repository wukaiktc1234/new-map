package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.FoodNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 菜品Mapper接口
 * 提供菜品数据访问操作
 */
@Mapper
public interface FoodNewMapper extends BaseMapper<FoodNew> {

    /**
     * 根据菜品编码查询
     */
    @Select("SELECT * FROM foods WHERE food_code = #{foodCode} AND deleted = 0")
    FoodNew selectByFoodCode(@Param("foodCode") String foodCode);

    /**
     * 查询在售菜品列表（按分类和排序）
     */
    @Select("SELECT * FROM foods WHERE status = 1 AND deleted = 0 ORDER BY sort_order ASC, food_id DESC")
    Page<FoodNew> selectOnSaleFoods(Page<FoodNew> page);

    /**
     * 查询推荐菜品
     */
    @Select("SELECT * FROM foods WHERE is_recommend = true AND status = 1 AND deleted = 0 ORDER BY sort_order ASC LIMIT #{limit}")
    java.util.List<FoodNew> selectRecommendFoods(@Param("limit") int limit);

    /**
     * 根据分类ID查询菜品数量
     */
    @Select("SELECT COUNT(*) FROM foods WHERE category_id = #{categoryId} AND deleted = 0")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 原子性扣减库存（乐观锁方式）—— foods 表
     * <p>
     * 与 FoodMapper.deductStock（扣减 food 表）配合使用，保证 POS 端下单后
     * 两张菜品表的库存数据一致。POS 端菜品展示来源于 foods 表（FoodNew 实体，
     * FoodServiceImpl.listOnSale() 查询），而库存扣减原本只扣减 food 表，
     * 导致前端看到的库存不会变化。此方法用于同步扣减 foods 表。
     *
     * @param foodCode 菜品编码
     * @param quantity 扣减数量
     * @return 影响行数（0表示库存不足或菜品不存在）
     */
    @Update("UPDATE foods SET stock = stock - #{quantity}, update_time = NOW() WHERE food_code = #{foodCode} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("foodCode") String foodCode, @Param("quantity") Integer quantity);

    /**
     * 原子性增加库存 —— foods 表
     * <p>
     * 与 FoodMapper.addStock（增加 food 表）配合使用，用于订单取消/退款时回滚库存。
     *
     * @param foodCode 菜品编码
     * @param quantity 增加数量
     * @return 影响行数
     */
    @Update("UPDATE foods SET stock = stock + #{quantity}, update_time = NOW() WHERE food_code = #{foodCode} AND deleted = 0 AND stock + #{quantity} <= 99999")
    int addStock(@Param("foodCode") String foodCode, @Param("quantity") Integer quantity);
}
