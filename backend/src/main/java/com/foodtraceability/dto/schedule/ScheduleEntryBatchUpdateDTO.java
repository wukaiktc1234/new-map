package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 排班条目批量更新请求DTO
 * 用于时间线日历的快速编辑、拖拽交换等场景
 *
 * <p>业务规则：
 * <ul>
 *   <li>entryId存在 → 更新现有条目</li>
 *   <li>entryId不存在 → 创建新条目</li>
 *   <li>同一方案同一员工同一天只能有一条记录</li>
 * </ul>
 */
@Schema(description = "排班条目批量更新请求DTO")
public class ScheduleEntryBatchUpdateDTO {

    /**
     * 待更新的条目列表
     * 支持混合操作：部分更新、部分新增
     */
    @Valid
    @NotEmpty(message = "更新条目列表不能为空")
    @Schema(description = "待更新的条目列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EntryUpdateItem> entries;

    /**
     * 单个更新条目
     */
    @Schema(description = "单个更新条目")
    public static class EntryUpdateItem {

        /**
         * 要更新的条目ID(可选)
         * 为空时表示创建新条目，有值时表示更新现有条目
         */
        @Schema(description = "条目ID(为空表示新增)")
        private String entryId;

        /**
         * 员工ID(必填)
         */
        @NotNull(message = "员工ID不能为空")
        @Schema(description = "员工ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long employeeId;

        /**
         * 工作日期(YYYY-MM-DD格式)
         */
        @NotNull(message = "工作日期不能为空")
        @Schema(description = "工作日期", example = "2026-05-11", requiredMode = Schema.RequiredMode.REQUIRED)
        private String workDate;

        /**
         * 班次类型编码
         * morning/noon/evening/night_off
         */
        @Schema(description = "班次类型编码",
                allowableValues = {"morning", "noon", "evening", "night_off"})
        private String shiftType;

        /**
         * 来源标识
         * manual-手动编辑 swap-换班产生
         */
        @Schema(description = "来源标识",
                allowableValues = {"manual", "swap"})
        private String source;

        // ==================== Getter & Setter ====================

        public String getEntryId() {
            return entryId;
        }

        public void setEntryId(String entryId) {
            this.entryId = entryId;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public String getWorkDate() {
            return workDate;
        }

        public void setWorkDate(String workDate) {
            this.workDate = workDate;
        }

        public String getShiftType() {
            return shiftType;
        }

        public void setShiftType(String shiftType) {
            this.shiftType = shiftType;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    // ==================== Getter & Setter ====================

    public List<EntryUpdateItem> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryUpdateItem> entries) {
        this.entries = entries;
    }
}
