package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.PrintTemplate;

import java.util.List;

public interface PrintTemplateService extends IService<PrintTemplate> {

    Result<PrintTemplate> createTemplate(PrintTemplate template);

    Result<PrintTemplate> updateTemplate(PrintTemplate template);

    Result<Void> deleteTemplate(Long templateId);

    Result<PrintTemplate> getTemplateById(Long templateId);

    Result<IPage<PrintTemplate>> getTemplatePage(Page<PrintTemplate> page, String templateType, Long storeId);

    Result<List<PrintTemplate>> getTemplateList(String templateType, Long storeId);

    Result<List<PrintTemplate>> getDefaultTemplates();

    Result<Void> setDefaultTemplate(Long templateId);

    Result<Void> enableTemplate(Long templateId);

    Result<Void> disableTemplate(Long templateId);
}
