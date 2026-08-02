package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.dto.PurchaseQualityCheckDTO;
import com.foodtraceability.dto.PurchaseStockinCreateDTO;
import com.foodtraceability.dto.PurchaseStockinItemDTO;
import com.foodtraceability.entity.AssetFlowRecord;
import com.foodtraceability.entity.AssetMaster;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.PurchaseRequest;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.event.PurchaseStockInEvent;
import com.foodtraceability.mapper.AssetFlowRecordMapper;
import com.foodtraceability.mapper.AssetMasterMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseRequestMapper;
import com.foodtraceability.mapper.PurchaseStockinItemMapper;
import com.foodtraceability.mapper.PurchaseStockinMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.PurchaseStockinService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.finance.PayableService;
import com.foodtraceability.service.purchase.PurchasePlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 采购入库单服务实现类
 * 管理采购商品的入库验收流程：创建 -> 质检 -> 确认入库
 *
 * 重构说明：
 * - 已移除 RabbitMQ 依赖（RabbitTemplate）
 * - 采购入库/入库确认消息改为日志记录
 * - 保留 Spring ApplicationEvent 事件发布（用于触发追溯码生成等同步事件）
 * - 保留所有业务逻辑和数据库操作
 */
@Service
public class PurchaseStockinServiceImpl extends ServiceImpl<PurchaseStockinMapper, PurchaseStockin> implements PurchaseStockinService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseStockinServiceImpl.class);

    /** 入库状态常量 */
    private static final int STATUS_PENDING = 0;     // 待入库
    private static final int STATUS_STOCKED = 1;     // 已入库
    private static final int STATUS_PARTIAL = 2;      // 部分入库
    private static final int STATUS_VOIDED = 9;       // 已作废

    /** 质检结果常量 */
    private static final int QC_PASSED = 1;          // 合格
    private static final int QC_FAILED = 2;           // 不合格
    private static final int QC_PENDING = 3;          // 待检

    private final PurchaseStockinMapper purchaseStockinMapper;
    private final PurchaseStockinItemMapper purchaseStockinItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SupplierMapper supplierMapper;
    private final InventoryService inventoryService;
    private final StoreInventoryService storeInventoryService;
    private final PayableService payableService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AssetMasterMapper assetMasterMapper;
    private final AssetFlowRecordMapper assetFlowRecordMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final PurchasePlanService purchasePlanService;

    public PurchaseStockinServiceImpl(PurchaseStockinMapper purchaseStockinMapper,
                                      PurchaseStockinItemMapper purchaseStockinItemMapper,
                                      PurchaseOrderMapper purchaseOrderMapper,
                                      PurchaseOrderItemMapper purchaseOrderItemMapper,
                                      SupplierMapper supplierMapper,
                                      InventoryService inventoryService,
                                      @Lazy StoreInventoryService storeInventoryService,
                                      PayableService payableService,
                                      ApplicationEventPublisher applicationEventPublisher,
                                      AssetMasterMapper assetMasterMapper,
                                      AssetFlowRecordMapper assetFlowRecordMapper,
                                      PurchaseRequestMapper purchaseRequestMapper,
                                      @Lazy PurchasePlanService purchasePlanService) {
        this.purchaseStockinMapper = purchaseStockinMapper;
        this.purchaseStockinItemMapper = purchaseStockinItemMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.supplierMapper = supplierMapper;
        this.inventoryService = inventoryService;
        this.storeInventoryService = storeInventoryService;
        this.payableService = payableService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.assetMasterMapper = assetMasterMapper;
        this.assetFlowRecordMapper = assetFlowRecordMapper;
        this.purchaseRequestMapper = purchaseRequestMapper;
        this.purchasePlanService = purchasePlanService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseStockin> getStockinPage(int current, int size, Long orderId, Integer status,
                                                String stockinNo, String orderNo, String supplierName) {
        Page<PurchaseStockin> page = new Page<>(current, size);
        LambdaQueryWrapper<PurchaseStockin> wrapper = new LambdaQueryWrapper<>();

        if (orderId != null) {
            wrapper.eq(PurchaseStockin::getOrderId, orderId);
        }
        if (status != null) {
            wrapper.eq(PurchaseStockin::getStatus, status);
        }
        // 入库单号模糊查询
        if (stockinNo != null && !stockinNo.isBlank()) {
            wrapper.like(PurchaseStockin::getStockinCode, stockinNo);
        }
        // 采购订单号精确匹配（先查订单ID）
        if (orderNo != null && !orderNo.isBlank()) {
            LambdaQueryWrapper<PurchaseOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(PurchaseOrder::getOrderCode, orderNo);
            PurchaseOrder order = purchaseOrderMapper.selectOne(orderWrapper);
            if (order == null) {
                return new Page<>(current, size, 0);
            }
            wrapper.eq(PurchaseStockin::getOrderId, order.getOrderId());
        }
        // 供应商名称模糊匹配（先查供应商ID列表）
        if (supplierName != null && !supplierName.isBlank()) {
            LambdaQueryWrapper<Supplier> supplierWrapper = new LambdaQueryWrapper<>();
            supplierWrapper.like(Supplier::getSupplierName, supplierName);
            List<Supplier> suppliers = supplierMapper.selectList(supplierWrapper);
            if (suppliers.isEmpty()) {
                return new Page<>(current, size, 0);
            }
            List<Long> supplierIds = suppliers.stream()
                    .map(Supplier::getSupplierId)
                    .collect(Collectors.toList());
            wrapper.in(PurchaseStockin::getSupplierId, supplierIds);
        }

        wrapper.orderByDesc(PurchaseStockin::getCreateTime);
        Page<PurchaseStockin> result = purchaseStockinMapper.selectPage(page, wrapper);
        //  enrich orderNo / supplierName for frontend display
        for (PurchaseStockin stockin : result.getRecords()) {
            enrichStockinDisplayFields(stockin);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseStockin getStockinDetail(Long stockinId) {
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "入库单不存在");
        }

        // 查询入库明细
        LambdaQueryWrapper<PurchaseStockinItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseStockinItem::getStockinId, stockinId);
        List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(itemWrapper);
        stockin.setItems(items);

        // enrich orderNo / supplierName for frontend display
        enrichStockinDisplayFields(stockin);

        return stockin;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseStockin createStockin(PurchaseStockinCreateDTO createDTO) {
        // 校验关联的采购订单是否存在且状态正确
        PurchaseOrder order = purchaseOrderMapper.selectById(createDTO.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关联的采购订单不存在");
        }
        // 允许"已审核"(2) 或 "已下单"(6) 状态的订单创建入库单
        if (order.getOrderStatus() != 2 && order.getOrderStatus() != 6) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有已审核或已下单状态的采购订单才能创建入库单，当前状态：" + order.getOrderStatus());
        }

        // 校验明细不为空
        if (createDTO.getItems() == null || createDTO.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "入库明细不能为空");
        }

        // 加载订单明细并校验入库明细与订单明细的一致性
        List<PurchaseOrderItem> orderItems = loadAndValidateOrderItems(createDTO.getOrderId(), createDTO.getItems());

        // 构建入库单实体
        PurchaseStockin stockin = new PurchaseStockin();
        stockin.setStockinCode(generateStockinCode());
        stockin.setOrderId(createDTO.getOrderId());
        stockin.setSupplierId(order.getSupplierId()); // 从订单获取供应商ID
        stockin.setWarehouseId(createDTO.getWarehouseId());
        stockin.setStockinDate(createDTO.getStockinDate() != null ? createDTO.getStockinDate() : LocalDate.now());
        stockin.setStockinType(createDTO.getStockinType() != null ? createDTO.getStockinType() : 1);
        stockin.setTotalQuantity(createDTO.calculateTotalQuantity());
        stockin.setTotalAmount(createDTO.calculateTotalAmount());
        stockin.setQualityCheckResult(QC_PENDING);   // 默认待检
        stockin.setStatus(STATUS_PENDING);             // 待入库
        // 使用qualityRemark字段存储创建时的备注信息（实体类无remark字段）
        stockin.setQualityRemark(createDTO.getRemark());

        purchaseStockinMapper.insert(stockin);
        log.info("创建采购入库单成功，编号：{}", stockin.getStockinCode());

        // 保存入库明细
        saveStockinItems(stockin.getStockinId(), createDTO.getItems());

        // 更新采购订单明细的已收货数量
        updateOrderItemReceivedQuantity(orderItems, createDTO.getItems());

        return getStockinDetail(stockin.getStockinId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseStockin qualityCheck(Long stockinId, PurchaseQualityCheckDTO qualityCheckDTO) {
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "入库单不存在");
        }

        // 状态校验：仅待入库状态可质检
        if (stockin.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有待入库状态的入库单可以执行质检操作");
        }

        // 不合格场景业务校验
        if (qualityCheckDTO.getQualityCheckResult() != null
                && qualityCheckDTO.getQualityCheckResult() == QC_FAILED) {
            if (qualityCheckDTO.getDisposalOpinion() == null || qualityCheckDTO.getDisposalOpinion().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "质检不合格时必须选择处理意见");
            }
            boolean hasReason = qualityCheckDTO.getQualityRemark() != null
                    && !qualityCheckDTO.getQualityRemark().isBlank();
            boolean hasType = qualityCheckDTO.getUnqualifiedType() != null
                    && !qualityCheckDTO.getUnqualifiedType().isBlank();
            if (!hasReason && !hasType) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "不合格原因与不合格类型至少填写一项");
            }
        }

        // 更新质检信息
        stockin.setQualityCheckResult(qualityCheckDTO.getQualityCheckResult());
        stockin.setQualityRemark(qualityCheckDTO.getQualityRemark());
        stockin.setQualityCheckTime(LocalDateTime.now());

        // 更新扩展质检字段
        stockin.setAppearanceResult(qualityCheckDTO.getAppearanceResult());
        stockin.setOdorResult(qualityCheckDTO.getOdorResult());
        stockin.setTemperature(qualityCheckDTO.getTemperature());
        stockin.setHumidity(qualityCheckDTO.getHumidity());
        stockin.setSampleQuantity(qualityCheckDTO.getSampleQuantity());
        stockin.setSampleRate(qualityCheckDTO.getSampleRate());
        stockin.setUnqualifiedType(qualityCheckDTO.getUnqualifiedType());
        stockin.setDisposalOpinion(qualityCheckDTO.getDisposalOpinion());
        stockin.setDocumentCheck(qualityCheckDTO.getDocumentCheck());

        // 如果不合格，更新状态为待入库（等待处理）
        // 如果合格或待检，保持待入库状态等待确认入库
        purchaseStockinMapper.updateById(stockin);

        log.info("入库单质检完成，编号：{}，结果：{}", stockin.getStockinCode(),
            qualityCheckDTO.getQualityCheckResult() == 1 ? "合格" :
            qualityCheckDTO.getQualityCheckResult() == 2 ? "不合格" : "待检");

        return stockin;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseStockin confirmStockin(Long stockinId) {
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "入库单不存在");
        }

        // 状态校验：仅待入库状态可确认入库
        if (stockin.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有待入库状态的入库单可以确认入库");
        }

        // 校验必须先完成质检
        if (stockin.getQualityCheckResult() == null || stockin.getQualityCheckResult() == QC_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "请先完成质检操作后再确认入库");
        }

        // 不合格的不允许入库
        if (stockin.getQualityCheckResult() == QC_FAILED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "质检不合格的入库单不允许入库，请处理后重新提交");
        }

        // 更新状态为已入库
        stockin.setStatus(STATUS_STOCKED);
        purchaseStockinMapper.updateById(stockin);

        log.info("确认入库成功，入库单编号：{}", stockin.getStockinCode());

        // 逐项增加库存
        increaseInventoryForStockin(stockin);

        // T-038: 创建应付账款（F-009 联动，同事务强一致性）
        // 应付创建失败则整个收货事务回滚；幂等性由 PayableService.createForStockin 内部保证
        createPayableForStockin(stockin);

        // 重构说明：已移除 RabbitMQ，采购入库完成消息改为日志记录
        log.info("采购入库完成：stockinId={}, stockinCode={}, orderId={}, warehouseId={}, supplierId={}, status={}",
                stockin.getStockinId(), stockin.getStockinCode(), stockin.getOrderId(),
                stockin.getWarehouseId(), stockin.getSupplierId(), stockin.getStatus());

        // 重构说明：入库确认回执消息改为日志记录
        log.info("入库确认回执：stockinId={}, stockinCode={}, orderId={}, confirmTime={}",
                stockin.getStockinId(), stockin.getStockinCode(), stockin.getOrderId(), LocalDateTime.now());

        updateOrderStockStatus(stockin.getOrderId());

        // LK-PURCHASE-04: 采购订单全部入库后回写采购计划状态为已完成
        triggerPlanStatusCheckIfNeeded(stockin.getOrderId());

        // T-007: 资产采购联动，当来源为资产采购申请时自动生成资产卡片
        createAssetsForStockin(stockin);

        // 发布采购入库完成事件（同步事件，触发追溯码生成与采购成本记录）
        // 监听器：PurchaseStockInEventListener（@EventListener + @Async）
        publishPurchaseStockInEvent(stockin, null);

        return stockin;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStockin(Long stockinId) {
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "入库单不存在");
        }
        if (stockin.getStatus() != null && stockin.getStatus() == STATUS_STOCKED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "已入库的入库单不允许删除");
        }

        // 查询入库明细并还原订单明细已收货数量
        LambdaQueryWrapper<PurchaseStockinItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseStockinItem::getStockinId, stockinId);
        List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(itemWrapper);

        for (PurchaseStockinItem item : items) {
            if (item.getOrderItemId() == null || item.getActualQuantity() == null) {
                continue;
            }
            PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectById(item.getOrderItemId());
            if (orderItem == null) {
                continue;
            }
            BigDecimal received = orderItem.getReceivedQuantity() != null
                    ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
            BigDecimal restored = received.subtract(item.getActualQuantity());
            orderItem.setReceivedQuantity(restored.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : restored);
            purchaseOrderItemMapper.updateById(orderItem);
            log.info("删除入库单还原订单明细已收货数量，itemId={}，还原={}，剩余={}",
                    orderItem.getItemId(), item.getActualQuantity(), orderItem.getReceivedQuantity());
        }

        boolean success = purchaseStockinMapper.deleteById(stockinId) > 0;
        if (success) {
            // 物理删除入库明细
            purchaseStockinItemMapper.delete(itemWrapper);
            log.info("删除采购入库单成功，ID：{}", stockinId);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseStockin voidStockin(Long stockinId, String remark, Long voidBy) {
        log.info("作废入库单，ID：{}，备注：{}", stockinId, remark);

        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "入库单不存在");
        }
        if (stockin.getStatus() == null || stockin.getStatus() != STATUS_STOCKED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "仅已入库状态的入库单可作废");
        }
        if (stockin.getVoidTime() != null) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "该入库单已作废，不可重复操作");
        }

        // 查询入库明细
        LambdaQueryWrapper<PurchaseStockinItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseStockinItem::getStockinId, stockinId);
        List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(itemWrapper);
        stockin.setItems(items);

        // 1. 作废关联的未付款应付账款（已付款则抛异常，需先作废付款单）
        payableService.voidPayableByStockinId(stockinId);

        // 2. 回滚库存
        decreaseInventoryForVoidStockin(stockin);

        // 3. 回滚采购订单已收货数量
        restoreOrderItemReceivedQuantity(items);

        // 4. 更新入库单状态为已作废
        stockin.setStatus(STATUS_VOIDED);
        stockin.setVoidTime(LocalDateTime.now());
        stockin.setVoidRemark(remark);
        stockin.setVoidBy(voidBy);
        purchaseStockinMapper.updateById(stockin);

        // 5. 重新计算采购订单入库状态
        updateOrderStockStatus(stockin.getOrderId());

        log.info("入库单作废完成，ID：{}，编号：{}", stockinId, stockin.getStockinCode());
        return getStockinDetail(stockinId);
    }

    /**
     * 发布采购入库完成事件
     * 触发 PurchaseStockInEventListener 联动生成原料追溯码和记录采购成本
     * 异常隔离，发布失败不影响主事务
     */
    private void publishPurchaseStockInEvent(PurchaseStockin stockin, PurchaseOrder order) {
        try {
            PurchaseStockInEvent event = new PurchaseStockInEvent();
            event.setStockinId(stockin.getStockinId());
            event.setStockinNo(stockin.getStockinCode());
            event.setSupplierId(stockin.getSupplierId());
            event.setWarehouseId(stockin.getWarehouseId());
            event.setQuantity(stockin.getTotalQuantity());
            // 金额：库存表存分，事件语义为元（BigDecimal）
            if (stockin.getTotalAmount() != null) {
                event.setTotalAmount(BigDecimal.valueOf(stockin.getTotalAmount())
                        .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP));
            }
            event.setStockinTime(LocalDateTime.now());
            if (order != null) {
                event.setOrderNo(order.getOrderCode());
            }
            applicationEventPublisher.publishEvent(event);
            log.info("已发布采购入库完成事件: stockinId={}", stockin.getStockinId());
        } catch (Exception e) {
            log.error("发布采购入库完成事件失败: stockinId={}, 错误={}",
                    stockin.getStockinId(), e.getMessage(), e);
        }
    }

    /**
     * 为入库单填充前端展示字段（orderNo / supplierName）
     * 非数据库字段，查询时动态关联
     */
    private void enrichStockinDisplayFields(PurchaseStockin stockin) {
        if (stockin == null) {
            return;
        }
        // 关联采购订单编号
        if (stockin.getOrderId() != null) {
            try {
                PurchaseOrder order = purchaseOrderMapper.selectById(stockin.getOrderId());
                if (order != null) {
                    stockin.setOrderNo(order.getOrderCode());
                }
            } catch (Exception e) {
                log.warn("查询关联采购订单失败，orderId={}，错误：{}", stockin.getOrderId(), e.getMessage());
            }
        }
        // 关联供应商名称
        if (stockin.getSupplierId() != null) {
            try {
                com.foodtraceability.entity.Supplier supplier = supplierMapper.selectById(stockin.getSupplierId());
                if (supplier != null) {
                    stockin.setSupplierName(supplier.getSupplierName());
                }
            } catch (Exception e) {
                log.warn("查询供应商失败，supplierId={}，错误：{}", stockin.getSupplierId(), e.getMessage());
            }
        }
    }

    /**
     * 生成入库单编号
     * 格式：SI + 年月日 + 4位序号
     */
    private String generateStockinCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SI" + dateStr;

        LambdaQueryWrapper<PurchaseStockin> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseStockin::getStockinCode, prefix);
        wrapper.orderByDesc(PurchaseStockin::getStockinCode);
        wrapper.last("LIMIT 1");
        PurchaseStockin lastStockin = purchaseStockinMapper.selectOne(wrapper);

        int seq = 1;
        if (lastStockin != null && lastStockin.getStockinCode() != null) {
            String lastCode = lastStockin.getStockinCode();
            String seqStr = lastCode.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%04d", seq);
    }

    /**
     * 保存入库明细列表
     */
    private void saveStockinItems(Long stockinId, List<PurchaseStockinItemDTO> itemDTOs) {
        for (PurchaseStockinItemDTO dto : itemDTOs) {
            PurchaseStockinItem item = new PurchaseStockinItem();
            item.setStockinId(stockinId);
            item.setOrderItemId(dto.getOrderItemId());
            item.setMaterialId(dto.getMaterialId());
            item.setMaterialName(dto.getMaterialName());
            item.setBatchNo(dto.getBatchNo());
            item.setProductionDate(dto.getProductionDate());
            item.setExpiryDate(dto.getExpiryDate());
            item.setActualQuantity(dto.getActualQuantity());
            item.setUnit(dto.getUnit());
            item.setUnitPrice(dto.getUnitPrice());
            item.setAmount(dto.calculateAmount());
            item.setLocationId(dto.getLocationId());
            item.setRemark(dto.getRemark());
            purchaseStockinItemMapper.insert(item);
        }
    }

    /**
     * 加载订单明细并校验入库明细与订单明细的一致性
     * 校验规则：
     * 1. 订单明细ID必须存在且属于当前订单
     * 2. 物料ID必须与订单明细一致（防止前端篡改物料）
     * 3. 物料名称必须与订单明细一致
     * 4. 入库数量不能超过订单剩余可收数量
     * 5. 单位与单价使用订单明细的值覆盖前端传入值
     */
    private List<PurchaseOrderItem> loadAndValidateOrderItems(Long orderId, List<PurchaseStockinItemDTO> itemDTOs) {
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectList(wrapper);

        for (PurchaseStockinItemDTO dto : itemDTOs) {
            PurchaseOrderItem orderItem = orderItems.stream()
                    .filter(oi -> oi.getItemId().equals(dto.getOrderItemId()))
                    .findFirst()
                    .orElse(null);

            if (orderItem == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "订单明细ID不存在或不属于当前采购订单：" + dto.getOrderItemId());
            }

            // 校验物料ID一致性
            if (dto.getMaterialId() != null && !dto.getMaterialId().equals(orderItem.getMaterialId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "入库物料与订单明细不匹配，物料ID：" + dto.getMaterialId());
            }

            // 校验物料名称一致性
            if (dto.getMaterialName() != null && !dto.getMaterialName().equals(orderItem.getMaterialName())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "入库物料名称与订单明细不匹配：" + dto.getMaterialName());
            }

            // 校验入库数量不能超过订单剩余可收数量
            BigDecimal received = orderItem.getReceivedQuantity() != null
                    ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
            BigDecimal remaining = orderItem.getQuantity().subtract(received);
            if (dto.getActualQuantity() == null || dto.getActualQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "物料「" + orderItem.getMaterialName() + "」入库数量必须大于0");
            }
            if (dto.getActualQuantity().compareTo(remaining) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "物料「" + orderItem.getMaterialName() + "」入库数量超过订单剩余可收数量，剩余：" + remaining);
            }

            // 使用订单明细的单位与单价覆盖前端值，防止篡改
            dto.setMaterialId(orderItem.getMaterialId());
            dto.setMaterialName(orderItem.getMaterialName());
            dto.setUnit(orderItem.getUnit());
            dto.setUnitPrice(orderItem.getUnitPrice());
        }

        return orderItems;
    }

    /**
     * 更新采购订单明细的已收货数量
     */
    private void updateOrderItemReceivedQuantity(List<PurchaseOrderItem> orderItems,
                                                  List<PurchaseStockinItemDTO> itemDTOs) {
        for (PurchaseStockinItemDTO dto : itemDTOs) {
            PurchaseOrderItem orderItem = orderItems.stream()
                    .filter(oi -> oi.getItemId().equals(dto.getOrderItemId()))
                    .findFirst()
                    .orElse(null);
            if (orderItem == null) {
                continue;
            }
            BigDecimal received = orderItem.getReceivedQuantity() != null
                    ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
            orderItem.setReceivedQuantity(received.add(dto.getActualQuantity()));
            purchaseOrderItemMapper.updateById(orderItem);
            log.info("更新订单明细已收货数量，itemId={}，新增={}，累计={}",
                    orderItem.getItemId(), dto.getActualQuantity(), orderItem.getReceivedQuantity());
        }
    }

    /**
     * 更新采购订单的入库状态
     * 根据订单明细的累计已收货数量与订单数量比较判断：
     * - 已收货数量 >= 订单数量：已完成（4）
     * - 0 < 已收货数量 < 订单数量：部分入库（3）
     */
    private void updateOrderStockStatus(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null || (order.getOrderStatus() != 2 && order.getOrderStatus() != 6)) {
            return;
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectList(itemWrapper);

        if (orderItems.isEmpty()) {
            return;
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        for (PurchaseOrderItem item : orderItems) {
            totalQuantity = totalQuantity.add(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
            totalReceived = totalReceived.add(item.getReceivedQuantity() != null ? item.getReceivedQuantity() : BigDecimal.ZERO);
        }

        int newStatus;
        String statusDesc;
        if (totalReceived.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        } else if (totalReceived.compareTo(totalQuantity) >= 0) {
            newStatus = 4; // 已完成
            statusDesc = "已完成";
        } else {
            newStatus = 3; // 部分入库
            statusDesc = "部分入库";
        }

        order.setOrderStatus(newStatus);
        purchaseOrderMapper.updateById(order);
        log.info("采购订单入库状态更新，订单ID：{}，累计收货={}，订单数量={}，状态更新为{}",
                orderId, totalReceived, totalQuantity, statusDesc);
    }

    /**
     * LK-PURCHASE-04: 订单入库状态更新后，检查并回写关联采购计划的状态
     *
     * <p>异常隔离：失败仅记录日志，不影响主入库事务（参考 publishPurchaseStockInEvent 模式）。</p>
     */
    private void triggerPlanStatusCheckIfNeeded(Long orderId) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
            if (order == null || order.getPlanId() == null) {
                return;
            }
            purchasePlanService.markPlanCompletedIfNeeded(order.getPlanId());
        } catch (Exception e) {
            log.error("采购计划状态回写失败，orderId={}, 错误={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 确认入库后逐项增加库存
     * 遍历入库明细，为每个物料调用InventoryService增加库存
     * 同时同步门店库存（StoreInventoryService），保证门店库存维度一致
     */
    private void increaseInventoryForStockin(PurchaseStockin stockin) {
        // 查询入库明细
        LambdaQueryWrapper<PurchaseStockinItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseStockinItem::getStockinId, stockin.getStockinId());
        List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(itemWrapper);

        // 门店ID：PurchaseStockin无storeId字段，使用warehouseId作为门店ID
        String storeIdForSync = stockin.getWarehouseId() != null
                ? String.valueOf(stockin.getWarehouseId()) : null;

        for (PurchaseStockinItem item : items) {
            try {
                InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
                increaseDTO.setMaterialId(item.getMaterialId());
                increaseDTO.setWarehouseId(stockin.getWarehouseId());
                increaseDTO.setLocationId(item.getLocationId());
                increaseDTO.setQuantity(item.getActualQuantity());
                increaseDTO.setUnitCost(item.getUnitPrice());
                increaseDTO.setTransactionType(1); // 采购入库
                increaseDTO.setBatchNo(item.getBatchNo());
                increaseDTO.setReferenceNo(stockin.getStockinCode());
                increaseDTO.setReferenceType("purchase_stockin");

                inventoryService.increaseInventory(increaseDTO);
                log.debug("入库明细库存增加成功：物料ID={}，数量={}", item.getMaterialId(), item.getActualQuantity());

                // 同步门店库存（独立门店库存维度）
                if (storeIdForSync != null && item.getActualQuantity() != null) {
                    try {
                        storeInventoryService.increaseStock(
                                storeIdForSync,
                                item.getMaterialId(),
                                item.getMaterialName(),
                                item.getActualQuantity(),
                                item.getUnit(),
                                item.getUnitPrice() != null ? item.getUnitPrice().longValue() : null,
                                1,
                                "采购入库 - 入库单:" + stockin.getStockinCode());
                        log.debug("门店库存同步成功：storeId={}, 物料ID={}, 数量={}, 单位成本={}分",
                                storeIdForSync, item.getMaterialId(), item.getActualQuantity(), item.getUnitPrice());
                    } catch (Exception syncEx) {
                        // DF-001 修复：门店库存同步失败必须回滚整个入库事务，避免库存数据不一致
                        // （采购入库表显示已入库但门店库存表未增加）。库存一致性是 ERP 核心约束。
                        log.error("门店库存同步失败，事务将回滚：storeId={}, 物料ID={}, 错误={}",
                                storeIdForSync, item.getMaterialId(), syncEx.getMessage(), syncEx);
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "门店库存同步失败：物料" + item.getMaterialName() + " - " + syncEx.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("入库明细库存增加失败：物料ID={}，错误：{}", item.getMaterialId(), e.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "库存增加失败：物料" + item.getMaterialName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 作废入库单时回滚库存
     */
    private void decreaseInventoryForVoidStockin(PurchaseStockin stockin) {
        String storeIdForSync = stockin.getWarehouseId() != null
                ? String.valueOf(stockin.getWarehouseId()) : null;

        for (PurchaseStockinItem item : stockin.getItems()) {
            try {
                InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
                decreaseDTO.setMaterialId(item.getMaterialId());
                decreaseDTO.setWarehouseId(stockin.getWarehouseId());
                decreaseDTO.setQuantity(item.getActualQuantity());
                decreaseDTO.setTransactionType(6); // 其他出库：入库作废
                decreaseDTO.setReferenceNo(stockin.getStockinCode());
                decreaseDTO.setReferenceType("purchase_stockin_void");
                decreaseDTO.setRemark("入库单作废回滚库存");

                inventoryService.decreaseInventory(decreaseDTO);
                log.debug("入库作废库存扣减成功：物料ID={}，数量={}", item.getMaterialId(), item.getActualQuantity());

                if (storeIdForSync != null && item.getActualQuantity() != null) {
                    try {
                        storeInventoryService.decreaseStock(storeIdForSync, item.getMaterialId(), item.getActualQuantity(),
                                2, "采购入库作废回滚 - 入库单:" + stockin.getStockinCode());
                        log.debug("门店库存作废回滚成功：storeId={}, 物料ID={}, 数量={}",
                                storeIdForSync, item.getMaterialId(), item.getActualQuantity());
                    } catch (Exception syncEx) {
                        log.error("门店库存作废回滚失败，事务将回滚：storeId={}, 物料ID={}, 错误={}",
                                storeIdForSync, item.getMaterialId(), syncEx.getMessage(), syncEx);
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "门店库存回滚失败：物料" + item.getMaterialName() + " - " + syncEx.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("入库作废库存扣减失败：物料ID={}，错误：{}", item.getMaterialId(), e.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "库存回滚失败：物料" + item.getMaterialName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 作废入库单时回滚采购订单明细已收货数量
     */
    private void restoreOrderItemReceivedQuantity(List<PurchaseStockinItem> items) {
        for (PurchaseStockinItem item : items) {
            if (item.getOrderItemId() == null || item.getActualQuantity() == null) {
                continue;
            }
            PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectById(item.getOrderItemId());
            if (orderItem == null) {
                continue;
            }
            BigDecimal received = orderItem.getReceivedQuantity() != null
                    ? orderItem.getReceivedQuantity() : BigDecimal.ZERO;
            BigDecimal restored = received.subtract(item.getActualQuantity());
            orderItem.setReceivedQuantity(restored.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : restored);
            purchaseOrderItemMapper.updateById(orderItem);
            log.info("作废入库单回滚订单明细已收货数量，itemId={}，回滚={}，剩余={}",
                    orderItem.getItemId(), item.getActualQuantity(), orderItem.getReceivedQuantity());
        }
    }

    /**
     * 创建采购入库应付账款（T-038 / F-009 联动）
     *
     * <p>同事务调用 PayableService.createForStockin 生成应付账款记录。
     * 应付创建失败将导致整个 confirmStockin 事务回滚（强一致性）。
     * 幂等性由 PayableService 通过确定性 payableNo 保证。</p>
     *
     * @param stockin 采购入库单
     */
    private void createPayableForStockin(PurchaseStockin stockin) {
        if (stockin.getTotalAmount() == null || stockin.getTotalAmount() <= 0L) {
            log.warn("入库单金额无效，跳过应付账款创建，入库单ID：{}，金额：{}",
                    stockin.getStockinId(), stockin.getTotalAmount());
            return;
        }

        // 优先使用供应商档案中的真实名称，便于财务模块按供应商名称查询与对账
        String supplierName = null;
        if (stockin.getSupplierId() != null) {
            try {
                com.foodtraceability.entity.Supplier supplier = supplierMapper.selectById(stockin.getSupplierId());
                if (supplier != null) {
                    supplierName = supplier.getSupplierName();
                }
            } catch (Exception e) {
                log.warn("查询供应商名称失败，supplierId={}，错误：{}", stockin.getSupplierId(), e.getMessage());
            }
        }
        if (supplierName == null || supplierName.isBlank()) {
            supplierName = "供应商" + stockin.getSupplierId();
        }

        // 加载关联采购订单以获取订单编号
        PurchaseOrder order = null;
        if (stockin.getOrderId() != null) {
            order = purchaseOrderMapper.selectById(stockin.getOrderId());
        }

        try {
            payableService.createForStockin(
                    stockin.getStockinId(),
                    stockin.getStockinCode(),
                    stockin.getSupplierId(),
                    supplierName,
                    stockin.getOrderId(),
                    order != null ? order.getOrderCode() : null,
                    stockin.getTotalAmount(),
                    stockin.getStockinDate()
            );
            log.info("采购入库应付账款创建完成，入库单ID：{}", stockin.getStockinId());
        } catch (Exception e) {
            log.error("创建采购入库应付账款失败，入库单ID：{}，错误：{}",
                    stockin.getStockinId(), e.getMessage(), e);
            // 应付创建失败回滚整个收货事务（强一致性，与"事件隔离"不同）
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "创建应付账款失败：" + e.getMessage());
        }
    }

    /**
     * 资产采购联动：当采购订单来源于资产采购申请时，自动生成资产卡片
     *
     * <p>触发条件：
     * <ul>
     *   <li>采购订单存在来源采购申请（request_id 不为空）</li>
     *   <li>来源采购申请的 request_type = 'asset'</li>
     * </ul>
     * 每个入库明细生成一条资产卡片，并记录资产流水（flow_type=1 购置）。</p>
     *
     * @param stockin 已确认的采购入库单
     */
    private void createAssetsForStockin(PurchaseStockin stockin) {
        if (stockin == null || stockin.getOrderId() == null) {
            return;
        }

        PurchaseOrder order = purchaseOrderMapper.selectById(stockin.getOrderId());
        if (order == null || !StringUtils.hasText(order.getRequestId())) {
            return;
        }

        PurchaseRequest request = purchaseRequestMapper.selectById(order.getRequestId());
        if (request == null || !"asset".equals(request.getRequestType())) {
            return;
        }

        LambdaQueryWrapper<PurchaseStockinItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseStockinItem::getStockinId, stockin.getStockinId());
        List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(itemWrapper);
        if (items.isEmpty()) {
            return;
        }

        Supplier supplier = null;
        if (stockin.getSupplierId() != null) {
            supplier = supplierMapper.selectById(stockin.getSupplierId());
        }

        for (PurchaseStockinItem item : items) {
            try {
                AssetMaster asset = new AssetMaster();
                asset.setAssetCode(generateAssetCode());
                asset.setAssetName(item.getMaterialName());
                // 入库明细无规格字段，规格从物料档案补充或留空
                asset.setSpecification(null);
                asset.setPurchaseOrderId(order.getOrderId());
                asset.setPurchaseOrderNo(order.getOrderCode());
                asset.setPurchaseDate(stockin.getStockinDate());
                asset.setSupplierId(stockin.getSupplierId());
                asset.setSupplierName(supplier != null ? supplier.getSupplierName() : null);
                asset.setRequestId(request.getRequestId());
                asset.setRequestNo(request.getRequestNo());
                asset.setDepartmentId(parseLongOrNull(request.getDepartmentId()));
                asset.setDepartmentName(request.getDepartmentName());
                asset.setStoreId(stockin.getWarehouseId());
                asset.setLocation(stockin.getWarehouseId() != null ? "仓库" + stockin.getWarehouseId() : null);

                BigDecimal amountYuan = item.getAmount() != null
                        ? BigDecimal.valueOf(item.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                asset.setOriginalValue(amountYuan);
                asset.setNetValue(amountYuan);
                asset.setAccumulatedDepreciation(BigDecimal.ZERO);
                asset.setStatus("idle");
                asset.setDepreciationMethod(1); // 直线法
                asset.setUsefulLifeMonths(36);  // 默认36个月
                asset.setQrCode(generateAssetQrCode(asset.getAssetCode()));

                assetMasterMapper.insert(asset);
                log.info("资产采购联动生成资产卡片成功: assetCode={}, assetId={}, stockinCode={}",
                        asset.getAssetCode(), asset.getId(), stockin.getStockinCode());

                // 记录资产购置流水
                AssetFlowRecord flow = new AssetFlowRecord();
                flow.setAssetId(asset.getId());
                flow.setAssetCode(asset.getAssetCode());
                flow.setAssetName(asset.getAssetName());
                flow.setFlowType("purchase");
                flow.setRelatedOrderId(String.valueOf(order.getOrderId()));
                flow.setQuantity(item.getActualQuantity() != null ? item.getActualQuantity().intValue() : 1);
                flow.setRemark("采购入库自动生成资产卡片，入库单号：" + stockin.getStockinCode());
                flow.setFlowTime(LocalDateTime.now());
                assetFlowRecordMapper.insert(flow);
                log.info("资产购置流水记录成功: assetCode={}, stockinCode={}",
                        asset.getAssetCode(), stockin.getStockinCode());
            } catch (Exception e) {
                log.error("资产采购联动生成资产卡片失败，stockinId={}, itemId={}, 错误={}",
                        stockin.getStockinId(), item.getStockinItemId(), e.getMessage(), e);
                // 资产卡片生成失败回滚整个入库事务，避免采购与资产数据不一致
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "资产卡片生成失败：" + e.getMessage());
            }
        }
    }

    /**
     * 生成资产编码
     * 格式：ASSET + yyyyMMdd + 4位序号
     */
    private String generateAssetCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ASSET" + dateStr;

        LambdaQueryWrapper<AssetMaster> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(AssetMaster::getAssetCode, prefix)
                .orderByDesc(AssetMaster::getAssetCode)
                .last("LIMIT 1");
        AssetMaster lastAsset = assetMasterMapper.selectOne(wrapper);

        int seq = 1;
        if (lastAsset != null && lastAsset.getAssetCode() != null) {
            String seqStr = lastAsset.getAssetCode().substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 生成资产二维码内容
     */
    private String generateAssetQrCode(String assetCode) {
        return "ASSET-QR-" + assetCode;
    }

    /**
     * 安全解析字符串为 Long，解析失败返回 null
     */
    private Long parseLongOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
