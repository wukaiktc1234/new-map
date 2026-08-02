package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AssetFlowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AssetFlowRecordMapper extends BaseMapper<AssetFlowRecord> {
    
    @Select("SELECT * FROM asset_flow_records WHERE asset_id = #{assetId} ORDER BY flow_time DESC LIMIT #{limit}")
    List<AssetFlowRecord> findByAssetId(@Param("assetId") Long assetId, @Param("limit") int limit);
    
    @Select("SELECT * FROM asset_flow_records WHERE asset_id = #{assetId} ORDER BY flow_time DESC")
    List<AssetFlowRecord> findAllByAssetId(@Param("assetId") Long assetId);
}
