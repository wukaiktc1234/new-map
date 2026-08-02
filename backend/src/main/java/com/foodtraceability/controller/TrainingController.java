package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.TrainingCertificateQueryDTO;
import com.foodtraceability.dto.TrainingCertificateVO;
import com.foodtraceability.dto.TrainingCourseCreateDTO;
import com.foodtraceability.dto.TrainingCourseQueryDTO;
import com.foodtraceability.dto.TrainingCourseUpdateDTO;
import com.foodtraceability.dto.TrainingCourseVO;
import com.foodtraceability.dto.TrainingStatisticsVO;
import com.foodtraceability.dto.TrainingStudyRecordQueryDTO;
import com.foodtraceability.dto.TrainingStudyRecordVO;
import com.foodtraceability.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 培训管理控制器
 * 对应前端 API 路径 /v1/hr/training
 *
 * 端点说明：
 * 1. GET /v1/hr/training              - 分页查询培训课程
 * 2. GET /v1/hr/training/{id}         - 获取课程详情
 * 3. POST /v1/hr/training             - 创建培训课程
 * 4. PUT /v1/hr/training/{id}         - 更新培训课程
 * 5. DELETE /v1/hr/training/{id}      - 删除培训课程
 * 6. POST /v1/hr/training/{id}/publish - 发布课程
 * 7. POST /v1/hr/training/{id}/archive - 归档课程
 * 8. GET /v1/hr/training/study-records - 分页查询学习记录
 * 9. GET /v1/hr/training/certificates  - 分页查询证书
 * 10. GET /v1/hr/training/statistics   - 获取培训统计
 *
 * 路径匹配优先级：字面量路径（study-records/certificates/statistics）优先于 {id} 匹配
 */
@RestController
@RequestMapping("/v1/hr/training")
@Tag(name = "培训管理", description = "培训课程、学习记录和证书的管理")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /**
     * 分页查询培训课程
     */
    @GetMapping
    @Operation(summary = "分页查询培训课程")
    @PreAuthorize("hasAuthority('hr:training:view') or hasAuthority('*')")
    public Result<IPage<TrainingCourseVO>> getCoursePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "课程类型（required/elective）") @RequestParam(required = false) String courseType,
            @Parameter(description = "发布状态（draft/published/archived）") @RequestParam(required = false) String publishStatus,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword) {
        try {
            TrainingCourseQueryDTO queryDTO = new TrainingCourseQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setCourseType(courseType);
            queryDTO.setPublishStatus(publishStatus);
            queryDTO.setKeyword(keyword);
            IPage<TrainingCourseVO> result = trainingService.getCoursePage(queryDTO);
            return Result.success(result, "查询培训课程列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询培训课程列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取课程详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取课程详情")
    @PreAuthorize("hasAuthority('hr:training:view') or hasAuthority('*')")
    public Result<TrainingCourseVO> getCourseById(
            @Parameter(description = "课程ID") @PathVariable("id") String id) {
        try {
            TrainingCourseVO vo = trainingService.getCourseById(id);
            return Result.success(vo, "获取课程详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取课程详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建培训课程
     */
    @PostMapping
    @Operation(summary = "创建培训课程")
    @PreAuthorize("hasAuthority('hr:training:manage') or hasAuthority('*')")
    public Result<TrainingCourseVO> createCourse(@Valid @RequestBody TrainingCourseCreateDTO createDTO) {
        try {
            TrainingCourseVO vo = trainingService.createCourse(createDTO);
            return Result.success(vo, "创建培训课程成功");
        } catch (Exception e) {
            return Result.error(500, "创建培训课程失败：" + e.getMessage());
        }
    }

    /**
     * 更新培训课程
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新培训课程")
    @PreAuthorize("hasAuthority('hr:training:manage') or hasAuthority('*')")
    public Result<TrainingCourseVO> updateCourse(
            @Parameter(description = "课程ID") @PathVariable("id") String id,
            @Valid @RequestBody TrainingCourseUpdateDTO updateDTO) {
        try {
            TrainingCourseVO vo = trainingService.updateCourse(id, updateDTO);
            return Result.success(vo, "更新培训课程成功");
        } catch (Exception e) {
            return Result.error(500, "更新培训课程失败：" + e.getMessage());
        }
    }

    /**
     * 删除培训课程
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除培训课程")
    @PreAuthorize("hasAuthority('hr:training:manage') or hasAuthority('*')")
    public Result<Void> deleteCourse(
            @Parameter(description = "课程ID") @PathVariable("id") String id) {
        try {
            trainingService.deleteCourse(id);
            return Result.success(null, "删除培训课程成功");
        } catch (Exception e) {
            return Result.error(500, "删除培训课程失败：" + e.getMessage());
        }
    }

    /**
     * 发布课程
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布课程")
    @PreAuthorize("hasAuthority('hr:training:manage') or hasAuthority('*')")
    public Result<Void> publishCourse(
            @Parameter(description = "课程ID") @PathVariable("id") String id) {
        try {
            trainingService.publishCourse(id);
            return Result.success(null, "发布课程成功");
        } catch (Exception e) {
            return Result.error(500, "发布课程失败：" + e.getMessage());
        }
    }

    /**
     * 归档课程
     */
    @PostMapping("/{id}/archive")
    @Operation(summary = "归档课程")
    @PreAuthorize("hasAuthority('hr:training:manage') or hasAuthority('*')")
    public Result<Void> archiveCourse(
            @Parameter(description = "课程ID") @PathVariable("id") String id) {
        try {
            trainingService.archiveCourse(id);
            return Result.success(null, "归档课程成功");
        } catch (Exception e) {
            return Result.error(500, "归档课程失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询培训学习记录
     * 注意：字面量路径优先于 {id} 匹配，确保 /study-records 不会被误识别为课程ID
     */
    @GetMapping("/study-records")
    @Operation(summary = "分页查询培训学习记录")
    @PreAuthorize("hasAuthority('hr:training:view') or hasAuthority('*')")
    public Result<IPage<TrainingStudyRecordVO>> getStudyRecordPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "课程ID") @RequestParam(required = false) String courseId,
            @Parameter(description = "员工ID") @RequestParam(required = false) String employeeId,
            @Parameter(description = "是否完成") @RequestParam(required = false) Boolean completed,
            @Parameter(description = "开始日期（YYYY-MM-DD）") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（YYYY-MM-DD）") @RequestParam(required = false) String endDate,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword) {
        try {
            TrainingStudyRecordQueryDTO queryDTO = new TrainingStudyRecordQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setCourseId(courseId);
            queryDTO.setEmployeeId(employeeId);
            queryDTO.setCompleted(completed);
            queryDTO.setStartDate(startDate);
            queryDTO.setEndDate(endDate);
            queryDTO.setKeyword(keyword);
            IPage<TrainingStudyRecordVO> result = trainingService.getStudyRecordPage(queryDTO);
            return Result.success(result, "查询学习记录成功");
        } catch (Exception e) {
            return Result.error(500, "查询学习记录失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询证书
     */
    @GetMapping("/certificates")
    @Operation(summary = "分页查询证书")
    @PreAuthorize("hasAuthority('hr:training:view') or hasAuthority('*')")
    public Result<IPage<TrainingCertificateVO>> getCertificatePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "课程ID") @RequestParam(required = false) String courseId,
            @Parameter(description = "员工ID") @RequestParam(required = false) String employeeId,
            @Parameter(description = "证书状态（valid/expiring/expired）") @RequestParam(required = false) String status,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword) {
        try {
            TrainingCertificateQueryDTO queryDTO = new TrainingCertificateQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setCourseId(courseId);
            queryDTO.setEmployeeId(employeeId);
            queryDTO.setStatus(status);
            queryDTO.setKeyword(keyword);
            IPage<TrainingCertificateVO> result = trainingService.getCertificatePage(queryDTO);
            return Result.success(result, "查询证书列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询证书列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取培训统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取培训统计信息")
    @PreAuthorize("hasAuthority('hr:training:view') or hasAuthority('*')")
    public Result<TrainingStatisticsVO> getStatistics() {
        try {
            TrainingStatisticsVO vo = trainingService.getStatistics();
            return Result.success(vo, "获取培训统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取培训统计失败：" + e.getMessage());
        }
    }
}
