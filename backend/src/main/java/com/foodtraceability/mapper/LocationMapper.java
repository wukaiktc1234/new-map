package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Location;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存位置 Mapper。
 */
@Mapper
public interface LocationMapper extends BaseMapper<Location> {

    /**
     * 按来源表与来源主键解析位置（经 location_id_map，规则 3）。
     */
    @Select("SELECT l.* FROM locations l " +
            "JOIN location_id_map m ON m.location_id = l.location_id " +
            "WHERE m.src_table = #{srcTable} AND m.src_id = #{srcId} AND l.deleted = 0")
    Location selectBySource(@Param("srcTable") String srcTable, @Param("srcId") Long srcId);

    /**
     * 按门店 ID（stores_new.store_id 别名）解析 STORE 型位置。
     */
    @Select("SELECT l.* FROM locations l " +
            "JOIN location_id_map m ON m.location_id = l.location_id " +
            "WHERE m.src_table = 'stores_new' AND m.src_id = #{storeId} AND l.deleted = 0")
    Location selectByStoreId(@Param("storeId") Long storeId);

    /**
     * 活跃位置列表（可按类型过滤）。
     */
    @Select("<script>SELECT * FROM locations WHERE deleted = 0 AND status = 1" +
            "<if test='locationType != null and locationType != \"\"'> AND location_type = #{locationType}</if>" +
            " ORDER BY location_type, location_id</script>")
    List<Location> selectActive(@Param("locationType") String locationType);
}
