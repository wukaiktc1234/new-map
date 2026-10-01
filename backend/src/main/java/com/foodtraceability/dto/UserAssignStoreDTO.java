package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 用户分配位置 DTO（P1-USER-LOCATION-001）。
 *
 * <p>字段名按 design-002 §5 修订一由 {@code storeId} 改为 <b>{@code locationId}</b>，
 * 且 <b>值 = {@code locations.location_id}</b>（不是 stores_new.store_id）——
 * 与"用户归属即位置"的模型一致（-002 §5.2：门店/仓库共用同一字段）。
 *
 * <p>修正顺带：原 {@code @NotBlank} 作用在 {@code Long} 上（@NotBlank 只对 CharSequence 生效，
 * 实际不校验），改为 {@code @NotNull} 使其真正生效。
 */
@Schema(description = "用户分配位置DTO")
public class UserAssignStoreDTO {

    @NotNull(message = "位置ID不能为空")
    @Schema(description = "位置ID（locations.location_id；STORE / CENTRAL / DEPOT 型均可）", example = "1")
    private Long locationId;

    public UserAssignStoreDTO() {
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
