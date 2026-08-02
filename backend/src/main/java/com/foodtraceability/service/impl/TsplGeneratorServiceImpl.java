package com.foodtraceability.service.impl;

import com.foodtraceability.dto.LabelElementDTO;
import com.foodtraceability.dto.LabelElementDTO.*;
import com.foodtraceability.dto.LabelLayoutConfigDTO;
import com.foodtraceability.dto.LabelLayoutConfigDTO.*;
import com.foodtraceability.service.TsplGeneratorService;
import com.foodtraceability.util.TsplFontMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TSPL指令生成服务实现
 * 提供专业的标签布局到TSPL指令的转换
 */
@Service
public class TsplGeneratorServiceImpl implements TsplGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TsplGeneratorServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public String generateTspl(LabelLayoutConfigDTO layoutConfig) {
        return generateTsplWithData(layoutConfig, null);
    }

    @Override
    public String generateTsplWithData(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data) {
        StringBuilder tspl = new StringBuilder();

        // 1. 生成标签设置指令
        appendLabelSetup(tspl, layoutConfig);

        // 2. 清除缓冲区
        tspl.append("CLS\n");

        // 3. 生成各元素指令
        List<LabelElementDTO> elements = layoutConfig.getElements();
        if (elements != null) {
            int dpi = layoutConfig.getSize() != null ?
                layoutConfig.getSize().getDpi() : 300;

            for (LabelElementDTO element : elements) {
                if (Boolean.FALSE.equals(element.getVisible())) {
                    continue;
                }
                String elementTspl = generateElementTspl(element, data, dpi);
                if (elementTspl != null && !elementTspl.isEmpty()) {
                    tspl.append(elementTspl);
                }
            }
        }

        // 4. 生成打印指令
        appendPrintCommand(tspl, layoutConfig);

        log.debug("生成TSPL指令完成，长度: {}", tspl.length());
        return tspl.toString();
    }

    /**
     * 生成标签设置指令
     */
    private void appendLabelSetup(StringBuilder tspl, LabelLayoutConfigDTO config) {
        LabelSizeConfig size = config.getSize();
        PrintConfig print = config.getPrint();

        if (size != null) {
            // 使用点数单位设置尺寸
            tspl.append("SIZE ").append(size.getWidthDots())
                .append(",").append(size.getHeightDots()).append("\n");
            tspl.append("GAP ").append(size.getGapDots()).append(",0\n");
        } else {
            // 默认40mm x 30mm @ 300 DPI
            tspl.append("SIZE 472,354\n");
            tspl.append("GAP 24,0\n");
        }

        if (print != null) {
            if (print.getSpeed() != null) {
                tspl.append("SPEED ").append(print.getSpeed()).append("\n");
            }
            if (print.getDensity() != null) {
                tspl.append("DENSITY ").append(print.getDensity()).append("\n");
            }
            if (print.getDirection() != null) {
                tspl.append("DIRECTION ").append(print.getDirection()).append("\n");
            }

            // 剥离模式
            if (Boolean.TRUE.equals(print.getPeelMode())) {
                tspl.append("SET PEEL ON\n");
                tspl.append("SET CUTTER OFF\n");
            }
        } else {
            tspl.append("SPEED 3\n");
            tspl.append("DENSITY 12\n");
            tspl.append("DIRECTION 1\n");
        }
    }

    /**
     * 生成打印指令
     */
    private void appendPrintCommand(StringBuilder tspl, LabelLayoutConfigDTO config) {
        PrintConfig print = config.getPrint();

        int copies = 1;
        boolean peelMode = false;

        if (print != null) {
            copies = print.getCopies() != null ? print.getCopies() : 1;
            peelMode = Boolean.TRUE.equals(print.getPeelMode());
        }

        if (peelMode) {
            tspl.append("PRINT 1,1\n");
        } else {
            tspl.append("PRINT ").append(copies).append(",1\n");
        }
    }

    @Override
    public String generateElementTspl(LabelElementDTO element, Map<String, Object> data, int dpi) {
        if (element == null || element.getPosition() == null) {
            return "";
        }

        try {
            switch (element.getType()) {
                case TEXT:
                    return generateTextTspl(element, data, dpi);
                case BARCODE:
                    return generateBarcodeTspl(element, data, dpi);
                case QRCODE:
                    return generateQrcodeTspl(element, data, dpi);
                case LINE:
                    return generateLineTspl(element, dpi);
                case BOX:
                    return generateBoxTspl(element, dpi);
                default:
                    log.warn("未知的元素类型: {}", element.getType());
                    return "";
            }
        } catch (Exception e) {
            log.error("生成元素TSPL失败: elementId={}, error={}",
                element.getId(), e.getMessage());
            return "";
        }
    }

    /**
     * 生成文本元素 TSPL 指令
     */
    private String generateTextTspl(LabelElementDTO element, Map<String, Object> data, int dpi) {
        StringBuilder tspl = new StringBuilder();
        TextConfig text = element.getText();
        PositionConfig pos = element.getPosition();

        if (text == null) {
            return "";
        }

        // 获取文本内容
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

        // 转义特殊字符
        content = escapeTspl(content);

        // 计算字体缩放倍数
        // 现在前端传来的 text.getFont() 应该直接是 scale 值 (1-4)
        // 或者可能是 fontSize 值，需要转换
        int scaleX = text.getScaleX() != null ? text.getScaleX() : 1;
        int scaleY = text.getScaleY() != null ? text.getScaleY() : 1;
        
        // 处理 font 字段
        String font = text.getFont();
        if (font != null) {
            // 如果 font 是数字字符串，说明是 fontSize，需要转换为 scale
            if (!font.startsWith("TSS") && !font.equals("2") && !font.equals("3")) {
                try {
                    int fontSizeOrScale = Integer.parseInt(font);
                    // 判断是 fontSize 还是 scale
                    // 如果值在 1-4 之间，认为是 scale
                    // 否则认为是 fontSize，需要转换
                    if (fontSizeOrScale >= 1 && fontSizeOrScale <= 4) {
                        // 这是 scale 值
                        scaleX = fontSizeOrScale;
                        scaleY = fontSizeOrScale;
                    } else {
                        // 这是 fontSize，转换为 scale
                        scaleX = TsplFontMapper.fontSizeToScale(fontSizeOrScale);
                        scaleY = TsplFontMapper.fontSizeToScale(fontSizeOrScale);
                    }
                    // 使用标准字体
                    font = "TSS24.BF2";
                } catch (NumberFormatException e) {
                    // 不是数字，使用原字体
                    log.warn("无法解析字体：{}, 使用默认字体 TSS24.BF2", font);
                    font = "TSS24.BF2";
                }
            }
        } else {
            // font 为 null，使用默认字体
            font = "TSS24.BF2";
        }

        // 生成 TEXT 指令
        // TEXT x,y,"font",rotation,x_scale,y_scale,"content"
        // 注意：pos.getX() 和 pos.getY() 是 mm，需要转换为 dots
        int xDots = TsplFontMapper.mmToDots(pos.getX());
        int yDots = TsplFontMapper.mmToDots(pos.getY());
        tspl.append("TEXT ")
            .append(xDots).append(",")
            .append(yDots).append(",")
            .append("\"").append(font).append("\",")
            .append(pos.getRotation() != null ? pos.getRotation() : 0).append(",")
            .append(scaleX).append(",")
            .append(scaleY).append(",")
            .append("\"").append(content).append("\"\n");

        return tspl.toString();
    }

    /**
     * 生成条码元素TSPL指令
     */
    private String generateBarcodeTspl(LabelElementDTO element, Map<String, Object> data, int dpi) {
        StringBuilder tspl = new StringBuilder();
        BarcodeConfig barcode = element.getBarcode();
        PositionConfig pos = element.getPosition();

        if (barcode == null) {
            return "";
        }

        // 获取条码内容
        String content = resolveContent(barcode.getContent(), element.getDataBinding(), data);

        if (content == null || content.isEmpty()) {
            return "";
        }

        // 转义特殊字符
        content = escapeTspl(content);

        // 生成BARCODE指令
        // BARCODE x,y,"type",height,rotation,narrow,wide,alignment,"content"
        // 注意：pos.getX() 和 pos.getY() 是 mm，需要转换为 dots
        int xDots = TsplFontMapper.mmToDots(pos.getX());
        int yDots = TsplFontMapper.mmToDots(pos.getY());
        // barcode.getHeight() 是 dots，不需要转换
        tspl.append("BARCODE ")
            .append(xDots).append(",")
            .append(yDots).append(",")
            .append("\"").append(barcode.getBarcodeType()).append("\",")
            .append(barcode.getHeight() != null ? barcode.getHeight() : 80).append(",")
            .append(pos.getRotation() != null ? pos.getRotation() : 0).append(",")
            .append(barcode.getNarrowWidth() != null ? barcode.getNarrowWidth() : 2).append(",")
            .append(barcode.getWideWidth() != null ? barcode.getWideWidth() : 2).append(",")
            .append(barcode.getShowText() != null && barcode.getShowText() ? barcode.getTextPosition() : 3).append(",")
            .append("\"").append(content).append("\"\n");

        return tspl.toString();
    }

    /**
     * 生成二维码元素TSPL指令
     */
    private String generateQrcodeTspl(LabelElementDTO element, Map<String, Object> data, int dpi) {
        StringBuilder tspl = new StringBuilder();
        QRCodeConfig qrcode = element.getQrcode();
        PositionConfig pos = element.getPosition();

        if (qrcode == null) {
            return "";
        }

        // 获取二维码内容
        String content = resolveContent(qrcode.getContent(), element.getDataBinding(), data);

        if (content == null || content.isEmpty()) {
            return "";
        }

        // 转义特殊字符
        content = escapeTspl(content);

        // 生成QRCODE指令
        // QRCODE x,y,errorLevel,moduleWidth,rotation,mode,mask,"content"
        // 注意：pos.getX() 和 pos.getY() 是 mm，需要转换为 dots
        int xDots = TsplFontMapper.mmToDots(pos.getX());
        int yDots = TsplFontMapper.mmToDots(pos.getY());
        tspl.append("QRCODE ")
            .append(xDots).append(",")
            .append(yDots).append(",")
            .append(qrcode.getErrorLevel() != null ? qrcode.getErrorLevel() : "M").append(",")
            .append(qrcode.getModuleSize() != null ? qrcode.getModuleSize() : 4).append(",")
            .append(pos.getRotation() != null ? pos.getRotation() : 0).append(",")
            .append(qrcode.getEncodeMode() != null ? qrcode.getEncodeMode() : "A").append(",")
            .append("M2,S3,")  // 模型2，标准速度
            .append("\"").append(content).append("\"\n");

        return tspl.toString();
    }

    /**
     * 生成线条元素TSPL指令
     */
    private String generateLineTspl(LabelElementDTO element, int dpi) {
        StringBuilder tspl = new StringBuilder();
        LineConfig line = element.getLine();
        PositionConfig pos = element.getPosition();

        if (line == null) {
            return "";
        }

        // 注意：pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight() 是 mm，需要转换为 dots
        int xDots = TsplFontMapper.mmToDots(pos.getX());
        int yDots = TsplFontMapper.mmToDots(pos.getY());
        // line.getLength() 和 line.getLineWidth() 是 dots，不需要转换
        int widthDots = pos.getWidth() != null ? TsplFontMapper.mmToDots(pos.getWidth()) :
            (line.getLength() != null ? line.getLength() : 100);
        int heightDots = pos.getHeight() != null ? TsplFontMapper.mmToDots(pos.getHeight()) :
            line.getLineWidth();

        // 使用BOX指令绘制线条（高度为线条宽度）
        tspl.append("BOX ")
            .append(xDots).append(",")
            .append(yDots).append(",")
            .append(xDots + widthDots).append(",")
            .append(yDots + heightDots).append(",")
            .append(line.getLineWidth()).append("\n");

        return tspl.toString();
    }

    /**
     * 生成矩形框元素TSPL指令
     */
    private String generateBoxTspl(LabelElementDTO element, int dpi) {
        StringBuilder tspl = new StringBuilder();
        BoxConfig box = element.getBox();
        PositionConfig pos = element.getPosition();

        if (box == null || pos.getWidth() == null || pos.getHeight() == null) {
            return "";
        }

        // 生成BOX指令
        // BOX x1,y1,x2,y2,lineWidth
        // 注意：pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight() 是 mm，需要转换为 dots
        int xDots = TsplFontMapper.mmToDots(pos.getX());
        int yDots = TsplFontMapper.mmToDots(pos.getY());
        int widthDots = TsplFontMapper.mmToDots(pos.getWidth());
        int heightDots = TsplFontMapper.mmToDots(pos.getHeight());
        tspl.append("BOX ")
            .append(xDots).append(",")
            .append(yDots).append(",")
            .append(xDots + widthDots).append(",")
            .append(yDots + heightDots).append(",")
            .append(box.getLineWidth()).append("\n");

        return tspl.toString();
    }

    /**
     * 解析内容（支持数据绑定）
     */
    private String resolveContent(String content, DataBindingConfig binding, Map<String, Object> data) {
        // 如果有数据绑定配置，优先使用绑定字段
        if (binding != null && binding.getField() != null && data != null) {
            Object value = data.get(binding.getField());
            if (value != null) {
                String strValue = formatValue(value, binding.getFormat());
                return strValue;
            }
            // 返回默认值
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

        // 日期格式化
        if (value instanceof LocalDate && format != null) {
            LocalDate date = (LocalDate) value;
            try {
                return date.format(DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                return date.format(DATE_FORMAT);
            }
        }

        // 其他类型直接转字符串
        return value.toString();
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
     * TSPL特殊字符转义（完整版）
     */
    private String escapeTspl(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\0", "\\0")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
    }

    @Override
    public ValidationResult validateLayout(LabelLayoutConfigDTO layoutConfig) {
        if (layoutConfig == null) {
            return ValidationResult.error("布局配置不能为空");
        }

        LabelSizeConfig size = layoutConfig.getSize();
        if (size == null) {
            return ValidationResult.error("标签尺寸配置不能为空");
        }

        if (size.getWidth() == null || size.getWidth() <= 0) {
            return ValidationResult.error("标签宽度必须大于0");
        }

        if (size.getHeight() == null || size.getHeight() <= 0) {
            return ValidationResult.error("标签高度必须大于0");
        }

        // 验证元素
        List<LabelElementDTO> elements = layoutConfig.getElements();
        if (elements != null) {
            for (LabelElementDTO element : elements) {
                ValidationResult result = validateElement(element, size);
                if (!result.isValid()) {
                    return result;
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * 验证单个元素
     */
    private ValidationResult validateElement(LabelElementDTO element, LabelSizeConfig size) {
        if (element.getPosition() == null) {
            return ValidationResult.error("元素位置配置不能为空", element.getId());
        }

        PositionConfig pos = element.getPosition();
        int maxX = size.getWidthDots();
        int maxY = size.getHeightDots();

        // 检查位置是否超出标签范围
        if (pos.getX() < 0 || pos.getX() >= maxX) {
            return ValidationResult.error(
                String.format("元素X坐标超出范围: %d (应在0-%d之间)", pos.getX(), maxX - 1),
                element.getId()
            );
        }

        if (pos.getY() < 0 || pos.getY() >= maxY) {
            return ValidationResult.error(
                String.format("元素Y坐标超出范围: %d (应在0-%d之间)", pos.getY(), maxY - 1),
                element.getId()
            );
        }

        return ValidationResult.success();
    }

    @Override
    public int[] calculateElementSize(LabelElementDTO element, Map<String, Object> data, int dpi) {
        if (element == null) {
            return new int[]{0, 0};
        }

        switch (element.getType()) {
            case TEXT:
                return calculateTextSize(element, data, dpi);
            case BARCODE:
                return calculateBarcodeSize(element, dpi);
            case QRCODE:
                return calculateQrcodeSize(element, dpi);
            case LINE:
                return calculateLineSize(element);
            case BOX:
                return calculateBoxSize(element);
            default:
                return new int[]{0, 0};
        }
    }

    /**
     * 计算文本元素尺寸
     */
    private int[] calculateTextSize(LabelElementDTO element, Map<String, Object> data, int dpi) {
        TextConfig text = element.getText();
        if (text == null) {
            return new int[]{0, 0};
        }

        String content = resolveContent(text.getContent(), element.getDataBinding(), data);
        if (content == null || content.isEmpty()) {
            return new int[]{0, 0};
        }

        // 根据字体估算尺寸
        int baseFontSize = getBaseFontSize(text.getFont());
        int scaleX = text.getScaleX() != null ? text.getScaleX() : 1;
        int scaleY = text.getScaleY() != null ? text.getScaleY() : 1;

        // 估算宽度（中文字符宽度约为字体大小的1.5倍）
        int charCount = content.length();
        int width = (int) (charCount * baseFontSize * 1.5 * scaleX);
        int height = baseFontSize * scaleY;

        return new int[]{width, height};
    }

    /**
     * 获取字体基础大小
     */
    private int getBaseFontSize(String font) {
        if (font == null) return 24;

        if (font.contains("24")) return 24;
        if (font.contains("20")) return 20;
        if (font.contains("16")) return 16;
        if (font.contains("12")) return 12;
        if (font.equals("2")) return 8;
        if (font.equals("3")) return 12;

        return 24;
    }

    /**
     * 计算条码元素尺寸
     */
    private int[] calculateBarcodeSize(LabelElementDTO element, int dpi) {
        BarcodeConfig barcode = element.getBarcode();
        if (barcode == null) {
            return new int[]{0, 0};
        }

        String content = barcode.getContent();
        if (content == null || content.isEmpty()) {
            return new int[]{0, 0};
        }

        // 条码宽度约为字符数 * 11 * narrowWidth
        int narrowWidth = barcode.getNarrowWidth() != null ? barcode.getNarrowWidth() : 2;
        int width = content.length() * 11 * narrowWidth;
        int height = barcode.getHeight() != null ? barcode.getHeight() : 80;

        return new int[]{width, height};
    }

    /**
     * 计算二维码元素尺寸
     */
    private int[] calculateQrcodeSize(LabelElementDTO element, int dpi) {
        QRCodeConfig qrcode = element.getQrcode();
        if (qrcode == null) {
            return new int[]{0, 0};
        }

        int moduleSize = qrcode.getModuleSize() != null ? qrcode.getModuleSize() : 4;

        // 二维码尺寸约为 (版本*4+21) * moduleSize
        // 版本根据内容长度估算
        String content = qrcode.getContent();
        int version = content != null ? Math.min((content.length() / 20) + 1, 10) : 1;
        int size = (version * 4 + 21) * moduleSize;

        return new int[]{size, size};
    }

    /**
     * 计算线条元素尺寸
     */
    private int[] calculateLineSize(LabelElementDTO element) {
        LineConfig line = element.getLine();
        PositionConfig pos = element.getPosition();

        if (line == null) {
            return new int[]{0, 0};
        }

        int width = pos.getWidth() != null ? pos.getWidth() :
            (line.getLength() != null ? line.getLength() : 100);
        int height = line.getLineWidth() != null ? line.getLineWidth() : 2;

        return new int[]{width, height};
    }

    /**
     * 计算矩形框元素尺寸
     */
    private int[] calculateBoxSize(LabelElementDTO element) {
        PositionConfig pos = element.getPosition();

        int width = pos.getWidth() != null ? pos.getWidth() : 100;
        int height = pos.getHeight() != null ? pos.getHeight() : 50;

        return new int[]{width, height};
    }
}
