package com.foodtraceability.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 库存预测/差异分析聚合查询 Mapper（方案D+/E）
 * 基于真实经营数据（订单销量 + 门店库存流水）自校准"实际每份用量"。
 */
@Mapper
public interface StockForecastMapper {

    /**
     * 菜品 BOM 中某原料的每份理论用量
     */
    @Select("SELECT COALESCE(required_quantity, 0) FROM dish_recipes " +
            "WHERE food_id = #{dishId} AND material_id = #{materialId} AND deleted = 0 LIMIT 1")
    BigDecimal getRecipeQty(@Param("dishId") Long dishId, @Param("materialId") Long materialId);

    /**
     * 近 N 天某原料的实际出库消耗量（Σ 库存减少，含销售/损耗/调拨出/调整减，天然含损耗）
     */
    @Select("SELECT COALESCE(SUM(before_stock - after_stock), 0) FROM store_inventory_log " +
            "WHERE product_id = #{materialId} AND before_stock > after_stock " +
            "AND deleted = 0 AND create_time >= #{since} " +
            "AND (#{storeId, jdbcType=VARCHAR} IS NULL OR store_id = #{storeId, jdbcType=VARCHAR})")
    BigDecimal getActualConsumed(@Param("materialId") Long materialId,
                                 @Param("since") LocalDateTime since, @Param("storeId") String storeId);

    /**
     * 近 N 天所有售出、且 BOM 中含该原料的菜品总份数（分母，用于反推实际每份用量）
     */
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "JOIN dish_recipes r ON r.food_id = CAST(oi.food_id AS BIGINT) AND r.material_id = #{materialId} AND r.deleted = 0 " +
            "WHERE oi.deleted = 0 AND o.deleted = 0 AND o.order_status = 2 " +
            "AND o.create_time >= #{since} " +
            "AND (#{storeIdLong, jdbcType=BIGINT} IS NULL OR o.store_id = #{storeIdLong, jdbcType=BIGINT})")
    BigDecimal getSoldServings(@Param("materialId") Long materialId,
                               @Param("since") LocalDateTime since, @Param("storeIdLong") Long storeIdLong);

    /**
     * 近 N 天某菜品的售出总份数（用于日均销量/售罄时间）
     */
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE oi.food_id = CAST(#{dishId} AS VARCHAR) AND oi.deleted = 0 AND o.deleted = 0 AND o.order_status = 2 " +
            "AND o.create_time >= #{since} " +
            "AND (#{storeIdLong, jdbcType=BIGINT} IS NULL OR o.store_id = #{storeIdLong, jdbcType=BIGINT})")
    BigDecimal getDishSoldServings(@Param("dishId") Long dishId,
                                   @Param("since") LocalDateTime since, @Param("storeIdLong") Long storeIdLong);

    /**
     * 某原料近 N 天的理论消耗量（Σ 售出份数 × BOM 用量，方案E 差异分析用）
     */
    @Select("SELECT COALESCE(SUM(oi.quantity * r.required_quantity), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "JOIN dish_recipes r ON r.food_id = CAST(oi.food_id AS BIGINT) AND r.material_id = #{materialId} AND r.deleted = 0 " +
            "WHERE oi.deleted = 0 AND o.deleted = 0 AND o.order_status = 2 " +
            "AND o.create_time >= #{since} " +
            "AND (#{storeIdLong, jdbcType=BIGINT} IS NULL OR o.store_id = #{storeIdLong, jdbcType=BIGINT})")
    BigDecimal getTheoreticalConsumed(@Param("materialId") Long materialId,
                                      @Param("since") LocalDateTime since, @Param("storeIdLong") Long storeIdLong);

    /**
     * 某原料当前可用库存（按门店，可为空=所有门店）
     */
    @Select("SELECT COALESCE(SUM(current_stock), 0) FROM store_inventory " +
            "WHERE material_id = #{materialId} AND deleted = 0 " +
            "AND (#{storeId, jdbcType=VARCHAR} IS NULL OR store_id = #{storeId, jdbcType=VARCHAR})")
    BigDecimal getAvailableStock(@Param("materialId") Long materialId, @Param("storeId") String storeId);

    /**
     * 某原料当前安全库存（单店取一条）
     */
    @Select("SELECT COALESCE(safety_stock, 0) FROM store_inventory " +
            "WHERE material_id = #{materialId} AND deleted = 0 " +
            "AND (#{storeId, jdbcType=VARCHAR} IS NULL OR store_id = #{storeId, jdbcType=VARCHAR}) LIMIT 1")
    BigDecimal getSafetyStock(@Param("materialId") Long materialId, @Param("storeId") String storeId);

    /**
     * 近 N 天所有发生过理论消耗的原料（含售出份数/理论消耗/名称/单位，方案E 差异分析用）
     */
    @Select("SELECT r.material_id AS materialId, r.material_name AS materialName, r.unit AS unit, " +
            "COALESCE(SUM(oi.quantity * r.required_quantity), 0) AS theoreticalConsumed, " +
            "COALESCE(SUM(oi.quantity), 0) AS soldServings " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "JOIN dish_recipes r ON r.food_id = CAST(oi.food_id AS BIGINT) AND r.deleted = 0 " +
            "WHERE oi.deleted = 0 AND o.deleted = 0 AND o.order_status = 2 " +
            "AND o.create_time >= #{since} " +
            "AND (#{storeIdLong, jdbcType=BIGINT} IS NULL OR o.store_id = #{storeIdLong, jdbcType=BIGINT}) " +
            "GROUP BY r.material_id, r.material_name, r.unit")
    List<Map<String, Object>> getTheoreticalConsumedGrouped(
            @Param("since") LocalDateTime since, @Param("storeIdLong") Long storeIdLong);
}
