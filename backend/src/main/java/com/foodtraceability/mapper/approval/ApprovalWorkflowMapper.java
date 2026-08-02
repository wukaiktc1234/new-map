package com.foodtraceability.mapper.approval;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.approval.ApprovalWorkflow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批流程定义 Mapper 接口
 */
@Mapper
public interface ApprovalWorkflowMapper extends BaseMapper<ApprovalWorkflow> {
}
