package com.foodtraceability.controller.integration;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.OCRMatchRequestDTO;
import com.foodtraceability.dto.OCRMatchResultDTO;
import com.foodtraceability.dto.OCRRequestDTO;
import com.foodtraceability.dto.OCRResultDTO;
import com.foodtraceability.service.OCRService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * OCR识别服务控制器
 * 提供商品包装图片识别和档案匹配功能
 */
@RestController
@RequestMapping("/v1/ocr")
@Tag(name = "OCR识别服务", description = "商品包装图片识别和档案匹配")
public class OCRController {

    public OCRController(OCRService ocrService) {
        this.ocrService = ocrService;
    }

    private final OCRService ocrService;

    /**
     * 识别图片中的商品信息
     * @param request OCR识别请求
     * @return 识别结果
     */
    @PostMapping("/recognize")
    @Operation(summary = "识别图片中的商品信息", description = "使用OCR技术识别图片中的商品信息，支持商品包装、发票、凭证等多种类型")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "识别成功"), @ApiResponse(responseCode = "400", description = "请求参数错误"), @ApiResponse(responseCode = "401", description = "未授权"), @ApiResponse(responseCode = "500", description = "服务器内部错误")})
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    public Result<OCRResultDTO> recognizeImage(@Valid @RequestBody OCRRequestDTO request) {
        OCRResultDTO result = ocrService.recognizeImage(request.getImageBase64());
        if (result != null && result.getSuccess() && result.getMaterialName() != null) {
            // 匹配已有档案并设置差异信息
            matchAndCompare(result);
        }
        return Result.success(result);
    }

    /**
     * 匹配已有商品档案
     * @param request OCR匹配请求
     * @return 匹配结果
     */
    @PostMapping("/match")
    @Operation(summary = "匹配已有商品档案", description = "根据商品名称和条形码匹配已有商品档案，并对比差异")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    public Result<OCRMatchResultDTO> matchTemplate(@Valid @RequestBody OCRMatchRequestDTO request) {
        Long matchedId = ocrService.matchExistingTemplate(request.getProductName(), request.getBarcode());
        OCRMatchResultDTO result = new OCRMatchResultDTO();
        result.setMatchedId(matchedId);
        result.setFound(matchedId != null);
        if (matchedId != null && request.getOcrResult() != null) {
            List<String> differences = ocrService.compareWithTemplate(matchedId, request.getOcrResult());
            result.setDifferences(differences);
            result.setHasDifference(!differences.isEmpty());
        }
        return Result.success(result);
    }

    /**
     * 匹配档案并设置差异信息
     * @param result OCR识别结果
     */
    private void matchAndCompare(OCRResultDTO result) {
        Long matchedId = ocrService.matchExistingTemplate(result.getMaterialName(), result.getBarcode());
        if (matchedId != null) {
            result.setMatchedTemplateId(matchedId);
            result.setIsNewProduct(false);
            List<String> differences = ocrService.compareWithTemplate(matchedId, result);
            result.setHasDifference(!differences.isEmpty());
            result.setDifferences(differences.isEmpty() ? null : differences);
        } else {
            result.setIsNewProduct(true);
            result.setHasDifference(false);
        }
    }

}
