package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.trace.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.trace.FoodTraceabilityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 食品追溯服务实现类
 * 系统核心服务，负责追溯码的全生命周期管理
 */
@Service
public class FoodTraceabilityServiceImpl extends ServiceImpl<TraceCodeMapper, TraceCode> implements FoodTraceabilityService {

    private static final Logger log = LoggerFactory.getLogger(FoodTraceabilityServiceImpl.class);

    /** 追溯码前缀 */
    private static final String TRACE_CODE_PREFIX = "TC";
    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TraceCodeMapper traceCodeMapper;
    private final TraceChainNodeMapper traceChainNodeMapper;
    private final PurchaseLedgerMapper purchaseLedgerMapper;
    private final ObjectMapper objectMapper;
    /** 订单明细Mapper（订单级追溯 DF-032/033） */
    private final OrderItemNewMapper orderItemNewMapper;
    /** 订单Mapper（订单级追溯 DF-032/033） */
    private final OrderNewMapper orderNewMapper;
    /** 会员Mapper（受影响客户列表 DF-034） */
    private final MemberMapper memberMapper;
    /** 菜品配料Mapper（原料→菜品反查 DF-034） */
    private final DishIngredientMapper dishIngredientMapper;

    public FoodTraceabilityServiceImpl(TraceCodeMapper traceCodeMapper,
                                TraceChainNodeMapper traceChainNodeMapper,
                                PurchaseLedgerMapper purchaseLedgerMapper,
                                ObjectMapper objectMapper,
                                OrderItemNewMapper orderItemNewMapper,
                                OrderNewMapper orderNewMapper,
                                MemberMapper memberMapper,
                                DishIngredientMapper dishIngredientMapper) {
        this.traceCodeMapper = traceCodeMapper;
        this.traceChainNodeMapper = traceChainNodeMapper;
        this.purchaseLedgerMapper = purchaseLedgerMapper;
        this.objectMapper = objectMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.orderNewMapper = orderNewMapper;
        this.memberMapper = memberMapper;
        this.dishIngredientMapper = dishIngredientMapper;
    }

    /**
     * 生成追溯码 - 核心方法
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceCodeVO generateTraceCode(TraceCodeCreateDTO dto, Long createUserId) {
        // 1. 生成唯一追溯码编号
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String traceCodeValue = TRACE_CODE_PREFIX + dateStr + generateSequenceNo();

        // 2. 构建实体对象
        TraceCode traceCode = new TraceCode();
        traceCode.setTraceCode(traceCodeValue);
        traceCode.setTraceType(dto.getTraceType());
        traceCode.setTargetType(dto.getTargetType());
        traceCode.setTargetId(dto.getTargetId());
        traceCode.setTargetName(dto.getTargetName());
        traceCode.setBatchNo(dto.getBatchNo());
        traceCode.setProductionDate(dto.getProductionDate());
        traceCode.setExpiryDate(dto.getExpiryDate());
        traceCode.setSupplierId(dto.getSupplierId());
        traceCode.setSupplierName(dto.getSupplierName());
        traceCode.setPurchaseStockinId(dto.getPurchaseStockinId());
        traceCode.setWarehouseId(dto.getWarehouseId());
        traceCode.setCurrentLocation(dto.getCurrentLocation() != null ? dto.getCurrentLocation() : "待入库");
        traceCode.setStatus(1); // 默认正常
        traceCode.setRiskLevel(calculateInitialRiskLevel(dto));
        traceCode.setExtraInfo(dto.getExtraInfo());
        traceCode.setCreateUserId(createUserId);

        // 3. 初始化追溯链数据
        Map<String, Object> chainData = initializeChainData(traceCode, dto);
        try {
            traceCode.setChainData(objectMapper.writeValueAsString(chainData));
        } catch (JsonProcessingException e) {
            log.warn("序列化追溯链数据失败: {}", e.getMessage());
            traceCode.setChainData("{}");
        }

        // 4. 保存追溯码
        save(traceCode);
        log.info("生成追溯码成功: code={}, target={}", traceCodeValue, dto.getTargetName());

        // 5. 如果有关联的采购入库单，自动创建第一个链节点（采购入库）
        if (dto.getPurchaseStockinId() != null) {
            addChainNode(traceCode.getTraceCodeId(), 1,
                    dto.getCurrentLocation() != null ? dto.getCurrentLocation() : "采购入库",
                    createUserId, "系统", buildPurchaseDetail(dto));
        }

        return convertToVO(traceCode);
    }

    /**
     * 正向追溯查询（扫码查询）
     */
    @Override
    public TraceCodeVO queryByTraceCode(String traceCode) {
        if (traceCode == null || traceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("追溯码不能为空");
        }

        TraceCode entity = traceCodeMapper.selectByTraceCode(traceCode.trim());
        if (entity == null) {
            throw new IllegalArgumentException("追溯码不存在: " + traceCode);
        }

        TraceCodeVO vo = convertToVO(entity);

        // 查询关联的追溯链节点
        List<TraceChainNode> nodes = traceChainNodeMapper.selectByTraceCodeIdOrderBySequence(entity.getTraceCodeId());
        vo.setChainNodes(nodes.stream().map(this::convertNodeToVO).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 反向召回查询
     */
    @Override
    public RecallQueryResultVO queryRecallImpact(String batchNo, Long supplierId, String targetName) {
        RecallQueryResultVO result = new RecallQueryResultVO();

        // 1. 查询匹配的追溯码
        LambdaQueryWrapper<TraceCode> wrapper = new LambdaQueryWrapper<>();
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.eq(TraceCode::getBatchNo, batchNo);
        }
        if (supplierId != null) {
            wrapper.eq(TraceCode::getSupplierId, supplierId);
        }
        if (targetName != null && !targetName.isEmpty()) {
            wrapper.like(TraceCode::getTargetName, targetName);
        }
        wrapper.orderByDesc(TraceCode::getCreateTime);

        List<TraceCode> affectedCodes = list(wrapper);

        // 2. 组装目标信息
        RecallQueryResultVO.TargetInfo targetInfo = new RecallQueryResultVO.TargetInfo();
        if (!affectedCodes.isEmpty()) {
            TraceCode first = affectedCodes.get(0);
            targetInfo.setTargetId(first.getTargetId());
            targetInfo.setTargetName(first.getTargetName());
            targetInfo.setBatchNo(first.getBatchNo());
            targetInfo.setSupplierName(first.getSupplierName());
        }
        targetInfo.setProblemDescription(buildProblemDescription(batchNo, supplierId, targetName));
        result.setTargetInfo(targetInfo);

        // 3. 转换受影响的追溯码列表
        List<RecallQueryResultVO.AffectedTraceCodeVO> affectedList = affectedCodes.stream()
                .map(code -> {
                    RecallQueryResultVO.AffectedTraceCodeVO vo = new RecallQueryResultVO.AffectedTraceCodeVO();
                    vo.setTraceCodeId(code.getTraceCodeId());
                    vo.setTraceCode(code.getTraceCode());
                    vo.setTargetName(code.getTargetName());
                    vo.setStatus(code.getStatus());
                    vo.setStatusName(getStatusName(code.getStatus()));
                    vo.setCreateTime(code.getCreateTime());
                    return vo;
                })
                .collect(Collectors.toList());
        result.setAffectedTraceCodes(affectedList);

        // 4. 统计汇总
        RecallQueryResultVO.RecallSummary summary = new RecallQueryResultVO.RecallSummary();
        summary.setTotalTraceCodes(affectedCodes.size());
        summary.setTotalDishTypes((int) affectedCodes.stream()
                .filter(c -> c.getTargetType() != null && c.getTargetType() == 1)
                .map(TraceCode::getTargetName)
                .distinct()
                .count());
        summary.setSoldQuantity((int) affectedCodes.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 5)
                .count());
        summary.setStockQuantity((int) affectedCodes.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .count());

        // 5. 订单级追溯（DF-032/DF-033/DF-034）：关联订单系统统计受影响订单和菜品
        RecallImpactData impactData = buildRecallImpactData(affectedCodes);
        summary.setTotalOrders(impactData.getAffectedOrders().size());
        result.setSummary(summary);

        result.setAffectedDishes(impactData.getAffectedDishes());
        result.setAffectedOrders(impactData.getAffectedOrders());

        return result;
    }

    /**
     * 查询批次受影响的所有订单（DF-032：订单级追溯）
     */
    @Override
    public List<OrderNew> findAffectedOrders(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 查询该批次的所有追溯码
        List<TraceCode> traceCodes = list(new LambdaQueryWrapper<TraceCode>()
                .eq(TraceCode::getBatchNo, batchNo));
        if (traceCodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 通过追溯码的目标ID查询关联订单
        Set<String> orderIds = findOrderIdsByTraceCodes(traceCodes);
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. 查询订单列表
        return orderNewMapper.selectList(new LambdaQueryWrapper<OrderNew>()
                .in(OrderNew::getOrderId, orderIds)
                .orderByDesc(OrderNew::getCreateTime));
    }

    /**
     * 查询批次受影响的所有客户（DF-034：受影响客户列表）
     */
    @Override
    public List<Member> findAffectedCustomers(String batchNo) {
        List<OrderNew> orders = findAffectedOrders(batchNo);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        // 提取去重的会员ID（customer_id 即会员ID）
        List<Long> memberIds = orders.stream()
                .map(OrderNew::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }
        return memberMapper.selectList(new LambdaQueryWrapper<Member>()
                .in(Member::getId, memberIds));
    }

    /**
     * 反向追溯：从溯源码查询所有受影响订单和客户（DF-033：反向追溯）
     */
    @Override
    public RecallQueryResultVO reverseTrace(String traceCode) {
        if (traceCode == null || traceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("追溯码不能为空");
        }
        // 1. 查询溯源码信息
        TraceCode code = traceCodeMapper.selectByTraceCode(traceCode.trim());
        if (code == null) {
            throw new IllegalArgumentException("追溯码不存在: " + traceCode);
        }
        // 2. 通过批次号反向追溯所有受影响范围
        return queryRecallImpact(code.getBatchNo(), null, null);
    }

    /**
     * 分页查询追溯码列表
     */
    @Override
    public IPage<TraceCodeVO> queryPage(TraceCodeQueryDTO queryDTO) {
        Page<TraceCode> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<TraceCode> wrapper = new LambdaQueryWrapper<>();

        // 关键字搜索
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(TraceCode::getTraceCode, queryDTO.getKeyword())
                    .or().like(TraceCode::getTargetName, queryDTO.getKeyword()));
        }
        if (queryDTO.getTraceType() != null) {
            wrapper.eq(TraceCode::getTraceType, queryDTO.getTraceType());
        }
        if (queryDTO.getTargetType() != null) {
            wrapper.eq(TraceCode::getTargetType, queryDTO.getTargetType());
        }
        if (queryDTO.getBatchNo() != null) {
            wrapper.eq(TraceCode::getBatchNo, queryDTO.getBatchNo());
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(TraceCode::getSupplierId, queryDTO.getSupplierId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(TraceCode::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getRiskLevel() != null) {
            wrapper.eq(TraceCode::getRiskLevel, queryDTO.getRiskLevel());
        }
        if (queryDTO.getProductionDateStart() != null) {
            wrapper.ge(TraceCode::getProductionDate, queryDTO.getProductionDateStart());
        }
        if (queryDTO.getProductionDateEnd() != null) {
            wrapper.le(TraceCode::getProductionDate, queryDTO.getProductionDateEnd());
        }
        if (queryDTO.getExpiryDateStart() != null) {
            wrapper.ge(TraceCode::getExpiryDate, queryDTO.getExpiryDateStart());
        }
        if (queryDTO.getExpiryDateEnd() != null) {
            wrapper.le(TraceCode::getExpiryDate, queryDTO.getExpiryDateEnd());
        }
        if (queryDTO.getCreateTimeStart() != null) {
            wrapper.ge(TraceCode::getCreateTime, queryDTO.getCreateTimeStart());
        }
        if (queryDTO.getCreateTimeEnd() != null) {
            wrapper.le(TraceCode::getCreateTime, queryDTO.getCreateTimeEnd());
        }

        wrapper.orderByDesc(TraceCode::getCreateTime);

        IPage<TraceCode> pageResult = page(page, wrapper);

        // 转换为VO
        Page<TraceCodeVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));

        return voPage;
    }

    /**
     * 更新追溯码状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long traceCodeId, Integer status) {
        TraceCode traceCode = getById(traceCodeId);
        if (traceCode == null) {
            throw new IllegalArgumentException("追溯码不存在: " + traceCodeId);
        }
        traceCode.setStatus(status);
        // 自动刷新风险等级
        refreshRiskLevel(traceCodeId);
        return updateById(traceCode);
    }

    /**
     * 执行召回操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeRecall(Long traceCodeId, String recallReason, Long operatorId, String operatorName) {
        TraceCode traceCode = getById(traceCodeId);
        if (traceCode == null) {
            throw new IllegalArgumentException("追溯码不存在: " + traceCodeId);
        }

        // 更新状态为已召回
        traceCode.setStatus(4);
        traceCode.setRiskLevel(3); // 召回后设为高风险
        updateById(traceCode);

        // 添加召回节点到追溯链
        Map<String, Object> recallDetail = new HashMap<>();
        recallDetail.put("recallReason", recallReason);
        recallDetail.put("recallTime", LocalDateTime.now().toString());
        addChainNode(traceCodeId, 99, traceCode.getCurrentLocation(),
                operatorId, operatorName, recallDetail);

        log.info("执行追溯码召回: traceCode={}, reason={}", traceCode.getTraceCode(), recallReason);
        return true;
    }

    /**
     * 添加追溯链节点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceChainNodeVO addChainNode(Long traceCodeId, Integer nodeType, String location,
                                          Long operatorId, String operatorName, Object detailJson) {
        // 获取当前最大序号
        Integer maxSeq = traceChainNodeMapper.selectMaxSequenceByTraceCodeId(traceCodeId);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

        TraceChainNode node = new TraceChainNode();
        node.setTraceCodeId(traceCodeId);
        node.setNodeSequence(nextSeq);
        node.setNodeType(nodeType);
        node.setEventTime(LocalDateTime.now());
        node.setLocation(location);
        node.setOperatorId(operatorId);
        node.setOperatorName(operatorName);
        node.setDetailJson(detailJson);

        traceChainNodeMapper.insert(node);
        log.info("添加追溯链节点: traceCodeId={}, seq={}, type={}", traceCodeId, nextSeq, nodeType);

        return convertNodeToVO(node);
    }

    /**
     * 记录消费者扫码行为
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordScanEvent(String traceCode) {
        TraceCode entity = traceCodeMapper.selectByTraceCode(traceCode);
        if (entity == null) {
            log.warn("记录扫码失败，追溯码不存在: {}", traceCode);
            return false;
        }

        // 添加扫码节点
        addChainNode(entity.getTraceCodeId(), 7, "消费者扫码", null, "消费者",
                Map.of("scanTime", LocalDateTime.now().toString()));

        // 如果之前未消费，更新为已消费
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            entity.setStatus(5);
            entity.setCurrentLocation("已消费");
            updateById(entity);
        }

        log.info("记录消费者扫码: {}", traceCode);
        return true;
    }

    /**
     * 刷新风险等级
     */
    @Override
    public Integer refreshRiskLevel(Long traceCodeId) {
        TraceCode traceCode = getById(traceCodeId);
        if (traceCode == null || traceCode.getExpiryDate() == null) {
            return 1; // 默认低风险
        }

        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), traceCode.getExpiryDate());
        Integer newRiskLevel;

        if (daysUntilExpiry < 0) {
            newRiskLevel = 3; // 已过期 - 高风险
        } else if (daysUntilExpiry <= 7) {
            newRiskLevel = 3; // 7天内 - 高风险
        } else if (daysUntilExpiry <= 30) {
            newRiskLevel = 2; // 30天内 - 中风险
        } else {
            newRiskLevel = 1; // 低风险
        }

        traceCode.setRiskLevel(newRiskLevel);
        // 根据风险等级同步更新状态
        if (newRiskLevel >= 3 && traceCode.getStatus() == 1) {
            traceCode.setStatus(daysUntilExpiry < 0 ? 3 : 2); // 已过期或即将过期
        }
        updateById(traceCode);

        return newRiskLevel;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建召回影响数据（DF-032/033/034 核心实现）
     * 通过追溯码的目标ID关联订单明细，统计受影响订单、菜品
     */
    private RecallImpactData buildRecallImpactData(List<TraceCode> affectedCodes) {
        RecallImpactData data = new RecallImpactData();
        if (affectedCodes == null || affectedCodes.isEmpty()) {
            return data;
        }

        // 1. 收集菜品ID（target_type=1成品菜品的target_id，以及原料关联的菜品ID）
        Set<Long> foodIds = collectFoodIdsFromTraceCodes(affectedCodes);
        if (foodIds.isEmpty()) {
            return data;
        }

        // 2. 查询包含这些菜品的订单明细
        List<OrderItemNew> orderItems = orderItemNewMapper.selectList(
                new LambdaQueryWrapper<OrderItemNew>()
                        .in(OrderItemNew::getFoodId, foodIds));
        if (orderItems.isEmpty()) {
            return data;
        }

        // 3. 聚合受影响菜品（按food_id分组，统计数量）
        Map<Long, RecallQueryResultVO.AffectedDishVO> dishMap = new LinkedHashMap<>();
        for (OrderItemNew item : orderItems) {
            if (item.getFoodId() == null) {
                continue;
            }
            RecallQueryResultVO.AffectedDishVO dish = dishMap.computeIfAbsent(
                    item.getFoodId(),
                    id -> {
                        RecallQueryResultVO.AffectedDishVO d = new RecallQueryResultVO.AffectedDishVO();
                        d.setDishId(id);
                        d.setDishName(item.getProductName());
                        d.setQuantity(0);
                        return d;
                    });
            dish.setQuantity(dish.getQuantity() + (item.getQuantity() != null ? item.getQuantity() : 0));
        }
        data.setAffectedDishes(new ArrayList<>(dishMap.values()));

        // 4. 查询受影响订单
        Set<String> orderIds = orderItems.stream()
                .map(OrderItemNew::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return data;
        }

        List<OrderNew> orders = orderNewMapper.selectList(
                new LambdaQueryWrapper<OrderNew>()
                        .in(OrderNew::getOrderId, orderIds)
                        .orderByDesc(OrderNew::getCreateTime));

        // 5. 统计每个订单涉及的问题菜品数量
        Map<String, Integer> orderQuantityMap = new HashMap<>();
        for (OrderItemNew item : orderItems) {
            if (item.getOrderId() != null) {
                orderQuantityMap.merge(item.getOrderId(),
                        item.getQuantity() != null ? item.getQuantity() : 0, Integer::sum);
            }
        }

        // 6. 构建受影响订单VO（客户信息脱敏）
        List<RecallQueryResultVO.AffectedOrderVO> affectedOrders = new ArrayList<>();
        for (OrderNew order : orders) {
            RecallQueryResultVO.AffectedOrderVO vo = new RecallQueryResultVO.AffectedOrderVO();
            vo.setOrderId(order.getOrderId());
            vo.setOrderNumber(order.getOrderCode());
            vo.setOrderTime(order.getCreateTime());
            vo.setCustomerInfo(maskCustomerInfo(order.getCustomerName(), order.getCustomerPhone()));
            vo.setQuantity(orderQuantityMap.getOrDefault(order.getOrderId(), 0));
            affectedOrders.add(vo);
        }
        data.setAffectedOrders(affectedOrders);

        return data;
    }

    /**
     * 通过追溯码列表查询关联的订单ID集合
     */
    private Set<String> findOrderIdsByTraceCodes(List<TraceCode> traceCodes) {
        Set<Long> foodIds = collectFoodIdsFromTraceCodes(traceCodes);
        if (foodIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<OrderItemNew> orderItems = orderItemNewMapper.selectList(
                new LambdaQueryWrapper<OrderItemNew>()
                        .in(OrderItemNew::getFoodId, foodIds));
        return orderItems.stream()
                .map(OrderItemNew::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 从追溯码列表收集关联的菜品ID
     * - target_type=1（成品菜品）：target_id 即 food_id
     * - target_type=3（原材料）：需通过 dish_ingredient 表反查使用该原料的菜品
     */
    private Set<Long> collectFoodIdsFromTraceCodes(List<TraceCode> traceCodes) {
        Set<Long> foodIds = new HashSet<>();
        List<String> materialIds = new ArrayList<>();

        for (TraceCode code : traceCodes) {
            if (code.getTargetId() == null || code.getTargetType() == null) {
                continue;
            }
            if (code.getTargetType() == 1) {
                // 成品菜品：target_id 直接对应 order_items.food_id
                foodIds.add(code.getTargetId());
            } else if (code.getTargetType() == 3) {
                // 原材料：收集 material_id，稍后通过 dish_ingredient 反查菜品
                materialIds.add(String.valueOf(code.getTargetId()));
            }
        }

        // 原材料 → 菜品反查：通过 dish_ingredient.ingredient_id 找到 dish_id
        if (!materialIds.isEmpty()) {
            List<DishIngredient> dishIngredients = dishIngredientMapper.selectList(
                    new LambdaQueryWrapper<DishIngredient>()
                            .in(DishIngredient::getIngredientId, materialIds));
            for (DishIngredient di : dishIngredients) {
                if (di.getDishId() != null) {
                    try {
                        foodIds.add(Long.parseLong(di.getDishId()));
                    } catch (NumberFormatException e) {
                        log.warn("dish_ingredient.dish_id 格式非法，跳过: {}", di.getDishId());
                    }
                }
            }
        }

        return foodIds;
    }

    /**
     * 客户信息脱敏（食品安全法合规：召回通知时保护客户隐私）
     * 姓名保留姓氏，手机号中间4位掩码
     */
    private String maskCustomerInfo(String name, String phone) {
        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isEmpty()) {
            sb.append(name.charAt(0));
            if (name.length() > 1) {
                sb.append("**");
            }
        }
        if (phone != null && phone.length() >= 11) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(phone.substring(0, 3)).append("****").append(phone.substring(7));
        }
        return sb.length() > 0 ? sb.toString() : "匿名客户";
    }

    /**
     * 召回影响数据内部容器
     */
    private static class RecallImpactData {
        private List<RecallQueryResultVO.AffectedDishVO> affectedDishes = new ArrayList<>();
        private List<RecallQueryResultVO.AffectedOrderVO> affectedOrders = new ArrayList<>();

        public List<RecallQueryResultVO.AffectedDishVO> getAffectedDishes() {
            return affectedDishes;
        }

        public void setAffectedDishes(List<RecallQueryResultVO.AffectedDishVO> affectedDishes) {
            this.affectedDishes = affectedDishes;
        }

        public List<RecallQueryResultVO.AffectedOrderVO> getAffectedOrders() {
            return affectedOrders;
        }

        public void setAffectedOrders(List<RecallQueryResultVO.AffectedOrderVO> affectedOrders) {
            this.affectedOrders = affectedOrders;
        }
    }

    /**
     * 生成序号（简化版，实际应使用Redis或数据库序列）
     */
    private synchronized String generateSequenceNo() {
        return String.format("%04d", System.currentTimeMillis() % 10000);
    }

    /**
     * 计算初始风险等级
     */
    private Integer calculateInitialRiskLevel(TraceCodeCreateDTO dto) {
        if (dto.getExpiryDate() == null) {
            return 1;
        }
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dto.getExpiryDate());
        if (daysUntilExpiry <= 7) return 3;
        if (daysUntilExpiry <= 30) return 2;
        return 1;
    }

    /**
     * 初始化追溯链数据
     */
    private Map<String, Object> initializeChainData(TraceCode traceCode, TraceCodeCreateDTO dto) {
        Map<String, Object> chainData = new LinkedHashMap<>();
        chainData.put("traceCode", traceCode.getTraceCode());
        chainData.put("targetName", dto.getTargetName());
        chainData.put("totalNodes", 0);
        chainData.put("nodes", new ArrayList<>());
        chainData.put("generatedAt", LocalDateTime.now().toString());
        return chainData;
    }

    /**
     * 构建采购入库详细信息
     */
    private Object buildPurchaseDetail(TraceCodeCreateDTO dto) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("supplierName", dto.getSupplierName());
        detail.put("batchNo", dto.getBatchNo());
        detail.put("productionDate", dto.getProductionDate() != null ? dto.getProductionDate().toString() : null);
        detail.put("purchaseStockinId", dto.getPurchaseStockinId());
        return detail;
    }

    /**
     * 构建问题描述
     */
    private String buildProblemDescription(String batchNo, Long supplierId, String targetName) {
        StringBuilder sb = new StringBuilder("发现以下问题需要召回：");
        if (batchNo != null) sb.append(" 批次号[").append(batchNo).append("]");
        if (supplierId != null) sb.append(" 供应商ID[").append(supplierId).append("]");
        if (targetName != null) sb.append(" 包含[").append(targetName).append("]");
        return sb.toString();
    }

    /**
     * 实体转VO
     */
    private TraceCodeVO convertToVO(TraceCode entity) {
        TraceCodeVO vo = new TraceCodeVO();
        vo.setTraceCodeId(entity.getTraceCodeId());
        vo.setTraceCode(entity.getTraceCode());
        vo.setTraceType(entity.getTraceType());
        vo.setTraceTypeName(getTraceTypeName(entity.getTraceType()));
        vo.setTargetType(entity.getTargetType());
        vo.setTargetTypeName(getTargetTypeName(entity.getTargetType()));
        vo.setTargetId(entity.getTargetId());
        vo.setTargetName(entity.getTargetName());
        vo.setBatchNo(entity.getBatchNo());
        vo.setProductionDate(entity.getProductionDate());
        vo.setExpiryDate(entity.getExpiryDate());
        vo.setSupplierId(entity.getSupplierId());
        vo.setSupplierName(entity.getSupplierName());
        vo.setPurchaseStockinId(entity.getPurchaseStockinId());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setCurrentLocation(entity.getCurrentLocation());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusName(entity.getStatus()));
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setRiskLevelName(getRiskLevelName(entity.getRiskLevel()));
        vo.setQrCodeImageUrl(entity.getQrCodeImageUrl());
        vo.setExtraInfo(entity.getExtraInfo());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 节点实体转VO
     */
    private TraceChainNodeVO convertNodeToVO(TraceChainNode node) {
        TraceChainNodeVO vo = new TraceChainNodeVO();
        vo.setNodeId(node.getNodeId());
        vo.setNodeSequence(node.getNodeSequence());
        vo.setNodeType(node.getNodeType());
        vo.setNodeTypeName(getNodeTypeName(node.getNodeType()));
        vo.setEventTime(node.getEventTime());
        vo.setLocation(node.getLocation());
        vo.setOperatorName(node.getOperatorName());
        vo.setDetailJson(node.getDetailJson());
        vo.setAttachmentUrl(node.getAttachmentUrl());
        return vo;
    }

    // ==================== 名称映射方法 ====================

    private static String getTraceTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "菜品追溯码";
            case 2: return "原料批次追溯码";
            case 3: return "物流追溯码";
            case 4: return "检验报告码";
            default: return "未知";
        }
    }

    private static String getTargetTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "成品菜品";
            case 2: return "半成品";
            case 3: return "原材料";
            case 4: return "包装材料";
            default: return "未知";
        }
    }

    private static String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 2: return "即将过期";
            case 3: return "已过期";
            case 4: return "已召回";
            case 5: return "已消费";
            default: return "未知";
        }
    }

    private static String getRiskLevelName(Integer level) {
        if (level == null) return "未知";
        switch (level) {
            case 1: return "低风险";
            case 2: return "中风险";
            case 3: return "高风险";
            default: return "未知";
        }
    }

    private static String getNodeTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "采购入库";
            case 2: return "仓储入库";
            case 3: return "加工制作";
            case 4: return "出库发货";
            case 5: return "上架销售";
            case 6: return "检验检测";
            case 7: return "消费者扫码";
            case 99: return "召回处理";
            default: return "其他";
        }
    }
}
