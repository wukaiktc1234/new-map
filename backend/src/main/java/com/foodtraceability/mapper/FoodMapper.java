package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Food;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FoodMapper extends BaseMapper<Food> {

    /**
     * 分页查询菜品
     * @param page 分页对象
     * @param params 查询参数
     * @return 分页结果
     */
    Page<Food> selectFoodPage(Page<Food> page, @Param("params") Map<String, Object> params);

    /**
     * 根据分类查询菜品
     * @param categoryId 分类ID
     * @return 菜品列表
     */
    List<Food> selectByCategory(@Param("categoryId") String categoryId);

    /**
     * 根据状态查询菜品
     * @param status 状态
     * @return 菜品列表
     */
    List<Food> selectByStatus(@Param("status") String status);

    /**
     * 批量更新菜品状态
     * @param ids 菜品ID列表
     * @param status 状态
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<String> ids, @Param("status") String status);

    /**
     * 查询菜品数量
     * @param params 查询参数
     * @return 菜品数量
     */
    Long selectFoodCount(@Param("params") Map<String, Object> params);

    /**
     * 查询最大菜品编码
     * @return 最大菜品编码
     */
    String selectMaxFoodCode();

    /**
     * 使用悲观锁查询菜品（用于库存扣减）
     * @param foodCode 菜品编码
     * @return 菜品实体
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM food WHERE food_code = #{foodCode} FOR UPDATE")
    Food selectByFoodCodeForUpdate(@Param("foodCode") String foodCode);

    /**
     * 原子性扣减库存（乐观锁方式）
     * @param foodCode 菜品编码
     * @param quantity 扣减数量
     * @return 影响行数（0表示库存不足或版本冲突）
     */
    @org.apache.ibatis.annotations.Update("UPDATE food SET stock = stock - #{quantity}, update_time = NOW() WHERE food_code = #{foodCode} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("foodCode") String foodCode, @Param("quantity") Integer quantity);

    @org.apache.ibatis.annotations.Update("UPDATE food SET stock = stock + #{quantity}, update_time = NOW() WHERE food_code = #{foodCode} AND deleted = 0 AND stock + #{quantity} <= 99999")
    int addStock(@Param("foodCode") String foodCode, @Param("quantity") Integer quantity);
}
