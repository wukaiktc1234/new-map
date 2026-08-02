package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 班次排序调整DTO
 */
@Schema(description = "班次排序调整DTO")
public class ScheduleShiftTypeReorderDTO {

    @NotNull(message = "门店ID不能为空")
    @Schema(description = "门店ID", required = true)
    private Long storeId;

    @NotEmpty(message = "排序列表不能为空")
    @Valid
    @Schema(description = "班次排序项列表(按新顺序排列)", required = true)
    private List<ReorderItem> items;

    /**
     * 排序项
     */
    @Schema(description = "排序项")
    public static class ReorderItem {

        @NotNull(message = "班次类型ID不能为空")
        @Schema(description = "班次类型ID", required = true)
        private Long shiftTypeId;

        @NotNull(message = "排序值不能为空")
        @Schema(description = "新的排序值", required = true)
        private Integer sortOrder;

        public Long getShiftTypeId() {
            return shiftTypeId;
        }

        public void setShiftTypeId(Long shiftTypeId) {
            this.shiftTypeId = shiftTypeId;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public List<ReorderItem> getItems() {
        return items;
    }

    public void setItems(List<ReorderItem> items) {
        this.items = items;
    }
}
