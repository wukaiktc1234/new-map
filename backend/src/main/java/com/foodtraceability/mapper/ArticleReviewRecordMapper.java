package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ArticleReviewRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 文章审核记录 Mapper 接口
 */
@Repository
public interface ArticleReviewRecordMapper extends BaseMapper<ArticleReviewRecord> {

    /**
     * 根据文章ID查询审核记录列表（按操作时间正序）
     *
     * @param articleId 文章ID
     * @return 审核记录列表
     */
    List<ArticleReviewRecord> selectByArticleId(@Param("articleId") String articleId);
}
