package com.foodtraceability.dataservice;

import com.foodtraceability.dto.DeviceAlertVO;
import java.util.List;
import java.util.Map;

/**
 * 设备告警数据服务接口
 */
public interface DeviceAlertDataService {

    /**
     * 批量获取告警基本信息
     * @param alertIds 告警ID列表
     * @return 告警ID到基本信息的映射
     */
    Map<Long, DeviceAlertVO> batchGetAlertBasicInfo(List<Long> alertIds);

    /**
     * 获取单个告警基本信息
     * @param alertId 告警ID
     * @return 告警基本信息
     */
    DeviceAlertVO getAlertBasicInfo(Long alertId);

    /**
     * 清除指定告警的缓存
     * @param alertId 告警ID
     */
    void clearAlertCache(Long alertId);
}
