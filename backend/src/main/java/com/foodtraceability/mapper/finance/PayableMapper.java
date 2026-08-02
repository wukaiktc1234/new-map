package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.Payable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Map;

/**
 * 应付账款Mapper接口
 */
@Mapper
public interface PayableMapper extends BaseMapper<Payable> {

    /**
     * 分页查询应付账款
     * @param page 分页参数
     * @param supplierName 供应商名称（模糊）
     * @param status 状态
     * @param startDate 到期日开始
     * @param endDate 到期日结束
     * @return 应付账款分页数据
     */
    IPage<Payable> selectPayablePage(Page<Payable> page,
                                     @Param("supplierName") String supplierName,
                                     @Param("status") Integer status,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * 统计应付账款汇总
     * @return 汇总数据（原始应付、已付、余额等）
     */
    Map<String, Object> selectPayableSummary();
}
