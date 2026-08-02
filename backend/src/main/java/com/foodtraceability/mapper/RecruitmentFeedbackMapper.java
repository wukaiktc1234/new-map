package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RecruitmentFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 招聘反馈 Mapper 接口
 * 对应表: recruitment_feedback
 */
@Mapper
public interface RecruitmentFeedbackMapper extends BaseMapper<RecruitmentFeedback> {
}
