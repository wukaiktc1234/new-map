package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.entity.CallNumberQueueNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 叫号队列Mapper接口
 */
@Mapper
public interface CallNumberQueueNewMapper extends BaseMapper<CallNumberQueueNew> {

    /**
     * 查询等待中的排队记录
     */
    @Select("SELECT * FROM call_number_queues WHERE queue_type = #{queueType} AND status = 1 AND deleted = 0 ORDER BY queue_id ASC")
    java.util.List<CallNumberQueueNew> selectWaitingByType(@Param("queueType") Integer queueType);

    /**
     * 获取当前最大号码
     */
    @Select("SELECT MAX(CAST(SUBSTRING(ticket_number FROM 2) AS INTEGER)) FROM call_number_queues WHERE queue_type = #{queueType} AND DATE(create_time) = CURRENT_DATE AND deleted = 0")
    Integer getMaxTicketNumberToday(@Param("queueType") Integer queueType);

    /**
     * 叫号操作：更新状态和叫号时间
     */
    @Update("UPDATE call_number_queues SET status = #{status}, call_time = CASE WHEN #{status} = 2 THEN NOW() ELSE call_time END, called_count = called_count + 1, update_time = NOW() WHERE queue_id = #{queueId} AND deleted = 0")
    int callNumber(@Param("queueId") Long queueId, @Param("status") Integer status);

    /** 统计各状态排队数量 */
    @Select("SELECT status, COUNT(*) as cnt FROM call_number_queues WHERE queue_type = #{queueType} AND DATE(create_time) = CURRENT_DATE AND deleted = 0 GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countByStatusToday(@Param("queueType") Integer queueType);

    /** 按门店统计今日各状态排队数量 */
    @Select("SELECT status, COUNT(*) as cnt FROM call_number_queues WHERE store_id = #{storeId} AND DATE(create_time) = CURRENT_DATE AND deleted = 0 GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countGroupByStatus(@Param("storeId") Long storeId);

    /**
     * 根据门店ID分页查询排队记录，支持状态、类型、关键词和日期范围过滤
     */
    IPage<CallNumberQueueNew> selectByStoreId(IPage<CallNumberQueueNew> page,
                                              @Param("storeId") Long storeId,
                                              @Param("status") Integer status,
                                              @Param("queueType") Integer queueType,
                                              @Param("keyword") String keyword,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);
}
