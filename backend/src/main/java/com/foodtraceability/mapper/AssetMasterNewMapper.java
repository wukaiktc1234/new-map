package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AssetMasterNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 资产主数据Mapper接口
 */
@Mapper
public interface AssetMasterNewMapper extends BaseMapper<AssetMasterNew> {

    /**
     * 根据资产编码查询资产
     * @param assetCode 资产编码
     * @return 资产信息
     */
    @Select("SELECT * FROM asset_masters_enhanced WHERE asset_code = #{assetCode} AND deleted = 0")
    AssetMasterNew selectByAssetCode(@Param("assetCode") String assetCode);

    /**
     * 根据二维码查询资产
     * @param qrCode 二维码
     * @return 资产信息
     */
    @Select("SELECT * FROM asset_masters_enhanced WHERE qr_code = #{qrCode} AND deleted = 0")
    AssetMasterNew selectByQrCode(@Param("qrCode") String qrCode);

    /**
     * 按状态统计资产数量
     * @return 统计结果列表
     */
    @Select("SELECT status, COUNT(*) as count FROM asset_masters_enhanced WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 按分类统计资产数量
     * @return 统计结果列表
     */
    @Select("SELECT category_id, COUNT(*) as count FROM asset_masters_enhanced WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> countByCategory();

    /**
     * 按门店统计资产数量
     * @return 统计结果列表
     */
    @Select("SELECT store_id, COUNT(*) as count FROM asset_masters_enhanced WHERE deleted = 0 GROUP BY store_id")
    List<Map<String, Object>> countByStore();

    /**
     * 查询需要折旧的资产（在用状态且未完全折旧）
     * @return 需要折旧的资产列表
     */
    @Select("SELECT * FROM asset_masters_enhanced WHERE status = 1 " +
            "AND (accumulated_depreciation IS NULL OR accumulated_depreciation < original_cost) " +
            "AND deleted = 0")
    List<AssetMasterNew> selectAssetsNeedDepreciation();
}
