package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.KnowledgeArticle;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 知识库文章 Mapper 接口
 */
@Repository
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

    /**
     * 分页查询知识库文章（支持分类、状态、关键词筛选）
     *
     * @param page          分页参数
     * @param category      分类
     * @param publishStatus 发布状态
     * @param keyword       关键词（匹配标题或摘要）
     * @return 文章分页结果
     */
    IPage<KnowledgeArticle> selectArticlePage(Page<KnowledgeArticle> page,
                                              @Param("category") String category,
                                              @Param("publishStatus") String publishStatus,
                                              @Param("keyword") String keyword);

    /**
     * 查询分类统计（按 category 分组，统计总数与已发布数）
     *
     * @return 分类统计列表，每项包含 category、count、publishedCount
     */
    List<Map<String, Object>> selectCategoryStats();
}
