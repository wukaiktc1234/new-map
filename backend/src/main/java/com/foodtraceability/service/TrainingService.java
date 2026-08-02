package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.TrainingCertificateQueryDTO;
import com.foodtraceability.dto.TrainingCertificateVO;
import com.foodtraceability.dto.TrainingCourseCreateDTO;
import com.foodtraceability.dto.TrainingCourseQueryDTO;
import com.foodtraceability.dto.TrainingCourseUpdateDTO;
import com.foodtraceability.dto.TrainingCourseVO;
import com.foodtraceability.dto.TrainingStatisticsVO;
import com.foodtraceability.dto.TrainingStudyRecordQueryDTO;
import com.foodtraceability.dto.TrainingStudyRecordVO;

/**
 * 培训管理Service接口
 * 提供培训课程、学习记录、证书的管理能力
 */
public interface TrainingService {

    /**
     * 分页查询培训课程
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TrainingCourseVO> getCoursePage(TrainingCourseQueryDTO queryDTO);

    /**
     * 根据ID获取课程详情
     *
     * @param id 课程ID
     * @return 课程VO
     */
    TrainingCourseVO getCourseById(String id);

    /**
     * 创建培训课程
     *
     * @param createDTO 创建DTO
     * @return 创建后的课程VO
     */
    TrainingCourseVO createCourse(TrainingCourseCreateDTO createDTO);

    /**
     * 更新培训课程
     *
     * @param id 课程ID
     * @param updateDTO 更新DTO
     * @return 更新后的课程VO
     */
    TrainingCourseVO updateCourse(String id, TrainingCourseUpdateDTO updateDTO);

    /**
     * 删除培训课程（逻辑删除）
     *
     * @param id 课程ID
     */
    void deleteCourse(String id);

    /**
     * 发布课程（状态 draft → published）
     *
     * @param id 课程ID
     */
    void publishCourse(String id);

    /**
     * 归档课程（状态 published → archived）
     *
     * @param id 课程ID
     */
    void archiveCourse(String id);

    /**
     * 分页查询培训学习记录
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TrainingStudyRecordVO> getStudyRecordPage(TrainingStudyRecordQueryDTO queryDTO);

    /**
     * 分页查询证书
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TrainingCertificateVO> getCertificatePage(TrainingCertificateQueryDTO queryDTO);

    /**
     * 获取培训统计信息
     *
     * @return 统计VO
     */
    TrainingStatisticsVO getStatistics();
}
