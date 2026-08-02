package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MarketingPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 促销活动数据访问层
 */
@Mapper
public interface MarketingPromotionMapper extends BaseMapper<MarketingPromotion> {

    /**
     * 查询进行中的活动
     */
    @Select("SELECT * FROM marketing_promotion " +
            "WHERE status = 2 AND start_time <= NOW() AND end_time >= NOW() AND deleted = 0")
    List<MarketingPromotion> selectActivePromotions();

    /**
     * 统计进行中的活动数量
     */
    @Select("SELECT COUNT(*) FROM marketing_promotion WHERE status = 2 AND deleted = 0")
    long countActivePromotions();

    /**
     * 查询活动效果统计
     */
    @Select("SELECT COALESCE(SUM(participant_count), 0) as totalParticipants, " +
            "COALESCE(SUM(sales_amount), 0) as totalSales, " +
            "COALESCE(SUM(discount_amount), 0) as totalDiscount, " +
            "COUNT(*) as activityCount " +
            "FROM marketing_promotion WHERE deleted = 0")
    Map<String, Object> selectEffectSummary();
}
