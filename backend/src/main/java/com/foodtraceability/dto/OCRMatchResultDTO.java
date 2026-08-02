package com.foodtraceability.dto;

import java.util.List;

/**
 * OCR匹配结果DTO
 */
public class OCRMatchResultDTO {
    /**
     * 匹配的档案ID
     */
    private Long matchedId;
    /**
     * 是否找到匹配
     */
    private Boolean found;
    /**
     * 是否存在差异
     */
    private Boolean hasDifference;
    /**
     * 差异列表
     */
    private List<String> differences;

    public OCRMatchResultDTO() {
    }

    /**
     * 匹配的档案ID
     */
    public Long getMatchedId() {
        return this.matchedId;
    }

    /**
     * 是否找到匹配
     */
    public Boolean getFound() {
        return this.found;
    }

    /**
     * 是否存在差异
     */
    public Boolean getHasDifference() {
        return this.hasDifference;
    }

    /**
     * 差异列表
     */
    public List<String> getDifferences() {
        return this.differences;
    }

    /**
     * 匹配的档案ID
     */
    public void setMatchedId(final Long matchedId) {
        this.matchedId = matchedId;
    }

    /**
     * 是否找到匹配
     */
    public void setFound(final Boolean found) {
        this.found = found;
    }

    /**
     * 是否存在差异
     */
    public void setHasDifference(final Boolean hasDifference) {
        this.hasDifference = hasDifference;
    }

    /**
     * 差异列表
     */
    public void setDifferences(final List<String> differences) {
        this.differences = differences;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof OCRMatchResultDTO)) return false;
        final OCRMatchResultDTO other = (OCRMatchResultDTO) o;
        if (!other.canEqual(this)) return false;
        final Object this$matchedId = this.getMatchedId();
        final Object other$matchedId = other.getMatchedId();
        if (this$matchedId == null ? other$matchedId != null : !this$matchedId.equals(other$matchedId)) return false;
        final Object this$found = this.getFound();
        final Object other$found = other.getFound();
        if (this$found == null ? other$found != null : !this$found.equals(other$found)) return false;
        final Object this$hasDifference = this.getHasDifference();
        final Object other$hasDifference = other.getHasDifference();
        if (this$hasDifference == null ? other$hasDifference != null : !this$hasDifference.equals(other$hasDifference)) return false;
        final Object this$differences = this.getDifferences();
        final Object other$differences = other.getDifferences();
        if (this$differences == null ? other$differences != null : !this$differences.equals(other$differences)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof OCRMatchResultDTO;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchedId = this.getMatchedId();
        result = result * PRIME + ($matchedId == null ? 43 : $matchedId.hashCode());
        final Object $found = this.getFound();
        result = result * PRIME + ($found == null ? 43 : $found.hashCode());
        final Object $hasDifference = this.getHasDifference();
        result = result * PRIME + ($hasDifference == null ? 43 : $hasDifference.hashCode());
        final Object $differences = this.getDifferences();
        result = result * PRIME + ($differences == null ? 43 : $differences.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "OCRMatchResultDTO(matchedId=" + this.getMatchedId() + ", found=" + this.getFound() + ", hasDifference=" + this.getHasDifference() + ", differences=" + this.getDifferences() + ")";
    }
}
