package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.ProcurementTraceAssetVO;
import com.foodtraceability.dto.ProcurementTraceInfoVO;
import com.foodtraceability.dto.ProcurementTraceMaterialItemVO;
import com.foodtraceability.dto.ProcurementTraceNodeVO;
import com.foodtraceability.dto.ProcurementTraceQueryDTO;
import com.foodtraceability.entity.AssetMaster;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.PurchaseRequest;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.AssetMasterMapper;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseRequestMapper;
import com.foodtraceability.mapper.PurchaseStockinItemMapper;
import com.foodtraceability.mapper.PurchaseStockinMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.ProcurementTraceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采购溯源服务实现类
 * 聚合采购申请、订单、入库、库存、资产等全链路数据
 */
@Service
public class ProcurementTraceServiceImpl implements ProcurementTraceService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final PurchaseStockinMapper purchaseStockinMapper;
    private final PurchaseStockinItemMapper purchaseStockinItemMapper;
    private final InventoryMapper inventoryMapper;
    private final AssetMasterMapper assetMasterMapper;
    private final WarehouseMapper warehouseMapper;
    private final SupplierMapper supplierMapper;

    public ProcurementTraceServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                       PurchaseOrderItemMapper purchaseOrderItemMapper,
                                       PurchaseRequestMapper purchaseRequestMapper,
                                       PurchaseStockinMapper purchaseStockinMapper,
                                       PurchaseStockinItemMapper purchaseStockinItemMapper,
                                       InventoryMapper inventoryMapper,
                                       AssetMasterMapper assetMasterMapper,
                                       WarehouseMapper warehouseMapper,
                                       SupplierMapper supplierMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.purchaseRequestMapper = purchaseRequestMapper;
        this.purchaseStockinMapper = purchaseStockinMapper;
        this.purchaseStockinItemMapper = purchaseStockinItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.assetMasterMapper = assetMasterMapper;
        this.warehouseMapper = warehouseMapper;
        this.supplierMapper = supplierMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcurementTraceInfoVO> getTraceInfoByMaterialId(Long materialId) {
        if (materialId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getMaterialId, materialId);
        List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectList(itemWrapper);

        if (CollectionUtils.isEmpty(orderItems)) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orderItems.stream()
                .map(PurchaseOrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return orderIds.stream()
                .map(this::buildTraceInfoByOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProcurementTraceInfoVO getTraceInfoByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }

        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getOrderNo, orderNo)
                .or()
                .eq(PurchaseOrder::getOrderCode, orderNo);
        PurchaseOrder order = purchaseOrderMapper.selectOne(wrapper);

        if (order == null) {
            return null;
        }

        return buildTraceInfoByOrderId(order.getOrderId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcurementTraceInfoVO> getTraceInfoByRequestNo(String requestNo) {
        if (!StringUtils.hasText(requestNo)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<PurchaseRequest> requestWrapper = new LambdaQueryWrapper<>();
        requestWrapper.eq(PurchaseRequest::getRequestNo, requestNo);
        List<PurchaseRequest> requests = purchaseRequestMapper.selectList(requestWrapper);

        if (CollectionUtils.isEmpty(requests)) {
            return Collections.emptyList();
        }

        List<String> requestIds = requests.stream()
                .map(PurchaseRequest::getRequestId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<PurchaseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(PurchaseOrder::getRequestId, requestIds);
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(orderWrapper);

        return orders.stream()
                .map(order -> buildTraceInfoByOrderId(order.getOrderId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProcurementTraceInfoVO getTraceInfoByStockinCode(String stockinCode) {
        if (!StringUtils.hasText(stockinCode)) {
            return null;
        }

        LambdaQueryWrapper<PurchaseStockin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseStockin::getStockinCode, stockinCode);
        PurchaseStockin stockin = purchaseStockinMapper.selectOne(wrapper);

        if (stockin == null || stockin.getOrderId() == null) {
            return null;
        }

        return buildTraceInfoByOrderId(stockin.getOrderId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcurementTraceInfoVO> getTraceInfoByBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<PurchaseStockinItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseStockinItem::getBatchNo, batchNo);
        List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(itemWrapper);

        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }

        List<Long> orderIds = items.stream()
                .map(PurchaseStockinItem::getStockinId)
                .distinct()
                .map(stockinId -> {
                    PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
                    return stockin != null ? stockin.getOrderId() : null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return orderIds.stream()
                .map(this::buildTraceInfoByOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcurementTraceInfoVO> searchTraceInfo(ProcurementTraceQueryDTO queryDTO) {
        if (queryDTO == null) {
            return Collections.emptyList();
        }

        if (queryDTO.getOrderNo() != null) {
            ProcurementTraceInfoVO info = getTraceInfoByOrderNo(queryDTO.getOrderNo());
            return info != null ? Collections.singletonList(info) : Collections.emptyList();
        }

        if (queryDTO.getRequestNo() != null) {
            return getTraceInfoByRequestNo(queryDTO.getRequestNo());
        }

        if (queryDTO.getStockinCode() != null) {
            ProcurementTraceInfoVO info = getTraceInfoByStockinCode(queryDTO.getStockinCode());
            return info != null ? Collections.singletonList(info) : Collections.emptyList();
        }

        if (queryDTO.getBatchNo() != null) {
            return getTraceInfoByBatchNo(queryDTO.getBatchNo());
        }

        if (queryDTO.getMaterialId() != null) {
            return getTraceInfoByMaterialId(queryDTO.getMaterialId());
        }

        return Collections.emptyList();
    }

    /**
     * 根据采购订单ID构建完整的采购溯源信息
     */
    private ProcurementTraceInfoVO buildTraceInfoByOrderId(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }

        ProcurementTraceInfoVO traceInfo = new ProcurementTraceInfoVO();
        fillOrderInfo(traceInfo, order);
        fillRequestInfo(traceInfo, order.getRequestId());

        List<PurchaseOrderItem> orderItems = getOrderItems(orderId);
        List<PurchaseStockin> stockins = getStockinsByOrderId(orderId);
        List<PurchaseStockinItem> stockinItems = getStockinItemsByStockins(stockins);

        fillStockinInfo(traceInfo, stockins, stockinItems);
        fillMaterialItems(traceInfo, orderItems, stockinItems);
        fillInventoryInfo(traceInfo, orderItems, stockinItems);
        fillAssetInfo(traceInfo, orderId);
        fillTraceNodes(traceInfo, order, stockins);

        return traceInfo;
    }

    private void fillOrderInfo(ProcurementTraceInfoVO traceInfo, PurchaseOrder order) {
        traceInfo.setOrderId(order.getOrderId());
        traceInfo.setOrderNo(order.getOrderNo());
        traceInfo.setOrderDate(order.getOrderDate());
        traceInfo.setOrderStatus(String.valueOf(order.getOrderStatus()));
        traceInfo.setOrderStatusText(convertOrderStatusToText(order.getOrderStatus()));
        traceInfo.setSupplierId(order.getSupplierId());
        traceInfo.setSupplierName(order.getSupplierName());
        traceInfo.setWarehouseId(order.getWarehouseId());

        if (order.getTotalAmount() != null) {
            traceInfo.setOrderAmount(BigDecimal.valueOf(order.getTotalAmount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        if (order.getSupplierId() != null && !StringUtils.hasText(order.getSupplierName())) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null) {
                traceInfo.setSupplierName(supplier.getSupplierName());
            }
        }

        if (order.getWarehouseId() != null) {
            Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
            if (warehouse != null) {
                traceInfo.setWarehouseName(warehouse.getWarehouseName());
            }
        }
    }

    private void fillRequestInfo(ProcurementTraceInfoVO traceInfo, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return;
        }

        PurchaseRequest request = purchaseRequestMapper.selectById(requestId);
        if (request == null) {
            return;
        }

        traceInfo.setRequestId(request.getRequestId());
        traceInfo.setRequestNo(request.getRequestNo());
        traceInfo.setRequestTitle(request.getTitle());
        traceInfo.setApplicantName(request.getApplicantName());
        traceInfo.setRequestTime(request.getCreateTime());
    }

    private List<PurchaseOrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        return purchaseOrderItemMapper.selectList(wrapper);
    }

    private List<PurchaseStockin> getStockinsByOrderId(Long orderId) {
        LambdaQueryWrapper<PurchaseStockin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseStockin::getOrderId, orderId)
                .orderByDesc(PurchaseStockin::getCreateTime);
        return purchaseStockinMapper.selectList(wrapper);
    }

    private List<PurchaseStockinItem> getStockinItemsByStockins(List<PurchaseStockin> stockins) {
        if (CollectionUtils.isEmpty(stockins)) {
            return Collections.emptyList();
        }

        List<Long> stockinIds = stockins.stream()
                .map(PurchaseStockin::getStockinId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<PurchaseStockinItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PurchaseStockinItem::getStockinId, stockinIds);
        return purchaseStockinItemMapper.selectList(wrapper);
    }

    private void fillStockinInfo(ProcurementTraceInfoVO traceInfo,
                                 List<PurchaseStockin> stockins,
                                 List<PurchaseStockinItem> stockinItems) {
        if (CollectionUtils.isEmpty(stockins)) {
            return;
        }

        List<String> stockinCodes = stockins.stream()
                .map(PurchaseStockin::getStockinCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        traceInfo.setStockinCodes(stockinCodes);

        PurchaseStockin latestStockin = stockins.get(0);
        traceInfo.setStockinTime(latestStockin.getCreateTime());
        traceInfo.setQualityCheckResult(latestStockin.getQualityCheckResult());
        traceInfo.setQualityCheckResultText(convertQualityCheckResultToText(latestStockin.getQualityCheckResult()));

        if (traceInfo.getWarehouseId() == null && latestStockin.getWarehouseId() != null) {
            traceInfo.setWarehouseId(latestStockin.getWarehouseId());
            Warehouse warehouse = warehouseMapper.selectById(latestStockin.getWarehouseId());
            if (warehouse != null) {
                traceInfo.setWarehouseName(warehouse.getWarehouseName());
            }
        }

        BigDecimal totalQuantity = stockinItems.stream()
                .map(PurchaseStockinItem::getActualQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        traceInfo.setTotalStockinQuantity(totalQuantity);

        String latestBatchNo = stockinItems.stream()
                .map(PurchaseStockinItem::getBatchNo)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        traceInfo.setTraceBatchNo(latestBatchNo);
    }

    private void fillMaterialItems(ProcurementTraceInfoVO traceInfo,
                                   List<PurchaseOrderItem> orderItems,
                                   List<PurchaseStockinItem> stockinItems) {
        if (CollectionUtils.isEmpty(orderItems)) {
            return;
        }

        Map<Long, List<PurchaseStockinItem>> stockinItemMap = stockinItems.stream()
                .filter(item -> item.getMaterialId() != null)
                .collect(Collectors.groupingBy(PurchaseStockinItem::getMaterialId));

        List<ProcurementTraceMaterialItemVO> materialItemVos = new ArrayList<>();
        for (PurchaseOrderItem orderItem : orderItems) {
            ProcurementTraceMaterialItemVO vo = new ProcurementTraceMaterialItemVO();
            vo.setMaterialId(orderItem.getMaterialId());
            vo.setMaterialName(orderItem.getMaterialName());
            vo.setSpecification(orderItem.getSpecification());
            vo.setUnit(orderItem.getUnit());
            vo.setOrderQuantity(orderItem.getQuantity());

            if (orderItem.getUnitPrice() != null) {
                vo.setUnitPrice(BigDecimal.valueOf(orderItem.getUnitPrice())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }

            List<PurchaseStockinItem> matchedStockinItems = stockinItemMap.getOrDefault(
                    orderItem.getMaterialId(), Collections.emptyList());

            BigDecimal stockinQuantity = matchedStockinItems.stream()
                    .map(PurchaseStockinItem::getActualQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setStockinQuantity(stockinQuantity);

            matchedStockinItems.stream()
                    .filter(item -> StringUtils.hasText(item.getBatchNo()))
                    .findFirst()
                    .ifPresent(item -> {
                        vo.setBatchNo(item.getBatchNo());
                        vo.setProductionDate(item.getProductionDate());
                        vo.setExpiryDate(item.getExpiryDate());
                    });

            materialItemVos.add(vo);
        }

        traceInfo.setMaterialItems(materialItemVos);
    }

    private void fillInventoryInfo(ProcurementTraceInfoVO traceInfo,
                                   List<PurchaseOrderItem> orderItems,
                                   List<PurchaseStockinItem> stockinItems) {
        Long warehouseId = traceInfo.getWarehouseId();
        if (warehouseId == null || CollectionUtils.isEmpty(orderItems)) {
            return;
        }

        List<Long> materialIds = orderItems.stream()
                .map(PurchaseOrderItem::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getWarehouseId, warehouseId)
                .in(Inventory::getMaterialId, materialIds);
        List<Inventory> inventories = inventoryMapper.selectList(wrapper);

        Map<Long, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getMaterialId, Function.identity(), (a, b) -> a));

        for (ProcurementTraceMaterialItemVO materialItem : traceInfo.getMaterialItems()) {
            Inventory inventory = inventoryMap.get(materialItem.getMaterialId());
            if (inventory != null) {
                materialItem.setCurrentStock(inventory.getQuantity());
                materialItem.setWarehouseId(inventory.getWarehouseId());
                materialItem.setMaterialCode(inventory.getMaterialCode());
            }
        }
    }

    private void fillAssetInfo(ProcurementTraceInfoVO traceInfo, Long orderId) {
        if (orderId == null) {
            return;
        }

        LambdaQueryWrapper<AssetMaster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetMaster::getPurchaseOrderId, orderId);
        List<AssetMaster> assets = assetMasterMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(assets)) {
            return;
        }

        List<ProcurementTraceAssetVO> assetVos = assets.stream()
                .map(this::convertToAssetVO)
                .collect(Collectors.toList());
        traceInfo.setAssets(assetVos);
    }

    private ProcurementTraceAssetVO convertToAssetVO(AssetMaster asset) {
        ProcurementTraceAssetVO vo = new ProcurementTraceAssetVO();
        vo.setAssetId(asset.getId());
        vo.setAssetCode(asset.getAssetCode());
        vo.setAssetName(asset.getAssetName());
        vo.setSpecification(asset.getSpecification());
        vo.setPurchaseOrderId(asset.getPurchaseOrderId());
        vo.setPurchaseOrderNo(asset.getPurchaseOrderNo());
        vo.setSupplierId(asset.getSupplierId());
        vo.setSupplierName(asset.getSupplierName());
        vo.setPurchaseDate(asset.getPurchaseDate());
        vo.setOriginalValue(asset.getOriginalValue());
        vo.setNetValue(asset.getNetValue());
        vo.setStatus(asset.getStatus());
        vo.setDepreciationMethod(asset.getDepreciationMethod());
        vo.setUsefulLifeMonths(asset.getUsefulLifeMonths());
        return vo;
    }

    private void fillTraceNodes(ProcurementTraceInfoVO traceInfo, PurchaseOrder order,
                                List<PurchaseStockin> stockins) {
        List<ProcurementTraceNodeVO> nodes = new ArrayList<>();

        if (StringUtils.hasText(traceInfo.getRequestNo())) {
            ProcurementTraceNodeVO requestNode = new ProcurementTraceNodeVO();
            requestNode.setNodeType("request");
            requestNode.setNodeId(traceInfo.getRequestId());
            requestNode.setNodeCode(traceInfo.getRequestNo());
            requestNode.setTitle(traceInfo.getRequestTitle());
            requestNode.setOperateTime(traceInfo.getRequestTime());
            requestNode.setOperatorName(traceInfo.getApplicantName());
            nodes.add(requestNode);
        }

        ProcurementTraceNodeVO orderNode = new ProcurementTraceNodeVO();
        orderNode.setNodeType("order");
        orderNode.setNodeId(String.valueOf(order.getOrderId()));
        orderNode.setNodeCode(order.getOrderNo());
        orderNode.setTitle("采购订单");
        orderNode.setStatus(traceInfo.getOrderStatus());
        orderNode.setStatusText(traceInfo.getOrderStatusText());
        orderNode.setOperateTime(order.getCreateTime() != null
                ? order.getCreateTime() : order.getOrderDate() != null
                ? order.getOrderDate().atStartOfDay() : null);
        nodes.add(orderNode);

        for (PurchaseStockin stockin : stockins) {
            ProcurementTraceNodeVO stockinNode = new ProcurementTraceNodeVO();
            stockinNode.setNodeType("stockin");
            stockinNode.setNodeId(String.valueOf(stockin.getStockinId()));
            stockinNode.setNodeCode(stockin.getStockinCode());
            stockinNode.setTitle("采购入库");
            stockinNode.setStatus(String.valueOf(stockin.getStatus()));
            stockinNode.setStatusText(convertStockinStatusToText(stockin.getStatus()));
            stockinNode.setOperateTime(stockin.getStockinDate() != null
                    ? stockin.getStockinDate().atStartOfDay() : stockin.getCreateTime());
            stockinNode.setRemark(stockin.getQualityRemark());
            nodes.add(stockinNode);
        }

        for (ProcurementTraceAssetVO asset : traceInfo.getAssets()) {
            ProcurementTraceNodeVO assetNode = new ProcurementTraceNodeVO();
            assetNode.setNodeType("asset");
            assetNode.setNodeId(String.valueOf(asset.getAssetId()));
            assetNode.setNodeCode(asset.getAssetCode());
            assetNode.setTitle("资产卡片");
            assetNode.setStatus(asset.getStatus());
            assetNode.setOperateTime(asset.getPurchaseDate() != null
                    ? asset.getPurchaseDate().atStartOfDay() : null);
            nodes.add(assetNode);
        }

        nodes.sort(Comparator.comparing(ProcurementTraceNodeVO::getOperateTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        traceInfo.setTraceNodes(nodes);
    }

    private String convertOrderStatusToText(Integer orderStatus) {
        if (orderStatus == null) {
            return "未知";
        }
        return switch (orderStatus) {
            case 0 -> "草稿";
            case 1 -> "待审核";
            case 2 -> "已审核";
            case 3 -> "部分入库";
            case 4 -> "已完成";
            case 5 -> "已取消";
            case 6 -> "已下单";
            default -> "未知";
        };
    }

    private String convertQualityCheckResultToText(Integer qualityCheckResult) {
        if (qualityCheckResult == null) {
            return "待检";
        }
        return switch (qualityCheckResult) {
            case 1 -> "合格";
            case 2 -> "不合格";
            case 3 -> "待检";
            default -> "待检";
        };
    }

    private String convertStockinStatusToText(Integer status) {
        if (status == null) {
            return "待入库";
        }
        return switch (status) {
            case 0 -> "待入库";
            case 1 -> "已入库";
            case 2 -> "部分入库";
            case 9 -> "已作废";
            default -> "待入库";
        };
    }
}
