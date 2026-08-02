package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.TrainingCertificateQueryDTO;
import com.foodtraceability.dto.TrainingCertificateVO;
import com.foodtraceability.dto.TrainingCourseCreateDTO;
import com.foodtraceability.dto.TrainingCourseQueryDTO;
import com.foodtraceability.dto.TrainingCourseUpdateDTO;
import com.foodtraceability.dto.TrainingCourseVO;
import com.foodtraceability.dto.TrainingStatisticsVO;
import com.foodtraceability.dto.TrainingStudyRecordQueryDTO;
import com.foodtraceability.dto.TrainingStudyRecordVO;
import com.foodtraceability.entity.TrainingCertificate;
import com.foodtraceability.entity.TrainingCourse;
import com.foodtraceability.entity.TrainingStudyRecord;
import com.foodtraceability.mapper.TrainingCertificateMapper;
import com.foodtraceability.mapper.TrainingCourseMapper;
import com.foodtraceability.mapper.TrainingStudyRecordMapper;
import com.foodtraceability.service.TrainingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 培训管理Service实现类
 * 实现培训课程、学习记录、证书的管理业务逻辑
 *
 * 课程发布状态机：
 * - draft → published（发布）
 * - published → archived（归档）
 * - archived → published（重新发布，允许）
 */
@Service
public class TrainingServiceImpl
        extends ServiceImpl<TrainingCourseMapper, TrainingCourse>
        implements TrainingService {

    private final TrainingCourseMapper courseMapper;
    private final TrainingStudyRecordMapper studyRecordMapper;
    private final TrainingCertificateMapper certificateMapper;

    private static final DateTimeFormatter BUSINESS_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public TrainingServiceImpl(TrainingCourseMapper courseMapper,
                               TrainingStudyRecordMapper studyRecordMapper,
                               TrainingCertificateMapper certificateMapper) {
        this.courseMapper = courseMapper;
        this.studyRecordMapper = studyRecordMapper;
        this.certificateMapper = certificateMapper;
    }

    // ============================================================
    // 课程管理
    // ============================================================

    @Override
    public IPage<TrainingCourseVO> getCoursePage(TrainingCourseQueryDTO queryDTO) {
        Page<TrainingCourse> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        IPage<TrainingCourse> coursePage = courseMapper.selectCoursePage(
                page,
                queryDTO.getCourseType(),
                queryDTO.getPublishStatus(),
                queryDTO.getKeyword());
        return coursePage.convert(this::convertToCourseVO);
    }

    @Override
    public TrainingCourseVO getCourseById(String id) {
        TrainingCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在：" + id);
        }
        return convertToCourseVO(course);
    }

    @Override
    public TrainingCourseVO createCourse(TrainingCourseCreateDTO createDTO) {
        TrainingCourse course = new TrainingCourse();
        course.setId(generateCourseId());
        course.setTitle(createDTO.getTitle());
        course.setDescription(createDTO.getDescription());
        course.setCourseType(createDTO.getCourseType());
        course.setRelatedArticleId(createDTO.getRelatedArticleId());
        // 关联文章标题由前端维护或后续通过关联查询补全，此处置空
        course.setRelatedArticleTitle(null);
        course.setInstructor(createDTO.getInstructor());
        course.setTotalLessons(createDTO.getTotalLessons());
        course.setDuration(createDTO.getDuration());
        course.setDeadline(createDTO.getDeadline());
        course.setCertificateEligible(createDTO.getCertificateEligible());
        course.setCertificateValidityDays(createDTO.getCertificateValidityDays());
        course.setPublishStatus(createDTO.getPublishStatus());
        // 新建课程初始统计为 0
        course.setAssignedCount(0);
        course.setCompletedCount(0);
        course.setAverageScore(0);
        course.setCreateTime(LocalDateTime.now());
        course.setUpdateTime(LocalDateTime.now());
        course.setDeleted(0);
        this.save(course);
        return convertToCourseVO(course);
    }

    @Override
    public TrainingCourseVO updateCourse(String id, TrainingCourseUpdateDTO updateDTO) {
        TrainingCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在：" + id);
        }
        // 仅更新非 null 字段
        if (updateDTO.getTitle() != null) {
            course.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null) {
            course.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getCourseType() != null) {
            course.setCourseType(updateDTO.getCourseType());
        }
        if (updateDTO.getRelatedArticleId() != null) {
            course.setRelatedArticleId(updateDTO.getRelatedArticleId());
        }
        if (updateDTO.getInstructor() != null) {
            course.setInstructor(updateDTO.getInstructor());
        }
        if (updateDTO.getTotalLessons() != null) {
            course.setTotalLessons(updateDTO.getTotalLessons());
        }
        if (updateDTO.getDuration() != null) {
            course.setDuration(updateDTO.getDuration());
        }
        if (updateDTO.getDeadline() != null) {
            course.setDeadline(updateDTO.getDeadline());
        }
        if (updateDTO.getCertificateEligible() != null) {
            course.setCertificateEligible(updateDTO.getCertificateEligible());
        }
        if (updateDTO.getCertificateValidityDays() != null) {
            course.setCertificateValidityDays(updateDTO.getCertificateValidityDays());
        }
        if (updateDTO.getPublishStatus() != null) {
            course.setPublishStatus(updateDTO.getPublishStatus());
        }
        course.setUpdateTime(LocalDateTime.now());
        this.updateById(course);
        return convertToCourseVO(course);
    }

    @Override
    public void deleteCourse(String id) {
        TrainingCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在：" + id);
        }
        this.removeById(id);
    }

    @Override
    public void publishCourse(String id) {
        TrainingCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在：" + id);
        }
        // 允许从 draft 或 archived 状态发布
        String currentStatus = course.getPublishStatus();
        if (!"draft".equals(currentStatus) && !"archived".equals(currentStatus)) {
            throw new RuntimeException("当前状态不允许发布：" + currentStatus);
        }
        course.setPublishStatus("published");
        course.setUpdateTime(LocalDateTime.now());
        this.updateById(course);
    }

    @Override
    public void archiveCourse(String id) {
        TrainingCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在：" + id);
        }
        // 仅 published 状态可归档
        if (!"published".equals(course.getPublishStatus())) {
            throw new RuntimeException("仅已发布课程可归档，当前状态：" + course.getPublishStatus());
        }
        course.setPublishStatus("archived");
        course.setUpdateTime(LocalDateTime.now());
        this.updateById(course);
    }

    // ============================================================
    // 学习记录管理
    // ============================================================

    @Override
    public IPage<TrainingStudyRecordVO> getStudyRecordPage(TrainingStudyRecordQueryDTO queryDTO) {
        Page<TrainingStudyRecord> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        IPage<TrainingStudyRecord> recordPage = studyRecordMapper.selectStudyRecordPage(
                page,
                queryDTO.getCourseId(),
                queryDTO.getEmployeeId(),
                queryDTO.getCompleted(),
                queryDTO.getKeyword(),
                queryDTO.getStartDate(),
                queryDTO.getEndDate());
        return recordPage.convert(this::convertToStudyRecordVO);
    }

    // ============================================================
    // 证书管理
    // ============================================================

    @Override
    public IPage<TrainingCertificateVO> getCertificatePage(TrainingCertificateQueryDTO queryDTO) {
        Page<TrainingCertificate> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        IPage<TrainingCertificate> certPage = certificateMapper.selectCertificatePage(
                page,
                queryDTO.getCourseId(),
                queryDTO.getEmployeeId(),
                queryDTO.getStatus(),
                queryDTO.getKeyword());
        return certPage.convert(this::convertToCertificateVO);
    }

    // ============================================================
    // 统计
    // ============================================================

    @Override
    public TrainingStatisticsVO getStatistics() {
        Map<String, Object> stats = courseMapper.selectCourseStatistics();
        TrainingStatisticsVO vo = new TrainingStatisticsVO();
        int totalCourses = toInt(stats.get("totalCourses"));
        int publishedCourses = toInt(stats.get("publishedCourses"));
        int totalAssigned = toInt(stats.get("totalAssigned"));
        int totalCompleted = toInt(stats.get("totalCompleted"));
        double averageScore = toDouble(stats.get("averageScore"));

        vo.setTotalCourses(totalCourses);
        vo.setPublishedCourses(publishedCourses);
        vo.setTotalAssigned(totalAssigned);
        vo.setTotalCompleted(totalCompleted);
        vo.setCompletionRate(totalAssigned > 0 ? (double) totalCompleted / totalAssigned : 0.0);
        // 保留一位小数
        vo.setAverageScore(Math.round(averageScore * 10.0) / 10.0);
        vo.setExpiringCertificates(certificateMapper.countExpiringCertificates());
        return vo;
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 生成课程业务编号：tr-{yyyyMMddHHmmss}{4位随机数}
     */
    private String generateCourseId() {
        return "tr-" + LocalDateTime.now().format(BUSINESS_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * Entity → VO 转换（课程）
     */
    private TrainingCourseVO convertToCourseVO(TrainingCourse course) {
        TrainingCourseVO vo = new TrainingCourseVO();
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setDescription(course.getDescription());
        vo.setCourseType(course.getCourseType());
        vo.setRelatedArticleId(course.getRelatedArticleId());
        vo.setRelatedArticleTitle(course.getRelatedArticleTitle());
        vo.setInstructor(course.getInstructor());
        vo.setTotalLessons(course.getTotalLessons());
        vo.setDuration(course.getDuration());
        vo.setDeadline(course.getDeadline());
        vo.setCertificateEligible(course.getCertificateEligible());
        vo.setCertificateValidityDays(course.getCertificateValidityDays());
        vo.setPublishStatus(course.getPublishStatus());
        vo.setAssignedCount(course.getAssignedCount());
        vo.setCompletedCount(course.getCompletedCount());
        vo.setAverageScore(course.getAverageScore());
        vo.setCreateTime(course.getCreateTime());
        vo.setUpdateTime(course.getUpdateTime());
        return vo;
    }

    /**
     * Entity → VO 转换（学习记录）
     */
    private TrainingStudyRecordVO convertToStudyRecordVO(TrainingStudyRecord record) {
        TrainingStudyRecordVO vo = new TrainingStudyRecordVO();
        vo.setId(record.getId());
        vo.setEmployeeId(record.getEmployeeId());
        vo.setEmployeeName(record.getEmployeeName());
        vo.setEmployeeCode(record.getEmployeeCode());
        vo.setDepartmentName(record.getDepartmentName());
        vo.setArticleId(record.getArticleId());
        vo.setArticleTitle(record.getArticleTitle());
        vo.setCourseId(record.getCourseId());
        vo.setCourseTitle(record.getCourseTitle());
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        vo.setDurationSeconds(record.getDurationSeconds());
        vo.setCompleted(record.getCompleted());
        vo.setScore(record.getScore());
        vo.setCertificateId(record.getCertificateId());
        vo.setCertificateExpiry(record.getCertificateExpiry());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    /**
     * Entity → VO 转换（证书）
     */
    private TrainingCertificateVO convertToCertificateVO(TrainingCertificate cert) {
        TrainingCertificateVO vo = new TrainingCertificateVO();
        vo.setId(cert.getId());
        vo.setEmployeeId(cert.getEmployeeId());
        vo.setEmployeeName(cert.getEmployeeName());
        vo.setCourseId(cert.getCourseId());
        vo.setCourseTitle(cert.getCourseTitle());
        vo.setIssueDate(cert.getIssueDate());
        vo.setExpiryDate(cert.getExpiryDate());
        vo.setStatus(cert.getStatus());
        vo.setCreateTime(cert.getCreateTime());
        vo.setUpdateTime(cert.getUpdateTime());
        return vo;
    }

    /**
     * 安全转换 Map 值为 int
     */
    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 安全转换 Map 值为 double
     */
    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
