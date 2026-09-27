package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存位置实体（门店与仓库统一）
 * 库存维度"货在哪"：STORE=门店（有库存的最小仓库）、CENTRAL=中央仓、DEPOT=普通仓储仓。
 * 与组织维度（departments）正交，不耦合。
 * ID 空间规则见 docs/design/location-organization-separation-design-002.md §7。
 */
@TableName("locations")
public class Location {

    /** 位置ID（库存/调拨/员工归属的唯一外键） */
    @TableId(type = IdType.AUTO)
    private Long locationId;

    /** 位置编码（唯一；吸收 stores_new.store_code 与 warehouses.warehouse_code） */
    private String locationCode;

    /** 位置名称 */
    private String locationName;

    /** 位置类型：STORE / CENTRAL / DEPOT / TRANSIT */
    private String locationType;

    /** 物理存储属性：NORMAL / COLD / FROZEN；NULL=不适用 */
    private String storageType;

    /** 地址 */
    private String address;

    /** 容量 */
    private BigDecimal capacity;

    /** 管理人用户ID（吸收 warehouses.manager_id） */
    private Long managerUserId;

    /** 联系电话 */
    private String phone;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0 未删除 1 已删除 */
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getCapacity() { return capacity; }
    public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }

    public Long getManagerUserId() { return managerUserId; }
    public void setManagerUserId(Long managerUserId) { this.managerUserId = managerUserId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
