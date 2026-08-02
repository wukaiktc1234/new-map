package com.foodtraceability.service;

import com.foodtraceability.dto.PositionBasicInfo;

import java.util.List;
import java.util.Map;

public interface PositionDataService {

    Map<Long, PositionBasicInfo> batchGetPositionBasicInfo(List<Long> positionIds);

    PositionBasicInfo getPositionBasicInfo(Long positionId);

    void clearPositionCache(Long positionId);

    void clearPositionBatchCache(List<Long> positionIds);

    void clearAllPositionCache();

    List<Long> getAllPositionIds();
}
