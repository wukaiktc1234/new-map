package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.PurchaseArrivalCloseDTO;
import com.foodtraceability.dto.PurchaseArrivalCreateDTO;
import com.foodtraceability.dto.PurchaseArrivalQueryDTO;
import com.foodtraceability.dto.PurchaseArrivalUpdateDTO;
import com.foodtraceability.entity.PurchaseArrival;
import com.foodtraceability.entity.PurchaseArrivalItem;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.event.PurchaseStockInEvent;
import com.foodtraceability.mapper.PurchaseArrivalItemMapper;
import com.foodtraceability.mapper.PurchaseArrivalMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.PurchaseArrivalService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.SystemConfigService;
import com.foodtraceability.service.finance.PayableService;
import com.foodtraceability.service.MaterialTraceCodeService;
import com.foodtraceability.dto.MaterialTraceCodeGenerateDTO;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采购到货单服务实现类
 * 仅负责到货登记，按收货地点自动分组，不处理库存和财务
 */
@Service
public class PurchaseArrivalServiceImpl extends ServiceImpl<PurchaseArrivalMapper, PurchaseArrival>
        implements PurchaseArrivalService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseArrivalServiceImpl.class);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RECEIVING = 1;
    private static final int STATUS_PARTIAL_RECEIVED = 2;
    private static final int STATUS_RECEIVED = 3;
    private static final int STATUS_CLOSED = 4;

    /** 质检结果：0待检 1通过 2失败 */
    private static final int QC_PENDING = 0;
    private static final int QC_PASSED = 1;
    private static final int QC_FAILED = 2;

    private static final String CLOSE_REASON_OVERDUE = "auto_closed_overdue";

    private final PurchaseArrivalMapper arrivalMapper;
    private final PurchaseArrivalItemMapper arrivalItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final SystemConfigService systemConfigService;
    private final SupplierMapper supplierMapper;
    private final InventoryService inventoryService;
    private final StoreInventoryService storeInventoryService;
    private final PayableService payableService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MaterialTraceCodeService materialTraceCodeService;

    public PurchaseArrivalServiceImpl(PurchaseArrivalMapper arrivalMapper,
                                      PurchaseArrivalItemMapper arrivalItemMapper,
                                      PurchaseOrderMapper orderMapper,
                                      PurchaseOrderItemMapper orderItemMapper,
                                      SystemConfigService systemConfigService,
                                      SupplierMapper supplierMapper,
                                      InventoryService inventoryService,
                                      StoreInventoryService storeInventoryService,
                                      PayableService payableService,
                                      ApplicationEventPublisher applicationEventPublisher,
                                      MaterialTraceCodeService materialTraceCodeService) {
        this.arrivalMapper = arrivalMapper;
        this.arrivalItemMapper = arrivalItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.systemConfigService = systemConfigService;
        this.supplierMapper = supplierMapper;
        this.inventoryService = inventoryService;
        this.storeInventoryService = storeInventoryService;
        this.payableService = payableService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.materialTraceCodeService = materialTraceCodeService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseArrival> getArrivalPage(PurchaseArrivalQueryDTO queryDTO) {
        Page<PurchaseArrival> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<PurchaseArrival> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getArrivalCode())) {
            wrapper.like(PurchaseArrival::getArrivalCode, queryDTO.getArrivalCode());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(PurchaseArrival::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getReceiverType())) {
            wrapper.eq(PurchaseArrival::getReceiverType, queryDTO.getReceiverType());
        }
        if (StringUtils.hasText(queryDTO.getStoreId())) {
            wrapper.eq(PurchaseArrival::getStoreId, queryDTO.getStoreId());
        }
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(PurchaseArrival::getWarehouseId, queryDTO.getWarehouseId());
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(PurchaseArrival::getSupplierId, queryDTO.getSupplierId());
        }
        if (queryDTO.getEstimatedStartDate() != null) {
            wrapper.ge(PurchaseArrival::getEstimatedArrivalDate, queryDTO.getEstimatedStartDate());
        }
        if (queryDTO.getEstimatedEndDate() != null) {
            wrapper.le(PurchaseArrival::getEstimatedArrivalDate, queryDTO.getEstimatedEndDate());
        }

        if (Boolean.TRUE.equals(queryDTO.getOverdueOnly())) {
            wrapper.isNotNull(PurchaseArrival::getEstimatedArrivalDate)
                    .lt(PurchaseArrival::getEstimatedArrivalDate, LocalDate.now())
                    .in(PurchaseArrival::getStatus, STATUS_PENDING, STATUS_RECEIVING, STATUS_PARTIAL_RECEIVED);
        }
        if (queryDTO.getCreateUserId() != null) {
            wrapper.eq(PurchaseArrival::getCreateUserId, queryDTO.getCreateUserId());
        }

        if (StringUtils.hasText(queryDTO.getOrderCode())) {
            List<Long> orderIds = findOrderIdsByCode(queryDTO.getOrderCode());
            if (orderIds.isEmpty()) {
                return new Page<>(queryDTO.getCurrent(), queryDTO.getSize(), 0);
            }
            wrapper.in(PurchaseArrival::getOrderId, orderIds);
        }

        wrapper.orderByDesc(PurchaseArrival::getCreateTime);
        Page<PurchaseArrival> result = arrivalMapper.selectPage(page, wrapper);
        for (PurchaseArrival arrival : result.getRecords()) {
            enrichDisplayFields(arrival);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseArrival getArrivalDetail(Long arrivalId) {
        PurchaseArrival arrival = arrivalMapper.selectById(arrivalId);
        if (arrival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "到货单不存在");
        }
        LambdaQueryWrapper<PurchaseArrivalItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseArrivalItem::getArrivalId, arrivalId);
        arrival.setItems(arrivalItemMapper.selectList(itemWrapper));
        enrichDisplayFields(arrival);
        return arrival;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PurchaseArrival> createArrivalsFromOrder(PurchaseArrivalCreateDTO createDTO) {
        Long orderId = createDTO.getOrderId();
        PurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
        }
        if (!isOrderAllowArrival(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "只有已审核、已下单、部分入库或已完成状态的订单可创建到货单");
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        List<PurchaseOrderItem> orderItems = orderItemMapper.selectList(itemWrapper);
        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "采购订单明细为空");
        }

        Map<ReceiverKey, List<PurchaseOrderItem>> groups = orderItems.stream()
                .filter(item -> calculateRemainingQuantity(item).compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(ReceiverKey::new));

        if (groups.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "订单已无剩余可到货数量");
        }

        List<PurchaseArrival> result = new ArrayList<>();
        for (Map.Entry<ReceiverKey, List<PurchaseOrderItem>> entry : groups.entrySet()) {
            PurchaseArrival arrival = buildAndSaveArrival(order, entry.getKey(), entry.getValue(), createDTO);
            result.add(arrival);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseArrival updateArrival(Long arrivalId, PurchaseArrivalUpdateDTO updateDTO) {
        PurchaseArrival arrival = arrivalMapper.selectById(arrivalId);
        if (arrival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "到货单不存在");
        }
        if (arrival.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待收货状态的到货单可更新物流信息");
        }
        if (StringUtils.hasText(updateDTO.getShipmentStatus())) {
            arrival.setShipmentStatus(updateDTO.getShipmentStatus());
        }
        if (updateDTO.getLogisticsNo() != null) {
            arrival.setLogisticsNo(updateDTO.getLogisticsNo());
        }
        if (updateDTO.getLogisticsCompany() != null) {
            arrival.setLogisticsCompany(updateDTO.getLogisticsCompany());
        }
        if (updateDTO.getTransportMode() != null) {
            arrival.setTransportMode(updateDTO.getTransportMode());
        }
        if (updateDTO.getVehiclePlateNo() != null) {
            arrival.setVehiclePlateNo(updateDTO.getVehiclePlateNo());
        }
        if (updateDTO.getVehicleType() != null) {
            arrival.setVehicleType(updateDTO.getVehicleType());
        }
        if (updateDTO.getDriverName() != null) {
            arrival.setDriverName(updateDTO.getDriverName());
        }
        if (updateDTO.getDriverPhone() != null) {
            arrival.setDriverPhone(updateDTO.getDriverPhone());
        }
        if (updateDTO.getFreightAmount() != null) {
            arrival.setFreightAmount(updateDTO.getFreightAmount());
        }
        if (updateDTO.getEstimatedArrivalDate() != null) {
            arrival.setEstimatedArrivalDate(updateDTO.getEstimatedArrivalDate());
        }
        if (updateDTO.getActualArrivalDate() != null) {
            arrival.setActualArrivalDate(updateDTO.getActualArrivalDate());
        }
        if (updateDTO.getRemark() != null) {
            arrival.setRemark(updateDTO.getRemark());
        }
        arrivalMapper.updateById(arrival);
        return getArrivalDetail(arrivalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseArrival closeArrival(Long arrivalId, PurchaseArrivalCloseDTO closeDTO) {
        PurchaseArrival arrival = arrivalMapper.selectById(arrivalId);
        if (arrival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "到货单不存在");
        }
        if (arrival.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待收货状态的到货单可手动关闭");
        }
        arrival.setStatus(STATUS_CLOSED);
        arrival.setCloseReason(closeDTO.getCloseReason());
        if (StringUtils.hasText(closeDTO.getRemark())) {
            arrival.setRemark(closeDTO.getRemark());
        }
        arrivalMapper.updateById(arrival);
        return getArrivalDetail(arrivalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseArrival qualityCheck(Long arrivalId, Integer result, String remark) {
        PurchaseArrival arrival = arrivalMapper.selectById(arrivalId);
        if (arrival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "到货单不存在");
        }
        if (arrival.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待收货状态的到货单可质检");
        }
        if (result == null || (result != QC_PASSED && result != QC_FAILED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "质检结果仅支持 1通过/2失败");
        }
        arrival.setQualityCheckResult(result);
        arrival.setQualityCheckRemark(remark);
        if (result == QC_FAILED) {
            // 质检失败：到货单关闭，等待退货/作废处理
            arrival.setStatus(STATUS_CLOSED);
            arrival.setCloseReason("quality_failed");
        }
        arrivalMapper.updateById(arrival);
        log.info("到货单质检完成：arrivalId={}, result={}, remark={}", arrivalId, result, remark);
        return getArrivalDetail(arrivalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseArrival confirmArrival(Long arrivalId) {
        PurchaseArrival arrival = arrivalMapper.selectById(arrivalId);
        if (arrival == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "到货单不存在");
        }
        if (arrival.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待收货状态的到货单可确认入库");
        }
        if (arrival.getQualityCheckResult() == null || arrival.getQualityCheckResult() == QC_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "请先完成质检（通过）后再确认入库");
        }
        if (arrival.getQualityCheckResult() == QC_FAILED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "质检不合格的到货单不能确认入库");
        }

        // 入库确认：状态置为已收货
        arrival.setStatus(STATUS_RECEIVED);
        arrival.setActualArrivalDate(arrival.getActualArrivalDate() != null ? arrival.getActualArrivalDate() : LocalDate.now());
        arrival.setConfirmTime(LocalDateTime.now());
        arrivalMapper.updateById(arrival);
        log.info("到货单确认入库：arrivalId={}, code={}", arrivalId, arrival.getArrivalCode());

        // 1. 增加库存（仓库 + 门店，unit_cost 按最新入库价覆盖）
        increaseInventoryForArrival(arrival);

        // 2. 创建应付账款（幂等：AP + arrivalId）
        createPayableForArrival(arrival);

        // 3. 回写采购订单明细实收数量
        writeBackOrderReceived(arrival);

        // 4. 同步生成原料追溯码（每条明细一个批次）
        generateMaterialTraceCodes(arrival);

        // 5. 发布事件：记录采购成本记录
        publishPurchaseStockInEvent(arrival);

        return getArrivalDetail(arrivalId);
    }

    /** 增加仓库库存 + 门店库存（含 unit_cost 覆盖与流水） */
    private void increaseInventoryForArrival(PurchaseArrival arrival) {
        List<PurchaseArrivalItem> items = loadArrivalItems(arrival.getArrivalId());
        // 集中式单店：门店维度 = 仓库维度；无仓库时兜底默认仓库 1
        String storeIdForSync = arrival.getWarehouseId() != null
                ? String.valueOf(arrival.getWarehouseId())
                : "1";

        for (PurchaseArrivalItem item : items) {
            BigDecimal actualQty = item.getReceivedQuantity() != null && item.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? item.getReceivedQuantity()
                    : item.getExpectedQuantity();
            if (actualQty == null || actualQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
                increaseDTO.setMaterialId(item.getMaterialId());
                increaseDTO.setWarehouseId(arrival.getWarehouseId());
                increaseDTO.setQuantity(actualQty);
                increaseDTO.setUnitCost(item.getUnitPrice());
                increaseDTO.setTransactionType(1); // 采购入库
                increaseDTO.setReferenceNo(arrival.getArrivalCode());
                increaseDTO.setReferenceType("purchase_arrival");
                inventoryService.increaseInventory(increaseDTO);
            } catch (Exception e) {
                log.warn("仓库库存增加失败（继续门店同步）: materialId={}, err={}", item.getMaterialId(), e.getMessage());
            }
            if (storeIdForSync != null) {
                try {
                    storeInventoryService.increaseStock(
                            storeIdForSync,
                            item.getMaterialId(),
                            item.getMaterialName(),
                            actualQty,
                            item.getUnit(),
                            item.getUnitPrice() != null ? item.getUnitPrice() : null,
                            1,
                            "采购入库 - 到货单:" + arrival.getArrivalCode());
                } catch (Exception syncEx) {
                    log.error("门店库存同步失败（事务回滚）: storeId={}, materialId={}, err={}",
                            storeIdForSync, item.getMaterialId(), syncEx.getMessage());
                    throw syncEx;
                }
            }
        }
    }

    /** 创建应付账款（幂等） */
    private void createPayableForArrival(PurchaseArrival arrival) {
        if (arrival.getTotalAmount() == null || arrival.getTotalAmount() <= 0L) {
            log.warn("到货单金额无效，跳过应付创建：arrivalId={}, amount={}", arrival.getArrivalId(), arrival.getTotalAmount());
            return;
        }
        String supplierName = null;
        if (arrival.getSupplierId() != null) {
            try {
                Supplier supplier = supplierMapper.selectById(arrival.getSupplierId());
                if (supplier != null) {
                    supplierName = supplier.getSupplierName();
                }
            } catch (Exception e) {
                log.warn("查询供应商名称失败：supplierId={}", arrival.getSupplierId());
            }
        }
        if (!StringUtils.hasText(supplierName)) {
            supplierName = "供应商" + arrival.getSupplierId();
        }
        PurchaseOrder order = arrival.getOrderId() != null ? orderMapper.selectById(arrival.getOrderId()) : null;
        payableService.createForStockin(
                arrival.getArrivalId(),
                arrival.getArrivalCode(),
                arrival.getSupplierId(),
                supplierName,
                arrival.getOrderId(),
                order != null ? order.getOrderCode() : null,
                arrival.getTotalAmount(),
                arrival.getConfirmTime() != null ? arrival.getConfirmTime().toLocalDate() : LocalDate.now());
        log.info("到货单生成应付成功：arrivalId={}, code={}, amount={}分", arrival.getArrivalId(), arrival.getArrivalCode(), arrival.getTotalAmount());
    }

    /** 回写采购订单明细实收数量（未录入实收时按预计数量兜底） */
    private void writeBackOrderReceived(PurchaseArrival arrival) {
        if (arrival.getOrderId() == null) {
            return;
        }
        for (PurchaseArrivalItem item : loadArrivalItems(arrival.getArrivalId())) {
            if (item.getOrderItemId() == null) {
                continue;
            }
            BigDecimal actualQty = item.getReceivedQuantity() != null && item.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? item.getReceivedQuantity()
                    : item.getExpectedQuantity();
            if (actualQty == null || actualQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 回写到货单明细（未录入实收时按预计数量兜底）
            if (item.getReceivedQuantity() == null || item.getReceivedQuantity().compareTo(BigDecimal.ZERO) == 0) {
                item.setReceivedQuantity(actualQty);
                arrivalItemMapper.updateById(item);
            }
            PurchaseOrderItem orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem == null) {
                continue;
            }
            BigDecimal received = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
            orderItem.setReceivedQuantity(received.add(actualQty));
            orderItemMapper.updateById(orderItem);
        }
    }

    /** 同步生成原料追溯码（每条明细生成一个批次） */
    private void generateMaterialTraceCodes(PurchaseArrival arrival) {
        PurchaseOrder order = arrival.getOrderId() != null ? orderMapper.selectById(arrival.getOrderId()) : null;
        for (PurchaseArrivalItem item : loadArrivalItems(arrival.getArrivalId())) {
            BigDecimal actualQty = item.getReceivedQuantity() != null && item.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? item.getReceivedQuantity()
                    : item.getExpectedQuantity();
            if (actualQty == null || actualQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                MaterialTraceCodeGenerateDTO dto = new MaterialTraceCodeGenerateDTO();
                dto.setMaterialId(item.getMaterialId() != null ? String.valueOf(item.getMaterialId()) : null);
                dto.setMaterialName(item.getMaterialName());
                dto.setProductCode(item.getMaterialId() != null ? String.valueOf(item.getMaterialId()) : null);
                dto.setSupplierId(arrival.getSupplierId() != null ? String.valueOf(arrival.getSupplierId()) : null);
                if (arrival.getSupplierId() != null) {
                    try {
                        Supplier supplier = supplierMapper.selectById(arrival.getSupplierId());
                        if (supplier != null) {
                            dto.setSupplierName(supplier.getSupplierName());
                        }
                    } catch (Exception ignored) {
                    }
                }
                dto.setPurchaseStockinId(String.valueOf(arrival.getArrivalId()));
                dto.setPurchaseOrderNo(order != null ? order.getOrderCode() : null);
                dto.setBatchNumber(arrival.getArrivalCode());
                dto.setInboundTime(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .format(LocalDateTime.now()));
                dto.setQuantity(actualQty);
                dto.setUnit(item.getUnit());
                dto.setUnitPrice(item.getUnitPrice() != null ? BigDecimal.valueOf(item.getUnitPrice()) : null);
                dto.setWarehouseId(arrival.getWarehouseId() != null ? String.valueOf(arrival.getWarehouseId()) : null);
                dto.setStoreId(arrival.getWarehouseId() != null ? String.valueOf(arrival.getWarehouseId()) : null);
                dto.setGenerateCount(1);
                materialTraceCodeService.generateBatch(dto);
            } catch (Exception e) {
                log.error("生成原料追溯码失败：arrivalId={}, materialId={}, err={}",
                        arrival.getArrivalId(), item.getMaterialId(), e.getMessage());
            }
        }
    }

    /** 发布采购入库事件（成本记录，异步监听） */
    private void publishPurchaseStockInEvent(PurchaseArrival arrival) {
        try {
            PurchaseStockInEvent event = new PurchaseStockInEvent();
            event.setStockinId(arrival.getArrivalId());
            event.setStockinNo(arrival.getArrivalCode());
            event.setSupplierId(arrival.getSupplierId());
            event.setWarehouseId(arrival.getWarehouseId());
            event.setQuantity(arrival.getTotalQuantity());
            if (arrival.getTotalAmount() != null) {
            event.setTotalAmount(BigDecimal.valueOf(arrival.getTotalAmount())
                    .divide(BigDecimal.valueOf(100L), 2, java.math.RoundingMode.HALF_UP));
            }
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("发布采购入库事件失败：arrivalId={}", arrival.getArrivalId(), e);
        }
    }

    /** 查询到货单明细 */
    private List<PurchaseArrivalItem> loadArrivalItems(Long arrivalId) {
        LambdaQueryWrapper<PurchaseArrivalItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseArrivalItem::getArrivalId, arrivalId)
                .eq(PurchaseArrivalItem::getDeleted, 0);
        return arrivalItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseArrival> getOverdueArrivals() {
        int days = getAutoCloseDays();
        LocalDate deadline = LocalDate.now().minusDays(days);
        LambdaQueryWrapper<PurchaseArrival> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(PurchaseArrival::getEstimatedArrivalDate)
                .lt(PurchaseArrival::getEstimatedArrivalDate, deadline)
                .eq(PurchaseArrival::getStatus, STATUS_PENDING)
                .orderByAsc(PurchaseArrival::getEstimatedArrivalDate);
        return arrivalMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCloseOverdueArrivals() {
        List<PurchaseArrival> overdueList = getOverdueArrivals();
        if (overdueList.isEmpty()) {
            return 0;
        }
        for (PurchaseArrival arrival : overdueList) {
            arrival.setStatus(STATUS_CLOSED);
            arrival.setCloseReason(CLOSE_REASON_OVERDUE);
            arrival.setUpdateTime(LocalDateTime.now());
            arrivalMapper.updateById(arrival);
            log.info("超期自动关闭到货单，arrivalId={}，arrivalCode={}", arrival.getArrivalId(), arrival.getArrivalCode());
        }
        return overdueList.size();
    }

    private boolean isOrderAllowArrival(Integer orderStatus) {
        if (orderStatus == null) {
            return false;
        }
        return orderStatus == 2 || orderStatus == 3 || orderStatus == 4 || orderStatus == 6;
    }

    private BigDecimal calculateRemainingQuantity(PurchaseOrderItem orderItem) {
        BigDecimal received = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
        BigDecimal plannedInArrival = getExistingArrivalExpectedQuantity(orderItem.getItemId());
        return orderItem.getQuantity().subtract(received).subtract(plannedInArrival);
    }

    private BigDecimal getExistingArrivalExpectedQuantity(Long orderItemId) {
        LambdaQueryWrapper<PurchaseArrivalItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseArrivalItem::getOrderItemId, orderItemId);
        List<PurchaseArrivalItem> items = arrivalItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 已确认入库(status=3)的到货单不再占用计划量（其实收数量已回写到订单 receivedQuantity）
        List<Long> arrivalIds = items.stream().map(PurchaseArrivalItem::getArrivalId).distinct().toList();
        Set<Long> confirmedIds = arrivalMapper.selectBatchIds(arrivalIds).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == STATUS_RECEIVED)
                .map(PurchaseArrival::getArrivalId)
                .collect(Collectors.toSet());
        return items.stream()
                .filter(i -> !confirmedIds.contains(i.getArrivalId()))
                .map(PurchaseArrivalItem::getExpectedQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseArrival buildAndSaveArrival(PurchaseOrder order, ReceiverKey key,
                                                List<PurchaseOrderItem> orderItems,
                                                PurchaseArrivalCreateDTO createDTO) {
        PurchaseArrival arrival = new PurchaseArrival();
        arrival.setArrivalCode(generateArrivalCode());
        arrival.setOrderId(order.getOrderId());
        arrival.setSupplierId(order.getSupplierId());
        arrival.setSupplierName(order.getSupplierName());
        arrival.setReceiverType(key.receiverType);
        arrival.setStoreId(key.storeId);
        arrival.setWarehouseId(key.warehouseId != null ? key.warehouseId : 1L); // 未指定收货仓库时兜底默认仓库（集中式单店）
        arrival.setShipmentStatus(createDTO.getShipmentStatus());
        arrival.setLogisticsNo(createDTO.getLogisticsNo());
        arrival.setLogisticsCompany(createDTO.getLogisticsCompany());
        arrival.setTransportMode(createDTO.getTransportMode());
        arrival.setVehiclePlateNo(createDTO.getVehiclePlateNo());
        arrival.setVehicleType(createDTO.getVehicleType());
        arrival.setDriverName(createDTO.getDriverName());
        arrival.setDriverPhone(createDTO.getDriverPhone());
        arrival.setFreightAmount(createDTO.getFreightAmount());
        arrival.setEstimatedArrivalDate(createDTO.getEstimatedArrivalDate());
        arrival.setStatus(STATUS_PENDING);
        arrival.setRemark(createDTO.getRemark());
        arrival.setCreateUserId(SecurityUtils.getCurrentUserId());
        arrivalMapper.insert(arrival);

        BigDecimal totalQuantity = BigDecimal.ZERO;
        long totalAmount = 0L;
        for (PurchaseOrderItem orderItem : orderItems) {
            BigDecimal remaining = calculateRemainingQuantity(orderItem);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PurchaseArrivalItem arrivalItem = new PurchaseArrivalItem();
            arrivalItem.setArrivalId(arrival.getArrivalId());
            arrivalItem.setOrderItemId(orderItem.getItemId());
            arrivalItem.setMaterialId(orderItem.getMaterialId());
            arrivalItem.setMaterialName(orderItem.getMaterialName());
            arrivalItem.setSpecification(orderItem.getSpecification());
            arrivalItem.setUnit(orderItem.getUnit());
            arrivalItem.setExpectedQuantity(remaining);
            arrivalItem.setReceivedQuantity(BigDecimal.ZERO);
            arrivalItem.setUnitPrice(orderItem.getUnitPrice());
            long amount = Math.round(remaining.doubleValue() * (orderItem.getUnitPrice() != null ? orderItem.getUnitPrice() : 0L));
            arrivalItem.setAmount(amount);
            arrivalItemMapper.insert(arrivalItem);

            totalQuantity = totalQuantity.add(remaining);
            totalAmount += amount;
        }

        arrival.setTotalQuantity(totalQuantity);
        arrival.setTotalAmount(totalAmount);
        arrivalMapper.updateById(arrival);
        return getArrivalDetail(arrival.getArrivalId());
    }

    private List<Long> findOrderIdsByCode(String orderCode) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(PurchaseOrder::getOrderCode, orderCode);
        return orderMapper.selectList(wrapper).stream()
                .map(PurchaseOrder::getOrderId)
                .collect(Collectors.toList());
    }

    private void enrichDisplayFields(PurchaseArrival arrival) {
        if (arrival == null || arrival.getOrderId() == null) {
            return;
        }
        PurchaseOrder order = orderMapper.selectById(arrival.getOrderId());
        if (order != null) {
            arrival.setOrderCode(order.getOrderCode());
            if (!StringUtils.hasText(arrival.getSupplierName())) {
                arrival.setSupplierName(order.getSupplierName());
            }
        }
    }

    private int getAutoCloseDays() {
        String value = systemConfigService.getConfigValue("arrival.auto_close.days");
        if (!StringUtils.hasText(value)) {
            return 7;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("到货自动关闭天数配置格式错误，使用默认值7：{}", value);
            return 7;
        }
    }

    private String generateArrivalCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "AR" + dateStr;
        LambdaQueryWrapper<PurchaseArrival> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseArrival::getArrivalCode, prefix);
        wrapper.orderByDesc(PurchaseArrival::getArrivalCode);
        wrapper.last("LIMIT 1");
        PurchaseArrival last = arrivalMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getArrivalCode() != null) {
            String seqStr = last.getArrivalCode().substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 收货地点分组键
     */
    private static class ReceiverKey {
        private final String receiverType;
        private final String storeId;
        private final Long warehouseId;

        ReceiverKey(PurchaseOrderItem item) {
            this.receiverType = StringUtils.hasText(item.getPlannedReceiverType())
                    ? item.getPlannedReceiverType() : "STORE";
            this.storeId = item.getPlannedStoreId();
            this.warehouseId = item.getPlannedWarehouseId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReceiverKey)) return false;
            ReceiverKey that = (ReceiverKey) o;
            return Objects.equals(receiverType, that.receiverType)
                    && Objects.equals(storeId, that.storeId)
                    && Objects.equals(warehouseId, that.warehouseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiverType, storeId, warehouseId);
        }
    }
}
