package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OnboardingArchive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 入职档案Mapper接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Mapper
public interface OnboardingArchiveMapper extends BaseMapper<OnboardingArchive> {

    /**
     * 根据状态查询档案列表
     *
     * @param status 状态
     * @return 档案列表
     */
    @Select("SELECT * FROM onboarding_archive WHERE status = #{status} AND deleted = 0 ORDER BY create_time DESC")
    List<OnboardingArchive> selectByStatus(@Param("status") String status);

    /**
     * 根据部门ID查询档案列表
     *
     * @param departmentId 部门ID
     * @return 档案列表
     */
    @Select("SELECT * FROM onboarding_archive WHERE department_id = #{departmentId} AND deleted = 0 ORDER BY create_time DESC")
    List<OnboardingArchive> selectByDepartmentId(@Param("departmentId") String departmentId);

    /**
     * 根据邀请码ID查询档案
     *
     * @param invitationCodeId 邀请码ID
     * @return 档案
     */
    @Select("SELECT * FROM onboarding_archive WHERE invitation_code_id = #{invitationCodeId} AND deleted = 0")
    OnboardingArchive selectByInvitationCodeId(@Param("invitationCodeId") Long invitationCodeId);

    /**
     * 查询待HR审查的档案列表
     *
     * @return 档案列表
     */
    @Select("SELECT * FROM onboarding_archive WHERE status = 'PENDING_HR' AND deleted = 0 ORDER BY create_time ASC")
    List<OnboardingArchive> selectPendingHrReview();

    /**
     * 查询待实质审查的档案列表
     *
     * @return 档案列表
     */
    @Select("SELECT * FROM onboarding_archive WHERE status = 'PENDING_SUBSTANTIVE' AND deleted = 0 ORDER BY create_time ASC")
    List<OnboardingArchive> selectPendingSubstantiveReview();

    /**
     * 查询指定年份前缀的最大员工编号序号
     * 使用原生SQL，不受逻辑删除影响，避免唯一索引冲突
     *
     * @param yearPrefix 年份前缀，如 EMP2026
     * @return 最大序号，如果没有返回null
     */
    @Select("SELECT MAX(CAST(SUBSTRING(employee_code, LENGTH(#{yearPrefix}) + 1) AS BIGINT)) FROM onboarding_archive WHERE employee_code LIKE CONCAT(#{yearPrefix}, '%')")
    Integer selectMaxEmployeeCodeSeq(@Param("yearPrefix") String yearPrefix);

    /**
     * 查询指定前缀的最大预设用户名序号
     * 使用原生SQL，不受逻辑删除影响，避免唯一索引冲突
     *
     * @param prefix 用户名前缀
     * @return 最大序号，如果没有返回null
     */
    @Select("SELECT MAX(CAST(REGEXP_REPLACE(preset_username, '[^0-9]', '') AS BIGINT)) FROM onboarding_archive WHERE preset_username LIKE CONCAT(#{prefix}, '%')")
    Integer selectMaxPresetUsernameSeq(@Param("prefix") String prefix);

    /**
     * 检查预设用户名是否已存在（不受逻辑删除影响）
     *
     * @param username 用户名
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(*) FROM onboarding_archive WHERE preset_username = #{username}")
    long countByUsername(@Param("username") String username);

    /**
     * 根据员工编号查询档案
     *
     * @param employeeCode 员工编号
     * @return 档案
     */
    @Select("SELECT * FROM onboarding_archive WHERE employee_code = #{employeeCode} AND deleted = 0")
    OnboardingArchive selectByEmployeeCode(@Param("employeeCode") String employeeCode);
}
