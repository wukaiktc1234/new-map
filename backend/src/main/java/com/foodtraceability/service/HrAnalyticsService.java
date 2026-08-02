package com.foodtraceability.service;

import java.util.Map;

/**
 * 人事分析服务接口
 *
 * 提供 HR 模块的人事数据分析能力，包括：
 * - 人事概览（员工总数/在职/离职/本月入职/本月离职）
 * - 部门人事统计（按部门分组聚合人数/性别/薪资/流动情况）
 * - 员工流动率分析
 * - 绩效分析
 * - 培训效果分析
 * - 招聘漏斗
 * - 考勤分析
 * - 薪资分布分析
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-07-03
 */
public interface HrAnalyticsService {

    /**
     * 获取人事概览数据
     * 包含：员工总数、在职人数、离职人数、本月入职人数、本月离职人数
     *
     * @return 概览数据
     */
    Map<String, Object> getOverview();

    /**
     * 获取部门人事统计（分页）
     * 按部门分组聚合：总人数、男女比例、人均薪资、本月新增、本月离职
     *
     * @param page 当前页码
     * @param pageSize 每页数量
     * @param department 部门名称筛选（可选）
     * @param period 时间周期筛选（可选，格式：yyyy-MM）
     * @return 分页结果，包含 records 和 total
     */
    Map<String, Object> getDepartmentStats(int page, int pageSize, String department, String period);

    /**
     * 获取员工流动率分析
     * 包含：入职率、离职率、净增长率，按月度趋势返回
     *
     * @return 流动率分析数据
     */
    Map<String, Object> getTurnoverRate();

    /**
     * 获取绩效分析数据
     * 包含：绩效分布、各部门绩效均值、绩效趋势
     *
     * @return 绩效分析数据
     */
    Map<String, Object> getPerformance();

    /**
     * 获取培训效果分析数据
     * 包含：培训完成率、培训场次、人均培训时长、培训满意度
     *
     * @return 培训效果分析数据
     */
    Map<String, Object> getTrainingEffect();

    /**
     * 获取招聘漏斗数据
     * 包含：各阶段人数（简历投递→初筛→面试→录用→入职）
     *
     * @return 招聘漏斗数据
     */
    Map<String, Object> getRecruitmentFunnel();

    /**
     * 获取考勤分析数据
     * 包含：出勤率、迟到次数、早退次数、缺勤次数、加班时长分布
     *
     * @return 考勤分析数据
     */
    Map<String, Object> getAttendance();

    /**
     * 获取薪资分布分析数据
     * 包含：薪资区间分布、各部门薪资均值、薪资分位数
     *
     * @return 薪资分布数据
     */
    Map<String, Object> getSalaryDistribution();
}
