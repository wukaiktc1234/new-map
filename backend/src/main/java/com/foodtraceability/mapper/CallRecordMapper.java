package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CallRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CallRecordMapper extends BaseMapper<CallRecord> {
    
    @Select("SELECT * FROM call_record WHERE status = #{status} ORDER BY create_time ASC")
    List<CallRecord> findByStatus(@Param("status") String status);
    
    @Select("SELECT * FROM call_record WHERE order_id = #{orderId}")
    CallRecord findByOrderId(@Param("orderId") String orderId);
    
    @Select("SELECT * FROM call_record WHERE order_number = #{orderNumber}")
    CallRecord findByOrderNumber(@Param("orderNumber") String orderNumber);
    
    @Select("SELECT * FROM call_record WHERE DATE(create_time) = #{date} ORDER BY create_time DESC")
    List<CallRecord> findByDate(@Param("date") LocalDate date);
    
    @Select("SELECT * FROM call_record WHERE status IN ('pending', 'called') ORDER BY create_time ASC")
    List<CallRecord> findActiveOrders();
    
    @Update("UPDATE call_record SET status = 'called', call_count = call_count + 1, last_call_time = NOW(), first_call_time = COALESCE(first_call_time, NOW()), update_time = NOW() WHERE order_id = #{orderId}")
    int updateCallStatus(@Param("orderId") String orderId);
    
    @Update("UPDATE call_record SET status = 'picked', pick_time = NOW(), wait_seconds = EXTRACT(EPOCH FROM (NOW() - first_call_time))::INTEGER, update_time = NOW() WHERE order_id = #{orderId}")
    int updatePickStatus(@Param("orderId") String orderId);
    
    @Select("SELECT COUNT(*) FROM call_record WHERE status = #{status} AND DATE(create_time) = CURRENT_DATE")
    int countByStatusToday(@Param("status") String status);
    
    @Delete("DELETE FROM call_record WHERE DATE(create_time) < CURRENT_DATE - CAST(#{days} AS INTEGER) * INTERVAL '1 day'")
    int deleteOldRecords(@Param("days") int days);
}
