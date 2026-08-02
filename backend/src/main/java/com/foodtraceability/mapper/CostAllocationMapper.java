package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CostAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 成本分配Mapper接口
 */
@Mapper
public interface CostAllocationMapper extends BaseMapper<CostAllocation> {

    /**
     * 根据期间查询成本分配
     * @param period 期间
     * @return 成本分配列表
     */
    List<CostAllocation> selectByPeriod(@Param("period") String period);

    /**
     * 根据日期范围查询成本分配
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本分配列表
     */
    List<CostAllocation> selectByDateRange(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    /**
     * 根据状态查询成本分配
     * @param status 状态
     * @return 成本分配列表
     */
    List<CostAllocation> selectByStatus(@Param("status") String status);

    /**
     * 根据分配部门查询成本分配
     * @param departmentId 部门ID
     * @return 成本分配列表
     */
    List<CostAllocation> selectByDepartment(@Param("departmentId") Long departmentId);
}