package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * OCR匹配请求DTO
 */
@Schema(description = "OCR匹配请求")
public class OCRMatchRequestDTO {
    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", required = true)
    private String productName;
    @Pattern(regexp = "^\\d{8,14}$", message = "条码格式不正确，应为8-14位数字")
    @Schema(description = "条形码（8-14位数字）")
    private String barcode;
    @NotNull(message = "OCR结果不能为空")
    @Schema(description = "OCR识别结果", required = true)
    private OCRResultDTO ocrResult;

    public OCRMatchRequestDTO() {
    }

    public String getProductName() {
        return this.productName;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public OCRResultDTO getOcrResult() {
        return this.ocrResult;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public void setOcrResult(final OCRResultDTO ocrResult) {
        this.ocrResult = ocrResult;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OCRMatchRequestDTO)) return false;
        final OCRMatchRequestDTO other = (OCRMatchRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$barcode = this.getBarcode();
        final java.lang.Object other$barcode = other.getBarcode();
        if (this$barcode == null ? other$barcode != null : !this$barcode.equals(other$barcode)) return false;
        final java.lang.Object this$ocrResult = this.getOcrResult();
        final java.lang.Object other$ocrResult = other.getOcrResult();
        if (this$ocrResult == null ? other$ocrResult != null : !this$ocrResult.equals(other$ocrResult)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OCRMatchRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $barcode = this.getBarcode();
        result = result * PRIME + ($barcode == null ? 43 : $barcode.hashCode());
        final java.lang.Object $ocrResult = this.getOcrResult();
        result = result * PRIME + ($ocrResult == null ? 43 : $ocrResult.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OCRMatchRequestDTO(productName=" + this.getProductName() + ", barcode=" + this.getBarcode() + ", ocrResult=" + this.getOcrResult() + ")";
    }
}
