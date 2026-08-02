package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.TrainingStudyRecord;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 培训学习记录Mapper接口
 * 提供培训学习记录相关的数据库操作
 */
@Repository
public interface TrainingStudyRecordMapper extends BaseMapper<TrainingStudyRecord> {

    /**
     * 分页查询培训学习记录
     *
     * @param page 分页对象
     * @param courseId 课程ID
     * @param employeeId 员工ID
     * @param completed 是否完成（true/false/null）
     * @param keyword 关键字（员工姓名或课程标题模糊匹配）
     * @param startDate 开始日期（按 start_time 过滤）
     * @param endDate 结束日期（按 start_time 过滤）
     * @return 分页结果
     */
    IPage<TrainingStudyRecord> selectStudyRecordPage(Page<TrainingStudyRecord> page,
                                                     @Param("courseId") String courseId,
                                                     @Param("employeeId") String employeeId,
                                                     @Param("completed") Boolean completed,
                                                     @Param("keyword") String keyword,
                                                     @Param("startDate") String startDate,
                                                     @Param("endDate") String endDate);
}
