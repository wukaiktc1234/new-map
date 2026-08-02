package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.marketing.RechargeCreateDTO;
import com.foodtraceability.entity.RechargePlan;
import com.foodtraceability.entity.RechargeRecord;
import com.foodtraceability.entity.MarketingMember;

import java.util.List;
import java.util.Map;

/**
 * 充值服务接口
 */
public interface RechargeService extends IService<RechargeRecord> {

    /** 创建充值记录（待支付） */
    RechargeRecord createRecharge(RechargeCreateDTO createDTO);

    /** 支付完成回调（入账余额+赠送） */
    RechargeRecord paymentSuccess(String recordNo, String transactionNo);

    /** 退款处理 */
    RechargeRecord refund(Long recordId, Long refundAmount, String reason);

    /** 查询会员充值记录 */
    List<RechargeRecord> getMemberRecords(Long memberId, int current, int size);

    /** 获取启用的充值方案列表 */
    List<RechargePlan> getAvailablePlans();

    /** 获取充值统计 */
    Map<String, Object> getRechargeStatistics();
}
