package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DeviceData;
import com.foodtraceability.mapper.DeviceDataMapper;
import com.foodtraceability.service.DeviceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备数据采集服务实现
 * <p>
 * 基于 MyBatis-Plus BaseMapper 实现设备数据的查询、统计和清理。
 * 设备数据按采集时间（collectTime）作为业务时间维度进行时间范围过滤与排序。
 * </p>
 */
@Service("deviceDataCollectorService")
public class DeviceDataCollectorServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements DeviceDataService {

    private static final Logger log = LoggerFactory.getLogger(DeviceDataCollectorServiceImpl.class);

    /**
     * 获取设备最新数据
     * <p>
     * 按 deviceId 过滤，按采集时间（collectTime）倒序取第一条；
     * 若 collectTime 相同则按数据创建时间（createdAt）倒序兜底。
     * </p>
     * @param deviceId 设备ID
     * @return 设备最新数据，若无则返回 null
     */
    @Override
    public DeviceData getLatestDeviceData(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceData::getDeviceId, deviceId)
                .orderByDesc(DeviceData::getCollectTime)
                .orderByDesc(DeviceData::getCreatedAt);
        // 取最新一条，使用分页限制为1条
        Page<DeviceData> page = new Page<>(1, 1);
        Page<DeviceData> result = baseMapper.selectPage(page, wrapper);
        if (result.getRecords() == null || result.getRecords().isEmpty()) {
            log.debug("未查询到设备最新数据，deviceId: {}", deviceId);
            return null;
        }
        return result.getRecords().get(0);
    }

    /**
     * 获取设备历史数据列表（分页）
     * @param deviceId 设备ID
     * @param startTime 开始时间（采集时间范围，可为 null）
     * @param endTime 结束时间（采集时间范围，可为 null）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 设备历史数据列表，按采集时间倒序
     */
    @Override
    public List<DeviceData> getDeviceHistoryData(String deviceId, LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        if (deviceId == null || deviceId.isEmpty()) {
            return Collections.emptyList();
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceData::getDeviceId, deviceId)
                .ge(startTime != null, DeviceData::getCollectTime, startTime)
                .le(endTime != null, DeviceData::getCollectTime, endTime)
                .orderByDesc(DeviceData::getCollectTime);
        Page<DeviceData> pageObj = new Page<>(page, size);
        Page<DeviceData> result = baseMapper.selectPage(pageObj, wrapper);
        return result.getRecords() == null ? Collections.emptyList() : result.getRecords();
    }

    /**
     * 获取设备数据统计
     * <p>
     * 统计指定时间范围内的数据总数、平均响应时间、平均温度、平均负载、
     * 平均信号强度、错误总数、操作总数及错误率。
     * </p>
     * @param deviceId 设备ID
     * @param startTime 开始时间（可为 null）
     * @param endTime 结束时间（可为 null）
     * @return 统计结果 Map
     */
    @Override
    public Map<String, Object> getDeviceDataStatistics(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        if (deviceId == null || deviceId.isEmpty()) {
            statistics.put("totalCount", 0L);
            return statistics;
        }
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceData::getDeviceId, deviceId)
                .ge(startTime != null, DeviceData::getCollectTime, startTime)
                .le(endTime != null, DeviceData::getCollectTime, endTime);
        List<DeviceData> list = baseMapper.selectList(wrapper);
        long totalCount = list.size();
        statistics.put("totalCount", totalCount);
        if (totalCount == 0) {
            return statistics;
        }
        // 平均响应时间（毫秒）
        double avgResponseTime = list.stream()
                .map(DeviceData::getResponseTime)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average().orElse(0D);
        statistics.put("avgResponseTime", avgResponseTime);
        // 平均温度
        double avgTemperature = list.stream()
                .map(DeviceData::getTemperature)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0D);
        statistics.put("avgTemperature", avgTemperature);
        // 平均负载
        double avgLoad = list.stream()
                .map(DeviceData::getLoad)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0D);
        statistics.put("avgLoad", avgLoad);
        // 平均信号强度
        double avgSignalStrength = list.stream()
                .map(DeviceData::getSignalStrength)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().orElse(0D);
        statistics.put("avgSignalStrength", avgSignalStrength);
        // 错误总数
        long totalErrorCount = list.stream()
                .map(DeviceData::getErrorCount)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        statistics.put("totalErrorCount", totalErrorCount);
        // 操作总数
        long totalOperationCount = list.stream()
                .map(DeviceData::getOperationCount)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        statistics.put("totalOperationCount", totalOperationCount);
        // 错误率 = 错误数 / 操作数
        double errorRate = totalOperationCount > 0 ? (double) totalErrorCount / totalOperationCount : 0D;
        statistics.put("errorRate", errorRate);
        return statistics;
    }

    /**
     * 获取设备连接状态统计
     * <p>
     * 按设备类型（可选）和时间范围统计连接状态分布。
     * 约定 connectionStatus=1 表示在线/已连接，其他值视为离线/未连接。
     * </p>
     * @param deviceType 设备类型（可选，为 null/空时统计所有类型）
     * @param startTime 开始时间（可为 null）
     * @param endTime 结束时间（可为 null）
     * @return 连接状态统计 Map
     */
    @Override
    public Map<String, Object> getConnectionStatusStatistics(String deviceType, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(DeviceData::getDeviceType, deviceType);
        }
        wrapper.ge(startTime != null, DeviceData::getCollectTime, startTime)
                .le(endTime != null, DeviceData::getCollectTime, endTime);
        List<DeviceData> list = baseMapper.selectList(wrapper);
        long totalCount = list.size();
        statistics.put("totalCount", totalCount);
        if (totalCount == 0) {
            statistics.put("connectedCount", 0L);
            statistics.put("disconnectedCount", 0L);
            statistics.put("connectionRate", 0D);
            statistics.put("statusDistribution", Collections.emptyMap());
            return statistics;
        }
        // 按 connectionStatus 分组统计
        Map<Integer, Long> statusDistribution = list.stream()
                .filter(d -> d.getConnectionStatus() != null)
                .collect(Collectors.groupingBy(DeviceData::getConnectionStatus, Collectors.counting()));
        statistics.put("statusDistribution", statusDistribution);
        // 在线数（约定 1 = 已连接）
        long connectedCount = statusDistribution.getOrDefault(1, 0L);
        long disconnectedCount = totalCount - connectedCount;
        statistics.put("connectedCount", connectedCount);
        statistics.put("disconnectedCount", disconnectedCount);
        double connectionRate = (double) connectedCount / totalCount;
        statistics.put("connectionRate", connectionRate);
        return statistics;
    }

    /**
     * 清理过期设备数据
     * <p>
     * 物理删除采集时间（collectTime）早于 beforeTime 的数据。
     * device_data 属于监控/采集类数据，按规范允许物理删除。
     * </p>
     * @param beforeTime 清理该时间之前的数据
     * @return 清理的数据条数
     */
    @Override
    public int cleanExpiredData(LocalDateTime beforeTime) {
        if (beforeTime == null) {
            log.warn("清理过期设备数据失败：beforeTime 为空");
            return 0;
        }
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(DeviceData::getCollectTime, beforeTime);
        int deletedCount = baseMapper.delete(wrapper);
        log.info("清理过期设备数据完成，截止时间: {}, 删除条数: {}", beforeTime, deletedCount);
        return deletedCount;
    }
}
