package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ComplianceReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合规报告Mapper接口
 */
@Mapper
public interface ComplianceReportMapper extends BaseMapper<ComplianceReport> {

    /**
     * 根据报告类型查询
     * @param reportType 报告类型
     * @return 报告列表
     */
    List<ComplianceReport> selectByReportType(@Param("reportType") Integer reportType);

    /**
     * 根据门店ID查询
     * @param storeId 门店ID
     * @return 报告列表
     */
    List<ComplianceReport> selectByStoreId(@Param("storeId") Long storeId);

    /**
     * 根据状态查询
     * @param status 状态
     * @return 报告列表
     */
    List<ComplianceReport> selectByStatus(@Param("status") Integer status);
}
