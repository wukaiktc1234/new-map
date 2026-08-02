package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.ProductAnalysisReport;
import com.foodtraceability.mapper.ProductAnalysisReportMapper;
import com.foodtraceability.dto.ProductAnalysisVO;
import com.foodtraceability.dto.ProductAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.ProductAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 产品分析服务实现类
 * 实现产品（菜品/套餐）数据的分析和报表生成功能
 */
@Service
public class ProductAnalysisServiceImpl extends ServiceImpl<ProductAnalysisReportMapper, ProductAnalysisReport> implements ProductAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ProductAnalysisServiceImpl.class);

    /**
     * 获取菜品盈利能力分析
     */
    @Override
    public Map<String, Object> getProductProfitabilityAnalysis(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 从菜品表和配方表计算毛利
        // 毛利 = 售价 - 成本（配方成本）
        result.put("profitData", new ArrayList<>());
        result.put("totalGrossProfit", "0.00");
        result.put("avgGrossProfitRate", 0);

        return result;
    }

    /**
     * 生成产品分析报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAnalysisVO generateProductReport(LocalDate startDate, LocalDate endDate) {
        logger.info("开始生成产品分析报表，期间: {} - {}", startDate, endDate);

        ProductAnalysisReport report = new ProductAnalysisReport();
        report.setReportNo(generateReportNo("PA", startDate));
        report.setPeriodStart(java.sql.Date.valueOf(startDate));
        report.setPeriodEnd(java.sql.Date.valueOf(endDate));
        report.setGenerateTime(new Date());
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());

        try {
            // 聚合产品数据
            Map<String, Object> productData = aggregateProductData(startDate, endDate);

            report.setTotalFoodsCount((Integer) productData.getOrDefault("totalFoodsCount", 0));
            report.setTotalCombosCount((Integer) productData.getOrDefault("totalCombosCount", 0));
            report.setGrossProfitTotal((Long) productData.getOrDefault("grossProfitTotal", 0L));
            report.setGrossProfitRate((BigDecimal) productData.getOrDefault("grossProfitRate", BigDecimal.ZERO));

            // 保存报表
            this.save(report);

            logger.info("产品分析报表生成成功，ID: {}", report.getReportId());
            return convertToVO(report);
        } catch (Exception e) {
            logger.error("产品分析报表生成失败", e);
            throw new RuntimeException("产品分析报表生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取菜单工程数据
     */
    @Override
    public Map<String, Object> getMenuEngineeringData() {
        Map<String, Object> result = new HashMap<>();

        // TODO: 根据销量和毛利率进行四象限分类
        // 明星：高销量+高毛利
        // 金牛：高销量+低毛利
        // 问题：低销量+高毛利
        // 瘦狗：低销量+低毛利

        result.put("stars", new ArrayList<>());      // 明星
        result.put("cows", new ArrayList<>());       // 金牛
        result.put("questions", new ArrayList<>());   // 问题
        result.put("dogs", new ArrayList<>());       // 瘦狗

        return result;
    }

    /**
     * 获取套餐表现分析
     */
    @Override
    public List<Map<String, Object>> getComboPerformance(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> comboList = new ArrayList<>();

        // TODO: 统计各套餐的销售情况

        return comboList;
    }

    /**
     * 获取价格弹性分析
     */
    @Override
    public Map<String, Object> getPriceElasticity(Long foodId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 分析调价对销量的影响
        // 价格弹性 = (销量变化率 / 价格变化率)

        result.put("priceHistory", new ArrayList<>());
        result.put("salesHistory", new ArrayList<>());
        result.put("elasticity", 0);

        return result;
    }

    /**
     * 获取客户口味偏好
     */
    @Override
    public Map<String, Object> getCustomerPreferenceAnalysis() {
        Map<String, Object> result = new HashMap<>();

        // TODO: 从订单数据分析口味偏好
        result.put("spicePreference", new HashMap<>());   // 辣度偏好
        result.put("tastePreference", new HashMap<>());   // 口味偏好
        result.put("cuisinePreference", new HashMap<>());  // 菜系偏好

        return result;
    }

    /**
     * 分页查询产品报表
     */
    @Override
    public PageResult<ProductAnalysisVO> queryProductReports(ProductAnalysisQueryDTO queryDTO) {
        Page<ProductAnalysisReport> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<ProductAnalysisReport> queryWrapper = new QueryWrapper<>();

        if (queryDTO.getStartDate() != null) {
            queryWrapper.ge("period_start", queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            queryWrapper.le("period_end", queryDTO.getEndDate());
        }
        queryWrapper.orderByDesc("period_start");

        IPage<ProductAnalysisReport> pageResult = this.page(page, queryWrapper);

        PageResult<ProductAnalysisVO> result = new PageResult<>();
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
    public ProductAnalysisVO getReportById(Long reportId) {
        ProductAnalysisReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        return convertToVO(report);
    }

    /**
     * 聚合产品数据
     */
    private Map<String, Object> aggregateProductData(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();

        // TODO: 从菜品表统计在售数、从订单表计算毛利
        data.put("totalFoodsCount", 0);
        data.put("totalCombosCount", 0);
        data.put("grossProfitTotal", 0L);
        data.put("grossProfitRate", BigDecimal.ZERO);

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
    private ProductAnalysisVO convertToVO(ProductAnalysisReport report) {
        ProductAnalysisVO vo = new ProductAnalysisVO();
        vo.setReportId(report.getReportId());
        vo.setReportNo(report.getReportNo());
        vo.setPeriodStart(formatDate(report.getPeriodStart()));
        vo.setPeriodEnd(formatDate(report.getPeriodEnd()));
        vo.setTotalFoodsCount(report.getTotalFoodsCount());
        vo.setTotalCombosCount(report.getTotalCombosCount());
        vo.setGrossProfitTotal(fenToYuan(report.getGrossProfitTotal()));
        vo.setGrossProfitRate(report.getGrossProfitRate());
        vo.setGenerateTime(formatDateTime(report.getGenerateTime()));

        return vo;
    }

    /**
     * 批量转换
     */
    private List<ProductAnalysisVO> convertToVOList(List<ProductAnalysisReport> reports) {
        List<ProductAnalysisVO> voList = new ArrayList<>();
        for (ProductAnalysisReport report : reports) {
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
