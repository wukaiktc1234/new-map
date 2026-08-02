package com.foodtraceability.service.operations;

import java.util.List;
import java.util.Map;

/**
 * 实时监控服务接口
 * 提供门店实时运营数据、趋势数据、告警列表等聚合查询能力
 *
 * <p>数据来源：
 * <ul>
 *   <li>{@link com.foodtraceability.mapper.OrderNewMapper}：订单数、销售额、堂食/外卖分布</li>
 *   <li>{@link com.foodtraceability.mapper.StoreNewMapper}：活跃门店列表</li>
 *   <li>{@link com.foodtraceability.mapper.schedule.ScheduleEntryMapper}：在岗人数</li>
 * </ul>
 */
public interface LiveMonitorService {

    /**
     * 获取实时监控总览
     * @return 总览数据（storeCount/onlineStores/todayOrders/todayRevenue/totalStaff/avgTurnover）
     */
    Map<String, Object> getOverview();

    /**
     * 获取门店实时状态列表
     * @return 门店列表，每条含 storeName/todayRevenue/yesterdayRevenue/orderCount/dineIn/takeaway/selfPickup/turnoverRate/staffOnDuty/status
     */
    List<Map<String, Object>> getStores();

    /**
     * 获取趋势数据
     * @param dimension 时间维度：24h（24小时）/ 7d（7天）/ 30d（30天）
     * @return 趋势数据（dimension/dates/revenue/orders）
     */
    Map<String, Object> getTrend(String dimension);

    /**
     * 获取实时告警列表
     * @return 告警列表，每条含 id/level/storeName/message/time
     */
    List<Map<String, Object>> getAlerts();
}
