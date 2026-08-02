package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.PrintFormatTemplate;
import com.foodtraceability.mapper.PrintFormatTemplateMapper;
import com.foodtraceability.service.PrintFormatTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 打印格式模板服务实现类
 */
@Service
public class PrintFormatTemplateServiceImpl extends ServiceImpl<PrintFormatTemplateMapper, PrintFormatTemplate> implements PrintFormatTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PrintFormatTemplateServiceImpl.class);


    public PrintFormatTemplateServiceImpl(PrintFormatTemplateMapper printFormatTemplateMapper) {
        this.printFormatTemplateMapper = printFormatTemplateMapper;
    }

    private final PrintFormatTemplateMapper printFormatTemplateMapper;

    /**
     * 根据模板类型和设备类型查询模板列表
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 模板列表
     */
    @Override
    public List<PrintFormatTemplate> getTemplatesByTypeAndDevice(String templateType, String deviceType, Long storeId) {
        log.info("查询打印格式模板列表: 模板类型={}, 设备类型={}, 门店ID={}", templateType, deviceType, storeId);
        return printFormatTemplateMapper.selectByTypeAndDevice(templateType, deviceType, storeId);
    }

    /**
     * 查询默认模板
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 默认模板
     */
    @Override
    public PrintFormatTemplate getDefaultTemplate(String templateType, String deviceType, Long storeId) {
        log.info("查询默认打印格式模板: 模板类型={}, 设备类型={}, 门店ID={}", templateType, deviceType, storeId);
        PrintFormatTemplate defaultTemplate = printFormatTemplateMapper.selectDefaultTemplate(templateType, deviceType, storeId);
        
        // 如果没有默认模板，返回第一个启用的模板
        if (defaultTemplate == null) {
            List<PrintFormatTemplate> templates = printFormatTemplateMapper.selectByTypeAndDevice(templateType, deviceType, storeId);
            if (!templates.isEmpty()) {
                for (PrintFormatTemplate template : templates) {
                    if (template.getStatus() == 1) {
                        defaultTemplate = template;
                        break;
                    }
                }
            }
        }
        
        return defaultTemplate;
    }

    /**
     * 保存打印格式模板
     * @param template 打印格式模板
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrintFormatTemplate saveTemplate(PrintFormatTemplate template) {
        log.info("保存打印格式模板: 模板名称={}, 模板类型={}, 设备类型={}, 门店ID={}", 
                template.getTemplateName(), template.getTemplateType(), template.getDeviceType(), template.getStoreId());
        
        Date now = new Date();
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        
        // 如果设置为默认模板，先将其他模板的默认状态取消
        if (template.getIsDefault() != null && template.getIsDefault() == 1) {
            printFormatTemplateMapper.updateDefaultTemplateStatus(
                    template.getTemplateType(), template.getDeviceType(), template.getStoreId());
        }
        
        save(template);
        return template;
    }

    /**
     * 删除打印格式模板
     * @param id 模板ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long id) {
        log.info("删除打印格式模板: 模板ID={}", id);
        
        PrintFormatTemplate template = getById(id);
        if (template == null) {
            log.warn("打印格式模板不存在: 模板ID={}", id);
            return false;
        }
        
        // 逻辑删除模板
        template.setDeleted(1);
        template.setUpdatedAt(new Date());
        updateById(template);
        
        return true;
    }

    /**
     * 设置默认模板
     * @param id 模板ID
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 设置结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultTemplate(Long id, String templateType, String deviceType, Long storeId) {
        log.info("设置默认打印格式模板: 模板ID={}, 模板类型={}, 设备类型={}, 门店ID={}", 
                id, templateType, deviceType, storeId);
        
        PrintFormatTemplate template = getById(id);
        if (template == null) {
            log.warn("打印格式模板不存在: 模板ID={}", id);
            return false;
        }
        
        // 将所有同类型同设备的模板默认状态取消
        printFormatTemplateMapper.updateDefaultTemplateStatus(templateType, deviceType, storeId);
        
        // 设置当前模板为默认
        template.setIsDefault(1);
        template.setUpdatedAt(new Date());
        updateById(template);
        
        return true;
    }

    /**
     * 启用/禁用模板
     * @param id 模板ID
     * @param status 状态：0-禁用, 1-启用
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplateStatus(Long id, Integer status) {
        log.info("更新打印格式模板状态: 模板ID={}, 状态={}", id, status);
        
        PrintFormatTemplate template = getById(id);
        if (template == null) {
            log.warn("打印格式模板不存在: 模板ID={}", id);
            return false;
        }
        
        template.setStatus(status);
        template.setUpdatedAt(new Date());
        updateById(template);
        
        return true;
    }
}
