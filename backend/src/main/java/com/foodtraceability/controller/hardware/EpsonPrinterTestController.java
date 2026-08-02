package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * EPSON 打印机测试控制器
 *
 * <p>提供基于 javax.print API 的打印机发现、诊断、测试打印和标签预览等能力。
 * 包含二维码生成、条形码绘制、标签排版等真实硬件集成逻辑，具有一定参考价值。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.PrinterDriver}、
 *             {@link com.foodtraceability.driver.WsdPrinterDriver} 等驱动实现类，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的硬件集成逻辑应下沉为 DeviceDriver 实现类，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。</p>
 */
@Deprecated
@RestController
@RequestMapping("/v1/epson-printer")
@Tag(name = "EPSON打印机测试", description = "EPSON打印机测试和标签打印接口")
public class EpsonPrinterTestController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EpsonPrinterTestController.class);
    private static final String FONT_NAME = "Microsoft YaHei";
    private static final int LABEL_WIDTH_MM = 40;
    private static final int LABEL_HEIGHT_MM = 30;
    private static final int PRINT_DPI = 72;
    private static final int LABEL_WIDTH = (int) (LABEL_WIDTH_MM / 25.4 * PRINT_DPI);
    private static final int LABEL_HEIGHT = (int) (LABEL_HEIGHT_MM / 25.4 * PRINT_DPI);
    private static final int MARGIN_X = 2;
    private static final int MARGIN_Y = 2;
    private static final int GAP_Y = 4;

    @GetMapping("/discover")
    @Operation(summary = "发现系统打印机")
    @PreAuthorize("hasAuthority('hardware:epson-test:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> discoverPrinters() {
        List<Map<String, Object>> printers = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        for (PrintService service : services) {
            Map<String, Object> printerInfo = new HashMap<>();
            printerInfo.put("name", service.getName());
            printerInfo.put("isDefault", service.equals(defaultPrinter));
            // 添加打印机详细信息
            try {
                // 检查打印机是否接受任务
                PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) service.getAttribute(PrinterIsAcceptingJobs.class);
                printerInfo.put("acceptingJobs", accepting == PrinterIsAcceptingJobs.ACCEPTING_JOBS);
                // 检查打印机状态
                javax.print.attribute.standard.PrinterState state = (javax.print.attribute.standard.PrinterState) service.getAttribute(javax.print.attribute.standard.PrinterState.class);
                printerInfo.put("state", state != null ? state.toString() : "UNKNOWN");
                // 检查打印机是否支持颜色
                javax.print.attribute.standard.ColorSupported colorSupported = (javax.print.attribute.standard.ColorSupported) service.getAttribute(javax.print.attribute.standard.ColorSupported.class);
                printerInfo.put("colorSupported", colorSupported == javax.print.attribute.standard.ColorSupported.SUPPORTED);
                // 获取支持的文档格式
                DocFlavor[] supportedFlavors = service.getSupportedDocFlavors();
                List<String> flavors = new ArrayList<>();
                if (supportedFlavors != null) {
                    for (DocFlavor flavor : supportedFlavors) {
                        if (flavor != null) {
                            flavors.add(flavor.getMimeType());
                        }
                    }
                }
                printerInfo.put("supportedFormats", flavors);
            } catch (Exception e) {
                log.warn("获取打印机 {} 详细信息失败: {}", service.getName(), e.getMessage());
                printerInfo.put("error", e.getMessage());
            }
            printers.add(printerInfo);
        }
        log.info("发现 {} 台打印机", printers.size());
        return Result.success(printers);
    }

    @GetMapping("/diagnose/{printerName}")
    @Operation(summary = "诊断指定打印机")
    @PreAuthorize("hasAuthority('hardware:epson-test:view') or hasAuthority('*')")
    public Result<Map<String, Object>> diagnosePrinter(@PathVariable String printerName) {
        Map<String, Object> diagnosis = new LinkedHashMap<>();
        try {
            // 1. 查找打印机
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService targetPrinter = null;
            for (PrintService service : services) {
                if (service.getName().equals(printerName) || service.getName().equalsIgnoreCase(printerName)) {
                    targetPrinter = service;
                    break;
                }
            }
            if (targetPrinter == null) {
                diagnosis.put("success", false);
                diagnosis.put("error", "未找到打印机: " + printerName);
                diagnosis.put("availablePrinters", Arrays.stream(services).map(PrintService::getName).toArray(String[]::new));
                return Result.success(diagnosis);
            }
            diagnosis.put("printerName", targetPrinter.getName());
            diagnosis.put("success", true);
            // 2. 检查打印机属性
            Map<String, Object> attributes = new LinkedHashMap<>();
            // 是否接受打印任务
            PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) targetPrinter.getAttribute(PrinterIsAcceptingJobs.class);
            attributes.put("acceptingJobs", accepting != null && accepting == PrinterIsAcceptingJobs.ACCEPTING_JOBS);
            // 打印机状态
            javax.print.attribute.standard.PrinterState state = (javax.print.attribute.standard.PrinterState) targetPrinter.getAttribute(javax.print.attribute.standard.PrinterState.class);
            attributes.put("state", state != null ? state.toString() : "UNKNOWN");
            // 打印机状态原因
            javax.print.attribute.standard.PrinterStateReasons reasons = (javax.print.attribute.standard.PrinterStateReasons) targetPrinter.getAttribute(javax.print.attribute.standard.PrinterStateReasons.class);
            if (reasons != null && !reasons.isEmpty()) {
                List<String> reasonList = new ArrayList<>();
                reasons.forEach((reason, severity) -> {
                    reasonList.add(reason.toString() + " (严重性: " + severity + ")");
                });
                attributes.put("stateReasons", reasonList);
            }
            // 颜色支持
            javax.print.attribute.standard.ColorSupported colorSupported = (javax.print.attribute.standard.ColorSupported) targetPrinter.getAttribute(javax.print.attribute.standard.ColorSupported.class);
            attributes.put("colorSupported", colorSupported != null && colorSupported == javax.print.attribute.standard.ColorSupported.SUPPORTED);
            // 打印队列
            javax.print.attribute.standard.QueuedJobCount queuedJobs = (javax.print.attribute.standard.QueuedJobCount) targetPrinter.getAttribute(javax.print.attribute.standard.QueuedJobCount.class);
            attributes.put("queuedJobCount", queuedJobs != null ? queuedJobs.getValue() : 0);
            diagnosis.put("attributes", attributes);
            // 3. 检查支持的文档格式
            DocFlavor[] supportedFlavors = targetPrinter.getSupportedDocFlavors();
            List<String> formats = new ArrayList<>();
            if (supportedFlavors != null) {
                for (DocFlavor flavor : supportedFlavors) {
                    if (flavor != null && flavor.getMimeType() != null) {
                        formats.add(flavor.getMimeType() + " (" + flavor.getRepresentationClassName() + ")");
                    }
                }
            }
            diagnosis.put("supportedFormats", formats);
            // 4. 检查支持的打印属性
            Class<?>[] attributeCategories = targetPrinter.getSupportedAttributeCategories();
            List<String> supportedAttrs = new ArrayList<>();
            if (attributeCategories != null) {
                for (Class<?> category : attributeCategories) {
                    if (category != null) {
                        supportedAttrs.add(category.getSimpleName());
                    }
                }
            }
            diagnosis.put("supportedAttributes", supportedAttrs);
            // 5. 测试打印能力
            Map<String, Object> capabilities = new LinkedHashMap<>();
            // 测试是否支持GIF格式(用于图形打印)
            boolean supportsGIF = false;
            boolean supportsPNG = false;
            boolean supportsJPG = false;
            if (supportedFlavors != null) {
                for (DocFlavor flavor : supportedFlavors) {
                    if (flavor != null) {
                        String mimeType = flavor.getMimeType();
                        if (mimeType != null) {
                            if (mimeType.contains("image/gif")) supportsGIF = true;
                            if (mimeType.contains("image/png")) supportsPNG = true;
                            if (mimeType.contains("image/jpeg")) supportsJPG = true;
                        }
                    }
                }
            }
            capabilities.put("supportsGIF", supportsGIF);
            capabilities.put("supportsPNG", supportsPNG);
            capabilities.put("supportsJPG", supportsJPG);
            capabilities.put("supportsGraphicsPrinting", supportsGIF || supportsPNG || supportsJPG);
            diagnosis.put("capabilities", capabilities);
            // 6. 总体评估
            List<String> issues = new ArrayList<>();
            if (!Boolean.TRUE.equals(attributes.get("acceptingJobs"))) {
                issues.add("打印机不接受打印任务");
            }
            if ("STOPPED".equals(attributes.get("state"))) {
                issues.add("打印机已停止");
            }
            if (attributes.get("stateReasons") != null) {
                issues.add("打印机有状态警告");
            }
            if (!Boolean.TRUE.equals(capabilities.get("supportsGraphicsPrinting"))) {
                issues.add("打印机可能不支持图形打印");
            }
            diagnosis.put("issues", issues);
            diagnosis.put("healthy", issues.isEmpty());
            log.info("打印机诊断完成: {}, 健康: {}", printerName, issues.isEmpty());
        } catch (Exception e) {
            log.error("诊断打印机失败: {}", printerName, e);
            diagnosis.put("success", false);
            diagnosis.put("error", e.getMessage());
        }
        return Result.success(diagnosis);
    }

    @PostMapping("/test-print/{printerName}")
    @Operation(summary = "测试打印 - 发送测试页")
    @PreAuthorize("hasAuthority('hardware:epson-test:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> testPrint(@PathVariable String printerName) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            log.info("========== 开始测试打印 ==========");
            log.info("目标打印机: {}", printerName);
            // 1. 查找打印机
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            final PrintService targetPrinter;
            PrintService foundPrinter = null;
            for (PrintService service : services) {
                if (service.getName().equals(printerName) || service.getName().equalsIgnoreCase(printerName)) {
                    foundPrinter = service;
                    break;
                }
            }
            targetPrinter = foundPrinter;
            if (targetPrinter == null) {
                result.put("success", false);
                result.put("error", "未找到打印机: " + printerName);
                result.put("availablePrinters", Arrays.stream(services).map(PrintService::getName).toArray(String[]::new));
                return Result.success(result);
            }
            result.put("printerName", targetPrinter.getName());
            // 2. 验证打印机状态
            log.info("验证打印机状态...");
            validatePrinterStatus(targetPrinter);
            result.put("statusValidated", true);
            // 3. 创建简单的测试打印任务
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(targetPrinter);
            job.setJobName("测试打印页");
            // 使用默认页面格式
            PageFormat pageFormat = job.defaultPage();
            job.setPrintable(new Printable() {
                @Override
                public int print(Graphics g, PageFormat pf, int pageIndex) {
                    if (pageIndex > 0) {
                        return NO_SUCH_PAGE;
                    }
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    // 绘制白色背景
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, (int) pf.getWidth(), (int) pf.getHeight());
                    // 绘制黑色边框
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(10, 10, (int) pf.getWidth() - 20, (int) pf.getHeight() - 20);
                    // 绘制测试文本
                    g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
                    String testText = "打印机测试页";
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(testText);
                    g2d.drawString(testText, (int) (pf.getWidth() - textWidth) / 2, (int) pf.getHeight() / 2 - 20);
                    // 绘制时间戳
                    g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
                    String timestamp = "打印时间: " + java.time.LocalDateTime.now().toString();
                    textWidth = fm.stringWidth(timestamp);
                    g2d.drawString(timestamp, (int) (pf.getWidth() - textWidth) / 2, (int) pf.getHeight() / 2 + 20);
                    // 绘制打印机名称
                    String printerInfo = "打印机: " + targetPrinter.getName();
                    textWidth = fm.stringWidth(printerInfo);
                    g2d.drawString(printerInfo, (int) (pf.getWidth() - textWidth) / 2, (int) pf.getHeight() / 2 + 50);
                    return PAGE_EXISTS;
                }
            }, pageFormat);
            // 4. 执行打印
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            log.info("发送测试打印任务...");
            job.print(attrs);
            result.put("success", true);
            result.put("message", "测试打印任务已成功发送到打印机");
            log.info("========== 测试打印成功 ==========");
        } catch (Exception e) {
            log.error("测试打印失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
        }
        return Result.success(result);
    }

    @PostMapping("/test-connection")
    @Operation(summary = "测试打印机连接")
    @PreAuthorize("hasAuthority('hardware:epson-test:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> testConnection(@RequestParam String ipAddress, @RequestParam(defaultValue = "10") int timeout) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("step1_ping", testPing(ipAddress));
            result.put("step2_wsd", testWsdService(ipAddress, timeout));
            result.put("step3_system_printer", findSystemPrinter(ipAddress));
            boolean overallSuccess = (boolean) ((Map<?, ?>) result.get("step1_ping")).get("success");
            result.put("overall_success", overallSuccess);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("overall_success", false);
        }
        return Result.success(result);
    }

    @PostMapping("/preview-label")
    @Operation(summary = "生成标签预览图片")
    @PreAuthorize("hasAuthority('hardware:epson-test:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> previewLabel(@RequestBody Map<String, String> params) {
        String traceCode = params.get("traceCode");
        String materialName = params.get("materialName");
        String batchNo = params.get("batchNo");
        String expiryDate = params.get("expiryDate");
        if (traceCode == null || traceCode.isEmpty()) {
            log.error("缺少必填参数：traceCode");
            return ResponseEntity.badRequest().build();
        }
        try {
            BufferedImage image = createLabelImage(traceCode, materialName, batchNo, expiryDate);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("生成预览图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/preview-page")
    @Operation(summary = "生成打印预览页面（多标签）")
    @PreAuthorize("hasAuthority('hardware:epson-test:manage') or hasAuthority('*')")
    public ResponseEntity<byte[]> previewPage(@RequestBody List<Map<String, Object>> labels) {
        if (labels == null || labels.isEmpty()) {
            log.warn("预览页面请求：标签列表为空");
            return ResponseEntity.badRequest().build();
        }
        try {
            log.info("开始生成预览页面，标签数量: {}", labels.size());
            BufferedImage pageImage = createPageImage(labels);
            if (pageImage == null) {
                log.error("生成预览页面失败：图像对象为null");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(pageImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            log.info("预览页面生成成功，图片大小: {} bytes", imageBytes.length);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("生成预览页面失败，异常类型: {}, 消息: {}", e.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/print-labels")
    @Operation(summary = "打印多个标签")
    @PreAuthorize("hasAuthority('hardware:epson-test:manage') or hasAuthority('*')")
    public Result<String> printLabels(@RequestParam(required = false) String ipAddress, @RequestBody List<Map<String, Object>> labels, @RequestParam(required = false) String printerName) {
        log.info("收到打印请求，标签数量: {}, 打印机: {}", labels.size(), printerName);
        try {
            PrintService targetPrinter = findPrinter(ipAddress, printerName);
            if (targetPrinter == null) {
                log.warn("未找到打印机");
                return Result.error("未找到打印机，请检查打印机是否已连接并安装驱动");
            }
            log.info("找到打印机: {}", targetPrinter.getName());
            final List<LabelData> labelDataList = new ArrayList<>();
            for (Map<String, Object> label : labels) {
                LabelData data = new LabelData();
                data.traceCode = getStringValue(label, "traceCode");
                data.materialName = getStringValue(label, "materialName");
                data.batchNo = getStringValue(label, "batchNo");
                data.expiryDate = getStringValue(label, "expiryDate");
                data.generateTime = getStringValue(label, "generateTime");
                Object shelfLifeObj = label.get("shelfLifeDays");
                if (shelfLifeObj instanceof Number) {
                    data.shelfLifeDays = ((Number) shelfLifeObj).intValue();
                } else if (shelfLifeObj instanceof String) {
                    try {
                        data.shelfLifeDays = Integer.parseInt((String) shelfLifeObj);
                    } catch (NumberFormatException e) {
                        data.shelfLifeDays = null;
                    }
                }
                data.supplierName = getStringValue(label, "supplierName");
                data.storeName = getStringValue(label, "storeName");
                labelDataList.add(data);
            }
            int successCount = 0;
            int failCount = 0;
            for (LabelData labelData : labelDataList) {
                try {
                    printSingleLabelToPrinter(targetPrinter, labelData);
                    successCount++;
                    log.info("标签打印成功: {}", labelData.traceCode);
                } catch (Exception e) {
                    failCount++;
                    log.error("标签打印失败: {}, 错误: {}", labelData.traceCode, e.getMessage());
                }
            }
            String message = String.format("打印完成: 成功 %d 个, 失败 %d 个, 打印机: %s", successCount, failCount, targetPrinter.getName());
            log.info(message);
            return Result.success(message);
        } catch (Exception e) {
            log.error("打印标签失败", e);
            return Result.error("打印失败: " + e.getMessage());
        }
    }

    private void printSingleLabelToPrinter(PrintService printer, LabelData data) throws PrinterException {
        log.info("========== 开始打印标签 ==========");
        log.info("打印机名称: {}", printer.getName());
        log.info("追溯码: {}", data.traceCode);
        // 1. 验证打印机状态
        validatePrinterStatus(printer);
        // 2. 创建打印任务
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(printer);
        job.setJobName("标签-" + data.traceCode);
        // 3. 设置页面格式
        PageFormat pageFormat = job.defaultPage();
        Paper paper = new Paper();
        double paperWidth = LABEL_WIDTH_MM / 25.4 * 72;
        double paperHeight = LABEL_HEIGHT_MM / 25.4 * 72;
        paper.setSize(paperWidth, paperHeight);
        paper.setImageableArea(0, 0, paperWidth, paperHeight);
        pageFormat.setPaper(paper);
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        log.info("纸张尺寸: {}mm x {}mm ({} x {} points)", LABEL_WIDTH_MM, LABEL_HEIGHT_MM, paperWidth, paperHeight);
        // 4. 设置打印内容
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) {
                if (pageIndex > 0) {
                    log.debug("打印页面索引: {}, 返回 NO_SUCH_PAGE", pageIndex);
                    return NO_SUCH_PAGE;
                }
                log.debug("开始绘制打印内容, 页面索引: {}", pageIndex);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, (int) pf.getWidth(), (int) pf.getHeight());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(0, 0, (int) pf.getWidth() - 1, (int) pf.getHeight() - 1);
                drawSingleLabel(g2d, 2, 2, LABEL_WIDTH - 4, LABEL_HEIGHT - 4, data);
                log.debug("打印内容绘制完成");
                return PAGE_EXISTS;
            }
        }, pageFormat);
        // 5. 设置打印属性
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies(1));
        attrs.add(OrientationRequested.PORTRAIT);
        // 6. 执行打印
        try {
            log.info("发送打印任务到打印机...");
            job.print(attrs);
            log.info("打印任务已发送: {} -> {}", data.traceCode, printer.getName());
            log.info("========== 打印任务发送完成 ==========");
        } catch (PrinterException e) {
            log.error("打印任务执行失败: {}", e.getMessage(), e);
            throw new PrinterException("打印失败: " + e.getMessage());
        }
    }

    /**
     * 验证打印机状态
     */
    private void validatePrinterStatus(PrintService printer) throws PrinterException {
        log.info("验证打印机状态...");
        // 检查打印机是否接受打印任务
        if (!printer.isAttributeCategorySupported(PrinterIsAcceptingJobs.class)) {
            log.warn("打印机不支持 PrinterIsAcceptingJobs 属性检查");
        } else {
            PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) printer.getAttribute(PrinterIsAcceptingJobs.class);
            if (accepting == PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
                throw new PrinterException("打印机当前不接受打印任务，请检查打印机是否脱机或暂停");
            }
            log.info("打印机状态: 接受打印任务");
        }
        // 检查打印机状态
        try {
            javax.print.attribute.standard.PrinterState state = (javax.print.attribute.standard.PrinterState) printer.getAttribute(javax.print.attribute.standard.PrinterState.class);
            if (state != null) {
                log.info("打印机状态: {}", state);
                if (state == javax.print.attribute.standard.PrinterState.STOPPED) {
                    throw new PrinterException("打印机已停止，请检查打印机状态");
                }
            }
        } catch (Exception e) {
            log.warn("无法获取打印机状态: {}", e.getMessage());
        }
        // 检查打印机是否有纸张
        try {
            javax.print.attribute.standard.PrinterMoreInfoManufacturer moreInfo = (javax.print.attribute.standard.PrinterMoreInfoManufacturer) printer.getAttribute(javax.print.attribute.standard.PrinterMoreInfoManufacturer.class);
            log.debug("打印机更多信息: {}", moreInfo);
        } catch (Exception e) {
            log.debug("无法获取打印机更多信息");
        }
        log.info("打印机状态验证通过");
    }

    @PostMapping("/print-single-label")
    @Operation(summary = "打印单个标签")
    @PreAuthorize("hasAuthority('hardware:epson-test:manage') or hasAuthority('*')")
    public Result<String> printSingleLabel(@RequestParam String ipAddress, @RequestParam String traceCode, @RequestParam String materialName, @RequestParam(required = false) String batchNo, @RequestParam(required = false) String expiryDate, @RequestParam(required = false) String printerName) {
        try {
            PrintService targetPrinter = findPrinter(ipAddress, printerName);
            if (targetPrinter == null) {
                return Result.error("未找到打印机");
            }
            List<LabelData> labelDataList = new ArrayList<>();
            LabelData data = new LabelData();
            data.traceCode = traceCode;
            data.materialName = materialName;
            data.batchNo = batchNo;
            data.expiryDate = expiryDate;
            labelDataList.add(data);
            printWithGraphics(targetPrinter, "标签-" + traceCode, (g, pf) -> {
                drawMultipleLabels(g, pf, labelDataList);
            });
            return Result.success("标签已打印: " + targetPrinter.getName());
        } catch (Exception e) {
            log.error("打印标签失败", e);
            return Result.error("打印失败: " + e.getMessage());
        }
    }

    private BufferedImage createLabelImage(String traceCode, String materialName, String batchNo, String expiryDate) {
        BufferedImage image = new BufferedImage(LABEL_WIDTH, LABEL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, LABEL_WIDTH, LABEL_HEIGHT);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, LABEL_WIDTH - 1, LABEL_HEIGHT - 1);
        LabelData data = new LabelData();
        data.traceCode = traceCode;
        data.materialName = materialName;
        data.batchNo = batchNo;
        data.expiryDate = expiryDate;
        drawSingleLabel(g, 2, 2, LABEL_WIDTH - 4, LABEL_HEIGHT - 4, data);
        g.dispose();
        return image;
    }

    private BufferedImage createPageImage(List<Map<String, Object>> labels) {
        if (labels == null || labels.isEmpty()) {
            log.warn("createPageImage: 标签列表为空，返回空白图像");
            return new BufferedImage(LABEL_WIDTH, LABEL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        }
        int totalLabels = labels.size();
        int pageWidth = LABEL_WIDTH + MARGIN_X * 2;
        int pageHeight = totalLabels * LABEL_HEIGHT + (totalLabels - 1) * GAP_Y + MARGIN_Y * 2;
        BufferedImage image = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = null;
        try {
            g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, pageWidth, pageHeight);
            List<LabelData> labelDataList = new ArrayList<>();
            for (Map<String, Object> label : labels) {
                LabelData data = new LabelData();
                data.traceCode = getStringValue(label, "traceCode");
                data.materialName = getStringValue(label, "materialName");
                data.batchNo = getStringValue(label, "batchNo");
                data.expiryDate = getStringValue(label, "expiryDate");
                data.generateTime = getStringValue(label, "generateTime");
                Object shelfLifeObj = label.get("shelfLifeDays");
                if (shelfLifeObj instanceof Number) {
                    data.shelfLifeDays = ((Number) shelfLifeObj).intValue();
                } else if (shelfLifeObj instanceof String && !((String) shelfLifeObj).isEmpty()) {
                    try {
                        data.shelfLifeDays = Integer.parseInt((String) shelfLifeObj);
                    } catch (NumberFormatException e) {
                        log.warn("无效的保质期天数: {}", shelfLifeObj);
                    }
                }
                data.supplierName = getStringValue(label, "supplierName");
                data.storeName = getStringValue(label, "storeName");
                labelDataList.add(data);
            }
            for (int i = 0; i < labelDataList.size(); i++) {
                int x = MARGIN_X;
                int y = MARGIN_Y + i * (LABEL_HEIGHT + GAP_Y);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, LABEL_WIDTH, LABEL_HEIGHT);
                drawSingleLabel(g, x + 2, y + 2, LABEL_WIDTH - 4, LABEL_HEIGHT - 4, labelDataList.get(i));
            }
            return image;
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
    }

    /**
     * 获取可用字体，带回退机制
     */
    private Font getAvailableFont(int style, int size) {
        String[] fontNames = {FONT_NAME, "SimHei", "SimSun", "Arial Unicode MS", "Dialog"};
        for (String fontName : fontNames) {
            Font font = new Font(fontName, style, size);
            // 检查字体是否可用
            if (!font.getFamily().equalsIgnoreCase("Dialog") || fontName.equals("Dialog")) {
                return font;
            }
        }
        // 默认返回系统默认字体
        return new Font(Font.SANS_SERIF, style, size);
    }

    private void drawSingleLabel(Graphics2D g, int x, int y, int width, int height, LabelData data) {
        if (data == null) {
            log.warn("drawSingleLabel: LabelData为null");
            return;
        }
        int margin = 2;
        int lineHeight = 7;
        int qrSize = 35;
        int leftWidth = width - qrSize - margin * 3;
        // 绘制背景和边框
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width - 1, height - 1);
        int textX = x + margin;
        int textY = y + margin + 7 + lineHeight;
        // 绘制物料名称
        g.setFont(getAvailableFont(Font.BOLD, 8));
        String name = truncateText(g, data.materialName != null ? data.materialName : "食品追溯", leftWidth - 4);
        g.drawString(name, textX, textY);
        textY += lineHeight + 2;
        // 绘制字段信息
        g.setFont(getAvailableFont(Font.PLAIN, 6));
        String[][] fields = {{"门店:", truncateText(g, data.storeName != null ? data.storeName : "", leftWidth - 22)}, {"保质:", data.shelfLifeDays != null ? data.shelfLifeDays + "天" : "-"}, {"入库:", truncateText(g, formatDate(data.generateTime), leftWidth - 22)}, {"到期:", truncateText(g, formatDate(data.expiryDate), leftWidth - 22)}, {"供应:", truncateText(g, data.supplierName != null ? data.supplierName : "", leftWidth - 22)}};
        for (String[] field : fields) {
            g.setFont(getAvailableFont(Font.BOLD, 6));
            g.drawString(field[0], textX, textY);
            g.setFont(getAvailableFont(Font.PLAIN, 6));
            g.drawString(field[1], textX + 20, textY);
            textY += lineHeight;
        }
        // 绘制二维码
        int qrX = x + width - qrSize - margin;
        int qrY = y + margin;
        drawQRCode(g, data.traceCode, qrX, qrY, qrSize);
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "-";
        }
        if (dateStr.length() >= 10) {
            return dateStr.substring(0, 10);
        }
        return dateStr;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String truncateText(Graphics2D g, String text, int maxWidth) {
        // 空值检查
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (g == null) {
            log.warn("truncateText: Graphics2D对象为null");
            return text;
        }
        try {
            FontMetrics fm = g.getFontMetrics();
            if (fm == null) {
                return text;
            }
            if (fm.stringWidth(text) <= maxWidth) {
                return text;
            }
            String truncated = text;
            while (fm.stringWidth(truncated + "...") > maxWidth && truncated.length() > 1) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            return truncated + "...";
        } catch (Exception e) {
            log.error("截断文本失败: {}", e.getMessage());
            return text;
        }
    }

    private void drawQRCode(Graphics2D g, String content, int x, int y, int size) {
        if (content == null || content.isEmpty()) {
            content = "N/A";
            log.warn("drawQRCode: 内容为空，使用默认值 N/A");
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            g.setColor(Color.WHITE);
            g.fillRect(x, y, size, size);
            g.setColor(Color.BLACK);
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (bitMatrix.get(i, j)) {
                        g.fillRect(x + i, y + j, 1, 1);
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成二维码失败，内容: {}, 错误: {}", content, e.getMessage(), e);
            // 绘制占位符
            try {
                g.setColor(Color.WHITE);
                g.fillRect(x, y, size, size);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, size - 1, size - 1);
                g.setFont(getAvailableFont(Font.PLAIN, 8));
                g.drawString("QR", x + size / 2 - 5, y + size / 2 + 3);
            } catch (Exception ex) {
                log.error("绘制二维码占位符失败: {}", ex.getMessage());
            }
        }
    }

    private void drawMultipleLabels(Graphics2D g, PageFormat pf, List<LabelData> labels) {
        double pageX = pf.getImageableX();
        double pageY = pf.getImageableY();
        g.setColor(Color.WHITE);
        g.fillRect((int) pageX, (int) pageY, (int) pf.getImageableWidth(), (int) pf.getImageableHeight());
        int labelIndex = 0;
        int x = (int) pageX + MARGIN_X;
        int y = (int) pageY + MARGIN_Y;
        while (labelIndex < labels.size()) {
            g.setColor(Color.BLACK);
            g.drawRect(x, y, LABEL_WIDTH, LABEL_HEIGHT);
            drawSingleLabel(g, x + 2, y + 2, LABEL_WIDTH - 4, LABEL_HEIGHT - 4, labels.get(labelIndex));
            labelIndex++;
        }
    }

    private void printWithGraphics(PrintService printer, String jobName, GraphicsPrinter drawer) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(printer);
        job.setJobName(jobName);
        PageFormat pageFormat = job.defaultPage();
        Paper paper = new Paper();
        double paperWidth = LABEL_WIDTH_MM / 25.4 * 72;
        double paperHeight = LABEL_HEIGHT_MM / 25.4 * 72;
        paper.setSize(paperWidth, paperHeight);
        paper.setImageableArea(0, 0, paperWidth, paperHeight);
        pageFormat.setPaper(paper);
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        Book book = new Book();
        book.append(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) {
                if (pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawer.draw(g2d, pf);
                return PAGE_EXISTS;
            }
        }, pageFormat);
        job.setPageable(book);
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies(1));
        attrs.add(OrientationRequested.PORTRAIT);
        job.print(attrs);
        log.info("打印任务已发送: {} -> {}", jobName, printer.getName());
    }

    private void drawCode128Barcode(Graphics2D g, String code, int x, int y, int width, int height) {
        String barcodeData = code.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (barcodeData.isEmpty()) {
            barcodeData = "UNKNOWN";
        }
        int barWidth = Math.max(1, width / (barcodeData.length() * 11 + 35));
        int totalBars = barcodeData.length() * 11 + 35;
        int totalWidth = totalBars * barWidth;
        int startX = x + (width - totalWidth) / 2;
        if (startX < x) startX = x;
        StringBuilder pattern = new StringBuilder();
        pattern.append("11010000100");
        for (char c : barcodeData.toCharArray()) {
            pattern.append(getCode128Pattern(c));
        }
        pattern.append("1100011101011");
        int currentX = startX;
        boolean isBlack = true;
        for (int i = 0; i < pattern.length() && currentX < x + width; i++) {
            int count = pattern.charAt(i) == '1' ? 2 : 1;
            for (int j = 0; j < count && currentX < x + width; j++) {
                if (isBlack) {
                    g.fillRect(currentX, y, barWidth, height);
                }
                currentX += barWidth;
            }
            isBlack = !isBlack;
        }
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 6));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(code);
        g.drawString(code, x + (width - textWidth) / 2, y + height + 8);
    }

    private String getCode128Pattern(char c) {
        String[] patterns = new String[128];
        for (int i = 0; i <= 9; i++) {
            patterns['0' + i] = getPatternForValue(i + 16);
        }
        for (int i = 0; i < 26; i++) {
            patterns['A' + i] = getPatternForValue(i + 48);
        }
        if (patterns[c] != null) {
            return patterns[c];
        }
        return "10101111000";
    }

    private String getPatternForValue(int value) {
        String[] code128B = {"11010010000", "11010011000", "11010011100", "11010011110", "11010011010", "11010011011", "11010011101", "11010011111", "11010010010", "11010010011", "11010010101", "11010010111", "11010010001", "11010010011", "11010010110", "11010010111", "11010011100", "11010011101", "11010011110", "11010011111", "11011010000", "11011011000", "11011011100", "11011011110", "11011011010", "11011011011", "11011011101", "11011011111", "11011010010", "11011010011", "11011010101", "11011010111", "11011010001", "11011010011", "11011010110", "11011010111", "11011011100", "11011011101", "11011011110", "11011011111", "11101010000", "11101011000", "11101011100", "11101011110", "11101011010", "11101011011", "11101011101", "11101011111", "11101010010", "11101010011", "11101010101", "11101010111", "11101010001", "11101010011", "11101010110", "11101010111", "11101011100", "11101011101", "11101011110", "11101011111", "11101101000", "11101101100", "11101101110", "11101101111", "11101101010", "11101101011", "11101101101", "11101101111", "11101101001", "11101101011", "11101101101", "11101101111", "11101101001", "11101101011", "11101101101", "11101101111", "11101101001", "11101101011", "11101101101", "11101101111"};
        if (value >= 0 && value < code128B.length) {
            return code128B[value];
        }
        return "11010000100";
    }


    private static class LabelData {
        String traceCode;
        String materialName;
        String batchNo;
        String expiryDate;
        String generateTime;
        Integer shelfLifeDays;
        String supplierName;
        String storeName;
    }


    @FunctionalInterface
    private interface GraphicsPrinter {
        void draw(Graphics2D g, PageFormat pf);
    }

    private Map<String, Object> testPing(String ipAddress) {
        Map<String, Object> pingResult = new HashMap<>();
        try {
            long start = System.currentTimeMillis();
            boolean reachable = java.net.InetAddress.getByName(ipAddress).isReachable(5000);
            long time = System.currentTimeMillis() - start;
            pingResult.put("success", reachable);
            pingResult.put("responseTime", time + "ms");
            pingResult.put("message", reachable ? "设备可达" : "设备不可达");
        } catch (Exception e) {
            pingResult.put("success", false);
            pingResult.put("error", e.getMessage());
        }
        return pingResult;
    }

    private Map<String, Object> testWsdService(String ipAddress, int timeout) {
        Map<String, Object> wsdResult = new HashMap<>();
        try {
            String wsdUrl = "http://" + ipAddress + "/wsd/PrintService";
            java.net.URL url = new java.net.URL(wsdUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout * 1000);
            connection.setReadTimeout(timeout * 1000);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            wsdResult.put("success", responseCode < 500);
            wsdResult.put("responseCode", responseCode);
            wsdResult.put("message", responseCode < 500 ? "WSD服务可用" : "WSD服务不可用");
        } catch (Exception e) {
            wsdResult.put("success", false);
            wsdResult.put("error", e.getMessage());
            wsdResult.put("message", "WSD服务检测失败，但可能支持其他打印方式");
        }
        return wsdResult;
    }

    private Map<String, Object> findSystemPrinter(String ipAddress) {
        Map<String, Object> result = new HashMap<>();
        List<String> foundPrinters = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            String name = service.getName().toLowerCase();
            if (name.contains("epson") || name.contains("l4366") || name.contains(ipAddress) || name.contains("wsd")) {
                foundPrinters.add(service.getName());
            }
        }
        result.put("found", !foundPrinters.isEmpty());
        result.put("printers", foundPrinters);
        result.put("message", foundPrinters.isEmpty() ? "未在系统中找到匹配的打印机" : "找到匹配的打印机");
        return result;
    }

    private PrintService findPrinter(String ipAddress, String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (printerName != null && !printerName.isEmpty()) {
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(printerName)) {
                    return service;
                }
            }
            for (PrintService service : services) {
                if (service.getName().toLowerCase().contains(printerName.toLowerCase())) {
                    return service;
                }
            }
        }
        if (ipAddress != null && !ipAddress.isEmpty()) {
            for (PrintService service : services) {
                String name = service.getName().toLowerCase();
                if (name.contains("epson") || name.contains("l4366") || name.contains(ipAddress) || name.contains("wsd")) {
                    return service;
                }
            }
        }
        for (PrintService service : services) {
            String name = service.getName().toLowerCase();
            if (name.contains("xp") || name.contains("d35") || name.contains("thermal") || name.contains("label") || name.contains("标签")) {
                return service;
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }
}
