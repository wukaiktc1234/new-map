package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dto.PrintTemplateVO;
import com.foodtraceability.dataservice.PrintTemplateDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 打印模板数据服务 - Stub实现（兼容性适配）
 * 
 * @deprecated 此类为临时兼容性实现，请使用 device 子包中的新实现
 */
@Deprecated
@Service("legacyPrintTemplateDataServiceImpl")
public class PrintTemplateDataServiceImpl implements PrintTemplateDataService {
    
    private static final Logger log = LoggerFactory.getLogger(PrintTemplateDataServiceImpl.class);

    @Override
    public Map<Long, PrintTemplateVO> batchGetTemplateBasicInfo(List<Long> templateIds) {
        log.warn("[STUB] batchGetTemplateBasicInfo() called - returning empty map");
        return Collections.emptyMap();
    }

    @Override
    public PrintTemplateVO getTemplateBasicInfo(Long templateId) {
        log.warn("[STUB] getTemplateBasicInfo({}) called - returning null", templateId);
        return null;
    }

    @Override
    public void clearTemplateCache(Long templateId) {
        log.warn("[STUB] clearTemplateCache({}) called - no-op", templateId);
    }
}
