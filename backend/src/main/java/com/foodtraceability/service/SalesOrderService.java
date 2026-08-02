package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.entity.SalesOrderDetail;

import java.util.Date;

public interface SalesOrderService extends IService<SalesOrder> {
    
    IPage<SalesOrder> getSalesOrderPage(Page<SalesOrder> page, String orderNo, Long customerId, String customerName, String status, Date startDate, Date endDate);
    
    SalesOrder createSalesOrder(SalesOrder order);
    
    SalesOrder updateSalesOrder(Long id, SalesOrder order);
    
    SalesOrder getSalesOrderById(Long id);
    
    void deleteSalesOrder(Long id);
    
    Boolean confirmOrder(Long id);

    Boolean completeOrder(Long id);

    /**
     * 确认出库/发货（T-039 联动）
     *
     * <p>Sprint 3.1 P0：订单完成后确认出库，触发应收账款创建。
     * 状态流转：completed → delivered。</p>
     *
     * @param id 订单ID
     * @return 是否成功
     */
    Boolean confirmDelivery(Long id);

    SalesOrderDetail addOrderDetail(Long orderId, SalesOrderDetail detail);
    
    void removeOrderDetail(Long detailId);
}