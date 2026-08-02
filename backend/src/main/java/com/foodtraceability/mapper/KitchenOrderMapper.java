package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.dto.KitchenOrderFullDTO;
import com.foodtraceability.entity.KitchenOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 后厨订单Mapper接口
 */
@Mapper
public interface KitchenOrderMapper extends BaseMapper<KitchenOrder> {

    @Select("SELECT * FROM kitchen_order WHERE order_id = #{orderId} AND deleted = 0")
    KitchenOrder selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM kitchen_order WHERE status = #{status} AND deleted = 0 ORDER BY priority DESC, create_time ASC")
    List<KitchenOrder> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM kitchen_order WHERE chef_id = #{chefId} AND status IN ('received', 'making') AND deleted = 0 ORDER BY priority DESC, create_time ASC")
    List<KitchenOrder> selectChefActiveOrders(@Param("chefId") Long chefId);

    @Select("SELECT * FROM kitchen_order WHERE store_id = #{storeId} AND status IN ('pending', 'received', 'making') AND deleted = 0 ORDER BY priority DESC, create_time ASC")
    List<KitchenOrder> selectStoreActiveOrders(@Param("storeId") Long storeId);

    @Update("UPDATE kitchen_order SET status = #{status}, update_time = NOW() WHERE kitchen_order_id = #{kitchenOrderId}")
    int updateStatus(@Param("kitchenOrderId") String kitchenOrderId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM kitchen_order WHERE status = #{status} AND store_id = #{storeId} AND deleted = 0")
    int countByStatusAndStore(@Param("status") String status, @Param("storeId") Long storeId);

    @Select("SELECT ko.id, ko.kitchen_order_id, ko.order_id, ko.order_number, ko.order_type, ko.table_number, " +
            "ko.dish_items, ko.total_dishes, o.ORDER_AMOUNT AS total_amount, o.ACTUAL_AMOUNT AS actual_amount, " +
            "CASE o.PAYMENT_METHOD WHEN 0 THEN '微信支付' WHEN 1 THEN '支付宝' WHEN 2 THEN '现金' WHEN 3 THEN '银行卡' ELSE '未知' END AS payment_method, " +
            "ko.priority, ko.status, ko.receive_time, ko.make_start_time, ko.make_complete_time, ko.serve_time, " +
            "ko.cancel_time, ko.cancel_reason, ko.chef_id, ko.chef_name, ko.store_id, ko.store_name, " +
            "ko.material_consumed, ko.material_consume_time, ko.material_locked, ko.material_lock_time, " +
            "ko.food_trace_codes, ko.remark, ko.create_time, ko.update_time, ko.create_by, ko.update_by, ko.deleted, " +
            "o.ORDER_STATUS AS order_status, o.PAYMENT_TIME AS payment_time, o.REMARKS AS order_remarks, " +
            "o.CONTACT_NAME AS contact_name, o.CONTACT_PHONE AS contact_phone " +
            "FROM kitchen_order ko LEFT JOIN orders_legacy o ON ko.order_id = o.ORDER_ID " +
            "WHERE ko.kitchen_order_id = #{kitchenOrderId} AND ko.deleted = 0")
    KitchenOrderFullDTO selectFullById(@Param("kitchenOrderId") String kitchenOrderId);

    @Select("SELECT ko.id, ko.kitchen_order_id, ko.order_id, ko.order_number, ko.order_type, ko.table_number, " +
            "ko.dish_items, ko.total_dishes, o.ORDER_AMOUNT AS total_amount, o.ACTUAL_AMOUNT AS actual_amount, " +
            "CASE o.PAYMENT_METHOD WHEN 0 THEN '微信支付' WHEN 1 THEN '支付宝' WHEN 2 THEN '现金' WHEN 3 THEN '银行卡' ELSE '未知' END AS payment_method, " +
            "ko.priority, ko.status, ko.receive_time, ko.make_start_time, ko.make_complete_time, ko.serve_time, " +
            "ko.cancel_time, ko.cancel_reason, ko.chef_id, ko.chef_name, ko.store_id, ko.store_name, " +
            "ko.material_consumed, ko.material_consume_time, ko.material_locked, ko.material_lock_time, " +
            "ko.food_trace_codes, ko.remark, ko.create_time, ko.update_time, ko.create_by, ko.update_by, ko.deleted, " +
            "o.ORDER_STATUS AS order_status, o.PAYMENT_TIME AS payment_time, o.REMARKS AS order_remarks, " +
            "o.CONTACT_NAME AS contact_name, o.CONTACT_PHONE AS contact_phone " +
            "FROM kitchen_order ko LEFT JOIN orders_legacy o ON ko.order_id = o.ORDER_ID " +
            "WHERE ko.status IN ('pending', 'received', 'making') AND ko.deleted = 0 " +
            "ORDER BY ko.priority DESC, ko.create_time ASC")
    List<KitchenOrderFullDTO> selectActiveOrdersFull();

    @Select("SELECT ko.id, ko.kitchen_order_id, ko.order_id, ko.order_number, ko.order_type, ko.table_number, " +
            "ko.dish_items, ko.total_dishes, o.ORDER_AMOUNT AS total_amount, o.ACTUAL_AMOUNT AS actual_amount, " +
            "CASE o.PAYMENT_METHOD WHEN 0 THEN '微信支付' WHEN 1 THEN '支付宝' WHEN 2 THEN '现金' WHEN 3 THEN '银行卡' ELSE '未知' END AS payment_method, " +
            "ko.priority, ko.status, ko.receive_time, ko.make_start_time, ko.make_complete_time, ko.serve_time, " +
            "ko.cancel_time, ko.cancel_reason, ko.chef_id, ko.chef_name, ko.store_id, ko.store_name, " +
            "ko.material_consumed, ko.material_consume_time, ko.material_locked, ko.material_lock_time, " +
            "ko.food_trace_codes, ko.remark, ko.create_time, ko.update_time, ko.create_by, ko.update_by, ko.deleted, " +
            "o.ORDER_STATUS AS order_status, o.PAYMENT_TIME AS payment_time, o.REMARKS AS order_remarks, " +
            "o.CONTACT_NAME AS contact_name, o.CONTACT_PHONE AS contact_phone " +
            "FROM kitchen_order ko LEFT JOIN orders_legacy o ON ko.order_id = o.ORDER_ID " +
            "WHERE ko.deleted = 0 ORDER BY ko.create_time DESC LIMIT #{limit}")
    List<KitchenOrderFullDTO> selectRecentOrdersFull(@Param("limit") int limit);

    /**
     * 查询超时订单（等待时间超过阈值的待处理订单）
     */
    @Select("SELECT * FROM kitchen_order " +
            "WHERE status IN ('pending', 'received') AND deleted = 0 " +
            "AND TIMESTAMPDIFF(MINUTE, create_time, NOW()) > #{thresholdMinutes} " +
            "ORDER BY create_time ASC")
    List<KitchenOrder> selectOverdueOrders(@Param("thresholdMinutes") int thresholdMinutes);

    /**
     * 统计指定日期的各状态订单数量
     */
    @Select("SELECT status, COUNT(*) as count FROM kitchen_order " +
            "WHERE DATE(create_time) = #{date} AND deleted = 0 " +
            "GROUP BY status")
    List<Map<String, Object>> countByStatusForDate(@Param("date") String date);

    /**
     * 计算指定日期的平均等待时间（从创建到完成制作）
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, create_time, make_complete_time)) as avg_wait_minutes " +
            "FROM kitchen_order WHERE status IN ('completed', 'served') " +
            "AND DATE(create_time) = #{date} AND make_complete_time IS NOT NULL AND deleted = 0")
    Double calculateAvgWaitMinutes(@Param("date") String date);

    /**
     * 计算指定日期的平均制作时间（从开始制作到完成制作）
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, make_start_time, make_complete_time)) as avg_making_minutes " +
            "FROM kitchen_order WHERE status IN ('completed', 'served') " +
            "AND DATE(create_time) = #{date} AND make_complete_time IS NOT NULL AND deleted = 0")
    Double calculateAvgMakingMinutes(@Param("date") String date);

    /**
     * 查找高峰时段（订单数最多的小时）
     */
    @Select("SELECT HOUR(create_time) as hour, COUNT(*) as count " +
            "FROM kitchen_order WHERE DATE(create_time) = #{date} AND deleted = 0 " +
            "GROUP BY HOUR(create_time) ORDER BY count DESC LIMIT 1")
    Map<String, Object> findPeakHour(@Param("date") String date);
}
