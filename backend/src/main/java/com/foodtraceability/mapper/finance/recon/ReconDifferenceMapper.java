package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.ReconDifference;
import com.foodtraceability.dto.finance.recon.ChannelFeeSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 差异池 Mapper（recon_differences，设计 §5.6）。
 *
 * <p>P0 仅提供基础 CRUD 能力（结构就位）；勾稽/写账逻辑属 P1/P2，不在 P0 范围。</p>
 */
@Mapper
public interface ReconDifferenceMapper extends BaseMapper<ReconDifference> {

    /**
     * AC-50 渠道手续费汇总（只读）：CHANNEL_FEE 差异 × bank_transactions.source_type 分组。
     *
     * <p>join 口径（lead 指定）：{@code bt.external_txn_id = rd.external_ref}，且
     * {@code rd.difference_type='CHANNEL_FEE'}、{@code rd.external_ref IS NOT NULL}。
     * 三个可选条件（runId / from / to）都不传时，生成的 SQL 与不带 {@code <if>} 的版本一致。</p>
     *
     * <p>{@code from} 含端点、{@code to} 不含端点（半开区间），按 {@code rd.create_time} 过滤。</p>
     */
    @Select("<script>"
            + "SELECT bt.source_type AS sourceType,"
            + "       COUNT(*) AS diffCount,"
            + "       COALESCE(SUM(bt.fee), 0) AS feeTotalFen,"
            + "       COALESCE(SUM(rd.diff_amount), 0) AS diffAmountFen "
            + "  FROM recon_differences rd "
            + "  JOIN bank_transactions bt ON bt.external_txn_id = rd.external_ref AND bt.deleted = 0 "
            + " WHERE rd.difference_type = 'CHANNEL_FEE' "
            + "   AND rd.external_ref IS NOT NULL "
            + "<if test='runId != null'> AND rd.run_id = #{runId} </if>"
            + "<if test='from != null'> AND rd.create_time &gt;= #{from} </if>"
            + "<if test='to != null'> AND rd.create_time &lt; #{to} </if>"
            + " GROUP BY bt.source_type "
            + " ORDER BY bt.source_type"
            + "</script>")
    List<ChannelFeeSummaryVO.Row> channelFeeSummary(@Param("runId") Long runId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
