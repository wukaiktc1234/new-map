package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Location;
import com.foodtraceability.mapper.LocationMapper;
import com.foodtraceability.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存位置服务实现（M1-M2 核心层，只读）。
 */
@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationServiceImpl.class);

    private final LocationMapper locationMapper;

    public LocationServiceImpl(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    @Override
    public Location getById(Long locationId) {
        if (locationId == null) {
            return null;
        }
        Location location = locationMapper.selectById(locationId);
        if (location == null || (location.getDeleted() != null && location.getDeleted() == 1)) {
            return null;
        }
        return location;
    }

    @Override
    public Location resolveByStoreId(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return locationMapper.selectByStoreId(storeId);
    }

    @Override
    public Location resolveByWarehouseId(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return locationMapper.selectBySource("warehouses", warehouseId);
    }

    @Override
    public List<Location> listActive(String locationType) {
        return locationMapper.selectActive(locationType);
    }
}
