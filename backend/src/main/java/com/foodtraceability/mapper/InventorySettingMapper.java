package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventorySetting;
import org.springframework.stereotype.Repository;

/**
 * 库存设置Mapper接口
 * 提供库存设置的数据库操作
 */
@Repository
public interface InventorySettingMapper extends BaseMapper<InventorySetting> {

}
