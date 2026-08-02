package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.AttendanceRecord;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.service.AttendanceRecordService;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.HrAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人事分析服务实现
 *
 * 聚合 EmployeeService、DepartmentService、AttendanceRecordService 等数据，
 * 返回 HR 模块所需的人事分析结果。
 *
 * 数据状态约定：
 * - 员工状态字段 status：1=在职(active)，0=离职(inactive)，2=试用(probation)
 * - 性别字段 gender：male/female/other
 * - 薪资字段 base_salary：以元为单位的 BigDecimal
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-07-03
 */
@Service
public class HrAnalyticsServiceImpl implements HrAnalyticsService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HrAnalyticsServiceImpl.class);

    /** 员工状态：在职 */
    private static final String STATUS_ACTIVE = "1";
    /** 员工状态：离职 */
    private static final String STATUS_INACTIVE = "0";
    /** 员工状态：试用 */
    private static final String STATUS_PROBATION = "2";

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final AttendanceRecordService attendanceRecordService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     *
     * @param employeeService 员工服务
     * @param departmentService 部门服务
     * @param attendanceRecordService 考勤服务
     */
    public HrAnalyticsServiceImpl(EmployeeService employeeService,
                                  DepartmentService departmentService,
                                  AttendanceRecordService attendanceRecordService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.attendanceRecordService = attendanceRecordService;
    }

    @Override
    public Map<String, Object> getOverview() {
        log.info("获取人事概览数据");
        Map<String, Object> overview = new HashMap<>();
        try {
            long total = employeeService.count();
            long activeCount = employeeService.count(new QueryWrapper<Employee>().eq("status", STATUS_ACTIVE));
            long inactiveCount = employeeService.count(new QueryWrapper<Employee>().eq("status", STATUS_INACTIVE));
            long probationCount = employeeService.count(new QueryWrapper<Employee>().eq("status", STATUS_PROBATION));

            // 本月入职/离职统计
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = LocalDateTime.now();
            long hiredThisMonth = employeeService.count(new QueryWrapper<Employee>()
                    .ge("hire_date", monthStart)
                    .le("hire_date", monthEnd));
            long leftThisMonth = employeeService.count(new QueryWrapper<Employee>()
                    .eq("status", STATUS_INACTIVE)
                    .ge("resign_date", monthStart)
                    .le("resign_date", monthEnd));

            overview.put("total", total);
            overview.put("active", activeCount);
            overview.put("inactive", inactiveCount);
            overview.put("probation", probationCount);
            overview.put("hiredThisMonth", hiredThisMonth);
            overview.put("leftThisMonth", leftThisMonth);
        } catch (Exception e) {
            log.error("获取人事概览数据失败", e);
        }
        return overview;
    }

    @Override
    public Map<String, Object> getDepartmentStats(int page, int pageSize, String department, String period) {
        log.info("获取部门人事统计，页码：{}，每页：{}，部门：{}，周期：{}", page, pageSize, department, period);
        Map<String, Object> result = new HashMap<>();
        try {
            List<Employee> allEmployees = employeeService.list();
            // 部门ID -> 部门名称 映射
            Map<String, String> departmentNameMap = buildDepartmentNameMap();

            // 按部门ID分组聚合
            Map<String, List<Employee>> groupedByDept = allEmployees.stream()
                    .filter(e -> e.getDepartmentId() != null)
                    .collect(Collectors.groupingBy(e -> String.valueOf(e.getDepartmentId())));

            // 解析周期：用于本月新增/离职计算，未指定时默认当月
            YearMonth targetMonth = parsePeriod(period);

            List<Map<String, Object>> records = new ArrayList<>();
            for (Map.Entry<String, List<Employee>> entry : groupedByDept.entrySet()) {
                String deptId = entry.getKey();
                String deptName = departmentNameMap.getOrDefault(deptId, "未分配部门");
                // 部门名称筛选
                if (department != null && !department.isEmpty() && !department.equals(deptName)) {
                    continue;
                }
                records.add(buildDepartmentStat(deptName, entry.getValue(), targetMonth));
            }

            // 按总人数倒序排序
            records.sort(Comparator.<Map<String, Object>>comparingInt(r -> ((Number) r.get("total")).intValue()).reversed());

            // 手动分页
            int total = records.size();
            int fromIndex = Math.max(0, (page - 1) * pageSize);
            int toIndex = Math.min(total, fromIndex + pageSize);
            List<Map<String, Object>> pageRecords = fromIndex <= toIndex
                    ? records.subList(fromIndex, toIndex)
                    : new ArrayList<>();

            result.put("records", pageRecords);
            result.put("total", total);
            result.put("current", page);
            result.put("size", pageSize);
            result.put("pages", (int) Math.ceil((double) total / pageSize));
        } catch (Exception e) {
            log.error("获取部门人事统计失败", e);
            result.put("records", new ArrayList<>());
            result.put("total", 0);
        }
        return result;
    }

    @Override
    public Map<String, Object> getTurnoverRate() {
        log.info("获取员工流动率分析");
        Map<String, Object> result = new HashMap<>();
        try {
            long totalEmployees = employeeService.count();
            long activeCount = employeeService.count(new QueryWrapper<Employee>().eq("status", STATUS_ACTIVE));

            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = LocalDateTime.now();
            long hiredThisMonth = employeeService.count(new QueryWrapper<Employee>()
                    .ge("hire_date", monthStart)
                    .le("hire_date", monthEnd));
            long leftThisMonth = employeeService.count(new QueryWrapper<Employee>()
                    .eq("status", STATUS_INACTIVE)
                    .ge("resign_date", monthStart)
                    .le("resign_date", monthEnd));

            // 计算比率（保留2位小数）
            double hireRate = activeCount > 0 ? (double) hiredThisMonth / activeCount * 100 : 0;
            double turnoverRate = activeCount > 0 ? (double) leftThisMonth / activeCount * 100 : 0;
            double netGrowthRate = activeCount > 0 ? (double) (hiredThisMonth - leftThisMonth) / activeCount * 100 : 0;

            result.put("totalEmployees", totalEmployees);
            result.put("activeEmployees", activeCount);
            result.put("hiredThisMonth", hiredThisMonth);
            result.put("leftThisMonth", leftThisMonth);
            result.put("hireRate", round2(hireRate));
            result.put("turnoverRate", round2(turnoverRate));
            result.put("netGrowthRate", round2(netGrowthRate));

            // 月度趋势（最近6个月，基于现有数据生成基础结构）
            result.put("monthlyTrend", buildMonthlyTrend());
        } catch (Exception e) {
            log.error("获取员工流动率分析失败", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getPerformance() {
        log.info("获取绩效分析数据");
        // 绩效模块尚未完整实现，先返回基础结构数据，后续接入绩效系统后完善
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("excellent", 0);
        distribution.put("good", 0);
        distribution.put("average", 0);
        distribution.put("needImprove", 0);
        result.put("distribution", distribution);
        result.put("departmentAvg", new ArrayList<>());
        result.put("trend", new ArrayList<>());
        result.put("message", "绩效模块数据待接入");
        return result;
    }

    @Override
    public Map<String, Object> getTrainingEffect() {
        log.info("获取培训效果分析数据");
        // 培训效果分析返回基础结构，后续接入培训系统后完善
        Map<String, Object> result = new HashMap<>();
        result.put("totalTraining", 0);
        result.put("completedRate", 0);
        result.put("avgHours", 0);
        result.put("satisfaction", 0);
        result.put("departmentStats", new ArrayList<>());
        result.put("message", "培训效果数据待接入");
        return result;
    }

    @Override
    public Map<String, Object> getRecruitmentFunnel() {
        log.info("获取招聘漏斗数据");
        // 招聘漏斗返回基础结构，后续接入招聘系统后完善
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> stages = new ArrayList<>();
        stages.add(buildFunnelStage("resumeSubmitted", "简历投递", 0));
        stages.add(buildFunnelStage("screened", "初筛通过", 0));
        stages.add(buildFunnelStage("interviewed", "面试完成", 0));
        stages.add(buildFunnelStage("offered", "录用", 0));
        stages.add(buildFunnelStage("onboarded", "入职", 0));
        result.put("stages", stages);
        result.put("message", "招聘漏斗数据待接入");
        return result;
    }

    @Override
    public Map<String, Object> getAttendance() {
        log.info("获取考勤分析数据");
        Map<String, Object> result = new HashMap<>();
        try {
            long totalRecords = attendanceRecordService.count();
            // 考勤状态：1=正常, 2=迟到, 3=早退, 4=缺勤, 5=加班, 6=请假
            long normalCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 1));
            long lateCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 2));
            long earlyLeaveCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 3));
            long absentCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 4));
            long overtimeCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 5));
            long onLeaveCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 6));

            double attendanceRate = totalRecords > 0 ? (double) normalCount / totalRecords * 100 : 0;

            Map<String, Object> statusDistribution = new LinkedHashMap<>();
            statusDistribution.put("normal", normalCount);
            statusDistribution.put("late", lateCount);
            statusDistribution.put("earlyLeave", earlyLeaveCount);
            statusDistribution.put("absent", absentCount);
            statusDistribution.put("overtime", overtimeCount);
            statusDistribution.put("onLeave", onLeaveCount);

            result.put("totalRecords", totalRecords);
            result.put("attendanceRate", round2(attendanceRate));
            result.put("statusDistribution", statusDistribution);
            result.put("abnormalCount", lateCount + earlyLeaveCount + absentCount);
        } catch (Exception e) {
            log.error("获取考勤分析数据失败", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getSalaryDistribution() {
        log.info("获取薪资分布分析数据");
        Map<String, Object> result = new HashMap<>();
        try {
            List<Employee> employees = employeeService.list(new QueryWrapper<Employee>()
                    .eq("status", STATUS_ACTIVE)
                    .isNotNull("base_salary"));

            // 薪资区间分布
            Map<String, Integer> rangeDistribution = new LinkedHashMap<>();
            rangeDistribution.put("below5000", 0);
            rangeDistribution.put("5000to8000", 0);
            rangeDistribution.put("8000to12000", 0);
            rangeDistribution.put("12000to20000", 0);
            rangeDistribution.put("above20000", 0);

            BigDecimal totalSalary = BigDecimal.ZERO;
            int validCount = 0;
            for (Employee emp : employees) {
                BigDecimal salary = emp.getBaseSalary();
                if (salary == null) {
                    continue;
                }
                totalSalary = totalSalary.add(salary);
                validCount++;
                double s = salary.doubleValue();
                if (s < 5000) {
                    rangeDistribution.merge("below5000", 1, Integer::sum);
                } else if (s < 8000) {
                    rangeDistribution.merge("5000to8000", 1, Integer::sum);
                } else if (s < 12000) {
                    rangeDistribution.merge("8000to12000", 1, Integer::sum);
                } else if (s < 20000) {
                    rangeDistribution.merge("12000to20000", 1, Integer::sum);
                } else {
                    rangeDistribution.merge("above20000", 1, Integer::sum);
                }
            }

            BigDecimal avgSalary = validCount > 0
                    ? totalSalary.divide(BigDecimal.valueOf(validCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.put("rangeDistribution", rangeDistribution);
            result.put("avgSalary", avgSalary);
            result.put("employeeCount", validCount);
            result.put("departmentAvg", buildDepartmentSalaryAvg(employees));
        } catch (Exception e) {
            log.error("获取薪资分布分析数据失败", e);
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建部门ID到部门名称的映射
     */
    private Map<String, String> buildDepartmentNameMap() {
        Map<String, String> map = new HashMap<>();
        try {
            List<Department> departments = departmentService.getAllDepartments();
            if (departments != null) {
                for (Department dept : departments) {
                    if (dept.getDepartmentId() != null) {
                        map.put(String.valueOf(dept.getDepartmentId()), dept.getDepartmentName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("构建部门名称映射失败：{}", e.getMessage());
        }
        return map;
    }

    /**
     * 构建单个部门的人事统计数据
     */
    private Map<String, Object> buildDepartmentStat(String deptName, List<Employee> employees, YearMonth targetMonth) {
        Map<String, Object> stat = new HashMap<>();
        int total = employees.size();
        int maleCount = (int) employees.stream().filter(e -> "male".equalsIgnoreCase(e.getGender())).count();
        int femaleCount = (int) employees.stream().filter(e -> "female".equalsIgnoreCase(e.getGender())).count();

        // 人均薪资
        BigDecimal totalSalary = BigDecimal.ZERO;
        int salaryCount = 0;
        for (Employee emp : employees) {
            if (emp.getBaseSalary() != null) {
                totalSalary = totalSalary.add(emp.getBaseSalary());
                salaryCount++;
            }
        }
        String avgSalaryStr = salaryCount > 0
                ? "¥" + totalSalary.divide(BigDecimal.valueOf(salaryCount), 2, RoundingMode.HALF_UP).toPlainString()
                : "¥0";

        // 本月新增/离职
        int newThisMonth = (int) employees.stream()
                .filter(e -> isSameMonth(e.getHireDate(), targetMonth))
                .count();
        int turnover = (int) employees.stream()
                .filter(e -> STATUS_INACTIVE.equals(e.getStatus()))
                .filter(e -> isSameMonth(e.getResignDate(), targetMonth))
                .count();

        stat.put("department", deptName);
        stat.put("total", total);
        stat.put("mcount", maleCount);
        stat.put("fcount", femaleCount);
        stat.put("avgSalary", avgSalaryStr);
        stat.put("newThisMonth", newThisMonth);
        stat.put("turnover", turnover);
        return stat;
    }

    /**
     * 构建部门薪资均值列表
     */
    private List<Map<String, Object>> buildDepartmentSalaryAvg(List<Employee> employees) {
        Map<String, String> departmentNameMap = buildDepartmentNameMap();
        Map<String, List<Employee>> grouped = employees.stream()
                .filter(e -> e.getDepartmentId() != null)
                .collect(Collectors.groupingBy(e -> String.valueOf(e.getDepartmentId())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Employee>> entry : grouped.entrySet()) {
            BigDecimal totalSalary = BigDecimal.ZERO;
            int count = 0;
            for (Employee emp : entry.getValue()) {
                if (emp.getBaseSalary() != null) {
                    totalSalary = totalSalary.add(emp.getBaseSalary());
                    count++;
                }
            }
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("department", departmentNameMap.getOrDefault(entry.getKey(), "未分配部门"));
                item.put("avgSalary", totalSalary.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
                item.put("employeeCount", count);
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 构建招聘漏斗单阶段
     */
    private Map<String, Object> buildFunnelStage(String key, String label, int count) {
        Map<String, Object> stage = new HashMap<>();
        stage.put("key", key);
        stage.put("label", label);
        stage.put("count", count);
        return stage;
    }

    /**
     * 构建月度趋势（最近6个月基础结构）
     */
    private List<Map<String, Object>> buildMonthlyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            YearMonth month = current.minusMonths(i);
            item.put("month", month.toString());
            item.put("hired", 0);
            item.put("left", 0);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 解析时间周期字符串
     *
     * @param period 周期字符串，格式 yyyy-MM，为空返回当月
     */
    private YearMonth parsePeriod(String period) {
        if (period == null || period.isEmpty()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(period);
        } catch (Exception e) {
            log.warn("解析周期失败：{}，使用当月", period);
            return YearMonth.now();
        }
    }

    /**
     * 判断 LocalDateTime 是否属于指定月份
     */
    private boolean isSameMonth(LocalDateTime dateTime, YearMonth targetMonth) {
        if (dateTime == null) {
            return false;
        }
        return YearMonth.from(dateTime).equals(targetMonth);
    }

    /**
     * 保留2位小数
     */
    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
