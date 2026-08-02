package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CostAllocationDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成本分配明细Mapper接口
 */
@Mapper
public interface CostAllocationDetailMapper extends BaseMapper<CostAllocationDetail> {

    /**
     * 根据成本分配ID查询明细
     * @param allocationId 成本分配ID
     * @return 成本分配明细列表
     */
    List<CostAllocationDetail> selectByAllocationId(@Param("allocationId") Long allocationId);

    /**
     * 根据成本对象ID查询明细
     * @param costObjectId 成本对象ID
     * @return 成本分配明细列表
     */
    List<CostAllocationDetail> selectByCostObjectId(@Param("costObjectId") Long costObjectId);

    /**
     * 根据科目ID查询明细
     * @param subjectId 科目ID
     * @return 成本分配明细列表
     */
    List<CostAllocationDetail> selectBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * 批量插入成本分配明细
     * @param details 成本分配明细列表
     * @return 影响行数
     */
    int batchInsert(@Param("details") List<CostAllocationDetail> details);

    /**
     * 根据成本分配ID删除明细
     * @param allocationId 成本分配ID
     * @return 影响行数
     */
    int deleteByAllocationId(@Param("allocationId") Long allocationId);
}