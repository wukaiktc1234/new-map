package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.DeviceStatusHistory;
import com.foodtraceability.mapper.DeviceStatusHistoryMapper;
import com.foodtraceability.service.DeviceStatusHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 设备状态历史记录服务实现类
 */
@Service
public class DeviceStatusHistoryServiceImpl extends ServiceImpl<DeviceStatusHistoryMapper, DeviceStatusHistory> implements DeviceStatusHistoryService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusHistoryServiceImpl.class);


    public DeviceStatusHistoryServiceImpl(DeviceStatusHistoryMapper deviceStatusHistoryMapper) {
        this.deviceStatusHistoryMapper = deviceStatusHistoryMapper;
    }

    private final DeviceStatusHistoryMapper deviceStatusHistoryMapper;

    /**
     * 记录设备状态到历史记录
     * @param status 设备状态
     */
    @Override
    public void recordDeviceStatus(DeviceStatus status) {
        if (status == null) {
            log.warn("设备状态为空，无法记录历史记录");
            return;
        }

        log.info("记录设备状态历史: 设备类型={}, 在线状态={}, 检测时间={}", 
                status.getDeviceType(), status.isOnline(), status.getLastCheckTime());

        // 创建历史记录实体
        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setDeviceType(status.getDeviceType());
        history.setDeviceId(status.getDeviceId());
        history.setDeviceName(status.getDeviceName());
        history.setDeviceModel(status.getDeviceModel());
        history.setOnline(status.isOnline());
        history.setResponseTime(status.getResponseTime());
        history.setFirmwareVersion(status.getFirmwareVersion());
        history.setConnectionType(status.getConnectionType());
        history.setIpAddress(status.getIpAddress());
        history.setPort(status.getPort());
        history.setCheckTime(status.getLastCheckTime());
        history.setDetails(status.getDetails());
        history.setErrorMessage(status.getErrorMessage());
        // 默认门店ID为1，实际项目中应从上下文获取
        history.setStoreId(1L);
        history.setCreatedAt(new Date());

        // 保存到数据库
        save(history);
    }

    /**
     * 按设备类型查询设备状态历史记录（后端真分页）
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 分页结果
     */
    @Override
    public IPage<DeviceStatusHistory> getHistoryByDeviceType(String deviceType, Date startTime, Date endTime, Long storeId, Integer pageSize, Integer pageNum) {
        log.info("查询设备状态历史记录: 设备类型={}, 开始时间={}, 结束时间={}, 门店ID={}, 页码={}, 每页大小={}",
                deviceType, startTime, endTime, storeId, pageNum, pageSize);

        Page<DeviceStatusHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceStatusHistory> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(DeviceStatusHistory::getDeviceType, deviceType);
        }
        if (startTime != null) {
            wrapper.ge(DeviceStatusHistory::getCheckTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(DeviceStatusHistory::getCheckTime, endTime);
        }
        if (storeId != null) {
            wrapper.eq(DeviceStatusHistory::getStoreId, storeId);
        }
        wrapper.orderByDesc(DeviceStatusHistory::getCheckTime);
        return page(page, wrapper);
    }

    /**
     * 查询指定设备的状态历史记录（后端真分页）
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 分页结果
     */
    @Override
    public IPage<DeviceStatusHistory> getHistoryByDeviceId(Long deviceId, Date startTime, Date endTime, Integer pageSize, Integer pageNum) {
        log.info("查询设备状态历史记录: 设备ID={}, 开始时间={}, 结束时间={}, 页码={}, 每页大小={}",
                deviceId, startTime, endTime, pageNum, pageSize);

        Page<DeviceStatusHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceStatusHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceStatusHistory::getDeviceId, deviceId);
        if (startTime != null) {
            wrapper.ge(DeviceStatusHistory::getCheckTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(DeviceStatusHistory::getCheckTime, endTime);
        }
        wrapper.orderByDesc(DeviceStatusHistory::getCheckTime);
        return page(page, wrapper);
    }

    /**
     * 清理指定时间之前的历史记录
     * @param beforeTime 指定时间
     * @return 清理结果
     */
    @Override
    public boolean cleanHistoryBeforeTime(Date beforeTime) {
        log.info("清理设备状态历史记录，删除{}之前的记录", beforeTime);
        LambdaQueryWrapper<DeviceStatusHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(DeviceStatusHistory::getCheckTime, beforeTime);
        boolean removed = remove(wrapper);
        log.info("清理设备状态历史记录完成，结果={}", removed);
        return removed;
    }

    /**
     * 统计设备状态变化次数
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @return 变化次数
     */
    @Override
    public int countStatusChanges(String deviceType, Date startTime, Date endTime, Long storeId) {
        log.info("统计设备状态变化次数: 设备类型={}, 开始时间={}, 结束时间={}, 门店ID={}", 
                deviceType, startTime, endTime, storeId);
        LambdaQueryWrapper<DeviceStatusHistory> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(DeviceStatusHistory::getDeviceType, deviceType);
        }
        if (startTime != null) {
            wrapper.ge(DeviceStatusHistory::getCheckTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(DeviceStatusHistory::getCheckTime, endTime);
        }
        if (storeId != null) {
            wrapper.eq(DeviceStatusHistory::getStoreId, storeId);
        }
        return Math.toIntExact(count(wrapper));
    }

    /**
     * 获取过去7天的设备状态历史记录
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 历史记录列表
     */
    @Override
    public List<DeviceStatusHistory> getLast7DaysHistory(String deviceType, Long storeId) {
        log.info("获取过去7天的设备状态历史记录: 设备类型={}, 门店ID={}", deviceType, storeId);
        
        // 计算7天前的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date startTime = calendar.getTime();
        Date endTime = new Date();
        
        // 使用 MyBatis Plus 内置 list 查询，不分页
        LambdaQueryWrapper<DeviceStatusHistory> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(DeviceStatusHistory::getDeviceType, deviceType);
        }
        wrapper.ge(DeviceStatusHistory::getCheckTime, startTime);
        wrapper.le(DeviceStatusHistory::getCheckTime, endTime);
        if (storeId != null) {
            wrapper.eq(DeviceStatusHistory::getStoreId, storeId);
        }
        wrapper.orderByDesc(DeviceStatusHistory::getCheckTime);
        return list(wrapper);
    }

    /**
     * 获取过去24小时的设备状态历史记录
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 历史记录列表
     */
    @Override
    public List<DeviceStatusHistory> getLast24HoursHistory(String deviceType, Long storeId) {
        log.info("获取过去24小时的设备状态历史记录: 设备类型={}, 门店ID={}", deviceType, storeId);
        
        // 计算24小时前的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        Date startTime = calendar.getTime();
        Date endTime = new Date();
        
        // 使用 MyBatis Plus 内置 list 查询，不分页
        LambdaQueryWrapper<DeviceStatusHistory> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(DeviceStatusHistory::getDeviceType, deviceType);
        }
        wrapper.ge(DeviceStatusHistory::getCheckTime, startTime);
        wrapper.le(DeviceStatusHistory::getCheckTime, endTime);
        if (storeId != null) {
            wrapper.eq(DeviceStatusHistory::getStoreId, storeId);
        }
        wrapper.orderByDesc(DeviceStatusHistory::getCheckTime);
        return list(wrapper);
    }
}
