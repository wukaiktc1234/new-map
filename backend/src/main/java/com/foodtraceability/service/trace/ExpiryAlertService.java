package com.foodtraceability.service.trace;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.trace.ExpiryAlertQueryDTO;
import com.foodtraceability.dto.trace.ExpiryAlertVO;
import com.foodtraceability.dto.trace.ExpiryDashboardVO;
import com.foodtraceability.dto.trace.ExpiryStatisticsVO;
import com.foodtraceability.entity.ExpiryAlertRecord;

/**
 * 临期预警服务接口
 * 提供物料/食品的临期预警看板、列表查询、一键报损/退货等管理功能
 */
public interface ExpiryAlertService extends IService<ExpiryAlertRecord> {

    /**
     * 临期预警看板聚合数据
     * @return 看板数据
     */
    ExpiryDashboardVO getDashboard();

    /**
     * 临期列表（按剩余天数排序）
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<ExpiryAlertVO> queryExpiringSoon(ExpiryAlertQueryDTO queryDTO);

    /**
     * 已过期列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<ExpiryAlertVO> queryExpired(ExpiryAlertQueryDTO queryDTO);

    /**
     * 一键报损
     * @param traceCodeId 追溯码ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     * @return 是否报损成功
     */
    boolean scrap(Long traceCodeId, Long operatorId, String operatorName, String remark);

    /**
     * 一键退货
     * @param traceCodeId 追溯码ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     * @return 是否退货成功
     */
    boolean returnGoods(Long traceCodeId, Long operatorId, String operatorName, String remark);

    /**
     * 预警统计
     * @return 统计结果
     */
    ExpiryStatisticsVO getStatistics();

    /**
     * 刷新预警数据（定时任务调用）
     * @return 刷新的记录数
     */
    int refreshAlertData();
}
