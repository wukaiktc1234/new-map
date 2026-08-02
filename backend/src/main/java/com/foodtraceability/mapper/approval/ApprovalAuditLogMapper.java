package com.foodtraceability.mapper.approval;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.approval.ApprovalAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录 Mapper 接口
 */
@Mapper
public interface ApprovalAuditLogMapper extends BaseMapper<ApprovalAuditLog> {
}
