package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RechargeFinanceLogQueryDTO;
import com.foodtraceability.dto.marketing.RechargeFinanceLogVO;
import com.foodtraceability.entity.marketing.RechargeFinanceLog;

/**
 * 储值财务流水服务接口
 * 记录每笔本金/赠送变动，对接财务中心
 *
 * 流水类型：recharge-充值 consume-消费 refund-退款 bonus_expire-赠送过期 bonus_grant-赠送发放
 */
public interface RechargeFinanceService {

    /**
     * 分页查询财务流水
     *
     * @param queryDTO 查询条件（含分页参数）
     * @return 分页结果
     */
    PageResult<RechargeFinanceLogVO> getFinanceLogPage(RechargeFinanceLogQueryDTO queryDTO);

    /**
     * 导出财务流水
     * 实际实现为占位
     *
     * @param queryDTO 查询条件
     */
    void exportFinanceLogs(RechargeFinanceLogQueryDTO queryDTO);

    /**
     * 记录储值财务流水（会员管理 → 财务中心跨模块流转入口）
     * <p>会员充值/消费/退款等业务发生时调用此方法写入财务流水记录，
     * 供财务中心查询和对账使用。</p>
     *
     * @param log 财务流水实体（含会员ID、变动金额、流水类型等）
     * @return 写入后的流水实体（含生成的logId）
     */
    RechargeFinanceLog recordFinanceLog(RechargeFinanceLog log);
}
