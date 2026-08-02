package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.RateLimit;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.LabelElementDTO;
import com.foodtraceability.dto.LabelLayoutConfigDTO;
import com.foodtraceability.dto.LabelPrintConfigDTO;
import com.foodtraceability.driver.TsplLabelPrinterDriver;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.LabelTemplate;
import com.foodtraceability.mapper.LabelTemplateMapper;
import com.foodtraceability.service.LabelPreviewService;
import com.foodtraceability.service.LabelPrintService;
import com.foodtraceability.service.TsplGeneratorService;
import com.foodtraceability.util.TsplFontMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 标签模板控制器
 * 提供标签设计、预览、打印等功能
 */
@RestController
@RequestMapping("/v1/label-template")
@Tag(name = "标签模板管理", description = "标签模板的增删改查、预览和打印")
@PreAuthorize("hasRole('USER')")
public class LabelTemplateController {

    private static final Logger log = LoggerFactory.getLogger(LabelTemplateController.class);


    public LabelTemplateController(TsplGeneratorService tsplGeneratorService, LabelPreviewService labelPreviewService, LabelPrintService labelPrintService, ObjectMapper objectMapper, LabelTemplateMapper labelTemplateMapper, TsplLabelPrinterDriver tsplDriver) {
        this.tsplGeneratorService = tsplGeneratorService;
        this.labelPreviewService = labelPreviewService;
        this.labelPrintService = labelPrintService;
        this.objectMapper = objectMapper;
        this.labelTemplateMapper = labelTemplateMapper;
        this.tsplDriver = tsplDriver;
    }

    private final TsplGeneratorService tsplGeneratorService;

    private final LabelPreviewService labelPreviewService;

    private final LabelPrintService labelPrintService;

    private final ObjectMapper objectMapper;

    private final LabelTemplateMapper labelTemplateMapper;
    
    private final TsplLabelPrinterDriver tsplDriver;

    /**
     * 获取模板列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取标签模板列表")
    @PreAuthorize("hasAuthority('label:template:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getTemplateList(
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Boolean enabled) {
        try {
            LambdaQueryWrapper<LabelTemplate> wrapper = new LambdaQueryWrapper<>();
            if (templateType != null && !templateType.isEmpty()) {
                wrapper.eq(LabelTemplate::getTemplateType, templateType);
            }
            if (enabled != null) {
                wrapper.eq(LabelTemplate::getEnabled, enabled);
            }
            wrapper.orderByDesc(LabelTemplate::getIsDefault).orderByDesc(LabelTemplate::getUpdatedAt);
            
            List<LabelTemplate> templates = labelTemplateMapper.selectList(wrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", templates);
            result.put("totalElements", templates.size());
            result.put("totalPages", 1);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取模板列表失败: {}", e.getMessage(), e);
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取标签模板详情")
    @PreAuthorize("hasAuthority('label:template:view')")
    public Result<LabelTemplate> getTemplate(@PathVariable String id) {
        try {
            if (!isValidUUID(id)) {
                return Result.error("无效的模板ID格式");
            }
            LabelTemplate template = labelTemplateMapper.selectById(id);
            if (template == null) {
                return Result.error("模板不存在");
            }
            return Result.success(template);
        } catch (Exception e) {
            log.error("获取模板详情失败: {}", e.getMessage(), e);
            return Result.error("获取模板详情失败");
        }
    }

    /**
     * 验证UUID格式
     */
    private boolean isValidUUID(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return id.matches(uuidPattern) || id.startsWith("template-") || id.startsWith("default-");
    }

    /**
     * 创建模板
     */
    @PostMapping
    @Operation(summary = "创建标签模板")
    @PreAuthorize("hasAuthority('label:template:create') or hasAuthority('*')")
    @AuditLog(value = "创建标签模板", module = "标签管理")
    @RateLimit(value = 10, time = 60)
    public Result<LabelTemplate> createTemplate(@RequestBody LabelTemplate template) {
        try {
            if (!isValidTemplateName(template.getTemplateName())) {
                return Result.error("模板名称格式不正确");
            }
            
            if (template.getLabelWidth() != null && (template.getLabelWidth() < 20 || template.getLabelWidth() > 100)) {
                return Result.error("标签宽度必须在20-100mm之间");
            }
            
            if (template.getLabelHeight() != null && (template.getLabelHeight() < 20 || template.getLabelHeight() > 100)) {
                return Result.error("标签高度必须在20-100mm之间");
            }
            
            String templateId = template.getId();
            if (templateId != null && !templateId.isEmpty()) {
                LabelTemplate existing = labelTemplateMapper.selectById(templateId);
                if (existing != null) {
                    templateId = UUID.randomUUID().toString();
                    log.warn("模板ID已存在，生成新ID: {} -> {}", template.getId(), templateId);
                }
            } else {
                templateId = UUID.randomUUID().toString();
            }
            
            template.setId(templateId);
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            if (template.getEnabled() == null) {
                template.setEnabled(true);
            }
            if (template.getIsDefault() == null) {
                template.setIsDefault(false);
            }
            if (template.getSize() == null && template.getLabelWidth() != null && template.getLabelHeight() != null) {
                Map<String, Object> size = new HashMap<>();
                size.put("width", template.getLabelWidth());
                size.put("height", template.getLabelHeight());
                template.setSize(size);
            }
            // 确保backgroundColor字段存在
            if (template.getBackgroundColor() == null) {
                template.setBackgroundColor("#FFFFFF");
            }
            
            labelTemplateMapper.insert(template);
            log.info("创建模板成功: {}", templateId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("创建模板失败: {}", e.getMessage(), e);
            return Result.error("创建模板失败");
        }
    }

    /**
     * 验证模板名称
     */
    private boolean isValidTemplateName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() > 50) {
            return false;
        }
        String pattern = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$";
        return name.matches(pattern);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新标签模板")
    @PreAuthorize("hasAuthority('label:template:update') or hasAuthority('*')")
    @AuditLog(value = "更新标签模板", module = "标签管理")
    public Result<LabelTemplate> updateTemplate(@PathVariable String id, @RequestBody LabelTemplate template) {
        try {
            log.info("更新模板请求: id={}, isDefault={}, templateType={}", id, template.getIsDefault(), template.getTemplateType());
            
            LabelTemplate existing = labelTemplateMapper.selectById(id);
            if (existing == null) {
                return Result.error("模板不存在");
            }
            
            if (template.getVersion() != null && !Objects.equals(existing.getVersion(), template.getVersion())) {
                return Result.error("模板已被其他用户修改,请刷新后重试");
            }
            
            // 处理默认模板设置
            if (Boolean.TRUE.equals(template.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
                String templateType = template.getTemplateType() != null ? template.getTemplateType() : existing.getTemplateType();
                log.info("设置默认模板: templateType={}", templateType);
                LambdaQueryWrapper<LabelTemplate> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(LabelTemplate::getTemplateType, templateType);
                wrapper.eq(LabelTemplate::getIsDefault, true);
                List<LabelTemplate> defaults = labelTemplateMapper.selectList(wrapper);
                log.info("找到{}个同类型默认模板", defaults.size());
                for (LabelTemplate t : defaults) {
                    log.info("清除模板{}的默认状态", t.getId());
                    t.setIsDefault(false);
                    labelTemplateMapper.updateById(t);
                }
            }
            
            template.setId(id);
            template.setUpdatedAt(LocalDateTime.now());
            template.setCreatedAt(existing.getCreatedAt());
            template.setCreatedBy(existing.getCreatedBy());
            
            if (template.getSize() == null && template.getLabelWidth() != null && template.getLabelHeight() != null) {
                Map<String, Object> size = new HashMap<>();
                size.put("width", template.getLabelWidth());
                size.put("height", template.getLabelHeight());
                template.setSize(size);
            }
            if (template.getBackgroundColor() == null) {
                template.setBackgroundColor("#FFFFFF");
            }
            
            int updateCount = labelTemplateMapper.updateById(template);
            if (updateCount == 0) {
                return Result.error("更新失败,模板可能已被其他用户修改");
            }
            
            // 重新查询数据库获取最新数据（包括version等自动更新的字段）
            LabelTemplate updated = labelTemplateMapper.selectById(id);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新模板失败: {}", e.getMessage(), e);
            return Result.error("更新模板失败");
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签模板")
    @PreAuthorize("hasAuthority('label:template:delete') or hasAuthority('*')")
    @AuditLog(value = "删除标签模板", module = "标签管理")
    public Result<Void> deleteTemplate(@PathVariable String id) {
        try {
            if (!isValidUUID(id)) {
                return Result.error("无效的模板ID格式");
            }
            LabelTemplate existing = labelTemplateMapper.selectById(id);
            if (existing == null) {
                return Result.error("模板不存在");
            }
            if (Boolean.TRUE.equals(existing.getIsDefault())) {
                return Result.error("不能删除默认模板");
            }
            
            labelTemplateMapper.deleteById(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除模板失败: {}", e.getMessage(), e);
            return Result.error("删除模板失败: " + e.getMessage());
        }
    }

    /**
     * 设为默认模板
     */
    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认模板")
    public Result<Void> setDefaultTemplate(@PathVariable String id) {
        try {
            LabelTemplate template = labelTemplateMapper.selectById(id);
            if (template == null) {
                return Result.error("模板不存在");
            }
            
            // 清除其他默认模板
            LambdaQueryWrapper<LabelTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LabelTemplate::getTemplateType, template.getTemplateType());
            wrapper.eq(LabelTemplate::getIsDefault, true);
            List<LabelTemplate> defaults = labelTemplateMapper.selectList(wrapper);
            for (LabelTemplate t : defaults) {
                t.setIsDefault(false);
                labelTemplateMapper.updateById(t);
            }
            
            // 设置当前模板为默认
            template.setIsDefault(true);
            labelTemplateMapper.updateById(template);
            
            return Result.success(null);
        } catch (Exception e) {
            log.error("设置默认模板失败: {}", e.getMessage(), e);
            return Result.error("设置默认模板失败: " + e.getMessage());
        }
    }

    /**
     * 生成标签预览HTML
     */
    @PostMapping("/preview")
    @Operation(summary = "生成标签预览HTML")
    public Result<String> generatePreview(@RequestBody Map<String, Object> request) {
        try {
            LabelLayoutConfigDTO layoutConfig = extractLayoutConfig(request);
            Map<String, Object> labelData = extractLabelData(request);

            String html = labelPreviewService.generateHtmlPreview(layoutConfig, labelData);
            return Result.success(html);

        } catch (Exception e) {
            log.error("生成预览失败: {}", e.getMessage(), e);
            return Result.error("生成预览失败: " + e.getMessage());
        }
    }

    /**
     * 生成标签预览数据（用于Canvas渲染）
     */
    @PostMapping("/preview-data")
    @Operation(summary = "生成标签预览数据")
    public Result<LabelPreviewService.PreviewData> generatePreviewData(@RequestBody Map<String, Object> request) {
        try {
            LabelLayoutConfigDTO layoutConfig = extractLayoutConfig(request);
            Map<String, Object> labelData = extractLabelData(request);

            LabelPreviewService.PreviewData previewData =
                labelPreviewService.generatePreviewData(layoutConfig, labelData);
            return Result.success(previewData);

        } catch (Exception e) {
            log.error("生成预览数据失败: {}", e.getMessage(), e);
            return Result.error("生成预览数据失败: " + e.getMessage());
        }
    }

    /**
     * 基于模板ID生成预览图片
     */
    @GetMapping(value = "/{id}/preview", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "基于模板生成预览图片")
    public byte[] generateTemplatePreview(
            @PathVariable String id,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String traceCode,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String inboundDate,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String shelfLife,
            @RequestParam(required = false) String supplierName) {
        try {
            log.info("开始生成预览: templateId={}", id);
            LabelTemplate template = labelTemplateMapper.selectById(id);
            if (template == null) {
                log.warn("模板不存在: {}", id);
                return generateErrorImage("模板不存在");
            }
            
            log.info("模板信息: name={}, width={}, height={}, elements={}", 
                template.getName(), template.getLabelWidth(), template.getLabelHeight(), 
                template.getElements() != null ? template.getElements().size() : 0);
            
            Map<String, Object> labelData = new HashMap<>();
            labelData.put("materialName", materialName != null ? materialName : "测试物料");
            labelData.put("traceCode", traceCode != null ? traceCode : "TC" + System.currentTimeMillis());
            labelData.put("storeName", storeName != null ? storeName : "测试门店");
            labelData.put("inboundDate", inboundDate != null ? inboundDate : "2026-03-06");
            labelData.put("expiryDate", expiryDate != null ? expiryDate : "2026-12-31");
            labelData.put("shelfLife", shelfLife != null ? shelfLife : "365天");
            labelData.put("supplierName", supplierName != null ? supplierName : "测试供应商");
            
            List<Map<String, Object>> labels = new ArrayList<>();
            labels.add(labelData);
            
            byte[] result = generatePreviewImageWithTemplate(labels, template);
            log.info("预览生成成功, 图片大小: {} bytes", result.length);
            return result;
            
        } catch (Exception e) {
            log.error("生成模板预览失败: {}", e.getMessage(), e);
            return generateErrorImage("生成预览失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成错误提示图片
     */
    private byte[] generateErrorImage(String message) {
        try {
            int width = 300;
            int height = 200;
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = image.createGraphics();
            
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            
            g.setColor(java.awt.Color.RED);
            g.setFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 14));
            g.drawString(message, 20, 100);
            
            g.dispose();
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    /**
     * 使用模板生成预览图像
     */
    private byte[] generatePreviewImageWithTemplate(List<Map<String, Object>> labels, LabelTemplate template) throws Exception {
        int labelWidthMm = template.getLabelWidth() != null ? template.getLabelWidth() : 40;
        int labelHeightMm = template.getLabelHeight() != null ? template.getLabelHeight() : 30;
        
        List<Map<String, Object>> elements = template.getElements();
        if (elements == null) {
            elements = new ArrayList<>();
        }
        
        log.info("模板 {} 尺寸: {}x{}mm, 元素数量: {}", template.getId(), labelWidthMm, labelHeightMm, elements.size());
        
        return generatePreviewImageWithElements(labels, labelWidthMm, labelHeightMm, elements);
    }
    
    /**
     * 使用模板元素生成预览图像
     */
    private byte[] generatePreviewImageWithElements(List<Map<String, Object>> labels, int labelWidthMm, int labelHeightMm, List<Map<String, Object>> elements) throws Exception {
        int dpi = 300;
        double mmToInch = 25.4;
        
        int labelWidthPx = (int) (labelWidthMm / mmToInch * dpi);
        int labelHeightPx = (int) (labelHeightMm / mmToInch * dpi);
        
        int gapPx = 20;
        
        int rows = labels.size();
        
        int imageWidth = labelWidthPx + 2 * gapPx;
        int imageHeight = rows * labelHeightPx + (rows + 1) * gapPx;
        
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            imageWidth, imageHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        
        for (int i = 0; i < rows; i++) {
            int x = gapPx;
            int y = gapPx + i * (labelHeightPx + gapPx);
            
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(x, y, labelWidthPx, labelHeightPx);
            
            g.setColor(java.awt.Color.BLACK);
            g.drawRect(x, y, labelWidthPx, labelHeightPx);
            
            Map<String, Object> labelData = labels.get(i);
            
            for (Map<String, Object> element : elements) {
                String type = (String) element.get("type");
                if (type == null) continue;
                
                Double elemX = getDouble(element, "x", 0.0);
                Double elemY = getDouble(element, "y", 0.0);
                Double elemWidth = getDouble(element, "width", 10.0);
                Double elemHeight = getDouble(element, "height", 10.0);
                
                int elemXpx = x + (int) (elemX / mmToInch * dpi);
                int elemYpx = y + (int) (elemY / mmToInch * dpi);
                int elemWidthPx = (int) (elemWidth / mmToInch * dpi);
                int elemHeightPx = (int) (elemHeight / mmToInch * dpi);
                
                switch (type) {
                    case "text":
                        renderTextElement(g, element, labelData, elemXpx, elemYpx, elemWidthPx, elemHeightPx);
                        break;
                    case "qrcode":
                        renderQrCodeElement(g, element, labelData, elemXpx, elemYpx, elemWidthPx, elemHeightPx);
                        break;
                    case "barcode":
                        renderBarcodeElement(g, element, labelData, elemXpx, elemYpx, elemWidthPx, elemHeightPx);
                        break;
                    case "line":
                        renderLineElement(g, element, elemXpx, elemYpx, elemWidthPx);
                        break;
                    case "rect":
                        renderRectElement(g, element, elemXpx, elemYpx, elemWidthPx, elemHeightPx);
                        break;
                }
            }
        }
        
        g.dispose();
        
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
    
    private void renderTextElement(java.awt.Graphics2D g, Map<String, Object> element, Map<String, Object> labelData, 
                                    int x, int y, int width, int height) {
        String textFieldType = (String) element.get("textFieldType");
        String customText = (String) element.get("customText");
        String fontFamily = (String) element.get("fontFamily");
        if (fontFamily == null) fontFamily = "SimHei";
        
        Integer fontSize = getInteger(element, "fontSize", 10);
        String fontColor = (String) element.get("fontColor");
        if (fontColor == null) fontColor = "#000000";
        
        Map<String, Boolean> fontStyle = (Map<String, Boolean>) element.get("fontStyle");
        boolean bold = fontStyle != null && Boolean.TRUE.equals(fontStyle.get("bold"));
        
        String text = "";
        String prefix = (String) element.get("prefix");
        if (prefix == null) prefix = "";
        
        if ("custom".equals(textFieldType)) {
            text = customText != null ? customText : "";
        } else if (textFieldType != null) {
            Object value = labelData.get(textFieldType);
            if (value == null) {
                switch (textFieldType) {
                    case "inboundDate":
                        value = labelData.get("generateTime");
                        if (value == null) value = labelData.get("inboundDate");
                        if (value == null) value = labelData.get("createTime");
                        if (value != null) {
                            text = formatDateOnly(value.toString());
                        }
                        break;
                    case "expiryDate":
                        value = labelData.get("expiryDate");
                        if (value != null) {
                            text = formatDateOnly(value.toString());
                        }
                        break;
                    case "shelfLife":
                        value = labelData.get("shelfLifeDays");
                        if (value == null) value = labelData.get("shelfLife");
                        if (value != null) {
                            text = value.toString() + "天";
                        }
                        break;
                    case "printDate":
                        text = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                        break;
                    case "materialName":
                        value = labelData.get("materialName");
                        if (value == null) value = labelData.get("productName");
                        break;
                    default:
                        value = labelData.get(textFieldType);
                }
            }
            if (text.isEmpty() && value != null) {
                text = value.toString();
            }
        }
        
        if (text.isEmpty()) return;
        
        // 添加前缀
        text = prefix + text;
        
        // TSPL字号映射：与打印一致
        // 使用yScale实现不同高度，避免"加粗"效果
        int actualFontSize;
        if (fontSize <= 24) {
            actualFontSize = 24;
        } else if (fontSize <= 36) {
            actualFontSize = 48;
        } else if (fontSize <= 48) {
            actualFontSize = 48;
        } else if (fontSize <= 60) {
            actualFontSize = 72;
        } else {
            actualFontSize = 72;
        }
        
        int style = bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN;
        g.setFont(new java.awt.Font(fontFamily, style, actualFontSize));
        
        java.awt.Color color = parseColor(fontColor);
        g.setColor(color != null ? color : java.awt.Color.BLACK);
        
        // 计算文本宽度
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        
        // 如果文本超出宽度，截断
        if (textWidth > width && width > 0) {
            while (textWidth > width && text.length() > 0) {
                text = text.substring(0, text.length() - 1);
                textWidth = fm.stringWidth(text);
            }
        }
        
        g.drawString(text, x, y + actualFontSize);
    }
    
    private String formatDateOnly(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            // 尝试解析各种日期格式
            java.text.SimpleDateFormat[] inputFormats = {
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                new java.text.SimpleDateFormat("yyyy-MM-dd")
            };
            for (java.text.SimpleDateFormat format : inputFormats) {
                try {
                    java.util.Date date = format.parse(dateStr);
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
                } catch (Exception ignored) {}
            }
            // 如果无法解析，返回原始字符串（只取日期部分）
            if (dateStr.length() >= 10) {
                return dateStr.substring(0, 10);
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private void renderQrCodeElement(java.awt.Graphics2D g, Map<String, Object> element, Map<String, Object> labelData,
                                      int x, int y, int width, int height) {
        String dataField = (String) element.get("dataField");
        if (dataField == null) dataField = "traceCode";
        
        Object value = labelData.get(dataField);
        String qrData = value != null ? value.toString() : "";
        
        if (qrData.isEmpty()) return;
        
        try {
            java.awt.Image qrImage = generateQrCodeImage(qrData, width, height);
            if (qrImage != null) {
                g.drawImage(qrImage, x, y, null);
            }
        } catch (Exception e) {
            log.warn("生成二维码失败: {}", e.getMessage());
        }
    }
    
    private void renderBarcodeElement(java.awt.Graphics2D g, Map<String, Object> element, Map<String, Object> labelData,
                                       int x, int y, int width, int height) {
        String dataField = (String) element.get("dataField");
        if (dataField == null) dataField = "traceCode";
        
        Object value = labelData.get(dataField);
        String barcodeData = value != null ? value.toString() : "";
        
        if (barcodeData.isEmpty()) return;
        
        int barHeight = (int) (height * 0.6);
        int barY = y;
        
        g.setColor(java.awt.Color.BLACK);
        for (int i = 0; i < barcodeData.length() && i * 3 < width; i++) {
            if (i % 2 == 0) {
                g.fillRect(x + i * 3, barY, 2, barHeight);
            }
        }
        
        Boolean showText = (Boolean) element.get("showText");
        if (Boolean.TRUE.equals(showText)) {
            g.setFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 10));
            g.drawString(barcodeData, x, y + barHeight + 12);
        }
    }
    
    private void renderLineElement(java.awt.Graphics2D g, Map<String, Object> element, int x, int y, int width) {
        Integer borderWidth = getInteger(element, "borderWidth", 1);
        String borderColor = (String) element.get("borderColor");
        if (borderColor == null) borderColor = "#000000";
        
        java.awt.Color color = parseColor(borderColor);
        g.setColor(color != null ? color : java.awt.Color.BLACK);
        g.fillRect(x, y, width, borderWidth);
    }
    
    private void renderRectElement(java.awt.Graphics2D g, Map<String, Object> element, int x, int y, int width, int height) {
        String fillColor = (String) element.get("fillColor");
        Integer borderWidth = getInteger(element, "borderWidth", 1);
        String borderColor = (String) element.get("borderColor");
        if (borderColor == null) borderColor = "#000000";
        
        if (fillColor != null && !fillColor.equals("transparent")) {
            java.awt.Color fill = parseColor(fillColor);
            if (fill != null) {
                g.setColor(fill);
                g.fillRect(x, y, width, height);
            }
        }
        
        java.awt.Color border = parseColor(borderColor);
        g.setColor(border != null ? border : java.awt.Color.BLACK);
        g.drawRect(x, y, width, height);
    }
    
    private java.awt.Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) return null;
        try {
            if (colorStr.startsWith("#")) {
                return java.awt.Color.decode(colorStr);
            }
            return java.awt.Color.decode("#" + colorStr);
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double getDouble(Map<String, Object> map, String key, Double defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private String truncateToWidth(java.awt.Graphics2D g, String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        if (textWidth <= maxWidth) return text;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String test = sb.toString() + text.charAt(i) + "...";
            if (fm.stringWidth(test) > maxWidth) {
                return sb.toString() + "...";
            }
            sb.append(text.charAt(i));
        }
        return text;
    }
    
    private java.awt.Image generateQrCodeImage(String data, int width, int height) {
        try {
            java.awt.image.BufferedImage qrImage = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = qrImage.createGraphics();
            
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            
            g.setColor(java.awt.Color.BLACK);
            int moduleSize = Math.min(width, height) / 25;
            int startX = (width - moduleSize * 21) / 2;
            int startY = (height - moduleSize * 21) / 2;
            
            // 绘制定位图案
            drawFinderPattern(g, startX, startY, moduleSize);
            drawFinderPattern(g, startX + moduleSize * 14, startY, moduleSize);
            drawFinderPattern(g, startX, startY + moduleSize * 14, moduleSize);
            
            // 绘制数据区域（简化版）
            java.util.Random random = new java.util.Random(data.hashCode());
            for (int row = 0; row < 21; row++) {
                for (int col = 0; col < 21; col++) {
                    // 跳过定位图案区域
                    if ((row < 7 && col < 7) || (row < 7 && col >= 14) || (row >= 14 && col < 7)) {
                        continue;
                    }
                    if (random.nextBoolean()) {
                        g.fillRect(startX + col * moduleSize, startY + row * moduleSize, moduleSize, moduleSize);
                    }
                }
            }
            
            g.dispose();
            return qrImage;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void drawFinderPattern(java.awt.Graphics2D g, int x, int y, int moduleSize) {
        // 外框
        g.fillRect(x, y, moduleSize * 7, moduleSize * 7);
        // 白色内框
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(x + moduleSize, y + moduleSize, moduleSize * 5, moduleSize * 5);
        // 黑色中心
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(x + moduleSize * 2, y + moduleSize * 2, moduleSize * 3, moduleSize * 3);
    }

    /**
     * 生成TSPL指令
     */
    @PostMapping("/generate-tspl")
    @Operation(summary = "生成TSPL打印指令")
    public Result<String> generateTspl(@RequestBody Map<String, Object> request) {
        try {
            LabelLayoutConfigDTO layoutConfig = extractLayoutConfig(request);
            Map<String, Object> labelData = extractLabelData(request);

            // 验证布局
            TsplGeneratorService.ValidationResult validation =
                tsplGeneratorService.validateLayout(layoutConfig);
            if (!validation.isValid()) {
                return Result.error("布局验证失败: " + validation.getMessage());
            }

            String tspl = tsplGeneratorService.generateTsplWithData(layoutConfig, labelData);
            log.info("生成TSPL指令成功，长度: {}", tspl.length());

            return Result.success(tspl);

        } catch (Exception e) {
            log.error("生成TSPL指令失败: {}", e.getMessage(), e);
            return Result.error("生成TSPL指令失败: " + e.getMessage());
        }
    }

    /**
     * 通过模板ID打印标签
     */
    @PostMapping("/{templateId}/print")
    @Operation(summary = "通过模板ID打印标签")
    @PreAuthorize("hasAuthority('label:template:print') or hasAuthority('*')")
    @AuditLog(value = "打印标签", module = "标签管理")
    public Result<Map<String, Object>> printByTemplateId(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取模板
            LabelTemplate template = labelTemplateMapper.selectById(templateId);
            if (template == null) {
                result.put("success", false);
                result.put("message", "模板不存在: " + templateId);
                return Result.success(result);
            }

            String printerName = (String) request.get("printerName");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> labelDataList = (List<Map<String, Object>>) request.get("labelDataList");
            if (labelDataList == null || labelDataList.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> labelData = (Map<String, Object>) request.get("labelData");
                if (labelData != null) {
                    labelDataList = List.of(labelData);
                }
            }
            
            if (labelDataList == null || labelDataList.isEmpty()) {
                result.put("success", false);
                result.put("message", "标签数据不能为空");
                return Result.success(result);
            }

            // 获取模板尺寸
            int labelWidthMm = template.getLabelWidth() != null ? template.getLabelWidth() : 40;
            int labelHeightMm = template.getLabelHeight() != null ? template.getLabelHeight() : 30;
            int dpi = 300;
            // 官方文档: 300 DPI 时 1 mm = 12 dot, 200 DPI 时 1 mm = 8 dot
            int labelWidthDots = labelWidthMm * 12;
            int labelHeightDots = labelHeightMm * 12;

            List<Map<String, Object>> elements = template.getElements();
            if (elements == null) {
                elements = new ArrayList<>();
            }

            log.info("模板 {} 尺寸: {}x{}mm, 元素数量: {}", templateId, labelWidthMm, labelHeightMm, elements.size());

            // 初始化打印机
            HardwareConfig printerConfig = new HardwareConfig();
            printerConfig.setDeviceType("LABEL_PRINTER");
            printerConfig.setDeviceName(printerName != null ? printerName : "Xprinter XP-D35E");
            printerConfig.setConnectionType("WINDOWS");

            if (!tsplDriver.init(printerConfig)) {
                result.put("success", false);
                result.put("message", "打印机初始化失败");
                return Result.success(result);
            }

            if (!tsplDriver.connect()) {
                result.put("success", false);
                result.put("message", "打印机连接失败");
                return Result.success(result);
            }

            int successCount = 0;
            int failCount = 0;
            double mmToDots = (dpi == 300) ? 12 : (dpi == 200 ? 8 : dpi / 25.4); // 官方文档: 300 DPI 时 1 mm = 12 dot, 200 DPI 时 1 mm = 8 dot

            for (Map<String, Object> labelData : labelDataList) {
                try {
                    String tspl = generateTsplFromTemplate(labelWidthDots, labelHeightDots, dpi, elements, labelData);
                    
                    Map<String, Object> printParams = new HashMap<>();
                    printParams.put("tsplData", tspl);
                    Map<String, Object> printResult = tsplDriver.executeOperation("PRINT_LABEL", printParams);
                    
                    if (Boolean.TRUE.equals(printResult.get("success"))) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("打印单个标签失败: {}", e.getMessage());
                    failCount++;
                }
            }

            result.put("success", failCount == 0);
            result.put("message", String.format("打印完成: 成功 %d, 失败 %d", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return Result.success(result);

        } catch (Exception e) {
            log.error("打印标签失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 打印标签
     */
    @PostMapping("/print")
    @Operation(summary = "打印标签")
    public Result<Map<String, Object>> printLabel(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            LabelLayoutConfigDTO layoutConfig = extractLayoutConfig(request);
            Map<String, Object> labelData = extractLabelData(request);
            String printerName = (String) request.get("printerName");

            // 验证布局
            TsplGeneratorService.ValidationResult validation =
                tsplGeneratorService.validateLayout(layoutConfig);
            if (!validation.isValid()) {
                result.put("success", false);
                result.put("message", "布局验证失败: " + validation.getMessage());
                return Result.success(result);
            }

            // 生成TSPL指令
            String tspl = tsplGeneratorService.generateTsplWithData(layoutConfig, labelData);

            // 执行打印
            boolean success = labelPrintService.printCustomLabel(tspl);

            result.put("success", success);
            result.put("message", success ? "标签打印成功" : "标签打印失败");
            return Result.success(result);

        } catch (Exception e) {
            log.error("打印标签失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 批量打印标签
     */
    @PostMapping("/batch-print")
    @Operation(summary = "批量打印标签")
    @PreAuthorize("hasAuthority('label:template:print')")
    @AuditLog(value = "批量打印标签", module = "标签管理")
    @RateLimit(value = 30, time = 60, message = "打印请求过于频繁,请稍后重试")
    public Result<Map<String, Object>> batchPrintLabels(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            LabelLayoutConfigDTO layoutConfig = extractLayoutConfig(request);
            String printerName = (String) request.get("printerName");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> labelDataList =
                (List<Map<String, Object>>) request.get("labelDataList");

            if (labelDataList == null || labelDataList.isEmpty()) {
                result.put("success", false);
                result.put("message", "标签数据列表不能为空");
                return Result.success(result);
            }

            final int MAX_BATCH_SIZE = 100;
            if (labelDataList.size() > MAX_BATCH_SIZE) {
                result.put("success", false);
                result.put("message", String.format("单次批量打印数量不能超过%d张，当前: %d", 
                    MAX_BATCH_SIZE, labelDataList.size()));
                return Result.success(result);
            }

            TsplGeneratorService.ValidationResult validation =
                tsplGeneratorService.validateLayout(layoutConfig);
            if (!validation.isValid()) {
                result.put("success", false);
                result.put("message", "布局验证失败: " + validation.getMessage());
                return Result.success(result);
            }

            int successCount = 0;
            int failCount = 0;
            int batchSize = 20;

            for (int i = 0; i < labelDataList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, labelDataList.size());
                List<Map<String, Object>> batch = labelDataList.subList(i, end);
                
                for (Map<String, Object> labelData : batch) {
                    String tspl = tsplGeneratorService.generateTsplWithData(layoutConfig, labelData);
                    boolean success = labelPrintService.printCustomLabel(tspl);

                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }

                    Thread.sleep(100);
                }
                
                if (i + batchSize < labelDataList.size()) {
                    Thread.sleep(500);
                }
            }

            result.put("success", failCount == 0);
            result.put("message", String.format("批量打印完成: 成功 %d, 失败 %d", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return Result.success(result);

        } catch (Exception e) {
            log.error("批量打印标签失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量打印失败: " + e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 验证布局配置
     */
    @PostMapping("/validate")
    @Operation(summary = "验证布局配置")
    public Result<Map<String, Object>> validateLayout(@RequestBody LabelLayoutConfigDTO layoutConfig) {
        Map<String, Object> result = new HashMap<>();

        try {
            TsplGeneratorService.ValidationResult validation =
                tsplGeneratorService.validateLayout(layoutConfig);

            result.put("valid", validation.isValid());
            result.put("message", validation.getMessage());
            if (validation.getElementId() != null) {
                result.put("elementId", validation.getElementId());
            }

            return Result.success(result);

        } catch (Exception e) {
            log.error("验证布局失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("message", "验证失败: " + e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 获取默认模板
     */
    @GetMapping("/default/{templateType}")
    @Operation(summary = "获取默认标签模板")
    public Result<LabelTemplate> getDefaultTemplate(@PathVariable String templateType) {
        try {
            LabelTemplate template = createDefaultTemplate(templateType);
            return Result.success(template);

        } catch (Exception e) {
            log.error("获取默认模板失败: {}", e.getMessage(), e);
            return Result.error("获取默认模板失败: " + e.getMessage());
        }
    }

    /**
     * 从请求中提取布局配置
     */
    private LabelLayoutConfigDTO extractLayoutConfig(Map<String, Object> request) {
        Object layoutConfigObj = request.get("layoutConfig");
        if (layoutConfigObj == null) {
            throw new IllegalArgumentException("布局配置不能为空");
        }

        try {
            String json = objectMapper.writeValueAsString(layoutConfigObj);
            return objectMapper.readValue(json, LabelLayoutConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("布局配置格式错误: " + e.getMessage());
        }
    }

    /**
     * 从请求中提取标签数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractLabelData(Map<String, Object> request) {
        Object labelDataObj = request.get("labelData");
        if (labelDataObj == null) {
            return new HashMap<>();
        }

        if (labelDataObj instanceof Map) {
            return (Map<String, Object>) labelDataObj;
        }

        try {
            String json = objectMapper.writeValueAsString(labelDataObj);
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("解析标签数据失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 创建默认模板
     */
    private LabelTemplate createDefaultTemplate(String templateType) {
        LabelTemplate template = new LabelTemplate();
        template.setTemplateName(getDefaultTemplateName(templateType));
        template.setTemplateCode("DEFAULT_" + templateType);
        template.setTemplateType(templateType);
        template.setLabelWidth(40);
        template.setLabelHeight(30);
        template.setDpi(300);
        template.setGapSize(2);
        template.setPrintSpeed(3);
        template.setPrintDensity(12);
        template.setDirection(1);
        template.setEnabled(true);
        template.setIsDefault(true);
        template.setSortOrder(0);

        // 生成默认布局配置
        LabelLayoutConfigDTO layoutConfig = createDefaultLayoutConfig(templateType);
        try {
            template.setLayoutConfig(objectMapper.convertValue(layoutConfig, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            log.error("序列化布局配置失败", e);
        }

        return template;
    }

    /**
     * 获取默认模板名称
     */
    private String getDefaultTemplateName(String templateType) {
        switch (templateType) {
            case "FOOD":
                return "食品标签模板";
            case "MATERIAL":
                return "原料标签模板";
            case "CUSTOM":
                return "自定义标签模板";
            default:
                return "默认标签模板";
        }
    }

    /**
     * 创建默认布局配置
     */
    private LabelLayoutConfigDTO createDefaultLayoutConfig(String templateType) {
        LabelLayoutConfigDTO config = new LabelLayoutConfigDTO();

        // 标签尺寸
        LabelLayoutConfigDTO.LabelSizeConfig size = new LabelLayoutConfigDTO.LabelSizeConfig();
        size.setWidth(40);
        size.setHeight(30);
        size.setDpi(300);
        size.setGap(2);
        config.setSize(size);

        // 打印配置
        LabelLayoutConfigDTO.PrintConfig print = new LabelLayoutConfigDTO.PrintConfig();
        print.setSpeed(3);
        print.setDensity(12);
        print.setDirection(1);
        print.setCopies(1);
        print.setPeelMode(false);
        config.setPrint(print);

        // 背景配置
        LabelLayoutConfigDTO.BackgroundConfig background = new LabelLayoutConfigDTO.BackgroundConfig();
        background.setColor("#FFFFFF");
        background.setBorderWidth(1);
        background.setBorderColor("#000000");
        config.setBackground(background);

        // 元素列表
        List<LabelElementDTO> elements = new java.util.ArrayList<>();

        if ("FOOD".equals(templateType)) {
            elements.addAll(createDefaultFoodElements());
        } else if ("MATERIAL".equals(templateType)) {
            elements.addAll(createDefaultMaterialElements());
        }

        config.setElements(elements);

        return config;
    }

    /**
     * 创建默认食品标签元素
     */
    private List<LabelElementDTO> createDefaultFoodElements() {
        List<LabelElementDTO> elements = new java.util.ArrayList<>();

        // 物料名称
        LabelElementDTO materialName = new LabelElementDTO();
        materialName.setId("material_name");
        materialName.setType(LabelElementDTO.ElementType.TEXT);
        materialName.setPosition(createPosition(15, 15));
        materialName.setText(createTextConfig(null, "TSS24.BF2", 2, 2));
        materialName.setDataBinding(createDataBinding("materialName", null, null, null, false));
        elements.add(materialName);

        // 门店名称
        LabelElementDTO storeName = new LabelElementDTO();
        storeName.setId("store_name");
        storeName.setType(LabelElementDTO.ElementType.TEXT);
        storeName.setPosition(createPosition(15, 55));
        storeName.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        storeName.setDataBinding(createDataBinding("storeName", "门店:", null, null, false));
        elements.add(storeName);

        // 保质期
        LabelElementDTO shelfLife = new LabelElementDTO();
        shelfLife.setId("shelf_life");
        shelfLife.setType(LabelElementDTO.ElementType.TEXT);
        shelfLife.setPosition(createPosition(15, 83));
        shelfLife.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        shelfLife.setDataBinding(createDataBinding("shelfLifeDays", "保质:", "天", null, false));
        elements.add(shelfLife);

        // 入库日期
        LabelElementDTO generateTime = new LabelElementDTO();
        generateTime.setId("generate_time");
        generateTime.setType(LabelElementDTO.ElementType.TEXT);
        generateTime.setPosition(createPosition(155, 83));
        generateTime.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        generateTime.setDataBinding(createDataBinding("generateTime", "入库:", null, "yyyy-MM-dd", false));
        elements.add(generateTime);

        // 到期日期
        LabelElementDTO expiryDate = new LabelElementDTO();
        expiryDate.setId("expiry_date");
        expiryDate.setType(LabelElementDTO.ElementType.TEXT);
        expiryDate.setPosition(createPosition(15, 111));
        expiryDate.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        expiryDate.setDataBinding(createDataBinding("expiryDate", "到期:", null, "yyyy-MM-dd", false));
        elements.add(expiryDate);

        // 供应商
        LabelElementDTO supplierName = new LabelElementDTO();
        supplierName.setId("supplier_name");
        supplierName.setType(LabelElementDTO.ElementType.TEXT);
        supplierName.setPosition(createPosition(155, 111));
        supplierName.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        supplierName.setDataBinding(createDataBinding("supplierName", "供应:", null, null, false));
        elements.add(supplierName);

        // 二维码
        LabelElementDTO qrcode = new LabelElementDTO();
        qrcode.setId("qrcode");
        qrcode.setType(LabelElementDTO.ElementType.QRCODE);
        qrcode.setPosition(createPosition(392, 15));
        qrcode.setQrcode(createQRCodeConfig(null, 4, "M", "A"));
        qrcode.setDataBinding(createDataBinding("traceCode", null, null, null, false));
        elements.add(qrcode);

        // 追溯码
        LabelElementDTO traceCode = new LabelElementDTO();
        traceCode.setId("trace_code");
        traceCode.setType(LabelElementDTO.ElementType.TEXT);
        traceCode.setPosition(createPosition(15, 315));
        traceCode.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        traceCode.setDataBinding(createDataBinding("traceCode", "码:", null, null, false));
        elements.add(traceCode);

        return elements;
    }

    /**
     * 创建默认原料标签元素
     */
    private List<LabelElementDTO> createDefaultMaterialElements() {
        List<LabelElementDTO> elements = new java.util.ArrayList<>();

        // 物料名称
        LabelElementDTO materialName = new LabelElementDTO();
        materialName.setId("material_name");
        materialName.setType(LabelElementDTO.ElementType.TEXT);
        materialName.setPosition(createPosition(15, 15));
        materialName.setText(createTextConfig(null, "TSS24.BF2", 2, 2));
        materialName.setDataBinding(createDataBinding("materialName", null, null, null, false));
        elements.add(materialName);

        // 生产日期
        LabelElementDTO productionDate = new LabelElementDTO();
        productionDate.setId("production_date");
        productionDate.setType(LabelElementDTO.ElementType.TEXT);
        productionDate.setPosition(createPosition(15, 55));
        productionDate.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        productionDate.setDataBinding(createDataBinding("productionDate", "生产:", null, "yyyy-MM-dd", false));
        elements.add(productionDate);

        // 到期日期
        LabelElementDTO expiryDate = new LabelElementDTO();
        expiryDate.setId("expiry_date");
        expiryDate.setType(LabelElementDTO.ElementType.TEXT);
        expiryDate.setPosition(createPosition(155, 55));
        expiryDate.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        expiryDate.setDataBinding(createDataBinding("expiryDate", "到期:", null, "MM-dd", false));
        elements.add(expiryDate);

        // 重量
        LabelElementDTO weight = new LabelElementDTO();
        weight.setId("weight");
        weight.setType(LabelElementDTO.ElementType.TEXT);
        weight.setPosition(createPosition(15, 83));
        weight.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        weight.setDataBinding(createDataBinding("weight", "重量:", "kg", null, false));
        elements.add(weight);

        // 条码
        LabelElementDTO barcode = new LabelElementDTO();
        barcode.setId("barcode");
        barcode.setType(LabelElementDTO.ElementType.BARCODE);
        barcode.setPosition(createPosition(15, 120));
        barcode.setBarcode(createBarcodeConfig(null, "128", 80, true));
        barcode.setDataBinding(createDataBinding("traceCode", null, null, null, false));
        elements.add(barcode);

        // 追溯码文本
        LabelElementDTO traceCode = new LabelElementDTO();
        traceCode.setId("trace_code");
        traceCode.setType(LabelElementDTO.ElementType.TEXT);
        traceCode.setPosition(createPosition(15, 220));
        traceCode.setText(createTextConfig(null, "TSS24.BF2", 1, 1));
        traceCode.setDataBinding(createDataBinding("traceCode", null, null, null, false));
        elements.add(traceCode);

        return elements;
    }

    // 辅助方法
    private LabelElementDTO.PositionConfig createPosition(int x, int y) {
        LabelElementDTO.PositionConfig pos = new LabelElementDTO.PositionConfig();
        pos.setX(x);
        pos.setY(y);
        pos.setRotation(0);
        pos.setAlign("left");
        return pos;
    }

    private LabelElementDTO.TextConfig createTextConfig(String content, String font,
                                                        Integer scaleX, Integer scaleY) {
        LabelElementDTO.TextConfig text = new LabelElementDTO.TextConfig();
        text.setContent(content);
        text.setFont(font);
        text.setScaleX(scaleX);
        text.setScaleY(scaleY);
        return text;
    }

    private LabelElementDTO.DataBindingConfig createDataBinding(String field, String prefix,
                                                                String suffix, String format,
                                                                Boolean hideIfEmpty) {
        LabelElementDTO.DataBindingConfig binding = new LabelElementDTO.DataBindingConfig();
        binding.setField(field);
        binding.setPrefix(prefix);
        binding.setSuffix(suffix);
        binding.setFormat(format);
        binding.setHideIfEmpty(hideIfEmpty);
        return binding;
    }

    private LabelElementDTO.QRCodeConfig createQRCodeConfig(String content, Integer moduleSize,
                                                            String errorLevel, String encodeMode) {
        LabelElementDTO.QRCodeConfig qrcode = new LabelElementDTO.QRCodeConfig();
        qrcode.setContent(content);
        qrcode.setModuleSize(moduleSize);
        qrcode.setErrorLevel(errorLevel);
        qrcode.setEncodeMode(encodeMode);
        return qrcode;
    }

    private LabelElementDTO.BarcodeConfig createBarcodeConfig(String content, String barcodeType,
                                                              Integer height, Boolean showText) {
        LabelElementDTO.BarcodeConfig barcode = new LabelElementDTO.BarcodeConfig();
        barcode.setContent(content);
        barcode.setBarcodeType(barcodeType);
        barcode.setHeight(height);
        barcode.setShowText(showText);
        return barcode;
    }

    /**
     * 获取默认食品标签模板
     */
    private Map<String, Object> getDefaultFoodTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("id", "default-food-template");
        template.put("name", "食品追溯标签");
        template.put("templateType", "FOOD");
        template.put("labelWidth", 40);
        template.put("labelHeight", 30);
        template.put("dpi", 300);
        template.put("enabled", true);
        template.put("createTime", java.time.LocalDateTime.now().toString());
        template.put("updateTime", java.time.LocalDateTime.now().toString());
        
        Map<String, Object> size = new HashMap<>();
        size.put("width", 40);
        size.put("height", 30);
        template.put("size", size);
        
        java.util.List<Map<String, Object>> elements = new java.util.ArrayList<>();
        
        Map<String, Object> materialName = new HashMap<>();
        materialName.put("id", "material_name");
        materialName.put("type", "text");
        materialName.put("x", 2);
        materialName.put("y", 2);
        materialName.put("width", 25);
        materialName.put("height", 6);
        materialName.put("fontSize", 16);
        materialName.put("fontWeight", "bold");
        materialName.put("dataField", "materialName");
        elements.add(materialName);
        
        Map<String, Object> qrcode = new HashMap<>();
        qrcode.put("id", "qrcode");
        qrcode.put("type", "qrcode");
        qrcode.put("x", 28);
        qrcode.put("y", 2);
        qrcode.put("width", 10);
        qrcode.put("height", 10);
        qrcode.put("dataField", "traceCode");
        elements.add(qrcode);
        
        Map<String, Object> storeName = new HashMap<>();
        storeName.put("id", "store_name");
        storeName.put("type", "text");
        storeName.put("x", 2);
        storeName.put("y", 10);
        storeName.put("width", 36);
        storeName.put("height", 4);
        storeName.put("fontSize", 12);
        storeName.put("dataField", "storeName");
        storeName.put("prefix", "门店:");
        elements.add(storeName);
        
        Map<String, Object> shelfLife = new HashMap<>();
        shelfLife.put("id", "shelf_life");
        shelfLife.put("type", "text");
        shelfLife.put("x", 2);
        shelfLife.put("y", 15);
        shelfLife.put("width", 18);
        shelfLife.put("height", 4);
        shelfLife.put("fontSize", 12);
        shelfLife.put("dataField", "shelfLifeDays");
        shelfLife.put("prefix", "保质:");
        shelfLife.put("suffix", "天");
        elements.add(shelfLife);
        
        Map<String, Object> expiryDate = new HashMap<>();
        expiryDate.put("id", "expiry_date");
        expiryDate.put("type", "text");
        expiryDate.put("x", 20);
        expiryDate.put("y", 15);
        expiryDate.put("width", 18);
        expiryDate.put("height", 4);
        expiryDate.put("fontSize", 12);
        expiryDate.put("dataField", "expiryDate");
        expiryDate.put("prefix", "到期:");
        elements.add(expiryDate);
        
        Map<String, Object> supplierName = new HashMap<>();
        supplierName.put("id", "supplier_name");
        supplierName.put("type", "text");
        supplierName.put("x", 2);
        supplierName.put("y", 20);
        supplierName.put("width", 36);
        supplierName.put("height", 4);
        supplierName.put("fontSize", 12);
        supplierName.put("dataField", "supplierName");
        supplierName.put("prefix", "供应:");
        elements.add(supplierName);
        
        Map<String, Object> traceCodeText = new HashMap<>();
        traceCodeText.put("id", "trace_code");
        traceCodeText.put("type", "text");
        traceCodeText.put("x", 2);
        traceCodeText.put("y", 25);
        traceCodeText.put("width", 36);
        traceCodeText.put("height", 4);
        traceCodeText.put("fontSize", 12);
        traceCodeText.put("dataField", "traceCode");
        elements.add(traceCodeText);
        
        template.put("elements", elements);
        
        return template;
    }
    
    private String generateTsplFromTemplate(int labelWidthDots, int labelHeightDots, int dpi,
                                              List<Map<String, Object>> elements, Map<String, Object> labelData) {
        StringBuilder tspl = new StringBuilder();
        
        int labelWidthMm = (dpi == 300) ? labelWidthDots / 12 : (dpi == 200 ? labelWidthDots / 8 : (int) Math.round(labelWidthDots * 25.4 / dpi));
        int labelHeightMm = (dpi == 300) ? labelHeightDots / 12 : (dpi == 200 ? labelHeightDots / 8 : (int) Math.round(labelHeightDots * 25.4 / dpi));
        
        tspl.append("SIZE ").append(labelWidthMm).append(" mm,").append(labelHeightMm).append(" mm\n");
        tspl.append("GAP 2 mm,0 mm\n");
        tspl.append("SPEED 3\n");
        tspl.append("DENSITY 12\n");
        tspl.append("DIRECTION 0\n");
        tspl.append("CLS\n");
        
        double mmToDots = (dpi == 300) ? 12 : (dpi == 200 ? 8 : dpi / 25.4); // 官方文档: 300 DPI 时 1 mm = 12 dot, 200 DPI 时 1 mm = 8 dot
        
        for (Map<String, Object> element : elements) {
            String type = (String) element.get("type");
            if (type == null) continue;
            
            Boolean visible = (Boolean) element.get("visible");
            if (Boolean.FALSE.equals(visible)) continue;
            
            Double xMm = getDouble(element, "x", 0.0);
            Double yMm = getDouble(element, "y", 0.0);
            Double widthMm = getDouble(element, "width", 10.0);
            Double heightMm = getDouble(element, "height", 10.0);
            
            int x = (int) Math.round(xMm * mmToDots);
            int y = (int) Math.round(yMm * mmToDots);
            int width = (int) Math.round(widthMm * mmToDots);
            int height = (int) Math.round(heightMm * mmToDots);
            
            if ("text".equals(type)) {
                appendTextTspl(tspl, element, labelData, x, y, mmToDots);
            } else if ("qrcode".equals(type)) {
                appendQrcodeTspl(tspl, element, labelData, x, y, width);
            }
        }
        
        tspl.append("PRINT 1,1\n");
        return tspl.toString();
    }
    
    private void appendTextTspl(StringBuilder tspl, Map<String, Object> element, Map<String, Object> labelData,
                                 int x, int y, double mmToDots) {
        String textFieldType = (String) element.get("textFieldType");
        String customText = (String) element.get("customText");
        Integer fontSize = getInteger(element, "fontSize", 12);
        String fontFamily = (String) element.get("fontFamily");
        if (fontFamily == null) fontFamily = "SimHei";
        String fontColor = (String) element.get("fontColor");
        if (fontColor == null) fontColor = "#000000";
        
        Map<String, Boolean> fontStyle = (Map<String, Boolean>) element.get("fontStyle");
        boolean bold = fontStyle != null && Boolean.TRUE.equals(fontStyle.get("bold"));
        boolean italic = fontStyle != null && Boolean.TRUE.equals(fontStyle.get("italic"));
        
        String text = "";
        String prefix = (String) element.get("prefix");
        if (prefix == null) prefix = "";
        
        if ("custom".equals(textFieldType)) {
            text = customText != null ? customText : "";
        } else if (textFieldType != null) {
            text = resolveFieldValue(textFieldType, labelData);
        }
        
        if (text == null || text.isEmpty()) return;
        
        // 添加前缀
        text = prefix + text;
        
        // 获取元素尺寸
        Double widthMm = getDouble(element, "width", 30.0);
        Double heightMm = getDouble(element, "height", 5.0);
        int widthDots = (int) Math.round(widthMm * mmToDots);
        int heightDots = (int) Math.round(heightMm * mmToDots);
        
        // 使用图片渲染文本（支持任意字号）
        try {
            log.info("渲染文本图片: text={}, fontSize={}, width={}, height={}", text, fontSize, widthDots, heightDots);
            appendTextAsBitmap(tspl, text, x, y, widthDots, heightDots, fontSize, fontFamily, fontColor, bold, italic);
        } catch (Exception e) {
            log.error("渲染文本图片失败: {}", e.getMessage(), e);
            // 降级到TSPL内置字体
            appendTextWithBuiltInFont(tspl, text, x, y, fontSize, bold);
        }
    }
    
    private void appendTextAsBitmap(StringBuilder tspl, String text, int x, int y, 
                                      int width, int height, int fontSize, 
                                      String fontFamily, String fontColor,
                                      boolean bold, boolean italic) {
        // 创建图片缓冲区
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        
        java.awt.Graphics2D g = image.createGraphics();
        
        // 设置背景为白色
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // 设置字体
        // 根据官方文档：300DPI时1mm = 12点
        // 所以"点"在这里就是像素
        // fontSize直接就是像素高度
        // Java Font的大小单位是点（1/72英寸），在屏幕渲染时1点≈1像素
        // 所以直接使用fontSize作为Java字体大小
        
        int fontStyle = java.awt.Font.PLAIN;
        if (bold) fontStyle |= java.awt.Font.BOLD;
        if (italic) fontStyle |= java.awt.Font.ITALIC;
        
        java.awt.Font font = new java.awt.Font(fontFamily, fontStyle, fontSize);
        g.setFont(font);
        
        log.info("字体渲染: text={}, fontSize={}, width={}, height={}", text, fontSize, width, height);
        
        // 设置文本颜色
        java.awt.Color color = parseColor(fontColor);
        g.setColor(color != null ? color : java.awt.Color.BLACK);
        
        // 启用抗锯齿
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, 
                          java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 渲染文本
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textY = fm.getAscent();
        g.drawString(text, 0, textY);
        
        g.dispose();
        
        // 转换为单色位图数据
        int bytesPerRow = (width + 7) / 8;
        byte[] bitmapData = new byte[bytesPerRow * height];
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int rgb = image.getRGB(col, row);
                int r = (rgb >> 16) & 0xFF;
                int g_val = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 计算灰度值，黑色像素设置为1
                int gray = (r + g_val + b) / 3;
                if (gray < 128) {
                    int byteIndex = row * bytesPerRow + col / 8;
                    int bitIndex = 7 - (col % 8);
                    bitmapData[byteIndex] |= (1 << bitIndex);
                }
            }
        }
        
        // 生成BITMAP指令
        tspl.append("BITMAP ").append(x).append(",").append(y)
            .append(",").append(bytesPerRow).append(",").append(height)
            .append(",1,");
        
        // 添加十六进制位图数据
        StringBuilder hexData = new StringBuilder();
        for (byte b : bitmapData) {
            hexData.append(String.format("%02X", b & 0xFF));
        }
        tspl.append(hexData).append("\n");
    }
    
    private void appendTextWithBuiltInFont(StringBuilder tspl, String text, int x, int y, 
                                             Integer fontSize, boolean bold) {
        // TSPL内置字体映射
        String fontName;
        if (fontSize <= 8) {
            fontName = "1";
        } else if (fontSize <= 12) {
            fontName = "2";
        } else if (fontSize <= 24) {
            fontName = "3";
        } else if (fontSize <= 32) {
            fontName = "4";
        } else {
            fontName = "5";
        }
        
        text = escapeTspl(text);
        
        tspl.append("TEXT ").append(x).append(",").append(y)
            .append(",\"").append(fontName).append("\",0,1,1,\"")
            .append(text).append("\"\n");
        
        if (bold) {
            tspl.append("TEXT ").append(x + 1).append(",").append(y)
                .append(",\"").append(fontName).append("\",0,1,1,\"")
                .append(text).append("\"\n");
        }
    }
    
    private void appendQrcodeTspl(StringBuilder tspl, Map<String, Object> element, Map<String, Object> labelData,
                                    int x, int y, int size) {
        String dataField = (String) element.get("dataField");
        if (dataField == null) dataField = "traceCode";
        
        String content = resolveFieldValue(dataField, labelData);
        if (content == null || content.isEmpty()) return;
        
        content = escapeTspl(content);
        int moduleSize = Math.max(2, size / 25);
        
        tspl.append("QRCODE ").append(x).append(",").append(y)
            .append(",M,").append(moduleSize).append(",A,0,M2,S3,\"")
            .append(content).append("\"\n");
    }
    
    private String resolveFieldValue(String fieldName, Map<String, Object> labelData) {
        if (labelData == null) return "";
        
        Object value = null;
        switch (fieldName) {
            case "inboundDate":
                value = labelData.get("generateTime");
                if (value == null) value = labelData.get("inboundDate");
                if (value == null) value = labelData.get("createTime");
                if (value != null) return formatDateOnly(value.toString());
                break;
            case "expiryDate":
                value = labelData.get("expiryDate");
                if (value != null) return formatDateOnly(value.toString());
                break;
            case "shelfLife":
                value = labelData.get("shelfLifeDays");
                if (value == null) value = labelData.get("shelfLife");
                if (value != null) return value.toString() + "天";
                break;
            case "printDate":
                return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            case "materialName":
                value = labelData.get("materialName");
                if (value == null) value = labelData.get("productName");
                break;
            default:
                value = labelData.get(fieldName);
        }
        
        return value != null ? value.toString() : "";
    }
    
    private String escapeTspl(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}
