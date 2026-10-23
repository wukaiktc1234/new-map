package com.foodtraceability.service;

import com.foodtraceability.entity.CallRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 叫号管理服务接口
 */
public interface CallNumberService {

    /**
     * 获取待叫号订单列表
     * @return 待叫号记录列表
     */
    List<CallRecord> getPendingOrders();

    /**
     * 获取指定日期的叫号历史
     * @param date 日期
     * @return 叫号记录列表
     */
    List<CallRecord> getHistoryByDate(LocalDate date);

    /**
     * 获取叫号统计
     * @return 统计信息
     */
    Map<String, Integer> getStats();

    /**
     * 叫号
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param tableNumber 桌号
     * @param orderType 订单类型
     * @param itemCount 菜品数量
     * @return 叫号记录
     */
    CallRecord callNumber(String orderId, String orderNumber, String tableNumber,
                          String orderType, Integer itemCount);

    /**
     * 重叫
     * @param orderId 订单ID
     * @return 叫号记录
     */
    CallRecord recallNumber(String orderId);

    /**
     * 标记已取餐
     * @param orderId 订单ID
     * @return 叫号记录
     */
    CallRecord markPicked(String orderId);

    /** 过号：状态置为 passed 并记录过号时间（历史留痕） */
    CallRecord markPassed(String orderId);

    /**
     * 叫号历史查询（标杆级）：关键字（取餐号/订单号模糊）、时间范围、状态、分页。
     *
     * @param keyword 取餐号或订单号模糊匹配，可空
     * @param range   today / 7d / 30d / all，默认 today
     * @param status  单个或逗号分隔的状态集合（pending,called,picked,passed），可空
     * @param page    页码（1 起）
     * @param size    每页条数
     */
    com.foodtraceability.dto.PageResult<CallRecord> queryHistory(String keyword, String range,
                                                                 String status, int page, int size);

    /**
     * 从后厨订单同步待取餐订单
     * @return 同步数量
     */
    int syncFromKitchen();

    /**
     * 从订单创建待取餐记录
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param tableNumber 桌号
     * @param orderType 订单类型
     * @param itemCount 菜品数量
     * @return 叫号记录
     */
    CallRecord createFromOrder(String orderId, String orderNumber, String tableNumber,
                               String orderType, Integer itemCount);

    /**
     * 清理历史记录
     * @param days 保留天数
     * @return 清理数量
     */
    int cleanupOldRecords(int days);
}
