package com.foodtraceability.dataservice.impl.schedule;

import com.foodtraceability.dataservice.schedule.ScheduleShiftTypeDataService;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeVO;
import com.foodtraceability.entity.schedule.ScheduleShiftType;
import com.foodtraceability.mapper.schedule.ScheduleShiftTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 班次类型数据服务实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class ScheduleShiftTypeDataServiceImpl implements ScheduleShiftTypeDataService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleShiftTypeDataServiceImpl.class);

    private final ScheduleShiftTypeMapper scheduleShiftTypeMapper;

    public ScheduleShiftTypeDataServiceImpl(ScheduleShiftTypeMapper scheduleShiftTypeMapper) {
        this.scheduleShiftTypeMapper = scheduleShiftTypeMapper;
    }

    @Override
    public Map<String, ScheduleShiftTypeVO> batchGetShiftTypeBasicInfo(List<Long> shiftTypeIds) {
        if (shiftTypeIds == null || shiftTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ScheduleShiftType> entities = scheduleShiftTypeMapper.selectBatchIds(shiftTypeIds);
        return entities.stream()
                .filter(entity -> entity != null)
                .collect(Collectors.toMap(
                        entity -> String.valueOf(entity.getShiftTypeId()),
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public ScheduleShiftTypeVO getShiftTypeBasicInfo(Long shiftTypeId) {
        if (shiftTypeId == null) {
            return null;
        }

        ScheduleShiftType entity = scheduleShiftTypeMapper.selectById(shiftTypeId);
        return convertToVO(entity);
    }

    @Override
    public List<ScheduleShiftTypeVO> getActiveShiftTypesByStore(Long storeId) {
        if (storeId == null) {
            return Collections.emptyList();
        }

        List<ScheduleShiftType> entities = scheduleShiftTypeMapper.selectActiveByStoreId(storeId);
        return entities.stream()
                .map(this::convertToVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleShiftTypeVO> getAllShiftTypesByStore(Long storeId) {
        if (storeId == null) {
            return Collections.emptyList();
        }

        List<ScheduleShiftType> entities = scheduleShiftTypeMapper.selectAllByStoreId(storeId);
        return entities.stream()
                .map(this::convertToVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void clearShiftTypeCache(String shiftTypeId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearShiftTypeBatchCache(List<Long> shiftTypeIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearStoreShiftTypeCache(Long storeId) {
        // 缓存已移除，空实现保留接口契约
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
