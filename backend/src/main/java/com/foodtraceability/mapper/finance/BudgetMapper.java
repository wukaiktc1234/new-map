package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.Budget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 预算Mapper接口
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {

    /**
     * 结构护栏查询（OICBE-B1-003 / KL-055）：
     * 统计 budgets 表中与 finance/Budget 实体映射列匹配的列数。
     * budgets 表为旧采购预算结构（V20260704_001），实体为财务预算结构，
     * 列漂移期间禁止分页 COUNT 短路返回假空列表——先校验结构再查询。
     *
     * @param columns 实体映射列名列表
     * @return 实际存在的列数（与 columns.size() 相等表示结构已对齐）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM information_schema.columns " +
            "WHERE table_schema = 'public' AND table_name = 'budgets' " +
            "AND column_name IN " +
            "<foreach collection='columns' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    int countEntityColumnsPresent(@Param("columns") List<String> columns);

    /**
     * 分页查询预算
     * @param page 分页参数
     * @param budgetYear 预算年份
     * @param budgetMonth 预算月份
     * @param budgetType 预算类型
     * @param categoryId 类别ID
     * @return 预算分页数据
     */
    IPage<Budget> selectBudgetPage(Page<Budget> page,
                                   @Param("budgetYear") Integer budgetYear,
                                   @Param("budgetMonth") Integer budgetMonth,
                                   @Param("budgetType") Integer budgetType,
                                   @Param("categoryId") Integer categoryId);

    /**
     * 根据年份和类型查询预算列表
     * @param budgetYear 年份
     * @param budgetType 类型
     * @return 预算列表
     */
    List<Budget> selectByYearAndType(@Param("budgetYear") Integer budgetYear,
                                     @Param("budgetType") Integer budgetType);

    /**
     * 预算执行情况汇总
     * @param budgetYear 年份
     * @param budgetType 类型
     * @return 汇总数据
     */
    Map<String, Object> selectBudgetSummary(@Param("budgetYear") Integer budgetYear,
                                            @Param("budgetType") Integer budgetType);
}
