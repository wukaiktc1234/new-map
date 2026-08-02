package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.Receivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 应收账款Mapper接口
 */
@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    /**
     * 分页查询应收账款
     * @param page 分页参数
     * @param customerName 客户名称（模糊）
     * @param status 状态
     * @param startDate 到期日开始
     * @param endDate 到期日结束
     * @return 应收账款分页数据
     */
    IPage<Receivable> selectReceivablePage(Page<Receivable> page,
                                          @Param("customerName") String customerName,
                                          @Param("status") Integer status,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * 统计应收账款汇总
     * @return 汇总数据（原始应收、已收、余额等）
     */
    Map<String, Object> selectReceivableSummary();

    /**
     * 账龄分析
     * @return 各账龄段的金额统计
     */
    List<Map<String, Object>> selectAgingAnalysis();
}
