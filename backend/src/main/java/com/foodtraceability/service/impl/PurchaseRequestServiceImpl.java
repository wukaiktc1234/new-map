package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.PurchaseRequestCreateDTO;
import com.foodtraceability.dto.PurchaseRequestDTO;
import com.foodtraceability.dto.PurchaseRequestUpdateDTO;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.PurchaseRequest;
import com.foodtraceability.entity.PurchaseRequestItem;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseRequestItemMapper;
import com.foodtraceability.mapper.PurchaseRequestMapper;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.PurchaseRequestService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 采购申请服务实现类
 * 提供采购申请的增删改查、提交、审批、生成订单等业务逻辑
 */
@Service
public class PurchaseRequestServiceImpl extends ServiceImpl<PurchaseRequestMapper, PurchaseRequest> implements PurchaseRequestService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PurchaseRequestServiceImpl.class);
    private final PurchaseRequestItemMapper itemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final DataPermissionService dataPermissionService;
    private final StoreNewMapper storeNewMapper;

    /**
     * 分页查询采购申请
     * @param page 页码
     * @param size 每页条数
     * @param requestNo 申请单号（模糊查询）
     * @param status 状态
     * @param departmentId 部门ID
     * @param applicantId 申请人ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    @Override
    public IPage<PurchaseRequestDTO> getPage(int page, int size, String requestNo, String status, String departmentId, String applicantId, String departmentName, String applicantName, String startDate, String endDate) {
        Page<PurchaseRequest> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(requestNo)) {
            wrapper.like(PurchaseRequest::getRequestNo, requestNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseRequest::getStatus, status);
        }
        if (StringUtils.hasText(departmentId)) {
            wrapper.eq(PurchaseRequest::getDepartmentId, departmentId);
        }
        if (StringUtils.hasText(applicantId)) {
            wrapper.eq(PurchaseRequest::getApplicantId, applicantId);
        }
        if (StringUtils.hasText(departmentName)) {
            wrapper.like(PurchaseRequest::getDepartmentName, departmentName);
        }
        if (StringUtils.hasText(applicantName)) {
            wrapper.like(PurchaseRequest::getApplicantName, applicantName);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseRequest::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseRequest::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        // 按当前用户数据范围过滤（数据权限隔离）
        applyDataScopeFilter(wrapper);
        wrapper.orderByDesc(PurchaseRequest::getCreateTime);
        IPage<PurchaseRequest> result = page(pageParam, wrapper);
        return result.convert(this::convertToDTO);
    }

    /**
     * 根据ID获取采购申请详情
     * @param requestId 申请ID
     * @return 采购申请详情
     */
    @Override
    public PurchaseRequestDTO getById(String requestId) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, requestId));
        if (request == null) {
            return null;
        }
        PurchaseRequestDTO dto = convertToDTO(request);
        List<PurchaseRequestItem> items = itemMapper.selectByRequestId(requestId);
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseRequestDTO create(PurchaseRequestCreateDTO dto) {
        PurchaseRequest request = new PurchaseRequest();
        BeanUtils.copyProperties(dto, request);

        // 强制绑定当前登录人信息，防止前端伪造申请人/部门/门店
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            com.foodtraceability.entity.User user = userService.getById(currentUserId);
            if (user != null) {
                request.setApplicantId(String.valueOf(currentUserId));
                request.setApplicantName(user.getFullName());
                if (user.getDepartmentId() != null) {
                    request.setDepartmentId(String.valueOf(user.getDepartmentId()));
                    com.foodtraceability.entity.Department dept = departmentService.getDepartmentById(user.getDepartmentId());
                    if (dept != null) {
                        request.setDepartmentName(dept.getDepartmentName());
                    }
                }
                if (user.getStoreId() != null) {
                    request.setStoreId(resolveValidStoreId(String.valueOf(user.getStoreId())));
                }
            }
        }

        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setRequestNo(generateRequestNo());
        request.setStatus("draft");
        request.setRequestType(dto.getRequestType() != null ? dto.getRequestType() : "routine");
        request.setPriority(dto.getPriority() != null ? dto.getPriority() : "normal");
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        // totalAmount 为 Long 分，subtotal 为 BigDecimal 元，需乘以 100 后转 Long 累加
        Long totalAmount = 0L;
        List<PurchaseRequestItem> items = new ArrayList<>();
        for (PurchaseRequestCreateDTO.PurchaseRequestItemDTO itemDTO : dto.getItems()) {
            PurchaseRequestItem item = new PurchaseRequestItem();
            BeanUtils.copyProperties(itemDTO, item);
            item.setItemId(UUID.randomUUID().toString().replace("-", ""));
            item.setQuantity(itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ONE);
            item.setEstimatedPrice(itemDTO.getEstimatedPrice() != null ? itemDTO.getEstimatedPrice() : BigDecimal.ZERO);
            BigDecimal subtotal = item.getQuantity().multiply(item.getEstimatedPrice());
            item.setSubtotalAmount(subtotal);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            items.add(item);
            // 元转分：subtotal（元） * 100 → Long（分）
            totalAmount = totalAmount + subtotal.multiply(new BigDecimal("100")).longValue();
        }
        request.setTotalAmount(totalAmount);
        save(request);
        for (PurchaseRequestItem item : items) {
            item.setRequestId(request.getRequestId());
            itemMapper.insert(item);
        }
        return getById(request.getRequestId());
    }

    /**
     * 更新采购申请
     * @param dto 更新参数
     * @return 更新后的采购申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseRequestDTO update(PurchaseRequestUpdateDTO dto) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, dto.getRequestId()));
        if (request == null) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        if (!"draft".equals(request.getStatus()) && !"rejected".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_CANNOT_MODIFY);
        }
        if (StringUtils.hasText(dto.getTitle())) {
            request.setTitle(dto.getTitle());
        }
        if (dto.getRequestType() != null) {
            request.setRequestType(dto.getRequestType());
        }
        if (dto.getDepartmentId() != null) {
            request.setDepartmentId(dto.getDepartmentId());
            request.setDepartmentName(dto.getDepartmentName());
        }
        if (dto.getPriority() != null) {
            request.setPriority(dto.getPriority());
        }
        if (dto.getExpectedDate() != null) {
            request.setExpectedDate(dto.getExpectedDate());
        }
        if (dto.getDescription() != null) {
            request.setDescription(dto.getDescription());
        }
        request.setUpdateTime(LocalDateTime.now());
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            deleteItemsByRequestId(dto.getRequestId());
            // totalAmount 为 Long 分，subtotal 为 BigDecimal 元，需乘以 100 后转 Long 累加
            Long totalAmount = 0L;
            for (PurchaseRequestUpdateDTO.PurchaseRequestItemDTO itemDTO : dto.getItems()) {
                PurchaseRequestItem item = new PurchaseRequestItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setItemId(UUID.randomUUID().toString().replace("-", ""));
                item.setRequestId(dto.getRequestId());
                item.setQuantity(itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ONE);
                item.setEstimatedPrice(itemDTO.getEstimatedPrice() != null ? itemDTO.getEstimatedPrice() : BigDecimal.ZERO);
                BigDecimal subtotal = item.getQuantity().multiply(item.getEstimatedPrice());
                item.setSubtotalAmount(subtotal);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                itemMapper.insert(item);
                // 元转分：subtotal（元） * 100 → Long（分）
                totalAmount = totalAmount + subtotal.multiply(new BigDecimal("100")).longValue();
            }
            request.setTotalAmount(totalAmount);
        }
        updateById(request);
        return getById(dto.getRequestId());
    }

    /**
     * 删除采购申请（逻辑删除）
     * @param requestId 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String requestId) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, requestId));
        if (request == null) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        if (!"draft".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_CANNOT_DELETE);
        }
        removeById(requestId);
        deleteItemsByRequestId(requestId);
    }

    /**
     * 提交采购申请
     * @param requestId 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(String requestId) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, requestId));
        if (request == null) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        if (!"draft".equals(request.getStatus()) && !"rejected".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_CANNOT_SUBMIT);
        }
        // A1 驳回次数限制：被驳回 3 次后禁止重新提交，需管理员重置（单店模式老板即管理员，一键解锁）
        if ("rejected".equals(request.getStatus()) && request.getRejectCount() != null && request.getRejectCount() >= 3) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_CANNOT_SUBMIT, "该申请已被驳回 3 次，已限制重新提交，请联系管理员重置后可继续提交");
        }
        List<PurchaseRequestItem> items = itemMapper.selectByRequestId(requestId);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_ITEMS_EMPTY);
        }
        request.setStatus("pending");
        request.setUpdateTime(LocalDateTime.now());
        updateById(request);
    }

    /**
     * 审批采购申请
     * @param requestId 申请ID
     * @param status 审批状态
     * @param remark 审批备注
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String requestId, String status, String remark) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, requestId));
        if (request == null) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        if (!"pending".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_CANNOT_APPROVE);
        }
        request.setStatus(status);
        request.setUpdateTime(LocalDateTime.now());
        if ("rejected".equals(status)) {
            request.setRejectReason(remark);
            // A1 驳回次数累计：审批多次驳回同一单据时限制提交者重复提交
            request.setRejectCount((request.getRejectCount() == null ? 0 : request.getRejectCount()) + 1);
        }
        // 审批通过时记录审批人与审批时间（供申请详情展示）
        if ("approved".equals(status)) {
            request.setApprovedTime(LocalDateTime.now());
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                try {
                    com.foodtraceability.entity.User approver = userService.getById(currentUserId);
                    if (approver != null) {
                        request.setApprovedBy(approver.getFullName());
                    }
                } catch (Exception ignored) {
                    // 审批人姓名获取失败不影响审批主流程
                }
            }
        }
        updateById(request);
    }

    /**
     * 校验门店ID有效性：stores_new 中不存在时回落默认门店（1），防止生成订单时外键违反
     * @param storeId 原始门店ID（字符串）
     * @return 有效的门店ID字符串；无法解析时返回默认门店
     */
    private String resolveValidStoreId(String storeId) {
        if (storeId == null) {
            return null;
        }
        try {
            Long id = Long.parseLong(storeId.trim());
            if (storeNewMapper.selectById(id) != null) {
                return storeId.trim();
            }
            log.warn("采购申请 storeId={} 在 stores_new 中不存在，回落默认门店 1", storeId);
            return "1";
        } catch (NumberFormatException e) {
            log.warn("采购申请 storeId 无法解析为 Long: {}", storeId);
            return "1";
        }
    }

    /**
     * 根据采购申请生成采购订单
     *
     * <p>转单时会在采购订单上记录来源申请信息（requestId/requestNo/sourceType/priority），
     * 并携带明细的收货地点，支持后续按门店/仓库自动拆分到货单。</p>
     *
     * @param requestId 申请ID
     * @return 更新后的采购申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseRequestDTO generateOrder(String requestId) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, requestId));
        if (request == null) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        if (!"approved".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_CANNOT_GENERATE_ORDER);
        }
        List<PurchaseRequestItem> requestItems = itemMapper.selectByRequestId(requestId);
        if (requestItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_ITEMS_EMPTY);
        }
        PurchaseOrder order = new PurchaseOrder();
        // 注意：orderId 为自增主键，插入后自动生成，无需手动设置
        String orderCode = generateOrderNo();
        order.setOrderCode(orderCode);
        // order_id_str 和 order_no 在DB中为 NOT NULL，使用 orderCode 填充
        order.setOrderIdStr(orderCode);
        order.setOrderNo(orderCode);
        order.setOrderDate(LocalDate.now());
        order.setDeleted(0);
        order.setVersion(0);
        // 来源申请信息追溯
        order.setRequestId(request.getRequestId());
        order.setRequestNo(request.getRequestNo());
        order.setSourceType("purchase_request");
        order.setPriority(request.getPriority());
        if (request.getStoreId() != null) {
            String validStoreId = resolveValidStoreId(request.getStoreId());
            if (validStoreId != null) {
                order.setStoreId(Long.parseLong(validStoreId));
            }
        }
        order.setRemark(request.getDescription());
        order.setSupplierId(null);  // 采购申请转订单时需后续指定供应商
        order.setWarehouseId(null);  // 多收货地点场景由明细 plannedWarehouseId 决定，表头暂不指定
        // 注意：totalAmount 为 Long 类型（单位：分），在循环中累计后设置，此处不再设初值 0L（避免冗余）
        // 使用 orderStatus（Integer）替代 status（String）
        order.setOrderStatus(0);  // 0=草稿状态
        // 记录创建人（OA 化：从登录上下文绑定）
        Long creatorId = SecurityUtils.getCurrentUserId();
        if (creatorId != null) {
            order.setCreateUserId(creatorId);
            try {
                com.foodtraceability.entity.User creator = userService.getById(creatorId);
                if (creator != null) {
                    order.setCreateByName(creator.getFullName());
                }
            } catch (Exception ignored) {
            }
        }
        // 注意：expectedDate 为 LocalDate 类型，无需调用 atStartOfDay()
        order.setExpectedDate(request.getExpectedDate());
        // 注意：createTime/updateTime 由 MyBatis-Plus 自动填充，无需手动设置
        Long totalAmount = 0L;  // 单位：分
        List<PurchaseOrderItem> orderItems = new ArrayList<>();
        for (PurchaseRequestItem requestItem : requestItems) {
            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            // 注意：itemId 为自增主键，插入后自动生成
            orderItem.setItemIdStr(UUID.randomUUID().toString().replace("-", ""));
            // 使用 materialId/materialName 替代 foodId/foodName（新实体类字段）
            // 注意：materialId 为 Long 类型，foodId 为 String，需转换
            // 修复（PR-4）：转换失败时使用 0L 占位而非 null，避免 NOT NULL 约束失败；
            // 同时记录 warn 日志，materialName 仍设置为原 foodName，便于人工核对
            try {
                orderItem.setMaterialId(Long.parseLong(requestItem.getFoodId()));
            } catch (NumberFormatException e) {
                log.warn("foodId 无法转换为 materialId，使用 0L 占位: foodId={}", requestItem.getFoodId());
                orderItem.setMaterialId(0L);
            }
            orderItem.setMaterialName(requestItem.getFoodName());
            orderItem.setSpecification(requestItem.getSpecification());
            // 修复（PR-4）：设置 unit 字段，避免 NOT NULL 约束失败
            // （PurchaseOrderItem.unit 为 NOT NULL，原代码未设置导致插入失败）
            orderItem.setUnit(requestItem.getUnit() != null ? requestItem.getUnit() : "个");
            // quantity 保持 BigDecimal 类型（两实体类一致）
            orderItem.setQuantity(requestItem.getQuantity());
            // 注意：unitPrice 为 Long 类型（单位：分），estimatedPrice 为 BigDecimal（元），需转换
            if (requestItem.getEstimatedPrice() != null) {
                orderItem.setUnitPrice(requestItem.getEstimatedPrice().multiply(new BigDecimal("100")).longValue());
            }
            // 注意：使用 amount（Long）替代 subtotalAmount（BigDecimal）（新实体类字段）
            if (requestItem.getSubtotalAmount() != null) {
                orderItem.setAmount(requestItem.getSubtotalAmount().multiply(new BigDecimal("100")).longValue());
            }
            // 转单时携带收货地点字段，支持后续按门店/仓库自动拆分到货单
            orderItem.setPlannedReceiverType(requestItem.getPlannedReceiverType());
            orderItem.setPlannedStoreId(requestItem.getPlannedStoreId());
            orderItem.setPlannedWarehouseId(requestItem.getPlannedWarehouseId());
            orderItem.setCreateTime(LocalDateTime.now());
            orderItem.setUpdateTime(LocalDateTime.now());
            orderItems.add(orderItem);
            // 累计金额（单位：分）
            if (requestItem.getSubtotalAmount() != null) {
                totalAmount = totalAmount + requestItem.getSubtotalAmount().multiply(new BigDecimal("100")).longValue();
            }
        }
        order.setTotalAmount(totalAmount);
        orderMapper.insert(order);
        for (PurchaseOrderItem orderItem : orderItems) {
            // 使用 orderId 而非 purchaseOrderId（新实体类字段）
            orderItem.setOrderId(order.getOrderId());
            orderItemMapper.insert(orderItem);
        }
        request.setStatus("completed");
        request.setUpdateTime(LocalDateTime.now());
        updateById(request);
        return getById(requestId);
    }

    /**
     * 根据状态获取采购申请列表
     * @param status 状态
     * @return 采购申请列表
     */
    @Override
    public List<PurchaseRequestDTO> getByStatus(String status) {
        LambdaQueryWrapper<PurchaseRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseRequest::getStatus, status);
        applyDataScopeFilter(wrapper);
        wrapper.orderByDesc(PurchaseRequest::getCreateTime);
        List<PurchaseRequest> requests = list(wrapper);
        return requests.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 统计指定状态的采购申请数量
     * @param status 状态
     * @return 数量
     */
    @Override
    public int countByStatus(String status) {
        LambdaQueryWrapper<PurchaseRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseRequest::getStatus, status);
        applyDataScopeFilter(wrapper);
        return (int) count(wrapper);
    }

    /**
     * 生成采购申请单号
     * @return 单号
     */
    private String generateRequestNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(new LambdaQueryWrapper<PurchaseRequest>().likeRight(PurchaseRequest::getRequestNo, "PR" + dateStr));
        return String.format("PR%s%03d", dateStr, count + 1);
    }

    /**
     * 生成采购订单单号
     * @return 单号
     */
    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用getOrderCode()而非getOrderNo()（新实体类字段名）
        long count = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>().likeRight(PurchaseOrder::getOrderCode, "PO" + dateStr));
        return String.format("PO%s%03d", dateStr, count + 1);
    }

    /**
     * 按当前登录用户的数据范围过滤采购申请
     *
     * <p>规则：
     * <ul>
     *   <li>all/company：不过滤</li>
     *   <li>department：仅看本部门申请</li>
     *   <li>store：仅看本门店申请</li>
     *   <li>self/其他：仅看自己提交的申请</li>
     * </ul>
     *
     * @param wrapper 查询 wrapper
     */
    private void applyDataScopeFilter(LambdaQueryWrapper<PurchaseRequest> wrapper) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            // 未登录或无法识别用户，安全失败：返回空结果
            wrapper.apply("1 = 0");
            return;
        }

        com.foodtraceability.entity.User user = userService.getById(currentUserId);
        if (user == null) {
            wrapper.apply("1 = 0");
            return;
        }

        String dataScope = dataPermissionService.getUserDataScope(String.valueOf(currentUserId));
        log.debug("applyDataScopeFilter 数据范围: currentUserId={}, dataScope={}", currentUserId, dataScope);
        if (dataScope == null) {
            dataScope = "self";
        }

        switch (dataScope) {
            case "all":
            case "company":
                // 全部/本公司数据，不做额外过滤
                break;
            case "department":
                if (user.getDepartmentId() != null) {
                    wrapper.eq(PurchaseRequest::getDepartmentId, String.valueOf(user.getDepartmentId()));
                } else {
                    wrapper.apply("1 = 0");
                }
                break;
            case "store":
                if (user.getStoreId() != null) {
                    wrapper.eq(PurchaseRequest::getStoreId, String.valueOf(user.getStoreId()));
                } else {
                    wrapper.apply("1 = 0");
                }
                break;
            case "self":
            default:
                wrapper.eq(PurchaseRequest::getApplicantId, String.valueOf(currentUserId));
                break;
        }
    }

    /**
     * 逻辑删除采购申请明细
     * @param requestId 申请ID
     */
    private void deleteItemsByRequestId(String requestId) {
        itemMapper.update(null, new LambdaUpdateWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, requestId).set(PurchaseRequestItem::getDeleted, 1));
    }

    /**
     * 重置驳回次数（A1：管理员解锁被限制的申请）
     * @param requestId 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetRejectCount(String requestId) {
        PurchaseRequest request = getOne(new LambdaQueryWrapper<PurchaseRequest>().eq(PurchaseRequest::getRequestId, requestId));
        if (request == null) {
            throw new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
        }
        request.setRejectCount(0);
        request.setUpdateTime(LocalDateTime.now());
        updateById(request);
    }

    /**
     * 转换实体为DTO
     * @param request 实体
     * @return DTO
     */
    private PurchaseRequestDTO convertToDTO(PurchaseRequest request) {
        PurchaseRequestDTO dto = new PurchaseRequestDTO();
        BeanUtils.copyProperties(request, dto);
        return dto;
    }

    /**
     * 转换明细实体为DTO
     * @param item 明细实体
     * @return 明细DTO
     */
    private PurchaseRequestDTO.PurchaseRequestItemDTO convertItemToDTO(PurchaseRequestItem item) {
        PurchaseRequestDTO.PurchaseRequestItemDTO dto = new PurchaseRequestDTO.PurchaseRequestItemDTO();
        BeanUtils.copyProperties(item, dto);
        return dto;
    }

    public PurchaseRequestServiceImpl(final PurchaseRequestItemMapper itemMapper,
                                      final PurchaseOrderMapper orderMapper,
                                      final PurchaseOrderItemMapper orderItemMapper,
                                      final UserService userService,
                                      final DepartmentService departmentService,
                                      final DataPermissionService dataPermissionService,
                                      final StoreNewMapper storeNewMapper) {
        this.itemMapper = itemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userService = userService;
        this.departmentService = departmentService;
        this.dataPermissionService = dataPermissionService;
        this.storeNewMapper = storeNewMapper;
    }
}
