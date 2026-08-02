package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dataservice.DeviceAlertDataService;
import com.foodtraceability.dto.DeviceAlertVO;
import com.foodtraceability.entity.DeviceAlert;
import com.foodtraceability.mapper.DeviceAlertMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备告警数据服务实现类
 */
@Service
public class DeviceAlertDataServiceImpl implements DeviceAlertDataService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAlertDataServiceImpl.class);
    private static final String CACHE_NAME = "deviceAlert";
    private static final long CACHE_EXPIRE_SECONDS = 3600; // 1小时

    private final DeviceAlertMapper deviceAlertMapper;

    public DeviceAlertDataServiceImpl(DeviceAlertMapper deviceAlertMapper) {
        this.deviceAlertMapper = deviceAlertMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#alertIds", unless = "#result == null || #result.isEmpty()")
    public Map<Long, DeviceAlertVO> batchGetAlertBasicInfo(List<Long> alertIds) {
        if (alertIds == null || alertIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DeviceAlert> alerts = deviceAlertMapper.selectBatchIds(alertIds);
        return alerts.stream()
                .collect(Collectors.toMap(
                        DeviceAlert::getAlertId,
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #alertId", unless = "#result == null")
    public DeviceAlertVO getAlertBasicInfo(Long alertId) {
        if (alertId == null) {
            return null;
        }

        DeviceAlert alert = deviceAlertMapper.selectById(alertId);
        return convertToVO(alert);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'basic:' + #alertId")
    public void clearAlertCache(Long alertId) {
        log.debug("清除设备告警缓存: alertId={}", alertId);
    }

    /**
     * 将实体转换为VO
     */
    private DeviceAlertVO convertToVO(DeviceAlert alert) {
        if (alert == null) {
            return null;
        }

        DeviceAlertVO vo = new DeviceAlertVO();
        vo.setAlertId(alert.getAlertId());
        vo.setDeviceId(alert.getDeviceId());
        vo.setAlertType(alert.getAlertType());
        vo.setAlertLevel(alert.getAlertLevel());
        vo.setAlertMessage(alert.getAlertMessage());
        vo.setIsHandled(alert.getIsHandled());
        vo.setHandleTime(alert.getHandleTime());
        vo.setHandleResult(alert.getHandleResult());
        vo.setCreateTime(alert.getCreateTime());

        return vo;
    }
}
