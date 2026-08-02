package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.Budget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 预算Mapper接口
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {

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
