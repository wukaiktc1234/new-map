package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.QuickStockInDTO;
import com.foodtraceability.entity.MaterialTemplate;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.service.MaterialTemplateService;
import com.foodtraceability.service.MaterialTraceCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/quick-stock-in")
@Tag(name = "快速入库管理")
public class QuickStockInController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuickStockInController.class);
    private final MaterialTemplateService materialTemplateService;
    private final MaterialTraceCodeService materialTraceCodeService;

    @PostMapping("/match-barcode")
    @Operation(summary = "匹配商品条码")
    public Result<Map<String, Object>> matchBarcode(@RequestParam String barcode) {
        log.info("匹配商品条码: {}", barcode);
        Map<String, Object> result = new HashMap<>();
        MaterialTemplate template = materialTemplateService.getByBarcode(barcode);
        if (template != null) {
            result.put("matched", true);
            result.put("template", template);
            result.put("message", "匹配成功");
        } else {
            result.put("matched", false);
            result.put("message", "未找到匹配的模板，请手动选择或创建新模板");
        }
        return Result.success(result);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行快速入库")
    public Result<List<MaterialTraceCode>> executeStockIn(@Valid @RequestBody QuickStockInDTO dto) {
        log.info("执行快速入库: {}", dto.getMaterialName());
        try {
            if (dto.getExpiryDate() == null && dto.getProductionDate() != null && dto.getShelfLifeDays() != null) {
                dto.setExpiryDate(dto.getProductionDate().plusDays(dto.getShelfLifeDays()));
            }
            if (dto.getBatchNumber() == null || dto.getBatchNumber().isEmpty()) {
                dto.setBatchNumber("BAT" + System.currentTimeMillis());
            }
            com.foodtraceability.dto.MaterialTraceCodeGenerateDTO generateDTO = new com.foodtraceability.dto.MaterialTraceCodeGenerateDTO();
            generateDTO.setEntryType("quick");
            generateDTO.setStoreId(dto.getStoreId());
            generateDTO.setStoreName(dto.getStoreName());
            generateDTO.setMaterialName(dto.getMaterialName());
            generateDTO.setSupplierId(dto.getSupplierId());
            generateDTO.setSupplierName(dto.getSupplierName());
            generateDTO.setWeight(dto.getWeight());
            generateDTO.setWeightUnit(dto.getWeightUnit());
            generateDTO.setBatchNumber(dto.getBatchNumber());
            generateDTO.setProductionDate(dto.getProductionDate());
            generateDTO.setShelfLifeDays(dto.getShelfLifeDays());
            generateDTO.setStorageCondition(dto.getStorageCondition());
            generateDTO.setRemark(dto.getRemark());
            generateDTO.setGenerateCount(dto.getGenerateCount() != null ? dto.getGenerateCount() : 1);
            List<MaterialTraceCode> codes = materialTraceCodeService.generateBatch(generateDTO);
            log.info("快速入库完成，生成追溯码数量: {}", codes.size());
            return Result.success(codes);
        } catch (Exception e) {
            log.error("快速入库失败", e);
            return Result.error("入库失败: " + e.getMessage());
        }
    }

    @PostMapping("/parse-date")
    @Operation(summary = "解析日期（支持纯数字输入）")
    public Result<Map<String, Object>> parseDate(@RequestParam String input) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cleaned = input.replaceAll("[^0-9]", "");
            LocalDate date = null;
            if (cleaned.length() == 8) {
                String year = cleaned.substring(0, 4);
                String month = cleaned.substring(4, 6);
                String day = cleaned.substring(6, 8);
                date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
            } else if (cleaned.length() == 6) {
                String year = cleaned.substring(0, 4);
                String month = cleaned.substring(4, 6);
                date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
            }
            if (date != null) {
                result.put("success", true);
                result.put("date", date.toString());
                result.put("message", "解析成功");
            } else {
                result.put("success", false);
                result.put("message", "无法解析日期，请检查输入格式");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "日期解析失败: " + e.getMessage());
        }
        return Result.success(result);
    }

    public QuickStockInController(final MaterialTemplateService materialTemplateService, final MaterialTraceCodeService materialTraceCodeService) {
        this.materialTemplateService = materialTemplateService;
        this.materialTraceCodeService = materialTraceCodeService;
    }
}
