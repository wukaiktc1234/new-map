package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.CostRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成本记录Mapper接口
 */
@Mapper
public interface CostRecordMapper extends BaseMapper<CostRecord> {

    /**
     * 分页查询成本记录
     * @param page 分页参数
     * @param costType 成本类型
     * @param period 成本归属期间
     * @param costCenterId 成本中心ID
     * @return 成本记录分页数据
     */
    IPage<CostRecord> selectCostRecordPage(Page<CostRecord> page,
                                          @Param("costType") Integer costType,
                                          @Param("period") String period,
                                          @Param("costCenterId") Long costCenterId);

    /**
     * 根据期间和类型汇总成本
     * @param period 期间
     * @param costType 成本类型
     * @return 总金额
     */
    BigDecimal sumAmountByPeriodAndType(@Param("period") String period,
                                        @Param("costType") Integer costType);

    /**
     * 成本结构分析
     * @param period 期间
     * @return 各成本类型的金额统计
     */
    List<Map<String, Object>> selectCostStructure(@Param("period") String period);
}
