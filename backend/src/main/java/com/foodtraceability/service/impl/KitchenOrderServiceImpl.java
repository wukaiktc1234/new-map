package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.BatchStatusUpdateDTO;
import com.foodtraceability.dto.FoodGroupedOrderDTO;
import com.foodtraceability.dto.KitchenOrderCreateDTO;
import com.foodtraceability.dto.KitchenOrderWithWaitTimeDTO;
import com.foodtraceability.dto.KitchenStatsDTO;
import com.foodtraceability.dto.MaterialScanConsumeDTO;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.service.KitchenOrderService;
import com.foodtraceability.service.MaterialConsumptionService;
import com.foodtraceability.service.FoodTraceCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 后厨订单服务实现类
 */
@Service
public class KitchenOrderServiceImpl extends ServiceImpl<KitchenOrderMapper, KitchenOrder> implements KitchenOrderService {

    private static final Logger log = LoggerFactory.getLogger(KitchenOrderServiceImpl.class);


    /**
     * 使用 ObjectProvider 替代 @Lazy 注解，避免 Spring 6 虚拟线程环境下
     * 代理创建时序问题导致的 Bean 注册失败
     */
    public KitchenOrderServiceImpl(KitchenOrderMapper kitchenOrderMapper, ObjectMapper objectMapper, ObjectProvider<MaterialConsumptionService> materialConsumptionServiceProvider, ObjectProvider<FoodTraceCodeService> foodTraceCodeServiceProvider) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.objectMapper = objectMapper;
        this.materialConsumptionServiceProvider = materialConsumptionServiceProvider;
        this.foodTraceCodeServiceProvider = foodTraceCodeServiceProvider;
    }

    private final KitchenOrderMapper kitchenOrderMapper;

    private final ObjectMapper objectMapper;

    private final ObjectProvider<MaterialConsumptionService> materialConsumptionServiceProvider;

    private final ObjectProvider<FoodTraceCodeService> foodTraceCodeServiceProvider;

    private static final String PREFIX = "KO";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public KitchenOrder create(KitchenOrderCreateDTO dto) {
        KitchenOrder order = new KitchenOrder();

        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String kitchenOrderId = PREFIX + dateStr + String.format("%04d", getNextSequence());

        order.setKitchenOrderId(kitchenOrderId);
        order.setOrderId(dto.getOrderId());
        order.setOrderNumber(dto.getOrderNumber());
        order.setOrderType(dto.getOrderType());
        order.setTableNumber(dto.getTableNumber());
        order.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        order.setStatus("pending");
        order.setStoreId(dto.getStoreId());
        order.setStoreName(dto.getStoreName());
        order.setMaterialConsumed(0);
        order.setRemark(dto.getRemark());
        
        if (dto.getDishItems() != null) {
            try {
                order.setDishItems(objectMapper.writeValueAsString(dto.getDishItems()));
                order.setTotalDishes(dto.getDishItems().size());
            } catch (Exception e) {
                order.setDishItems("[]");
                order.setTotalDishes(0);
            }
        }
        
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        save(order);
        return order;
    }

    @Override
    public KitchenOrder getByOrderId(String orderId) {
        return kitchenOrderMapper.selectByOrderId(orderId);
    }

    @Override
    public KitchenOrder getByKitchenOrderId(String kitchenOrderId) {
        return kitchenOrderMapper.selectOne(
            new LambdaQueryWrapper<KitchenOrder>()
                .eq(KitchenOrder::getKitchenOrderId, kitchenOrderId)
        );
    }

    @Override
    @Transactional
    public boolean receiveOrder(String kitchenOrderId, Long chefId, String chefName) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null || !"pending".equals(order.getStatus())) {
            return false;
        }
        
        order.setStatus("received");
        order.setReceiveTime(LocalDateTime.now());
        order.setChefId(chefId);
        order.setChefName(chefName);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(chefName);
        
        return updateById(order);
    }

    @Override
    @Transactional
    public boolean startMake(String kitchenOrderId) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null || !"received".equals(order.getStatus())) {
            return false;
        }
        
        order.setStatus("making");
        order.setMakeStartTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        return updateById(order);
    }

    @Override
    @Transactional
    public boolean completeMake(String kitchenOrderId) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null || !"making".equals(order.getStatus())) {
            return false;
        }
        
        order.setStatus("completed");
        order.setMakeCompleteTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        generateFoodTraceCodes(kitchenOrderId);
        
        return updateById(order);
    }

    @Override
    @Transactional
    public boolean serve(String kitchenOrderId) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null || !"completed".equals(order.getStatus())) {
            return false;
        }
        
        order.setStatus("served");
        order.setServeTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        return updateById(order);
    }

    @Override
    @Transactional
    public boolean cancel(String kitchenOrderId, String reason) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null) {
            return false;
        }
        
        order.setStatus("cancelled");
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        
        return updateById(order);
    }

    @Override
    @Transactional
    public boolean scanConsumeMaterial(MaterialScanConsumeDTO dto) {
        materialConsumptionServiceProvider.getObject().record(dto);
        
        KitchenOrder order = getByKitchenOrderId(dto.getKitchenOrderId());
        if (order != null && order.getMaterialConsumed() == 0) {
            order.setMaterialConsumed(1);
            order.setMaterialConsumeTime(LocalDateTime.now());
            updateById(order);
        }
        
        return true;
    }

    @Override
    public List<KitchenOrder> getStoreActiveOrders(Long storeId) {
        return kitchenOrderMapper.selectStoreActiveOrders(storeId);
    }

    @Override
    public List<KitchenOrder> getChefActiveOrders(Long chefId) {
        return kitchenOrderMapper.selectChefActiveOrders(chefId);
    }

    @Override
    @Transactional
    public boolean updatePriority(String kitchenOrderId, Integer priority) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null) {
            return false;
        }
        
        order.setPriority(priority);
        order.setUpdateTime(LocalDateTime.now());
        
        return updateById(order);
    }

    @Override
    public int countByStatusAndStore(String status, Long storeId) {
        return kitchenOrderMapper.countByStatusAndStore(status, storeId);
    }

    @Override
    public boolean generateFoodTraceCodes(String kitchenOrderId) {
        KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
        if (order == null) {
            return false;
        }
        
        List<String> traceCodes = new ArrayList<>();
        
        try {
            List<KitchenOrderCreateDTO.DishItemDTO> dishItems = objectMapper.readValue(
                    order.getDishItems(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, KitchenOrderCreateDTO.DishItemDTO.class)
            );
            
            for (KitchenOrderCreateDTO.DishItemDTO item : dishItems) {
                for (int i = 0; i < item.getQuantity(); i++) {
                    var dto = new com.foodtraceability.dto.FoodTraceCodeGenerateDTO();
                    dto.setOrderId(order.getOrderId());
                    dto.setOrderNumber(order.getOrderNumber());
                    dto.setOrderType(order.getOrderType());
                    dto.setDishId(item.getDishId());
                    dto.setDishName(item.getDishName());
                    dto.setQuantity(1);
                    dto.setKitchenOrderId(Long.parseLong(kitchenOrderId.replace(PREFIX, "")));
                    dto.setChefId(order.getChefId());
                    dto.setChefName(order.getChefName());
                    dto.setStoreId(order.getStoreId());
                    dto.setStoreName(order.getStoreName());
                    dto.setTableNumber(order.getTableNumber());
                    
                    var foodTraceCode = foodTraceCodeServiceProvider.getObject().generate(dto);
                    traceCodes.add(foodTraceCode.getTraceCode());
                }
            }
            
            order.setFoodTraceCodes(objectMapper.writeValueAsString(traceCodes));
            updateById(order);
            
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }

    private int getNextSequence() {
        List<KitchenOrder> orders = lambdaQuery()
                .likeRight(KitchenOrder::getKitchenOrderId, PREFIX + LocalDate.now().format(DATE_FORMAT))
                .orderByDesc(KitchenOrder::getKitchenOrderId)
                .last("LIMIT 1")
                .list();

        if (orders.isEmpty()) {
            return 1;
        }
        String lastId = orders.get(0).getKitchenOrderId();
        String seqStr = lastId.substring(lastId.length() - 4);
        return Integer.parseInt(seqStr) + 1;
    }

    @Override
    public List<KitchenOrderWithWaitTimeDTO> getSortedOrders(String status, String sort, String sortOrder, int page, int size) {
        // 构建查询条件
        LambdaQueryWrapper<KitchenOrder> wrapper = new LambdaQueryWrapper<>();

        // 状态过滤
        if (status != null && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(KitchenOrder::getStatus, status);
        }

        // 排序逻辑
        boolean isAsc = !"desc".equalsIgnoreCase(sortOrder);
        if ("waitTime".equalsIgnoreCase(sort)) {
            // 按等待时间排序（创建时间升序=等待时间长的排前面）
            if (isAsc) {
                wrapper.orderByAsc(KitchenOrder::getCreateTime);
            } else {
                wrapper.orderByDesc(KitchenOrder::getCreateTime);
            }
        } else if ("orderTime".equalsIgnoreCase(sort)) {
            // 按下单时间排序
            if (isAsc) {
                wrapper.orderByAsc(KitchenOrder::getCreateTime);
            } else {
                wrapper.orderByDesc(KitchenOrder::getCreateTime);
            }
        } else if ("quantity".equalsIgnoreCase(sort)) {
            // 按菜品数量排序
            if (isAsc) {
                wrapper.orderByAsc(KitchenOrder::getTotalDishes);
            } else {
                wrapper.orderByDesc(KitchenOrder::getTotalDishes);
            }
        } else {
            // 默认按等待时间排序
            wrapper.orderByAsc(KitchenOrder::getCreateTime);
        }

        // 优先级排序（高优先级优先）
        wrapper.orderByDesc(KitchenOrder::getPriority);

        // 分页
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + size + " OFFSET " + offset);

        List<KitchenOrder> orders = list(wrapper);
        LocalDateTime now = LocalDateTime.now();

        // 转换为带等待时间的DTO
        return orders.stream().map(kitchenOrderItem -> {
            KitchenOrderWithWaitTimeDTO dto = new KitchenOrderWithWaitTimeDTO();
            dto.setId(kitchenOrderItem.getId());
            dto.setKitchenOrderId(kitchenOrderItem.getKitchenOrderId());
            dto.setOrderId(kitchenOrderItem.getOrderId());
            dto.setOrderNumber(kitchenOrderItem.getOrderNumber());
            dto.setOrderType(kitchenOrderItem.getOrderType());
            dto.setTableNumber(kitchenOrderItem.getTableNumber());
            dto.setTotalDishes(kitchenOrderItem.getTotalDishes());
            dto.setPriority(kitchenOrderItem.getPriority());
            dto.setStatus(kitchenOrderItem.getStatus());
            dto.setCreateTime(kitchenOrderItem.getCreateTime());
            dto.setChefName(kitchenOrderItem.getChefName());
            dto.setStoreName(kitchenOrderItem.getStoreName());

            // 计算等待分钟数
            if (kitchenOrderItem.getCreateTime() != null) {
                long minutes = java.time.Duration.between(kitchenOrderItem.getCreateTime(), now).toMinutes();
                dto.setWaitMinutes(Math.max(0, minutes));
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int batchUpdateStatus(BatchStatusUpdateDTO dto) {
        int successCount = 0;

        for (String kitchenOrderId : dto.getOrderIds()) {
            try {
                KitchenOrder order = getByKitchenOrderId(kitchenOrderId);
                if (order == null) {
                    log.warn("订单不存在: {}", kitchenOrderId);
                    continue;
                }

                // 验证状态转换是否合法
                String currentStatus = order.getStatus();
                String targetStatus = dto.getTargetStatus();

                if (!isValidStatusTransition(currentStatus, targetStatus)) {
                    log.warn("非法状态转换: {} -> {}", currentStatus, targetStatus);
                    continue;
                }

                // 根据目标状态设置相应时间字段
                order.setStatus(targetStatus);
                switch (targetStatus) {
                    case "received":
                        order.setReceiveTime(LocalDateTime.now());
                        break;
                    case "making":
                        order.setMakeStartTime(LocalDateTime.now());
                        break;
                    case "completed":
                        order.setMakeCompleteTime(LocalDateTime.now());
                        generateFoodTraceCodes(kitchenOrderId);
                        break;
                    case "served":
                        order.setServeTime(LocalDateTime.now());
                        break;
                    default:
                        break;
                }
                order.setUpdateTime(LocalDateTime.now());

                if (updateById(order)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量更新状态失败: {}, 错误: {}", kitchenOrderId, e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 验证状态转换是否合法
     */
    private boolean isValidStatusTransition(String currentStatus, String targetStatus) {
        Map<String, List<String>> validTransitions = new HashMap<>();
        validTransitions.put("pending", Arrays.asList("received", "cancelled"));
        validTransitions.put("received", Arrays.asList("making", "cancelled"));
        validTransitions.put("making", Arrays.asList("completed", "cancelled"));
        validTransitions.put("completed", Arrays.asList("served"));
        validTransitions.put("served", Collections.emptyList());

        List<String> allowedTargets = validTransitions.getOrDefault(currentStatus, Collections.emptyList());
        return allowedTargets.contains(targetStatus);
    }

    @Override
    public KitchenStatsDTO getKitchenStats(String date) {
        // 解析日期参数
        LocalDate queryDate;
        if ("today".equalsIgnoreCase(date)) {
            queryDate = LocalDate.now();
        } else {
            try {
                queryDate = LocalDate.parse(date);
            } catch (Exception e) {
                queryDate = LocalDate.now();
            }
        }
        String dateStr = queryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        KitchenStatsDTO stats = new KitchenStatsDTO();

        // 统计各状态数量
        List<Map<String, Object>> statusCounts = kitchenOrderMapper.countByStatusForDate(dateStr);
        int totalOrders = 0;
        int pendingCount = 0;
        int makingCount = 0;
        int completedCount = 0;

        for (Map<String, Object> item : statusCounts) {
            String status = (String) item.get("status");
            long count = ((Number) item.get("count")).longValue();
            totalOrders += count;

            if ("pending".equals(status) || "received".equals(status)) {
                pendingCount += count;
            } else if ("making".equals(status)) {
                makingCount += count;
            } else if ("completed".equals(status) || "served".equals(status)) {
                completedCount += count;
            }
        }

        stats.setTotalOrders(totalOrders);
        stats.setPendingCount(pendingCount);
        stats.setMakingCount(makingCount);
        stats.setCompletedCount(completedCount);

        // 完成率
        if (totalOrders > 0) {
            BigDecimal rate = BigDecimal.valueOf(completedCount)
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
            stats.setCompletionRate(rate);
        } else {
            stats.setCompletionRate(BigDecimal.ZERO);
        }

        // 平均等待时间
        Double avgWaitMinutes = kitchenOrderMapper.calculateAvgWaitMinutes(dateStr);
        stats.setAvgWaitMinutes(avgWaitMinutes != null ?
                BigDecimal.valueOf(avgWaitMinutes).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 平均制作时间
        Double avgMakingMinutes = kitchenOrderMapper.calculateAvgMakingMinutes(dateStr);
        stats.setAvgMakingMinutes(avgMakingMinutes != null ?
                BigDecimal.valueOf(avgMakingMinutes).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 超时订单数（超过15分钟的待处理订单）
        List<KitchenOrder> overdueOrders = kitchenOrderMapper.selectOverdueOrders(15);
        LocalDate finalQueryDate = queryDate;  // 创建final副本供lambda使用
        long todayOverdueCount = overdueOrders.stream()
                .filter(o -> o.getCreateTime().toLocalDate().isEqual(finalQueryDate))
                .count();
        stats.setOverdueCount((int) todayOverdueCount);

        // 高峰时段
        Map<String, Object> peakHourResult = kitchenOrderMapper.findPeakHour(dateStr);
        if (peakHourResult != null && peakHourResult.containsKey("hour")) {
            Number hourNum = (Number) peakHourResult.get("hour");
            int hour = hourNum.intValue();
            stats.setPeakHour(String.format("%02d:00", hour));
        } else {
            stats.setPeakHour("-");
        }

        return stats;
    }

    @Override
    public List<KitchenOrder> getOverdueOrders(int thresholdMinutes) {
        return kitchenOrderMapper.selectOverdueOrders(thresholdMinutes);
    }

    @Override
    public List<FoodGroupedOrderDTO> getOrdersGroupedByFood(String status) {
        // 查询符合条件的订单
        LambdaQueryWrapper<KitchenOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(KitchenOrder::getStatus, status);
        }
        wrapper.orderByDesc(KitchenOrder::getPriority)
               .orderByAsc(KitchenOrder::getCreateTime);

        List<KitchenOrder> orders = list(wrapper);
        LocalDateTime now = LocalDateTime.now();

        // 按菜品聚合
        Map<String, FoodGroupedOrderDTO> foodGroups = new LinkedHashMap<>();

        for (KitchenOrder order : orders) {
            if (order.getDishItems() == null || order.getDishItems().isEmpty()) {
                continue;
            }

            try {
                List<KitchenOrderCreateDTO.DishItemDTO> dishItems = objectMapper.readValue(
                        order.getDishItems(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, KitchenOrderCreateDTO.DishItemDTO.class)
                );

                for (KitchenOrderCreateDTO.DishItemDTO dishItem : dishItems) {
                    String dishKey = dishItem.getDishId();

                    FoodGroupedOrderDTO group = foodGroups.computeIfAbsent(dishKey, k -> {
                        FoodGroupedOrderDTO g = new FoodGroupedOrderDTO();
                        g.setDishId(dishItem.getDishId());
                        g.setDishName(dishItem.getDishName());
                        g.setTotalQuantity(0);
                        g.setOrderCount(0);
                        g.setKitchenOrderIds(new ArrayList<>());
                        g.setMaxPriority(0);
                        g.setMinWaitMinutes(Long.MAX_VALUE);
                        return g;
                    });

                    // 累加数量
                    group.setTotalQuantity(group.getTotalQuantity() + dishItem.getQuantity());

                    // 添加订单ID（去重）
                    if (!group.getKitchenOrderIds().contains(order.getKitchenOrderId())) {
                        group.getKitchenOrderIds().add(order.getKitchenOrderId());
                        group.setOrderCount(group.getOrderCount() + 1);
                    }

                    // 更新最高优先级
                    if (order.getPriority() != null && order.getPriority() > group.getMaxPriority()) {
                        group.setMaxPriority(order.getPriority());
                    }

                    // 更新最小等待时间
                    if (order.getCreateTime() != null) {
                        long waitMinutes = java.time.Duration.between(order.getCreateTime(), now).toMinutes();
                        if (waitMinutes < group.getMinWaitMinutes()) {
                            group.setMinWaitMinutes(Math.max(0, waitMinutes));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析菜品JSON失败: {}", e.getMessage());
            }
        }

        // 处理没有等待时间的情况
        for (FoodGroupedOrderDTO group : foodGroups.values()) {
            if (group.getMinWaitMinutes() == Long.MAX_VALUE) {
                group.setMinWaitMinutes(0L);
            }
        }

        return new ArrayList<>(foodGroups.values());
    }
}
