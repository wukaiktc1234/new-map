package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.AttendanceRecordCreateDTO;
import com.foodtraceability.dto.AttendanceRecordQueryDTO;
import com.foodtraceability.dto.AttendanceRecordUpdateDTO;
import com.foodtraceability.dto.AttendanceRecordVO;
import com.foodtraceability.entity.AttendanceRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理服务接口
 */
public interface AttendanceRecordService extends IService<AttendanceRecord> {

    /**
     * 分页查询考勤记录
     * @param queryDTO 查询条件
     * @return 分页结果（含VO数据）
     */
    Map<String, Object> getAttendancePage(AttendanceRecordQueryDTO queryDTO);

    /**
     * 获取考勤记录详情
     * @param recordId 记录ID
     * @return 考勤VO详情
     */
    AttendanceRecordVO getAttendanceDetail(Long recordId);

    /**
     * 创建考勤记录（打卡）
     * @param createDTO 创建信息
     * @return 创建的考勤记录
     */
    AttendanceRecord createAttendance(AttendanceRecordCreateDTO createDTO);

    /**
     * 上班打卡
     * @param employeeId 员工ID
     * @return 打卡结果
     */
    AttendanceRecord clockIn(String employeeId);

    /**
     * 下班打卡
     * @param employeeId 员工ID
     * @return 打卡结果（自动计算工作时长、迟到/早退等）
     */
    AttendanceRecord clockOut(String employeeId);

    /**
     * 更新考勤记录
     * @param recordId 记录ID
     * @param updateDTO 更新信息
     * @return 更新后的记录
     */
    AttendanceRecord updateAttendance(Long recordId, AttendanceRecordUpdateDTO updateDTO);

    /**
     * 请假申请
     * @param employeeId 员工ID
     * @param leaveType 请假类型
     * @param leaveHours 请假时长
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param remark 备注
     * @return 创建的请假记录列表
     */
    List<AttendanceRecord> applyLeave(String employeeId, Integer leaveType,
                                       BigDecimal leaveHours, LocalDate startDate,
                                       LocalDate endDate, String remark);

    /**
     * 加班登记
     * @param employeeId 员工ID
     * @param overtimeHours 加班时长
     * @param date 日期
     * @param remark 备注
     * @return 更新后的记录
     */
    AttendanceRecord registerOvertime(String employeeId, BigDecimal overtimeHours,
                                      LocalDate date, String remark);

    /**
     * 获取员工月度考勤统计
     * @param employeeId 员工ID
     * @param yearMonth 年月（格式：2026-04）
     * @return 月度统计数据
     */
    Map<String, Object> getMonthlySummary(String employeeId, String yearMonth);

    /**
     * 批量获取部门某日考勤记录
     * @param departmentId 部门ID
     * @param date 日期
     * @return 考勤VO列表
     */
    List<AttendanceRecordVO> getDepartmentDailyAttendance(String departmentId, LocalDate date);

    /**
     * 从排班计划同步考勤记录
     * 为方案下每个排班条目创建/更新考勤记录，写入排班关联信息
     * @param planId 排班计划ID
     * @return 同步的记录数
     */
    int syncFromSchedule(String planId);
}
