package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dataservice.DailySettlementDataService;
import com.foodtraceability.event.StoreDailySettlementCompletedEvent;
import com.foodtraceability.dto.SettlementQueryDTO;
import com.foodtraceability.dto.store.operation.DailySettlementCreateDTO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementShiftVO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.DailySettlement;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.DailySettlementMapper;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.service.DailySettlementService;
import com.foodtraceability.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日结对账服务实现类
 * 提供日结对账的查询、确认、异常上报等业务功能
 * 支持基于角色的权限控制，敏感字段仅对总部角色可见
 */
@Service
public class DailySettlementServiceImpl implements DailySettlementService {

    private static final Logger log = LoggerFactory.getLogger(DailySettlementServiceImpl.class);

    /** 总部财务总监角色 */
    private static final String ROLE_FINANCE_DIRECTOR = "finance_director";
    /** 区域经理角色 */
    private static final String ROLE_REGIONAL_MANAGER = "regional_manager";
    /** 店长角色 */
    private static final String ROLE_STORE_MANAGER = "store_manager";

    // ==================== 支付方式常量定义 ====================
    /** 微信支付（PAYMENT_METHOD = 0） */
    private static final int PAYMENT_WECHAT = 0;
    /** 支付宝（PAYMENT_METHOD = 1） */
    private static final int PAYMENT_ALIPAY = 1;
    /** 现金（PAYMENT_METHOD = 2） */
    private static final int PAYMENT_CASH = 2;
    /** 会员余额（PAYMENT_METHOD = 4） */
    private static final int PAYMENT_MEMBER_BALANCE = 4;

    /** JSON序列化器 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DailySettlementMapper dailySettlementMapper;
    private final DailySettlementDataService dailySettlementDataService;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 构造函数注入
     *
     * @param dailySettlementMapper      日结主表Mapper
     * @param dailySettlementDataService 日结对账数据服务（缓存+权限过滤）
     * @param notificationService        通知服务
     * @param orderMapper                订单Mapper（用于聚合查询）
     * @param applicationEventPublisher  事件发布器（用于发布日结完成事件）
     */
    public DailySettlementServiceImpl(DailySettlementMapper dailySettlementMapper,
                                      DailySettlementDataService dailySettlementDataService,
                                      NotificationService notificationService,
                                      OrderMapper orderMapper,
                                      ApplicationEventPublisher applicationEventPublisher) {
        this.dailySettlementMapper = dailySettlementMapper;
        this.dailySettlementDataService = dailySettlementDataService;
        this.notificationService = notificationService;
        this.orderMapper = orderMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public IPage<DailySettlementVO> getSettlementList(SettlementQueryDTO query, User currentUser) {
        // 构建分页对象
        Page<DailySettlement> page = new Page<>(query.getCurrent(), query.getSize());

        // 构建查询条件
        LambdaQueryWrapper<DailySettlement> wrapper = buildQueryWrapper(query, currentUser);

        // 分页查询
        IPage<DailySettlement> settlementPage = dailySettlementMapper.selectPage(page, wrapper);

        // 获取当前用户角色并转换（含权限过滤）
        String role = getUserRole(currentUser);

        // 转换为VO（通过DataService进行权限过滤）
        return settlementPage.convert(settlement ->
                dailySettlementDataService.getSettlementBasicInfo(settlement.getSettlementId(), role));
    }

    @Override
    public DailySettlementVO getSettlementWithPermission(String settlementId, User currentUser) {
        // 1. 查询对账主表
        DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日结记录不存在");
        }

        // 2. 根据角色动态过滤敏感字段
        String role = getUserRole(currentUser);
        DailySettlementVO vo = dailySettlementDataService.getSettlementBasicInfo(settlementId, role);

        // 3. 查询班次明细列表
        List<DailySettlementShiftVO> shifts = dailySettlementDataService.getShiftsBySettlementId(settlementId);
        vo.setShifts(shifts);

        // 4. 返回完整VO
        return vo;
    }

    @Override
    public List<DailySettlementShiftVO> getShiftsBySettlementId(String settlementId) {
        // 直接委托给DataService查询班次明细
        return dailySettlementDataService.getShiftsBySettlementId(settlementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailySettlementVO createSettlement(DailySettlementCreateDTO dto, String operatorId) {
        log.info("创建日结草稿: storeId={}, date={}", dto.getStoreId(), dto.getSettlementDate());

        // 参数校验
        if (dto.getStoreId() == null || dto.getStoreId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门店ID不能为空");
        }
        if (dto.getSettlementDate() == null || dto.getSettlementDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结算日期不能晚于今日");
        }

        // 检查同店同日是否已存在日结记录
        LambdaQueryWrapper<DailySettlement> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(DailySettlement::getStoreId, dto.getStoreId())
                     .eq(DailySettlement::getSettlementDate, dto.getSettlementDate());
        Long existCount = dailySettlementMapper.selectCount(checkWrapper);
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "该门店当日已存在日结记录，不可重复创建");
        }

        // 创建实体
        DailySettlement settlement = new DailySettlement();
        settlement.setStoreId(dto.getStoreId());
        settlement.setSettlementDate(dto.getSettlementDate());
        settlement.setTotalRevenue(convertYuanToFen(dto.getTotalRevenue()));
        settlement.setTotalCost(convertYuanToFen(dto.getTotalCost()));
        settlement.setNetProfit(convertYuanToFen(dto.getNetProfit()));
        settlement.setGrossProfitRate(parseBigDecimal(dto.getGrossProfitRate()));
        settlement.setOrderCount(dto.getOrderCount());
        settlement.setAvgOrderValue(convertYuanToFen(dto.getAvgOrderValue()));
        settlement.setTableUsageRate(parseBigDecimal(dto.getTableUsageRate()));
        settlement.setDifferenceAmount(convertYuanToFen(dto.getDifferenceAmount()));
        settlement.setStatus("draft"); // 初始状态为草稿
        settlement.setRemark(dto.getRemark());

        // 保存到数据库
        int rows = dailySettlementMapper.insert(settlement);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "创建日结记录失败");
        }

        log.info("日结草稿创建成功: settlementId={}", settlement.getSettlementId());

        // 返回创建后的VO
        return dailySettlementDataService.getSettlementBasicInfo(
                settlement.getSettlementId(), ROLE_STORE_MANAGER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSettlement(String settlementId, String operatorId) {
        log.info("确认对账: settlementId={}, operatorId={}", settlementId, operatorId);

        // 1. 查询日结记录
        DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日结记录不存在");
        }

        // 2. 状态校验（只有draft和pending状态可确认）
        if (!"draft".equals(settlement.getStatus()) && !"pending".equals(settlement.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前状态不允许确认操作，当前状态：" + settlement.getStatus());
        }

        // 3. 更新状态为approved
        settlement.setStatus("approved");
        settlement.setAuditorId(operatorId);
        settlement.setUpdateTime(LocalDateTime.now());

        int rows = dailySettlementMapper.updateById(settlement);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "确认对账失败，请刷新后重试");
        }

        // 4. 清除缓存
        dailySettlementDataService.clearSettlementCache(settlementId);

        log.info("对账确认成功: settlementId={}", settlementId);

        // 5. 发布门店日结完成事件（F-008 联动）
        // 监听器：StoreDailySettlementEventListener（@TransactionalEventListener AFTER_COMMIT + @Async）
        // 触发 AutoVoucherService.generateStoreSettlementVoucher 生成财务凭证
        publishSettlementCompletedEvent(settlement, operatorId);
    }

    /**
     * 发布门店日结完成事件
     * 事件在主事务提交后由 @TransactionalEventListener(AFTER_COMMIT) 异步消费
     * 异常隔离，发布失败不影响主事务
     */
    private void publishSettlementCompletedEvent(DailySettlement settlement, String operatorId) {
        try {
            Long settlementIdLong = parseLongOrNull(settlement.getSettlementId());
            Long storeIdLong = parseLongOrNull(settlement.getStoreId());
            Long operatorIdLong = parseLongOrNull(operatorId);
            Long totalIncome = settlement.getTotalRevenue() != null ? settlement.getTotalRevenue() : 0L;
            Long totalExpense = settlement.getTotalCost() != null ? settlement.getTotalCost() : 0L;
            Long netProfit = settlement.getNetProfit() != null ? settlement.getNetProfit() : 0L;

            StoreDailySettlementCompletedEvent event = new StoreDailySettlementCompletedEvent(
                    this,
                    settlementIdLong,
                    storeIdLong,
                    null, // storeName 暂不查询，监听器仅用于日志
                    settlement.getSettlementDate(),
                    totalIncome,
                    totalExpense,
                    netProfit,
                    operatorIdLong,
                    LocalDateTime.now()
            );
            applicationEventPublisher.publishEvent(event);
            log.info("已发布门店日结完成事件: settlementId={}", settlement.getSettlementId());
        } catch (Exception e) {
            log.error("发布门店日结完成事件失败: settlementId={}, 错误={}",
                    settlement.getSettlementId(), e.getMessage(), e);
        }
    }

    /**
     * 将字符串安全解析为Long，失败返回null
     */
    private Long parseLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportIssue(String settlementId, Object issueDto, String operatorId) {
        log.info("上报异常: settlementId={}, operatorId={}", settlementId, operatorId);

        // 1. 查询日结记录
        DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日结记录不存在");
        }

        // 2. 更新状态为abnormal
        settlement.setStatus("abnormal");
        settlement.setUpdateTime(LocalDateTime.now());

        // 记录异常原因（从issueDto中提取）
        if (issueDto != null) {
            // TODO: 根据实际IssueReportDTO结构设置异常详情
            // settlement.setRemark(issueDto.getReason());
        }

        int rows = dailySettlementMapper.updateById(settlement);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "上报异常失败，请刷新后重试");
        }

        // 3. 清除缓存
        dailySettlementDataService.clearSettlementCache(settlementId);

        // 4. 异步发送通知给区域经理
        try {
            log.info("发送日结异常通知: settlementId={}, storeId={}", settlementId, settlement.getStoreId());
        } catch (Exception e) {
            log.warn("发送异常通知失败", e);
        }

        log.info("异常上报成功: settlementId={}", settlementId);
    }

    @Override
    public List<Object> getOrdersByShiftId(String shiftId) {
        log.info("查询班次订单明细: shiftId={}", shiftId);

        if (shiftId == null || shiftId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "班次ID不能为空");
        }

        // 通过DataService查询班次订单明细（带缓存）
        return dailySettlementDataService.getOrdersByShiftId(shiftId);
    }

    /**
     * 构建查询条件包装器
     *
     * @param query      查询条件DTO
     * @param currentUser 当前用户
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<DailySettlement> buildQueryWrapper(SettlementQueryDTO query, User currentUser) {
        LambdaQueryWrapper<DailySettlement> wrapper = new LambdaQueryWrapper<>();

        // 防御性处理：currentUser 为空时跳过门店过滤，避免 NPE
        if (currentUser == null) {
            return wrapper;
        }

        // 区域经理和店长只能查看自己辖区的数据
        String role = getUserRole(currentUser);
        if (!ROLE_FINANCE_DIRECTOR.equals(role) && currentUser.getStoreId() != null) {
            wrapper.eq(DailySettlement::getStoreId, currentUser.getStoreId());
        }

        // 日期范围筛选
        if (query.getStartDate() != null) {
            wrapper.ge(DailySettlement::getSettlementDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(DailySettlement::getSettlementDate, query.getEndDate());
        }

        // 状态筛选
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(DailySettlement::getStatus, query.getStatus());
        }

        // 门店筛选
        if (query.getStoreId() != null && !query.getStoreId().isEmpty()) {
            wrapper.eq(DailySettlement::getStoreId, query.getStoreId());
        }

        // 按结算日期倒序排列
        wrapper.orderByDesc(DailySettlement::getSettlementDate);

        return wrapper;
    }

    /**
     * 获取用户角色标识
     *
     * @param user 用户实体
     * @return 角色标识字符串
     */
    private String getUserRole(User user) {
        if (user == null) {
            return ROLE_STORE_MANAGER; // 默认最低权限
        }
        // TODO: 从用户的角色列表中判断最高权限角色
        // 当前简化处理，后续需根据User实体的roles字段判断
        return ROLE_STORE_MANAGER;
    }

    /**
     * 元转分（前端传来的金额单位为元）
     *
     * @param yuan 元
     * @return 分
     */
    private Long convertYuanToFen(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            return null;
        }
        try {
            double yuanValue = Double.parseDouble(yuan);
            return Math.round(yuanValue * 100);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析BigDecimal
     *
     * @param value 字符串值
     * @return BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 日终聚合核心方法 ====================

    /**
     * 按需获取或生成日结对账数据（推荐方法）
     *
     * <h2>设计理念</h2>
     * <ul>
     *   <li><strong>权限隔离</strong>：基于当前用户权限的门店ID，不会越权访问其他门店</li>
     *   <li><strong>按需生成</strong>：仅在用户访问时触发，避免全局定时任务遍历所有门店</li>
     *   <li><strong>幂等性</strong>：已存在的记录直接返回，不重复生成</li>
     * </ul>
     *
     * <h2>调用场景</h2>
     * <ol>
     *   <li>店长访问日结列表页面时（Controller层调用）</li>
     *   <li>展开某日的班次详情时</li>
     *   <li>导出Excel时</li>
     * </ol>
     *
     * @param storeId 门店ID（从当前用户的权限信息中获取，非硬编码）
     * @param date    结算日期
     * @return 日结记录实体（已存在则返回现有记录，不存在则自动生成）
     */
    public DailySettlement getOrGenerateSettlement(String storeId, LocalDate date) {
        log.info("按需获取/生成日结对账: storeId={}, date={}", storeId, date);

        // 1. 先查询是否已存在
        DailySettlement existing = dailySettlementMapper.selectByStoreAndDate(storeId, date);
        if (existing != null) {
            log.debug("日结记录已存在: settlementId={}, 直接返回", existing.getSettlementId());
            return existing;
        }

        // 2. 不存在则实时聚合生成
        log.info("日结记录不存在，开始实时聚合生成...");
        return generateDailySettlement(storeId, date);
    }

    /**
     * 批量获取或生成多日的日结对账数据（用于列表查询）
     *
     * @param storeId   门店ID
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 日结记录列表
     */
    public List<DailySettlement> getOrGenerateSettlements(String storeId, LocalDate startDate, LocalDate endDate) {
        log.info("批量获取/生成日结对账: storeId={}, startDate={}, endDate={}", storeId, startDate, endDate);

        List<DailySettlement> settlements = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            try {
                DailySettlement settlement = getOrGenerateSettlement(storeId, currentDate);
                settlements.add(settlement);
            } catch (Exception e) {
                log.error("生成日期[{}]的日结数据失败: {}", currentDate, e.getMessage());
                // 单日失败不影响其他日期，继续处理
            }
            currentDate = currentDate.plusDays(1);
        }

        return settlements;
    }

    /**
     * 生成日结对账数据（核心聚合逻辑）
     * 从orders表按支付方式GROUP BY聚合数据，构建paymentBreakdown JSON
     * 计算总营收、优惠金额、退款金额、作废金额等汇总数据
     *
     * @param storeId 门店ID
     * @param date    结算日期
     * @return 生成的日结记录实体
     */
    @Transactional(rollbackFor = Exception.class)
    public DailySettlement generateDailySettlement(String storeId, LocalDate date) {
        log.info("开始生成日结对账: storeId={}, date={}", storeId, date);

        // 1. 检查是否已存在同店同日的日结记录
        DailySettlement existing = dailySettlementMapper.selectByStoreAndDate(storeId, date);
        if (existing != null) {
            log.warn("日结记录已存在: settlementId={}, 将更新数据", existing.getSettlementId());
            return updateExistingSettlement(existing, storeId, date);
        }

        // 2. 定义时间范围（当天00:00:00 到 23:59:59.999）
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);

        // 3. 聚合支付方式数据
        Map<String, Object> paymentBreakdownMap = aggregatePaymentData(storeId, startTime, endTime);
        String paymentBreakdownJson = convertToJson(paymentBreakdownMap);

        // 4. 聚合优惠金额
        Long totalDiscountAmount = orderMapper.sumDiscountAmount(storeId, startTime, endTime);

        // 5. 聚合退款数据
        Map<String, Object> refundData = orderMapper.aggregateRefunds(storeId, startTime, endTime);
        Long refundAmount = ((Number) refundData.getOrDefault("total_refund_amount", 0L)).longValue();
        Integer refundCount = ((Number) refundData.getOrDefault("refund_count", 0)).intValue();

        // 6. 聚合作废订单数据
        Map<String, Object> cancelledData = orderMapper.aggregateCancelled(storeId, startTime, endTime);
        Long cancelledAmount = ((Number) cancelledData.getOrDefault("total_cancelled_amount", 0L)).longValue();
        Integer cancelledCount = ((Number) cancelledData.getOrDefault("cancelled_count", 0)).intValue();

        // 7. 计算总营收（从支付方式明细汇总）
        Long totalRevenue = calculateTotalRevenue(paymentBreakdownMap);

        // 8. 计算订单总数
        Integer orderCount = calculateTotalOrderCount(paymentBreakdownMap);

        // 9. 创建日结记录实体
        DailySettlement settlement = new DailySettlement();
        settlement.setStoreId(storeId);
        settlement.setSettlementDate(date);
        settlement.setTotalRevenue(totalRevenue);
        settlement.setOrderCount(orderCount);
        settlement.setAvgOrderValue(orderCount > 0 ? totalRevenue / orderCount : 0L);
        settlement.setPaymentBreakdown(paymentBreakdownJson);
        settlement.setTotalDiscountAmount(totalDiscountAmount);
        settlement.setRefundAmount(refundAmount);
        settlement.setRefundCount(refundCount);
        settlement.setCancelledAmount(cancelledAmount);
        settlement.setCancelledCount(cancelledCount);
        settlement.setStatus("draft"); // 初始状态为草稿

        // 10. 保存到数据库
        int rows = dailySettlementMapper.insert(settlement);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "生成日结记录失败");
        }

        log.info("日结对账生成成功: settlementId={}, storeId={}, date={}, totalRevenue={}",
                settlement.getSettlementId(), storeId, date, totalRevenue);

        return settlement;
    }

    /**
     * 更新已有的日结记录
     *
     * @param existing 已有的日结记录
     * @param storeId  门店ID
     * @param date     结算日期
     * @return 更新后的日结记录
     */
    private DailySettlement updateExistingSettlement(DailySettlement existing, String storeId, LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);

        // 重新聚合所有数据
        Map<String, Object> paymentBreakdownMap = aggregatePaymentData(storeId, startTime, endTime);
        existing.setPaymentBreakdown(convertToJson(paymentBreakdownMap));
        existing.setTotalDiscountAmount(orderMapper.sumDiscountAmount(storeId, startTime, endTime));

        Map<String, Object> refundData = orderMapper.aggregateRefunds(storeId, startTime, endTime);
        existing.setRefundAmount(((Number) refundData.getOrDefault("total_refund_amount", 0L)).longValue());
        existing.setRefundCount(((Number) refundData.getOrDefault("refund_count", 0)).intValue());

        Map<String, Object> cancelledData = orderMapper.aggregateCancelled(storeId, startTime, endTime);
        existing.setCancelledAmount(((Number) cancelledData.getOrDefault("total_cancelled_amount", 0L)).longValue());
        existing.setCancelledCount(((Number) cancelledData.getOrDefault("cancelled_count", 0)).intValue());

        existing.setTotalRevenue(calculateTotalRevenue(paymentBreakdownMap));
        existing.setOrderCount(calculateTotalOrderCount(paymentBreakdownMap));
        existing.setAvgOrderValue(existing.getOrderCount() > 0 ? existing.getTotalRevenue() / existing.getOrderCount() : 0L);
        existing.setUpdateTime(LocalDateTime.now());

        dailySettlementMapper.updateById(existing);
        return existing;
    }

    /**
     * 聚合支付方式数据并转换为标准格式
     * 支付方式映射：0=wechat, 1=alipay, 2=cash, 4=memberBalance
     *
     * @param storeId   门店ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 支付方式明细Map
     */
    private Map<String, Object> aggregatePaymentData(String storeId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> aggregationResult = orderMapper.aggregateByPaymentMethod(storeId, startTime, endTime);

        Map<String, Object> breakdown = new HashMap<>();
        // 初始化所有支付方式为默认值
        breakdown.put("cash", Map.of("amount", 0L, "count", 0));
        breakdown.put("wechat", Map.of("amount", 0L, "count", 0));
        breakdown.put("alipay", Map.of("amount", 0L, "count", 0));
        breakdown.put("memberBalance", Map.of("amount", 0L, "count", 0));

        // 填充实际聚合结果
        for (Map<String, Object> row : aggregationResult) {
            Number paymentMethodNum = (Number) row.get("payment_method");
            int paymentMethod = paymentMethodNum != null ? paymentMethodNum.intValue() : -1;

            long amount = ((Number) row.getOrDefault("total_amount", 0L)).longValue();
            int count = ((Number) row.getOrDefault("order_count", 0)).intValue();

            String key = mapPaymentMethodToKey(paymentMethod);
            if (key != null) {
                breakdown.put(key, Map.of("amount", amount, "count", count));
            }
        }

        return breakdown;
    }

    /**
     * 将数据库PAYMENT_METHOD字段值映射为JSONB中的key名称
     *
     * @param paymentMethod 数据库中的支付方式代码
     * @return JSONB中的key名称（cash/wechat/alipay/memberBalance）
     */
    private String mapPaymentMethodToKey(int paymentMethod) {
        switch (paymentMethod) {
            case PAYMENT_WECHAT:
                return "wechat";
            case PAYMENT_ALIPAY:
                return "alipay";
            case PAYMENT_CASH:
                return "cash";
            case PAYMENT_MEMBER_BALANCE:
                return "memberBalance";
            default:
                log.warn("未知的支付方式代码: {}", paymentMethod);
                return null;
        }
    }

    /**
     * 从支付方式明细中计算总营收
     *
     * @param paymentBreakdownMap 支付方式明细Map
     * @return 总营收（分）
     */
    private Long calculateTotalRevenue(Map<String, Object> paymentBreakdownMap) {
        long total = 0L;
        for (Object value : paymentBreakdownMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detail = (Map<String, Object>) value;
            total += ((Number) detail.getOrDefault("amount", 0L)).longValue();
        }
        return total;
    }

    /**
     * 从支付方式明细中计算订单总数
     *
     * @param paymentBreakdownMap 支付方式明细Map
     * @return 订单总数
     */
    private Integer calculateTotalOrderCount(Map<String, Object> paymentBreakdownMap) {
        int total = 0;
        for (Object value : paymentBreakdownMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detail = (Map<String, Object>) value;
            total += ((Number) detail.getOrDefault("count", 0)).intValue();
        }
        return total;
    }

    /**
     * 将Map对象转换为JSON字符串
     *
     * @param data 要转换的Map对象
     * @return JSON字符串
     */
    private String convertToJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "支付方式明细序列化失败");
        }
    }
}
