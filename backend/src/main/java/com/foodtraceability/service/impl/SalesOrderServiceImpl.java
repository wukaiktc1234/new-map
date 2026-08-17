package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.FinanceVoucher;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryCode;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.entity.SalesOrderDetail;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryCodeMapper;
import com.foodtraceability.mapper.SalesOrderMapper;
import com.foodtraceability.service.FinanceVoucherService;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.SalesOrderService;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.service.finance.ReceivableService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder> implements SalesOrderService {

    private static final Logger log = LoggerFactory.getLogger(SalesOrderServiceImpl.class);

    public SalesOrderServiceImpl(SalesOrderMapper salesOrderMapper, InventoryMapper inventoryMapper, InventoryCodeMapper inventoryCodeMapper, InventoryService inventoryService, FinanceVoucherService financeVoucherService, ReceivableService receivableService) {
        this.salesOrderMapper = salesOrderMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryCodeMapper = inventoryCodeMapper;
        this.inventoryService = inventoryService;
        this.financeVoucherService = financeVoucherService;
        this.receivableService = receivableService;
    }

    private final SalesOrderMapper salesOrderMapper;

    private final InventoryMapper inventoryMapper;

    private final InventoryCodeMapper inventoryCodeMapper;

    private final InventoryService inventoryService;

    private final FinanceVoucherService financeVoucherService;

    private final ReceivableService receivableService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    @Override
    public IPage<SalesOrder> getSalesOrderPage(Page<SalesOrder> page, String orderNo, Long customerId, String customerName, String status, Date startDate, Date endDate) {
        return salesOrderMapper.selectSalesOrderPage(page, orderNo, customerId, customerName, status, startDate, endDate);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrder createSalesOrder(SalesOrder order) {
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            order.setOrderNo(generateOrderNo());
        }
        
        if (order.getOrderTime() == null) {
            order.setOrderTime(new Date());
        }
        
        if (order.getStatus() == null) {
            order.setStatus("pending");
        }
        
        salesOrderMapper.insert(order);
        return order;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrder updateSalesOrder(Long id, SalesOrder order) {
        // OICBE-B2-001 类型适配（T-1 主键随表 String）：接口/Controller 签名（Batch 3 B-5 范围）本批不变，
        // 实体层 setId(String) 需由 Long 参数转换；业务语义不变（查询/更新语义迁移归 Batch 3）。
        order.setId(String.valueOf(id));
        salesOrderMapper.updateById(order);
        return salesOrderMapper.selectById(id);
    }
    
    @Override
    public SalesOrder getSalesOrderById(Long id) {
        return salesOrderMapper.selectById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalesOrder(Long id) {
        salesOrderMapper.deleteById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmOrder(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        
        if (!"pending".equals(order.getStatus())) {
            return false;
        }
        
        order.setStatus("preparing");
        order.setUpdatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        salesOrderMapper.updateById(order);
        
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrder(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        
        if (!"preparing".equals(order.getStatus())) {
            return false;
        }
        
        order.setStatus("completed");
        order.setCompletedTime(new Date());
        order.setUpdatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        salesOrderMapper.updateById(order);

        deductInventoryForOrder(id);

        return true;
    }

    /**
     * 确认出库/发货（T-039 联动）
     *
     * <p>状态流转：completed → delivered。状态变更后同事务内调用
     * {@link ReceivableService#createForOrder} 创建应收账款。</p>
     *
     * <p>强一致性：应收创建失败回滚整个出库事务（与"事件隔离"模式不同，
     * 参见 ADR-004）。幂等性由 {@link ReceivableService#createForOrder}
     * 通过确定性 receivableNo（"AR" + 0填充 orderId）保证。</p>
     *
     * @param id 订单ID
     * @return 是否成功（订单不存在或状态非 completed 返回 false）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmDelivery(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) {
            log.warn("确认出库失败：销售订单不存在，订单ID：{}", id);
            return false;
        }

        if (!"completed".equals(order.getStatus())) {
            log.warn("确认出库失败：订单状态非 completed，当前状态：{}，订单ID：{}",
                    order.getStatus(), id);
            return false;
        }

        // 状态流转：completed → delivered
        order.setStatus("delivered");
        order.setUpdatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        salesOrderMapper.updateById(order);

        // 同事务创建应收账款（强一致性，失败回滚）
        createReceivableForOrder(order);

        return true;
    }

    /**
     * 为销售订单创建应收账款（T-039 内部方法）
     *
     * <p>SalesOrder.actualAmount 为 Long（分）（OICBE-B2-001 T-3 类型对齐：Integer→Long），
     * 金额无效时跳过创建。</p>
     *
     * <p>强一致性：异常时抛出 BusinessException 触发事务回滚。
     * 幂等性由 ReceivableService 通过确定性 receivableNo 保证。</p>
     *
     * @param order 销售订单
     */
    private void createReceivableForOrder(SalesOrder order) {
        // OICBE-B2-001 类型适配（T-3）：实体金额对齐 Long
        Long actualAmount = order.getActualAmount();
        if (actualAmount == null || actualAmount <= 0) {
            log.warn("销售订单金额无效，跳过应收账款创建，订单ID：{}，金额：{}",
                    order.getId(), actualAmount);
            return;
        }

        try {
            Long amount = actualAmount;
            // OICBE-B2-001 类型适配（T-1 主键 String 化）：ReceivableService.createForOrder 签名 Long 属
            // T-039 应收联动契约（Batch 3-002 迁移范围），本批不改接口签名；String 订单号转 Long——
            // 非数字订单号（orders.order_id 业务字符串 "O"+时间戳）将抛 NumberFormatException → 下方 catch
            // → BusinessException 明确错误态（与原缺表 500 语义一致，不吞错），如实报告待 Batch 3 契约。
            receivableService.createForOrder(
                    Long.valueOf(order.getId()),
                    order.getCustomerId(),
                    order.getCustomerName(),
                    amount,
                    order.getOrderTime()
            );
            log.info("销售订单应收账款创建完成，订单ID：{}", order.getId());
        } catch (Exception e) {
            log.error("创建销售订单应收账款失败，订单ID：{}，错误：{}",
                    order.getId(), e.getMessage(), e);
            // 应收创建失败回滚整个出库事务（强一致性，与"事件隔离"不同）
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "创建应收账款失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrderDetail addOrderDetail(Long orderId, SalesOrderDetail detail) {
        if (detail.getInventoryCode() == null || detail.getInventoryCode().isEmpty()) {
            // OICBE-B2-001 类型适配（T-7 productId String 化）：generateInventoryCode 参数 Long → String
            detail.setInventoryCode(generateInventoryCode(detail.getProductId()));
        }
        
        salesOrderMapper.insertOrderDetail(detail);
        return detail;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrderDetail(Long detailId) {
        // OICBE-B2-001 类型适配（T-1 明细主键 String 化）：Mapper 签名 Long → String
        salesOrderMapper.deleteOrderDetail(String.valueOf(detailId));
    }
    
    private String generateOrderNo() {
        return "SO" + System.currentTimeMillis();
    }
    
    private String generateInventoryCode(String productId) {
        return "IC" + productId + "-" + System.currentTimeMillis();
    }
    
    private void deductInventoryForOrder(Long orderId) {
        // OICBE-B2-001 类型适配（T-1 主键 String 化）：Mapper.selectOrderDetailList 签名 Long → String
        List<SalesOrderDetail> details = salesOrderMapper.selectOrderDetailList(String.valueOf(orderId));
        
        String storeIdStr = SecurityUtils.getCurrentUserStoreId();
        Long storeId = storeIdStr != null ? Long.parseLong(storeIdStr) : null;
        String username = SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system";
        
        for (SalesOrderDetail detail : details) {
            LambdaQueryWrapper<Inventory> inventoryQuery = new LambdaQueryWrapper<>();
            inventoryQuery.eq(Inventory::getProductId, detail.getProductId());
            inventoryQuery.eq(Inventory::getStoreId, storeId);
            
            Inventory inventory = inventoryMapper.selectOne(inventoryQuery);
            
            if (inventory != null && inventory.getCurrentStock() != null) {
                BigDecimal currentStock = inventory.getCurrentStock();
                BigDecimal quantity = BigDecimal.valueOf(detail.getQuantity());
                BigDecimal newStock = currentStock.subtract(quantity);
                if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                    newStock = BigDecimal.ZERO;
                }
                inventory.setCurrentStock(newStock);
                inventory.setUpdateTime(LocalDateTime.now());
                inventoryMapper.updateById(inventory);
                
                InventoryCode inventoryCode = new InventoryCode();
                inventoryCode.setInventoryId(inventory.getId());
                inventoryCode.setCodeType("CUSTOM");
                inventoryCode.setUniqueCode(detail.getInventoryCode() != null ? detail.getInventoryCode() : "");
                inventoryCode.setQrCode(generateQRCode(detail.getInventoryCode() != null ? detail.getInventoryCode() : ""));
                inventoryCode.setStatus(1);
                inventoryCode.setStoreId(storeId);
                inventoryCode.setCreatedBy(username);
                inventoryCodeMapper.insert(inventoryCode);
            }
        }
        
        createFinanceVoucherForOrder(orderId);
    }
    
    private String generateQRCode(String inventoryCode) {
        return "QR:" + inventoryCode;
    }
    
    private void createFinanceVoucherForOrder(Long orderId) {
        LambdaQueryWrapper<SalesOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SalesOrder::getId, orderId);
        
        SalesOrder order = salesOrderMapper.selectOne(queryWrapper);
        
        if (order != null) {
            FinanceVoucher voucher = new FinanceVoucher();
            voucher.setVoucherType("sales");
            voucher.setBusinessType("order");
            voucher.setBusinessId(orderId);
            voucher.setDebitAmount(0L);
            voucher.setCreditAmount(order.getActualAmount() != null ? order.getActualAmount().longValue() : 0L);
            voucher.setCurrency("CNY");
            voucher.setVoucherDate(order.getOrderTime() != null ? order.getOrderTime() : new Date());
            voucher.setStatus("pending");
            voucher.setRemark("销售订单：" + (order.getOrderNo() != null ? order.getOrderNo() : ""));
            
            String storeIdStr = SecurityUtils.getCurrentUserStoreId();
            voucher.setStoreId(storeIdStr != null ? Long.parseLong(storeIdStr) : null);
            voucher.setCreatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
            financeVoucherService.createFinanceVoucher(voucher);
        }
    }
}
