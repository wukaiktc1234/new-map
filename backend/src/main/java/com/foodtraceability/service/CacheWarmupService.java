package com.foodtraceability.service;

import java.util.Map;

public interface CacheWarmupService {

    void warmupAll();

    void warmupProducts();

    void warmupUsers();

    void warmupStores();

    void warmupPositions();

    void warmupDepartments();

    void warmupPermissions();

    Map<String, Object> getWarmupStatus();

    void scheduleWarmup();
}
