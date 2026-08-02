package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.CostRecord;

import java.time.LocalDate;

/**
 * 成本记录Service接口
 * 记录企业各项成本支出，用于成本核算和分析
 */
public interface CostRecordService extends IService<CostRecord> {

    /**
     * 创建成本记录
     */
    CostRecordVO create(CostRecordCreateDTO dto);

    /**
     * 更新成本记录
     */
    boolean update(CostRecordUpdateDTO dto);

    /**
     * 获取成本记录详情
     */
    CostRecordVO getDetail(Long costId);

    /**
     * 分页查询成本记录
     */
    IPage<CostRecordVO> getPage(CostRecordQueryDTO query);

    /**
     * 按期间汇总成本
     */
    java.util.Map<String, Object> summarizeByPeriod(String period);

    /**
     * 记录报销成本（F-011 联动：发票报销审批通过后按部门归集成本）
     *
     * <p>Sprint 3.1 P0 T-041：报销审批通过后由
     * {@code InvoiceReimbursementApprovedEventListener} 调用此方法，
     * 将报销金额按部门归集到成本记录表（costType=7 其他成本）。</p>
     *
     * @param reimbursementId   报销单ID
     * @param departmentId      部门ID（作为成本中心）
     * @param reimbursementType 报销类型（差旅费/招待费/办公费等）
     * @param amount            报销金额（单位：分）
     * @param occurDate         发生日期（用于归属期间 yyyy-MM）
     */
    void recordReimbursementCost(Long reimbursementId,
                                 Long departmentId,
                                 String reimbursementType,
                                 Long amount,
                                 LocalDate occurDate);
}
