package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.asset.AssetMasterCreateDTO;
import com.foodtraceability.entity.AssetMasterNew;

import java.util.List;
import java.util.Map;

/**
 * 资产主数据服务接口
 */
public interface AssetMasterNewService extends IService<AssetMasterNew> {

    /**
     * 分页查询资产列表
     * @param page 分页参数
     * @param categoryId 分类ID
     * @param status 状态
     * @param storeId 门店ID
     * @param keyword 关键词
     * @return 分页结果
     */
    Page<AssetMasterNew> getPageList(Page<AssetMasterNew> page, Long categoryId,
                                     Integer status, Long storeId, String keyword);

    /**
     * 根据资产编码查询
     * @param assetCode 资产编码
     * @return 资产信息
     */
    AssetMasterNew getByAssetCode(String assetCode);

    /**
     * 根据二维码查询
     * @param qrCode 二维码
     * @return 资产信息
     */
    AssetMasterNew getByQrCode(String qrCode);

    /**
     * 创建新资产
     * @param createDTO 创建DTO
     * @return 新创建的资产
     */
    AssetMasterNew createAsset(AssetMasterCreateDTO createDTO);

    /**
     * 更新资产状态
     * @param assetId 资产ID
     * @param status 新状态
     * @param reason 原因（报废时必填）
     */
    void updateStatus(Long assetId, Integer status, String reason);

    /**
     * 报废资产
     * @param assetId 资产ID
     * @param reason 报废原因
     */
    void scrapAsset(Long assetId, String reason);

    /**
     * 处置资产
     * @param assetId 资产ID
     * @param reason 处置原因
     */
    void disposeAsset(Long assetId, String reason);

    /**
     * 获取资产统计概览
     * @return 统计数据Map
     */
    Map<String, Object> getOverviewStats();

    /**
     * 生成资产编码
     * @param prefix 编码前缀
     * @return 新的资产编码
     */
    String generateAssetCode(String prefix);

    /**
     * 执行月度折旧计算
     * @param period 折旧期间（YYYY-MM）
     * @return 折旧的资产数量
     */
    int executeMonthlyDepreciation(String period);
}
