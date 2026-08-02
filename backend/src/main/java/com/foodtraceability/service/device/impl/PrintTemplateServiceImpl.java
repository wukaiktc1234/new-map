package com.foodtraceability.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dataservice.PrintTemplateDataService;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.PrintTemplate;
import com.foodtraceability.mapper.PrintTemplateMapper;
import com.foodtraceability.service.device.PrintTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 打印模板服务实现类
 */
@Service
public class PrintTemplateServiceImpl implements PrintTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PrintTemplateServiceImpl.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");

    private final PrintTemplateMapper printTemplateMapper;
    private final PrintTemplateDataService printTemplateDataService;

    public PrintTemplateServiceImpl(PrintTemplateMapper printTemplateMapper,
                                   PrintTemplateDataService printTemplateDataService) {
        this.printTemplateMapper = printTemplateMapper;
        this.printTemplateDataService = printTemplateDataService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PrintTemplateVO> createTemplate(PrintTemplateCreateDTO dto) {
        try {
            // 检查同门店下是否已存在同名模板
            LambdaQueryWrapper<PrintTemplate> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(PrintTemplate::getTemplateName, dto.getTemplateName())
                       .eq(dto.getStoreId() != null, PrintTemplate::getStoreId, dto.getStoreId())
                       .isNull(dto.getStoreId() == null, PrintTemplate::getStoreId);
            if (printTemplateMapper.selectCount(checkWrapper) > 0) {
                return Result.error("同名模板已存在");
            }

            PrintTemplate template = new PrintTemplate();
            BeanUtils.copyProperties(dto, template);
            template.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);

            // 如果设置为默认，先取消其他默认模板
            if (Boolean.TRUE.equals(template.getIsDefault())) {
                clearDefaultTemplate(dto.getTemplateType(), dto.getStoreId());
            }

            printTemplateMapper.insert(template);

            log.info("创建打印模板成功: templateName={}, type={}", dto.getTemplateName(), dto.getTemplateType());

            return Result.success(printTemplateDataService.getTemplateBasicInfo(template.getTemplateId()), "创建打印模板成功");
        } catch (Exception e) {
            log.error("创建打印模板失败", e);
            return Result.error("创建打印模板失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PrintTemplateVO> updateTemplate(Long templateId, PrintTemplateUpdateDTO dto) {
        try {
            PrintTemplate existing = printTemplateMapper.selectById(templateId);
            if (existing == null) {
                return Result.error("模板不存在");
            }

            // 如果修改了名称，检查是否重复
            if (dto.getTemplateName() != null && !dto.getTemplateName().equals(existing.getTemplateName())) {
                LambdaQueryWrapper<PrintTemplate> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(PrintTemplate::getTemplateName, dto.getTemplateName())
                           .ne(PrintTemplate::getTemplateId, templateId)
                           .eq(existing.getStoreId() != null, PrintTemplate::getStoreId, existing.getStoreId())
                           .isNull(existing.getStoreId() == null, PrintTemplate::getStoreId);
                if (printTemplateMapper.selectCount(checkWrapper) > 0) {
                    return Result.error("同名模板已存在");
                }
            }

            PrintTemplate template = new PrintTemplate();
            BeanUtils.copyProperties(dto, template);
            template.setTemplateId(templateId);

            // 如果设置为默认，先取消其他默认模板
            if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
                clearDefaultTemplate(existing.getTemplateType(), existing.getStoreId());
            }

            printTemplateMapper.updateById(template);

            // 清除缓存
            printTemplateDataService.clearTemplateCache(templateId);

            log.info("更新打印模板成功: templateId={}", templateId);
            return Result.success(printTemplateDataService.getTemplateBasicInfo(templateId), "更新打印模板成功");
        } catch (Exception e) {
            log.error("更新打印模板失败: templateId={}", templateId, e);
            return Result.error("更新打印模板失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteTemplate(Long templateId) {
        try {
            int result = printTemplateMapper.deleteById(templateId);
            if (result > 0) {
                // 清除缓存
                printTemplateDataService.clearTemplateCache(templateId);
                log.info("删除打印模板成功: templateId={}", templateId);
                return Result.success(null, "删除打印模板成功");
            }
            return Result.error("模板不存在");
        } catch (Exception e) {
            log.error("删除打印模板失败: templateId={}", templateId, e);
            return Result.error("删除打印模板失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PrintTemplateVO> getTemplateById(Long templateId) {
        try {
            PrintTemplateVO vo = printTemplateDataService.getTemplateBasicInfo(templateId);
            if (vo == null) {
                return Result.error("模板不存在");
            }
            return Result.success(vo, "查询打印模板成功");
        } catch (Exception e) {
            log.error("查询打印模板失败: templateId={}", templateId, e);
            return Result.error("查询打印模板失败：" + e.getMessage());
        }
    }

    @Override
    public Result<IPage<PrintTemplateVO>> getTemplatePage(Page<?> page, Integer templateType, Long storeId) {
        try {
            LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();

            if (templateType != null) {
                wrapper.eq(PrintTemplate::getTemplateType, templateType);
            }
            if (storeId != null) {
                wrapper.and(w -> w.eq(PrintTemplate::getStoreId, storeId).isNull(PrintTemplate::getStoreId));
            }

            wrapper.orderByDesc(PrintTemplate::getIsDefault)
                   .orderByDesc(PrintTemplate::getCreateTime);

            @SuppressWarnings("unchecked")
            Page<PrintTemplate> templatePage = (Page<PrintTemplate>) page;
            IPage<PrintTemplate> resultPage = printTemplateMapper.selectPage(templatePage, wrapper);

            IPage<PrintTemplateVO> voPage = resultPage.convert(t ->
                    printTemplateDataService.getTemplateBasicInfo(t.getTemplateId()));

            return Result.success(voPage, "查询打印模板列表成功");
        } catch (Exception e) {
            log.error("分页查询打印模板失败", e);
            return Result.error("查询打印模板列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PrintTemplateVO>> getEnabledTemplates(Integer templateType, Long storeId) {
        try {
            LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();

            if (templateType != null) {
                wrapper.eq(PrintTemplate::getTemplateType, templateType);
            }
            if (storeId != null) {
                wrapper.and(w -> w.eq(PrintTemplate::getStoreId, storeId).isNull(PrintTemplate::getStoreId));
            }

            wrapper.orderByDesc(PrintTemplate::getIsDefault)
                   .orderByAsc(PrintTemplate::getTemplateName);

            List<PrintTemplate> templates = printTemplateMapper.selectList(wrapper);
            List<PrintTemplateVO> voList = templates.stream()
                    .map(t -> printTemplateDataService.getTemplateBasicInfo(t.getTemplateId()))
                    .collect(Collectors.toList());

            return Result.success(voList, "查询启用模板成功");
        } catch (Exception e) {
            log.error("查询启用模板失败", e);
            return Result.error("查询启用模板失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PrintTemplateVO> getDefaultTemplate(Integer templateType, Long storeId) {
        try {
            LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PrintTemplate::getIsDefault, true)
                   .eq(templateType != null, PrintTemplate::getTemplateType, templateType)
                   .and(storeId != null,
                           w -> w.eq(PrintTemplate::getStoreId, storeId).isNull(PrintTemplate::getStoreId))
                   .last("LIMIT 1");

            PrintTemplate template = printTemplateMapper.selectOne(wrapper);
            if (template == null) {
                return Result.error("未找到默认模板");
            }

            return Result.success(printTemplateDataService.getTemplateBasicInfo(template.getTemplateId()),
                    "查询默认模板成功");
        } catch (Exception e) {
            log.error("查询默认模板失败", e);
            return Result.error("查询默认模板失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setAsDefault(Long templateId) {
        try {
            PrintTemplate template = printTemplateMapper.selectById(templateId);
            if (template == null) {
                return Result.error("模板不存在");
            }

            // 先取消其他默认模板
            clearDefaultTemplate(template.getTemplateType(), template.getStoreId());

            // 设置当前模板为默认
            template.setIsDefault(true);
            template.setUpdateTime(LocalDateTime.now());
            printTemplateMapper.updateById(template);

            // 清除缓存
            printTemplateDataService.clearTemplateCache(templateId);

            log.info("设置默认模板成功: templateId={}, templateName={}",
                    templateId, template.getTemplateName());

            return Result.success(null, "设置默认模板成功");
        } catch (Exception e) {
            log.error("设置默认模板失败: templateId={}", templateId, e);
            return Result.error("设置默认模板失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> renderTemplate(Long templateId, Map<String, Object> variables) {
        try {
            PrintTemplate template = printTemplateMapper.selectById(templateId);
            if (template == null) {
                return Result.error("模板不存在");
            }

            String content = template.getTemplateContent();

            // 替换变量占位符 ${variableName}
            Matcher matcher = VARIABLE_PATTERN.matcher(content);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String varName = matcher.group(1);
                Object value = variables.get(varName);
                String replacement = value != null ? value.toString() : "";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);

            log.info("模板渲染成功: templateId={}, 变量数量={}", templateId, variables.size());
            return Result.success(sb.toString(), "模板渲染成功");
        } catch (Exception e) {
            log.error("模板渲染失败: templateId={}", templateId, e);
            return Result.error("模板渲染失败：" + e.getMessage());
        }
    }

    /**
     * 清除指定类型和门店的默认模板标记
     */
    private void clearDefaultTemplate(Integer templateType, Long storeId) {
        try {
            LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PrintTemplate::getIsDefault, true)
                   .eq(templateType != null, PrintTemplate::getTemplateType, templateType)
                   .eq(storeId != null, PrintTemplate::getStoreId, storeId)
                   .isNull(storeId == null, PrintTemplate::getStoreId);

            List<PrintTemplate> defaultTemplates = printTemplateMapper.selectList(wrapper);
            for (PrintTemplate t : defaultTemplates) {
                t.setIsDefault(false);
                t.setUpdateTime(LocalDateTime.now());
                printTemplateMapper.updateById(t);
                printTemplateDataService.clearTemplateCache(t.getTemplateId());
            }
        } catch (Exception e) {
            log.error("清除默认模板标记失败", e);
        }
    }
}
