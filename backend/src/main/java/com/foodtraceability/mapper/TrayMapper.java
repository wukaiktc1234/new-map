package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Tray;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrayMapper extends BaseMapper<Tray> {

    @Select("SELECT * FROM tray WHERE tray_code = #{trayCode} AND deleted = 0")
    Tray findByTrayCode(@Param("trayCode") String trayCode);

    @Select("SELECT * FROM tray WHERE status = #{status} AND deleted = 0 ORDER BY tray_code")
    List<Tray> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM tray WHERE status = 'idle' AND deleted = 0 ORDER BY tray_code LIMIT #{limit}")
    List<Tray> findIdleTrays(@Param("limit") int limit);

    @Select("SELECT * FROM tray WHERE current_order_id = #{orderId} AND deleted = 0")
    Tray findByOrderId(@Param("orderId") String orderId);

    @Update("UPDATE tray SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE tray SET status = 'bound', current_order_id = #{orderId}, current_kitchen_order_id = #{kitchenOrderId}, bind_time = NOW(), last_state_change_time = NOW(), last_use_time = NOW(), use_count = use_count + 1, update_time = NOW() WHERE id = #{id}")
    int bindOrder(@Param("id") Long id, @Param("orderId") String orderId, @Param("kitchenOrderId") Long kitchenOrderId);

    @Update("UPDATE tray SET status = 'idle', current_order_id = NULL, current_kitchen_order_id = NULL, bind_time = NULL, last_state_change_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int releaseTray(@Param("id") Long id);

    @Update("UPDATE tray SET status = 'making', last_state_change_time = NOW(), last_scan_time = NOW(), last_scan_device_id = #{scanDeviceId}, update_time = NOW() WHERE id = #{id}")
    int updateToMaking(@Param("id") Long id, @Param("scanDeviceId") Long scanDeviceId);

    @Update("UPDATE tray SET status = 'ready', last_state_change_time = NOW(), last_scan_time = NOW(), last_scan_device_id = #{scanDeviceId}, camera_snapshot_url = #{snapshotUrl}, camera_snapshot_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int updateToReady(@Param("id") Long id, @Param("scanDeviceId") Long scanDeviceId, @Param("snapshotUrl") String snapshotUrl);

    @Update("UPDATE tray SET status = 'served', last_state_change_time = NOW(), last_scan_time = NOW(), last_scan_device_id = #{scanDeviceId}, update_time = NOW() WHERE id = #{id}")
    int updateToServed(@Param("id") Long id, @Param("scanDeviceId") Long scanDeviceId);

    @Update("UPDATE tray SET status = 'cleaning', update_time = NOW() WHERE id = #{id}")
    int startCleaning(@Param("id") Long id);

    @Update("UPDATE tray SET status = 'idle', update_time = NOW() WHERE id = #{id}")
    int finishCleaning(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM tray WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") String status);
}
