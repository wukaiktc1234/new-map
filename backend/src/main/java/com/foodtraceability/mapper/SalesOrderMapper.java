package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.entity.SalesOrderDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
    
    IPage<SalesOrder> selectSalesOrderPage(Page<SalesOrder> page, @Param("orderNo") String orderNo, @Param("customerId") Long customerId, @Param("customerName") String customerName, @Param("status") String status, @Param("startDate") java.util.Date startDate, @Param("endDate") java.util.Date endDate);
    
    List<SalesOrderDetail> selectOrderDetailList(@Param("orderId") Long orderId);
    
    void insertOrderDetail(SalesOrderDetail detail);
    
    void deleteOrderDetail(@Param("detailId") Long detailId);
}