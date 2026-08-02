package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.EmployeeArchiveDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工档案详细Mapper
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Mapper
public interface EmployeeArchiveDetailMapper extends BaseMapper<EmployeeArchiveDetail> {

    @Select("SELECT * FROM employee_archive_detail WHERE archive_id = #{archiveId} AND deleted = 0")
    EmployeeArchiveDetail selectByArchiveId(@Param("archiveId") Long archiveId);

    @Select("SELECT * FROM employee_archive_detail WHERE employee_id = #{employeeId} AND deleted = 0")
    EmployeeArchiveDetail selectByEmployeeId(@Param("employeeId") String employeeId);

    @Select("SELECT COUNT(*) FROM employee_archive_detail WHERE review_status = #{reviewStatus} AND deleted = 0")
    int countByReviewStatus(@Param("reviewStatus") String reviewStatus);

    @Select("SELECT AVG(completeness_score) FROM employee_archive_detail WHERE deleted = 0")
    Double avgCompletenessScore();
}
