package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量分配门店归属 DTO（P1-USER-LOCATION-001 / U-7 拍板）。
 *
 * <p>用途：门店开业时一次性给多名用户配置归属（design-001 §2.2「批量分配」）。
 * 权限与单点分配一致（{@code system:user:assign-store}），并写入审计留痕
 * （模块 {@code USER_STORE_ASSIGN}）。
 */
@Schema(description = "批量分配门店归属DTO")
public class UserAssignStoreBatchDTO {

    @NotEmpty(message = "用户ID列表不能为空")
    @Schema(description = "目标用户ID列表", required = true)
    private List<Long> userIds;

    @NotNull(message = "门店ID不能为空")
    @Schema(description = "门店ID（stores_new.store_id，内部经 location_id_map 换算）", required = true, example = "1")
    private Long storeId;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
