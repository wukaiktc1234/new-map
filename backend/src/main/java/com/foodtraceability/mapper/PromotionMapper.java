package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Promotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PromotionMapper extends BaseMapper<Promotion> {

    /**
     * 查询有效的促销
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @param currentTime 当前时间
     * @return 促销列表
     */
    List<Promotion> selectValidPromotions(@Param("targetId") Long targetId, @Param("targetType") String targetType, @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查询所有促销
     * @param params 查询参数
     * @return 促销列表
     */
    List<Promotion> selectAllPromotions(@Param("params") java.util.Map<String, Object> params);

    /**
     * 更新促销状态
     * @param id 促销ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 批量更新促销状态
     * @param ids 促销ID列表
     * @param status 状态
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}
