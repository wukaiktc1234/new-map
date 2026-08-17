package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.entity.SalesOrderDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 销售订单 Mapper（OICBE-B2-002 查询迁移：泛型绑定 SalesOrder 实体随 @TableName("orders") 对齐）
 *
 * <p>T 契约：主键随表 String（T-1，order_id/item_id）、明细归属 order_items（T-2）。
 * B 项（状态机 status 过滤待 PD-017）在 XML 层不参与 SQL。
 */
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    IPage<SalesOrder> selectSalesOrderPage(Page<SalesOrder> page, @Param("orderNo") String orderNo, @Param("customerId") Long customerId, @Param("customerName") String customerName, @Param("status") String status, @Param("startDate") java.util.Date startDate, @Param("endDate") java.util.Date endDate);

    List<SalesOrderDetail> selectOrderDetailList(@Param("orderId") String orderId);

    void insertOrderDetail(SalesOrderDetail detail);

    void deleteOrderDetail(@Param("detailId") String detailId);
}
