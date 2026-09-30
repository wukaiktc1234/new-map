package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.LocationIdMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 旧 ID → location_id 映射桥 Mapper。
 */
@Mapper
public interface LocationIdMapMapper extends BaseMapper<LocationIdMap> {

    /**
     * 反查：location_id → 旧来源主键（规则 3/4：一切跨 ID 空间的换算必须经本表）。
     * P1-USER-LOCATION-001 用途：users.location_id → stores_new.store_id。
     */
    @Select("SELECT src_id FROM location_id_map WHERE src_table = #{srcTable} AND location_id = #{locationId} LIMIT 1")
    Long selectSrcIdByLocationId(@Param("srcTable") String srcTable, @Param("locationId") Long locationId);

    /**
     * 正查：旧来源主键 → location_id。
     */
    @Select("SELECT location_id FROM location_id_map WHERE src_table = #{srcTable} AND src_id = #{srcId} LIMIT 1")
    Long selectLocationIdBySrcId(@Param("srcTable") String srcTable, @Param("srcId") Long srcId);
}
