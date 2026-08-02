package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.TrainingCourse;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * 培训课程Mapper接口
 * 提供培训课程相关的数据库操作
 */
@Repository
public interface TrainingCourseMapper extends BaseMapper<TrainingCourse> {

    /**
     * 分页查询培训课程列表
     *
     * @param page 分页对象
     * @param courseType 课程类型（required/elective）
     * @param publishStatus 发布状态（draft/published/archived）
     * @param keyword 关键字（标题模糊匹配）
     * @return 分页结果
     */
    IPage<TrainingCourse> selectCoursePage(Page<TrainingCourse> page,
                                            @Param("courseType") String courseType,
                                            @Param("publishStatus") String publishStatus,
                                            @Param("keyword") String keyword);

    /**
     * 查询培训统计信息
     * 返回 Map 包含：
     * - totalCourses: 总课程数
     * - publishedCourses: 已发布课程数
     * - totalAssigned: 总分配人数
     * - totalCompleted: 总完成人数
     * - averageScore: 平均得分
     *
     * @return 统计聚合 Map
     */
    Map<String, Object> selectCourseStatistics();
}
