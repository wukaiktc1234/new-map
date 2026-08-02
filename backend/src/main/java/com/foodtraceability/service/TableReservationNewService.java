package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.order.ReservationStatsVO;
import com.foodtraceability.entity.TableReservationNew;
import java.time.LocalDate;
import java.util.List;

/**
 * 预约记录服务接口
 */
public interface TableReservationNewService {

    /** 创建预约 */
    TableReservationNew create(TableReservationNew reservation);

    /** 更新预约信息 */
    TableReservationNew update(TableReservationNew reservation);

    /**
     * 根据ID获取预约详情
     * @return 预约记录，若不存在返回 null（由 Controller 决定返回 Result.error）
     */
    TableReservationNew getById(Long reservationId);

    /** 分页查询预约列表（支持关键词搜索、日期范围筛选） */
    Page<TableReservationNew> queryPage(Page<TableReservationNew> page, Integer status, String keyword, LocalDate date, LocalDate dateEnd);

    /** 查询指定日期的预约 */
    List<TableReservationNew> listByDate(LocalDate date);

    /** 确认预约（待确认->已确认） */
    void confirm(Long reservationId, Long operatorId);

    /** 标记到店（已确认->已到店） */
    void arrive(Long reservationId, Long operatorId);

    /** 取消预约 */
    void cancel(Long reservationId, String reason, Long operatorId);

    /**
     * 获取预约统计（今日预约数 / 待确认数 / 已到店数 / 取消率）
     */
    ReservationStatsVO getStats();
}
