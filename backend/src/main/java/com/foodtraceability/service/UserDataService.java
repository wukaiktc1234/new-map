package com.foodtraceability.service;

import com.foodtraceability.dto.UserBasicInfo;

import java.util.List;
import java.util.Map;

public interface UserDataService {

    Map<String, UserBasicInfo> batchGetUserBasicInfo(List<String> userIds);

    UserBasicInfo getUserBasicInfo(String userId);

    void clearUserCache(String userId);

    void clearUserBatchCache(List<String> userIds);

    void clearAllUserCache();

    List<String> getAllUserIds();
}
