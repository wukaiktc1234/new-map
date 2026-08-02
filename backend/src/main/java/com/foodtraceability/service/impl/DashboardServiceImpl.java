package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.DashboardConfig;
import com.foodtraceability.entity.SalesAnalysisReport;
import com.foodtraceability.entity.InventoryAnalysisReport;
import com.foodtraceability.entity.CustomerAnalysisReport;
import com.foodtraceability.mapper.DashboardConfigMapper;
import com.foodtraceability.mapper.SalesOrderMapper;
import com.foodtraceability.mapper.MemberMapper;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.dto.DashboardConfigVO;
import com.foodtraceability.dto.DashboardConfigQueryDTO;
import com.foodtraceability.dto.DashboardConfigCreateDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 看板服务实现类
 * 实现经营看板的数据聚合和配置管理功能
 */
@Service
public class DashboardServiceImpl extends ServiceImpl<DashboardConfigMapper, DashboardConfig> implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final SalesOrderMapper salesOrderMapper;
    private final MemberMapper memberMapper;
    private final InventoryMapper inventoryMapper;

    public DashboardServiceImpl(SalesOrderMapper salesOrderMapper, MemberMapper memberMapper, InventoryMapper inventoryMapper) {
        this.salesOrderMapper = salesOrderMapper;
        this.memberMapper = memberMapper;
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * 获取看板概览数据
     * @param dashboardType 看板类型
     * @return 看板概览数据
     */
    @Override
    public Map<String, Object> getDashboardOverview(Integer dashboardType) {
        Map<String, Object> result = new HashMap<>();

        switch (dashboardType) {
            case 1: // 总览
                result.putAll(getTodaySalesOverview());
                result.put("inventoryAlerts", getInventoryAlerts());
                break;
            case 2: // 销售
                result.putAll(getTodaySalesOverview());
                break;
            case 3: // 库存
                result.putAll(getInventoryAlerts());
                break;
            case 4: // 财务
                result.put("financeSummary", getFinanceSummary());
                break;
            case 5: // 会员
                result.put("memberGrowth", getMemberGrowthTrend(30));
                break;
            default:
                throw new RuntimeException("不支持的看板类型: " + dashboardType);
        }

        return result;
    }

    /**
     * 获取今日销售概览
     * @return 今日销售数据
     */
    @Override
    public Map<String, Object> getTodaySalesOverview() {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        try {
            // 查询今日订单统计（通过SalesOrderMapper读取销售模块数据）
            Long todayOrderCount = salesOrderMapper.selectCount(
                    new QueryWrapper<com.foodtraceability.entity.SalesOrder>()
                            .ge("order_time", startOfDay)
                            .le("order_time", endOfDay)
                            .eq("status", "completed") // 已完成
            );

            result.put("todayOrderCount", todayOrderCount != null ? todayOrderCount : 0);
            result.put("todayAmount", 0); // TODO: 聚合计算今日销售额
            result.put("yesterdayGrowth", 0); // TODO: 计算环比增长

            logger.info("获取今日销售概览成功");
        } catch (Exception e) {
            logger.error("获取今日销售概览失败", e);
            result.put("todayOrderCount", 0);
            result.put("todayAmount", 0);
        }

        return result;
    }

    /**
     * 获取库存预警信息
     * @return 库存预警列表
     */
    @Override
    public Map<String, Object> getInventoryAlerts() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 查询预警数量（通过InventoryMapper读取仓储模块数据）
            Long warningCount = inventoryMapper.selectCount(
                    new QueryWrapper<com.foodtraceability.entity.Inventory>()
                            .eq("warning_status", 1)
            );

            result.put("warningCount", warningCount != null ? warningCount : 0);
            result.put("outOfStockCount", 0); // TODO: 查询缺货数
            result.put("overstockCount", 0); // TODO: 查询积压数

            logger.info("获取库存预警信息成功");
        } catch (Exception e) {
            logger.error("获取库存预警信息失败", e);
            result.put("warningCount", 0);
        }

        return result;
    }

    /**
     * 获取会员增长数据
     * @param days 最近天数
     * @return 会员增长趋势
     */
    @Override
    public Map<String, Object> getMemberGrowthTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 查询会员总数（通过MemberMapper读取营销模块数据）
            Long totalMembers = memberMapper.selectCount(null);
            LocalDate startDate = LocalDate.now().minusDays(days);

            // 查询新增会员数
            Long newMembers = memberMapper.selectCount(
                    new QueryWrapper<com.foodtraceability.entity.Member>()
                            .ge("create_time", startDate.atStartOfDay())
            );

            result.put("totalMembers", totalMembers != null ? totalMembers : 0);
            result.put("newMembers", newMembers != null ? newMembers : 0);
            result.put("growthRate", calculateGrowthRate(newMembers, totalMembers));

            logger.info("获取会员增长数据成功");
        } catch (Exception e) {
            logger.error("获取会员增长数据失败", e);
            result.put("totalMembers", 0);
            result.put("newMembers", 0);
        }

        return result;
    }

    /**
     * 获取财务概况
     * @return 财务关键指标
     */
    @Override
    public Map<String, Object> getFinanceSummary() {
        Map<String, Object> result = new HashMap<>();

        // TODO: 从财务模块聚合数据
        result.put("monthlyRevenue", 0);
        result.put("monthlyExpense", 0);
        result.put("monthlyProfit", 0);
        result.put("profitMargin", 0);

        return result;
    }

    /**
     * 保存看板配置
     * @param dto 配置信息
     * @return 配置ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDashboardConfig(DashboardConfigCreateDTO dto) {
        DashboardConfig config = new DashboardConfig();
        config.setUserId(dto.getUserId());
        config.setDashboardType(dto.getDashboardType());
        config.setWidgetLayout(dto.getWidgetLayout());
        config.setRefreshInterval(dto.getRefreshInterval());
        config.setIsDefault(dto.getIsDefault());
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());

        this.save(config);

        logger.info("保存看板配置成功，ID: {}, 用户ID: {}", config.getConfigId(), dto.getUserId());
        return config.getConfigId();
    }

    /**
     * 获取用户看板配置
     * @param userId 用户ID
     * @param dashboardType 看板类型
     * @return 配置信息
     */
    @Override
    public DashboardConfigVO getUserDashboardConfig(Long userId, Integer dashboardType) {
        QueryWrapper<DashboardConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                     .eq("dashboard_type", dashboardType)
                     .last("LIMIT 1");

        DashboardConfig config = this.getOne(queryWrapper);

        if (config == null) {
            return null;
        }

        return convertToVO(config);
    }

    /**
     * 分页查询看板配置
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<DashboardConfigVO> queryDashboardConfigs(DashboardConfigQueryDTO queryDTO) {
        Page<DashboardConfig> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<DashboardConfig> queryWrapper = new QueryWrapper<>();

        if (queryDTO.getUserId() != null) {
            queryWrapper.eq("user_id", queryDTO.getUserId());
        }
        if (queryDTO.getDashboardType() != null) {
            queryWrapper.eq("dashboard_type", queryDTO.getDashboardType());
        }
        queryWrapper.orderByDesc("create_time");

        IPage<DashboardConfig> pageResult = this.page(page, queryWrapper);

        PageResult<DashboardConfigVO> result = new PageResult<>();
        result.setRecords(convertToVOList(pageResult.getRecords()));
        result.setTotal(pageResult.getTotal());
        result.setCurrent(pageResult.getCurrent());
        result.setSize(pageResult.getSize());

        return result;
    }

    /**
     * 删除看板配置
     * @param configId 配置ID
     * @param userId 用户ID（权限校验）
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDashboardConfig(Long configId, Long userId) {
        // 权限校验：只能删除自己的配置
        DashboardConfig config = this.getById(configId);
        if (config == null || !config.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此配置");
        }

        boolean result = this.removeById(configId);
        logger.info("删除看板配置，ID: {}, 用户ID: {}", configId, userId);
        return result;
    }

    /**
     * 将实体转换为VO
     */
    private DashboardConfigVO convertToVO(DashboardConfig config) {
        DashboardConfigVO vo = new DashboardConfigVO();
        vo.setConfigId(config.getConfigId());
        vo.setUserId(config.getUserId());
        vo.setDashboardType(config.getDashboardType());
        vo.setDashboardTypeName(getDashboardTypeName(config.getDashboardType()));
        vo.setWidgetLayout(config.getWidgetLayout());
        vo.setRefreshInterval(config.getRefreshInterval());
        vo.setIsDefault(config.getIsDefault());
        vo.setCreateTime(formatDate(config.getCreateTime()));
        return vo;
    }

    /**
     * 批量转换实体列表为VO列表
     */
    private List<DashboardConfigVO> convertToVOList(List<DashboardConfig> configs) {
        List<DashboardConfigVO> voList = new ArrayList<>();
        for (DashboardConfig config : configs) {
            voList.add(convertToVO(config));
        }
        return voList;
    }

    /**
     * 获取看板类型名称
     */
    private String getDashboardTypeName(Integer type) {
        switch (type) {
            case 1: return "总览";
            case 2: return "销售";
            case 3: return "库存";
            case 4: return "财务";
            case 5: return "会员";
            default: return "未知";
        }
    }

    /**
     * 计算增长率
     */
    private double calculateGrowthRate(Long newValue, Long totalValue) {
        if (totalValue == null || totalValue == 0) {
            return 0;
        }
        return (newValue.doubleValue() / totalValue.doubleValue()) * 100;
    }

    /**
     * 格式化日期
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
