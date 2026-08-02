package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "OCR识别结果DTO")
public class OCRResultDTO {
    @Schema(description = "是否识别成功")
    private Boolean success;
    @Schema(description = "原始识别文本")
    private String rawText;
    @Schema(description = "商品名称")
    private String materialName;
    @Schema(description = "条形码")
    private String barcode;
    @Schema(description = "生产日期")
    private LocalDate productionDate;
    @Schema(description = "保质期天数")
    private Integer shelfLifeDays;
    @Schema(description = "到期日期")
    private LocalDate expiryDate;
    @Schema(description = "重量")
    private BigDecimal weight;
    @Schema(description = "重量单位")
    private String weightUnit;
    @Schema(description = "存储条件")
    private String storageCondition;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "配料表")
    private String ingredients;
    @Schema(description = "生产商/制造商")
    private String manufacturer;
    @Schema(description = "产地")
    private String origin;
    @Schema(description = "产品类型")
    private String productType;
    @Schema(description = "品牌")
    private String brand;
    @Schema(description = "规格")
    private String specifications;
    @Schema(description = "营养成分")
    private String nutritionInfo;
    @Schema(description = "过敏原信息")
    private String allergenInfo;
    @Schema(description = "匹档档案ID")
    private Long matchedTemplateId;
    @Schema(description = "匹档档案名称")
    private String matchedTemplateName;
    @Schema(description = "是否有差异")
    private Boolean hasDifference;
    @Schema(description = "差异列表")
    private List<String> differences;
    @Schema(description = "是否为新商品")
    private Boolean isNewProduct;
    @Schema(description = "识别置信度")
    private Double confidence;
    @Schema(description = "错误信息")
    private String errorMessage;

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public String getStorageCondition() {
        return storageCondition;
    }

    public OCRResultDTO() {
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public String getRawText() {
        return this.rawText;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public LocalDate getProductionDate() {
        return this.productionDate;
    }

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }

    public BigDecimal getWeight() {
        return this.weight;
    }

    public String getWeightUnit() {
        return this.weightUnit;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getIngredients() {
        return this.ingredients;
    }

    public String getManufacturer() {
        return this.manufacturer;
    }

    public String getOrigin() {
        return this.origin;
    }

    public String getProductType() {
        return this.productType;
    }

    public String getBrand() {
        return this.brand;
    }

    public String getSpecifications() {
        return this.specifications;
    }

    public String getNutritionInfo() {
        return this.nutritionInfo;
    }

    public String getAllergenInfo() {
        return this.allergenInfo;
    }

    public Long getMatchedTemplateId() {
        return this.matchedTemplateId;
    }

    public String getMatchedTemplateName() {
        return this.matchedTemplateName;
    }

    public Boolean getHasDifference() {
        return this.hasDifference;
    }

    public List<String> getDifferences() {
        return this.differences;
    }

    public Boolean getIsNewProduct() {
        return this.isNewProduct;
    }

    public Double getConfidence() {
        return this.confidence;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    public void setRawText(final String rawText) {
        this.rawText = rawText;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public void setProductionDate(final LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setExpiryDate(final LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setWeight(final BigDecimal weight) {
        this.weight = weight;
    }

    public void setWeightUnit(final String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public void setStorageCondition(final String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setIngredients(final String ingredients) {
        this.ingredients = ingredients;
    }

    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setOrigin(final String origin) {
        this.origin = origin;
    }

    public void setProductType(final String productType) {
        this.productType = productType;
    }

    public void setBrand(final String brand) {
        this.brand = brand;
    }

    public void setSpecifications(final String specifications) {
        this.specifications = specifications;
    }

    public void setNutritionInfo(final String nutritionInfo) {
        this.nutritionInfo = nutritionInfo;
    }

    public void setAllergenInfo(final String allergenInfo) {
        this.allergenInfo = allergenInfo;
    }

    public void setMatchedTemplateId(final Long matchedTemplateId) {
        this.matchedTemplateId = matchedTemplateId;
    }

    public void setMatchedTemplateName(final String matchedTemplateName) {
        this.matchedTemplateName = matchedTemplateName;
    }

    public void setHasDifference(final Boolean hasDifference) {
        this.hasDifference = hasDifference;
    }

    public void setDifferences(final List<String> differences) {
        this.differences = differences;
    }

    public void setIsNewProduct(final Boolean isNewProduct) {
        this.isNewProduct = isNewProduct;
    }

    public void setConfidence(final Double confidence) {
        this.confidence = confidence;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OCRResultDTO)) return false;
        final OCRResultDTO other = (OCRResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$success = this.getSuccess();
        final java.lang.Object other$success = other.getSuccess();
        if (this$success == null ? other$success != null : !this$success.equals(other$success)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$matchedTemplateId = this.getMatchedTemplateId();
        final java.lang.Object other$matchedTemplateId = other.getMatchedTemplateId();
        if (this$matchedTemplateId == null ? other$matchedTemplateId != null : !this$matchedTemplateId.equals(other$matchedTemplateId)) return false;
        final java.lang.Object this$hasDifference = this.getHasDifference();
        final java.lang.Object other$hasDifference = other.getHasDifference();
        if (this$hasDifference == null ? other$hasDifference != null : !this$hasDifference.equals(other$hasDifference)) return false;
        final java.lang.Object this$isNewProduct = this.getIsNewProduct();
        final java.lang.Object other$isNewProduct = other.getIsNewProduct();
        if (this$isNewProduct == null ? other$isNewProduct != null : !this$isNewProduct.equals(other$isNewProduct)) return false;
        final java.lang.Object this$confidence = this.getConfidence();
        final java.lang.Object other$confidence = other.getConfidence();
        if (this$confidence == null ? other$confidence != null : !this$confidence.equals(other$confidence)) return false;
        final java.lang.Object this$rawText = this.getRawText();
        final java.lang.Object other$rawText = other.getRawText();
        if (this$rawText == null ? other$rawText != null : !this$rawText.equals(other$rawText)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$barcode = this.getBarcode();
        final java.lang.Object other$barcode = other.getBarcode();
        if (this$barcode == null ? other$barcode != null : !this$barcode.equals(other$barcode)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$expiryDate = this.getExpiryDate();
        final java.lang.Object other$expiryDate = other.getExpiryDate();
        if (this$expiryDate == null ? other$expiryDate != null : !this$expiryDate.equals(other$expiryDate)) return false;
        final java.lang.Object this$weight = this.getWeight();
        final java.lang.Object other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !this$weight.equals(other$weight)) return false;
        final java.lang.Object this$weightUnit = this.getWeightUnit();
        final java.lang.Object other$weightUnit = other.getWeightUnit();
        if (this$weightUnit == null ? other$weightUnit != null : !this$weightUnit.equals(other$weightUnit)) return false;
        final java.lang.Object this$storageCondition = this.getStorageCondition();
        final java.lang.Object other$storageCondition = other.getStorageCondition();
        if (this$storageCondition == null ? other$storageCondition != null : !this$storageCondition.equals(other$storageCondition)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$ingredients = this.getIngredients();
        final java.lang.Object other$ingredients = other.getIngredients();
        if (this$ingredients == null ? other$ingredients != null : !this$ingredients.equals(other$ingredients)) return false;
        final java.lang.Object this$manufacturer = this.getManufacturer();
        final java.lang.Object other$manufacturer = other.getManufacturer();
        if (this$manufacturer == null ? other$manufacturer != null : !this$manufacturer.equals(other$manufacturer)) return false;
        final java.lang.Object this$origin = this.getOrigin();
        final java.lang.Object other$origin = other.getOrigin();
        if (this$origin == null ? other$origin != null : !this$origin.equals(other$origin)) return false;
        final java.lang.Object this$productType = this.getProductType();
        final java.lang.Object other$productType = other.getProductType();
        if (this$productType == null ? other$productType != null : !this$productType.equals(other$productType)) return false;
        final java.lang.Object this$brand = this.getBrand();
        final java.lang.Object other$brand = other.getBrand();
        if (this$brand == null ? other$brand != null : !this$brand.equals(other$brand)) return false;
        final java.lang.Object this$specifications = this.getSpecifications();
        final java.lang.Object other$specifications = other.getSpecifications();
        if (this$specifications == null ? other$specifications != null : !this$specifications.equals(other$specifications)) return false;
        final java.lang.Object this$nutritionInfo = this.getNutritionInfo();
        final java.lang.Object other$nutritionInfo = other.getNutritionInfo();
        if (this$nutritionInfo == null ? other$nutritionInfo != null : !this$nutritionInfo.equals(other$nutritionInfo)) return false;
        final java.lang.Object this$allergenInfo = this.getAllergenInfo();
        final java.lang.Object other$allergenInfo = other.getAllergenInfo();
        if (this$allergenInfo == null ? other$allergenInfo != null : !this$allergenInfo.equals(other$allergenInfo)) return false;
        final java.lang.Object this$matchedTemplateName = this.getMatchedTemplateName();
        final java.lang.Object other$matchedTemplateName = other.getMatchedTemplateName();
        if (this$matchedTemplateName == null ? other$matchedTemplateName != null : !this$matchedTemplateName.equals(other$matchedTemplateName)) return false;
        final java.lang.Object this$differences = this.getDifferences();
        final java.lang.Object other$differences = other.getDifferences();
        if (this$differences == null ? other$differences != null : !this$differences.equals(other$differences)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OCRResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $success = this.getSuccess();
        result = result * PRIME + ($success == null ? 43 : $success.hashCode());
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $matchedTemplateId = this.getMatchedTemplateId();
        result = result * PRIME + ($matchedTemplateId == null ? 43 : $matchedTemplateId.hashCode());
        final java.lang.Object $hasDifference = this.getHasDifference();
        result = result * PRIME + ($hasDifference == null ? 43 : $hasDifference.hashCode());
        final java.lang.Object $isNewProduct = this.getIsNewProduct();
        result = result * PRIME + ($isNewProduct == null ? 43 : $isNewProduct.hashCode());
        final java.lang.Object $confidence = this.getConfidence();
        result = result * PRIME + ($confidence == null ? 43 : $confidence.hashCode());
        final java.lang.Object $rawText = this.getRawText();
        result = result * PRIME + ($rawText == null ? 43 : $rawText.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $barcode = this.getBarcode();
        result = result * PRIME + ($barcode == null ? 43 : $barcode.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $expiryDate = this.getExpiryDate();
        result = result * PRIME + ($expiryDate == null ? 43 : $expiryDate.hashCode());
        final java.lang.Object $weight = this.getWeight();
        result = result * PRIME + ($weight == null ? 43 : $weight.hashCode());
        final java.lang.Object $weightUnit = this.getWeightUnit();
        result = result * PRIME + ($weightUnit == null ? 43 : $weightUnit.hashCode());
        final java.lang.Object $storageCondition = this.getStorageCondition();
        result = result * PRIME + ($storageCondition == null ? 43 : $storageCondition.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $ingredients = this.getIngredients();
        result = result * PRIME + ($ingredients == null ? 43 : $ingredients.hashCode());
        final java.lang.Object $manufacturer = this.getManufacturer();
        result = result * PRIME + ($manufacturer == null ? 43 : $manufacturer.hashCode());
        final java.lang.Object $origin = this.getOrigin();
        result = result * PRIME + ($origin == null ? 43 : $origin.hashCode());
        final java.lang.Object $productType = this.getProductType();
        result = result * PRIME + ($productType == null ? 43 : $productType.hashCode());
        final java.lang.Object $brand = this.getBrand();
        result = result * PRIME + ($brand == null ? 43 : $brand.hashCode());
        final java.lang.Object $specifications = this.getSpecifications();
        result = result * PRIME + ($specifications == null ? 43 : $specifications.hashCode());
        final java.lang.Object $nutritionInfo = this.getNutritionInfo();
        result = result * PRIME + ($nutritionInfo == null ? 43 : $nutritionInfo.hashCode());
        final java.lang.Object $allergenInfo = this.getAllergenInfo();
        result = result * PRIME + ($allergenInfo == null ? 43 : $allergenInfo.hashCode());
        final java.lang.Object $matchedTemplateName = this.getMatchedTemplateName();
        result = result * PRIME + ($matchedTemplateName == null ? 43 : $matchedTemplateName.hashCode());
        final java.lang.Object $differences = this.getDifferences();
        result = result * PRIME + ($differences == null ? 43 : $differences.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OCRResultDTO(success=" + this.getSuccess() + ", rawText=" + this.getRawText() + ", materialName=" + this.getMaterialName() + ", barcode=" + this.getBarcode() + ", productionDate=" + this.getProductionDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", expiryDate=" + this.getExpiryDate() + ", weight=" + this.getWeight() + ", weightUnit=" + this.getWeightUnit() + ", storageCondition=" + this.getStorageCondition() + ", supplierName=" + this.getSupplierName() + ", ingredients=" + this.getIngredients() + ", manufacturer=" + this.getManufacturer() + ", origin=" + this.getOrigin() + ", productType=" + this.getProductType() + ", brand=" + this.getBrand() + ", specifications=" + this.getSpecifications() + ", nutritionInfo=" + this.getNutritionInfo() + ", allergenInfo=" + this.getAllergenInfo() + ", matchedTemplateId=" + this.getMatchedTemplateId() + ", matchedTemplateName=" + this.getMatchedTemplateName() + ", hasDifference=" + this.getHasDifference() + ", differences=" + this.getDifferences() + ", isNewProduct=" + this.getIsNewProduct() + ", confidence=" + this.getConfidence() + ", errorMessage=" + this.getErrorMessage() + ")";
    }
}
