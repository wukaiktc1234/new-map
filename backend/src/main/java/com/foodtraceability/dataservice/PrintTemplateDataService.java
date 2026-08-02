package com.foodtraceability.dataservice;

import com.foodtraceability.dto.PrintTemplateVO;
import java.util.List;
import java.util.Map;

/**
 * 打印模板数据服务接口
 */
public interface PrintTemplateDataService {

    /**
     * 批量获取打印模板基本信息
     * @param templateIds 模板ID列表
     * @return 模板ID到基本信息的映射
     */
    Map<Long, PrintTemplateVO> batchGetTemplateBasicInfo(List<Long> templateIds);

    /**
     * 获取单个打印模板基本信息
     * @param templateId 模板ID
     * @return 打印模板基本信息
     */
    PrintTemplateVO getTemplateBasicInfo(Long templateId);

    /**
     * 清除指定模板的缓存
     * @param templateId 模板ID
     */
    void clearTemplateCache(Long templateId);
}
