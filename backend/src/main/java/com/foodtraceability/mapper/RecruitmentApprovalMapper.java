package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.ApprovalQueryDTO;
import com.foodtraceability.entity.RecruitmentApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 招聘审批记录Mapper接口
 * 对应数据库表 recruitment_approvals
 * 用于记录招聘流程中的审批环节
 */
@Mapper
public interface RecruitmentApprovalMapper extends BaseMapper<RecruitmentApproval> {

    /**
     * 分页查询招聘审批记录
     *
     * @param page  分页参数
     * @param query 查询条件（支持岗位ID、审批状态、审批人等条件筛选）
     * @return 分页结果
     */
    IPage<RecruitmentApproval> selectApprovalPage(IPage<RecruitmentApproval> page, @Param("query") ApprovalQueryDTO query);

    /**
     * 查询指定审批人的待审批记录
     * 待审批定义：审批状态为pending且当前审批人为指定用户
     *
     * @param approverId 审批人ID
     * @return 待审批记录列表（按创建时间升序）
     */
    List<RecruitmentApproval> selectPendingApprovals(String approverId);

    /**
     * 根据招聘岗位ID查询所有关联的审批记录
     * 用于查看某个岗位的所有应聘者审批情况
     *
     * @param postId 招聘岗位ID
     * @return 审批记录列表（按创建时间降序）
     */
    List<RecruitmentApproval> selectByPostId(String postId);
}
