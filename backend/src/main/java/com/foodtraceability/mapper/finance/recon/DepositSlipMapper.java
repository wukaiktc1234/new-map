package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.DepositSlip;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 现金交款单 Mapper（deposit_slips，设计 §5.7）。
 *
 * <p>除基础 CRUD 外，P2-A 追加缴款单号号段（复用通用 order_sequence 表，键 SLIPyyyyMMdd），
 * 与 FundFlowMapper 的 FLOW 号段同构：并发下用 ON CONFLICT 原子自增，避免「查最后一条 +1」撞唯一键。</p>
 */
@Mapper
public interface DepositSlipMapper extends BaseMapper<DepositSlip> {

    /** 取当天已有最大缴款单序号（DSyyyyMMdd 之后的 4 位）；无记录返回 0 */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(slip_no FROM 11) AS INTEGER)), 0) FROM deposit_slips "
            + "WHERE slip_no LIKE CONCAT(#{prefix}, '%')")
    Integer maxSlipSequenceByPrefix(@Param("prefix") String prefix);

    /**
     * 【P2 共用号段】缴款单号：抬升 + 原子取号（与流水号同构，单条语句）。
     *
     * <p>GREATEST(current, 当天 deposit_slips 最大序号) + 1 RETURNING；
     * flushCache/useCache 保证同事务内多次调用都真实执行，不命中 MyBatis 一级缓存。</p>
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES ("
            + "#{sequenceKey}, "
            + "(SELECT COALESCE(MAX(CAST(SUBSTRING(slip_no FROM 11) AS INTEGER)), 0) FROM deposit_slips "
            + "  WHERE slip_no LIKE CONCAT(#{prefix}, '%')) + 1, NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = GREATEST(order_sequence.sequence_value, "
            + "(SELECT COALESCE(MAX(CAST(SUBSTRING(slip_no FROM 11) AS INTEGER)), 0) FROM deposit_slips "
            + "  WHERE slip_no LIKE CONCAT(#{prefix}, '%'))) + 1 "
            + "RETURNING sequence_value")
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    Integer nextSlipSequenceWithFloor(@Param("sequenceKey") String sequenceKey, @Param("prefix") String prefix);

    /** 号段初始化（并发下仅一条 INSERT 生效） */
    @Insert("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, #{initValue}, NOW()) "
            + "ON CONFLICT (sequence_key) DO NOTHING")
    int initSlipSequenceIfAbsent(@Param("sequenceKey") String sequenceKey, @Param("initValue") Integer initValue);

    /**
     * 号段原子自增并返回新值。
     *
     * <p>flushCache=TRUE / useCache=false：INSERT ... RETURNING 被 MyBatis 当查询处理，
     * 同一事务内同参重复调用会命中一级缓存而不执行 SQL（P2-A 缺陷 1：一次存行取两个流水号撞唯一键）。
     * 这里显式清缓存，保证每次调用都是真实的 sequence_value + 1。</p>
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, 1, NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = order_sequence.sequence_value + 1 "
            + "RETURNING sequence_value")
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    Integer nextSlipSequence(@Param("sequenceKey") String sequenceKey);
}
