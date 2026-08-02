package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.MsgSendRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息发送记录Mapper接口
 * 提供发送记录的分页查询、状态统计、业务关联查询等方法
 */
@Mapper
public interface MsgSendRecordMapper extends BaseMapper<MsgSendRecord> {

    /**
     * 分页查询发送记录列表
     *
     * @param page 分页参数
     * @param recipient 收件人(模糊)
     * @param channel 发送渠道
     * @param sendStatus 发送状态
     * @param bizType 业务类型
     * @param triggerType 触发类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    IPage<MsgSendRecord> selectRecordPage(
        Page<MsgSendRecord> page,
        @Param("recipient") String recipient,
        @Param("channel") String channel,
        @Param("sendStatus") Integer sendStatus,
        @Param("bizType") String bizType,
        @Param("triggerType") Integer triggerType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 按发送状态统计数量
     *
     * @return 统计结果Map，key为sendStatus描述，value为数量
     */
    Map<String, Long> countByStatus();

    /**
     * 按业务类型统计发送数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果List
     */
    List<Map<String, Object>> countByBizType(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 按日期统计发送趋势数据
     *
     * @param days 统计天数
     * @return 趋势数据List
     */
    List<Map<String, Object>> countDailyTrend(@Param("days") int days);

    /**
     * 按业务ID查询发送记录
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 发送记录列表
     */
    List<MsgSendRecord> selectByBizInfo(
        @Param("bizType") String bizType,
        @Param("bizId") String bizId
    );
}
