package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SysStore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 门店Mapper接口
 */
@Mapper
public interface SysStoreMapper extends BaseMapper<SysStore> {

    /**
     * 根据用户ID查询门店
     */
    @Select("SELECT s.* FROM sys_stores s " +
            "INNER JOIN sys_user_stores us ON s.id = us.store_id " +
            "WHERE us.user_id = #{userId} AND s.status = 'active'")
    List<SysStore> selectStoresByUserId(@Param("userId") String userId);

    /**
     * 查询用户的默认门店
     */
    @Select("SELECT s.* FROM sys_stores s " +
            "INNER JOIN sys_user_stores us ON s.id = us.store_id " +
            "WHERE us.user_id = #{userId} AND us.is_default = 1 AND s.status = 'active' " +
            "LIMIT 1")
    SysStore selectDefaultStoreByUserId(@Param("userId") String userId);

    /**
     * 根据区域ID查询门店
     */
    @Select("SELECT * FROM sys_stores WHERE region_id = #{regionId} AND status = 'active'")
    List<SysStore> selectByRegionId(@Param("regionId") Long regionId);
}
