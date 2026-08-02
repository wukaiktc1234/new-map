package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.dataservice.schedule.ScheduleShiftTypeDataService;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeCreateDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeReorderDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeUpdateDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeVO;
import com.foodtraceability.entity.schedule.ScheduleShiftType;
import com.foodtraceability.mapper.schedule.ScheduleShiftTypeMapper;
import com.foodtraceability.service.schedule.ScheduleShiftTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 班次类型配置服务实现类
 * 实现班次类型的业务逻辑处理
 */
@Service
public class ScheduleShiftTypeServiceImpl implements ScheduleShiftTypeService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleShiftTypeServiceImpl.class);

    private final ScheduleShiftTypeMapper scheduleShiftTypeMapper;
    private final ScheduleShiftTypeDataService scheduleShiftTypeDataService;

    public ScheduleShiftTypeServiceImpl(
            ScheduleShiftTypeMapper scheduleShiftTypeMapper,
            ScheduleShiftTypeDataService scheduleShiftTypeDataService) {
        this.scheduleShiftTypeMapper = scheduleShiftTypeMapper;
        this.scheduleShiftTypeDataService = scheduleShiftTypeDataService;
    }

    @Override
    public List<ScheduleShiftTypeVO> getActiveShiftTypes(Long storeId) {
        log.debug("查询门店启用班次列表: storeId={}", storeId);
        return scheduleShiftTypeDataService.getActiveShiftTypesByStore(storeId);
    }

    @Override
    public List<ScheduleShiftTypeVO> getAllShiftTypes(Long storeId) {
        log.debug("查询门店全部班次列表: storeId={}", storeId);
        return scheduleShiftTypeDataService.getAllShiftTypesByStore(storeId);
    }

    @Override
    public ScheduleShiftTypeVO getShiftTypeById(Long shiftTypeId) {
        log.debug("查询班次详情: shiftTypeId={}", shiftTypeId);
        return scheduleShiftTypeDataService.getShiftTypeBasicInfo(shiftTypeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleShiftTypeVO createShiftType(ScheduleShiftTypeCreateDTO createDTO) {
        log.info("创建班次类型: shiftCode={}, storeId={}", createDTO.getShiftCode(), createDTO.getStoreId());

        // 校验同一门店下班次编码唯一性
        validateShiftCodeUnique(createDTO.getStoreId(), createDTO.getShiftCode(), null);

        // 构建实体
        ScheduleShiftType entity = new ScheduleShiftType();
        entity.setShiftCode(createDTO.getShiftCode());
        entity.setShiftName(createDTO.getShiftName());
        entity.setStoreId(createDTO.getStoreId());
        entity.setStartTime(createDTO.getStartTime());
        entity.setEndTime(createDTO.getEndTime());
        entity.setColor(createDTO.getColor());
        entity.setIcon(createDTO.getIcon());
        entity.setIsRest(createDTO.getIsRest() != null ? createDTO.getIsRest() : Boolean.FALSE);
        entity.setSortOrder(createDTO.getSortOrder() != null ? createDTO.getSortOrder() : 0);
        entity.setStatus(ScheduleShiftType.STATUS_ACTIVE);

        // 自动计算持续时长
        entity.setDurationMinutes(calculateDuration(createDTO.getStartTime(), createDTO.getEndTime()));

        // 插入数据库
        scheduleShiftTypeMapper.insert(entity);

        // 清除缓存
        scheduleShiftTypeDataService.clearStoreShiftTypeCache(createDTO.getStoreId());

        log.info("班次类型创建成功: shiftTypeId={}, shiftCode={}", entity.getShiftTypeId(), entity.getShiftCode());
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleShiftTypeVO updateShiftType(Long shiftTypeId, ScheduleShiftTypeUpdateDTO updateDTO) {
        log.info("更新班次类型: shiftTypeId={}", shiftTypeId);

        // 查询现有记录
        ScheduleShiftType existingEntity = scheduleShiftTypeMapper.selectById(shiftTypeId);
        if (existingEntity == null) {
            throw new RuntimeException("班次类型不存在");
        }

        // 更新非空字段
        if (updateDTO.getShiftName() != null) {
            existingEntity.setShiftName(updateDTO.getShiftName());
        }
        if (updateDTO.getStartTime() != null) {
            existingEntity.setStartTime(updateDTO.getStartTime());
        }
        if (updateDTO.getEndTime() != null) {
            existingEntity.setEndTime(updateDTO.getEndTime());
        }
        if (updateDTO.getColor() != null) {
            existingEntity.setColor(updateDTO.getColor());
        }
        if (updateDTO.getIcon() != null) {
            existingEntity.setIcon(updateDTO.getIcon());
        }
        if (updateDTO.getIsRest() != null) {
            existingEntity.setIsRest(updateDTO.getIsRest());
        }

        // 如果时间有变更，重新计算持续时长
        if (updateDTO.getStartTime() != null || updateDTO.getEndTime() != null) {
            LocalTime startTime = updateDTO.getStartTime() != null
                    ? updateDTO.getStartTime() : existingEntity.getStartTime();
            LocalTime endTime = updateDTO.getEndTime() != null
                    ? updateDTO.getEndTime() : existingEntity.getEndTime();
            existingEntity.setDurationMinutes(calculateDuration(startTime, endTime));
        }

        // 更新数据库
        scheduleShiftTypeMapper.updateById(existingEntity);

        // 清除缓存
        scheduleShiftTypeDataService.clearShiftTypeCache(String.valueOf(shiftTypeId));
        scheduleShiftTypeDataService.clearStoreShiftTypeCache(existingEntity.getStoreId());

        log.info("班次类型更新成功: shiftTypeId={}", shiftTypeId);
        return convertToVO(existingEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleShiftTypeVO toggleStatus(Long shiftTypeId) {
        log.info("切换班次状态: shiftTypeId={}", shiftTypeId);

        // 查询现有记录
        ScheduleShiftType existingEntity = scheduleShiftTypeMapper.selectById(shiftTypeId);
        if (existingEntity == null) {
            throw new RuntimeException("班次类型不存在");
        }

        // 切换状态
        String newStatus = ScheduleShiftType.STATUS_ACTIVE.equals(existingEntity.getStatus())
                ? ScheduleShiftType.STATUS_INACTIVE
                : ScheduleShiftType.STATUS_ACTIVE;
        existingEntity.setStatus(newStatus);

        // 更新数据库
        scheduleShiftTypeMapper.updateById(existingEntity);

        // 清除缓存
        scheduleShiftTypeDataService.clearShiftTypeCache(String.valueOf(shiftTypeId));
        scheduleShiftTypeDataService.clearStoreShiftTypeCache(existingEntity.getStoreId());

        log.info("班次状态切换成功: shiftTypeId={}, newStatus={}", shiftTypeId, newStatus);
        return convertToVO(existingEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderShiftTypes(ScheduleShiftTypeReorderDTO reorderDTO) {
        Long storeId = reorderDTO.getStoreId();
        List<ScheduleShiftTypeReorderDTO.ReorderItem> items = reorderDTO.getItems();

        log.info("批量调整班次排序: storeId={}, count={}", storeId, items.size());

        for (ScheduleShiftTypeReorderDTO.ReorderItem item : items) {
            LambdaUpdateWrapper<ScheduleShiftType> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ScheduleShiftType::getShiftTypeId, item.getShiftTypeId())
                    .eq(ScheduleShiftType::getStoreId, storeId)
                    .set(ScheduleShiftType::getSortOrder, item.getSortOrder());
            scheduleShiftTypeMapper.update(null, updateWrapper);
        }

        // 清除门店缓存
        scheduleShiftTypeDataService.clearStoreShiftTypeCache(storeId);

        log.info("班次排序调整完成: storeId={}", storeId);
    }

    /**
     * 校验班次编码在门店下的唯一性
     * @param storeId 门店ID
     * @param shiftCode 班次编码
     * @param excludeId 排除的ID(编辑时使用)
     */
    private void validateShiftCodeUnique(Long storeId, String shiftCode, String excludeId) {
        LambdaQueryWrapper<ScheduleShiftType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScheduleShiftType::getStoreId, storeId)
                .eq(ScheduleShiftType::getShiftCode, shiftCode);
        if (excludeId != null && !excludeId.isEmpty()) {
            queryWrapper.ne(ScheduleShiftType::getShiftTypeId, excludeId);
        }

        Long count = scheduleShiftTypeMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new RuntimeException("该班次编码已存在: " + shiftCode);
        }
    }

    /**
     * 计算持续时长(分钟)
     * 支持跨夜场景(如22:00-06:00)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 持续时长(分钟)
     */
    private Integer calculateDuration(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }

        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        // 如果结束时间小于开始时间，说明跨夜了
        if (minutes < 0) {
            minutes += 24 * 60; // 加上24小时
        }

        return (int) minutes;
    }

    /**
     * 将实体转换为视图对象
     * @param entity 班次类型实体
     * @return 视图对象
     */
    private ScheduleShiftTypeVO convertToVO(ScheduleShiftType entity) {
        if (entity == null) {
            return null;
        }

        ScheduleShiftTypeVO vo = new ScheduleShiftTypeVO();
        vo.setShiftTypeId(entity.getShiftTypeId());
        vo.setShiftCode(entity.getShiftCode());
        vo.setShiftName(entity.getShiftName());
        vo.setStoreId(entity.getStoreId());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setColor(entity.getColor());
        vo.setIcon(entity.getIcon());
        vo.setDurationMinutes(entity.getDurationMinutes());
        vo.setIsRest(entity.getIsRest());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        return vo;
    }
}
