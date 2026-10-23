package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FundFlow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资金流水Mapper接口
 */
@Mapper
public interface FundFlowMapper extends BaseMapper<FundFlow> {

    /**
     * 当日流水号序列原子自增（复用通用号段表 order_sequence，键 {@code FLOWyyyyMMdd}）。
     *
     * <p>原实现是「查当天最后一条 flow_no → 末 4 位 +1」，并发下多个请求会拿到同一个号，
     * 撞 fund_flows_flow_no_key 唯一键 → 资金流水写入失败（高并发支付丢账）。
     * 改为数据库端原子自增后，行锁串行化，重复压力下也不会撞号。</p>
     *
     * @param sequenceKey 号段键，如 FLOW20261023
     * @return 分配到的序号
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, 1, NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = order_sequence.sequence_value + 1 "
            + "RETURNING sequence_value")
    Integer nextFlowSequence(@Param("sequenceKey") String sequenceKey);

    /**
     * 首次使用当日号段时按「当天已有最大流水号」初始化，避免从 1 重新开始撞历史流水号。
     * ON CONFLICT DO NOTHING 保证并发初始化只有一条生效。
     */
    @Insert("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, #{initValue}, NOW()) "
            + "ON CONFLICT (sequence_key) DO NOTHING")
    int initFlowSequenceIfAbsent(@Param("sequenceKey") String sequenceKey, @Param("initValue") Integer initValue);

    /** 当日流水号已用的最大末 4 位序号（无记录返回 0） */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM 11) AS INTEGER)), 0) FROM fund_flows "
            + "WHERE flow_no LIKE CONCAT(#{prefix}, '%') AND flow_no ~ CONCAT('^', #{prefix}, '[0-9]+$')")
    Integer maxFlowSequenceByPrefix(@Param("prefix") String prefix);
}
