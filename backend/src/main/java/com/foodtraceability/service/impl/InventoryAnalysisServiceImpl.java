package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryAnalysisReport;
import com.foodtraceability.mapper.InventoryAnalysisReportMapper;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.dto.InventoryAnalysisVO;
import com.foodtraceability.dto.InventoryAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.InventoryAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 库存分析服务实现类
 * 实现库存数据的分析和报表生成功能
 */
@Service
public class InventoryAnalysisServiceImpl extends ServiceImpl<InventoryAnalysisReportMapper, InventoryAnalysisReport> implements InventoryAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryAnalysisServiceImpl.class);

    private final InventoryMapper inventoryMapper;

    public InventoryAnalysisServiceImpl(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * 获取库存概览
     */
    @Override
    public Map<String, Object> getInventoryOverview() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 查询SKU总数（从仓储模块读取）
            Long totalSkuCount = inventoryMapper.selectCount(null);

            result.put("totalSkuCount", totalSkuCount != null ? totalSkuCount : 0);
            result.put("totalInventoryValue", "0.00"); // TODO: SUM(quantity * unit_price)
            result.put("outOfStockCount", 0); // TODO: 统计缺货SKU
            result.put("warningCount", 0); // TODO: 统计预警数

            logger.info("获取库存概览成功");
        } catch (Exception e) {
            logger.error("获取库存概览失败", e);
            result.put("totalSkuCount", 0);
        }

        return result;
    }

    /**
     * 生成库存分析报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAnalysisVO generateInventoryReport(LocalDate date) {
        logger.info("开始生成库存分析报表，日期: {}", date);

        InventoryAnalysisReport report = new InventoryAnalysisReport();
        report.setReportNo(generateReportNo("IA", date));
        report.setReportDate(java.sql.Date.valueOf(date));
        report.setGenerateTime(new Date());
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());

        try {
            // 聚合库存数据
            Map<String, Object> inventoryData = aggregateInventoryData();

            report.setTotalSkuCount((Integer) inventoryData.getOrDefault("totalSkuCount", 0));
            report.setTotalInventoryValue((Long) inventoryData.getOrDefault("totalInventoryValue", 0L));
            report.setTurnoverRate((BigDecimal) inventoryData.getOrDefault("turnoverRate", BigDecimal.ZERO));
            report.setOutOfStockCount((Integer) inventoryData.getOrDefault("outOfStockCount", 0));
            report.setOverstockCount((Integer) inventoryData.getOrDefault("overstockCount", 0));
            report.setWasteAmount((Long) inventoryData.getOrDefault("wasteAmount", 0L));
            report.setWarningCount((Integer) inventoryData.getOrDefault("warningCount", 0));

            // 保存报表
            this.save(report);

            logger.info("库存分析报表生成成功，ID: {}", report.getReportId());
            return convertToVO(report);
        } catch (Exception e) {
            logger.error("库存分析报表生成失败，日期: {}", date, e);
            throw new RuntimeException("库存分析报表生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取周转率分析
     */
    @Override
    public Map<String, Object> getInventoryTurnoverAnalysis(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 计算各品类的库存周转率
        result.put("turnoverData", new ArrayList<>());

        return result;
    }

    /**
     * 获取ABC分类
     */
    @Override
    public List<Map<String, Object>> getABCClassification() {
        List<Map<String, Object>> abcList = new ArrayList<>();

        // TODO: 根据周转率进行ABC分类
        // A类：高周转（周转率>阈值1）
        // B类：中等周转（阈值2 <= 周转率 <= 阈值1）
        // C类：滞销（周转率 < 阈值2）

        return abcList;
    }

    /**
     * 获取报损分析
     */
    @Override
    public Map<String, Object> getWasteAnalysis(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 从报损记录表聚合数据
        result.put("wasteByReason", new ArrayList<>());
        result.put("wasteByCategory", new ArrayList<>());
        result.put("wasteByTime", new ArrayList<>());

        return result;
    }

    /**
     * 获取缺货风险列表
     */
    @Override
    public List<Map<String, Object>> getStockoutRiskList() {
        List<Map<String, Object>> riskList = new ArrayList<>();

        // TODO: 查询低于安全库存的物料
        // WHERE quantity < safety_stock

        return riskList;
    }

    /**
     * 获取各仓库库存价值分布
     */
    @Override
    public List<Map<String, Object>> getInventoryValueByWarehouse() {
        List<Map<String, Object>> warehouseValues = new ArrayList<>();

        // TODO: 按仓库分组统计库存价值

        return warehouseValues;
    }

    /**
     * 分页查询库存报表
     */
    @Override
    public PageResult<InventoryAnalysisVO> queryInventoryReports(InventoryAnalysisQueryDTO queryDTO) {
        Page<InventoryAnalysisReport> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<InventoryAnalysisReport> queryWrapper = new QueryWrapper<>();

        if (queryDTO.getStartDate() != null) {
            queryWrapper.ge("report_date", queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            queryWrapper.le("report_date", queryDTO.getEndDate());
        }
        queryWrapper.orderByDesc("report_date");

        IPage<InventoryAnalysisReport> pageResult = this.page(page, queryWrapper);

        PageResult<InventoryAnalysisVO> result = new PageResult<>();
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
    public InventoryAnalysisVO getReportById(Long reportId) {
        InventoryAnalysisReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        return convertToVO(report);
    }

    /**
     * 聚合库存数据
     */
    private Map<String, Object> aggregateInventoryData() {
        Map<String, Object> data = new HashMap<>();

        try {
            Long totalSkuCount = inventoryMapper.selectCount(null);

            data.put("totalSkuCount", totalSkuCount != null ? totalSkuCount.intValue() : 0);
            data.put("totalInventoryValue", 0L); // TODO: 计算库存总值
            data.put("turnoverRate", BigDecimal.ZERO); // TODO: 计算周转率
            data.put("outOfStockCount", 0);
            data.put("overstockCount", 0);
            data.put("wasteAmount", 0L);
            data.put("warningCount", 0);

        } catch (Exception e) {
            logger.error("聚合库存数据失败", e);
            data.put("totalSkuCount", 0);
        }

        return data;
    }

    /**
     * 生成报表编号
     */
    private String generateReportNo(String prefix, LocalDate date) {
        String dateStr = date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("%s%s%04d", prefix, dateStr,
                System.currentTimeMillis() % 10000);
    }

    /**
     * 将实体转换为VO
     */
    private InventoryAnalysisVO convertToVO(InventoryAnalysisReport report) {
        InventoryAnalysisVO vo = new InventoryAnalysisVO();
        vo.setReportId(report.getReportId());
        vo.setReportNo(report.getReportNo());
        vo.setReportDate(formatDate(report.getReportDate()));
        vo.setTotalSkuCount(report.getTotalSkuCount());
        vo.setTotalInventoryValue(fenToYuan(report.getTotalInventoryValue()));
        vo.setTurnoverRate(report.getTurnoverRate());
        vo.setOutOfStockCount(report.getOutOfStockCount());
        vo.setOverstockCount(report.getOverstockCount());
        vo.setWasteAmount(fenToYuan(report.getWasteAmount()));
        vo.setWarningCount(report.getWarningCount());
        vo.setGenerateTime(formatDateTime(report.getGenerateTime()));

        return vo;
    }

    /**
     * 批量转换
     */
    private List<InventoryAnalysisVO> convertToVOList(List<InventoryAnalysisReport> reports) {
        List<InventoryAnalysisVO> voList = new ArrayList<>();
        for (InventoryAnalysisReport report : reports) {
            voList.add(convertToVO(report));
        }
        return voList;
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
