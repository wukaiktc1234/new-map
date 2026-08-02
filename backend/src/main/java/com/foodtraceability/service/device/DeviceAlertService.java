package com.foodtraceability.service.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DeviceAlertQueryDTO;
import com.foodtraceability.dto.DeviceAlertVO;
import com.foodtraceability.entity.DeviceAlert;
import com.foodtraceability.entity.DeviceStatus;

import java.util.List;

/**
 * 设备告警服务接口
 * 提供告警查询、创建、处理等功能
 */
public interface DeviceAlertService {

    /**
     * 创建告警
     * @param deviceId 设备ID
     * @param alertType 告警类型：1离线超时 2纸张缺 3碳带缺 4故障 5维护提醒
     * @param alertLevel 告警级别：1信息 2警告 3严重 4紧急
     * @return 创建结果
     */
    Result<DeviceAlertVO> createAlert(Long deviceId, Integer alertType, Integer alertLevel);

    /**
     * 创建自定义消息告警
     * @param deviceId 设备ID
     * @param alertType 告警类型
     * @param alertLevel 告警级别
     * @param message 自定义消息
     * @return 创建结果
     */
    Result<DeviceAlertVO> createAlertWithMessage(Long deviceId, Integer alertType,
                                                  Integer alertLevel, String message);

    /**
     * 根据ID查询告警
     * @param alertId 告警ID
     * @return 告警信息
     */
    Result<DeviceAlertVO> getAlertById(Long alertId);

    /**
     * 分页查询告警列表
     * @param page 分页参数
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Result<IPage<DeviceAlertVO>> getAlertPage(Page<?> page, DeviceAlertQueryDTO queryDto);

    /**
     * 查询设备的未处理告警
     * @param deviceId 设备ID
     * @return 未处理告警列表
     */
    Result<List<DeviceAlertVO>> getUnhandledAlertsByDevice(Long deviceId);

    /**
     * 查询所有未处理告警
     * @return 未处理告警列表
     */
    Result<List<DeviceAlertVO>> getAllUnhandledAlerts();

    /**
     * 处理告警
     * @param alertId 告警ID
     * @param handleResult 处理结果描述
     * @return 处理结果
     */
    Result<Void> handleAlert(Long alertId, String handleResult);

    /**
     * 批量处理告警
     * @param alertIds 告警ID列表
     * @param handleResult 处理结果描述
     * @return 处理结果
     */
    Result<Void> batchHandleAlerts(List<Long> alertIds, String handleResult);

    /**
     * 获取告警统计信息
     * @return 统计信息
     */
    Result<Object> getAlertStatistics();

    /**
     * 检查设备状态并触发告警
     * 根据设备状态自动判断是否需要创建告警
     * @param status 设备状态信息
     */
    void checkDeviceStatusAndAlert(DeviceStatus status);

    /**
     * 保存告警（创建）
     * @param alert 告警实体
     * @return 创建结果
     */
    Result<DeviceAlertVO> save(DeviceAlert alert);

    /**
     * 根据ID逻辑删除告警
     * @param alertId 告警ID
     * @return 删除结果
     */
    Result<Void> removeById(Long alertId);

    /**
     * 根据ID更新告警信息
     * @param alert 告警实体（需包含alertId）
     * @return 更新结果
     */
    Result<DeviceAlertVO> updateById(DeviceAlert alert);
}
