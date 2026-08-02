package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.dto.DeptReviewDTO;
import com.foodtraceability.dto.FinalReviewDTO;
import com.foodtraceability.dto.KnowledgeArticleCreateDTO;
import com.foodtraceability.dto.KnowledgeArticleQueryDTO;
import com.foodtraceability.dto.KnowledgeArticleUpdateDTO;
import com.foodtraceability.dto.KnowledgeArticleVO;
import com.foodtraceability.dto.KnowledgeCategoryStatVO;
import com.foodtraceability.dto.QuizQuestionDTO;
import com.foodtraceability.dto.ReviewRecordVO;
import com.foodtraceability.dto.StudyRecordQueryDTO;
import com.foodtraceability.dto.StudyRecordStatisticsVO;
import com.foodtraceability.dto.StudyRecordVO;
import com.foodtraceability.dto.SubmitReviewDTO;
import com.foodtraceability.entity.ArticleReviewRecord;
import com.foodtraceability.entity.ArticleStudyRecord;
import com.foodtraceability.entity.KnowledgeArticle;
import com.foodtraceability.mapper.ArticleReviewRecordMapper;
import com.foodtraceability.mapper.ArticleStudyRecordMapper;
import com.foodtraceability.mapper.KnowledgeArticleMapper;
import com.foodtraceability.service.KnowledgeBaseService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库服务实现类
 * 实现知识库文章管理、联邦式审核流程、员工学习记录管理
 */
@Service
public class KnowledgeBaseServiceImpl
        extends ServiceImpl<KnowledgeArticleMapper, KnowledgeArticle>
        implements KnowledgeBaseService {

    /**
     * 文章ID前缀
     */
    private static final String ARTICLE_ID_PREFIX = "kb-";

    /**
     * 学习记录ID前缀
     */
    private static final String STUDY_RECORD_ID_PREFIX = "SR-";

    /**
     * 审核记录ID前缀
     */
    private static final String REVIEW_RECORD_ID_PREFIX = "RR-";

    /**
     * 业务编号时间格式
     */
    private static final DateTimeFormatter ID_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 分类标签映射
     */
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "safety", "安全规范",
            "service", "服务标准",
            "manual", "操作手册",
            "policy", "公司制度"
    );

    private final KnowledgeArticleMapper articleMapper;
    private final ArticleStudyRecordMapper studyRecordMapper;
    private final ArticleReviewRecordMapper reviewRecordMapper;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseServiceImpl(KnowledgeArticleMapper articleMapper,
                                    ArticleStudyRecordMapper studyRecordMapper,
                                    ArticleReviewRecordMapper reviewRecordMapper,
                                    ObjectMapper objectMapper) {
        this.articleMapper = articleMapper;
        this.studyRecordMapper = studyRecordMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // 文章管理
    // ============================================================

    @Override
    public IPage<KnowledgeArticleVO> getArticlePage(KnowledgeArticleQueryDTO queryDTO) {
        Page<KnowledgeArticle> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        IPage<KnowledgeArticle> articlePage = articleMapper.selectArticlePage(
                page,
                queryDTO.getCategory(),
                queryDTO.getPublishStatus(),
                queryDTO.getKeyword()
        );
        // 转换为 VO 分页
        Page<KnowledgeArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        List<KnowledgeArticleVO> voRecords = new ArrayList<>();
        for (KnowledgeArticle article : articlePage.getRecords()) {
            voRecords.add(convertToVO(article));
        }
        voPage.setRecords(voRecords);
        return voPage;
    }

    @Override
    public KnowledgeArticleVO getArticleById(String id) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        // 阅读次数 +1
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        this.updateById(article);
        return convertToVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO createArticle(KnowledgeArticleCreateDTO createDTO) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(generateArticleId());
        article.setTitle(createDTO.getTitle());
        article.setSummary(createDTO.getSummary());
        article.setCategory(createDTO.getCategory());
        article.setIcon(createDTO.getIcon());
        article.setContent(createDTO.getContent());
        article.setCoverUrl(createDTO.getCoverUrl());
        article.setAuthor(createDTO.getAuthor());
        article.setPublishStatus(createDTO.getPublishStatus() == null ? "draft" : createDTO.getPublishStatus());
        article.setViewCount(0);
        article.setTags(toJson(createDTO.getTags()));
        article.setQuiz(quizListToJson(createDTO.getQuiz()));
        article.setAuthorId(createDTO.getAuthorId());
        article.setAuthorDepartment(createDTO.getAuthorDepartment());
        article.setIsImportant(createDTO.getIsImportant() == null ? false : createDTO.getIsImportant());
        article.setVersion(1);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        this.save(article);
        return convertToVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO updateArticle(String id, KnowledgeArticleUpdateDTO updateDTO) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (updateDTO.getTitle() != null) article.setTitle(updateDTO.getTitle());
        if (updateDTO.getSummary() != null) article.setSummary(updateDTO.getSummary());
        if (updateDTO.getCategory() != null) article.setCategory(updateDTO.getCategory());
        if (updateDTO.getIcon() != null) article.setIcon(updateDTO.getIcon());
        if (updateDTO.getContent() != null) article.setContent(updateDTO.getContent());
        if (updateDTO.getCoverUrl() != null) article.setCoverUrl(updateDTO.getCoverUrl());
        if (updateDTO.getAuthor() != null) article.setAuthor(updateDTO.getAuthor());
        if (updateDTO.getPublishStatus() != null) article.setPublishStatus(updateDTO.getPublishStatus());
        if (updateDTO.getTags() != null) article.setTags(toJson(updateDTO.getTags()));
        if (updateDTO.getQuiz() != null) article.setQuiz(quizListToJson(updateDTO.getQuiz()));
        if (updateDTO.getIsImportant() != null) article.setIsImportant(updateDTO.getIsImportant());
        if (updateDTO.getAuthorDepartment() != null) article.setAuthorDepartment(updateDTO.getAuthorDepartment());
        article.setVersion((article.getVersion() == null ? 1 : article.getVersion()) + 1);
        article.setUpdateTime(LocalDateTime.now());
        this.updateById(article);
        return convertToVO(article);
    }

    @Override
    public void deleteArticle(String id) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishArticle(String id) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        String currentStatus = article.getPublishStatus();
        if (!"draft".equals(currentStatus) && !"pending_review".equals(currentStatus)
                && !"pending_final".equals(currentStatus)) {
            throw new RuntimeException("当前状态不允许发布：" + currentStatus);
        }
        article.setPublishStatus("published");
        article.setPublishTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        this.updateById(article);
        createReviewRecord(id, "publish", "system", "系统管理员", "super_admin", null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveArticle(String id) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!"published".equals(article.getPublishStatus())) {
            throw new RuntimeException("仅已发布状态的文章可以归档");
        }
        article.setPublishStatus("archived");
        article.setUpdateTime(LocalDateTime.now());
        this.updateById(article);
        createReviewRecord(id, "archive", "system", "系统管理员", "super_admin", null, null);
    }

    // ============================================================
    // 联邦式审核流程
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(String id, SubmitReviewDTO dto) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!"draft".equals(article.getPublishStatus()) && !"rejected".equals(article.getPublishStatus())) {
            throw new RuntimeException("仅草稿或已退回状态的文章可以提交审核");
        }
        article.setPublishStatus("pending_review");
        article.setIsImportant(dto.getIsImportant());
        article.setUpdateTime(LocalDateTime.now());
        this.updateById(article);
        createReviewRecord(id, "submit",
                article.getAuthorId() == null ? "author" : article.getAuthorId(),
                article.getAuthor() == null ? "作者" : article.getAuthor(),
                "contributor", null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deptReview(String id, DeptReviewDTO dto) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!"pending_review".equals(article.getPublishStatus())) {
            throw new RuntimeException("仅待部门审核状态的文章可以进行部门审核");
        }
        LocalDateTime now = LocalDateTime.now();
        article.setReviewerId("system");
        article.setReviewerName("部门负责人");
        article.setReviewedAt(now);
        article.setReviewComment(dto.getComment());
        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 部门审核通过
            if (Boolean.TRUE.equals(article.getIsImportant())) {
                // 重要内容 → 待店长终审
                article.setPublishStatus("pending_final");
            } else {
                // 普通内容 → 直接发布
                article.setPublishStatus("published");
                article.setPublishTime(now);
                article.setPublishedBy("system");
            }
            article.setRejectReason(null);
            this.updateById(article);
            createReviewRecord(id, "dept_approve", "system", "部门负责人", "dept_manager", dto.getComment(), null);
        } else {
            // 部门审核退回
            article.setPublishStatus("rejected");
            article.setRejectReason(dto.getRejectReason());
            this.updateById(article);
            createReviewRecord(id, "dept_reject", "system", "部门负责人", "dept_manager", dto.getComment(), dto.getRejectReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalReview(String id, FinalReviewDTO dto) {
        KnowledgeArticle article = this.getById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!"pending_final".equals(article.getPublishStatus())) {
            throw new RuntimeException("仅待店长终审状态的文章可以进行终审");
        }
        LocalDateTime now = LocalDateTime.now();
        article.setReviewerId("system");
        article.setReviewerName("店长");
        article.setReviewedAt(now);
        article.setReviewComment(dto.getComment());
        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 终审通过 → 发布
            article.setPublishStatus("published");
            article.setPublishTime(now);
            article.setPublishedBy("system");
            article.setRejectReason(null);
            this.updateById(article);
            createReviewRecord(id, "final_approve", "system", "店长", "hr_manager", dto.getComment(), null);
        } else {
            // 终审退回
            article.setPublishStatus("rejected");
            article.setRejectReason(dto.getRejectReason());
            this.updateById(article);
            createReviewRecord(id, "final_reject", "system", "店长", "hr_manager", dto.getComment(), dto.getRejectReason());
        }
    }

    @Override
    public List<ReviewRecordVO> getReviewRecords(String id) {
        List<ArticleReviewRecord> records = reviewRecordMapper.selectByArticleId(id);
        List<ReviewRecordVO> voList = new ArrayList<>();
        for (ArticleReviewRecord record : records) {
            voList.add(convertToVO(record));
        }
        return voList;
    }

    @Override
    public List<KnowledgeCategoryStatVO> getCategoryStats() {
        List<Map<String, Object>> stats = articleMapper.selectCategoryStats();
        List<KnowledgeCategoryStatVO> result = new ArrayList<>();
        // 确保所有 4 个分类都返回（即使没有文章）
        Map<String, long[]> statMap = new java.util.HashMap<>();
        for (Map<String, Object> row : stats) {
            String category = (String) row.get("category");
            long count = row.get("count") == null ? 0 : ((Number) row.get("count")).longValue();
            long publishedCount = row.get("publishedCount") == null ? 0 : ((Number) row.get("publishedCount")).longValue();
            statMap.put(category, new long[]{count, publishedCount});
        }
        for (String category : new String[]{"safety", "service", "manual", "policy"}) {
            KnowledgeCategoryStatVO vo = new KnowledgeCategoryStatVO();
            vo.setCategory(category);
            vo.setLabel(CATEGORY_LABELS.getOrDefault(category, category));
            long[] data = statMap.getOrDefault(category, new long[]{0, 0});
            vo.setCount((int) data[0]);
            vo.setPublishedCount((int) data[1]);
            result.add(vo);
        }
        return result;
    }

    // ============================================================
    // 学习记录管理
    // ============================================================

    @Override
    public IPage<StudyRecordVO> getStudyRecordPage(StudyRecordQueryDTO queryDTO) {
        Page<ArticleStudyRecord> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        IPage<ArticleStudyRecord> recordPage = studyRecordMapper.selectStudyRecordPage(
                page,
                queryDTO.getArticleId(),
                queryDTO.getEmployeeId(),
                queryDTO.getStudyStatus(),
                queryDTO.getQuizStatus(),
                queryDTO.getCategory(),
                queryDTO.getKeyword()
        );
        Page<StudyRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<StudyRecordVO> voRecords = new ArrayList<>();
        for (ArticleStudyRecord record : recordPage.getRecords()) {
            voRecords.add(convertToVO(record));
        }
        voPage.setRecords(voRecords);
        return voPage;
    }

    @Override
    public StudyRecordStatisticsVO getStudyRecordStatistics(String articleId) {
        Map<String, Object> stats = studyRecordMapper.selectStudyRecordStatistics(articleId);
        StudyRecordStatisticsVO vo = new StudyRecordStatisticsVO();
        vo.setTotalRecords(getIntValue(stats, "totalRecords"));
        vo.setCompletedCount(getIntValue(stats, "completedCount"));
        vo.setInProgressCount(getIntValue(stats, "inProgressCount"));
        vo.setQuizPassedCount(getIntValue(stats, "quizPassedCount"));
        vo.setAvgReadProgress(getIntValue(stats, "avgReadProgress"));
        vo.setAvgQuizScore(getIntValue(stats, "avgQuizScore"));
        vo.setCertifiedCount(getIntValue(stats, "certifiedCount"));
        return vo;
    }

    @Override
    public List<StudyRecordVO> getStudyRecordsByArticle(String articleId) {
        // 使用 Mapper 的 selectList 配合 LambdaQueryWrapper
        List<ArticleStudyRecord> records = studyRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArticleStudyRecord>()
                        .eq(ArticleStudyRecord::getArticleId, articleId)
                        .orderByDesc(ArticleStudyRecord::getCreateTime)
        );
        List<StudyRecordVO> voList = new ArrayList<>();
        for (ArticleStudyRecord record : records) {
            voList.add(convertToVO(record));
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetStudyRecord(String recordId) {
        ArticleStudyRecord record = studyRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("学习记录不存在");
        }
        record.setStudyStatus("not_started");
        record.setReadProgress(0);
        record.setReadDuration(0);
        record.setQuizStatus("not_attempted");
        record.setQuizScore(null);
        record.setQuizAttemptCount(0);
        record.setLastQuizTime(null);
        record.setCertified(false);
        record.setLastReadTime(null);
        record.setFirstReadTime(null);
        record.setUpdateTime(LocalDateTime.now());
        studyRecordMapper.updateById(record);
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 生成文章业务编号
     * 格式：kb-{yyyyMMddHHmmss}{4位随机数}
     */
    private String generateArticleId() {
        return ARTICLE_ID_PREFIX + LocalDateTime.now().format(ID_TIME_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 生成审核记录业务编号
     * 格式：RR-{yyyyMMddHHmmss}{4位随机数}
     */
    private String generateReviewRecordId() {
        return REVIEW_RECORD_ID_PREFIX + LocalDateTime.now().format(ID_TIME_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 创建审核记录
     */
    private void createReviewRecord(String articleId, String action, String operatorId,
                                    String operatorName, String operatorRole,
                                    String comment, String rejectReason) {
        ArticleReviewRecord record = new ArticleReviewRecord();
        record.setRecordId(generateReviewRecordId());
        record.setArticleId(articleId);
        record.setAction(action);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setOperatorRole(operatorRole);
        record.setCreatedAt(LocalDateTime.now());
        record.setComment(comment);
        record.setRejectReason(rejectReason);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        reviewRecordMapper.insert(record);
    }

    /**
     * 文章 Entity → VO 转换（包含 tags/quiz JSON 解析）
     */
    private KnowledgeArticleVO convertToVO(KnowledgeArticle article) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setCategory(article.getCategory());
        vo.setIcon(article.getIcon());
        vo.setContent(article.getContent());
        vo.setCoverUrl(article.getCoverUrl());
        vo.setAuthor(article.getAuthor());
        vo.setPublishStatus(article.getPublishStatus());
        vo.setViewCount(article.getViewCount());
        vo.setTags(parseTags(article.getTags()));
        vo.setQuiz(parseQuiz(article.getQuiz()));
        vo.setPublishTime(article.getPublishTime());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        vo.setAuthorId(article.getAuthorId());
        vo.setAuthorDepartment(article.getAuthorDepartment());
        vo.setReviewerId(article.getReviewerId());
        vo.setReviewerName(article.getReviewerName());
        vo.setReviewedAt(article.getReviewedAt());
        vo.setReviewComment(article.getReviewComment());
        vo.setRejectReason(article.getRejectReason());
        vo.setPublishedBy(article.getPublishedBy());
        vo.setIsImportant(article.getIsImportant());
        vo.setVersion(article.getVersion());
        return vo;
    }

    /**
     * 学习记录 Entity → VO 转换
     */
    private StudyRecordVO convertToVO(ArticleStudyRecord record) {
        StudyRecordVO vo = new StudyRecordVO();
        vo.setId(record.getId());
        vo.setArticleId(record.getArticleId());
        vo.setArticleTitle(record.getArticleTitle());
        vo.setArticleCategory(record.getArticleCategory());
        vo.setEmployeeId(record.getEmployeeId());
        vo.setEmployeeName(record.getEmployeeName());
        vo.setEmployeeNo(record.getEmployeeNo());
        vo.setStoreName(record.getStoreName());
        vo.setStudyStatus(record.getStudyStatus());
        vo.setReadProgress(record.getReadProgress());
        vo.setReadDuration(record.getReadDuration());
        vo.setLastReadTime(record.getLastReadTime());
        vo.setFirstReadTime(record.getFirstReadTime());
        vo.setQuizStatus(record.getQuizStatus());
        vo.setQuizScore(record.getQuizScore());
        vo.setQuizAttemptCount(record.getQuizAttemptCount());
        vo.setLastQuizTime(record.getLastQuizTime());
        vo.setCertified(record.getCertified());
        return vo;
    }

    /**
     * 审核记录 Entity → VO 转换
     */
    private ReviewRecordVO convertToVO(ArticleReviewRecord record) {
        ReviewRecordVO vo = new ReviewRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setArticleId(record.getArticleId());
        vo.setAction(record.getAction());
        vo.setOperatorId(record.getOperatorId());
        vo.setOperatorName(record.getOperatorName());
        vo.setOperatorRole(record.getOperatorRole());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setComment(record.getComment());
        vo.setRejectReason(record.getRejectReason());
        return vo;
    }

    /**
     * 解析 tags JSON 字符串为 List
     */
    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 解析 quiz JSON 字符串为 List<QuizQuestionDTO>
     */
    private List<QuizQuestionDTO> parseQuiz(String quizJson) {
        if (quizJson == null || quizJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(quizJson, new TypeReference<List<QuizQuestionDTO>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * List<String> 转 JSON 字符串
     */
    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * List<QuizQuestionDTO> 转 JSON 字符串
     */
    private String quizListToJson(List<QuizQuestionDTO> quizList) {
        if (quizList == null || quizList.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(quizList);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从 Map 中安全获取 Integer 值
     */
    private Integer getIntValue(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return 0;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
