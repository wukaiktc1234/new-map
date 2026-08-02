package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.entity.SalesOrderDetail;
import com.foodtraceability.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/v1/sales/order")
@Tag(name = "销售订单管理")
public class SalesOrderController {
    

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    private final SalesOrderService salesOrderService;
    
    @GetMapping("/page")
    @Operation(summary = "分页查询销售订单")
    public Result<IPage<SalesOrder>> getSalesOrderPage(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String orderNo,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) String customerName,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        Page<SalesOrder> page = new Page<>(current, size);
        IPage<SalesOrder> result = salesOrderService.getSalesOrderPage(page, orderNo, customerId, customerName, status, 
            startDate != null ? new Date(Long.parseLong(startDate)) : null,
            endDate != null ? new Date(Long.parseLong(endDate)) : null
        );
        return Result.success(result);
    }
    
    @PostMapping
    @Operation(summary = "创建销售订单")
    public Result<SalesOrder> createSalesOrder(@RequestBody SalesOrder order) {
        return Result.success(salesOrderService.createSalesOrder(order));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新销售订单")
    public Result<SalesOrder> updateSalesOrder(@PathVariable Long id, @RequestBody SalesOrder order) {
        return Result.success(salesOrderService.updateSalesOrder(id, order));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除销售订单")
    public Result<Void> deleteSalesOrder(@PathVariable Long id) {
        salesOrderService.deleteSalesOrder(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认销售订单")
    public Result<Boolean> confirmOrder(@PathVariable Long id) {
        return Result.success(salesOrderService.confirmOrder(id));
    }
    
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成销售订单")
    public Result<Boolean> completeOrder(@PathVariable Long id) {
        return Result.success(salesOrderService.completeOrder(id));
    }
    
    @PostMapping("/{orderId}/detail")
    @Operation(summary = "添加订单明细")
    public Result<SalesOrderDetail> addOrderDetail(@PathVariable Long orderId, @RequestBody SalesOrderDetail detail) {
        return Result.success(salesOrderService.addOrderDetail(orderId, detail));
    }
    
    @DeleteMapping("/detail/{detailId}")
    @Operation(summary = "删除订单明细")
    public Result<Void> removeOrderDetail(@PathVariable Long detailId) {
        salesOrderService.removeOrderDetail(detailId);
        return Result.success();
    }
}