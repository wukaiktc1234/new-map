package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SalesAnalysisReport;
import com.foodtraceability.mapper.SalesAnalysisReportMapper;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.dto.SalesAnalysisVO;
import com.foodtraceability.dto.SalesAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.SalesAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 销售分析服务实现类
 * 实现销售数据的分析和报表生成功能
 */
@Service
public class SalesAnalysisServiceImpl extends ServiceImpl<SalesAnalysisReportMapper, SalesAnalysisReport> implements SalesAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(SalesAnalysisServiceImpl.class);

    private final OrderMapper orderMapper;

    public SalesAnalysisServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 生成日报
     * @param date 日期
     * @return 销售分析报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesAnalysisVO generateDailySalesReport(LocalDate date) {
        logger.info("开始生成销售日报，日期: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 创建报表记录
        SalesAnalysisReport report = new SalesAnalysisReport();
        report.setReportNo(generateReportNo("SA", "D", date));
        report.setReportType(1); // 日报
        report.setPeriodStart(java.sql.Date.valueOf(date));
        report.setPeriodEnd(java.sql.Date.valueOf(date));
        report.setGenerateTime(new Date());
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());

        try {
            // 聚合销售数据（从订单表读取）
            Map<String, Object> salesData = aggregateSalesData(startOfDay, endOfDay);

            report.setTotalOrders((Integer) salesData.getOrDefault("totalOrders", 0));
            report.setTotalAmount((Long) salesData.getOrDefault("totalAmount", 0L));
            report.setAvgOrderValue(calculateAvgOrderValue(
                    report.getTotalOrders(),
                    report.getTotalAmount()
            ));
            report.setRefundCount((Integer) salesData.getOrDefault("refundCount", 0));
            report.setRefundAmount((Long) salesData.getOrDefault("refundAmount", 0L));
            report.setDineInAmount((Long) salesData.getOrDefault("dineInAmount", 0L));
            report.setTakeoutAmount((Long) salesData.getOrDefault("takeoutAmount", 0L));
            report.setSelfPickupAmount((Long) salesData.getOrDefault("selfPickupAmount", 0L));
            report.setPeakHour((Integer) salesData.getOrDefault("peakHour", null));

            // TODO: 获取TOP10菜品和支付方式统计
            report.setTopFoodsJson(null);
            report.setPaymentMethodStats(null);

            // 保存报表
            this.save(report);

            logger.info("销售日报生成成功，ID: {}, 订单数: {}", report.getReportId(), report.getTotalOrders());
            return convertToVO(report);
        } catch (Exception e) {
            logger.error("销售日报生成失败，日期: {}", date, e);
            throw new RuntimeException("销售日报生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成周报
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesAnalysisVO generateWeeklySalesReport(LocalDate startDate, LocalDate endDate) {
        // 类似日报逻辑，按周聚合
        SalesAnalysisReport report = new SalesAnalysisReport();
        report.setReportNo(generateReportNo("SA", "W", startDate));
        report.setReportType(2); // 周报
        report.setPeriodStart(java.sql.Date.valueOf(startDate));
        report.setPeriodEnd(java.sql.Date.valueOf(endDate));
        report.setGenerateTime(new Date());
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());

        this.save(report);
        return convertToVO(report);
    }

    /**
     * 获取销售趋势
     */
    @Override
    public Map<String, Object> getSalesTrend(LocalDate startDate, LocalDate endDate, String granularity) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> trendData = new ArrayList<>();

        // TODO: 根据粒度（day/week/month）聚合销售数据
        result.put("trendData", trendData);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("granularity", granularity);

        return result;
    }

    /**
     * 获取品类销售占比
     */
    @Override
    public Map<String, Object> getCategorySalesAnalysis(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 按品类聚合销售额
        result.put("categoryData", new ArrayList<>());

        return result;
    }

    /**
     * 获取热销菜品TOP N
     */
    @Override
    public List<Map<String, Object>> getTopSellingFoods(Integer limit, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> topFoods = new ArrayList<>();

        // TODO: 从订单明细表统计热销菜品
        return topFoods;
    }

    /**
     * 获取时段分布
     */
    @Override
    public Map<String, Object> getHourlyDistribution(LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> hourlyData = new ArrayList<>();

        // TODO: 按小时统计订单分布
        for (int i = 0; i < 24; i++) {
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", i);
            hourData.put("orderCount", 0);
            hourData.put("amount", 0);
            hourlyData.add(hourData);
        }

        result.put("hourlyData", hourlyData);
        result.put("date", date.toString());

        return result;
    }

    /**
     * 获取渠道对比
     */
    @Override
    public Map<String, Object> getChannelComparison(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 按渠道（堂食/外卖/自提）对比销售数据
        result.put("channels", new ArrayList<>());

        return result;
    }

    /**
     * 分页查询销售报表
     */
    @Override
    public PageResult<SalesAnalysisVO> querySalesReports(SalesAnalysisQueryDTO queryDTO) {
        Page<SalesAnalysisReport> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<SalesAnalysisReport> queryWrapper = new QueryWrapper<>();

        if (queryDTO.getReportType() != null) {
            queryWrapper.eq("report_type", queryDTO.getReportType());
        }
        if (queryDTO.getStartDate() != null) {
            queryWrapper.ge("period_start", queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            queryWrapper.le("period_end", queryDTO.getEndDate());
        }
        queryWrapper.orderByDesc("period_start");

        IPage<SalesAnalysisReport> pageResult = this.page(page, queryWrapper);

        PageResult<SalesAnalysisVO> result = new PageResult<>();
        result.setRecords(convertToVOList(pageResult.getRecords()));
        result.setTotal(pageResult.getTotal());
        result.setCurrent(pageResult.getCurrent());
        result.setSize(pageResult.getSize());

        return result;
    }

    /**
     * 根据ID获取报表详情
     */
    @Override
    public SalesAnalysisVO getReportById(Long reportId) {
        SalesAnalysisReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        return convertToVO(report);
    }

    /**
     * 聚合销售数据
     */
    private Map<String, Object> aggregateSalesData(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> data = new HashMap<>();

        try {
            // 查询订单总数和总金额
            Long orderCount = orderMapper.selectCount(
                    new QueryWrapper<com.foodtraceability.entity.Order>()
                            .ge("create_time", start)
                            .le("create_time", end)
                            .eq("order_status", 2) // 已完成
            );

            data.put("totalOrders", orderCount != null ? orderCount.intValue() : 0);
            data.put("totalAmount", 0L); // TODO: SUM(order_amount)
            data.put("refundCount", 0); // TODO: 统计退款订单
            data.put("refundAmount", 0L);
            data.put("dineInAmount", 0L); // TODO: 按渠道统计
            data.put("takeoutAmount", 0L);
            data.put("selfPickupAmount", 0L);
            data.put("peakHour", null); // TODO: 找出高峰时段

        } catch (Exception e) {
            logger.error("聚合销售数据失败", e);
            data.put("totalOrders", 0);
            data.put("totalAmount", 0L);
        }

        return data;
    }

    /**
     * 计算客单价
     */
    private Long calculateAvgOrderValue(Integer orderCount, Long totalAmount) {
        if (orderCount == null || orderCount == 0) {
            return 0L;
        }
        return totalAmount / orderCount;
    }

    /**
     * 生成报表编号
     */
    private String generateReportNo(String prefix, String type, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("%s%s%s%04d", prefix, type, dateStr,
                System.currentTimeMillis() % 10000);
    }

    /**
     * 将实体转换为VO
     */
    private SalesAnalysisVO convertToVO(SalesAnalysisReport report) {
        SalesAnalysisVO vo = new SalesAnalysisVO();
        vo.setReportId(report.getReportId());
        vo.setReportNo(report.getReportNo());
        vo.setReportType(report.getReportType());
        vo.setReportTypeName(getReportTypeName(report.getReportType()));
        vo.setPeriodStart(formatDate(report.getPeriodStart()));
        vo.setPeriodEnd(formatDate(report.getPeriodEnd()));
        vo.setTotalOrders(report.getTotalOrders());
        vo.setTotalAmount(fenToYuan(report.getTotalAmount()));
        vo.setAvgOrderValue(fenToYuan(report.getAvgOrderValue()));
        vo.setRefundCount(report.getRefundCount());
        vo.setRefundAmount(fenToYuan(report.getRefundAmount()));
        vo.setDineInAmount(fenToYuan(report.getDineInAmount()));
        vo.setTakeoutAmount(fenToYuan(report.getTakeoutAmount()));
        vo.setSelfPickupAmount(fenToYuan(report.getSelfPickupAmount()));
        vo.setPeakHour(report.getPeakHour());
        vo.setGenerateTime(formatDateTime(report.getGenerateTime()));

        // 解析JSON字段
        // vo.setTopFoods(parseJson(report.getTopFoodsJson()));
        // vo.setPaymentMethodStats(parseJson(report.getPaymentMethodStats()));

        return vo;
    }

    /**
     * 批量转换
     */
    private List<SalesAnalysisVO> convertToVOList(List<SalesAnalysisReport> reports) {
        List<SalesAnalysisVO> voList = new ArrayList<>();
        for (SalesAnalysisReport report : reports) {
            voList.add(convertToVO(report));
        }
        return voList;
    }

    /**
     * 获取报表类型名称
     */
    private String getReportTypeName(Integer type) {
        switch (type) {
            case 1: return "日报";
            case 2: return "周报";
            case 3: return "月报";
            case 4: return "季报";
            case 5: return "年报";
            default: return "未知";
        }
    }

    /**
     * 分转元
     */
    private String fenToYuan(Long fen) {
        if (fen == null) {
            return "0.00";
        }
        return String.format("%.2f", fen / 100.0);
    }

    /**
     * 格式化日期
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
