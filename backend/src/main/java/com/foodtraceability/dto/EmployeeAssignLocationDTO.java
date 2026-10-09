package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 员工归属分配请求 DTO（P1-USER-LOCATION-002 C3，D3-2）。
 *
 * <p>与 {@link UserAssignStoreDTO} 对称：只携带一个 {@code locationId}，
 * 归属类型（STORE/WAREHOUSE/HEADQUARTERS）一律由后端按 U9 映射推导，
 * 前端不得再提交 workLocationType / storeId / warehouseId（C5 落点）。
 */
@Schema(description = "员工归属分配DTO")
public class EmployeeAssignLocationDTO {

    @NotNull(message = "位置ID不能为空")
    @Schema(description = "位置ID（locations.location_id；STORE / CENTRAL / DEPOT 型均可，TRANSIT 型后端 400 拒绝）", example = "1")
    private Long locationId;

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
