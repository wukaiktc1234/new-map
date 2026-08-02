package com.foodtraceability.service.purchase.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.purchase.PurchasePlanCreateDTO;
import com.foodtraceability.dto.purchase.PurchasePlanItemDTO;
import com.foodtraceability.dto.purchase.PurchasePlanQueryDTO;
import com.foodtraceability.dto.purchase.PurchasePlanUpdateDTO;
import com.foodtraceability.dto.purchase.PurchasePlanVO;
import com.foodtraceability.entity.PurchasePlan;
import com.foodtraceability.entity.PurchasePlanItem;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.StoreInventory;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchasePlanItemMapper;
import com.foodtraceability.mapper.PurchasePlanMapper;
import com.foodtraceability.mapper.StoreInventoryMapper;
import com.foodtraceability.service.purchase.PurchasePlanService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * 采购计划服务实现
 *
 * <p>核心业务：
 * <ul>
 *   <li>分页查询：支持按计划编号/状态/部门/日期范围/关键词过滤</li>
 *   <li>创建：自动生成 planNo（PL+yyyyMMdd+3位序号）+ 计算总金额/物料项数 + 批量插入明细</li>
 *   <li>更新：仅草稿状态可更新，先删后插覆盖明细</li>
 *   <li>删除：仅草稿/已拒绝状态可删除（逻辑删除主表+明细）</li>
 *   <li>审批流：submit(0→1) / approve(1→2) / reject(1→5)，记录审批人和时间</li>
 * </ul>
 * </p>
 */
@Service
public class PurchasePlanServiceImpl extends ServiceImpl<PurchasePlanMapper, PurchasePlan> implements PurchasePlanService {

    private static final Logger log = LoggerFactory.getLogger(PurchasePlanServiceImpl.class);

    /** 状态编码常量 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_EXECUTING = 3;
    private static final int STATUS_COMPLETED = 4;
    private static final int STATUS_REJECTED = 5;

    /** 采购订单状态：已完成（与 PurchaseOrderServiceImpl.STATUS_COMPLETED 对齐） */
    private static final int ORDER_STATUS_COMPLETED = 4;

    private final PurchasePlanMapper planMapper;
    private final PurchasePlanItemMapper itemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final StoreInventoryMapper storeInventoryMapper;
    private final MaterialArchiveMapper materialArchiveMapper;

    public PurchasePlanServiceImpl(PurchasePlanMapper planMapper,
                                    PurchasePlanItemMapper itemMapper,
                                    PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderItemMapper purchaseOrderItemMapper,
                                    StoreInventoryMapper storeInventoryMapper,
                                    MaterialArchiveMapper materialArchiveMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.storeInventoryMapper = storeInventoryMapper;
        this.materialArchiveMapper = materialArchiveMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<PurchasePlanVO> getPlanPage(PurchasePlanQueryDTO queryDTO) {
        Page<PurchasePlan> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<PurchasePlan> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StringUtils.hasText(queryDTO.getPlanNo())) {
            wrapper.like(PurchasePlan::getPlanNo, queryDTO.getPlanNo());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(PurchasePlan::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getDepartmentId() != null) {
            wrapper.eq(PurchasePlan::getDepartmentId, queryDTO.getDepartmentId());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(PurchasePlan::getPlanDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(PurchasePlan::getPlanDate, queryDTO.getEndDate());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(PurchasePlan::getPlanNo, queryDTO.getKeyword())
                    .or().like(PurchasePlan::getRemark, queryDTO.getKeyword()));
        }

        wrapper.orderByDesc(PurchasePlan::getCreateTime);
        IPage<PurchasePlan> planPage = planMapper.selectPage(page, wrapper);

        // 转换为 VO（列表不含明细，items=null）
        Page<PurchasePlanVO> voPage = new Page<>(planPage.getCurrent(), planPage.getSize(), planPage.getTotal());
        List<PurchasePlanVO> voList = planPage.getRecords().stream()
                .map(this::convertToVOWithoutItems)
                .toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchasePlanVO getPlanDetail(Long id) {
        PurchasePlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        PurchasePlanVO vo = convertToVOWithoutItems(plan);

        // 查询明细
        LambdaQueryWrapper<PurchasePlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchasePlanItem::getPlanId, id)
                .orderByAsc(PurchasePlanItem::getItemId);
        List<PurchasePlanItem> items = itemMapper.selectList(itemWrapper);
        vo.setItems(items.stream().map(this::convertItemToVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchasePlanVO createPlan(PurchasePlanCreateDTO createDTO) {
        // 生成计划编号
        String planNo = generatePlanNo();

        // 计算总金额和物料项数
        long totalAmountFen = 0L;
        for (PurchasePlanItemDTO item : createDTO.getItems()) {
            BigDecimal qty = item.getQuantity();
            long priceFen = item.getEstimatedPrice();
            // totalAmountFen += qty * priceFen（精度：qty 保留3位小数，priceFen 是整数）
            // 用 BigDecimal 计算后转为 long
            BigDecimal itemTotal = qty.multiply(BigDecimal.valueOf(priceFen));
            totalAmountFen += itemTotal.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }

        // 构建主表实体
        PurchasePlan plan = new PurchasePlan();
        plan.setPlanNo(planNo);
        plan.setPlanDate(createDTO.getPlanDate());
        plan.setDepartmentId(createDTO.getDepartmentId());
        plan.setDepartmentName(createDTO.getDepartmentName());
        plan.setTotalAmount(totalAmountFen);
        plan.setItemCount(createDTO.getItems().size());
        plan.setStatus(STATUS_DRAFT);
        plan.setRemark(createDTO.getRemark());
        // creatorId/creatorName 由 Controller 从 SecurityContext 注入（本期留空，待接入认证上下文）
        plan.setCreatorId(null);
        plan.setCreatorName(null);

        planMapper.insert(plan);
        log.info("创建采购计划成功，planNo={}, itemCount={}, totalAmount={}分",
                planNo, createDTO.getItems().size(), totalAmountFen);

        // 批量插入明细
        List<PurchasePlanItem> itemEntities = new ArrayList<>(createDTO.getItems().size());
        for (PurchasePlanItemDTO itemDTO : createDTO.getItems()) {
            PurchasePlanItem item = convertDTOToItem(itemDTO);
            item.setPlanId(plan.getPlanId());
            itemEntities.add(item);
        }
        for (PurchasePlanItem item : itemEntities) {
            itemMapper.insert(item);
        }

        // 返回 VO（含明细）
        PurchasePlanVO vo = convertToVOWithoutItems(plan);
        vo.setItems(itemEntities.stream().map(this::convertItemToVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchasePlanVO updatePlan(Long id, PurchasePlanUpdateDTO updateDTO) {
        PurchasePlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅草稿状态的计划可更新");
        }

        // 重新计算总金额
        long totalAmountFen = 0L;
        for (PurchasePlanItemDTO item : updateDTO.getItems()) {
            BigDecimal itemTotal = item.getQuantity().multiply(BigDecimal.valueOf(item.getEstimatedPrice()));
            totalAmountFen += itemTotal.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }

        // 更新主表
        existing.setDepartmentId(updateDTO.getDepartmentId());
        existing.setDepartmentName(updateDTO.getDepartmentName());
        existing.setPlanDate(updateDTO.getPlanDate());
        existing.setRemark(updateDTO.getRemark());
        existing.setTotalAmount(totalAmountFen);
        existing.setItemCount(updateDTO.getItems().size());
        planMapper.updateById(existing);

        // 覆盖式更新明细：先逻辑删除旧明细，再插入新明细
        LambdaQueryWrapper<PurchasePlanItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PurchasePlanItem::getPlanId, id);
        itemMapper.delete(deleteWrapper);

        for (PurchasePlanItemDTO itemDTO : updateDTO.getItems()) {
            PurchasePlanItem item = convertDTOToItem(itemDTO);
            item.setPlanId(id);
            itemMapper.insert(item);
        }

        log.info("更新采购计划成功，id={}, itemCount={}", id, updateDTO.getItems().size());

        // 返回最新 VO
        return getPlanDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        PurchasePlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (existing.getStatus() != STATUS_DRAFT && existing.getStatus() != STATUS_REJECTED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅草稿或已拒绝状态的计划可删除");
        }

        // 逻辑删除明细
        LambdaQueryWrapper<PurchasePlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchasePlanItem::getPlanId, id);
        itemMapper.delete(itemWrapper);

        // 逻辑删除主表
        planMapper.deleteById(id);
        log.info("删除采购计划成功，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPlan(Long id) {
        PurchasePlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅草稿状态的计划可提交审批");
        }
        existing.setStatus(STATUS_PENDING);
        planMapper.updateById(existing);
        log.info("采购计划提交审批成功，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePlan(Long id, String approveBy) {
        PurchasePlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (existing.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅待审批状态的计划可审批通过");
        }
        existing.setStatus(STATUS_APPROVED);
        existing.setApproveBy(StringUtils.hasText(approveBy) ? approveBy : "当前用户");
        existing.setApproveTime(LocalDateTime.now());
        existing.setRejectReason(null);
        planMapper.updateById(existing);
        log.info("采购计划审批通过，id={}, approveBy={}", id, approveBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectPlan(Long id, String reason) {
        PurchasePlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (existing.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅待审批状态的计划可拒绝");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "拒绝原因不能为空");
        }
        existing.setStatus(STATUS_REJECTED);
        existing.setRejectReason(reason);
        existing.setApproveTime(LocalDateTime.now());
        planMapper.updateById(existing);
        log.info("采购计划审批拒绝，id={}，reason={}", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executePlan(Long id) {
        PurchasePlan existing = planMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (existing.getStatus() != STATUS_APPROVED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已审批状态的计划可开始执行");
        }
        existing.setStatus(STATUS_EXECUTING);
        existing.setApproveTime(LocalDateTime.now());
        planMapper.updateById(existing);
        log.info("采购计划开始执行，id={}", id);
    }

    /**
     * 采购订单入库完成后回写计划状态（executing(3) → completed(4)）
     *
     * <p>不使用 @Transactional：需加入调用方事务（confirmStockin）以读取未提交的订单状态更新；
     * 调用方负责 try/catch 异常隔离。</p>
     */
    @Override
    public void markPlanCompletedIfNeeded(Long planId) {
        if (planId == null) {
            return;
        }
        PurchasePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            log.warn("采购计划状态回写：计划不存在，planId={}", planId);
            return;
        }
        // 幂等：仅执行中状态才检查
        if (plan.getStatus() == null || plan.getStatus() != STATUS_EXECUTING) {
            return;
        }

        // 查询关联的所有采购订单（@TableLogic 自动过滤 deleted=0）
        LambdaQueryWrapper<PurchaseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PurchaseOrder::getPlanId, planId);
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(orderWrapper);

        if (orders.isEmpty()) {
            return;
        }

        // 检查所有订单是否都已完成
        for (PurchaseOrder order : orders) {
            if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_STATUS_COMPLETED) {
                return;
            }
        }

        // 全部完成，更新计划状态为已完成
        plan.setStatus(STATUS_COMPLETED);
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);
        log.info("采购计划状态回写为已完成，planId={}, planNo={}, 关联订单数={}",
                planId, plan.getPlanNo(), orders.size());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据采购计划生成采购订单（草稿状态）
     *
     * <p>业务链：采购计划（已审批/执行中）→ 生成采购订单（草稿）。
     * 订单记录来源计划信息（requestNo=planNo / sourceType=purchase_plan），支持追溯。</p>
     *
     * <p>幂等性：若该计划已生成过订单，直接返回既有订单，不重复生成。</p>
     *
     * @param planId 计划ID
     * @return 新建（或既有）的采购订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder generateOrder(Long planId) {
        PurchasePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购计划不存在");
        }
        if (plan.getStatus() != STATUS_APPROVED && plan.getStatus() != STATUS_EXECUTING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已审批/执行中的计划可生成采购订单");
        }

        // 幂等：已生成过订单则直接返回
        LambdaQueryWrapper<PurchaseOrder> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(PurchaseOrder::getRequestNo, plan.getPlanNo());
        PurchaseOrder existing = purchaseOrderMapper.selectOne(existWrapper);
        if (existing != null) {
            log.info("采购计划已生成过订单，直接返回：planNo={}, orderId={}", plan.getPlanNo(), existing.getOrderId());
            return existing;
        }

        String orderCode = generateOrderNo();
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderCode(orderCode);
        order.setOrderIdStr(orderCode);
        order.setOrderNo(orderCode);
        order.setOrderDate(LocalDate.now());
        order.setDeleted(0);
        order.setVersion(0);
        // 来源计划信息追溯
        order.setRequestId(String.valueOf(plan.getPlanId()));
        order.setRequestNo(plan.getPlanNo());
        order.setSourceType("purchase_plan");
        order.setPriority("normal");
        // 集中式单店：默认门店 1
        order.setStoreId(1L);
        order.setRemark(plan.getRemark());
        order.setSupplierId(null);
        order.setWarehouseId(null);
        order.setOrderStatus(0); // 草稿
        order.setExpectedDate(plan.getPlanDate());

        LambdaQueryWrapper<PurchasePlanItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchasePlanItem::getPlanId, planId);
        List<PurchasePlanItem> planItems = itemMapper.selectList(itemWrapper);

        Long totalAmount = 0L;
        List<PurchaseOrderItem> orderItems = new ArrayList<>();
        for (PurchasePlanItem planItem : planItems) {
            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            orderItem.setItemIdStr(UUID.randomUUID().toString().replace("-", ""));
            try {
                orderItem.setMaterialId(Long.parseLong(planItem.getMaterialId()));
            } catch (NumberFormatException e) {
                log.warn("计划明细 materialId 无法转换为 Long: {}", planItem.getMaterialId());
                orderItem.setMaterialId(0L);
            }
            orderItem.setMaterialName(planItem.getMaterialName());
            orderItem.setSpecification(planItem.getSpecification());
            orderItem.setUnit(planItem.getUnit() != null ? planItem.getUnit() : "个");
            orderItem.setQuantity(planItem.getQuantity());
            if (planItem.getEstimatedPrice() != null) {
                orderItem.setUnitPrice(planItem.getEstimatedPrice());
            }
            BigDecimal subtotal = planItem.getQuantity() != null
                    ? planItem.getQuantity().multiply(BigDecimal.valueOf(planItem.getEstimatedPrice() != null ? planItem.getEstimatedPrice() : 0L))
                    : BigDecimal.ZERO;
            orderItem.setAmount(subtotal.setScale(0, RoundingMode.HALF_UP).longValueExact());
            orderItem.setCreateTime(LocalDateTime.now());
            orderItem.setUpdateTime(LocalDateTime.now());
            orderItems.add(orderItem);
            totalAmount += orderItem.getAmount();
        }
        order.setTotalAmount(totalAmount);
        purchaseOrderMapper.insert(order);
        for (PurchaseOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getOrderId());
            purchaseOrderItemMapper.insert(orderItem);
        }
        log.info("采购计划生成采购订单成功：planNo={}, orderNo={}, itemCount={}", plan.getPlanNo(), orderCode, orderItems.size());
        return order;
    }

    /**
     * 生成订单编号：PO + yyyyMMdd + 4位序号（与采购申请转单一致）
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO" + dateStr;
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseOrder::getOrderNo, prefix);
        wrapper.orderByDesc(PurchaseOrder::getOrderNo);
        wrapper.last("LIMIT 1");
        PurchaseOrder last = purchaseOrderMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getOrderNo() != null) {
            String lastNo = last.getOrderNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {
                // 保留默认序号 1
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 生成计划编号：PL + yyyyMMdd + 3位序号
     */
    private String generatePlanNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PL" + dateStr;

        LambdaQueryWrapper<PurchasePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchasePlan::getPlanNo, prefix);
        wrapper.orderByDesc(PurchasePlan::getPlanNo);
        wrapper.last("LIMIT 1");
        PurchasePlan lastPlan = planMapper.selectOne(wrapper);

        int seq = 1;
        if (lastPlan != null && lastPlan.getPlanNo() != null) {
            String lastNo = lastPlan.getPlanNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    /**
     * Entity → VO（不含明细）
     */
    private PurchasePlanVO convertToVOWithoutItems(PurchasePlan plan) {
        PurchasePlanVO vo = new PurchasePlanVO();
        vo.setPlanId(plan.getPlanId());
        vo.setPlanNo(plan.getPlanNo());
        vo.setPlanDate(plan.getPlanDate());
        vo.setDepartmentId(plan.getDepartmentId());
        vo.setDepartmentName(plan.getDepartmentName());
        vo.setTotalAmount(plan.getTotalAmount());
        vo.setItemCount(plan.getItemCount());
        vo.setCreatorId(plan.getCreatorId());
        vo.setCreatorName(plan.getCreatorName());
        vo.setStatus(plan.getStatus());
        vo.setRemark(plan.getRemark());
        vo.setApproveBy(plan.getApproveBy());
        vo.setApproveTime(plan.getApproveTime());
        vo.setRejectReason(plan.getRejectReason());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());
        return vo;
    }

    /**
     * Item Entity → Item VO
     */
    private PurchasePlanVO.PurchasePlanItemVO convertItemToVO(PurchasePlanItem item) {
        PurchasePlanVO.PurchasePlanItemVO vo = new PurchasePlanVO.PurchasePlanItemVO();
        vo.setItemId(item.getItemId());
        vo.setMaterialId(item.getMaterialId());
        vo.setMaterialName(item.getMaterialName());
        vo.setSpecification(item.getSpecification());
        vo.setQuantity(item.getQuantity());
        vo.setUnit(item.getUnit());
        vo.setEstimatedPrice(item.getEstimatedPrice());
        vo.setIsTempMaterial(item.getIsTempMaterial());
        vo.setSupplierId(item.getSupplierId());
        vo.setSupplierName(item.getSupplierName());
        vo.setRemark(item.getRemark());
        return vo;
    }

    /**
     * Item DTO → Item Entity（未设置 planId，由调用方设置）
     */
    private PurchasePlanItem convertDTOToItem(PurchasePlanItemDTO dto) {
        PurchasePlanItem item = new PurchasePlanItem();
        item.setMaterialId(dto.getMaterialId());
        item.setMaterialName(dto.getMaterialName());
        item.setSpecification(dto.getSpecification());
        item.setQuantity(dto.getQuantity());
        item.setUnit(dto.getUnit());
        item.setEstimatedPrice(dto.getEstimatedPrice());
        item.setIsTempMaterial(dto.getIsTempMaterial() != null ? dto.getIsTempMaterial() : 0);
        item.setSupplierId(dto.getSupplierId());
        item.setSupplierName(dto.getSupplierName());
        item.setRemark(dto.getRemark());
        return item;
    }

    /**
     * 从库存预警自动生成采购计划（草稿状态）
     *
     * <p>扫描 store_inventory 中 safety_stock &gt; 0 且当前库存低于安全库存的物料，
     * 建议采购量 = max_stock - current_stock（未设置 max_stock 时按 safety_stock×2 - current_stock），
     * 至少为 1；预估单价取库存最新成本价（unit_cost），无则 0（待人工填写）。</p>
     *
     * @return 新建的计划（含明细）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchasePlanVO generateFromStock() {
        LambdaQueryWrapper<StoreInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreInventory::getDeleted, 0)
                .gt(StoreInventory::getSafetyStock, BigDecimal.ZERO)
                .apply("current_stock < safety_stock")
                .orderByAsc(StoreInventory::getMaterialId);
        List<StoreInventory> lowStockItems = storeInventoryMapper.selectList(wrapper);
        if (lowStockItems.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无库存低于安全库存的物料，无需生成采购计划");
        }

        // 去重：同一物料多次出现时按最小库存合并（取库存最低的店）
        Map<Long, StoreInventory> merged = new java.util.LinkedHashMap<>();
        for (StoreInventory inv : lowStockItems) {
            StoreInventory existing = merged.get(inv.getMaterialId());
            if (existing == null || inv.getCurrentStock().compareTo(existing.getCurrentStock()) < 0) {
                merged.put(inv.getMaterialId(), inv);
            }
        }

        String planNo = generatePlanNo();
        PurchasePlan plan = new PurchasePlan();
        plan.setPlanNo(planNo);
        plan.setPlanDate(java.time.LocalDate.now());
        plan.setDepartmentId(null);
        plan.setDepartmentName("自动生成");
        plan.setRemark("由库存预警自动生成，请人工核对后提交审批");
        plan.setStatus(STATUS_DRAFT);
        plan.setCreatorId(null);
        plan.setCreatorName(null);
        planMapper.insert(plan);

        long totalAmountFen = 0L;
        List<PurchasePlanItem> itemEntities = new ArrayList<>();
        for (StoreInventory inv : merged.values()) {
            BigDecimal suggestQty = inv.getMaxStock() != null && inv.getMaxStock().compareTo(BigDecimal.ZERO) > 0
                    ? inv.getMaxStock().subtract(inv.getCurrentStock())
                    : inv.getSafetyStock().multiply(new BigDecimal("2")).subtract(inv.getCurrentStock());
            if (suggestQty.compareTo(BigDecimal.ONE) < 0) {
                suggestQty = BigDecimal.ONE;
            }
            PurchasePlanItem item = new PurchasePlanItem();
            item.setPlanId(plan.getPlanId());
            item.setMaterialId(String.valueOf(inv.getMaterialId()));
            item.setMaterialName(inv.getMaterialName());
            item.setUnit(inv.getUnit());
            item.setQuantity(suggestQty);
            item.setEstimatedPrice(inv.getUnitCost() != null ? inv.getUnitCost() : 0L);
            item.setIsTempMaterial(0);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            item.setDeleted(0);
            itemEntities.add(item);
            totalAmountFen += suggestQty.multiply(BigDecimal.valueOf(item.getEstimatedPrice()))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        for (PurchasePlanItem item : itemEntities) {
            itemMapper.insert(item);
        }

        plan.setTotalAmount(totalAmountFen);
        plan.setItemCount(itemEntities.size());
        planMapper.updateById(plan);
        log.info("库存预警生成采购计划：planNo={}, itemCount={}, totalAmount={}分", planNo, itemEntities.size(), totalAmountFen);

        PurchasePlanVO vo = convertToVOWithoutItems(plan);
        vo.setItems(itemEntities.stream().map(this::convertItemToVO).toList());
        return vo;
    }

    /**
     * 导出采购计划 Excel
     * @param queryDTO 查询条件（与列表一致）
     * @param response HTTP 响应
     */
    @Override
    public void exportPlans(PurchasePlanQueryDTO queryDTO, HttpServletResponse response) {
        // 复用列表查询条件，导出全部匹配记录
        Page<PurchasePlan> page = new Page<>(1, 10000);
        LambdaQueryWrapper<PurchasePlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getPlanNo())) {
            wrapper.like(PurchasePlan::getPlanNo, queryDTO.getPlanNo());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(PurchasePlan::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getDepartmentId() != null) {
            wrapper.eq(PurchasePlan::getDepartmentId, queryDTO.getDepartmentId());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(PurchasePlan::getPlanDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(PurchasePlan::getPlanDate, queryDTO.getEndDate());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(PurchasePlan::getPlanNo, queryDTO.getKeyword())
                    .or().like(PurchasePlan::getRemark, queryDTO.getKeyword()));
        }
        wrapper.orderByDesc(PurchasePlan::getCreateTime);
        List<PurchasePlan> plans = planMapper.selectList(wrapper);

        String[] headers = {"计划编号", "计划日期", "部门", "状态", "物料数", "总金额(元)", "备注"};
        java.util.Map<Integer, String> statusMap = java.util.Map.of(
                0, "草稿", 1, "待审批", 2, "已审批", 3, "执行中", 4, "已完成", 5, "已拒绝");

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("采购计划");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (PurchasePlan plan : plans) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(plan.getPlanNo() != null ? plan.getPlanNo() : "");
                row.createCell(1).setCellValue(plan.getPlanDate() != null ? plan.getPlanDate().toString() : "");
                row.createCell(2).setCellValue(plan.getDepartmentName() != null ? plan.getDepartmentName() : "");
                row.createCell(3).setCellValue(statusMap.getOrDefault(plan.getStatus(), String.valueOf(plan.getStatus())));
                row.createCell(4).setCellValue(plan.getItemCount() != null ? plan.getItemCount() : 0);
                row.createCell(5).setCellValue(plan.getTotalAmount() != null
                        ? BigDecimal.valueOf(plan.getTotalAmount()).divide(new BigDecimal("100")).doubleValue() : 0);
                row.createCell(6).setCellValue(plan.getRemark() != null ? plan.getRemark() : "");
            }
            setExcelResponseHeaders(response, "采购计划导出");
            workbook.write(response.getOutputStream());
            workbook.dispose();
        } catch (IOException e) {
            log.error("导出采购计划 Excel 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "导出采购计划失败: " + e.getMessage());
        }
        log.info("采购计划导出完成：共 {} 条", plans.size());
    }

    private void setExcelResponseHeaders(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8));
    }
}
