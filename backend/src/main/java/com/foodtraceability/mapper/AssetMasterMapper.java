package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AssetMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface AssetMasterMapper extends BaseMapper<AssetMaster> {
    
    @Select("SELECT status, COUNT(*) as count FROM asset_masters_enhanced WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();
    
    @Select("SELECT category_id, COUNT(*) as count FROM asset_masters_enhanced WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> countByType();
    
    @Select("SELECT store_id, COUNT(*) as count FROM asset_masters_enhanced WHERE deleted = 0 AND store_id IS NOT NULL GROUP BY store_id")
    List<Map<String, Object>> countByStore();
    
    @Select("SELECT COUNT(*) FROM asset_masters_enhanced WHERE deleted = 0 AND status = CAST(#{status} AS INTEGER)")
    int countStatus(@Param("status") String status);
    
    @Select("SELECT * FROM asset_masters_enhanced WHERE deleted = 0 AND qr_code = #{qrCode}")
    AssetMaster findByQrCode(@Param("qrCode") String qrCode);
    
    @Select("SELECT * FROM asset_masters_enhanced WHERE deleted = 0 AND asset_code = #{assetCode}")
    AssetMaster findByAssetCode(@Param("assetCode") String assetCode);
}
