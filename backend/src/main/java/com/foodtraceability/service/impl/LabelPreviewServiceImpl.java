package com.foodtraceability.service.impl;

import com.foodtraceability.dto.LabelElementDTO;
import com.foodtraceability.dto.LabelElementDTO.*;
import com.foodtraceability.dto.LabelLayoutConfigDTO;
import com.foodtraceability.dto.LabelLayoutConfigDTO.*;
import com.foodtraceability.service.LabelPreviewService;
import com.foodtraceability.service.TsplGeneratorService;
import com.foodtraceability.util.TsplFontMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 标签预览服务实现
 * 生成与打印完全一致的HTML预览
 */
@Service
public class LabelPreviewServiceImpl implements LabelPreviewService {

    private static final Logger log = LoggerFactory.getLogger(LabelPreviewServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 屏幕DPI（用于预览）
    private static final int SCREEN_DPI = 96;


    public LabelPreviewServiceImpl(TsplGeneratorService tsplGeneratorService, ObjectMapper objectMapper) {
        this.tsplGeneratorService = tsplGeneratorService;
        this.objectMapper = objectMapper;
    }

    private final TsplGeneratorService tsplGeneratorService;

    private final ObjectMapper objectMapper;

    @Override
    public String generateHtmlPreview(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data) {
        StringBuilder html = new StringBuilder();

        LabelSizeConfig size = layoutConfig.getSize();
        int widthMm = size != null ? size.getWidth() : 40;
        int heightMm = size != null ? size.getHeight() : 30;
        int printerDpi = size != null ? size.getDpi() : 300;

        // widthMm 和 heightMm 是 mm 单位，需要先转换为 dots，再转换为像素
        // mm -> dots: mm * printerDpi / 25.4
        // dots -> pixels: dots * SCREEN_DPI / printerDpi
        // 简化: pixels = mm * SCREEN_DPI / 25.4
        int widthDots = TsplFontMapper.mmToDots(widthMm);
        int heightDots = TsplFontMapper.mmToDots(heightMm);
        int widthPx = TsplFontMapper.dotsToPixels(widthDots, SCREEN_DPI);
        int heightPx = TsplFontMapper.dotsToPixels(heightDots, SCREEN_DPI);

        // HTML 头部
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>标签预览</title>\n");
        html.append("<style>\n");
        html.append(generatePreviewStyles(widthMm, heightMm));
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 标签容器
        html.append("<div class=\"label-container\">\n");
        // 使用像素而不是 mm
        html.append("<div class=\"label\" style=\"width:").append(widthPx).append("px; height:")
            .append(heightPx).append("px;\">\n");

        // 生成元素预览
        List<LabelElementDTO> elements = layoutConfig.getElements();
        if (elements != null) {
            for (LabelElementDTO element : elements) {
                if (Boolean.FALSE.equals(element.getVisible())) {
                    continue;
                }
                String elementHtml = generateElementHtml(element, data, printerDpi, widthMm, heightMm);
                html.append(elementHtml);
            }
        }

        html.append("</div>\n");
        html.append("</div>\n");

        // 添加打印脚本
        html.append("<script>\n");
        html.append("function printLabel() { window.print(); }\n");
        html.append("</script>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 生成预览样式
     */
    private String generatePreviewStyles(int widthMm, int heightMm) {
        StringBuilder styles = new StringBuilder();

        styles.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        styles.append("body { background: #f5f5f5; padding: 20px; font-family: 'Microsoft YaHei', sans-serif; }\n");
        styles.append(".label-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n");
        styles.append(".label { background: white; border: 1px solid #333; position: relative; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n");
        styles.append(".element { position: absolute; }\n");
        styles.append(".text-element { white-space: nowrap; }\n");
        styles.append(".barcode-element { display: flex; flex-direction: column; align-items: center; }\n");
        styles.append(".barcode-bars { display: flex; height: 100%; }\n");
        styles.append(".barcode-bar { background: black; }\n");
        styles.append(".qrcode-element { display: flex; justify-content: center; align-items: center; }\n");
        styles.append(".line-element { background: black; }\n");
        styles.append(".box-element { border: 1px solid black; }\n");

        // 打印样式
        styles.append("@media print {\n");
        styles.append("  body { background: white; padding: 0; }\n");
        styles.append("  .label-container { min-height: auto; }\n");
        styles.append("  .label { box-shadow: none; }\n");
        styles.append("}\n");

        return styles.toString();
    }

    /**
     * 生成元素 HTML
     * 注意：pos.getX()/getY() 返回的是 mm，需要先转换为 dots，再转换为像素用于 HTML 显示
     */
    private String generateElementHtml(LabelElementDTO element, Map<String, Object> data,
                                      int printerDpi, int labelWidthMm, int labelHeightMm) {
        PositionConfig pos = element.getPosition();
        if (pos == null) {
            return "";
        }

        // 将 mm 转换为 dots，再转换为像素 (使用 96 DPI 用于屏幕显示)
        // mm -> dots: mm * printerDpi / 25.4
        // dots -> pixels: dots * 96 / printerDpi
        // 简化: pixels = mm * 96 / 25.4
        int xDots = TsplFontMapper.mmToDots(pos.getX());
        int yDots = TsplFontMapper.mmToDots(pos.getY());
        double xPixels = TsplFontMapper.dotsToPixels(xDots, SCREEN_DPI);
        double yPixels = TsplFontMapper.dotsToPixels(yDots, SCREEN_DPI);

        StringBuilder html = new StringBuilder();

        switch (element.getType()) {
            case TEXT:
                html.append(generateTextHtml(element, data, xPixels, yPixels, printerDpi));
                break;
            case BARCODE:
                html.append(generateBarcodeHtml(element, data, xPixels, yPixels, printerDpi));
                break;
            case QRCODE:
                html.append(generateQrcodeHtml(element, data, xPixels, yPixels, printerDpi));
                break;
            case LINE:
                html.append(generateLineHtml(element, xPixels, yPixels, printerDpi));
                break;
            case BOX:
                html.append(generateBoxHtml(element, xPixels, yPixels, printerDpi));
                break;
            default:
                break;
        }

        return html.toString();
    }

    /**
     * 生成文本元素 HTML
     */
    private String generateTextHtml(LabelElementDTO element, Map<String, Object> data,
                                   double xPixels, double yPixels, int printerDpi) {
        TextConfig text = element.getText();
        if (text == null) {
            return "";
        }

        String content = resolveContent(text.getContent(), element.getDataBinding(), data);

        // 检查空值隐藏
        if (content == null || content.isEmpty()) {
            DataBindingConfig binding = element.getDataBinding();
            if (binding != null && Boolean.TRUE.equals(binding.getHideIfEmpty())) {
                return "";
            }
            content = "";
        }

        // 添加前缀和后缀
        DataBindingConfig binding = element.getDataBinding();
        if (binding != null) {
            if (binding.getPrefix() != null) {
                content = binding.getPrefix() + content;
            }
            if (binding.getSuffix() != null) {
                content = content + binding.getSuffix();
            }
        }

        // 文本截断
        if (text.getTruncateChars() != null && text.getTruncateChars() > 0) {
            content = truncateText(content, text.getTruncateChars());
        }

        // 转义 HTML
        content = escapeHtml(content);

        // 计算字体大小（像素）
        // 使用 TsplFontMapper 统一计算
        int baseFontSize = getBaseFontSize(text.getFont());
        int scaleX = text.getScaleX() != null ? text.getScaleX() : 1;
        int scaleY = text.getScaleY() != null ? text.getScaleY() : 1;
        // dots * 96 / 300 = pixels
        double fontSizePixels = baseFontSize * scaleY * 96.0 / printerDpi;

        // 生成样式 (使用像素)
        StringBuilder style = new StringBuilder();
        style.append("left:").append(xPixels).append("px;");
        style.append("top:").append(yPixels).append("px;");
        style.append("font-size:").append(fontSizePixels).append("px;");
        style.append("transform:scaleX(").append(scaleX).append(");");

        if (Boolean.TRUE.equals(text.getBold())) {
            style.append("font-weight:bold;");
        }

        // 对齐方式
        String align = element.getPosition() != null ? element.getPosition().getAlign() : "left";
        if ("center".equals(align)) {
            style.append("text-align:center;");
        } else if ("right".equals(align)) {
            style.append("text-align:right;");
        }

        return String.format("<div class=\"element text-element\" style=\"%s\">%s</div>\n",
            style.toString(), content);
    }

    /**
     * 生成条码元素HTML
     */
    private String generateBarcodeHtml(LabelElementDTO element, Map<String, Object> data,
                                      double xMm, double yMm, int printerDpi) {
        BarcodeConfig barcode = element.getBarcode();
        if (barcode == null) {
            return "";
        }

        String content = resolveContent(barcode.getContent(), element.getDataBinding(), data);
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 计算尺寸
        int heightPt = barcode.getHeight() != null ? barcode.getHeight() : 80;
        double heightMm = heightPt * 25.4 / printerDpi;
        int narrowWidth = barcode.getNarrowWidth() != null ? barcode.getNarrowWidth() : 2;

        // 生成条码条
        StringBuilder barsHtml = new StringBuilder();
        String barcodePattern = generateBarcodePattern(content, barcode.getBarcodeType());

        for (int i = 0; i < barcodePattern.length(); i++) {
            char c = barcodePattern.charAt(i);
            if (c == '1') {
                double barWidthMm = narrowWidth * 25.4 / printerDpi;
                barsHtml.append(String.format(
                    "<div class=\"barcode-bar\" style=\"width:%.3fmm;height:%.2fmm;\"></div>",
                    barWidthMm, heightMm));
            } else {
                double spaceWidthMm = narrowWidth * 25.4 / printerDpi;
                barsHtml.append(String.format(
                    "<div style=\"width:%.3fmm;\"></div>", spaceWidthMm));
            }
        }

        // 生成HTML
        StringBuilder html = new StringBuilder();
        html.append(String.format(
            "<div class=\"element barcode-element\" style=\"left:%.2fmm;top:%.2fmm;\">",
            xMm, yMm));
        html.append("<div class=\"barcode-bars\">").append(barsHtml).append("</div>");

        if (Boolean.TRUE.equals(barcode.getShowText())) {
            html.append("<div style=\"font-size:2mm;\">").append(escapeHtml(content)).append("</div>");
        }

        html.append("</div>\n");

        return html.toString();
    }

    /**
     * 生成条码图案（简化版，仅用于预览）
     */
    private String generateBarcodePattern(String content, String barcodeType) {
        // 简化实现：生成随机的条码图案用于预览
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            // 每个字符生成11位模式
            pattern.append("10101110110");
        }
        return pattern.toString();
    }

    /**
     * 生成二维码元素HTML
     */
    private String generateQrcodeHtml(LabelElementDTO element, Map<String, Object> data,
                                     double xMm, double yMm, int printerDpi) {
        QRCodeConfig qrcode = element.getQrcode();
        if (qrcode == null) {
            return "";
        }

        String content = resolveContent(qrcode.getContent(), element.getDataBinding(), data);
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 计算尺寸
        int moduleSize = qrcode.getModuleSize() != null ? qrcode.getModuleSize() : 4;
        int version = Math.min((content.length() / 20) + 1, 10);
        int modules = version * 4 + 17; // 二维码模块数
        double sizeMm = modules * moduleSize * 25.4 / printerDpi;

        // 使用在线二维码API生成预览图
        String qrImageUrl = String.format(
            "https://api.qrserver.com/v1/create-qr-code/?size=%dx%d&data=%s",
            (int)(sizeMm * SCREEN_DPI / 25.4),
            (int)(sizeMm * SCREEN_DPI / 25.4),
            encodeUrl(content)
        );

        return String.format(
            "<div class=\"element qrcode-element\" style=\"left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">" +
            "<img src=\"%s\" style=\"width:100%%;height:100%%;\" />" +
            "</div>\n",
            xMm, yMm, sizeMm, sizeMm, qrImageUrl);
    }

    /**
     * 生成线条元素HTML
     */
    private String generateLineHtml(LabelElementDTO element, double xMm, double yMm, int printerDpi) {
        LineConfig line = element.getLine();
        PositionConfig pos = element.getPosition();

        if (line == null) {
            return "";
        }

        int widthPt = pos.getWidth() != null ? pos.getWidth() :
            (line.getLength() != null ? line.getLength() : 100);
        int heightPt = line.getLineWidth() != null ? line.getLineWidth() : 2;

        double widthMm = widthPt * 25.4 / printerDpi;
        double heightMm = heightPt * 25.4 / printerDpi;

        return String.format(
            "<div class=\"element line-element\" style=\"left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\"></div>\n",
            xMm, yMm, widthMm, heightMm);
    }

    /**
     * 生成矩形框元素HTML
     */
    private String generateBoxHtml(LabelElementDTO element, double xMm, double yMm, int printerDpi) {
        BoxConfig box = element.getBox();
        PositionConfig pos = element.getPosition();

        if (box == null || pos.getWidth() == null || pos.getHeight() == null) {
            return "";
        }

        double widthMm = pos.getWidth() * 25.4 / printerDpi;
        double heightMm = pos.getHeight() * 25.4 / printerDpi;
        double borderWidthMm = box.getLineWidth() * 25.4 / printerDpi;

        StringBuilder style = new StringBuilder();
        style.append("left:").append(xMm).append("mm;");
        style.append("top:").append(yMm).append("mm;");
        style.append("width:").append(widthMm).append("mm;");
        style.append("height:").append(heightMm).append("mm;");
        style.append("border-width:").append(borderWidthMm).append("mm;");

        return String.format("<div class=\"element box-element\" style=\"%s\"></div>\n", style.toString());
    }

    @Override
    public PreviewData generatePreviewData(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data) {
        PreviewData previewData = new PreviewData();

        LabelSizeConfig size = layoutConfig.getSize();
        int widthMm = size != null ? size.getWidth() : 40;
        int heightMm = size != null ? size.getHeight() : 30;
        int printerDpi = size != null ? size.getDpi() : 300;

        // 计算预览尺寸（像素）
        double scale = SCREEN_DPI / 25.4;
        previewData.setWidth((int) (widthMm * scale));
        previewData.setHeight((int) (heightMm * scale));
        previewData.setScale(scale);

        // 转换元素
        List<PreviewElement> previewElements = new ArrayList<>();
        List<LabelElementDTO> elements = layoutConfig.getElements();

        if (elements != null) {
            for (LabelElementDTO element : elements) {
                if (Boolean.FALSE.equals(element.getVisible())) {
                    continue;
                }

                PreviewElement previewElement = convertToPreviewElement(element, data, printerDpi, scale);
                if (previewElement != null) {
                    previewElements.add(previewElement);
                }
            }
        }

        previewData.setElements(previewElements);
        return previewData;
    }

    /**
     * 转换为预览元素
     */
    private PreviewElement convertToPreviewElement(LabelElementDTO element, Map<String, Object> data,
                                                  int printerDpi, double scale) {
        PositionConfig pos = element.getPosition();
        if (pos == null) {
            return null;
        }

        PreviewElement previewElement = new PreviewElement();
        previewElement.setId(element.getId());
        previewElement.setType(element.getType().name().toLowerCase());
        previewElement.setX((int) (pos.getX() * scale / printerDpi * 25.4));
        previewElement.setY((int) (pos.getY() * scale / printerDpi * 25.4));

        // 计算尺寸
        int[] size = tsplGeneratorService.calculateElementSize(element, data, printerDpi);
        previewElement.setWidth((int) (size[0] * scale / printerDpi * 25.4));
        previewElement.setHeight((int) (size[1] * scale / printerDpi * 25.4));

        // 设置内容和样式
        Map<String, Object> style = new HashMap<>();

        switch (element.getType()) {
            case TEXT:
                TextConfig text = element.getText();
                if (text != null) {
                    String content = resolveContent(text.getContent(), element.getDataBinding(), data);
                    previewElement.setContent(content);

                    int baseFontSize = getBaseFontSize(text.getFont());
                    style.put("fontSize", baseFontSize * (text.getScaleY() != null ? text.getScaleY() : 1));
                    style.put("fontFamily", "Microsoft YaHei");
                    if (Boolean.TRUE.equals(text.getBold())) {
                        style.put("fontWeight", "bold");
                    }
                }
                break;

            case BARCODE:
                BarcodeConfig barcode = element.getBarcode();
                if (barcode != null) {
                    previewElement.setContent(resolveContent(barcode.getContent(), element.getDataBinding(), data));
                    style.put("barcodeType", barcode.getBarcodeType());
                    style.put("showText", barcode.getShowText());
                }
                break;

            case QRCODE:
                QRCodeConfig qrcode = element.getQrcode();
                if (qrcode != null) {
                    previewElement.setContent(resolveContent(qrcode.getContent(), element.getDataBinding(), data));
                    style.put("moduleSize", qrcode.getModuleSize());
                    style.put("errorLevel", qrcode.getErrorLevel());
                }
                break;

            case LINE:
                LineConfig line = element.getLine();
                if (line != null) {
                    style.put("lineWidth", line.getLineWidth());
                }
                break;

            case BOX:
                BoxConfig box = element.getBox();
                if (box != null) {
                    style.put("borderWidth", box.getLineWidth());
                }
                break;
        }

        previewElement.setStyle(style);
        return previewElement;
    }

    @Override
    public String generateThumbnail(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data,
                                   int maxWidth, int maxHeight) {
        // 简化实现：返回HTML预览
        // 实际项目中可以使用Html2Image库生成真实缩略图
        return generateHtmlPreview(layoutConfig, data);
    }

    /**
     * 解析内容（支持数据绑定）
     */
    private String resolveContent(String content, DataBindingConfig binding, Map<String, Object> data) {
        if (binding != null && binding.getField() != null && data != null) {
            Object value = data.get(binding.getField());
            if (value != null) {
                return formatValue(value, binding.getFormat());
            }
            return binding.getDefaultValue();
        }
        return content;
    }

    /**
     * 格式化值
     */
    private String formatValue(Object value, String format) {
        if (value == null) {
            return "";
        }

        if (value instanceof LocalDate && format != null) {
            LocalDate date = (LocalDate) value;
            try {
                return date.format(DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                return date.format(DATE_FORMAT);
            }
        }

        return value.toString();
    }

    /**
     * 获取字体基础大小
     * 使用 TsplFontMapper 统一计算逻辑
     */
    private int getBaseFontSize(String font) {
        // 从字体名称中提取基础大小
        if (font == null) return 24;

        // 尝试从字体名称中解析数字
        for (int size : TsplFontMapper.FONT_SIZES) {
            if (font.contains(String.valueOf(size))) {
                return size;
            }
        }

        // 默认返回 24
        return 24;
    }

    /**
     * 截断文本
     */
    private String truncateText(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars);
    }

    /**
     * 转义HTML
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * URL编码
     */
    private String encodeUrl(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }
}
