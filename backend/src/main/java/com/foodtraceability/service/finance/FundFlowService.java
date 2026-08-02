package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FundFlow;

import java.time.LocalDate;
import java.util.Map;

/**
 * 资金流水Service接口
 * 记录银行账户的资金收支流水，支持资金监控与对账
 */
public interface FundFlowService extends IService<FundFlow> {

    /**
     * 创建资金流水
     * @param dto 创建DTO
     * @return 流水VO
     */
    FundFlowVO create(FundFlowCreateDTO dto);

    /**
     * 获取流水详情
     * @param flowId 流水ID
     * @return 流水VO
     */
    FundFlowVO getDetail(Long flowId);

    /**
     * 分页查询资金流水
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<FundFlowVO> getPage(FundFlowQueryDTO query);

    /**
     * 统计指定账户在指定期间的收支情况
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计结果（包含总收入、总支出、净额、笔数）
     */
    Map<String, Object> getStatistics(Long accountId, LocalDate startDate, LocalDate endDate);
}
