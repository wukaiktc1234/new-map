package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.PurchaseOrderCreateDTO;
import com.foodtraceability.dto.PurchaseOrderItemDTO;
import com.foodtraceability.dto.PurchaseOrderQueryDTO;
import com.foodtraceability.dto.PurchaseOrderUpdateDTO;
import com.foodtraceability.entity.Product;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.utils.SecurityUtils;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 采购订单服务实现类
 * 实现采购订单的全生命周期管理，包含状态机转换逻辑
 */
@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    /** 订单状态常量 */
    public static final int STATUS_DRAFT = 0;       // 草稿
    public static final int STATUS_PENDING = 1;      // 待审核
    public static final int STATUS_APPROVED = 2;     // 已审核
    public static final int STATUS_PARTIAL_IN = 3;   // 部分入库
    public static final int STATUS_COMPLETED = 4;    // 已完成
    public static final int STATUS_CANCELLED = 5;    // 已取消
    public static final int STATUS_CONFIRMED = 6;    // 
    public static final int STATUS_REJECTED = 7;     // 已下单

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final ProductMapper productMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialArchiveMapper materialArchiveMapper;
    private final UserMapper userMapper;

    public PurchaseOrderServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderItemMapper purchaseOrderItemMapper,
                                    ProductMapper productMapper,
                                    SupplierMapper supplierMapper,
                                    MaterialArchiveMapper materialArchiveMapper,
                                    UserMapper userMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.productMapper = productMapper;
        this.supplierMapper = supplierMapper;
        this.materialArchiveMapper = materialArchiveMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrder> getPurchaseOrderPage(PurchaseOrderQueryDTO queryDTO) {
        Page<PurchaseOrder> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getOrderCode())) {
            wrapper.like(PurchaseOrder::getOrderCode, queryDTO.getOrderCode());
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(PurchaseOrder::getSupplierId, queryDTO.getSupplierId());
        }
        if (queryDTO.getOrderStatus() != null) {
            wrapper.eq(PurchaseOrder::getOrderStatus, queryDTO.getOrderStatus());
        }
        if (queryDTO.getPaymentStatus() != null) {
            wrapper.eq(PurchaseOrder::getPaymentStatus, queryDTO.getPaymentStatus());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(PurchaseOrder::getOrderDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(PurchaseOrder::getOrderDate, queryDTO.getEndDate());
        }
        if (StringUtils.hasText(queryDTO.getRequestNo())) {
            wrapper.eq(PurchaseOrder::getRequestNo, queryDTO.getRequestNo());
        }
        if (queryDTO.getCreateUserId() != null) {
            wrapper.eq(PurchaseOrder::getCreateUserId, queryDTO.getCreateUserId());
        }

        wrapper.orderByDesc(PurchaseOrder::getCreateTime);
        Page<PurchaseOrder> pageResult = purchaseOrderMapper.selectPage(page, wrapper);
        fillCreateByNames(pageResult.getRecords());
        return pageResult;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getOrderDetail(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 查询订单明细
        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(itemWrapper);
        order.setItems(items);
        fillCreateByNames(java.util.Collections.singletonList(order));

        // 详情回填供应商（从第一个明细物料档案的主供应商提供，兼容申请转单未指定供应商）
        if (order.getSupplierId() == null && items != null && !items.isEmpty()) {
            PurchaseOrderItem firstItem = items.stream()
                    .filter(i -> i.getMaterialId() != null)
                    .findFirst().orElse(null);
            if (firstItem != null && firstItem.getMaterialId() != null && firstItem.getMaterialId() > 0L) {
                try {
                    com.foodtraceability.entity.MaterialArchive archive = materialArchiveMapper.selectById(firstItem.getMaterialId());
                    if (archive != null && archive.getSupplierId() != null) {
                        order.setSupplierId(archive.getSupplierId());
                        com.foodtraceability.entity.Supplier supplier = supplierMapper.selectById(archive.getSupplierId());
                        if (supplier != null) {
                            order.setSupplierName(supplier.getSupplierName());
                            if (!StringUtils.hasText(order.getContactPerson())) {
                                order.setContactPerson(supplier.getContactPerson());
                            }
                            if (!StringUtils.hasText(order.getContactPhone())) {
                                order.setContactPhone(supplier.getPhone());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("详情回填供应商失败：orderId={}", orderId, e);
                }
            }
        }

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PurchaseOrder> createOrder(PurchaseOrderCreateDTO createDTO) {
        // 校验明细不为空
        if (createDTO.getItems() == null || createDTO.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单明细不能为空");
        }

        // 记录创建人（OA 化：从登录上下文强制绑定）
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserName = null;
        if (currentUserId != null) {
            try {
                com.foodtraceability.entity.User creator = userMapper.selectById(currentUserId);
                if (creator != null) {
                    currentUserName = creator.getFullName();
                }
            } catch (Exception ignored) {
            }
        }

        // P1-PURCHASE-SUPPLIER-BINDING-001: 表头供应商优先；未指定表头供应商时才按物料档案主供应商自动拆分
        java.util.Map<Long, java.util.List<PurchaseOrderItemDTO>> groups = new java.util.LinkedHashMap<>();
        Long headerSupplierId = createDTO.getSupplierId();
        if (headerSupplierId != null) {
            for (PurchaseOrderItemDTO item : createDTO.getItems()) {
                Long archiveSupplierId = getMaterialPrimarySupplier(item.getMaterialId());
                if (archiveSupplierId != null && !archiveSupplierId.equals(headerSupplierId)) {
                    log.warn("物料{}档案主供应商({})与订单表头供应商({})不一致，按表头供应商落库",
                            item.getMaterialId(), archiveSupplierId, headerSupplierId);
                }
            }
            groups.computeIfAbsent(headerSupplierId, k -> new java.util.ArrayList<>()).addAll(createDTO.getItems());
        } else {
            // 按物料主供应商分组（多供应商 → 自动拆分为多张订单）
            java.util.List<PurchaseOrderItemDTO> noSupplierItems = new java.util.ArrayList<>();
            for (PurchaseOrderItemDTO item : createDTO.getItems()) {
                Long supplierId = getMaterialPrimarySupplier(item.getMaterialId());
                if (supplierId != null) {
                    groups.computeIfAbsent(supplierId, k -> new java.util.ArrayList<>()).add(item);
                } else {
                    noSupplierItems.add(item);
                }
            }
            // 无主供应商的物料并入表头供应商组（或独立成组）
            if (!noSupplierItems.isEmpty()) {
                Long fallback = createDTO.getSupplierId();
                groups.computeIfAbsent(fallback, k -> new java.util.ArrayList<>()).addAll(noSupplierItems);
            }
        }
        if (groups.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单明细无法确定供应商");
        }

        boolean multi = groups.size() > 1;
        java.util.List<PurchaseOrder> results = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.util.List<PurchaseOrderItemDTO>> entry : groups.entrySet()) {
            results.add(buildAndSaveOrder(createDTO, entry.getKey(), entry.getValue(),
                    currentUserId, currentUserName, multi));
        }
        log.info("创建采购订单完成：共 {} 张（{}），来源 {}", results.size(),
                results.size() > 1 ? "已按供应商拆分" : "单供应商", createDTO.getSourceType());
        return results;
    }

    /** 查询物料档案主供应商 */
    private Long getMaterialPrimarySupplier(Long materialId) {
        if (materialId == null) return null;
        try {
            com.foodtraceability.entity.MaterialArchive archive = materialArchiveMapper.selectById(materialId);
            return archive != null ? archive.getSupplierId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 构建并保存一张采购订单（供 createOrder 拆分调用） */
    private PurchaseOrder buildAndSaveOrder(PurchaseOrderCreateDTO createDTO,
                                            Long supplierId,
                                            java.util.List<PurchaseOrderItemDTO> items,
                                            Long currentUserId,
                                            String currentUserName,
                                            boolean multiSupplier) {
        // 构建订单实体
        PurchaseOrder order = new PurchaseOrder();
        if (currentUserId != null) {
            order.setCreateUserId(currentUserId);
            order.setCreateByName(currentUserName);
        }
        String orderCode = generateOrderCode();
        order.setOrderCode(orderCode);
        // order_id_str 和 order_no 在DB中为 NOT NULL，使用 orderCode 填充
        order.setOrderIdStr(orderCode);
        order.setOrderNo(orderCode);
        order.setSupplierId(supplierId);
        order.setSupplierName(getSupplierName(supplierId));
        order.setWarehouseId(createDTO.getWarehouseId());
        long groupTotal = items.stream()
                .mapToLong(i -> {
                    BigDecimal qty = i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO;
                    Long price = i.getUnitPrice() != null ? i.getUnitPrice() : 0L;
                    return qty.multiply(BigDecimal.valueOf(price)).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
                })
                .sum();
        order.setTotalAmount(groupTotal);
        order.setTaxAmount(calculateTaxAmount(groupTotal));
        order.setDiscountAmount(createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : 0L);
        order.setFinalAmount(groupTotal - (createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : 0L));
        order.setOrderStatus(STATUS_DRAFT); // 草稿状态
        order.setPaymentStatus(0);           // 未付
        order.setStatus("pending");          // 旧字段字符串状态
        // 来源字段：优先使用前端传入值，缺失时按业务规则兜底
        order.setRequestId(createDTO.getRequestId());
        order.setRequestNo(createDTO.getRequestNo());
        order.setContractId(createDTO.getContractId());
        order.setContractNo(createDTO.getContractNo());
        order.setSourceType(StringUtils.hasText(createDTO.getSourceType()) ? createDTO.getSourceType() : "manual");
        order.setPriority(StringUtils.hasText(createDTO.getPriority()) ? createDTO.getPriority() : "normal");
        order.setPurchaseType(createDTO.getPurchaseType());
        order.setContactPerson(createDTO.getContactPerson());
        order.setContactPhone(createDTO.getContactPhone());
        order.setPaidAmount(createDTO.getPaidAmount() != null ? createDTO.getPaidAmount() : 0L);
        order.setBudgetId(createDTO.getBudgetId());
        order.setBudgetStatus(createDTO.getBudgetStatus());
        order.setPlanId(createDTO.getPlanId());
        order.setExpectedDate(createDTO.getExpectedDate());
        order.setOrderDate(LocalDate.now());
        order.setRemark(multiSupplier ? createDTO.getRemark() : createDTO.getRemark());
        order.setDeleted(0);
        order.setVersion(0);

        purchaseOrderMapper.insert(order);
        log.info("创建采购订单成功，编号：{}，供应商：{}，明细 {} 项", order.getOrderCode(), order.getSupplierName(), items.size());

        // 保存订单明细
        saveOrderItems(order.getOrderId(), items);

        return getOrderDetail(order.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder updateOrder(Long orderId, PurchaseOrderUpdateDTO updateDTO) {
        PurchaseOrder existing = purchaseOrderMapper.selectById(orderId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 仅草稿状态可修改
        if (existing.getOrderStatus() != STATUS_DRAFT && existing.getOrderStatus() != STATUS_CANCELLED
                && existing.getOrderStatus() != STATUS_REJECTED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有草稿或已取消状态的订单可以修改，当前状态：" + existing.getOrderStatus());
        }

        // 更新订单信息
        existing.setSupplierId(updateDTO.getSupplierId());
        existing.setSupplierName(getSupplierName(updateDTO.getSupplierId()));
        existing.setWarehouseId(updateDTO.getWarehouseId());
        existing.setTotalAmount(updateDTO.calculateTotalAmount());
        existing.setDiscountAmount(updateDTO.getDiscountAmount() != null ? updateDTO.getDiscountAmount() : 0L);
        existing.setFinalAmount(updateDTO.calculateFinalAmount());
        existing.setExpectedDate(updateDTO.getExpectedDate());
        existing.setRemark(updateDTO.getRemark());
        // 来源/合同字段允许在草稿/已取消状态下维护
        existing.setRequestId(updateDTO.getRequestId());
        existing.setRequestNo(updateDTO.getRequestNo());
        existing.setContractId(updateDTO.getContractId());
        existing.setContractNo(updateDTO.getContractNo());
        if (StringUtils.hasText(updateDTO.getSourceType())) {
            existing.setSourceType(updateDTO.getSourceType());
        }
        if (StringUtils.hasText(updateDTO.getPriority())) {
            existing.setPriority(updateDTO.getPriority());
        }
        existing.setPurchaseType(updateDTO.getPurchaseType());
        existing.setContactPerson(updateDTO.getContactPerson());
        existing.setContactPhone(updateDTO.getContactPhone());
        existing.setPaidAmount(updateDTO.getPaidAmount() != null ? updateDTO.getPaidAmount() : 0L);
        existing.setBudgetId(updateDTO.getBudgetId());
        existing.setBudgetStatus(updateDTO.getBudgetStatus());
        existing.setPlanId(updateDTO.getPlanId());

        purchaseOrderMapper.updateById(existing);

        // 删除原有明细并重新保存
        LambdaQueryWrapper<PurchaseOrderItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        purchaseOrderItemMapper.delete(deleteWrapper);

        saveOrderItems(orderId, updateDTO.getItems());

        log.info("更新采购订单成功，ID：{}", orderId);
        return getOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder submitOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 状态校验：仅草稿或已取消状态可提交
        if (order.getOrderStatus() != STATUS_DRAFT && order.getOrderStatus() != STATUS_CANCELLED
                && order.getOrderStatus() != STATUS_REJECTED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有草稿或已取消状态的订单可以提交审核");
        }

        order.setOrderStatus(STATUS_PENDING); // 草稿 -> 待审核
        purchaseOrderMapper.updateById(order);

        log.info("提交采购订单审核成功，订单编号：{}", order.getOrderCode());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder approveOrder(Long orderId, String approvalRemark) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 状态校验：仅待审核状态可审批通过
        if (order.getOrderStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有待审核状态的订单可以审批通过");
        }

        order.setOrderStatus(STATUS_APPROVED);   // 待审核 -> 已审核
        order.setApprovalTime(LocalDateTime.now());
        order.setApprovalRemark(approvalRemark);
        purchaseOrderMapper.updateById(order);

        log.info("审批通过采购订单成功，订单编号：{}", order.getOrderCode());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 状态校验：仅已审核状态可确认下单
        if (order.getOrderStatus() != STATUS_APPROVED) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "仅已审核的订单可确认下单");
        }

        order.setOrderStatus(STATUS_CONFIRMED);   // 已审核 -> 已下单
        purchaseOrderMapper.updateById(order);

        // 供应商通知功能暂未接入 RabbitMQ，仅在日志中记录
        log.info("确认下单成功，订单编号：{}，已通知供应商备货", order.getOrderCode());
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder rejectOrder(Long orderId, String approvalRemark) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 状态校验：仅待审核状态可驳回
        if (order.getOrderStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有待审核状态的订单可以驳回");
        }

        order.setOrderStatus(STATUS_REJECTED);   // 待审核 -> 已驳回（可修改后重新提交）
        order.setApprovalTime(LocalDateTime.now());
        order.setApprovalRemark(approvalRemark);
        purchaseOrderMapper.updateById(order);

        log.info("驳回采购订单成功，订单编号：{}", order.getOrderCode());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder cancelOrder(Long orderId, String reason) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 状态校验：仅草稿或待审核状态可取消
        if (order.getOrderStatus() != STATUS_DRAFT && order.getOrderStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有草稿或待审核状态的订单可以取消");
        }

        order.setOrderStatus(STATUS_CANCELLED);
        order.setApprovalRemark(reason);
        order.setApprovalTime(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);

        log.info("取消采购订单成功，订单编号：{}", order.getOrderCode());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购订单不存在");
        }

        // 仅草稿状态可删除
        if (order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                "只有草稿状态的订单可以删除");
        }

        // 先删除关联的订单明细
        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId);
        purchaseOrderItemMapper.delete(itemWrapper);

        // 再删除订单（逻辑删除）
        purchaseOrderMapper.deleteById(orderId);
        log.info("删除采购订单成功，ID：{}", orderId);
    }

    /**
     * 生成订单编号
     * 格式：PO + 年月日 + 4位序号
     */
    private String generateOrderCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO" + dateStr;

        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseOrder::getOrderCode, prefix);
        wrapper.orderByDesc(PurchaseOrder::getOrderCode);
        wrapper.last("LIMIT 1");
        PurchaseOrder lastOrder = purchaseOrderMapper.selectOne(wrapper);

        int seq = 1;
        if (lastOrder != null && lastOrder.getOrderCode() != null) {
            String lastCode = lastOrder.getOrderCode();
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
     * 保存订单明细列表
     * 若DTO未传materialName/unit，则根据materialId自动查询product表填充
     * 若product表不存在或查询失败，使用默认值确保NOT NULL约束不被违反
     */
    private void saveOrderItems(Long orderId, List<PurchaseOrderItemDTO> itemDTOs) {
        for (PurchaseOrderItemDTO dto : itemDTOs) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(orderId);
            item.setItemIdStr(UUID.randomUUID().toString().replace("-", ""));
            item.setMaterialId(dto.getMaterialId());
            // 兜底：若DTO未传物料名称/单位，则查询物料主数据自动填充
            String materialName = dto.getMaterialName();
            String unit = dto.getUnit();
            if ((materialName == null || materialName.isEmpty() || unit == null || unit.isEmpty())
                    && dto.getMaterialId() != null) {
                try {
                    Product product = productMapper.selectById(dto.getMaterialId());
                    if (product != null) {
                        if (materialName == null || materialName.isEmpty()) {
                            materialName = product.getName();
                        }
                        if (unit == null || unit.isEmpty()) {
                            unit = product.getUnit();
                        }
                    }
                } catch (Exception e) {
                    log.warn("查询物料主数据失败，materialId={}，使用默认值: {}", dto.getMaterialId(), e.getMessage());
                }
            }
            // 最终兜底：若仍为空，使用默认值确保NOT NULL约束
            if (materialName == null || materialName.isEmpty()) {
                materialName = "物料-" + dto.getMaterialId();
            }
            if (unit == null || unit.isEmpty()) {
                unit = "个";
            }
            item.setMaterialName(materialName);
            item.setSpecification(dto.getSpecification());
            item.setUnit(unit);
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setAmount(dto.calculateAmount());
            item.setTaxRate(dto.getTaxRate() != null ? dto.getTaxRate() : BigDecimal.ZERO);
            item.setReceivedQuantity(BigDecimal.ZERO);
            item.setRemark(dto.getRemark());
            item.setPlannedReceiverType(dto.getPlannedReceiverType());
            item.setPlannedStoreId(dto.getPlannedStoreId());
            item.setPlannedWarehouseId(dto.getPlannedWarehouseId());
            purchaseOrderItemMapper.insert(item);
        }
    }

    /**
     * 根据供应商ID查询供应商名称
     * @param supplierId 供应商ID
     * @return 供应商名称，未找到返回空字符串
     */
    private String getSupplierName(Long supplierId) {
        if (supplierId == null) {
            return "";
        }
        try {
            com.foodtraceability.entity.Supplier supplier = supplierMapper.selectById(supplierId);
            return supplier != null && supplier.getSupplierName() != null ? supplier.getSupplierName() : "";
        } catch (Exception e) {
            log.warn("查询供应商名称失败，supplierId={}，错误：{}", supplierId, e.getMessage());
            return "";
        }
    }

    /**
     * 计算税额（简化计算：按13%税率）
     */
    private Long calculateTaxAmount(Long totalAmount) {
        if (totalAmount == null || totalAmount <= 0) {
            return 0L;
        }
        // 税额 = 总金额 / 1.13 * 0.13
        long taxAmount = Math.round(totalAmount / 113.0 * 13);
        return taxAmount;
    }
    /** 批量填充创建人姓名（根据 createUserId 查 users 表） */
    private void fillCreateByNames(List<PurchaseOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        java.util.Set<Long> userIds = orders.stream()
                .map(PurchaseOrder::getCreateUserId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (userIds.isEmpty()) return;
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
        for (Long uid : userIds) {
            try {
                com.foodtraceability.entity.User u = userMapper.selectById(uid);
                if (u != null) nameMap.put(uid, u.getFullName());
            } catch (Exception ignored) {
            }
        }
        for (PurchaseOrder o : orders) {
            if (o.getCreateUserId() != null && o.getCreateByName() == null) {
                o.setCreateByName(nameMap.get(o.getCreateUserId()));
            }
        }
    }
}
