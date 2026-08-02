package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 进货台账Mapper接口
 */
@Mapper
public interface PurchaseLedgerMapper extends BaseMapper<PurchaseLedger> {

    /**
     * 根据供应商ID查询台账列表
     * @param supplierId 供应商ID
     * @return 台账列表
     */
    List<PurchaseLedger> selectBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 根据批次号查询台账列表
     * @param batchNo 批次号
     * @return 台账列表
     */
    List<PurchaseLedger> selectByBatchNo(@Param("batchNo") String batchNo);

    /**
     * 查询即将过期的台账记录
     * @param days 天数
     * @return 台账列表
     */
    List<PurchaseLedger> selectExpiringWithinDays(@Param("days") int days);

    /**
     * 查询已过期的台账记录
     * @param expiryDate 当前日期
     * @return 台账列表
     */
    List<PurchaseLedger> selectExpired(@Param("expiryDate") LocalDate expiryDate);

    /**
     * 统计按供应商分组的采购金额
     * @return 统计结果列表
     */
    List<PurchaseLedger> selectAmountGroupBySupplier();
}
