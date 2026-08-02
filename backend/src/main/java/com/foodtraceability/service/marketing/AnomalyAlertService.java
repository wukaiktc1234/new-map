package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.AnomalyAlertQueryDTO;
import com.foodtraceability.dto.marketing.AnomalyAlertVO;

/**
 * 异常交易告警服务接口
 * 风控触发异常时生成告警记录
 *
 * 状态：pending-待处理 handled-已处理 ignored-已忽略
 * 告警类型：frequent_recharge/fast_consume/high_refund_rate/new_member_high_recharge/multi_device/over_limit
 */
public interface AnomalyAlertService {

    /**
     * 分页查询异常告警
     *
     * @param queryDTO 查询条件（含分页参数）
     * @return 分页结果
     */
    PageResult<AnomalyAlertVO> getAlertPage(AnomalyAlertQueryDTO queryDTO);

    /**
     * 处理告警
     * 业务流程：状态置为 handled，记录处理人和处理备注
     *
     * @param alertId 告警ID
     * @param remark 处理备注
     */
    void handleAlert(String alertId, String remark);

    /**
     * 忽略告警
     * 业务流程：状态置为 ignored，记录处理人
     *
     * @param alertId 告警ID
     */
    void ignoreAlert(String alertId);
}
