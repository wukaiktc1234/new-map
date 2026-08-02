package com.foodtraceability.service.impl;

import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.service.MaterialLabelPrintService;
import com.foodtraceability.service.DevicePrintService;
import com.foodtraceability.dto.LabelPrintConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MaterialLabelPrintServiceImpl implements MaterialLabelPrintService {

    private static final Logger log = LoggerFactory.getLogger(MaterialLabelPrintServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    private final DevicePrintService devicePrintService;

    private AtomicInteger printSequence = new AtomicInteger(0);

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param devicePrintService 设备打印服务，可选依赖
     */
    public MaterialLabelPrintServiceImpl(@Nullable DevicePrintService devicePrintService) {
        this.devicePrintService = devicePrintService;
    }

    @Override
    public Boolean printMaterialLabel(MaterialTraceCode traceCode) {
        return printMaterialLabelWithPeelMode(traceCode, LabelPrintConfigDTO.defaultConfig());
    }
    
    @Override
    public Boolean printMaterialLabelWithPeelMode(MaterialTraceCode traceCode, LabelPrintConfigDTO config) {
        try {
            String printData = generateZPLDataWithPeelMode(traceCode, config);
            log.info("剥离模式打印原料标签: traceCode={}, peelMode={}", traceCode.getTraceCode(), config.isPeelModeEnabled());
            
            if (devicePrintService != null) {
                return devicePrintService.printTraceabilityLabel(traceCode.getTraceCode(), traceCode.getMaterialName());
            }
            
            log.info("打印服务未配置，模拟打印成功");
            return true;
        } catch (Exception e) {
            log.error("剥离模式打印原料标签失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean batchPrintMaterialLabels(List<MaterialTraceCode> traceCodes) {
        return batchPrintMaterialLabelsWithPeelMode(traceCodes, LabelPrintConfigDTO.defaultConfig());
    }
    
    @Override
    public Boolean batchPrintMaterialLabelsWithPeelMode(List<MaterialTraceCode> traceCodes, LabelPrintConfigDTO config) {
        boolean allSuccess = true;
        int totalLabels = traceCodes.size();
        int currentLabel = 0;
        
        for (MaterialTraceCode traceCode : traceCodes) {
            currentLabel++;
            log.info("剥离模式批量打印进度: {}/{}, traceCode={}", currentLabel, totalLabels, traceCode.getTraceCode());
            
            boolean success = printMaterialLabelWithPeelMode(traceCode, config);
            if (!success) {
                allSuccess = false;
                log.warn("标签打印失败: traceCode={}", traceCode.getTraceCode());
                
                if (config.isReprintOnJam()) {
                    log.info("尝试重新打印: traceCode={}", traceCode.getTraceCode());
                    success = printMaterialLabelWithPeelMode(traceCode, config);
                    if (!success) {
                        log.error("重新打印失败: traceCode={}", traceCode.getTraceCode());
                    }
                }
            }
            
            if (success && config.isWaitForPeel() && config.isPeelModeEnabled()) {
                try {
                    Thread.sleep(config.getPeelDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return allSuccess;
    }

    @Override
    public String generateZPLData(MaterialTraceCode traceCode) {
        StringBuilder zpl = new StringBuilder();
        
        zpl.append("^XA\n");
        zpl.append("^CI28\n");
        
        zpl.append("^PW400\n");
        zpl.append("^LL240\n");
        
        zpl.append("^FO20,20^A0N,28,28^FD").append(escapeZPL(traceCode.getMaterialName())).append("^FS\n");
        
        String productionDate = traceCode.getProductionDate() != null ? 
            traceCode.getProductionDate().format(DATE_FORMAT) : "-";
        String expiryDate = traceCode.getExpiryDate() != null ? 
            traceCode.getExpiryDate().format(SHORT_DATE_FORMAT) : "-";
        zpl.append("^FO20,60^A0N,20,20^FD生产: ").append(productionDate);
        zpl.append("  到期: ").append(expiryDate).append("^FS\n");
        
        String weight = traceCode.getWeight() != null ? 
            traceCode.getWeight() + (traceCode.getWeightUnit() != null ? traceCode.getWeightUnit() : "kg") : "-";
        String storage = traceCode.getStorageCondition() != null ? traceCode.getStorageCondition() : "-";
        zpl.append("^FO20,90^A0N,20,20^FD重量: ").append(weight);
        zpl.append("  存储: ").append(storage).append("^FS\n");
        
        zpl.append("^FO20,130^BY2,3,100^BCN,100,Y,N,N^FD").append(traceCode.getTraceCode()).append("^FS\n");
        
        zpl.append("^FO20,200^A0N,18,18^FD").append(traceCode.getTraceCode()).append("^FS\n");
        
        zpl.append("^XZ\n");
        
        return zpl.toString();
    }
    
    @Override
    public String generateZPLDataWithPeelMode(MaterialTraceCode traceCode, LabelPrintConfigDTO config) {
        StringBuilder zpl = new StringBuilder();
        
        zpl.append("^XA\n");
        zpl.append("^CI28\n");
        
        zpl.append("^PW").append(config.getLabelWidth() * 10).append("\n");
        zpl.append("^LL").append(config.getLabelHeight() * 10).append("\n");
        
        if (config.isPeelModeEnabled()) {
            zpl.append("^MMP\n");
            zpl.append("^PON\n");
            log.debug("启用剥离模式: peelMode=true");
        }
        
        if (config.getPrintSpeed() > 0) {
            zpl.append("^PR").append(config.getPrintSpeed()).append("\n");
        }
        
        if (config.getPrintDensity() > 0) {
            zpl.append("~SD").append(config.getPrintDensity()).append("\n");
        }
        
        zpl.append("^FO20,20^A0N,28,28^FD").append(escapeZPL(traceCode.getMaterialName())).append("^FS\n");
        
        String productionDate = traceCode.getProductionDate() != null ? 
            traceCode.getProductionDate().format(DATE_FORMAT) : "-";
        String expiryDate = traceCode.getExpiryDate() != null ? 
            traceCode.getExpiryDate().format(SHORT_DATE_FORMAT) : "-";
        zpl.append("^FO20,60^A0N,20,20^FD生产: ").append(productionDate);
        zpl.append("  到期: ").append(expiryDate).append("^FS\n");
        
        String weight = traceCode.getWeight() != null ? 
            traceCode.getWeight() + (traceCode.getWeightUnit() != null ? traceCode.getWeightUnit() : "kg") : "-";
        String storage = traceCode.getStorageCondition() != null ? traceCode.getStorageCondition() : "-";
        zpl.append("^FO20,90^A0N,20,20^FD重量: ").append(weight);
        zpl.append("  存储: ").append(storage).append("^FS\n");
        
        zpl.append("^FO20,130^BY2,3,100^BCN,100,Y,N,N^FD").append(traceCode.getTraceCode()).append("^FS\n");
        
        zpl.append("^FO20,200^A0N,18,18^FD").append(traceCode.getTraceCode()).append("^FS\n");
        
        if (config.isPeelModeEnabled()) {
            zpl.append("^PQ1,0,0,");
            zpl.append(config.isWaitForPeel() ? "Y" : "N");
            zpl.append("\n");
        } else {
            zpl.append("^PQ").append(config.getCopies()).append("\n");
        }
        
        if (config.isBeepOnComplete()) {
            zpl.append("^HB\n");
        }
        
        zpl.append("^XZ\n");
        
        return zpl.toString();
    }
    
    @Override
    public String generateTSPLDataWithPeelMode(MaterialTraceCode traceCode, LabelPrintConfigDTO config) {
        StringBuilder tspl = new StringBuilder();
        
        tspl.append("SIZE ").append(config.getLabelWidth()).append(" mm, ").append(config.getLabelHeight()).append(" mm\n");
        tspl.append("GAP ").append(config.getGapSize()).append(" mm, 0 mm\n");
        tspl.append("DIRECTION 1\n");
        
        if (config.getPrintSpeed() > 0) {
            tspl.append("SPEED ").append(config.getPrintSpeed()).append("\n");
        }
        if (config.getPrintDensity() > 0) {
            tspl.append("DENSITY ").append(config.getPrintDensity()).append("\n");
        }
        
        if (config.isPeelModeEnabled()) {
            tspl.append("SET PEEL ON\n");
            tspl.append("SET CUTTER OFF\n");
            log.debug("TSPL启用剥离模式: peelMode=true");
        }
        
        tspl.append("CLS\n");
        
        tspl.append("TEXT 20,20,\"2\",0,1,1,\"").append(traceCode.getMaterialName()).append("\"\n");
        
        String productionDate = traceCode.getProductionDate() != null ? 
            traceCode.getProductionDate().format(DATE_FORMAT) : "-";
        String expiryDate = traceCode.getExpiryDate() != null ? 
            traceCode.getExpiryDate().format(SHORT_DATE_FORMAT) : "-";
        tspl.append("TEXT 20,60,\"1\",0,1,1,\"生产: ").append(productionDate);
        tspl.append(" 到期: ").append(expiryDate).append("\"\n");
        
        String weight = traceCode.getWeight() != null ? 
            traceCode.getWeight() + (traceCode.getWeightUnit() != null ? traceCode.getWeightUnit() : "kg") : "-";
        String storage = traceCode.getStorageCondition() != null ? traceCode.getStorageCondition() : "-";
        tspl.append("TEXT 20,90,\"1\",0,1,1,\"重量: ").append(weight);
        tspl.append(" 存储: ").append(storage).append("\"\n");
        
        tspl.append("BARCODE 20,130,\"128\",100,1,0,2,2,\"").append(traceCode.getTraceCode()).append("\"\n");
        
        tspl.append("TEXT 20,200,\"1\",0,1,1,\"").append(traceCode.getTraceCode()).append("\"\n");
        
        if (config.isPeelModeEnabled()) {
            tspl.append("PRINT 1,1\n");
            if (config.isBeepOnComplete()) {
                tspl.append("SOUND 1000,100\n");
            }
        } else {
            tspl.append("PRINT ").append(config.getCopies()).append(",1\n");
        }
        
        return tspl.toString();
    }

    @Override
    public String generateTSPLData(MaterialTraceCode traceCode) {
        StringBuilder tspl = new StringBuilder();
        
        tspl.append("SIZE 40 mm, 24 mm\n");
        tspl.append("GAP 2 mm, 0 mm\n");
        tspl.append("DIRECTION 1\n");
        tspl.append("CLS\n");
        
        tspl.append("TEXT 20,20,\"2\",0,1,1,\"").append(traceCode.getMaterialName()).append("\"\n");
        
        String productionDate = traceCode.getProductionDate() != null ? 
            traceCode.getProductionDate().format(DATE_FORMAT) : "-";
        String expiryDate = traceCode.getExpiryDate() != null ? 
            traceCode.getExpiryDate().format(SHORT_DATE_FORMAT) : "-";
        tspl.append("TEXT 20,60,\"1\",0,1,1,\"生产: ").append(productionDate);
        tspl.append(" 到期: ").append(expiryDate).append("\"\n");
        
        String weight = traceCode.getWeight() != null ? 
            traceCode.getWeight() + (traceCode.getWeightUnit() != null ? traceCode.getWeightUnit() : "kg") : "-";
        String storage = traceCode.getStorageCondition() != null ? traceCode.getStorageCondition() : "-";
        tspl.append("TEXT 20,90,\"1\",0,1,1,\"重量: ").append(weight);
        tspl.append(" 存储: ").append(storage).append("\"\n");
        
        tspl.append("BARCODE 20,130,\"128\",100,1,0,2,2,\"").append(traceCode.getTraceCode()).append("\"\n");
        
        tspl.append("TEXT 20,200,\"1\",0,1,1,\"").append(traceCode.getTraceCode()).append("\"\n");
        
        tspl.append("PRINT 1,1\n");
        
        return tspl.toString();
    }

    @Override
    public String generateHTMLPreview(MaterialTraceCode traceCode) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head><meta charset=\"UTF-8\">\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 10px; }\n");
        html.append(".label { width: 80mm; height: 48mm; border: 1px solid #000; padding: 5mm; box-sizing: border-box; }\n");
        html.append(".name { font-size: 14pt; font-weight: bold; margin-bottom: 3mm; }\n");
        html.append(".info { font-size: 10pt; margin-bottom: 2mm; }\n");
        html.append(".barcode { text-align: center; margin: 3mm 0; }\n");
        html.append(".barcode-img { height: 15mm; }\n");
        html.append(".code { font-size: 10pt; text-align: center; }\n");
        html.append("</style></head><body>\n");
        
        html.append("<div class=\"label\">\n");
        html.append("<div class=\"name\">").append(escapeHTML(traceCode.getMaterialName())).append("</div>\n");
        
        String productionDate = traceCode.getProductionDate() != null ? 
            traceCode.getProductionDate().format(DATE_FORMAT) : "-";
        String expiryDate = traceCode.getExpiryDate() != null ? 
            traceCode.getExpiryDate().format(DATE_FORMAT) : "-";
        html.append("<div class=\"info\">生产日期: ").append(productionDate);
        html.append(" | 到期: ").append(expiryDate).append("</div>\n");
        
        String weight = traceCode.getWeight() != null ? 
            traceCode.getWeight() + (traceCode.getWeightUnit() != null ? traceCode.getWeightUnit() : "kg") : "-";
        String storage = traceCode.getStorageCondition() != null ? traceCode.getStorageCondition() : "-";
        html.append("<div class=\"info\">重量: ").append(weight);
        html.append(" | 存储: ").append(storage).append("</div>\n");
        
        html.append("<div class=\"barcode\">\n");
        html.append("<img src=\"https://barcode.tec-it.com/barcode.ashx?code=Code128&data=");
        html.append(traceCode.getTraceCode()).append("\" class=\"barcode-img\" />\n");
        html.append("</div>\n");
        
        html.append("<div class=\"code\">").append(traceCode.getTraceCode()).append("</div>\n");
        html.append("</div>\n");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    private String escapeZPL(String text) {
        if (text == null) return "";
        return text.replace("_", "_5F").replace("^", "_5E").replace("~", "_7E");
    }
    
    private String escapeHTML(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
