package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ComplianceCheckItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合规检查项明细Mapper接口
 */
@Mapper
public interface ComplianceCheckItemMapper extends BaseMapper<ComplianceCheckItem> {

    /**
     * 根据报告ID查询所有检查项
     * @param reportId 报告ID
     * @return 检查项列表
     */
    List<ComplianceCheckItem> selectByReportId(@Param("reportId") Long reportId);

    /**
     * 根据报告ID和检查分类查询
     * @param reportId 报告ID
     * @param checkCategory 检查分类
     * @return 检查项列表
     */
    List<ComplianceCheckItem> selectByReportIdAndCategory(
            @Param("reportId") Long reportId,
            @Param("checkCategory") Integer checkCategory);

    /**
     * 查询需要整改的检查项（跨报告）
     * @return 检查项列表
     */
    List<ComplianceCheckItem> selectPendingRectification();

    /**
     * 统计报告中各状态的检查项数量
     * @param reportId 报告ID
     * @return 统计结果Map
     */
    List<ComplianceCheckItem> selectStatusCountByReportId(@Param("reportId") Long reportId);
}
