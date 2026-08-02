package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OrderItemNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 订单明细Mapper接口
 */
@Mapper
public interface OrderItemNewMapper extends BaseMapper<OrderItemNew> {

    /**
     * 根据订单ID查询所有明细
     */
    @Select("SELECT * FROM order_items WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time ASC")
    java.util.List<OrderItemNew> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 更新厨房状态
     */
    @Update("UPDATE order_items SET kitchen_status = #{kitchenStatus}, update_time = NOW() WHERE item_id = #{itemId} AND deleted = 0")
    int updateKitchenStatus(@Param("itemId") String itemId, @Param("kitchenStatus") Integer kitchenStatus);

    /**
     * 批量更新厨房状态
     */
    @Update("UPDATE order_items SET kitchen_status = #{kitchenStatus}, update_time = NOW() WHERE order_id = #{orderId} AND kitchen_status = #{fromStatus} AND deleted = 0")
    int batchUpdateKitchenStatus(@Param("orderId") String orderId, @Param("kitchenStatus") Integer kitchenStatus, @Param("fromStatus") Integer fromStatus);

    /**
     * 统计待制作/制作中的明细数
     */
    @Select("SELECT COUNT(*) FROM order_items WHERE order_id = #{orderId} AND kitchen_status IN (0, 1) AND deleted = 0")
    long countPendingItems(@Param("orderId") String orderId);

    /**
     * 按厨房状态统计
     */
    @Select("SELECT kitchen_status, COUNT(*) as cnt FROM order_items WHERE order_id = #{orderId} AND deleted = 0 GROUP BY kitchen_status")
    java.util.List<java.util.Map<String, Object>> countByKitchenStatus(@Param("orderId") String orderId);

    /**
     * 销售数据聚合：按商品（菜品/套餐）汇总销量和销售额，返回 TOP N
     * 用于菜品成本分析页面"销售数据图表"展示
     * - product_type: 1单品 2套餐
     * - sales_count: 累计销量（SUM(quantity)）
     * - sales_amount: 累计销售额（SUM(amount)，单位：分）
     * 仅统计未逻辑删除的订单明细
     */
    @Select("SELECT product_name AS productName, product_type AS productType, " +
            "COALESCE(SUM(quantity), 0) AS salesCount, " +
            "COALESCE(SUM(amount), 0) AS salesAmount " +
            "FROM order_items " +
            "WHERE deleted = 0 AND product_name IS NOT NULL AND product_name <> '' " +
            "GROUP BY product_type, food_id, combo_id, product_name " +
            "ORDER BY salesAmount DESC " +
            "LIMIT #{topN}")
    java.util.List<java.util.Map<String, Object>> selectSalesSummary(@Param("topN") int topN);

    /**
     * 周销售量统计：按 food_id 聚合过去 7 天的销量
     * 用于菜品成本分析页面的"销售量"和"周转率"列展示
     * 周转率 = 周销售量 / 当前库存
     *
     * 关联条件：order_items.food_id 不为空且 product_type=1（单品）
     * 时间范围：基于 order_items.create_time 近 7 天（含今天）
     *
     * @return Map<foodId, weeklySales> 通过 List 形式返回，前端需自行聚合
     */
    @Select("SELECT food_id AS foodId, COALESCE(SUM(quantity), 0) AS weeklySales " +
            "FROM order_items " +
            "WHERE deleted = 0 AND food_id IS NOT NULL AND product_type = 1 " +
            "AND create_time >= DATEADD('DAY', -7, CURRENT_TIMESTAMP) " +
            "GROUP BY food_id")
    java.util.List<java.util.Map<String, Object>> selectWeeklySalesForFoods();

    /**
     * 周销售量统计（套餐）：按 combo_id 聚合过去 7 天的销量
     * 用于菜品成本分析页面的"销售量"和"周转率"列展示
     *
     * 关联条件：order_items.combo_id 不为空且 product_type=2（套餐）
     */
    @Select("SELECT combo_id AS comboId, COALESCE(SUM(quantity), 0) AS weeklySales " +
            "FROM order_items " +
            "WHERE deleted = 0 AND combo_id IS NOT NULL AND product_type = 2 " +
            "AND create_time >= DATEADD('DAY', -7, CURRENT_TIMESTAMP) " +
            "GROUP BY combo_id")
    java.util.List<java.util.Map<String, Object>> selectWeeklySalesForCombos();
}
