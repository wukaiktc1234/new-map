package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.driver.TsplLabelPrinterDriver;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.LabelTemplate;
import com.foodtraceability.mapper.LabelTemplateMapper;
import com.foodtraceability.printer.XprinterSdkService;
import com.foodtraceability.service.LabelPrintService;
import com.foodtraceability.util.TsplFontMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 标签打印机管理控制器
 *
 * <p>提供标签打印机的发现、诊断、测试打印、批量打印、模板打印和预览等能力。
 * 包含 TSPL 指令生成、PowerShell 打印机发现、二维码/条形码渲染等真实硬件集成逻辑，
 * 具有较高参考价值。已集成 {@link com.foodtraceability.driver.TsplLabelPrinterDriver} 驱动。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.TsplLabelPrinterDriver}、
 *             {@link com.foodtraceability.driver.WsdPrinterDriver} 等驱动实现类，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的标签打印和模板渲染逻辑应下沉为 DeviceDriver 实现类，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/label-printer")
@Tag(name = "标签打印机管理", description = "标签打印机配置、测试和打印接口")
public class LabelPrinterController {

    private static final Logger log = LoggerFactory.getLogger(LabelPrinterController.class);


    public LabelPrinterController(LabelPrintService labelPrintService, TsplLabelPrinterDriver tsplDriver, LabelTemplateMapper labelTemplateMapper, XprinterSdkService sdkPrinterService) {
        this.labelPrintService = labelPrintService;
        this.tsplDriver = tsplDriver;
        this.labelTemplateMapper = labelTemplateMapper;
        this.sdkPrinterService = sdkPrinterService;
    }

    private final LabelPrintService labelPrintService;

    private final TsplLabelPrinterDriver tsplDriver;

    private final LabelTemplateMapper labelTemplateMapper;
    
    private final XprinterSdkService sdkPrinterService;

    /**
     * 发现系统中的所有打印机
     */
    @GetMapping("/discover")
    @Operation(summary = "发现系统打印机")
    @PreAuthorize("hasAuthority('hardware:label-printer:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> discoverPrinters() {
        List<Map<String, Object>> printers = new ArrayList<>();

        try {
            // 使用PowerShell命令获取打印机列表（更准确）
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", 
                "Get-Printer | Select-Object Name, PortName, DriverName | ConvertTo-Json");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            int exitCode = process.waitFor();
            String jsonOutput = output.toString().trim();
            
            if (exitCode == 0 && !jsonOutput.isEmpty()) {
                // 解析JSON输出
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> psPrinters;
                
                if (jsonOutput.startsWith("[")) {
                    psPrinters = mapper.readValue(jsonOutput, new TypeReference<List<Map<String, Object>>>() {});
                } else {
                    Map<String, Object> singlePrinter = mapper.readValue(jsonOutput, new TypeReference<Map<String, Object>>() {});
                    psPrinters = new ArrayList<>();
                    psPrinters.add(singlePrinter);
                }
                
                for (Map<String, Object> psPrinter : psPrinters) {
                    Map<String, Object> printerInfo = new LinkedHashMap<>();
                    String name = (String) psPrinter.get("Name");
                    printerInfo.put("name", name);
                    printerInfo.put("port", psPrinter.get("PortName"));
                    printerInfo.put("driver", psPrinter.get("DriverName"));
                    
                    // 判断是否可能是标签打印机
                    String nameUpper = name != null ? name.toUpperCase() : "";
                    boolean isLabelPrinter = nameUpper.contains("XPRINTER") ||
                                            nameUpper.contains("LABEL") ||
                                            nameUpper.contains("标签") ||
                                            nameUpper.contains("TSC") ||
                                            nameUpper.contains("ZEBRA") ||
                                            nameUpper.contains("GODEX") ||
                                            nameUpper.contains("POSTEK");
                    printerInfo.put("isLabelPrinter", isLabelPrinter);
                    
                    printers.add(printerInfo);
                }
            }
            
            // 如果PowerShell失败，回退到Java API
            if (printers.isEmpty()) {
                PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();

                for (PrintService service : services) {
                    Map<String, Object> printerInfo = new LinkedHashMap<>();
                    printerInfo.put("name", service.getName());
                    printerInfo.put("isDefault", service.equals(defaultPrinter));

                    String name = service.getName().toUpperCase();
                    boolean isLabelPrinter = name.contains("XPRINTER") ||
                                            name.contains("LABEL") ||
                                            name.contains("标签") ||
                                            name.contains("TSC") ||
                                            name.contains("ZEBRA") ||
                                            name.contains("GODEX") ||
                                            name.contains("POSTEK");
                    printerInfo.put("isLabelPrinter", isLabelPrinter);

                    printers.add(printerInfo);
                }
            }

            log.info("发现 {} 台打印机", printers.size());
            return Result.success(printers);

        } catch (Exception e) {
            log.error("发现打印机失败", e);
            return Result.error("发现打印机失败: " + e.getMessage());
        }
    }

    /**
     * 诊断打印机
     */
    @PostMapping("/diagnose")
    @Operation(summary = "诊断标签打印机")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> diagnosePrinter(@RequestBody(required = false) Map<String, String> params) {
        Map<String, Object> diagnosis = new LinkedHashMap<>();

        try {
            log.info("========== 开始打印机诊断 ==========");

            // 1. 检查系统打印机
            diagnosis.put("step1_systemPrinters", checkSystemPrinters());

            // 2. 检查标签打印机
            diagnosis.put("step2_labelPrinters", checkLabelPrinters());

            // 3. 检查驱动状态
            diagnosis.put("step3_driverStatus", checkDriverStatus());

            // 4. 检查打印机 RAW 模式支持
            if (params != null && params.containsKey("printerName")) {
                diagnosis.put("step4_rawModeSupport", checkRawModeSupport(params.get("printerName")));
                diagnosis.put("step5_connectionTest", testConnection(params.get("printerName")));
            }

            // 5. 总体评估
            List<String> issues = new ArrayList<>();
            List<Map<String, Object>> labelPrinters = (List<Map<String, Object>>) diagnosis.get("step2_labelPrinters");
            if (labelPrinters == null || labelPrinters.isEmpty()) {
                issues.add("未检测到标签打印机");
            }

            diagnosis.put("issues", issues);
            diagnosis.put("healthy", issues.isEmpty());

            log.info("========== 打印机诊断完成 ==========");
            return Result.success(diagnosis);

        } catch (Exception e) {
            log.error("打印机诊断失败", e);
            diagnosis.put("error", e.getMessage());
            return Result.success(diagnosis);
        }
    }

    /**
     * 测试打印
     */
    @PostMapping("/test-print")
    @Operation(summary = "测试打印")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> testPrint(@RequestBody(required = false) Map<String, String> params) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            log.info("执行测试打印");

            // 如果指定了打印机名称，先配置
            if (params != null && params.containsKey("printerName")) {
                String printerName = params.get("printerName");
                log.info("使用指定打印机: {}", printerName);

                // 设置打印机名称
                labelPrintService.setPrinterName(printerName);
            }

            // 执行测试打印
            boolean success = labelPrintService.testPrint();

            result.put("success", success);
            result.put("message", success ? "测试打印成功" : "测试打印失败");

            return Result.success(result);

        } catch (Exception e) {
            log.error("测试打印失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 打印追溯码标签
     */
    @PostMapping("/print-trace-label")
    @Operation(summary = "打印追溯码标签")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> printTraceLabel(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String traceCode = (String) params.get("traceCode");
            String materialName = (String) params.get("materialName");
            String batchNo = (String) params.get("batchNo");
            String expiryDate = (String) params.get("expiryDate");
            String supplierName = (String) params.get("supplierName");
            String storeName = (String) params.get("storeName");

            if (traceCode == null || traceCode.isEmpty()) {
                return Result.error("追溯码不能为空");
            }

            log.info("打印追溯码标签: {}", traceCode);

            boolean success = labelPrintService.printTraceabilityLabel(
                traceCode, materialName, batchNo, expiryDate, supplierName, storeName
            );

            result.put("success", success);
            result.put("message", success ? "标签打印成功" : "标签打印失败");

            return Result.success(result);

        } catch (Exception e) {
            log.error("打印追溯码标签失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 批量打印标签
     */
    @PostMapping("/batch-print")
    @Operation(summary = "批量打印标签")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> batchPrint(
            @RequestParam(required = false) String printerName,
            @RequestParam(required = false) String templateId,
            @RequestBody List<Map<String, Object>> labels) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            if (labels == null || labels.isEmpty()) {
                return Result.error("标签列表不能为空");
            }

            log.info("批量打印标签, 数量: {}, 打印机: {}, 模板: {}", labels.size(), printerName, templateId);

            // 如果指定了打印机名称，先设置配置
            if (printerName != null && !printerName.isEmpty()) {
                labelPrintService.setPrinterName(printerName);
            }

            // 如果指定了模板ID，使用模板打印
            if (templateId != null && !templateId.isEmpty()) {
                log.info("使用模板打印: {}", templateId);
                LabelTemplate template = labelTemplateMapper.selectById(templateId);
                if (template == null) {
                    return Result.error("模板不存在: " + templateId);
                }
                
                // 使用模板生成 TSPL 并打印
                return batchPrintWithTemplate(labels, template, printerName);
            }

            boolean success = labelPrintService.batchPrintLabels(labels);

            result.put("success", success);
            result.put("message", success ? "批量打印成功" : "批量打印失败");
            result.put("count", labels.size());

            return Result.success(result);

        } catch (Exception e) {
            log.error("批量打印失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }
    
    /**
     * 使用模板批量打印标签 - 使用SDK服务实现精确字号控制
     */
    private Result<Map<String, Object>> batchPrintWithTemplate(List<Map<String, Object>> labels, LabelTemplate template, String printerName) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            // 获取模板尺寸
            int labelWidthMm = template.getLabelWidth() != null ? template.getLabelWidth() : 40;
            int labelHeightMm = template.getLabelHeight() != null ? template.getLabelHeight() : 30;
            
            // 获取模板元素
            List<Map<String, Object>> elements = template.getElements();
            if (elements == null) {
                elements = new ArrayList<>();
            }
            
            log.info("SDK打印 - 模板 {} 尺寸: {}x{}mm, 元素数量: {}", 
                template.getId(), labelWidthMm, labelHeightMm, elements.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Map<String, Object> labelData : labels) {
                try {
                    // 使用SDK服务打印
                    boolean success = sdkPrinterService.printLabel(
                        printerName != null ? printerName : "Xprinter XP-D35E",
                        labelWidthMm,
                        labelHeightMm,
                        elements,
                        labelData
                    );
                    
                    if (success) {
                        successCount++;
                        log.info("SDK打印成功");
                    } else {
                        failCount++;
                        log.warn("SDK打印失败");
                    }
                    
                    // 打印间隔
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("打印单个标签失败: {}", e.getMessage(), e);
                    failCount++;
                }
            }
            
            result.put("success", failCount == 0);
            result.put("message", String.format("批量打印完成: 成功 %d, 失败 %d", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("count", labels.size());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("模板打印失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }
    
    /**
     * 根据模板生成 TSPL 指令
     */
    private String generateTsplFromTemplate(int labelWidthDots, int labelHeightDots, int dpi, 
                                             List<Map<String, Object>> elements, Map<String, Object> labelData) {
        StringBuilder tspl = new StringBuilder();
        
        // 打印机设置 - SIZE 格式为 mm
        int labelWidthMm = (int) Math.round(labelWidthDots * 25.4 / dpi);
        int labelHeightMm = (int) Math.round(labelHeightDots * 25.4 / dpi);
        
        tspl.append("SIZE ").append(labelWidthMm).append(" mm,").append(labelHeightMm).append(" mm\n");
        tspl.append("GAP 2 mm,0 mm\n");
        tspl.append("SPEED 3\n");
        tspl.append("DENSITY 12\n");
        tspl.append("DIRECTION 1\n");
        tspl.append("REFERENCE 0,0\n");
        tspl.append("OFFSET 0 mm\n");
        tspl.append("SET PEEL OFF\n");
        tspl.append("SET CUTTER OFF\n");
        tspl.append("SET PARTIAL_CUTTER OFF\n");
        tspl.append("SET TEAR ON\n");
        tspl.append("CLS\n");
        
        // mm 到 dots 的转换系数
        double mmToDots = dpi / 25.4;
        
        // 渲染每个元素
        for (Map<String, Object> element : elements) {
            String type = (String) element.get("type");
            if (type == null) continue;
            
            // 跳过不可见元素
            Boolean visible = (Boolean) element.get("visible");
            if (Boolean.FALSE.equals(visible)) continue;
            
            // 获取元素位置（mm 转换为 dots）
            Double xMm = getDouble(element, "x", 0.0);
            Double yMm = getDouble(element, "y", 0.0);
            Double widthMm = getDouble(element, "width", 10.0);
            Double heightMm = getDouble(element, "height", 10.0);
            
            int x = (int) Math.round(xMm * mmToDots);
            int y = (int) Math.round(yMm * mmToDots);
            int width = (int) Math.round(widthMm * mmToDots);
            int height = (int) Math.round(heightMm * mmToDots);
            
            switch (type) {
                case "text":
                    appendTextTspl(tspl, element, labelData, x, y, mmToDots);
                    break;
                case "qrcode":
                    appendQrcodeTspl(tspl, element, labelData, x, y, width);
                    break;
                case "barcode":
                    appendBarcodeTspl(tspl, element, labelData, x, y, width, height);
                    break;
                case "line":
                    appendLineTspl(tspl, element, x, y, width);
                    break;
                case "rect":
                    appendRectTspl(tspl, element, x, y, width, height);
                    break;
            }
        }
        
        // 执行打印
        tspl.append("PRINT 1,1\n");
        
        return tspl.toString();
    }
    
    private void appendTextTspl(StringBuilder tspl, Map<String, Object> element, Map<String, Object> labelData,
                                 int x, int y, double mmToDots) {
        String textFieldType = (String) element.get("textFieldType");
        String customText = (String) element.get("customText");
        Integer fontSize = getInteger(element, "fontSize", 24);

        Map<String, Boolean> fontStyle = (Map<String, Boolean>) element.get("fontStyle");
        boolean bold = fontStyle != null && Boolean.TRUE.equals(fontStyle.get("bold"));

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
        text = escapeTspl(text);

        // TSPL中文字体 TSS24.BF2 是24点阵字体
        // 使用scale来缩放，scale必须相同避免变形
        // 字号范围：4-72，按24点一级映射
        int scale;
        if (fontSize <= 24) {
            scale = 1;  // 实际24点
        } else if (fontSize <= 48) {
            scale = 2;  // 实际48点
        } else {
            scale = 3;  // 实际72点
        }

        // TSPL TEXT指令: TEXT x,y,"font",rotation,xMult,yMult,"content"
        tspl.append("TEXT ").append(x).append(",").append(y)
            .append(",\"TSS24.BF2\",0,")
            .append(scale).append(",").append(scale)
            .append(",\"").append(text).append("\"\n");

        // 加粗效果：偏移1点再打印一次
        if (bold) {
            tspl.append("TEXT ").append(x + 1).append(",").append(y)
                .append(",\"TSS24.BF2\",0,")
                .append(scale).append(",").append(scale)
                .append(",\"").append(text).append("\"\n");
        }
    }
    
    private void appendQrcodeTspl(StringBuilder tspl, Map<String, Object> element, Map<String, Object> labelData,
                                    int x, int y, int size) {
        String dataField = (String) element.get("dataField");
        if (dataField == null) dataField = "traceCode";
        
        String content = resolveFieldValue(dataField, labelData);
        if (content == null || content.isEmpty()) return;
        
        // 转义特殊字符
        content = escapeTspl(content);
        
        // 计算模块大小
        int moduleSize = Math.max(2, size / 25);
        
        // 生成 QRCODE 指令
        tspl.append("QRCODE ").append(x).append(",").append(y)
            .append(",M,").append(moduleSize).append(",A,0,M2,S3,\"")
            .append(content).append("\"\n");
    }
    
    private void appendBarcodeTspl(StringBuilder tspl, Map<String, Object> element, Map<String, Object> labelData,
                                     int x, int y, int width, int height) {
        String dataField = (String) element.get("dataField");
        if (dataField == null) dataField = "traceCode";
        
        String content = resolveFieldValue(dataField, labelData);
        if (content == null || content.isEmpty()) return;
        
        // 转义特殊字符
        content = escapeTspl(content);
        
        // 生成 BARCODE 指令
        tspl.append("BARCODE ").append(x).append(",").append(y)
            .append(",\"128\",").append(height).append(",0,2,2,3,\"")
            .append(content).append("\"\n");
    }
    
    private void appendLineTspl(StringBuilder tspl, Map<String, Object> element, 
                                  int x, int y, int width) {
        Integer borderWidth = getInteger(element, "borderWidth", 1);
        
        // 使用 BOX 指令绘制线条
        tspl.append("BOX ").append(x).append(",").append(y)
            .append(",").append(x + width).append(",").append(y + borderWidth)
            .append(",").append(borderWidth).append("\n");
    }
    
    private void appendRectTspl(StringBuilder tspl, Map<String, Object> element, 
                                  int x, int y, int width, int height) {
        Integer borderWidth = getInteger(element, "borderWidth", 1);
        
        // 生成 BOX 指令
        tspl.append("BOX ").append(x).append(",").append(y)
            .append(",").append(x + width).append(",").append(y + height)
            .append(",").append(borderWidth).append("\n");
    }
    
    private String resolveFieldValue(String fieldName, Map<String, Object> labelData) {
        if (labelData == null) return "";
        
        Object value = null;
        switch (fieldName) {
            case "inboundDate":
                value = labelData.get("generateTime");
                if (value == null) value = labelData.get("inboundDate");
                if (value == null) value = labelData.get("createTime");
                if (value != null) {
                    return formatDateOnly(value.toString());
                }
                break;
            case "expiryDate":
                value = labelData.get("expiryDate");
                if (value != null) {
                    return formatDateOnly(value.toString());
                }
                break;
            case "shelfLife":
                value = labelData.get("shelfLifeDays");
                if (value == null) value = labelData.get("shelfLife");
                if (value != null) {
                    return value.toString() + "天";
                }
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

    /**
     * 设置打印机配置
     */
    @PostMapping("/config")
    @Operation(summary = "设置打印机配置")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> setConfig(@RequestBody Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            int labelWidth = (Integer) config.getOrDefault("labelWidth", 40);
            int labelHeight = (Integer) config.getOrDefault("labelHeight", 30);
            int gapSize = (Integer) config.getOrDefault("gapSize", 2);
            int speed = (Integer) config.getOrDefault("speed", 4);
            int density = (Integer) config.getOrDefault("density", 10);

            log.info("设置打印机配置: {}x{}mm, 间隙={}mm, 速度={}, 浓度={}",
                    labelWidth, labelHeight, gapSize, speed, density);

            boolean success = labelPrintService.setPrinterConfig(
                labelWidth, labelHeight, gapSize, speed, density
            );

            result.put("success", success);
            result.put("message", success ? "配置设置成功" : "配置设置失败");

            return Result.success(result);

        } catch (Exception e) {
            log.error("设置打印机配置失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 获取打印机状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取打印机状态")
    @PreAuthorize("hasAuthority('hardware:label-printer:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = labelPrintService.getPrinterStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取打印机状态失败", e);
            return Result.error("获取打印机状态失败: " + e.getMessage());
        }
    }

    /**
     * 打印自定义标签（TSPL 指令）
     */
    @PostMapping("/print-custom")
    @Operation(summary = "打印自定义标签")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> printCustom(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String tsplData = params.get("tsplData");

            if (tsplData == null || tsplData.isEmpty()) {
                return Result.error("TSPL 指令不能为空");
            }

            log.info("打印自定义标签, TSPL 长度: {}", tsplData.length());

            boolean success = labelPrintService.printCustomLabel(tsplData);

            result.put("success", success);
            result.put("message", success ? "自定义标签打印成功" : "自定义标签打印失败");

            return Result.success(result);

        } catch (Exception e) {
            log.error("打印自定义标签失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 预览单个标签
     */
    @PostMapping("/preview-label")
    @Operation(summary = "预览单个标签")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public void previewLabel(@RequestBody Map<String, Object> labelData,
                             jakarta.servlet.http.HttpServletResponse response) {
        try {
            log.info("预览单个标签: {}", labelData.get("traceCode"));
            
            byte[] imageBytes = generatePreviewImage(Collections.singletonList(labelData));
            
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "inline; filename=label-preview.png");
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            log.error("预览标签失败", e);
        }
    }

    /**
     * 预览多标签页面
     */
    @PostMapping("/preview-page")
    @Operation(summary = "预览多标签页面")
    @PreAuthorize("hasAuthority('hardware:label-printer:manage') or hasAuthority('*')")
    public void previewPage(
            @RequestParam(required = false) String templateId,
            @RequestBody List<Map<String, Object>> labels,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            log.info("预览多标签页面, 数量: {}, 模板: {}", labels.size(), templateId);
            
            byte[] imageBytes;
            
            // 如果指定了模板ID，使用模板生成预览
            if (templateId != null && !templateId.isEmpty()) {
                imageBytes = generatePreviewImageWithTemplate(labels, templateId);
            } else {
                imageBytes = generatePreviewImage(labels);
            }
            
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "inline; filename=labels-preview.png");
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            log.error("预览多标签页面失败", e);
        }
    }
    
    /**
     * 使用模板生成预览图像
     */
    private byte[] generatePreviewImageWithTemplate(List<Map<String, Object>> labels, String templateId) throws Exception {
        log.info("使用模板 {} 生成预览", templateId);
        
        LabelTemplate template = labelTemplateMapper.selectById(templateId);
        if (template == null) {
            log.warn("模板 {} 不存在，使用默认预览", templateId);
            return generatePreviewImage(labels);
        }
        
        // 获取模板尺寸
        int labelWidthMm = template.getLabelWidth() != null ? template.getLabelWidth() : 40;
        int labelHeightMm = template.getLabelHeight() != null ? template.getLabelHeight() : 30;
        
        // 获取模板元素
        List<Map<String, Object>> elements = template.getElements();
        if (elements == null) {
            elements = new ArrayList<>();
        }
        
        log.info("模板 {} 尺寸: {}x{}mm, 元素数量: {}", templateId, labelWidthMm, labelHeightMm, elements.size());
        
        // 生成预览图像
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
        
        int labelsPerRow = 1;
        int rows = labels.size();
        
        int imageWidth = labelsPerRow * labelWidthPx + (labelsPerRow + 1) * gapPx;
        int imageHeight = rows * labelHeightPx + (rows + 1) * gapPx;
        
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            imageWidth, imageHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        
        g.setColor(new java.awt.Color(200, 200, 200));
        g.fillRect(0, 0, gapPx, imageHeight);
        g.fillRect(imageWidth - gapPx, 0, gapPx, imageHeight);
        
        for (int i = 0; i < rows; i++) {
            int x = gapPx;
            int y = gapPx + i * (labelHeightPx + gapPx);
            
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(x, y, labelWidthPx, labelHeightPx);
            
            g.setColor(java.awt.Color.BLACK);
            g.drawRect(x, y, labelWidthPx, labelHeightPx);
            
            Map<String, Object> labelData = labels.get(i);
            
            // 按照模板元素渲染
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

        Integer fontSize = getInteger(element, "fontSize", 12);
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
            // 尝试多个可能的字段名
            Object value = labelData.get(textFieldType);
            if (value == null) {
                // 字段名映射
                switch (textFieldType) {
                    case "inboundDate":
                        value = labelData.get("generateTime");
                        if (value == null) value = labelData.get("inboundDate");
                        if (value == null) value = labelData.get("createTime");
                        // 格式化日期，只显示日期部分
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
                        // 自动生成打印日期
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

        // 使用统一的字体映射工具类，确保预览与打印一致
        // 预览图像使用 300 DPI 生成，所以直接使用 fontSizeToPixels(fontSize, 300)
        int fontSizePx = TsplFontMapper.fontSizeToPixels(fontSize, TsplFontMapper.PRINTER_DPI);
        int style = bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN;
        g.setFont(new java.awt.Font(fontFamily, style, fontSizePx));

        java.awt.Color color = parseColor(fontColor);
        g.setColor(color != null ? color : java.awt.Color.BLACK);

        // 截断文本以适应宽度
        String truncated = truncateToWidth(g, text, width);
        g.drawString(truncated, x, y + fontSizePx);
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
        
        // 简单的条形码渲染
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

    /**
     * 生成预览图像
     * 按照 Xprinter XP-D35E 标签纸尺寸 (40mm x 30mm) 生成
     * DPI: 300, 像素尺寸: 472 x 354
     * 
     * 布局与 TSPL 打印指令保持一致：
     * ┌─────────────────────────────┐
     * │ 物料名称          [二维码] │  ← 第1行：物料名称 + 二维码
     * │ 门店:XXX                    │  ← 第2行
     * │ 保质:XX天  入库:XXXX-XX-XX  │  ← 第3行：两列布局
     * │ 到期:XXXX-XX-XX  供应:XXX   │  ← 第4行：两列布局
     * │ 追溯码:XXXXXXXXXXXX         │  ← 第5行：追溯码
     * └─────────────────────────────┘
     */
    private byte[] generatePreviewImage(List<Map<String, Object>> labels) throws Exception {
        int dpi = 300;
        double mmToInch = 25.4;
        
        int labelWidthMm = 40;
        int labelHeightMm = 30;
        
        int labelWidthPx = (int) (labelWidthMm / mmToInch * dpi);
        int labelHeightPx = (int) (labelHeightMm / mmToInch * dpi);
        
        int gapPx = 20;
        
        int labelsPerRow = 1;
        int rows = labels.size();
        
        int imageWidth = labelsPerRow * labelWidthPx + (labelsPerRow + 1) * gapPx;
        int imageHeight = rows * labelHeightPx + (rows + 1) * gapPx;
        
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            imageWidth, imageHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        
        g.setColor(new java.awt.Color(200, 200, 200));
        g.fillRect(0, 0, gapPx, imageHeight);
        g.fillRect(imageWidth - gapPx, 0, gapPx, imageHeight);
        
        for (int i = 0; i < rows; i++) {
            int x = gapPx;
            int y = gapPx + i * (labelHeightPx + gapPx);
            
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(x, y, labelWidthPx, labelHeightPx);
            
            g.setColor(java.awt.Color.BLACK);
            g.drawRect(x, y, labelWidthPx, labelHeightPx);
            
            Map<String, Object> label = labels.get(i);
            
            int margin = 15;
            
            // 二维码 - 右上角
            int qrSize = 85;
            int qrX = x + labelWidthPx - qrSize - margin;
            int qrY = y + margin;
            
            try {
                String traceCode = getString(label, "traceCode", "");
                if (!traceCode.isEmpty()) {
                    java.awt.Image qrImage = generateQrCodeImage(traceCode, qrSize, qrSize);
                    if (qrImage != null) {
                        g.drawImage(qrImage, qrX, qrY, null);
                    }
                }
            } catch (Exception e) {
                log.warn("生成二维码失败: {}", e.getMessage());
            }
            
            int textX = x + margin;
            int textY = y + margin + 24;
            int textWidth = qrX - textX - 10;
            
            g.setColor(java.awt.Color.BLACK);
            
            // 第1行：物料名称（大字体）
            String materialName = getString(label, "materialName", "");
            g.setFont(new java.awt.Font("SimHei", java.awt.Font.BOLD, 28));
            String truncatedName = truncateToWidth(g, materialName, textWidth);
            g.drawString(truncatedName, textX, textY);
            textY += 40;
            
            // 第2行：门店
            g.setFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 16));
            String storeName = getString(label, "storeName", "");
            g.drawString("门店:" + truncateToWidth(g, storeName, textWidth - 70), textX, textY);
            textY += 28;
            
            // 第3行：保质期 + 入库日期（两列）
            int col1X = textX;
            int col2X = textX + 150;
            
            String shelfLife = getString(label, "shelfLifeDays", "");
            g.drawString("保质:" + shelfLife + "天", col1X, textY);
            
            String generateTime = getString(label, "generateTime", "");
            if (generateTime != null && generateTime.length() > 10) {
                generateTime = generateTime.substring(0, 10);
            }
            g.drawString("入库:" + generateTime, col2X, textY);
            textY += 28;
            
            // 第4行：到期日期 + 供应商（两列）
            String expiryDate = getString(label, "expiryDate", "");
            if (expiryDate != null && expiryDate.length() > 10) {
                expiryDate = expiryDate.substring(0, 10);
            }
            g.drawString("到期:" + expiryDate, col1X, textY);
            
            String supplierName = getString(label, "supplierName", "");
            g.drawString("供应:" + truncateToWidth(g, supplierName, 80), col2X, textY);
            
            // 第5行：追溯码 - 底部
            String traceCode = getString(label, "traceCode", "");
            g.setFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 16));
            int codeY = y + labelHeightPx - margin - 8;
            g.drawString("码:" + traceCode, textX, codeY);
            
            g.setFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 10));
            g.setColor(java.awt.Color.GRAY);
            g.drawString("40mm x 30mm", x + labelWidthPx - 80, y + labelHeightPx - 8);
        }
        
        g.dispose();
        
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
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

    private String truncateToWidth(java.awt.Graphics2D g, String str, int maxWidth) {
        if (str == null || str.isEmpty()) return "";
        
        java.awt.FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(str) <= maxWidth) {
            return str;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            String test = sb.toString() + str.charAt(i) + "...";
            if (fm.stringWidth(test) > maxWidth) {
                return sb.toString() + "...";
            }
            sb.append(str.charAt(i));
        }
        return str;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value.toString();
    }

    private java.awt.Image generateQrCodeImage(String content, int width, int height) throws Exception {
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(
            content, com.google.zxing.BarcodeFormat.QR_CODE, width, height);
        
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? java.awt.Color.BLACK.getRGB() : java.awt.Color.WHITE.getRGB());
            }
        }
        
        return image;
    }

    /**
     * 检查系统打印机
     */
    private List<Map<String, Object>> checkSystemPrinters() {
        List<Map<String, Object>> printers = new ArrayList<>();

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", service.getName());
            printers.add(info);
        }

        return printers;
    }

    /**
     * 检查标签打印机
     */
    private List<Map<String, Object>> checkLabelPrinters() {
        List<Map<String, Object>> labelPrinters = new ArrayList<>();

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            String name = service.getName().toUpperCase();
            if (name.contains("XPRINTER") ||
                name.contains("LABEL") ||
                name.contains("标签") ||
                name.contains("TSC") ||
                name.contains("ZEBRA") ||
                name.contains("GODEX") ||
                name.contains("POSTEK")) {

                Map<String, Object> info = new HashMap<>();
                info.put("name", service.getName());
                info.put("model", detectPrinterModel(name));
                labelPrinters.add(info);
            }
        }

        return labelPrinters;
    }

    /**
     * 检测打印机型号
     */
    private String detectPrinterModel(String printerName) {
        if (printerName.contains("XP-D35") || printerName.contains("XPRINTER")) {
            return "Xprinter XP-D35E";
        }
        if (printerName.contains("TSC")) {
            return "TSC Label Printer";
        }
        if (printerName.contains("ZEBRA")) {
            return "Zebra Label Printer";
        }
        return "Unknown Label Printer";
    }

    /**
     * 检查驱动状态
     */
    private Map<String, Object> checkDriverStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        status.put("driverName", tsplDriver.getDriverName());
        status.put("driverVersion", tsplDriver.getDriverVersion());
        status.put("supportedDeviceType", tsplDriver.getSupportedDeviceType());
        status.put("connected", tsplDriver.isConnected());

        return status;
    }

    /**
     * 测试连接（修复资源泄露）
     */
    private Map<String, Object> testConnection(String printerName) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            HardwareConfig config = new HardwareConfig();
            config.setDeviceType("LABEL_PRINTER");
            config.setDeviceName(printerName);
            config.setConnectionType("WINDOWS");

            boolean initSuccess = tsplDriver.init(config);
            result.put("initSuccess", initSuccess);

            if (initSuccess) {
                tsplDriver.setPrinterName(printerName);
                boolean connectSuccess = tsplDriver.connect();
                result.put("connectSuccess", connectSuccess);
                result.put("message", connectSuccess ? "连接成功" : "连接失败");
                
                if (connectSuccess) {
                    tsplDriver.disconnect();
                    log.info("测试连接完成，已断开打印机连接");
                }
            } else {
                result.put("connectSuccess", false);
                result.put("message", "初始化失败");
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("connectSuccess", false);
        } finally {
            if (tsplDriver.isConnected()) {
                try {
                    tsplDriver.disconnect();
                } catch (Exception e) {
                    log.warn("断开打印机连接失败: {}", e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * 检查打印机 RAW 模式支持
     */
    private Map<String, Object> checkRawModeSupport(String printerName) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService targetPrinter = null;

            for (PrintService service : services) {
                if (service.getName().equals(printerName)) {
                    targetPrinter = service;
                    break;
                }
            }

            if (targetPrinter == null) {
                result.put("found", false);
                result.put("message", "未找到打印机: " + printerName);
                return result;
            }

            result.put("found", true);
            result.put("printerName", targetPrinter.getName());

            // 检查支持的 DocFlavor
            javax.print.DocFlavor[] flavors = targetPrinter.getSupportedDocFlavors();
            List<String> supportedFormats = new ArrayList<>();
            boolean supportsRaw = false;
            boolean supportsAutoSense = false;

            for (javax.print.DocFlavor flavor : flavors) {
                String mimeType = flavor.getMimeType();
                if (mimeType != null) {
                    supportedFormats.add(mimeType);
                    
                    if (mimeType.contains("raw") || mimeType.contains("application/octet-stream")) {
                        supportsRaw = true;
                    }
                    
                    if (flavor.equals(javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE)) {
                        supportsAutoSense = true;
                    }
                }
            }

            result.put("supportedFormats", supportedFormats);
            result.put("supportsRaw", supportsRaw);
            result.put("supportsAutoSense", supportsAutoSense);

            // 建议
            List<String> recommendations = new ArrayList<>();
            if (supportsRaw) {
                recommendations.add("打印机支持 RAW 模式，可以直接发送 TSPL 指令");
            } else if (supportsAutoSense) {
                recommendations.add("打印机不支持 RAW 模式，但支持 AUTOSENSE");
                recommendations.add("建议安装 Xprinter 官方驱动或使用 USB 虚拟串口模式");
            } else {
                recommendations.add("打印机不支持 RAW 或 AUTOSENSE 模式");
                recommendations.add("必须使用厂商提供的驱动或 SDK");
            }
            result.put("recommendations", recommendations);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}
