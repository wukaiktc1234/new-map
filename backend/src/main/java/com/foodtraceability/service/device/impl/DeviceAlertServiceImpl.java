package com.foodtraceability.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dataservice.DeviceAlertDataService;
import com.foodtraceability.dataservice.DeviceDataService;
import com.foodtraceability.dto.DeviceAlertQueryDTO;
import com.foodtraceability.dto.DeviceAlertVO;
import com.foodtraceability.dto.DeviceVO;
import com.foodtraceability.entity.Device;
import com.foodtraceability.entity.DeviceAlert;
import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.mapper.DeviceAlertMapper;
import com.foodtraceability.mapper.DeviceMapper;
import com.foodtraceability.service.device.DeviceAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备告警服务实现类
 */
@Service
public class DeviceAlertServiceImpl implements DeviceAlertService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAlertServiceImpl.class);

    private final DeviceAlertMapper deviceAlertMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceAlertDataService deviceAlertDataService;
    private final DeviceDataService deviceDataService;

    public DeviceAlertServiceImpl(DeviceAlertMapper deviceAlertMapper,
                                  DeviceMapper deviceMapper,
                                  DeviceAlertDataService deviceAlertDataService,
                                  DeviceDataService deviceDataService) {
        this.deviceAlertMapper = deviceAlertMapper;
        this.deviceMapper = deviceMapper;
        this.deviceAlertDataService = deviceAlertDataService;
        this.deviceDataService = deviceDataService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeviceAlertVO> createAlert(Long deviceId, Integer alertType, Integer alertLevel) {
        String message = generateDefaultMessage(alertType);
        return createAlertWithMessage(deviceId, alertType, alertLevel, message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeviceAlertVO> createAlertWithMessage(Long deviceId, Integer alertType,
                                                        Integer alertLevel, String message) {
        try {
            // 验证设备是否存在
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            DeviceAlert alert = new DeviceAlert();
            alert.setDeviceId(deviceId);
            alert.setAlertType(alertType);
            alert.setAlertLevel(alertLevel != null ? alertLevel : 2); // 默认警告级别
            alert.setAlertMessage(message);
            alert.setIsHandled(false);
            alert.setCreateTime(LocalDateTime.now());
            alert.setUpdateTime(LocalDateTime.now());

            deviceAlertMapper.insert(alert);

            log.info("创建设备告警: deviceId={}, alertType={}, level={}",
                    deviceId, alertType, alertLevel);

            return Result.success(convertToVO(alert), "创建告警成功");
        } catch (Exception e) {
            log.error("创建设备告警失败", e);
            return Result.error("创建告警失败：" + e.getMessage());
        }
    }

    @Override
    public Result<DeviceAlertVO> getAlertById(Long alertId) {
        try {
            DeviceAlertVO vo = deviceAlertDataService.getAlertBasicInfo(alertId);
            if (vo == null) {
                return Result.error("告警不存在");
            }
            return Result.success(vo, "查询告警成功");
        } catch (Exception e) {
            log.error("查询告警失败: alertId={}", alertId, e);
            return Result.error("查询告警失败：" + e.getMessage());
        }
    }

    @Override
    public Result<IPage<DeviceAlertVO>> getAlertPage(Page<?> page, DeviceAlertQueryDTO queryDto) {
        try {
            LambdaQueryWrapper<DeviceAlert> wrapper = buildQueryWrapper(queryDto);
            wrapper.orderByDesc(DeviceAlert::getAlertLevel)
                   .orderByDesc(DeviceAlert::getCreateTime);

            @SuppressWarnings("unchecked")
            Page<DeviceAlert> alertPage = (Page<DeviceAlert>) page;
            IPage<DeviceAlert> resultPage = deviceAlertMapper.selectPage(alertPage, wrapper);

            IPage<DeviceAlertVO> voPage = resultPage.convert(this::convertToVO);

            return Result.success(voPage, "查询告警列表成功");
        } catch (Exception e) {
            log.error("分页查询告警失败", e);
            return Result.error("查询告警列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<DeviceAlertVO>> getUnhandledAlertsByDevice(Long deviceId) {
        try {
            LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceAlert::getDeviceId, deviceId)
                   .eq(DeviceAlert::getIsHandled, false)
                   .orderByDesc(DeviceAlert::getAlertLevel)
                   .orderByDesc(DeviceAlert::getCreateTime);

            List<DeviceAlert> alerts = deviceAlertMapper.selectList(wrapper);
            List<DeviceAlertVO> voList = alerts.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return Result.success(voList, "查询未处理告警成功");
        } catch (Exception e) {
            log.error("查询设备未处理告警失败: deviceId={}", deviceId, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<DeviceAlertVO>> getAllUnhandledAlerts() {
        try {
            LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceAlert::getIsHandled, false)
                   .orderByDesc(DeviceAlert::getAlertLevel)
                   .orderByDesc(DeviceAlert::getCreateTime);

            List<DeviceAlert> alerts = deviceAlertMapper.selectList(wrapper);
            List<DeviceAlertVO> voList = alerts.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return Result.success(voList, "查询所有未处理告警成功");
        } catch (Exception e) {
            log.error("查询所有未处理告警失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handleAlert(Long alertId, String handleResult) {
        try {
            DeviceAlert alert = deviceAlertMapper.selectById(alertId);
            if (alert == null) {
                return Result.error("告警不存在");
            }

            if (Boolean.TRUE.equals(alert.getIsHandled())) {
                return Result.error("该告警已被处理");
            }

            alert.setIsHandled(true);
            alert.setHandleTime(LocalDateTime.now());
            alert.setHandleResult(handleResult);
            alert.setUpdateTime(LocalDateTime.now());
            deviceAlertMapper.updateById(alert);

            // 清除缓存
            deviceAlertDataService.clearAlertCache(alertId);

            log.info("处理告警成功: alertId={}, result={}", alertId, handleResult);
            return Result.success(null, "告警处理成功");
        } catch (Exception e) {
            log.error("处理告警失败: alertId={}", alertId, e);
            return Result.error("处理告警失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchHandleAlerts(List<Long> alertIds, String handleResult) {
        try {
            int handledCount = 0;

            for (Long alertId : alertIds) {
                Result<Void> result = handleAlert(alertId, handleResult);
                if (result.isSuccess()) {
                    handledCount++;
                }
            }

            log.info("批量处理告警成功: 总数={}, 成功={}", alertIds.size(), handledCount);
            return Result.success(null, String.format("批量处理完成，成功%d条", handledCount));
        } catch (Exception e) {
            log.error("批量处理告警失败", e);
            return Result.error("批量处理告警失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Object> getAlertStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 总告警数
            LambdaQueryWrapper<DeviceAlert> totalWrapper = new LambdaQueryWrapper<>();
            statistics.put("total", deviceAlertMapper.selectCount(totalWrapper));

            // 未处理告警数
            LambdaQueryWrapper<DeviceAlert> unhandledWrapper = new LambdaQueryWrapper<>();
            unhandledWrapper.eq(DeviceAlert::getIsHandled, false);
            statistics.put("unhandled", deviceAlertMapper.selectCount(unhandledWrapper));

            // 已处理告警数
            LambdaQueryWrapper<DeviceAlert> handledWrapper = new LambdaQueryWrapper<>();
            handledWrapper.eq(DeviceAlert::getIsHandled, true);
            statistics.put("handled", deviceAlertMapper.selectCount(handledWrapper));

            // 按类型统计
            Map<String, Long> typeStats = new LinkedHashMap<>();
            for (int type = 1; type <= 5; type++) {
                LambdaQueryWrapper<DeviceAlert> typeWrapper = new LambdaQueryWrapper<>();
                typeWrapper.eq(DeviceAlert::getAlertType, type);
                Long count = deviceAlertMapper.selectCount(typeWrapper);
                typeStats.put(getAlertTypeName(type), count);
            }
            statistics.put("byType", typeStats);

            // 按级别统计
            Map<String, Long> levelStats = new LinkedHashMap<>();
            for (int level = 1; level <= 4; level++) {
                LambdaQueryWrapper<DeviceAlert> levelWrapper = new LambdaQueryWrapper<>();
                levelWrapper.eq(DeviceAlert::getAlertLevel, level);
                Long count = deviceAlertMapper.selectCount(levelWrapper);
                levelStats.put(getAlertLevelName(level), count);
            }
            statistics.put("byLevel", levelStats);

            return Result.success(statistics, "获取告警统计信息成功");
        } catch (Exception e) {
            log.error("获取告警统计信息失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @Override
    public void checkDeviceStatusAndAlert(DeviceStatus status) {
        try {
            if (status == null || status.getDeviceId() == null) {
                log.warn("设备状态信息不完整，跳过告警检查");
                return;
            }

            // 检查设备是否离线
            if (!status.isOnline()) {
                // 检查是否已有未处理的离线告警
                LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DeviceAlert::getDeviceId, status.getDeviceId())
                       .eq(DeviceAlert::getAlertType, 1) // 1-离线超时
                       .eq(DeviceAlert::getIsHandled, false);
                Long existingCount = deviceAlertMapper.selectCount(wrapper);

                if (existingCount == 0) {
                    log.info("设备 {} 离线，创建离线告警", status.getDeviceId());
                    createAlert(status.getDeviceId(), 1, 3); // 类型1-离线, 级别3-严重
                }
            }

            // 可以根据需要添加其他状态检查逻辑
            // 例如：纸张缺少、碳带缺少、故障等

        } catch (Exception e) {
            log.error("检查设备状态并触发告警失败, deviceId={}", status != null ? status.getDeviceId() : null, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeviceAlertVO> save(DeviceAlert alert) {
        try {
            if (alert.getDeviceId() == null) {
                return Result.error("设备ID不能为空");
            }

            // 设置默认值
            if (alert.getIsHandled() == null) {
                alert.setIsHandled(false);
            }
            if (alert.getAlertStatus() == null) {
                alert.setAlertStatus(0); // 0-未处理
            }
            alert.setCreateTime(LocalDateTime.now());
            alert.setUpdateTime(LocalDateTime.now());

            deviceAlertMapper.insert(alert);

            log.info("保存设备告警: alertId={}, deviceId={}", alert.getAlertId(), alert.getDeviceId());
            return Result.success(convertToVO(alert), "创建告警成功");
        } catch (Exception e) {
            log.error("保存设备告警失败", e);
            return Result.error("创建告警失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeById(Long alertId) {
        try {
            DeviceAlert alert = deviceAlertMapper.selectById(alertId);
            if (alert == null) {
                return Result.error("告警不存在");
            }

            // 逻辑删除（实体配置了@TableLogic，deleteById会执行逻辑删除）
            deviceAlertMapper.deleteById(alertId);

            // 清除缓存
            deviceAlertDataService.clearAlertCache(alertId);

            log.info("删除设备告警: alertId={}", alertId);
            return Result.success(null, "删除告警成功");
        } catch (Exception e) {
            log.error("删除设备告警失败: alertId={}", alertId, e);
            return Result.error("删除告警失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeviceAlertVO> updateById(DeviceAlert alert) {
        try {
            if (alert.getAlertId() == null) {
                return Result.error("告警ID不能为空");
            }

            DeviceAlert existing = deviceAlertMapper.selectById(alert.getAlertId());
            if (existing == null) {
                return Result.error("告警不存在");
            }

            alert.setUpdateTime(LocalDateTime.now());
            deviceAlertMapper.updateById(alert);

            // 清除缓存
            deviceAlertDataService.clearAlertCache(alert.getAlertId());

            log.info("更新设备告警: alertId={}", alert.getAlertId());
            return Result.success(convertToVO(alert), "更新告警成功");
        } catch (Exception e) {
            log.error("更新设备告警失败: alertId={}", alert.getAlertId(), e);
            return Result.error("更新告警失败：" + e.getMessage());
        }
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<DeviceAlert> buildQueryWrapper(DeviceAlertQueryDTO queryDto) {
        LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();

        if (queryDto == null) {
            return wrapper;
        }

        if (queryDto.getDeviceId() != null) {
            wrapper.eq(DeviceAlert::getDeviceId, queryDto.getDeviceId());
        }
        if (queryDto.getAlertType() != null) {
            wrapper.eq(DeviceAlert::getAlertType, queryDto.getAlertType());
        }
        if (queryDto.getAlertLevel() != null) {
            wrapper.eq(DeviceAlert::getAlertLevel, queryDto.getAlertLevel());
        }
        // 告警状态优先：alertStatus 不为空时按 alertStatus 过滤，保留更精确的语义
        // 兼容性：alertStatus 为空但 isHandled 不为空时，仍按 isHandled 过滤
        if (queryDto.getAlertStatus() != null) {
            wrapper.eq(DeviceAlert::getAlertStatus, queryDto.getAlertStatus());
        } else if (queryDto.getIsHandled() != null) {
            wrapper.eq(DeviceAlert::getIsHandled, queryDto.getIsHandled());
        }
        if (queryDto.getStartTime() != null && !queryDto.getStartTime().isEmpty()) {
            wrapper.ge(DeviceAlert::getCreateTime, queryDto.getStartTime());
        }
        if (queryDto.getEndTime() != null && !queryDto.getEndTime().isEmpty()) {
            wrapper.le(DeviceAlert::getCreateTime, queryDto.getEndTime());
        }

        return wrapper;
    }

    /**
     * 将实体转换为VO
     */
    private DeviceAlertVO convertToVO(DeviceAlert alert) {
        DeviceAlertVO vo = new DeviceAlertVO();
        vo.setAlertId(alert.getAlertId());
        vo.setDeviceId(alert.getDeviceId());
        vo.setAlertType(alert.getAlertType());
        vo.setAlertTypeName(getAlertTypeName(alert.getAlertType()));
        vo.setAlertLevel(alert.getAlertLevel());
        vo.setAlertLevelName(getAlertLevelName(alert.getAlertLevel()));
        vo.setAlertMessage(alert.getAlertMessage());
        vo.setIsHandled(alert.getIsHandled());
        vo.setHandleTime(alert.getHandleTime());
        vo.setHandleResult(alert.getHandleResult());
        vo.setHandleUserId(alert.getHandleUserId());
        vo.setCreateTime(alert.getCreateTime());

        // 补充设备信息
        DeviceVO deviceVO = deviceDataService.getDeviceBasicInfo(alert.getDeviceId());
        if (deviceVO != null) {
            vo.setDeviceName(deviceVO.getDeviceName());
            vo.setDeviceCode(deviceVO.getDeviceCode());
        }

        return vo;
    }

    /**
     * 生成默认告警消息
     */
    private String generateDefaultMessage(Integer alertType) {
        if (alertType == null) return "未知告警";

        return switch (alertType) {
            case 1 -> "设备离线超时，请检查网络连接或设备状态";
            case 2 -> "打印机纸张不足，请及时添加纸张";
            case 3 -> "打印机碳带不足，请及时更换碳带";
            case 4 -> "设备发生故障，请联系技术人员";
            case 5 -> "设备需要进行例行维护";
            default -> "未知类型的告警";
        };
    }

    /**
     * 获取告警类型名称
     */
    private String getAlertTypeName(Integer alertType) {
        if (alertType == null) return "未知";
        return switch (alertType) {
            case 1 -> "离线超时";
            case 2 -> "纸张缺";
            case 3 -> "碳带缺";
            case 4 -> "故障";
            case 5 -> "维护提醒";
            default -> "未知";
        };
    }

    /**
     * 获取告警级别名称
     */
    private String getAlertLevelName(Integer alertLevel) {
        if (alertLevel == null) return "未知";
        return switch (alertLevel) {
            case 1 -> "信息";
            case 2 -> "警告";
            case 3 -> "严重";
            case 4 -> "紧急";
            default -> "未知";
        };
    }
}
