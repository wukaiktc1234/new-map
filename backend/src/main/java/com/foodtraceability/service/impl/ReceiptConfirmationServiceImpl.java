package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.dto.ReceiptConfirmationCreateDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.entity.PurchaseArrival;
import com.foodtraceability.entity.PurchaseArrivalItem;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.ReceiptConfirmation;
import com.foodtraceability.entity.ReceiptConfirmationItem;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.event.EventPublisher;
import com.foodtraceability.event.ReceiptConfirmationCompletedEvent;
import com.foodtraceability.mapper.PurchaseArrivalItemMapper;
import com.foodtraceability.mapper.PurchaseArrivalMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.ReceiptConfirmationItemMapper;
import com.foodtraceability.mapper.ReceiptConfirmationMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.ReceiptConfirmationService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.SupplierService;
import com.foodtraceability.service.finance.PayableService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 收货确认单服务实现类
 * 门店/仓库共用，负责实物确认、库存增加、应付生成
 */
@Service
public class ReceiptConfirmationServiceImpl extends ServiceImpl<ReceiptConfirmationMapper, ReceiptConfirmation>
        implements ReceiptConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptConfirmationServiceImpl.class);

    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_REJECTED = 2;

    private static final int ARRIVAL_STATUS_PENDING = 0;
    private static final int ARRIVAL_STATUS_RECEIVING = 1;
    private static final int ARRIVAL_STATUS_PARTIAL_RECEIVED = 2;
    private static final int ARRIVAL_STATUS_RECEIVED = 3;

    private static final String RECEIVER_STORE = "STORE";
    private static final String RECEIVER_WAREHOUSE = "WAREHOUSE";

    private static final String RECEIPT_SOURCE_ARRIVAL = "arrival";

    private final ReceiptConfirmationMapper confirmationMapper;
    private final ReceiptConfirmationItemMapper confirmationItemMapper;
    private final PurchaseArrivalMapper arrivalMapper;
    private final PurchaseArrivalItemMapper arrivalItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final StoreInventoryService storeInventoryService;
    private final PayableService payableService;
    private final SupplierService supplierService;
    private final EventPublisher eventPublisher;

    public ReceiptConfirmationServiceImpl(ReceiptConfirmationMapper confirmationMapper,
                                          ReceiptConfirmationItemMapper confirmationItemMapper,
                                          PurchaseArrivalMapper arrivalMapper,
                                          PurchaseArrivalItemMapper arrivalItemMapper,
                                          PurchaseOrderMapper orderMapper,
                                          PurchaseOrderItemMapper orderItemMapper,
                                          InventoryService inventoryService,
                                          StoreInventoryService storeInventoryService,
                                          PayableService payableService,
                                          SupplierService supplierService,
                                          EventPublisher eventPublisher) {
        this.confirmationMapper = confirmationMapper;
        this.confirmationItemMapper = confirmationItemMapper;
        this.arrivalMapper = arrivalMapper;
        this.arrivalItemMapper = arrivalItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.storeInventoryService = storeInventoryService;
        this.payableService = payableService;
        this.supplierService = supplierService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiptConfirmation createConfirmation(ReceiptConfirmationCreateDTO createDTO) {
        // 2026-07-31 决策：移除「无单直收」，收货确认仅支持到货确认（采购订单→到货单→收货确认）
        return createArrivalConfirmation(createDTO);
    }

    /**
     * 到货确认收货（默认）：基于采购到货单收货
     */
    private ReceiptConfirmation createArrivalConfirmation(ReceiptConfirmationCreateDTO createDTO) {
        PurchaseArrival arrival = arrivalMapper.selectById(createDTO.getArrivalId());
        if (arrival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "到货单不存在");
        }
        if (!isArrivalAllowConfirm(arrival.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前到货单状态不允许收货确认");
        }

        List<PurchaseArrivalItem> arrivalItems = getArrivalItems(arrival.getArrivalId());
        List<ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO> itemDTOs = createDTO.getItems();
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收货明细不能为空");
        }

        // 预校验：每个收货数量不超过剩余可收数量
        for (ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO : itemDTOs) {
            validateConfirmQuantity(itemDTO, arrivalItems);
        }

        // 保存收货确认单主表
        ReceiptConfirmation confirmation = buildConfirmation(arrival, createDTO);
        confirmation.setReceiptSource(RECEIPT_SOURCE_ARRIVAL);
        confirmationMapper.insert(confirmation);

        BigDecimal totalQuantity = BigDecimal.ZERO;
        long totalAmount = 0L;
        for (ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO : itemDTOs) {
            PurchaseArrivalItem arrivalItem = findArrivalItem(itemDTO.getArrivalItemId(), arrivalItems);
            BigDecimal confirmedQty = itemDTO.getConfirmedQuantity() != null ? itemDTO.getConfirmedQuantity() : BigDecimal.ZERO;
            BigDecimal rejectedQty = itemDTO.getRejectedQuantity() != null ? itemDTO.getRejectedQuantity() : BigDecimal.ZERO;
            long itemAmount = confirmedQty.longValue() * (arrivalItem.getUnitPrice() != null ? arrivalItem.getUnitPrice() : 0L);

            ReceiptConfirmationItem confirmationItem = buildConfirmationItem(confirmation.getConfirmationId(), arrivalItem, itemDTO, itemAmount);
            confirmationItemMapper.insert(confirmationItem);

            // 更新到货明细已确认数量
            updateArrivalItemReceived(arrivalItem, confirmedQty);
            // 更新采购订单明细已收货数量
            updateOrderItemReceived(arrivalItem.getOrderItemId(), confirmedQty);
            // 增加库存
            increaseInventory(arrival, arrivalItem, confirmedQty, confirmation.getConfirmationCode(), itemDTO.getBatchNo());

            totalQuantity = totalQuantity.add(confirmedQty);
            totalAmount += itemAmount;
        }

        confirmation.setTotalQuantity(totalQuantity);
        confirmation.setTotalAmount(totalAmount);
        confirmationMapper.updateById(confirmation);

        // 更新到货单状态
        updateArrivalStatus(arrival);

        // 生成应付账款
        createPayable(confirmation, arrival);

        // SR-7: 发布收货确认完成事件，异步生成原料追溯码
        publishReceiptConfirmationEvent(confirmation, arrival.getSupplierId(), arrival.getSupplierName());

        return getConfirmationDetail(confirmation.getConfirmationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptConfirmation> getConfirmationPage(ReceiptConfirmationQueryDTO queryDTO) {
        Page<ReceiptConfirmation> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<ReceiptConfirmation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getConfirmationCode())) {
            wrapper.like(ReceiptConfirmation::getConfirmationCode, queryDTO.getConfirmationCode());
        }
        if (queryDTO.getArrivalId() != null) {
            wrapper.eq(ReceiptConfirmation::getArrivalId, queryDTO.getArrivalId());
        }
        if (queryDTO.getOrderId() != null) {
            wrapper.eq(ReceiptConfirmation::getOrderId, queryDTO.getOrderId());
        }
        if (StringUtils.hasText(queryDTO.getReceiverType())) {
            wrapper.eq(ReceiptConfirmation::getReceiverType, queryDTO.getReceiverType());
        }
        if (StringUtils.hasText(queryDTO.getStoreId())) {
            wrapper.eq(ReceiptConfirmation::getStoreId, queryDTO.getStoreId());
        }
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(ReceiptConfirmation::getWarehouseId, queryDTO.getWarehouseId());
        }
        wrapper.orderByDesc(ReceiptConfirmation::getCreateTime);
        Page<ReceiptConfirmation> result = confirmationMapper.selectPage(page, wrapper);
        for (ReceiptConfirmation confirmation : result.getRecords()) {
            confirmation.setItems(getConfirmationItems(confirmation.getConfirmationId()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptConfirmation getConfirmationDetail(Long confirmationId) {
        ReceiptConfirmation confirmation = confirmationMapper.selectById(confirmationId);
        if (confirmation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货确认单不存在");
        }
        confirmation.setItems(getConfirmationItems(confirmationId));
        return confirmation;
    }

    private List<ReceiptConfirmationItem> getConfirmationItems(Long confirmationId) {
        LambdaQueryWrapper<ReceiptConfirmationItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReceiptConfirmationItem::getConfirmationId, confirmationId);
        return confirmationItemMapper.selectList(wrapper);
    }

    private List<PurchaseArrivalItem> getArrivalItems(Long arrivalId) {
        LambdaQueryWrapper<PurchaseArrivalItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseArrivalItem::getArrivalId, arrivalId);
        return arrivalItemMapper.selectList(wrapper);
    }

    private boolean isArrivalAllowConfirm(Integer status) {
        return status != null && (status == ARRIVAL_STATUS_PENDING
                || status == ARRIVAL_STATUS_RECEIVING
                || status == ARRIVAL_STATUS_PARTIAL_RECEIVED);
    }

    private void validateConfirmQuantity(ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO,
                                         List<PurchaseArrivalItem> arrivalItems) {
        PurchaseArrivalItem arrivalItem = findArrivalItem(itemDTO.getArrivalItemId(), arrivalItems);
        BigDecimal confirmedQty = itemDTO.getConfirmedQuantity() != null ? itemDTO.getConfirmedQuantity() : BigDecimal.ZERO;
        BigDecimal rejectedQty = itemDTO.getRejectedQuantity() != null ? itemDTO.getRejectedQuantity() : BigDecimal.ZERO;
        if (confirmedQty.compareTo(BigDecimal.ZERO) < 0 || rejectedQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收货数量不能为负数");
        }
        BigDecimal alreadyReceived = arrivalItem.getReceivedQuantity() != null ? arrivalItem.getReceivedQuantity() : BigDecimal.ZERO;
        BigDecimal remaining = arrivalItem.getExpectedQuantity().subtract(alreadyReceived);
        BigDecimal total = confirmedQty.add(rejectedQty);
        if (total.compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("物料[%s]本次确认数量（%s）超过剩余可收数量（%s）",
                            arrivalItem.getMaterialName(), total.toPlainString(), remaining.toPlainString()));
        }
    }

    private PurchaseArrivalItem findArrivalItem(Long arrivalItemId, List<PurchaseArrivalItem> arrivalItems) {
        return arrivalItems.stream()
                .filter(item -> item.getArrivalItemId().equals(arrivalItemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "到货明细ID不存在: " + arrivalItemId));
    }

    private ReceiptConfirmation buildConfirmation(PurchaseArrival arrival, ReceiptConfirmationCreateDTO createDTO) {
        ReceiptConfirmation confirmation = new ReceiptConfirmation();
        confirmation.setConfirmationCode(generateConfirmationCode());
        confirmation.setArrivalId(arrival.getArrivalId());
        confirmation.setOrderId(arrival.getOrderId());
        confirmation.setReceiverType(arrival.getReceiverType());
        confirmation.setStoreId(arrival.getStoreId());
        confirmation.setWarehouseId(arrival.getWarehouseId());
        confirmation.setConfirmUserId(SecurityUtils.getCurrentUserId());
        confirmation.setConfirmTime(LocalDateTime.now());
        confirmation.setQualityCheckResult(createDTO.getQualityCheckResult());
        confirmation.setQualityRemark(createDTO.getQualityRemark());
        confirmation.setStatus(STATUS_CONFIRMED);
        confirmation.setRemark(createDTO.getRemark());
        return confirmation;
    }

    private ReceiptConfirmationItem buildConfirmationItem(Long confirmationId, PurchaseArrivalItem arrivalItem,
                                                          ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO,
                                                          long itemAmount) {
        ReceiptConfirmationItem item = new ReceiptConfirmationItem();
        item.setConfirmationId(confirmationId);
        item.setArrivalItemId(arrivalItem.getArrivalItemId());
        item.setOrderItemId(arrivalItem.getOrderItemId());
        item.setMaterialId(arrivalItem.getMaterialId());
        item.setMaterialName(arrivalItem.getMaterialName());
        item.setSpecification(arrivalItem.getSpecification());
        item.setUnit(arrivalItem.getUnit());
        item.setConfirmedQuantity(itemDTO.getConfirmedQuantity());
        item.setRejectedQuantity(itemDTO.getRejectedQuantity());
        item.setUnitPrice(arrivalItem.getUnitPrice());
        item.setAmount(itemAmount);
        item.setBatchNo(itemDTO.getBatchNo());
        item.setProductionDate(itemDTO.getProductionDate());
        item.setExpiryDate(itemDTO.getExpiryDate());
        item.setRemark(itemDTO.getRemark());
        return item;
    }

    private void updateArrivalItemReceived(PurchaseArrivalItem arrivalItem, BigDecimal confirmedQty) {
        BigDecimal currentReceived = arrivalItem.getReceivedQuantity() != null ? arrivalItem.getReceivedQuantity() : BigDecimal.ZERO;
        arrivalItem.setReceivedQuantity(currentReceived.add(confirmedQty));
        arrivalItem.setUpdateTime(LocalDateTime.now());
        arrivalItemMapper.updateById(arrivalItem);
    }

    private void updateOrderItemReceived(Long orderItemId, BigDecimal confirmedQty) {
        PurchaseOrderItem orderItem = orderItemMapper.selectById(orderItemId);
        if (orderItem == null) {
            return;
        }
        BigDecimal currentReceived = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
        orderItem.setReceivedQuantity(currentReceived.add(confirmedQty));
        orderItem.setUpdateTime(LocalDateTime.now());
        orderItemMapper.updateById(orderItem);
    }

    private void updateArrivalStatus(PurchaseArrival arrival) {
        List<PurchaseArrivalItem> items = getArrivalItems(arrival.getArrivalId());
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalExpected = BigDecimal.ZERO;
        for (PurchaseArrivalItem item : items) {
            BigDecimal received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : BigDecimal.ZERO;
            BigDecimal expected = item.getExpectedQuantity() != null ? item.getExpectedQuantity() : BigDecimal.ZERO;
            totalReceived = totalReceived.add(received);
            totalExpected = totalExpected.add(expected);
        }

        arrival.setReceivedQuantity(totalReceived);
        if (totalReceived.compareTo(BigDecimal.ZERO) > 0 && totalReceived.compareTo(totalExpected) < 0) {
            arrival.setStatus(ARRIVAL_STATUS_PARTIAL_RECEIVED);
        } else if (totalReceived.compareTo(totalExpected) >= 0) {
            arrival.setStatus(ARRIVAL_STATUS_RECEIVED);
            if (arrival.getActualArrivalDate() == null) {
                arrival.setActualArrivalDate(LocalDate.now());
            }
        } else {
            arrival.setStatus(ARRIVAL_STATUS_RECEIVING);
        }
        arrival.setUpdateTime(LocalDateTime.now());
        arrivalMapper.updateById(arrival);
    }

    private void increaseInventory(PurchaseArrival arrival, PurchaseArrivalItem arrivalItem,
                                   BigDecimal confirmedQty, String confirmationCode, String batchNo) {
        if (confirmedQty == null || confirmedQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String receiverType = arrival.getReceiverType();
        Long unitPrice = arrivalItem.getUnitPrice() != null ? arrivalItem.getUnitPrice() : 0L;
        if (RECEIVER_STORE.equals(receiverType)) {
            storeInventoryService.increaseStock(
                    arrival.getStoreId(),
                    arrivalItem.getMaterialId(),
                    arrivalItem.getMaterialName(),
                    confirmedQty,
                    arrivalItem.getUnit(),
                    unitPrice,
                    1,
                    "采购到货确认入库 - 确认单:" + confirmationCode);
        } else if (RECEIVER_WAREHOUSE.equals(receiverType)) {
            InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
            increaseDTO.setMaterialId(arrivalItem.getMaterialId());
            increaseDTO.setWarehouseId(arrival.getWarehouseId());
            increaseDTO.setQuantity(confirmedQty);
            increaseDTO.setUnitCost(unitPrice);
            increaseDTO.setTransactionType(1);
            increaseDTO.setBatchNo(batchNo);
            increaseDTO.setReferenceNo(confirmationCode);
            increaseDTO.setReferenceType("receipt_confirmation");
            increaseDTO.setRemark("采购收货确认入库");
            inventoryService.increaseInventory(increaseDTO);
        }
    }

    private void createPayable(ReceiptConfirmation confirmation, PurchaseArrival arrival) {
        if (confirmation.getTotalAmount() == null || confirmation.getTotalAmount() <= 0) {
            return;
        }
        PurchaseOrder order = orderMapper.selectById(arrival.getOrderId());
        String orderNo = order != null ? order.getOrderCode() : null;
        Long supplierId = arrival.getSupplierId();
        String supplierName = null;
        if (supplierId != null) {
            Supplier supplier = supplierService.getById(supplierId);
            if (supplier != null) {
                supplierName = supplier.getSupplierName();
            }
        }
        payableService.createForReceiptConfirmation(
                confirmation.getConfirmationId(),
                confirmation.getConfirmationCode(),
                supplierId,
                supplierName,
                arrival.getOrderId(),
                orderNo,
                confirmation.getTotalAmount(),
                LocalDate.now()
        );
    }

    /**
     * 发布收货确认完成事件（SR-7：异步生成原料追溯码）。
     * 事件发布失败仅记录日志，不影响收货确认主事务。
     */
    private void publishReceiptConfirmationEvent(ReceiptConfirmation confirmation,
                                                 Long supplierId, String supplierName) {
        try {
            ReceiptConfirmationCompletedEvent event = new ReceiptConfirmationCompletedEvent();
            event.setConfirmationId(confirmation.getConfirmationId());
            event.setConfirmationCode(confirmation.getConfirmationCode());
            event.setReceiptSource(confirmation.getReceiptSource());
            event.setSupplierId(supplierId);
            event.setSupplierName(supplierName);
            event.setStoreId(confirmation.getStoreId());
            List<ReceiptConfirmationItem> items = getConfirmationItems(confirmation.getConfirmationId());
            List<ReceiptConfirmationCompletedEvent.Item> eventItems = new ArrayList<>();
            for (ReceiptConfirmationItem item : items) {
                ReceiptConfirmationCompletedEvent.Item eItem = new ReceiptConfirmationCompletedEvent.Item();
                eItem.setMaterialId(item.getMaterialId());
                eItem.setMaterialName(item.getMaterialName());
                eItem.setConfirmedQuantity(item.getConfirmedQuantity());
                eItem.setUnit(item.getUnit());
                eItem.setBatchNo(item.getBatchNo());
                eItem.setProductionDate(item.getProductionDate());
                eItem.setExpiryDate(item.getExpiryDate());
                eventItems.add(eItem);
            }
            event.setItems(eventItems);
            eventPublisher.publishReceiptConfirmationEvent(event);
        } catch (Exception e) {
            log.error("发布收货确认完成事件失败: confirmationId={}, 错误={}",
                    confirmation.getConfirmationId(), e.getMessage(), e);
        }
    }

    private String generateConfirmationCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RC" + dateStr;
        LambdaQueryWrapper<ReceiptConfirmation> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(ReceiptConfirmation::getConfirmationCode, prefix);
        wrapper.orderByDesc(ReceiptConfirmation::getConfirmationCode);
        wrapper.last("LIMIT 1");
        ReceiptConfirmation last = confirmationMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getConfirmationCode() != null) {
            String seqStr = last.getConfirmationCode().substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }
}
