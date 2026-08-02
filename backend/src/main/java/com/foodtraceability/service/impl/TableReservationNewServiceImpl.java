package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.order.ReservationStatsVO;
import com.foodtraceability.entity.TableReservationNew;
import com.foodtraceability.mapper.TableReservationNewMapper;
import com.foodtraceability.service.TableReservationNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约记录服务实现类
 */
@Service
public class TableReservationNewServiceImpl implements TableReservationNewService {

    private static final Logger log = LoggerFactory.getLogger(TableReservationNewServiceImpl.class);

    private final TableReservationNewMapper tableReservationNewMapper;

    public TableReservationNewServiceImpl(TableReservationNewMapper tableReservationNewMapper) {
        this.tableReservationNewMapper = tableReservationNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableReservationNew create(TableReservationNew reservation) {
        log.info("创建预约, 顾客: {}", reservation.getCustomerName());
        // 生成预约编码：RES + yyyyMMddHHmmssSSS（含毫秒，避免并发冲突）
        String code = "RES" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        reservation.setReservationCode(code);
        reservation.setStatus(1); // 1=待确认
        tableReservationNewMapper.insert(reservation);
        return reservation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableReservationNew update(TableReservationNew reservation) {
        log.info("更新预约, ID: {}", reservation.getReservationId());
        tableReservationNewMapper.updateById(reservation);
        return tableReservationNewMapper.selectById(reservation.getReservationId());
    }

    @Override
    public TableReservationNew getById(Long reservationId) {
        // 返回 null 而非抛异常，由 Controller 决定如何返回错误响应
        return tableReservationNewMapper.selectById(reservationId);
    }

    @Override
    public Page<TableReservationNew> queryPage(Page<TableReservationNew> page, Integer status, String keyword, LocalDate date, LocalDate dateEnd) {
        QueryWrapper<TableReservationNew> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        // 关键词搜索：匹配顾客姓名或手机号
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("customer_name", keyword.trim())
                    .or().like("customer_phone", keyword.trim()));
        }
        // 日期范围：date ~ dateEnd（若 dateEnd 为空则精确匹配 date）
        if (date != null && dateEnd != null) {
            wrapper.ge("reservation_date", date);
            wrapper.le("reservation_date", dateEnd);
        } else if (date != null) {
            wrapper.eq("reservation_date", date);
        }
        wrapper.orderByDesc("create_time");
        return tableReservationNewMapper.selectPage(page, wrapper);
    }

    @Override
    public List<TableReservationNew> listByDate(LocalDate date) {
        return tableReservationNewMapper.selectByDate(date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long reservationId, Long operatorId) {
        log.info("确认预约, ID: {}, 操作人: {}", reservationId, operatorId);
        TableReservationNew reservation = requireReservation(reservationId);
        if (reservation.getStatus() != 1) {
            throw new RuntimeException("只有待确认状态的预约才能确认");
        }
        reservation.setStatus(2); // 已确认
        reservation.setConfirmTime(LocalDateTime.now());
        reservation.setConfirmOperatorId(operatorId);
        tableReservationNewMapper.updateById(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long reservationId, Long operatorId) {
        log.info("标记到店, ID: {}, 操作人: {}", reservationId, operatorId);
        TableReservationNew reservation = requireReservation(reservationId);
        if (reservation.getStatus() != 2) {
            throw new RuntimeException("只有已确认的预约才能标记到店");
        }
        reservation.setStatus(3); // 已到店
        reservation.setArriveTime(LocalDateTime.now());
        reservation.setArriveOperatorId(operatorId);
        tableReservationNewMapper.updateById(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long reservationId, String reason, Long operatorId) {
        log.info("取消预约, ID: {}, 原因: {}, 操作人: {}", reservationId, reason, operatorId);
        TableReservationNew reservation = requireReservation(reservationId);
        if (reservation.getStatus() == 3 || reservation.getStatus() == 4 || reservation.getStatus() == 5) {
            throw new RuntimeException("当前状态不允许取消");
        }
        reservation.setStatus(4); // 已取消
        reservation.setCancelTime(LocalDateTime.now());
        reservation.setCancelOperatorId(operatorId);
        tableReservationNewMapper.updateById(reservation);
    }

    @Override
    public ReservationStatsVO getStats() {
        ReservationStatsVO stats = new ReservationStatsVO();
        LocalDate today = LocalDate.now();
        stats.setTodayReservations(safeCount(() -> tableReservationNewMapper.countByDate(today)));
        stats.setPendingConfirm(safeCount(() -> tableReservationNewMapper.countPendingConfirm()));
        stats.setArrived(safeCount(() -> tableReservationNewMapper.countArrived()));
        stats.setConfirmed(safeCount(() -> tableReservationNewMapper.countByDateAndStatus(today, 2)));
        stats.setCancelled(safeCount(() -> tableReservationNewMapper.countByDateAndStatus(today, 4)));
        Long total = safeCount(() -> tableReservationNewMapper.countAll());
        Long cancelled = safeCount(() -> tableReservationNewMapper.countCancelled());
        if (total != null && total > 0) {
            stats.setCancelRate(Math.round((cancelled * 100.0 / total) * 100.0) / 100.0);
        } else {
            stats.setCancelRate(0.0);
        }
        return stats;
    }

    /**
     * 内部辅助：按 ID 查询预约，若不存在抛业务异常
     */
    private TableReservationNew requireReservation(Long reservationId) {
        TableReservationNew reservation = tableReservationNewMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约记录不存在，ID: " + reservationId);
        }
        return reservation;
    }

    /**
     * 内部辅助：安全执行 COUNT 查询，避免空指针
     */
    private Long safeCount(java.util.function.Supplier<Long> supplier) {
        try {
            Long result = supplier.get();
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("统计查询失败: {}", e.getMessage());
            return 0L;
        }
    }
}
