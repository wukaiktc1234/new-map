package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TrayScanRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 托盘扫码记录 Mapper
 */
@Mapper
public interface TrayScanRecordMapper extends BaseMapper<TrayScanRecord> {

    /**
     * 查询某托盘指定扫码类型的最近一次成功（非防抖忽略）记录
     * <p>用于防抖判断：本次扫码时间 - 上次扫码时间 &lt; 阈值 则忽略</p>
     */
    @Select("SELECT * FROM tray_scan_record " +
            "WHERE tray_id = #{trayId} AND scan_type = #{scanType} AND is_debounced = 0 AND deleted = 0 " +
            "ORDER BY scan_time DESC LIMIT 1")
    TrayScanRecord findLastEffectiveScan(@Param("trayId") Long trayId, @Param("scanType") String scanType);
}
