package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.ArticleStudyRecord;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 员工学习记录 Mapper 接口
 */
@Repository
public interface ArticleStudyRecordMapper extends BaseMapper<ArticleStudyRecord> {

    /**
     * 分页查询员工学习记录（支持多条件筛选）
     *
     * @param page         分页参数
     * @param articleId    文章ID
     * @param employeeId   员工ID
     * @param studyStatus  学习状态
     * @param quizStatus   测验状态
     * @param category     文章分类
     * @param keyword      关键词（匹配员工姓名/工号/文章标题/门店名）
     * @return 学习记录分页结果
     */
    IPage<ArticleStudyRecord> selectStudyRecordPage(Page<ArticleStudyRecord> page,
                                                     @Param("articleId") String articleId,
                                                     @Param("employeeId") String employeeId,
                                                     @Param("studyStatus") String studyStatus,
                                                     @Param("quizStatus") String quizStatus,
                                                     @Param("category") String category,
                                                     @Param("keyword") String keyword);

    /**
     * 查询学习记录统计
     *
     * @param articleId 文章ID（可选，传入时仅统计该文章的记录）
     * @return 统计结果 Map（totalRecords, completedCount, inProgressCount, quizPassedCount, avgReadProgress, avgQuizScore, certifiedCount）
     */
    Map<String, Object> selectStudyRecordStatistics(@Param("articleId") String articleId);
}
