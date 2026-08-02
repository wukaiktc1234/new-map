package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.SummaryTemplate;

import java.util.List;

/**
 * 摘要模板Service接口
 * 管理凭证录入时的常用摘要模板
 */
public interface SummaryTemplateService extends IService<SummaryTemplate> {

    /**
     * 创建摘要模板
     * @param dto 创建DTO
     * @return 模板VO
     */
    SummaryTemplateVO create(SummaryTemplateCreateDTO dto);

    /**
     * 更新摘要模板
     * @param templateId 模板ID
     * @param dto 创建DTO（复用为更新DTO）
     * @return 是否成功
     */
    boolean update(Long templateId, SummaryTemplateCreateDTO dto);

    /**
     * 删除摘要模板
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean delete(Long templateId);

    /**
     * 分页查询摘要模板
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<SummaryTemplateVO> getPage(SummaryTemplateQueryDTO query);

    /**
     * 搜索摘要模板
     * @param keyword 关键词
     * @param limit 返回条数上限
     * @return 模板列表
     */
    List<SummaryTemplateVO> search(String keyword, int limit);

    /**
     * 增加使用次数
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean incrementUsageCount(Long templateId);
}
