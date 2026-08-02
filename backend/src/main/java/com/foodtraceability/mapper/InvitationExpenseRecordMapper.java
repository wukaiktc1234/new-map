package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InvitationExpenseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀请码费用记录Mapper接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Mapper
public interface InvitationExpenseRecordMapper extends BaseMapper<InvitationExpenseRecord> {

    /**
     * 根据批次ID查询费用记录
     *
     * @param batchId 批次ID
     * @return 费用记录列表
     */
    @Select("SELECT * FROM invitation_expense_record WHERE batch_id = #{batchId}")
    List<InvitationExpenseRecord> selectByBatchId(@Param("batchId") String batchId);

    /**
     * 根据部门ID查询费用记录
     *
     * @param departmentId 部门ID
     * @return 费用记录列表
     */
    @Select("SELECT * FROM invitation_expense_record WHERE department_id = #{departmentId} ORDER BY create_time DESC")
    List<InvitationExpenseRecord> selectByDepartmentId(@Param("departmentId") String departmentId);

    /**
     * 查询待同步的费用记录
     *
     * @return 费用记录列表
     */
    @Select("SELECT * FROM invitation_expense_record WHERE status = 'PENDING' ORDER BY create_time ASC")
    List<InvitationExpenseRecord> selectPendingSync();

    /**
     * 查询时间范围内的费用记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 费用记录列表
     */
    @Select("SELECT * FROM invitation_expense_record WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<InvitationExpenseRecord> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
