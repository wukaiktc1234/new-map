package com.foodtraceability.service.impl;

import com.foodtraceability.dto.PositionBasicInfo;
import com.foodtraceability.entity.Position;
import com.foodtraceability.mapper.PositionMapper;
import com.foodtraceability.service.DepartmentDataService;
import com.foodtraceability.service.PositionDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class PositionDataServiceImpl implements PositionDataService {

    private static final Logger log = LoggerFactory.getLogger(PositionDataServiceImpl.class);

    private final PositionMapper positionMapper;

    private final DepartmentDataService departmentDataService;

    public PositionDataServiceImpl(PositionMapper positionMapper, DepartmentDataService departmentDataService) {
        this.positionMapper = positionMapper;
        this.departmentDataService = departmentDataService;
    }

    @Override
    @Cacheable(value = "positionBasicInfo", key = "#positionId", unless = "#result == null")
    public PositionBasicInfo getPositionBasicInfo(Long positionId) {
        if (positionId == null) {
            return null;
        }

        Position position = positionMapper.selectById(positionId);
        if (position == null) {
            return null;
        }

        return convertToBasicInfo(position);
    }

    @Override
    public Map<Long, PositionBasicInfo> batchGetPositionBasicInfo(List<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            return new HashMap<>();
        }

        // 直接批量查询数据库
        List<Position> positions = positionMapper.selectBatchIds(positionIds);
        Map<Long, Position> positionMap = positions.stream()
            .collect(Collectors.toMap(Position::getId, pos -> pos));

        // 批量查询部门名称
        List<Long> departmentIds = positions.stream()
            .map(Position::getDepartmentId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, String> departmentNames = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            Map<Long, com.foodtraceability.dto.DepartmentBasicInfo> departmentInfoMap =
                departmentDataService.batchGetDepartmentBasicInfo(departmentIds);
            departmentInfoMap.forEach((deptId, deptInfo) -> {
                if (deptInfo != null) {
                    departmentNames.put(deptId, deptInfo.getDepartmentName());
                }
            });
        }

        Map<Long, PositionBasicInfo> result = new HashMap<>();
        for (Long positionId : positionIds) {
            Position position = positionMap.get(positionId);
            if (position != null) {
                result.put(positionId, convertToBasicInfo(position, departmentNames));
            } else {
                result.put(positionId, null);
            }
        }

        return result;
    }

    @Override
    @CacheEvict(value = "positionBasicInfo", key = "#positionId")
    public void clearPositionCache(Long positionId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearPositionBatchCache(List<Long> positionIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearPositionCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "positionBasicInfo", allEntries = true)
    public void clearAllPositionCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private PositionBasicInfo convertToBasicInfo(Position position) {
        return convertToBasicInfo(position, new HashMap<>());
    }

    private PositionBasicInfo convertToBasicInfo(Position position, Map<Long, String> departmentNames) {
        String departmentName = null;
        if (position.getDepartmentId() != null) {
            departmentName = departmentNames.get(position.getDepartmentId());
            if (departmentName == null) {
                departmentName = position.getDepartment();
            }
        }

        Boolean statusValue = null;
        if (position.getStatus() != null) {
            statusValue = "active".equals(position.getStatus());
        }

        return PositionBasicInfo.builder()
            .positionId(position.getId())
            .positionName(position.getPositionName())
            .positionCode(position.getPositionCode())
            .departmentId(position.getDepartmentId())
            .departmentName(departmentName)
            .description(position.getDescription())
            .employeeCount(position.getEmployeeCount())
            .status(statusValue)
            .version(1L)
            .updateTime(position.getUpdatedAt())
            .build();
    }

    @Override
    public List<Long> getAllPositionIds() {
        List<Position> positions = positionMapper.selectList(null);
        return positions.stream()
            .map(Position::getId)
            .collect(Collectors.toList());
    }
}
