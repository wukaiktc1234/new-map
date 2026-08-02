package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
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
import com.foodtraceability.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理控制器
 * 对接前端 /v1/hr/knowledge-base 路径，提供 16 个端点
 * 包含文章 CRUD、联邦式审核流程、员工学习记录管理
 */
@RestController
@RequestMapping("/v1/hr/knowledge-base")
@Tag(name = "HR 知识库管理")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    // ============================================================
    // 文章管理（CRUD）
    // ============================================================

    /**
     * 分页查询知识库文章列表
     */
    @GetMapping
    @Operation(summary = "分页查询知识库文章列表")
    public Result<IPage<KnowledgeArticleVO>> getArticlePage(KnowledgeArticleQueryDTO queryDTO) {
        try {
            IPage<KnowledgeArticleVO> page = knowledgeBaseService.getArticlePage(queryDTO);
            return Result.success(page, "查询知识库文章列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询知识库文章列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取文章详情（阅读次数 +1）
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取文章详情")
    public Result<KnowledgeArticleVO> getArticleById(
            @Parameter(description = "文章ID") @PathVariable("id") String id) {
        try {
            KnowledgeArticleVO vo = knowledgeBaseService.getArticleById(id);
            return Result.success(vo, "获取文章详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取文章详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建文章
     */
    @PostMapping
    @Operation(summary = "创建知识库文章")
    public Result<KnowledgeArticleVO> createArticle(@Valid @RequestBody KnowledgeArticleCreateDTO createDTO) {
        try {
            KnowledgeArticleVO vo = knowledgeBaseService.createArticle(createDTO);
            return Result.success(vo, "创建文章成功");
        } catch (Exception e) {
            return Result.error(500, "创建文章失败：" + e.getMessage());
        }
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识库文章")
    public Result<KnowledgeArticleVO> updateArticle(
            @Parameter(description = "文章ID") @PathVariable("id") String id,
            @Valid @RequestBody KnowledgeArticleUpdateDTO updateDTO) {
        try {
            KnowledgeArticleVO vo = knowledgeBaseService.updateArticle(id, updateDTO);
            return Result.success(vo, "更新文章成功");
        } catch (Exception e) {
            return Result.error(500, "更新文章失败：" + e.getMessage());
        }
    }

    /**
     * 删除文章（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库文章")
    public Result<Void> deleteArticle(
            @Parameter(description = "文章ID") @PathVariable("id") String id) {
        try {
            knowledgeBaseService.deleteArticle(id);
            return Result.success(null, "删除文章成功");
        } catch (Exception e) {
            return Result.error(500, "删除文章失败：" + e.getMessage());
        }
    }

    // ============================================================
    // 文章发布与归档
    // ============================================================

    /**
     * 发布文章（draft/pending_review/pending_final → published）
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布知识库文章")
    public Result<Void> publishArticle(
            @Parameter(description = "文章ID") @PathVariable("id") String id) {
        try {
            knowledgeBaseService.publishArticle(id);
            return Result.success(null, "发布文章成功");
        } catch (Exception e) {
            return Result.error(500, "发布文章失败：" + e.getMessage());
        }
    }

    /**
     * 归档文章（published → archived）
     */
    @PostMapping("/{id}/archive")
    @Operation(summary = "归档知识库文章")
    public Result<Void> archiveArticle(
            @Parameter(description = "文章ID") @PathVariable("id") String id) {
        try {
            knowledgeBaseService.archiveArticle(id);
            return Result.success(null, "归档文章成功");
        } catch (Exception e) {
            return Result.error(500, "归档文章失败：" + e.getMessage());
        }
    }

    // ============================================================
    // 联邦式审核流程
    // ============================================================

    /**
     * 提交审核（draft → pending_review）
     */
    @PostMapping("/{id}/submit-review")
    @Operation(summary = "提交文章审核")
    public Result<Void> submitReview(
            @Parameter(description = "文章ID") @PathVariable("id") String id,
            @Valid @RequestBody SubmitReviewDTO dto) {
        try {
            knowledgeBaseService.submitReview(id, dto);
            return Result.success(null, "提交审核成功");
        } catch (Exception e) {
            return Result.error(500, "提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 部门审核（pending_review → pending_final/rejected）
     */
    @PostMapping("/{id}/dept-review")
    @Operation(summary = "部门审核文章")
    public Result<Void> deptReview(
            @Parameter(description = "文章ID") @PathVariable("id") String id,
            @Valid @RequestBody DeptReviewDTO dto) {
        try {
            knowledgeBaseService.deptReview(id, dto);
            return Result.success(null, "部门审核成功");
        } catch (Exception e) {
            return Result.error(500, "部门审核失败：" + e.getMessage());
        }
    }

    /**
     * 店长终审（pending_final → published/rejected）
     */
    @PostMapping("/{id}/final-review")
    @Operation(summary = "店长终审文章")
    public Result<Void> finalReview(
            @Parameter(description = "文章ID") @PathVariable("id") String id,
            @Valid @RequestBody FinalReviewDTO dto) {
        try {
            knowledgeBaseService.finalReview(id, dto);
            return Result.success(null, "店长终审成功");
        } catch (Exception e) {
            return Result.error(500, "店长终审失败：" + e.getMessage());
        }
    }

    /**
     * 获取文章审核记录列表
     */
    @GetMapping("/{id}/review-records")
    @Operation(summary = "获取文章审核记录")
    public Result<List<ReviewRecordVO>> getReviewRecords(
            @Parameter(description = "文章ID") @PathVariable("id") String id) {
        try {
            List<ReviewRecordVO> records = knowledgeBaseService.getReviewRecords(id);
            return Result.success(records, "获取审核记录成功");
        } catch (Exception e) {
            return Result.error(500, "获取审核记录失败：" + e.getMessage());
        }
    }

    // ============================================================
    // 分类统计
    // ============================================================

    /**
     * 获取分类统计（按 category 分组统计总数与已发布数）
     * 注意：此路径为字面量路径，优先于 /{id} 匹配
     */
    @GetMapping("/category-stats")
    @Operation(summary = "获取知识库分类统计")
    public Result<List<KnowledgeCategoryStatVO>> getCategoryStats() {
        try {
            List<KnowledgeCategoryStatVO> stats = knowledgeBaseService.getCategoryStats();
            return Result.success(stats, "获取分类统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取分类统计失败：" + e.getMessage());
        }
    }

    // ============================================================
    // 员工学习记录管理
    // ============================================================

    /**
     * 分页查询员工学习记录
     * 注意：此路径为字面量路径，优先于 /{id} 匹配
     */
    @GetMapping("/study-records")
    @Operation(summary = "分页查询员工学习记录")
    public Result<IPage<StudyRecordVO>> getStudyRecordPage(StudyRecordQueryDTO queryDTO) {
        try {
            IPage<StudyRecordVO> page = knowledgeBaseService.getStudyRecordPage(queryDTO);
            return Result.success(page, "查询学习记录成功");
        } catch (Exception e) {
            return Result.error(500, "查询学习记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取学习记录统计
     * 注意：此路径为字面量路径，优先于 /{id} 匹配
     */
    @GetMapping("/study-records/statistics")
    @Operation(summary = "获取学习记录统计")
    public Result<StudyRecordStatisticsVO> getStudyRecordStatistics(
            @Parameter(description = "文章ID（可选）") @org.springframework.web.bind.annotation.RequestParam(required = false) String articleId) {
        try {
            StudyRecordStatisticsVO statistics = knowledgeBaseService.getStudyRecordStatistics(articleId);
            return Result.success(statistics, "获取学习记录统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取学习记录统计失败：" + e.getMessage());
        }
    }

    /**
     * 根据文章ID获取学习记录列表
     */
    @GetMapping("/articles/{articleId}/study-records")
    @Operation(summary = "根据文章ID获取学习记录")
    public Result<List<StudyRecordVO>> getStudyRecordsByArticle(
            @Parameter(description = "文章ID") @PathVariable("articleId") String articleId) {
        try {
            List<StudyRecordVO> records = knowledgeBaseService.getStudyRecordsByArticle(articleId);
            return Result.success(records, "获取文章学习记录成功");
        } catch (Exception e) {
            return Result.error(500, "获取文章学习记录失败：" + e.getMessage());
        }
    }

    /**
     * 重置员工学习记录（管理员操作）
     */
    @PostMapping("/study-records/{recordId}/reset")
    @Operation(summary = "重置员工学习记录")
    public Result<Void> resetStudyRecord(
            @Parameter(description = "学习记录ID") @PathVariable("recordId") String recordId) {
        try {
            knowledgeBaseService.resetStudyRecord(recordId);
            return Result.success(null, "重置学习记录成功");
        } catch (Exception e) {
            return Result.error(500, "重置学习记录失败：" + e.getMessage());
        }
    }
}
