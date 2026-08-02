package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.CustomReport;
import com.foodtraceability.entity.CustomReportExecution;
import com.foodtraceability.mapper.CustomReportMapper;
import com.foodtraceability.mapper.CustomReportExecutionMapper;
import com.foodtraceability.dto.CustomReportVO;
import com.foodtraceability.dto.CustomReportCreateDTO;
import com.foodtraceability.dto.CustomReportQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.CustomReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 自定义报表服务实现类
 * 实现自定义SQL报表的创建、执行和管理功能
 */
@Service
public class CustomReportServiceImpl extends ServiceImpl<CustomReportMapper, CustomReport> implements CustomReportService {

    private static final Logger logger = LoggerFactory.getLogger(CustomReportServiceImpl.class);

    private final CustomReportExecutionMapper executionMapper;

    public CustomReportServiceImpl(CustomReportExecutionMapper executionMapper) {
        this.executionMapper = executionMapper;
    }

    /**
     * 创建自定义报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomReport(CustomReportCreateDTO dto, Long ownerUserId) {
        // SQL安全校验
        validateSqlSafety(dto.getSqlQuery());

        CustomReport report = new CustomReport();
        report.setReportName(dto.getReportName());
        report.setReportDescription(dto.getReportDescription());
        report.setOwnerUserId(ownerUserId);
        report.setSqlQuery(dto.getSqlQuery());
        report.setQueryParamsConfig(dto.getQueryParamsConfig());
        report.setChartType(dto.getChartType());
        report.setScheduleCron(dto.getScheduleCron());
        report.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());

        this.save(report);

        logger.info("创建自定义报表成功，ID: {}, 名称: {}", report.getReportId(), dto.getReportName());
        return report.getReportId();
    }

    /**
     * 执行自定义报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeCustomReport(Long reportId, Map<String, Object> params, Long executedBy) {
        // 查询报表配置
        CustomReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }

        // 创建执行记录
        CustomReportExecution execution = new CustomReportExecution();
        execution.setReportId(reportId);
        execution.setExecutedBy(executedBy);
        execution.setExecutionStatus(1); // 运行中
        execution.setExecutedAt(new Date());
        execution.setCreateTime(new Date());
        execution.setUpdateTime(new Date());

        executionMapper.insert(execution);

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        try {
            // SQL注入防护检查
            validateSqlSafety(report.getSqlQuery());

            // TODO: 执行动态SQL查询（需要使用JdbcTemplate或MyBatis动态SQL）
            // 注意：生产环境必须严格限制SQL权限，建议只允许SELECT语句

            result.put("executionId", execution.getExecutionId());
            result.put("status", "success");
            result.put("data", new ArrayList<>()); // 查询结果

            long endTime = System.currentTimeMillis();

            // 更新执行记录为成功
            execution.setExecutionStatus(2); // 成功
            execution.setResultDataJson(convertToJson(result.get("data")));
            execution.setExecutionTimeMs(endTime - startTime);
            execution.setUpdateTime(new Date());
            executionMapper.updateById(execution);

            // 更新报表最后执行时间
            report.setLastExecutedAt(new Date());
            this.updateById(report);

            logger.info("执行自定义报表成功，报表ID: {}, 耗时: {}ms", reportId, (endTime - startTime));
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();

            // 更新执行记录为失败
            execution.setExecutionStatus(3); // 失败
            execution.setErrorMessage(e.getMessage());
            execution.setExecutionTimeMs(endTime - startTime);
            execution.setUpdateTime(new Date());
            executionMapper.updateById(execution);

            logger.error("执行自定义报表失败，报表ID: {}", reportId, e);
            throw new RuntimeException("报表执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 设置定时调度
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scheduleReport(Long reportId, String cronExpression, Long userId) {
        CustomReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }

        // 权限校验：只有所有者或管理员可以设置调度
        if (!report.getOwnerUserId().equals(userId)) {
            throw new RuntimeException("无权操作此报表");
        }

        // 验证cron表达式格式
        if (cronExpression != null && !cronExpression.isEmpty()) {
            if (!isValidCronExpression(cronExpression)) {
                throw new RuntimeException("无效的cron表达式");
            }
        }

        report.setScheduleCron(cronExpression);
        report.setUpdateTime(new Date());

        boolean result = this.updateById(report);

        if (result) {
            logger.info("设置报表定时调度成功，报表ID: {}, cron: {}", reportId, cronExpression);
        }

        return result;
    }

    /**
     * 获取执行历史
     */
    @Override
    public PageResult<Map<String, Object>> getReportHistory(Long reportId, Integer pageNum, Integer pageSize) {
        Page<CustomReportExecution> page = new Page<>(pageNum, pageSize);
        QueryWrapper<CustomReportExecution> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("report_id", reportId)
                     .orderByDesc("executed_at");

        IPage<CustomReportExecution> pageResult = executionMapper.selectPage(page, queryWrapper);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(convertExecutionToMapList(pageResult.getRecords()));
        result.setTotal(pageResult.getTotal());
        result.setCurrent(pageResult.getCurrent());
        result.setSize(pageResult.getSize());

        return result;
    }

    /**
     * 分页查询自定义报表
     */
    @Override
    public PageResult<CustomReportVO> queryCustomReports(CustomReportQueryDTO queryDTO, Long userId) {
        Page<CustomReport> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<CustomReport> queryWrapper = new QueryWrapper<>();

        if (queryDTO.getReportName() != null && !queryDTO.getReportName().isEmpty()) {
            queryWrapper.like("report_name", queryDTO.getReportName());
        }
        if (queryDTO.getOwnerUserId() != null) {
            queryWrapper.eq("owner_user_id", queryDTO.getOwnerUserId());
        }
        if (queryDTO.getChartType() != null) {
            queryWrapper.eq("chart_type", queryDTO.getChartType());
        }
        if (Boolean.TRUE.equals(queryDTO.getIsPublicOnly())) {
            queryWrapper.eq("is_public", true);
        } else {
            // 默认只显示自己的和公开的
            queryWrapper.and(w -> w.eq("owner_user_id", userId)
                                .or()
                                .eq("is_public", true));
        }
        queryWrapper.orderByDesc("create_time");

        IPage<CustomReport> pageResult = this.page(page, queryWrapper);

        PageResult<CustomReportVO> result = new PageResult<>();
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
    public CustomReportVO getReportById(Long reportId) {
        CustomReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        return convertToVO(report);
    }

    /**
     * 删除自定义报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCustomReport(Long reportId, Long userId) {
        CustomReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }

        // 权限校验：只能删除自己的报表
        if (!report.getOwnerUserId().equals(userId)) {
            throw new RuntimeException("无权删除此报表");
        }

        boolean result = this.removeById(reportId);

        if (result) {
            logger.info("删除自定义报表成功，ID: {}", reportId);
        }

        return result;
    }

    /**
     * 更新自定义报表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCustomReport(Long reportId, CustomReportCreateDTO dto, Long userId) {
        CustomReport report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }

        // 权限校验
        if (!report.getOwnerUserId().equals(userId)) {
            throw new RuntimeException("无权修改此报表");
        }

        // 如果更新了SQL，需要重新校验安全性
        if (dto.getSqlQuery() != null && !dto.getSqlQuery().equals(report.getSqlQuery())) {
            validateSqlSafety(dto.getSqlQuery());
        }

        report.setReportName(dto.getReportName());
        report.setReportDescription(dto.getReportDescription());
        if (dto.getSqlQuery() != null) {
            report.setSqlQuery(dto.getSqlQuery());
        }
        if (dto.getQueryParamsConfig() != null) {
            report.setQueryParamsConfig(dto.getQueryParamsConfig());
        }
        if (dto.getChartType() != null) {
            report.setChartType(dto.getChartType());
        }
        if (dto.getScheduleCron() != null) {
            report.setScheduleCron(dto.getScheduleCron());
        }
        if (dto.getIsPublic() != null) {
            report.setIsPublic(dto.getIsPublic());
        }
        report.setUpdateTime(new Date());

        boolean result = this.updateById(report);

        if (result) {
            logger.info("更新自定义报表成功，ID: {}", reportId);
        }

        return result;
    }

    /**
     * SQL安全校验
     * 防止SQL注入攻击
     */
    private void validateSqlSafety(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new RuntimeException("SQL不能为空");
        }

        String upperSql = sql.toUpperCase().trim();

        // 只允许SELECT语句
        if (!upperSql.startsWith("SELECT")) {
            throw new RuntimeException("只允许SELECT查询语句");
        }

        // 禁止危险关键字
        String[] dangerousKeywords = {
                "DROP", "DELETE", "INSERT", "UPDATE", "ALTER",
                "CREATE", "TRUNCATE", "EXEC", "EXECUTE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                throw new RuntimeException("SQL包含不安全的操作: " + keyword);
            }
        }

        // 禁止多语句执行
        if (sql.contains(";")) {
            throw new RuntimeException("禁止执行多条SQL语句");
        }

        // TODO: 更严格的SQL解析和权限控制
    }

    /**
     * 验证cron表达式格式
     */
    private boolean isValidCronExpression(String cronExpression) {
        // 基本验证：cron表达式应该是5位或6位
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");
        return parts.length >= 5 && parts.length <= 6;
    }

    /**
     * 将对象转换为JSON字符串
     */
    private String convertToJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            logger.warn("JSON转换失败", e);
            return null;
        }
    }

    /**
     * 将实体转换为VO
     */
    private CustomReportVO convertToVO(CustomReport report) {
        CustomReportVO vo = new CustomReportVO();
        vo.setReportId(report.getReportId());
        vo.setReportName(report.getReportName());
        vo.setReportDescription(report.getReportDescription());
        vo.setOwnerUserId(report.getOwnerUserId());
        vo.setSqlQueryDisplay(maskSqlQuery(report.getSqlQuery()));
        vo.setChartType(report.getChartType());
        vo.setChartTypeName(getChartTypeName(report.getChartType()));
        vo.setScheduleCron(report.getScheduleCron());
        vo.setLastExecutedAt(formatDateTime(report.getLastExecutedAt()));
        vo.setIsPublic(report.getIsPublic());
        vo.setCreateTime(formatDateTime(report.getCreateTime()));

        return vo;
    }

    /**
     * 批量转换
     */
    private List<CustomReportVO> convertToVOList(List<CustomReport> reports) {
        List<CustomReportVO> voList = new ArrayList<>();
        for (CustomReport report : reports) {
            voList.add(convertToVO(report));
        }
        return voList;
    }

    /**
     * 将执行记录转换为Map列表
     */
    private List<Map<String, Object>> convertExecutionToMapList(List<CustomReportExecution> executions) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CustomReportExecution execution : executions) {
            Map<String, Object> map = new HashMap<>();
            map.put("executionId", execution.getExecutionId());
            map.put("reportId", execution.getReportId());
            map.put("executedBy", execution.getExecutedBy());
            map.put("executionStatus", execution.getExecutionStatus());
            map.put("executionStatusName", getExecutionStatusName(execution.getExecutionStatus()));
            map.put("errorMessage", execution.getErrorMessage());
            map.put("executionTimeMs", execution.getExecutionTimeMs());
            map.put("executedAt", formatDateTime(execution.getExecutedAt()));
            list.add(map);
        }
        return list;
    }

    /**
     * 脱敏处理SQL（只显示前50个字符）
     */
    private String maskSqlQuery(String sql) {
        if (sql == null) {
            return null;
        }
        if (sql.length() > 50) {
            return sql.substring(0, 50) + "...";
        }
        return sql;
    }

    /**
     * 获取图表类型名称
     */
    private String getChartTypeName(Integer chartType) {
        switch (chartType) {
            case 1: return "表格";
            case 2: return "柱状图";
            case 3: return "折线图";
            case 4: return "饼图";
            case 5: return "散点图";
            default: return "未知";
        }
    }

    /**
     * 获取执行状态名称
     */
    private String getExecutionStatusName(Integer status) {
        switch (status) {
            case 1: return "运行中";
            case 2: return "成功";
            case 3: return "失败";
            default: return "未知";
        }
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
