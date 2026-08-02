package com.foodtraceability.printer;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class XprinterSdkService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XprinterSdkService.class);
    private static final int PRINTER_DPI = 300;

    static {
        try {
            String dllPath = "P:\\my-new-project\\芯烨条码热转印打印机SDK开发包Windows SDK 2.3.1\\Windows SDK 2.3.1\\tspl\\lib\\x64\\printer.sdk.dll";
            System.load(dllPath);
            log.info("DLL加载成功: {}", dllPath);
        } catch (UnsatisfiedLinkError e) {
            log.error("DLL加载失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("DLL加载异常: {}", e.getMessage(), e);
        }
    }

    public List<String> listPrinters() {
        try {
            byte[] buffer = new byte[4096];
            IntByReference needSize = new IntByReference();
            int result = PrinterSdk.INSTANCE.ListPrinters(buffer, buffer.length, needSize);
            if (result == 0) {
                String printers = new String(buffer, StandardCharsets.UTF_8).trim();
                List<String> list = new ArrayList<>();
                for (String p : printers.split("\n")) {
                    if (!p.isEmpty()) {
                        list.add(p.trim());
                    }
                }
                return list;
            }
        } catch (Exception e) {
            log.error("Failed to list printers", e);
        }
        return new ArrayList<>();
    }

    public boolean printLabel(String printerName, int labelWidthMm, int labelHeightMm, List<Map<String, Object>> elements, Map<String, Object> data) {
        log.info("开始SDK打印: printerName={}, size={}x{}mm, elements={}", printerName, labelWidthMm, labelHeightMm, elements.size());
        PointerByReference handleRef = new PointerByReference();
        int result;
        try {
            result = PrinterSdk.INSTANCE.PrinterCreator(handleRef, "Xprinter XP-D35E");
            log.info("PrinterCreator result: {}", result);
        } catch (Exception e) {
            log.error("PrinterCreator异常: {}", e.getMessage(), e);
            return false;
        }
        if (result != 0) {
            log.error("PrinterCreator failed: {}", result);
            return false;
        }
        com.sun.jna.Pointer handle = handleRef.getValue();
        if (handle == null) {
            log.error("Failed to create printer handle");
            return false;
        }
        try {
            result = PrinterSdk.INSTANCE.OpenPortA(handle, printerName);
            if (result != 0) {
                log.error("OpenPortA failed: {}", result);
                return false;
            }
            int labelWidthDots = labelWidthMm * 12;
            int labelHeightDots = labelHeightMm * 12;
            result = PrinterSdk.INSTANCE.TSPL_Setup(handle, 3, 12, labelWidthDots, labelHeightDots, 1, 24, 0);
            if (result != 0) {
                log.error("TSPL_Setup failed: {}", result);
                return false;
            }
            result = PrinterSdk.INSTANCE.TSPL_ClearBuffer(handle);
            if (result != 0) {
                log.error("TSPL_ClearBuffer failed: {}", result);
                return false;
            }
            double mmToDots = 12;
            for (Map<String, Object> element : elements) {
                renderElement(handle, element, data, mmToDots);
            }
            result = PrinterSdk.INSTANCE.TSPL_Print(handle, 1, 1);
            if (result != 0) {
                log.error("TSPL_Print failed: {}", result);
                return false;
            }
            return true;
        } finally {
            if (handle != null) {
                PrinterSdk.INSTANCE.ClosePort(handle);
                PrinterSdk.INSTANCE.ReleasePrinter(handle);
            }
        }
    }

    private void renderElement(com.sun.jna.Pointer handle, Map<String, Object> element, Map<String, Object> data, double mmToDots) {
        String type = (String) element.get("type");
        int x = (int) Math.round(getDouble(element, "x", 0) * mmToDots);
        int y = (int) Math.round(getDouble(element, "y", 0) * mmToDots);
        switch (type) {
        case "text": 
            renderTextAsBitmap(handle, element, data, x, y);
            break;
        case "qrcode": 
            renderQrCode(handle, element, data, x, y);
            break;
        case "barcode": 
            renderBarCode(handle, element, data, x, y);
            break;
        case "rect": 
            renderRect(handle, element, x, y, mmToDots);
            break;
        case "line": 
            renderLine(handle, element, x, y, mmToDots);
            break;
        }
    }

    private void renderTextAsBitmap(com.sun.jna.Pointer handle, Map<String, Object> element, Map<String, Object> data, int x, int y) {
        String textFieldType = (String) element.get("textFieldType");
        String customText = (String) element.get("customText");
        Integer fontSize = getInteger(element, "fontSize", 24);
        String prefix = (String) element.get("prefix");
        if (prefix == null) prefix = "";
        String text = "";
        if ("custom".equals(textFieldType)) {
            text = customText != null ? customText : "";
        } else if (textFieldType != null) {
            text = resolveFieldValue(textFieldType, data);
        }
        if (text == null || text.isEmpty()) return;
        text = prefix + text;
        Map<String, Boolean> fontStyle = (Map<String, Boolean>) element.get("fontStyle");
        boolean bold = fontStyle != null && Boolean.TRUE.equals(fontStyle.get("bold"));
        try {
            BitmapResult result = createMonoBitmap(text, fontSize, bold);
            int ret = PrinterSdk.INSTANCE.TSPL_BitMap(handle, x, y, result.width, result.height, 0, result.data);
            if (ret != 0) {
                log.error("TSPL_BitMap failed: {}", ret);
            }
            log.info("打印文本: text={}, fontSize={}, width={}, height={}, dataSize={}", text, fontSize, result.width, result.height, result.data.length);
        } catch (Exception e) {
            log.error("Failed to render text as bitmap", e);
        }
    }


    private static class BitmapResult {
        int width;
        int height;
        byte[] data;

        BitmapResult(int width, int height, byte[] data) {
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }

    private BitmapResult createMonoBitmap(String text, int fontSize, boolean bold) {
        int fontStyle = bold ? Font.BOLD : Font.PLAIN;
        Font font = new Font("SimHei", fontStyle, fontSize);
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D tempG = tempImage.createGraphics();
        tempG.setFont(font);
        FontMetrics fm = tempG.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int ascent = fm.getAscent();
        tempG.dispose();
        int printerWidth = ((textWidth + 7) / 8) * 8;
        int printerHeight = textHeight;
        BufferedImage colorImage = new BufferedImage(printerWidth, printerHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = colorImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, printerWidth, printerHeight);
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int drawX = (printerWidth - textWidth) / 2;
        g.drawString(text, drawX, ascent);
        g.dispose();
        int bytesPerRow = printerWidth / 8;
        byte[] bitmapData = new byte[bytesPerRow * printerHeight];
        for (int row = 0; row < printerHeight; row++) {
            for (int col = 0; col < printerWidth; col++) {
                int rgb = colorImage.getRGB(col, row);
                int r = (rgb >> 16) & 255;
                int g_val = (rgb >> 8) & 255;
                int b = rgb & 255;
                int gray = (r + g_val + b) / 3;
                if (gray < 128) {
                    int byteIndex = row * bytesPerRow + col / 8;
                    int bitIndex = 7 - (col % 8);
                    bitmapData[byteIndex] |= (byte) (1 << bitIndex);
                }
            }
        }
        log.info("生成单色位图: fontSize={}, printerWidth={}, printerHeight={}, bytesPerRow={}, dataSize={}", fontSize, printerWidth, printerHeight, bytesPerRow, bitmapData.length);
        return new BitmapResult(printerWidth, printerHeight, bitmapData);
    }

    private void renderQrCode(com.sun.jna.Pointer handle, Map<String, Object> element, Map<String, Object> data, int x, int y) {
        String dataField = (String) element.get("dataField");
        String content = (String) data.getOrDefault(dataField, "");
        if (content.isEmpty()) return;
        Integer width = getInteger(element, "width", 4);
        String eccLevel = (String) element.get("errorCorrectionLevel");
        int ecc = 2;
        if ("L".equals(eccLevel)) ecc = 1;
         else if ("Q".equals(eccLevel)) ecc = 3;
         else if ("H".equals(eccLevel)) ecc = 4;
        int result = PrinterSdk.INSTANCE.TSPL_QrCode(handle, x, y, width * 12, ecc, 0, 0, 2, 3, content);
        if (result != 0) {
            log.error("TSPL_QrCode failed: {}", result);
        }
    }

    private void renderBarCode(com.sun.jna.Pointer handle, Map<String, Object> element, Map<String, Object> data, int x, int y) {
        String dataField = (String) element.get("dataField");
        String content = (String) data.getOrDefault(dataField, "");
        if (content.isEmpty()) return;
        Integer height = getInteger(element, "height", 50);
        int result = PrinterSdk.INSTANCE.TSPL_BarCode(handle, x, y, 7, content, height, 0, 0, 2, 2);
        if (result != 0) {
            log.error("TSPL_BarCode failed: {}", result);
        }
    }

    private void renderRect(com.sun.jna.Pointer handle, Map<String, Object> element, int x, int y, double mmToDots) {
        int width = (int) Math.round(getDouble(element, "width", 10) * mmToDots);
        int height = (int) Math.round(getDouble(element, "height", 10) * mmToDots);
        Double borderWidth = getDouble(element, "borderWidth", 0.5);
        int thickness = Math.max(1, (int) Math.round(borderWidth * mmToDots));
        int result = PrinterSdk.INSTANCE.TSPL_Box(handle, x, y, x + width, y + height, thickness, 0);
        if (result != 0) {
            log.error("TSPL_Box failed: {}", result);
        }
    }

    private void renderLine(com.sun.jna.Pointer handle, Map<String, Object> element, int x, int y, double mmToDots) {
        int width = (int) Math.round(getDouble(element, "width", 10) * mmToDots);
        int height = (int) Math.round(getDouble(element, "height", 1) * mmToDots);
        int result = PrinterSdk.INSTANCE.TSPL_Bar(handle, x, y, width, height);
        if (result != 0) {
            log.error("TSPL_Bar failed: {}", result);
        }
    }

    private String resolveFieldValue(String field, Map<String, Object> data) {
        Object value = data.get(field);
        if (value == null) return "";
        return value.toString();
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int getInteger(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
