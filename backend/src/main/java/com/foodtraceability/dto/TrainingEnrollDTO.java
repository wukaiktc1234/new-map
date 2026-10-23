package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 培训报名请求DTO
 * 批量将员工报名到指定培训课程，生成学习记录
 */
@Schema(description = "培训报名请求")
public class TrainingEnrollDTO {

    /** 课程ID */
    @Schema(description = "课程ID")
    private String courseId;

    /** 报名员工列表 */
    @Schema(description = "报名员工列表")
    private List<TrainingEnrollEmployeeDTO> employees;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public List<TrainingEnrollEmployeeDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<TrainingEnrollEmployeeDTO> employees) {
        this.employees = employees;
    }
}
