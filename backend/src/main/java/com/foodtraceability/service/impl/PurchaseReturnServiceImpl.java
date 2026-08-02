package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.PurchaseReturnApproveDTO;
import com.foodtraceability.dto.PurchaseReturnCreateDTO;
import com.foodtraceability.dto.PurchaseReturnItemCreateDTO;
import com.foodtraceability.dto.PurchaseReturnItemVO;
import com.foodtraceability.dto.PurchaseReturnVO;
import com.foodtraceability.entity.PurchaseReturn;
import com.foodtraceability.entity.PurchaseReturnItem;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.entity.finance.SupplierRefundRequest;
import com.foodtraceability.mapper.PurchaseReturnItemMapper;
import com.foodtraceability.mapper.PurchaseReturnMapper;
import com.foodtraceability.mapper.finance.SupplierRefundRequestMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.PurchaseReturnService;
import com.foodtraceability.service.PurchaseStockinService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.SupplierService;
import com.foodtraceability.service.finance.PayableService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购退货单服务实现类
 *
 * <p>核心业务规则：
 * 1. 退货单必须关联已入库的采购入库单，支持部分物料退货。
 * 2. 审批通过后按总量扣减库存，并生成红字应付单（金额为负）。
 * 3. 红字应付单通过 related_invoice_id 关联原正向应付单，实现余额实时扣减。
 * 4. 若原正向应付已付清，退货金额转为供应商可抵扣余额（return_credit_balance）。
 * 5. 现金退款路径生成供应商退款申请单，走独立审批/到账确认流程。</p>
 */
@Service
public class PurchaseReturnServiceImpl extends ServiceImpl<PurchaseReturnMapper, PurchaseReturn>
        implements PurchaseReturnService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseReturnServiceImpl.class);

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_COMPLETED = "completed";

    private static final String REFUND_METHOD_OFFSET = "offset";
    private static final String REFUND_METHOD_CASH = "cash";

    private static final int PAYABLE_STATUS_PENDING = 1;
    private static final int PAYABLE_STATUS_SETTLED = 3;

    private final PurchaseReturnMapper purchaseReturnMapper;
    private final PurchaseReturnItemMapper purchaseReturnItemMapper;
    private final SupplierRefundRequestMapper supplierRefundRequestMapper;
    private final PurchaseStockinService purchaseStockinService;
    private final InventoryService inventoryService;
    private final StoreInventoryService storeInventoryService;
    private final PayableService payableService;
    private final SupplierService supplierService;

    public PurchaseReturnServiceImpl(PurchaseReturnMapper purchaseReturnMapper,
                                     PurchaseReturnItemMapper purchaseReturnItemMapper,
                                     SupplierRefundRequestMapper supplierRefundRequestMapper,
                                     PurchaseStockinService purchaseStockinService,
                                     InventoryService inventoryService,
                                     StoreInventoryService storeInventoryService,
                                     PayableService payableService,
                                     SupplierService supplierService) {
        this.purchaseReturnMapper = purchaseReturnMapper;
        this.purchaseReturnItemMapper = purchaseReturnItemMapper;
        this.supplierRefundRequestMapper = supplierRefundRequestMapper;
        this.purchaseStockinService = purchaseStockinService;
        this.inventoryService = inventoryService;
        this.storeInventoryService = storeInventoryService;
        this.payableService = payableService;
        this.supplierService = supplierService;
    }

    @Override
    public Page<PurchaseReturnVO> getPurchaseReturnPage(int current, int size, String returnNo,
                                                        Long supplierId, String stockinNo,
                                                        String status, String startDate, String endDate) {
        Page<PurchaseReturn> page = new Page<>(current, size);
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(returnNo)) {
            wrapper.like(PurchaseReturn::getReturnNo, returnNo);
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseReturn::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(stockinNo)) {
            wrapper.like(PurchaseReturn::getStockinNo, stockinNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseReturn::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseReturn::getCreateTime, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseReturn::getCreateTime, endDate);
        }
        wrapper.orderByDesc(PurchaseReturn::getCreateTime);

        Page<PurchaseReturn> result = purchaseReturnMapper.selectPage(page, wrapper);
        List<PurchaseReturnVO> voList = new ArrayList<>();
        for (PurchaseReturn record : result.getRecords()) {
            voList.add(convertToVO(record, false));
        }
        Page<PurchaseReturnVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnVO createPurchaseReturn(PurchaseReturnCreateDTO dto) {
        PurchaseStockin stockin = loadStockinForReturn(dto.getStockinId());

        PurchaseReturn purchaseReturn = buildReturnHeader(stockin, dto);
        List<PurchaseReturnItem> items = buildReturnItems(stockin, dto.getItems(), purchaseReturn);

        purchaseReturn.setTotalQuantity(calculateTotalQuantity(items));
        purchaseReturn.setTotalAmount(calculateTotalAmount(items));

        purchaseReturnMapper.insert(purchaseReturn);
        for (PurchaseReturnItem item : items) {
            item.setReturnId(purchaseReturn.getId());
            purchaseReturnItemMapper.insert(item);
        }

        log.info("创建采购退货单成功，ID：{}，单号：{}，原入库单：{}",
                purchaseReturn.getId(), purchaseReturn.getReturnNo(), stockin.getStockinCode());
        return getPurchaseReturnById(purchaseReturn.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnVO updatePurchaseReturn(Long id, PurchaseReturnCreateDTO dto) {
        PurchaseReturn existing = getReturnById(id);
        if (!STATUS_PENDING.equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待审批状态的退货单可修改");
        }

        PurchaseStockin stockin = loadStockinForReturn(dto.getStockinId());
        PurchaseReturn updatedHeader = buildReturnHeader(stockin, dto);
        updatedHeader.setId(id);
        updatedHeader.setReturnNo(existing.getReturnNo());
        updatedHeader.setStatus(existing.getStatus());

        List<PurchaseReturnItem> items = buildReturnItems(stockin, dto.getItems(), updatedHeader);
        updatedHeader.setTotalQuantity(calculateTotalQuantity(items));
        updatedHeader.setTotalAmount(calculateTotalAmount(items));

        purchaseReturnMapper.updateById(updatedHeader);

        LambdaQueryWrapper<PurchaseReturnItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseReturnItem::getReturnId, id);
        purchaseReturnItemMapper.delete(itemWrapper);

        for (PurchaseReturnItem item : items) {
            item.setReturnId(id);
            purchaseReturnItemMapper.insert(item);
        }

        log.info("更新采购退货单成功，ID：{}", id);
        return getPurchaseReturnById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseReturn(Long id) {
        PurchaseReturn purchaseReturn = getReturnById(id);
        if (!STATUS_PENDING.equals(purchaseReturn.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待审批状态的退货单可删除");
        }
        removeById(id);
        log.info("删除采购退货单，ID：{}", id);
    }

    @Override
    public PurchaseReturnVO getPurchaseReturnById(Long id) {
        PurchaseReturn purchaseReturn = getReturnById(id);
        return convertToVO(purchaseReturn, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnVO approvePurchaseReturn(Long id, PurchaseReturnApproveDTO dto) {
        PurchaseReturn purchaseReturn = getReturnById(id);
        if (!STATUS_PENDING.equals(purchaseReturn.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有待审批状态的退货单可审批");
        }

        purchaseReturn.setStatus(dto.getStatus());
        purchaseReturn.setApprovalRemark(dto.getRemark());

        if (STATUS_APPROVED.equals(dto.getStatus())) {
            executeApprovedLinkage(purchaseReturn);
        }

        purchaseReturnMapper.updateById(purchaseReturn);
        log.info("审批采购退货单，ID：{}，状态：{}", id, dto.getStatus());
        return getPurchaseReturnById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnVO completePurchaseReturn(Long id) {
        PurchaseReturn purchaseReturn = getReturnById(id);
        if (!STATUS_APPROVED.equals(purchaseReturn.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有已审批状态的退货单可完成");
        }
        if (!REFUND_METHOD_CASH.equals(purchaseReturn.getRefundMethod())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "仅现金退款方式的退货单需要执行完成操作");
        }

        // 更新现金退款申请单为已到账
        LambdaQueryWrapper<SupplierRefundRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierRefundRequest::getReturnId, id)
               .eq(SupplierRefundRequest::getStatus, SupplierRefundRequest.STATUS_PENDING);
        SupplierRefundRequest refundRequest = supplierRefundRequestMapper.selectOne(wrapper);
        if (refundRequest != null) {
            refundRequest.setStatus(SupplierRefundRequest.STATUS_COMPLETED);
            refundRequest.setUpdateTime(LocalDateTime.now());
            supplierRefundRequestMapper.updateById(refundRequest);
        }

        // 红字应付单标记为已付清（已退款到账）
        if (purchaseReturn.getRelatedPayableId() != null) {
            Payable redPayable = payableService.getById(purchaseReturn.getRelatedPayableId());
            if (redPayable != null) {
                redPayable.setStatus(PAYABLE_STATUS_SETTLED);
                redPayable.setPaidAmount(redPayable.getOriginalAmount());
                redPayable.setBalanceAmount(0L);
                payableService.updateById(redPayable);
            }
        }

        purchaseReturn.setStatus(STATUS_COMPLETED);
        purchaseReturnMapper.updateById(purchaseReturn);
        log.info("采购退货单完成，ID：{}", id);
        return getPurchaseReturnById(id);
    }

    // ==================== 私有业务方法 ====================

    private PurchaseReturn getReturnById(Long id) {
        PurchaseReturn purchaseReturn = getById(id);
        if (purchaseReturn == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购退货单不存在");
        }
        return purchaseReturn;
    }

    private PurchaseStockin loadStockinForReturn(Long stockinId) {
        if (stockinId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原入库单ID不能为空");
        }
        PurchaseStockin stockin = purchaseStockinService.getStockinDetail(stockinId);
        if (stockin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "原入库单不存在");
        }
        if (stockin.getStatus() == null || stockin.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "只有已入库状态的入库单可发起退货");
        }
        return stockin;
    }

    private PurchaseReturn buildReturnHeader(PurchaseStockin stockin, PurchaseReturnCreateDTO dto) {
        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setReturnNo(generateReturnNo());
        purchaseReturn.setStockinId(stockin.getStockinId());
        purchaseReturn.setStockinNo(stockin.getStockinCode());
        purchaseReturn.setOrderId(stockin.getOrderId());
        purchaseReturn.setOrderNo(stockin.getOrderNo());
        purchaseReturn.setSupplierId(stockin.getSupplierId());
        purchaseReturn.setSupplierName(stockin.getSupplierName());
        purchaseReturn.setWarehouseId(stockin.getWarehouseId());
        purchaseReturn.setReturnDate(dto.getReturnDate() != null ? dto.getReturnDate() : LocalDate.now());
        purchaseReturn.setRefundMethod(dto.getRefundMethod() != null ? dto.getRefundMethod() : REFUND_METHOD_OFFSET);
        purchaseReturn.setStatus(STATUS_PENDING);
        purchaseReturn.setRemark(dto.getRemark());
        return purchaseReturn;
    }

    private List<PurchaseReturnItem> buildReturnItems(PurchaseStockin stockin,
                                                      List<PurchaseReturnItemCreateDTO> itemDTOs,
                                                      PurchaseReturn purchaseReturn) {
        Map<Long, PurchaseStockinItem> stockinItemMap = stockin.getItems().stream()
                .collect(Collectors.toMap(PurchaseStockinItem::getStockinItemId, item -> item));

        List<PurchaseReturnItem> items = new ArrayList<>();
        for (PurchaseReturnItemCreateDTO dto : itemDTOs) {
            PurchaseStockinItem stockinItem = stockinItemMap.get(dto.getStockinItemId());
            if (stockinItem == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "原入库明细ID不存在或不属于当前入库单：" + dto.getStockinItemId());
            }
            if (!stockinItem.getMaterialId().equals(dto.getMaterialId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "退货物料与原入库明细不匹配，物料ID：" + dto.getMaterialId());
            }
            if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "物料「" + stockinItem.getMaterialName() + "」退货数量必须大于0");
            }
            if (dto.getQuantity().compareTo(stockinItem.getActualQuantity()) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "物料「" + stockinItem.getMaterialName() + "」退货数量不能超过入库数量");
            }

            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setReturnId(purchaseReturn.getId());
            item.setStockinItemId(stockinItem.getStockinItemId());
            item.setMaterialId(stockinItem.getMaterialId());
            item.setMaterialName(stockinItem.getMaterialName());
            item.setUnit(stockinItem.getUnit());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(stockinItem.getUnitPrice() != null ? stockinItem.getUnitPrice() : 0L);
            item.setTotalAmount(Math.round(dto.getQuantity().doubleValue() * item.getUnitPrice()));
            item.setReturnReason(dto.getReturnReason());
            item.setBatchNo(dto.getBatchNo() != null ? dto.getBatchNo() : stockinItem.getBatchNo());
            items.add(item);
        }
        return items;
    }

    private BigDecimal calculateTotalQuantity(List<PurchaseReturnItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturnItem item : items) {
            if (item.getQuantity() != null) {
                total = total.add(item.getQuantity());
            }
        }
        return total;
    }

    private Long calculateTotalAmount(List<PurchaseReturnItem> items) {
        long total = 0L;
        for (PurchaseReturnItem item : items) {
            if (item.getTotalAmount() != null) {
                total += item.getTotalAmount();
            }
        }
        return total;
    }

    private String generateReturnNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PR" + dateStr;

        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseReturn::getReturnNo, prefix)
               .orderByDesc(PurchaseReturn::getReturnNo)
               .last("LIMIT 1");
        PurchaseReturn last = purchaseReturnMapper.selectOne(wrapper);

        int seq = 1;
        if (last != null && last.getReturnNo() != null) {
            String seqStr = last.getReturnNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private void executeApprovedLinkage(PurchaseReturn purchaseReturn) {
        List<PurchaseReturnItem> items = loadReturnItems(purchaseReturn.getId());
        purchaseReturn.setItems(items);

        // 1. 扣减库存（按总量）
        decreaseInventoryForReturn(purchaseReturn, items);

        // 2. 查找原正向应付单
        Payable originalPayable = payableService.lambdaQuery()
                .eq(Payable::getStockinId, purchaseReturn.getStockinId())
                .eq(Payable::getDeleted, 0)
                .one();
        if (originalPayable == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "原入库单关联的应付账款不存在");
        }

        // 3. 创建红字应付单
        boolean originalPaid = originalPayable.getStatus() != null
                && originalPayable.getStatus() == PAYABLE_STATUS_SETTLED;
        Integer redPayableStatus = REFUND_METHOD_CASH.equals(purchaseReturn.getRefundMethod())
                ? PAYABLE_STATUS_PENDING
                : (originalPaid ? PAYABLE_STATUS_SETTLED : PAYABLE_STATUS_PENDING);

        var redPayable = payableService.createRedPayableForReturn(
                purchaseReturn.getId(),
                originalPayable.getPayableId(),
                purchaseReturn.getSupplierId(),
                purchaseReturn.getSupplierName(),
                purchaseReturn.getOrderId(),
                purchaseReturn.getOrderNo(),
                purchaseReturn.getStockinId(),
                purchaseReturn.getStockinNo(),
                purchaseReturn.getTotalAmount(),
                purchaseReturn.getReturnDate(),
                redPayableStatus
        );
        purchaseReturn.setRelatedPayableId(redPayable.getPayableId());

        // 4. 退款方式后续处理
        if (REFUND_METHOD_OFFSET.equals(purchaseReturn.getRefundMethod())) {
            // 若原单已付清，退货金额转为供应商可抵扣余额（其他应收/预付款）
            if (originalPaid) {
                addSupplierReturnCreditBalance(purchaseReturn.getSupplierId(), purchaseReturn.getTotalAmount());
            }
        } else if (REFUND_METHOD_CASH.equals(purchaseReturn.getRefundMethod())) {
            createSupplierRefundRequest(purchaseReturn);
        }
    }

    private void decreaseInventoryForReturn(PurchaseReturn purchaseReturn, List<PurchaseReturnItem> items) {
        String storeIdForSync = purchaseReturn.getWarehouseId() != null
                ? String.valueOf(purchaseReturn.getWarehouseId()) : null;

        for (PurchaseReturnItem item : items) {
            try {
                InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
                decreaseDTO.setMaterialId(item.getMaterialId());
                decreaseDTO.setWarehouseId(purchaseReturn.getWarehouseId());
                decreaseDTO.setQuantity(item.getQuantity());
                decreaseDTO.setTransactionType(7); // 采购退货出库
                decreaseDTO.setReferenceNo(purchaseReturn.getReturnNo());
                decreaseDTO.setReferenceType("purchase_return");
                decreaseDTO.setRemark("采购退货扣减库存");

                inventoryService.decreaseInventory(decreaseDTO);
                log.debug("退货库存扣减成功：物料ID={}，数量={}", item.getMaterialId(), item.getQuantity());

                if (storeIdForSync != null && item.getQuantity() != null) {
                    try {
                        storeInventoryService.decreaseStock(storeIdForSync, item.getMaterialId(), item.getQuantity(),
                                2, "采购退货出库 - 退货单:" + purchaseReturn.getReturnNo());
                        log.debug("门店库存退货扣减成功：storeId={}, 物料ID={}, 数量={}",
                                storeIdForSync, item.getMaterialId(), item.getQuantity());
                    } catch (Exception syncEx) {
                        log.error("门店库存退货扣减失败，事务将回滚：storeId={}, 物料ID={}, 错误={}",
                                storeIdForSync, item.getMaterialId(), syncEx.getMessage(), syncEx);
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                                "门店库存扣减失败：物料" + item.getMaterialName() + " - " + syncEx.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("退货库存扣减失败：物料ID={}，错误：{}", item.getMaterialId(), e.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "库存扣减失败：物料" + item.getMaterialName() + " - " + e.getMessage());
            }
        }
    }

    private void addSupplierReturnCreditBalance(Long supplierId, Long amount) {
        if (supplierId == null || amount == null || amount <= 0) {
            return;
        }
        Supplier supplier = supplierService.getById(supplierId);
        if (supplier == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        long balance = supplier.getReturnCreditBalance() != null ? supplier.getReturnCreditBalance() : 0L;
        supplier.setReturnCreditBalance(balance + amount);
        supplierService.updateById(supplier);
        log.info("供应商可抵扣退货余额增加，supplierId：{}，增加：{}，余额：{}",
                supplierId, amount, supplier.getReturnCreditBalance());
    }

    private void createSupplierRefundRequest(PurchaseReturn purchaseReturn) {
        SupplierRefundRequest request = new SupplierRefundRequest();
        request.setReturnId(purchaseReturn.getId());
        request.setSupplierId(purchaseReturn.getSupplierId());
        request.setSupplierName(purchaseReturn.getSupplierName());
        request.setAmount(purchaseReturn.getTotalAmount());
        request.setStatus(SupplierRefundRequest.STATUS_PENDING);
        supplierRefundRequestMapper.insert(request);
        log.info("创建供应商退款申请，退货单ID：{}，金额：{}分", purchaseReturn.getId(), purchaseReturn.getTotalAmount());
    }

    private List<PurchaseReturnItem> loadReturnItems(Long returnId) {
        LambdaQueryWrapper<PurchaseReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturnItem::getReturnId, returnId);
        return purchaseReturnItemMapper.selectList(wrapper);
    }

    private PurchaseReturnVO convertToVO(PurchaseReturn purchaseReturn, boolean withItems) {
        PurchaseReturnVO vo = new PurchaseReturnVO();
        vo.setId(purchaseReturn.getId());
        vo.setReturnNo(purchaseReturn.getReturnNo());
        vo.setStockinId(purchaseReturn.getStockinId());
        vo.setStockinNo(purchaseReturn.getStockinNo());
        vo.setOrderId(purchaseReturn.getOrderId());
        vo.setOrderNo(purchaseReturn.getOrderNo());
        vo.setSupplierId(purchaseReturn.getSupplierId());
        vo.setSupplierName(purchaseReturn.getSupplierName());
        vo.setWarehouseId(purchaseReturn.getWarehouseId());
        vo.setReturnDate(purchaseReturn.getReturnDate());
        vo.setTotalQuantity(purchaseReturn.getTotalQuantity());
        vo.setTotalAmount(purchaseReturn.getTotalAmount());
        vo.setRefundMethod(purchaseReturn.getRefundMethod());
        vo.setStatus(purchaseReturn.getStatus());
        vo.setApprovalRemark(purchaseReturn.getApprovalRemark());
        vo.setRelatedPayableId(purchaseReturn.getRelatedPayableId());
        vo.setRemark(purchaseReturn.getRemark());
        vo.setCreateTime(purchaseReturn.getCreateTime());
        vo.setUpdateTime(purchaseReturn.getUpdateTime());

        if (withItems) {
            List<PurchaseReturnItem> items = loadReturnItems(purchaseReturn.getId());
            List<PurchaseReturnItemVO> itemVOs = new ArrayList<>();
            for (PurchaseReturnItem item : items) {
                PurchaseReturnItemVO itemVO = new PurchaseReturnItemVO();
                itemVO.setId(item.getId());
                itemVO.setReturnId(item.getReturnId());
                itemVO.setStockinItemId(item.getStockinItemId());
                itemVO.setMaterialId(item.getMaterialId());
                itemVO.setMaterialName(item.getMaterialName());
                itemVO.setSpecification(item.getSpecification());
                itemVO.setUnit(item.getUnit());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setUnitPrice(item.getUnitPrice());
                itemVO.setTotalAmount(item.getTotalAmount());
                itemVO.setReturnReason(item.getReturnReason());
                itemVO.setBatchNo(item.getBatchNo());
                itemVO.setCreateTime(item.getCreateTime());
                itemVOs.add(itemVO);
            }
            vo.setItems(itemVOs);
        }
        return vo;
    }
}
