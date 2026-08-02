package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceAuditLog;

/**
 * 财务审计日志Service接口
 * 用于记录财务模块的操作审计日志，仅允许INSERT，禁止UPDATE和DELETE
 * 不继承BaseEntity，不进行逻辑删除，仅保留创建时间字段
 */
public interface FinanceAuditLogService extends IService<FinanceAuditLog> {

    /**
     * 获取日志详情
     * @param logId 日志ID
     * @return 日志VO
     */
    FinanceAuditLogVO getDetail(Long logId);

    /**
     * 分页查询审计日志
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<FinanceAuditLogVO> getPage(FinanceAuditLogQueryDTO query);

    /**
     * 记录审计日志（直接传入实体）
     * @param entity 审计日志实体
     * @return 是否成功
     */
    boolean log(FinanceAuditLog entity);

    /**
     * 记录审计日志（按参数构建）
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param operationType 操作类型
     * @param module 模块
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @param operationDesc 操作描述
     * @param operationData 操作数据（JSON）
     * @param ipAddress IP地址
     * @return 是否成功
     */
    boolean log(Long operatorId, String operatorName, String operationType, String module,
                Long targetId, String targetType, String operationDesc, String operationData, String ipAddress);
}
