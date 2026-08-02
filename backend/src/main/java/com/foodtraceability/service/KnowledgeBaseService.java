package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.DeptReviewDTO;
import com.foodtraceability.dto.FinalReviewDTO;
import com.foodtraceability.dto.KnowledgeArticleCreateDTO;
import com.foodtraceability.dto.KnowledgeArticleQueryDTO;
import com.foodtraceability.dto.KnowledgeArticleUpdateDTO;
import com.foodtraceability.dto.KnowledgeArticleVO;
import com.foodtraceability.dto.KnowledgeCategoryStatVO;
import com.foodtraceability.dto.ReviewRecordVO;
import com.foodtraceability.dto.StudyRecordQueryDTO;
import com.foodtraceability.dto.StudyRecordStatisticsVO;
import com.foodtraceability.dto.StudyRecordVO;
import com.foodtraceability.dto.SubmitReviewDTO;
import java.util.List;

/**
 * 知识库服务接口
 * 提供知识库文章管理、联邦式审核流程、员工学习记录管理能力
 */
public interface KnowledgeBaseService {

    /**
     * 分页查询知识库文章列表
     *
     * @param queryDTO 查询参数
     * @return 文章分页结果
     */
    IPage<KnowledgeArticleVO> getArticlePage(KnowledgeArticleQueryDTO queryDTO);

    /**
     * 根据ID获取文章详情
     *
     * @param id 文章ID
     * @return 文章视图对象
     */
    KnowledgeArticleVO getArticleById(String id);

    /**
     * 创建文章
     *
     * @param createDTO 创建DTO
     * @return 创建后的文章视图对象
     */
    KnowledgeArticleVO createArticle(KnowledgeArticleCreateDTO createDTO);

    /**
     * 更新文章
     *
     * @param id        文章ID
     * @param updateDTO 更新DTO
     * @return 更新后的文章视图对象
     */
    KnowledgeArticleVO updateArticle(String id, KnowledgeArticleUpdateDTO updateDTO);

    /**
     * 删除文章（逻辑删除）
     *
     * @param id 文章ID
     */
    void deleteArticle(String id);

    /**
     * 发布文章（draft/pending_review/pending_final → published）
     *
     * @param id 文章ID
     */
    void publishArticle(String id);

    /**
     * 归档文章（published → archived）
     *
     * @param id 文章ID
     */
    void archiveArticle(String id);

    /**
     * 提交审核（draft → pending_review）
     *
     * @param id   文章ID
     * @param dto  提交审核DTO
     */
    void submitReview(String id, SubmitReviewDTO dto);

    /**
     * 部门审核（pending_review → pending_final/rejected）
     *
     * @param id  文章ID
     * @param dto 部门审核DTO
     */
    void deptReview(String id, DeptReviewDTO dto);

    /**
     * 店长终审（pending_final → published/rejected）
     *
     * @param id  文章ID
     * @param dto 终审DTO
     */
    void finalReview(String id, FinalReviewDTO dto);

    /**
     * 获取文章审核记录列表
     *
     * @param id 文章ID
     * @return 审核记录列表
     */
    List<ReviewRecordVO> getReviewRecords(String id);

    /**
     * 获取分类统计
     *
     * @return 分类统计列表
     */
    List<KnowledgeCategoryStatVO> getCategoryStats();

    /**
     * 分页查询员工学习记录
     *
     * @param queryDTO 查询参数
     * @return 学习记录分页结果
     */
    IPage<StudyRecordVO> getStudyRecordPage(StudyRecordQueryDTO queryDTO);

    /**
     * 获取学习记录统计
     *
     * @param articleId 文章ID（可选，传入时仅统计该文章的记录）
     * @return 统计结果
     */
    StudyRecordStatisticsVO getStudyRecordStatistics(String articleId);

    /**
     * 根据文章ID获取学习记录列表
     *
     * @param articleId 文章ID
     * @return 学习记录列表
     */
    List<StudyRecordVO> getStudyRecordsByArticle(String articleId);

    /**
     * 重置员工学习记录
     *
     * @param recordId 学习记录ID
     */
    void resetStudyRecord(String recordId);
}
