package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批记录Mapper接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    /**
     * 根据档案ID查询审批记录列表
     *
     * @param archiveId 档案ID
     * @return 审批记录列表
     */
    @Select("SELECT * FROM approval_record WHERE archive_id = #{archiveId} ORDER BY step_number ASC")
    List<ApprovalRecord> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据审批人ID查询待审批记录
     *
     * @param reviewerId 审批人ID
     * @return 审批记录列表
     */
    @Select("SELECT * FROM approval_record WHERE reviewer_id = #{reviewerId} AND status = 'PENDING' ORDER BY create_time ASC")
    List<ApprovalRecord> selectPendingByReviewerId(@Param("reviewerId") Long reviewerId);

    /**
     * 查询档案的最新审批记录
     *
     * @param archiveId 档案ID
     * @return 最新审批记录
     */
    @Select("SELECT * FROM approval_record WHERE archive_id = #{archiveId} ORDER BY step_number DESC LIMIT 1")
    ApprovalRecord selectLatestByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 查询所有待审批记录
     *
     * @return 待审批记录列表
     */
    @Select("SELECT * FROM approval_record WHERE status = 'PENDING' ORDER BY create_time ASC")
    List<ApprovalRecord> selectAllPending();

    /**
     * 查询所有已完成的审批记录（已通过或已拒绝）
     *
     * @return 已完成的审批记录列表
     */
    @Select("SELECT * FROM approval_record WHERE status IN ('APPROVED', 'REJECTED') ORDER BY action_time DESC")
    List<ApprovalRecord> selectCompletedApprovals();
}
