package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FundFlow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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
     * <p><b>为什么必须 flushCache=TRUE（P2-A 缺陷 1）</b>：本方法是 INSERT ... RETURNING，
     * 但注解是 {@code @Select}，MyBatis 会把它当查询：同一 SqlSession（=同一事务）内用相同参数
     * 再调一次时，直接命中**一级缓存**返回上次的结果，SQL 根本不会执行 ——
     * 于是「一次缴款单存行连续取两个流水号」会拿到同一个号，第二条 insert 撞
     * {@code fund_flows_flow_no_key}。加上 flushCache=TRUE 后，每次调用前先清本地缓存、
     * useCache=false 跳过二级缓存，保证同一事务内 N 次取号每次都真实执行
     * {@code ON CONFLICT ... sequence_value + 1}，因而互不相同。</p>
     *
     * @param sequenceKey 号段键，如 FLOW20261023
     * @return 分配到的序号
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, 1, NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = order_sequence.sequence_value + 1 "
            + "RETURNING sequence_value")
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    Integer nextFlowSequence(@Param("sequenceKey") String sequenceKey);

    /**
     * 首次使用当日号段时按「当天已有最大流水号」初始化，避免从 1 重新开始撞历史流水号。
     * ON CONFLICT DO NOTHING 保证并发初始化只有一条生效。
     */
    @Insert("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, #{initValue}, NOW()) "
            + "ON CONFLICT (sequence_key) DO NOTHING")
    int initFlowSequenceIfAbsent(@Param("sequenceKey") String sequenceKey, @Param("initValue") Integer initValue);

    /**
     * 【P2 共用号段】抬升 + 原子取号（单条语句，SL6/SL10 使用）。
     *
     * <p>语义：先把 order_sequence 的 sequence_value <b>抬到 ≥ 当天已用最大序号</b>
     * （GREATEST(current, MAX(今日 flow_no 末 4 位))），再 +1 并 RETURNING。
     * 首次创建走 VALUES 分支（= 当天 MAX + 1）。</p>
     *
     * <p><b>为什么同事务内连续取 N 个号不会重号</b>：</p>
     * <ol>
     *   <li>号源是号段表 sequence_value 自身（行级锁 +1），不是「查 MAX + 1」；</li>
     *   <li>每次调用都真实执行本语句：flushCache=TRUE 清一级缓存、useCache=false 跳过二级缓存，
     *       不会像普通 {@code @Select} 那样被 MyBatis 把第二次调用当缓存命中；</li>
     *   <li>GREATEST(..., 当天 MAX) 只是「下限兜底」（防号段落后于历史流水），
     *       在事务内它最多等于上一号的序号，不会撞上本事务刚取走的下一个值。</li>
     * </ol>
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES ("
            + "#{sequenceKey}, "
            + "(SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM 11) AS INTEGER)), 0) FROM fund_flows "
            + "  WHERE flow_no LIKE CONCAT(#{prefix}, '%') AND flow_no ~ CONCAT('^', #{prefix}, '[0-9]+$')) + 1, "
            + "NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = GREATEST(order_sequence.sequence_value, "
            + "(SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM 11) AS INTEGER)), 0) FROM fund_flows "
            + "  WHERE flow_no LIKE CONCAT(#{prefix}, '%') AND flow_no ~ CONCAT('^', #{prefix}, '[0-9]+$'))) + 1 "
            + "RETURNING sequence_value")
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    Integer nextFlowSequenceWithFloor(@Param("sequenceKey") String sequenceKey, @Param("prefix") String prefix);

    /** 当日流水号已用的最大末 4 位序号（无记录返回 0） */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM 11) AS INTEGER)), 0) FROM fund_flows "
            + "WHERE flow_no LIKE CONCAT(#{prefix}, '%') AND flow_no ~ CONCAT('^', #{prefix}, '[0-9]+$')")
    Integer maxFlowSequenceByPrefix(@Param("prefix") String prefix);
}
