package com.foodtraceability.util;

import java.util.HashMap;
import java.util.Map;

public class LabelRenderConfig {
    
    public static final int DEFAULT_DPI = 300;
    public static final double MM_TO_INCH = 25.4;
    public static final int TSPL_FONT_BASE_SIZE = 24;
    
    private final int dpi;
    private final double mmToDots;
    
    public LabelRenderConfig() {
        this(DEFAULT_DPI);
    }
    
    public LabelRenderConfig(int dpi) {
        this.dpi = dpi;
        this.mmToDots = dpi / MM_TO_INCH;
    }
    
    public int mmToDots(double mm) {
        return (int) Math.round(mm * mmToDots);
    }
    
    public double dotsToMm(int dots) {
        return dots / mmToDots;
    }
    
    public int ptToPixels(int pt) {
        return (int) Math.round(pt * dpi / 72.0);
    }
    
    public int getTsplFontScale(int fontSize) {
        if (fontSize >= 14) {
            return 2;
        } else if (fontSize >= 10) {
            return 1;
        }
        return 1;
    }
    
    public int getTsplCharWidth(int fontSize) {
        int scale = getTsplFontScale(fontSize);
        return TSPL_FONT_BASE_SIZE * scale;
    }
    
    public int getTsplCharHeight(int fontSize) {
        int scale = getTsplFontScale(fontSize);
        return TSPL_FONT_BASE_SIZE * scale;
    }
    
    public Map<String, Object> getTextMetrics(String text, int fontSize, boolean bold) {
        Map<String, Object> metrics = new HashMap<>();
        
        int scale = getTsplFontScale(fontSize);
        int charWidth = getTsplCharWidth(fontSize);
        int charHeight = getTsplCharHeight(fontSize);
        
        int actualScaleX = bold ? scale + 1 : scale;
        int actualScaleY = bold ? scale + 1 : scale;
        
        int textWidth = text.length() * charWidth;
        int textHeight = charHeight;
        
        metrics.put("scaleX", actualScaleX);
        metrics.put("scaleY", actualScaleY);
        metrics.put("charWidth", charWidth);
        metrics.put("charHeight", charHeight);
        metrics.put("textWidth", textWidth);
        metrics.put("textHeight", textHeight);
        metrics.put("fontSizePt", fontSize);
        metrics.put("fontSizePx", ptToPixels(fontSize));
        
        return metrics;
    }
    
    public String generateTsplText(int x, int y, String text, int fontSize, boolean bold) {
        int scale = getTsplFontScale(fontSize);
        int actualScaleX = bold ? scale + 1 : scale;
        int actualScaleY = bold ? scale + 1 : scale;
        
        return String.format("TEXT %d,%d,\"TSS24.BF2\",0,%d,%d,\"%s\"\n", 
            x, y, actualScaleX, actualScaleY, escapeTspl(text));
    }
    
    public String generateTsplQrcode(int x, int y, String content, int size) {
        int moduleSize = Math.max(2, size / 25);
        return String.format("QRCODE %d,%d,M,%d,A,0,M2,S3,\"%s\"\n", 
            x, y, moduleSize, escapeTspl(content));
    }
    
    private String escapeTspl(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
    
    public int getDpi() {
        return dpi;
    }
    
    public double getMmToDots() {
        return mmToDots;
    }
}
