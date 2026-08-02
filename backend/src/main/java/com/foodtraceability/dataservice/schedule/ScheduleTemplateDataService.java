package com.foodtraceability.dataservice.schedule;

import com.foodtraceability.dto.schedule.ScheduleTemplateVO;

import java.util.List;
import java.util.Map;

/**
 * 排班模板数据服务接口
 * 提供排班模板数据的缓存和批量查询功能
 */
public interface ScheduleTemplateDataService {

    /**
     * 批量获取排班模板基本信息
     * @param templateIds 模板ID列表
     * @return 模板ID到基本信息的映射
     */
    Map<String, ScheduleTemplateVO> batchGetTemplateBasicInfo(List<Long> templateIds);

    /**
     * 获取单个排班模板基本信息
     * @param templateId 模板ID
     * @return 模板基本信息
     */
    ScheduleTemplateVO getTemplateBasicInfo(Long templateId);

    /**
     * 获取门店所有模板(从缓存或数据库)
     * @param storeId 门店ID
     * @return 模板列表(按创建时间倒序)
     */
    List<ScheduleTemplateVO> getTemplatesByStore(Long storeId);

    /**
     * 获取门店的默认模板
     * @param storeId 门店ID
     * @return 默认模板(每个门店仅一个)
     */
    ScheduleTemplateVO getDefaultTemplateByStore(Long storeId);

    /**
     * 清除指定模板的缓存
     * @param templateId 模板ID
     */
    void clearTemplateCache(String templateId);

    /**
     * 批量清除模板缓存
     * @param templateIds 模板ID列表
     */
    void clearTemplateBatchCache(List<Long> templateIds);

    /**
     * 清除门店下所有模板的缓存
     * @param storeId 门店ID
     */
    void clearStoreTemplateCache(Long storeId);
}
