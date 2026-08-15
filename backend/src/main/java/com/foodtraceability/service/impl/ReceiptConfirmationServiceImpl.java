package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.dto.ReceiptConfirmationCreateDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.dto.ReceiptSignatureDTO;
import com.foodtraceability.dto.ReceiptVerifyVO;
import com.foodtraceability.entity.PurchaseArrival;
import com.foodtraceability.entity.PurchaseArrivalItem;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.ReceiptConfirmation;
import com.foodtraceability.entity.ReceiptConfirmationItem;
import com.foodtraceability.entity.ReceiptEvidence;
import com.foodtraceability.entity.ReceiptPrintLog;
import com.foodtraceability.entity.ReceiptSignature;
import com.foodtraceability.entity.Store;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.entity.User;
import com.foodtraceability.event.EventPublisher;
import com.foodtraceability.event.ReceiptConfirmationCompletedEvent;
import com.foodtraceability.mapper.PurchaseArrivalItemMapper;
import com.foodtraceability.mapper.PurchaseArrivalMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.ReceiptConfirmationItemMapper;
import com.foodtraceability.mapper.ReceiptConfirmationMapper;
import com.foodtraceability.mapper.ReceiptEvidenceMapper;
import com.foodtraceability.mapper.ReceiptPrintLogMapper;
import com.foodtraceability.mapper.ReceiptSignatureMapper;
import com.foodtraceability.mapper.StoreMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.ReceiptConfirmationService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.SupplierService;
import com.foodtraceability.service.finance.PayableService;
import com.foodtraceability.utils.IpUtils;
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
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final String SIGNER_RECEIVER = "RECEIVER";
    private static final String SIGNER_SUPPLIER = "SUPPLIER";

    private static final int MAX_PRINT_COUNT = 10;

    private final ReceiptConfirmationMapper confirmationMapper;
    private final ReceiptConfirmationItemMapper confirmationItemMapper;
    private final ReceiptEvidenceMapper receiptEvidenceMapper;
    private final ReceiptSignatureMapper receiptSignatureMapper;
    private final PurchaseArrivalMapper arrivalMapper;
    private final PurchaseArrivalItemMapper arrivalItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final StoreInventoryService storeInventoryService;
    private final PayableService payableService;
    private final SupplierService supplierService;
    private final EventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final StoreMapper storeMapper;
    private final SupplierMapper supplierMapper;
    private final ReceiptPrintLogMapper receiptPrintLogMapper;

    public ReceiptConfirmationServiceImpl(ReceiptConfirmationMapper confirmationMapper,
                                          ReceiptConfirmationItemMapper confirmationItemMapper,
                                          ReceiptEvidenceMapper receiptEvidenceMapper,
                                          ReceiptSignatureMapper receiptSignatureMapper,
                                          PurchaseArrivalMapper arrivalMapper,
                                          PurchaseArrivalItemMapper arrivalItemMapper,
                                          PurchaseOrderMapper orderMapper,
                                          PurchaseOrderItemMapper orderItemMapper,
                                          InventoryService inventoryService,
                                          StoreInventoryService storeInventoryService,
                                          PayableService payableService,
                                          SupplierService supplierService,
                                          EventPublisher eventPublisher,
UserMapper userMapper,
                                           StoreMapper storeMapper,
                                           SupplierMapper supplierMapper,
                                           ReceiptPrintLogMapper receiptPrintLogMapper) {
        this.confirmationMapper = confirmationMapper;
        this.confirmationItemMapper = confirmationItemMapper;
        this.receiptEvidenceMapper = receiptEvidenceMapper;
        this.receiptSignatureMapper = receiptSignatureMapper;
        this.arrivalMapper = arrivalMapper;
        this.arrivalItemMapper = arrivalItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.storeInventoryService = storeInventoryService;
        this.payableService = payableService;
        this.supplierService = supplierService;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
        this.storeMapper = storeMapper;
        this.supplierMapper = supplierMapper;
        this.receiptPrintLogMapper = receiptPrintLogMapper;
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
        // 到货单与确认单一对一：整单确认一次成型，禁止重复确认造成数据链断裂
        Long existing = confirmationMapper.selectCount(new LambdaQueryWrapper<ReceiptConfirmation>()
                .eq(ReceiptConfirmation::getArrivalId, arrival.getArrivalId())
                .eq(ReceiptConfirmation::getReceiptSource, RECEIPT_SOURCE_ARRIVAL));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "该到货单已生成收货确认单，不可重复确认（请到收货确认单查询历史）");
        }

        // 兼容历史数据：STORE 类型到货单缺失门店ID时回退默认门店，避免门店库存写入空门店
        resolveStoreIdIfMissing(arrival);

        List<PurchaseArrivalItem> arrivalItems = getArrivalItems(arrival.getArrivalId());
        List<ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO> itemDTOs = createDTO.getItems();
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收货明细不能为空");
        }

        // 预校验：每个收货数量不超过剩余可收数量
        for (ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO : itemDTOs) {
            validateConfirmQuantity(itemDTO, arrivalItems);
        }
        // 留证校验：拒收数量>0 的明细必须携带现场照片（防绕过前端直接调用）
        for (ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO : itemDTOs) {
            validateEvidence(itemDTO);
        }

        // 保存收货确认单主表
        ReceiptConfirmation confirmation = buildConfirmation(arrival, createDTO);
        confirmation.setReceiptSource(RECEIPT_SOURCE_ARRIVAL);
        // 防伪码：纯随机不可预测（SecureRandom），唯一索引兜底，冲突重试
        for (int i = 0; i < 5; i++) {
            confirmation.setVerifyCode(generateVerifyCode());
            try {
                confirmationMapper.insert(confirmation);
                break;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                if (i == 4) {
                    throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "防伪码生成冲突，请重试");
                }
            }
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;
        long totalAmount = 0L;
        for (ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO : itemDTOs) {
            PurchaseArrivalItem arrivalItem = findArrivalItem(itemDTO.getArrivalItemId(), arrivalItems);
            BigDecimal confirmedQty = itemDTO.getConfirmedQuantity() != null ? itemDTO.getConfirmedQuantity() : BigDecimal.ZERO;
            BigDecimal rejectedQty = itemDTO.getRejectedQuantity() != null ? itemDTO.getRejectedQuantity() : BigDecimal.ZERO;
            long itemAmount = confirmedQty.longValue() * (arrivalItem.getUnitPrice() != null ? arrivalItem.getUnitPrice() : 0L);

            ReceiptConfirmationItem confirmationItem = buildConfirmationItem(confirmation.getConfirmationId(), arrivalItem, itemDTO, itemAmount);
            confirmationItemMapper.insert(confirmationItem);

            // 保存留证照片（现场拍摄证据链）
            saveEvidence(confirmation.getConfirmationId(), confirmationItem, itemDTO);

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
        // 全部拒收(实收为0)的确认单状态置为已拒收，否则为已确认
        confirmation.setStatus(totalQuantity.compareTo(BigDecimal.ZERO) == 0 ? STATUS_REJECTED : STATUS_CONFIRMED);
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
        // 确认时间区间（yyyy-MM-dd；止日按当天 23:59:59.999 取全天）
        if (StringUtils.hasText(queryDTO.getConfirmDateStart())) {
            wrapper.ge(ReceiptConfirmation::getConfirmTime, parseStartOfDay(queryDTO.getConfirmDateStart()));
        }
        if (StringUtils.hasText(queryDTO.getConfirmDateEnd())) {
            wrapper.le(ReceiptConfirmation::getConfirmTime, parseEndOfDay(queryDTO.getConfirmDateEnd()));
        }
        // 供应商名称过滤：确认单不存供应商，先经由 suppliers 表匹配到货单 supplier_id 集合
        if (StringUtils.hasText(queryDTO.getSupplierName())) {
            LambdaQueryWrapper<Supplier> supplierWrapper = new LambdaQueryWrapper<>();
            supplierWrapper.select(Supplier::getSupplierId)
                    .like(Supplier::getSupplierName, queryDTO.getSupplierName())
                    .eq(Supplier::getDeleted, 0);
            Set<Long> supplierIds = supplierMapper.selectList(supplierWrapper).stream()
                    .map(Supplier::getSupplierId)
                    .collect(Collectors.toSet());
            if (supplierIds.isEmpty()) {
                return new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            }
            LambdaQueryWrapper<PurchaseArrival> arrivalWrapper = new LambdaQueryWrapper<>();
            arrivalWrapper.select(PurchaseArrival::getArrivalId)
                    .in(PurchaseArrival::getSupplierId, supplierIds)
                    .eq(PurchaseArrival::getDeleted, 0);
            List<Long> arrivalIds = arrivalMapper.selectList(arrivalWrapper).stream()
                    .map(PurchaseArrival::getArrivalId)
                    .collect(Collectors.toList());
            if (arrivalIds.isEmpty()) {
                return new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            }
            wrapper.in(ReceiptConfirmation::getArrivalId, arrivalIds);
        }
        wrapper.orderByDesc(ReceiptConfirmation::getCreateTime);
        Page<ReceiptConfirmation> result = confirmationMapper.selectPage(page, wrapper);
        for (ReceiptConfirmation confirmation : result.getRecords()) {
            confirmation.setItems(getConfirmationItems(confirmation.getConfirmationId()));
            enrichArrivalInfo(confirmation);
        }
        return result;
    }

    /** 为确认单填充关联到货单的到货单号与供应商名称（分页展示/打印用） */
    private void enrichArrivalInfo(ReceiptConfirmation confirmation) {
        if (confirmation == null || confirmation.getArrivalId() == null) {
            return;
        }
        PurchaseArrival arrival = arrivalMapper.selectById(confirmation.getArrivalId());
        if (arrival != null) {
            confirmation.setArrivalCode(arrival.getArrivalCode());
            if (StringUtils.hasText(arrival.getSupplierName())) {
                confirmation.setSupplierName(arrival.getSupplierName());
            } else if (arrival.getSupplierId() != null) {
                Supplier supplier = supplierMapper.selectById(arrival.getSupplierId());
                if (supplier != null) {
                    confirmation.setSupplierName(supplier.getSupplierName());
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptConfirmation getConfirmationDetail(Long confirmationId) {
        ReceiptConfirmation confirmation = confirmationMapper.selectById(confirmationId);
        if (confirmation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货确认单不存在");
        }
        confirmation.setItems(getConfirmationItems(confirmationId));
        enrichArrivalInfo(confirmation);
        return confirmation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiptSignature addSignature(Long confirmationId, ReceiptSignatureDTO signatureDTO) {
        ReceiptConfirmation confirmation = confirmationMapper.selectById(confirmationId);
        if (confirmation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货确认单不存在");
        }
        if (!SIGNER_RECEIVER.equals(signatureDTO.getSignerType())
                && !SIGNER_SUPPLIER.equals(signatureDTO.getSignerType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "签名方类型仅支持 RECEIVER/SUPPLIER");
        }
        String image = signatureDTO.getSignatureImage();
        if (!StringUtils.hasText(image) || !image.startsWith("data:image/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "签名图片格式不正确");
        }

        String signerName;
        if (SIGNER_RECEIVER.equals(signatureDTO.getSignerType())) {
            // 收货方签名人由后端取当前登录用户，前端传入值不生效
            Long userId = SecurityUtils.getCurrentUserId();
            signerName = null;
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    signerName = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getUsername();
                }
            }
            if (!StringUtils.hasText(signerName)) {
                signerName = "收货人";
            }
        } else {
            signerName = signatureDTO.getSignerName();
            if (!StringUtils.hasText(signerName)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写供应商/司机签名人姓名");
            }
        }

        ReceiptSignature signature = new ReceiptSignature();
        signature.setConfirmationId(confirmationId);
        signature.setSignerType(signatureDTO.getSignerType());
        signature.setSignerName(signerName);
        signature.setSignatureImage(image);
        signature.setSignTime(LocalDateTime.now());
        receiptSignatureMapper.insert(signature);
        log.info("收货确认单签名存证：confirmationId={}, signerType={}, signerName={}",
                confirmationId, signatureDTO.getSignerType(), signerName);
        return signature;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptSignature> listSignatures(Long confirmationId) {
        LambdaQueryWrapper<ReceiptSignature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReceiptSignature::getConfirmationId, confirmationId)
                .orderByAsc(ReceiptSignature::getSignTime);
        return receiptSignatureMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public ReceiptConfirmation recordPrint(Long confirmationId) {
        ReceiptConfirmation confirmation = confirmationMapper.selectById(confirmationId);
        if (confirmation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "确认单不存在");
        }
        Integer current = confirmation.getPrintCount() == null ? 0 : confirmation.getPrintCount();
        if (current >= MAX_PRINT_COUNT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("该确认单已打印 %d 次，达到上限，禁止继续打印", current));
        }
        int next = current + 1;
        ReceiptConfirmation update = new ReceiptConfirmation();
        update.setConfirmationId(confirmationId);
        update.setPrintCount(next);
        update.setLastPrintTime(LocalDateTime.now());
        confirmationMapper.updateById(update);
        confirmation.setPrintCount(next);
        confirmation.setLastPrintTime(update.getLastPrintTime());
        // 打印日志表：每次打印一行，供查验接口追溯
        ReceiptPrintLog logRow = new ReceiptPrintLog();
        logRow.setConfirmationId(confirmationId);
        logRow.setOperatorUserId(SecurityUtils.getCurrentUserId());
        logRow.setOperatorName(SecurityUtils.getCurrentUsername());
        logRow.setPrintTime(update.getLastPrintTime());
        logRow.setSourceIp(IpUtils.getClientIp(currentRequest()));
        receiptPrintLogMapper.insert(logRow);
        log.info("确认单打印登记：confirmationId={}, code={}, printCount={}, operator={}, ip={}",
                confirmationId, confirmation.getConfirmationCode(), next,
                logRow.getOperatorName(), logRow.getSourceIp());
        return confirmation;
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptVerifyVO verifyByCode(String verifyCode) {
        ReceiptVerifyVO vo = new ReceiptVerifyVO();
        if (verifyCode == null || verifyCode.trim().isEmpty()) {
            vo.setValid(false);
            vo.setMessage("请输入防伪码");
            return vo;
        }
        String code = verifyCode.trim().toUpperCase();
        ReceiptConfirmation confirmation = confirmationMapper.selectOne(
                new LambdaQueryWrapper<ReceiptConfirmation>()
                        .eq(ReceiptConfirmation::getVerifyCode, code));
        if (confirmation == null) {
            vo.setValid(false);
            vo.setMessage("防伪码不存在，请核对后重试");
            return vo;
        }
        vo.setValid(true);
        vo.setConfirmationCode(confirmation.getConfirmationCode());
        vo.setVerifyCode(confirmation.getVerifyCode());
        PurchaseArrival arrival = confirmation.getArrivalId() != null
                ? arrivalMapper.selectById(confirmation.getArrivalId()) : null;
        String supplierName = null;
        if (arrival != null) {
            supplierName = StringUtils.hasText(arrival.getSupplierName()) ? arrival.getSupplierName()
                    : (arrival.getSupplierId() != null && supplierMapper.selectById(arrival.getSupplierId()) != null
                        ? supplierMapper.selectById(arrival.getSupplierId()).getSupplierName() : null);
        }
        vo.setSupplierName(supplierName);
        vo.setConfirmTime(confirmation.getConfirmTime());
        vo.setTotalAmount(confirmation.getTotalAmount());
        vo.setTotalQuantity(confirmation.getTotalQuantity());
        vo.setPrintCount(confirmation.getPrintCount() == null ? 0 : confirmation.getPrintCount());
        vo.setLastPrintTime(confirmation.getLastPrintTime());
        vo.setPrintLogs(receiptPrintLogMapper.selectList(
                new LambdaQueryWrapper<ReceiptPrintLog>()
                        .eq(ReceiptPrintLog::getConfirmationId, confirmation.getConfirmationId())
                        .orderByAsc(ReceiptPrintLog::getPrintTime)));
        return vo;
    }

    private List<ReceiptConfirmationItem> getConfirmationItems(Long confirmationId) {
        LambdaQueryWrapper<ReceiptConfirmationItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReceiptConfirmationItem::getConfirmationId, confirmationId);
        List<ReceiptConfirmationItem> items = confirmationItemMapper.selectList(wrapper);
        // 填充留证照片（按明细分组）
        LambdaQueryWrapper<ReceiptEvidence> evidenceWrapper = new LambdaQueryWrapper<>();
        evidenceWrapper.eq(ReceiptEvidence::getConfirmationId, confirmationId);
        List<ReceiptEvidence> evidences = receiptEvidenceMapper.selectList(evidenceWrapper);
        for (ReceiptConfirmationItem item : items) {
            List<ReceiptEvidence> itemEvidences = new ArrayList<>();
            for (ReceiptEvidence evidence : evidences) {
                if (item.getConfirmationItemId().equals(evidence.getConfirmationItemId())) {
                    itemEvidences.add(evidence);
                }
            }
            if (!itemEvidences.isEmpty()) {
                item.setEvidence(itemEvidences);
            }
        }
        return items;
    }

    /**
     * 留证校验：拒收数量>0 的明细必须有现场照片证据（QC_REJECT）
     */
    private void validateEvidence(ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO) {
        BigDecimal rejectedQty = itemDTO.getRejectedQuantity() != null ? itemDTO.getRejectedQuantity() : BigDecimal.ZERO;
        if (rejectedQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<ReceiptConfirmationCreateDTO.ReceiptEvidenceDTO> evidences = itemDTO.getEvidence();
        if (evidences == null || evidences.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, String.format("明细[arrivalItemId=%s]存在拒收数量，必须上传现场照片留证", itemDTO.getArrivalItemId()));
        }
        boolean hasPhoto = evidences.stream().anyMatch(e -> e != null && StringUtils.hasText(e.getPhotoUrl()));
        if (!hasPhoto) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, String.format("明细[arrivalItemId=%s]拒收照片无效", itemDTO.getArrivalItemId()));
        }
    }

    /**
     * 保存留证照片（含 Exif 元数据：拍摄时间/设备/GPS）
     */
    private void saveEvidence(Long confirmationId, ReceiptConfirmationItem confirmationItem,
                              ReceiptConfirmationCreateDTO.ReceiptConfirmationItemDTO itemDTO) {
        List<ReceiptConfirmationCreateDTO.ReceiptEvidenceDTO> evidences = itemDTO.getEvidence();
        if (evidences == null || evidences.isEmpty()) {
            return;
        }
        for (ReceiptConfirmationCreateDTO.ReceiptEvidenceDTO evidenceDTO : evidences) {
            if (evidenceDTO == null || !StringUtils.hasText(evidenceDTO.getPhotoUrl())) {
                continue;
            }
            ReceiptEvidence evidence = new ReceiptEvidence();
            evidence.setConfirmationId(confirmationId);
            evidence.setConfirmationItemId(confirmationItem.getConfirmationItemId());
            evidence.setArrivalItemId(confirmationItem.getArrivalItemId());
            evidence.setEvidenceType(StringUtils.hasText(evidenceDTO.getEvidenceType())
                    ? evidenceDTO.getEvidenceType() : "QC_REJECT");
            evidence.setPhotoUrl(evidenceDTO.getPhotoUrl());
            evidence.setPhotoTakenAt(evidenceDTO.getPhotoTakenAt());
            evidence.setDeviceModel(evidenceDTO.getDeviceModel());
            evidence.setGps(evidenceDTO.getGps());
            receiptEvidenceMapper.insert(evidence);
            log.info("收货留证照片保存：confirmationId={}, itemId={}, type={}, url={}",
                    confirmationId, confirmationItem.getConfirmationItemId(), evidence.getEvidenceType(), evidenceDTO.getPhotoUrl());
        }
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

    /**
     * 门店收货补充门店ID：
     * 部分历史到货单/采购单未落门店ID，而 store_inventory.store_id 为 NOT NULL，
     * 直接写入会导致数据库约束异常（表现为“系统繁忙”）。此处回退到默认启用门店。
     */
    private void resolveStoreIdIfMissing(PurchaseArrival arrival) {
        if (!RECEIVER_STORE.equals(arrival.getReceiverType()) || StringUtils.hasText(arrival.getStoreId())) {
            return;
        }
        String fallback = resolveDefaultStoreId();
        if (!StringUtils.hasText(fallback)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门店收货缺少门店信息，请先在系统设置中维护门店");
        }
        log.warn("到货单[{}]门店ID为空，回退默认门店[{}]进行门店收货", arrival.getArrivalCode(), fallback);
        arrival.setStoreId(fallback);
    }

    private String resolveDefaultStoreId() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getStatus, "active")
                .orderByAsc(Store::getStoreId)
                .last("LIMIT 1"));
        return stores.isEmpty() ? null : stores.get(0).getStoreId();
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
        confirmation.setBarcode(createDTO.getBarcode());
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
        item.setIssueType(itemDTO.getIssueType());
        item.setQualityCheckResult(itemDTO.getQualityCheckResult());
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

        // 手持整单验收：一张确认单覆盖到货单全部明细（接收/拒收/差额拒收），确认即完结
        arrival.setReceivedQuantity(totalReceived);
        arrival.setConfirmTime(LocalDateTime.now());
        arrival.setStatus(ARRIVAL_STATUS_RECEIVED);
        if (arrival.getActualArrivalDate() == null) {
            arrival.setActualArrivalDate(LocalDate.now());
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

    /** 解析 yyyy-MM-dd 为当天开始时间；非法时返回 null（忽略该条件） */
    private LocalDateTime parseStartOfDay(String dateStr) {
        try {
            return LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            log.warn("确认时间范围-起 解析失败: {}", dateStr);
            return null;
        }
    }

    /** 解析 yyyy-MM-dd 为当天结束时间（23:59:59.999）；非法时返回 null（忽略该条件） */
    private LocalDateTime parseEndOfDay(String dateStr) {
        try {
            return LocalDate.parse(dateStr).atTime(23, 59, 59, 999_000_000);
        } catch (Exception e) {
            log.warn("确认时间范围-止 解析失败: {}", dateStr);
            return null;
        }
    }

    /** 生成 16 位防伪码：SecureRandom 纯随机，排除易混淆字符 0O1I */
    private static final String VERIFY_CODE_CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private String generateVerifyCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(VERIFY_CODE_CHARSET.charAt(random.nextInt(VERIFY_CODE_CHARSET.length())));
        }
        return sb.toString();
    }

    /** 获取当前请求（用于记录来源IP）；无请求上下文时返回 null */
    private jakarta.servlet.http.HttpServletRequest currentRequest() {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
