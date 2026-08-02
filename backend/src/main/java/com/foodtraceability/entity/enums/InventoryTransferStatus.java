package com.foodtraceability.entity.enums;

/**
 * 库存调拨单状态枚举
 * 统一定义调拨单生命周期各阶段的状态码，避免 magic number
 *
 * 状态流转：
 *   DRAFT(0) -> PENDING_APPROVAL(1) -> APPROVED(2) -> COMPLETED(3)
 *                                  -> CANCELLED(4)
 */
public enum InventoryTransferStatus {
    /** 草稿 */
    DRAFT(0, "草稿"),
    /** 待审批 */
    PENDING_APPROVAL(1, "待审批"),
    /** 已审批 */
    APPROVED(2, "已审批"),
    /** 已完成 */
    COMPLETED(3, "已完成"),
    /** 已取消 / 已拒绝 */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;

    InventoryTransferStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InventoryTransferStatus fromCode(Integer code) {
        if (code == null) {
            return DRAFT;
        }
        for (InventoryTransferStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
