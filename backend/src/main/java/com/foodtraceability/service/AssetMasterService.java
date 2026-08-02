package com.foodtraceability.service;

import com.foodtraceability.entity.AssetMaster;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;

public interface AssetMasterService extends IService<AssetMaster> {
    
    List<Map<String, Object>> countByStatus();
    
    List<Map<String, Object>> countByType();
    
    List<Map<String, Object>> countByStore();
    
    Map<String, Object> getOverviewStats();
    
    AssetMaster findByQrCode(String qrCode);
    
    AssetMaster findByAssetCode(String assetCode);
    
    void bindOrder(String assetCode, String orderId, Long kitchenOrderId, String operatorName);
    
    void releaseAsset(String assetCode, String operatorName);
    
    void allocateToStore(Long assetId, Long storeId, String storeName, String operatorName);
    
    void startRepair(Long assetId, String operatorName);
    
    void completeRepair(Long assetId, String operatorName);
    
    void scrapAsset(Long assetId, String reason, String operatorName);
    
    String generateAssetCode(String assetType);

    // ========== 同步设置 ==========

    /**
     * 获取同步设置
     *
     * @return 同步设置
     */
    Map<String, Object> getSyncSettings();

    /**
     * 更新同步设置
     *
     * @param settings 同步设置数据
     * @return 操作结果
     */
    boolean updateSyncSettings(Map<String, Object> settings);

    /**
     * 同步资产数据
     *
     * @param assetIds 资产ID列表
     * @return 同步任务ID
     */
    String syncAssets(List<String> assetIds);

    /**
     * 获取同步日志
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页同步日志
     */
    Map<String, Object> getSyncLogs(int page, int size);

    // ========== 折旧 ==========

    /**
     * 获取折旧列表
     *
     * @param assetId 资产ID（可选）
     * @param page    页码
     * @param size    每页条数
     * @return 折旧列表
     */
    List<Map<String, Object>> getDepreciationList(Long assetId, int page, int size);

    /**
     * 计提折旧
     *
     * @param assetId 资产ID
     * @param method  折旧方法
     * @param months  折旧月数
     * @return 折旧计算结果
     */
    Map<String, Object> calculateDepreciation(Long assetId, String method, int months);

    // ========== 处置 ==========

    /**
     * 获取处置列表
     *
     * @param page 页码
     * @param size 每页条数
     * @return 处置列表
     */
    List<Map<String, Object>> getDisposalList(int page, int size);

    /**
     * 获取处置详情
     *
     * @param disposalId 处置单ID
     * @return 处置详情
     */
    Map<String, Object> getDisposalDetail(Long disposalId);

    /**
     * 创建处置申请
     *
     * @param assetId 资产ID
     * @param type    处置类型
     * @param reason  处置原因
     * @param handler 处理人
     * @return 处置单ID
     */
    Long createDisposal(Long assetId, String type, String reason, String handler);

    /**
     * 审批处置申请
     *
     * @param disposalId 处置单ID
     * @param approved   是否通过
     * @param comment    审批意见
     * @return 操作结果
     */
    boolean approveDisposal(Long disposalId, boolean approved, String comment);
}
