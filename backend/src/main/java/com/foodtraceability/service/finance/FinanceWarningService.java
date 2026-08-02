package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.FinanceWarningCreateDTO;
import com.foodtraceability.dto.finance.FinanceWarningProcessDTO;
import com.foodtraceability.dto.finance.FinanceWarningQueryDTO;
import com.foodtraceability.dto.finance.FinanceWarningStatsVO;
import com.foodtraceability.dto.finance.FinanceWarningVO;

/**
 * 财务预警Service接口
 *
 * <p>Sprint F-020：财务风险预警管理，支持预警创建、处理流转与统计分析。</p>
 */
public interface FinanceWarningService {

    /**
     * 分页查询预警列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<FinanceWarningVO> queryPage(FinanceWarningQueryDTO query);

    /**
     * 获取预警详情
     *
     * @param id 预警ID
     * @return 预警VO
     */
    FinanceWarningVO getDetail(Long id);

    /**
     * 创建预警记录，初始状态为 UNHANDLED
     *
     * @param dto 创建DTO
     * @return 创建后的预警VO
     */
    FinanceWarningVO create(FinanceWarningCreateDTO dto);

    /**
     * 处理预警，更新状态为 HANDLING 或 RESOLVED
     *
     * @param id  预警ID
     * @param dto 处理DTO
     * @return 操作结果
     */
    boolean process(Long id, FinanceWarningProcessDTO dto);

    /**
     * 逻辑删除预警
     *
     * @param id 预警ID
     * @return 操作结果
     */
    boolean delete(Long id);

    /**
     * 统计预警数量（总数/待处理/处理中/已解决）
     *
     * @param startDate 起始日期（yyyy-MM-dd），可为空
     * @param endDate   结束日期（yyyy-MM-dd），可为空
     * @return 统计VO
     */
    FinanceWarningStatsVO getStats(String startDate, String endDate);
}
