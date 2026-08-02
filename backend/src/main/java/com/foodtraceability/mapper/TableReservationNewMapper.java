package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TableReservationNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 预约记录Mapper接口
 */
@Mapper
public interface TableReservationNewMapper extends BaseMapper<TableReservationNew> {

    /**
     * 根据预约编码查询
     */
    @Select("SELECT * FROM table_reservations WHERE reservation_code = #{reservationCode} AND deleted = 0")
    TableReservationNew selectByReservationCode(@Param("reservationCode") String reservationCode);

    /**
     * 查询指定日期的预约记录
     */
    @Select("SELECT * FROM table_reservations WHERE reservation_date = #{date} AND status IN (1, 2) AND deleted = 0 ORDER BY reservation_time ASC")
    java.util.List<TableReservationNew> selectByDate(@Param("date") java.time.LocalDate date);

    /**
     * 查询桌台在指定日期的预约记录
     */
    @Select("SELECT * FROM table_reservations WHERE table_id = #{tableId} AND reservation_date = #{date} AND status IN (1, 2) AND deleted = 0 ORDER BY reservation_time ASC")
    java.util.List<TableReservationNew> selectByTableAndDate(@Param("tableId") Long tableId, @Param("date") java.time.LocalDate date);

    /**
     * 统计今日预约数（按 reservation_date）
     */
    @Select("SELECT COUNT(*) FROM table_reservations WHERE reservation_date = #{date} AND deleted = 0")
    Long countByDate(@Param("date") java.time.LocalDate date);

    /**
     * 统计待确认数（status=1）
     */
    @Select("SELECT COUNT(*) FROM table_reservations WHERE status = 1 AND deleted = 0")
    Long countPendingConfirm();

    /**
     * 统计已到店数（status=3）
     */
    @Select("SELECT COUNT(*) FROM table_reservations WHERE status = 3 AND deleted = 0")
    Long countArrived();

    /**
     * 统计已取消数（status=4）
     */
    @Select("SELECT COUNT(*) FROM table_reservations WHERE status = 4 AND deleted = 0")
    Long countCancelled();

    /**
     * 统计全部预约数（用于取消率分母）
     */
    @Select("SELECT COUNT(*) FROM table_reservations WHERE deleted = 0")
    Long countAll();

    /**
     * 按日期和状态统计预约数
     */
    @Select("SELECT COUNT(*) FROM table_reservations WHERE reservation_date = #{date} AND status = #{status} AND deleted = 0")
    Long countByDateAndStatus(@Param("date") java.time.LocalDate date, @Param("status") Integer status);
}
