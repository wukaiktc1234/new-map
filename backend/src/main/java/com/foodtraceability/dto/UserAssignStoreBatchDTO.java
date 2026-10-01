package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量分配位置归属 DTO（P1-USER-LOCATION-001 / U-7 拍板）。
 *
 * <p>用途：门店/仓库开业时一次性给多名用户配置归属（design-001 §2.2「批量分配」）。
 * 权限与单点一致（{@code system:user:assign-store}），并写入审计留痕
 * （模块 {@code USER_STORE_ASSIGN}）。
 *
 * <p>字段名与取值空间按 design-002 §5 修订一：<b>{@code locationId} = locations.location_id</b>。
 */
@Schema(description = "批量分配位置归属DTO")
public class UserAssignStoreBatchDTO {

    @NotEmpty(message = "用户ID列表不能为空")
    @Schema(description = "目标用户ID列表", required = true)
    private List<Long> userIds;

    @NotNull(message = "位置ID不能为空")
    @Schema(description = "位置ID（locations.location_id；STORE / CENTRAL / DEPOT 型均可）", required = true, example = "1")
    private Long locationId;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
