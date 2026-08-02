package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.BatchStatusUpdateDTO;
import com.foodtraceability.dto.FoodGroupedOrderDTO;
import com.foodtraceability.dto.KitchenOrderCreateDTO;
import com.foodtraceability.dto.KitchenOrderWithWaitTimeDTO;
import com.foodtraceability.dto.KitchenStatsDTO;
import com.foodtraceability.dto.MaterialScanConsumeDTO;
import com.foodtraceability.entity.KitchenOrder;

import java.util.List;

/**
 * 后厨订单服务接口
 */
public interface KitchenOrderService extends IService<KitchenOrder> {

    /**
     * 创建后厨订单
     */
    KitchenOrder create(KitchenOrderCreateDTO dto);

    /**
     * 根据订单ID查询
     */
    KitchenOrder getByOrderId(String orderId);

    /**
     * 根据后厨订单ID查询
     */
    KitchenOrder getByKitchenOrderId(String kitchenOrderId);

    /**
     * 接单
     */
    boolean receiveOrder(String kitchenOrderId, Long chefId, String chefName);

    /**
     * 开始制作
     */
    boolean startMake(String kitchenOrderId);

    /**
     * 完成制作
     */
    boolean completeMake(String kitchenOrderId);

    /**
     * 出餐
     */
    boolean serve(String kitchenOrderId);

    /**
     * 取消订单
     */
    boolean cancel(String kitchenOrderId, String reason);

    /**
     * 扫码消耗原料
     */
    boolean scanConsumeMaterial(MaterialScanConsumeDTO dto);

    /**
     * 获取门店待处理订单
     */
    List<KitchenOrder> getStoreActiveOrders(Long storeId);

    /**
     * 获取厨师进行中订单
     */
    List<KitchenOrder> getChefActiveOrders(Long chefId);

    /**
     * 更新优先级
     */
    boolean updatePriority(String kitchenOrderId, Integer priority);

    /**
     * 获取各状态订单数量
     */
    int countByStatusAndStore(String status, Long storeId);

    /**
     * 生成食品追溯码
     */
    boolean generateFoodTraceCodes(String kitchenOrderId);

    /**
     * 智能排序查询（支持排序和筛选）
     * @param status 状态过滤
     * @param sort 排序字段
     * @param order 排序方向
     * @param page 页码
     * @param size 每页大小
     * @return 带等待时间的订单列表
     */
    List<KitchenOrderWithWaitTimeDTO> getSortedOrders(String status, String sort, String order, int page, int size);

    /**
     * 批量更新状态
     * @param dto 批量状态更新请求
     * @return 更新成功的数量
     */
    int batchUpdateStatus(BatchStatusUpdateDTO dto);

    /**
     * 获取厨房统计面板数据
     * @param date 日期（today或yyyy-MM-dd格式）
     * @return 统计数据
     */
    KitchenStatsDTO getKitchenStats(String date);

    /**
     * 获取超时预警订单列表
     * @param thresholdMinutes 超时阈值（分钟）
     * @return 超时订单列表
     */
    List<KitchenOrder> getOverdueOrders(int thresholdMinutes);

    /**
     * 按菜品聚合订单视图
     * @param status 状态过滤
     * @return 按菜品聚合的数据
     */
    List<FoodGroupedOrderDTO> getOrdersGroupedByFood(String status);
}
