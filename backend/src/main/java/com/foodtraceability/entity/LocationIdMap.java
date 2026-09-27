package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 旧 ID → location_id 映射桥。
 * 规则（location-organization-separation-design-002.md §7）：
 * store_id / warehouse_id ↔ location_id 的一切换算必须经本表，严禁数值假设相等。
 */
@TableName("location_id_map")
public class LocationIdMap {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源表名：'stores_new' / 'warehouses' */
    private String srcTable;

    /** 来源表主键值 */
    private Long srcId;

    /** 映射到的位置ID */
    private Long locationId;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSrcTable() { return srcTable; }
    public void setSrcTable(String srcTable) { this.srcTable = srcTable; }

    public Long getSrcId() { return srcId; }
    public void setSrcId(Long srcId) { this.srcId = srcId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
