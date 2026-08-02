package com.foodtraceability.service.impl;

import com.foodtraceability.service.CacheWarmupService;
import com.foodtraceability.service.ProductDataService;
import com.foodtraceability.service.UserDataService;
import com.foodtraceability.service.StoreDataService;
import com.foodtraceability.service.PositionDataService;
import com.foodtraceability.service.DepartmentDataService;
import com.foodtraceability.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存预热服务实现（基于内存，已移除 Redis 依赖）
 * 预热通过触发 DataService 查询将数据加载到各 DataService 内部缓存
 */
@Service
public class CacheWarmupServiceImpl implements CacheWarmupService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupServiceImpl.class);

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     */
    public CacheWarmupServiceImpl(ProductDataService productDataService, UserDataService userDataService, StoreDataService storeDataService, PositionDataService positionDataService, DepartmentDataService departmentDataService, PermissionService permissionService) {
        this.productDataService = productDataService;
        this.userDataService = userDataService;
        this.storeDataService = storeDataService;
        this.positionDataService = positionDataService;
        this.departmentDataService = departmentDataService;
        this.permissionService = permissionService;
    }

    private final ProductDataService productDataService;

    private final UserDataService userDataService;

    private final StoreDataService storeDataService;

    private final PositionDataService positionDataService;

    private final DepartmentDataService departmentDataService;

    private final PermissionService permissionService;

    private final Map<String, WarmupStatus> warmupStatusMap = new ConcurrentHashMap<>();

    @Override
    public void warmupAll() {
        log.info("开始全量缓存预热，时间：{}", LocalDateTime.now());

        List<CompletableFuture<Void>> futures = List.of(
            CompletableFuture.runAsync(this::warmupProducts),
            CompletableFuture.runAsync(this::warmupUsers),
            CompletableFuture.runAsync(this::warmupStores),
            CompletableFuture.runAsync(this::warmupPositions),
            CompletableFuture.runAsync(this::warmupDepartments),
            CompletableFuture.runAsync(this::warmupPermissions)
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("全量缓存预热完成，时间：{}", LocalDateTime.now());
    }

    @Override
    public void warmupProducts() {
        WarmupStatus status = new WarmupStatus("products", "预热中", 0, 0);
        warmupStatusMap.put("products", status);

        try {
            log.info("开始预热商品数据缓存");
            List<String> productIds = productDataService.getAllProductIds();
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (String productId : productIds) {
                try {
                    productDataService.getProductBasicInfo(productId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("预热商品数据失败: productId={}", productId, e);
                }
            }

            status.setStatus("完成");
            status.setSuccessCount(successCount.get());
            status.setFailCount(failCount.get());
            log.info("商品数据缓存预热完成，成功：{}，失败：{}", successCount.get(), failCount.get());
        } catch (Exception e) {
            status.setStatus("失败");
            log.error("商品数据缓存预热异常", e);
        }
    }

    @Override
    public void warmupUsers() {
        WarmupStatus status = new WarmupStatus("users", "预热中", 0, 0);
        warmupStatusMap.put("users", status);

        try {
            log.info("开始预热用户数据缓存");
            List<String> userIds = userDataService.getAllUserIds();
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (String userId : userIds) {
                try {
                    userDataService.getUserBasicInfo(userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("预热用户数据失败: userId={}", userId, e);
                }
            }

            status.setStatus("完成");
            status.setSuccessCount(successCount.get());
            status.setFailCount(failCount.get());
            log.info("用户数据缓存预热完成，成功：{}，失败：{}", successCount.get(), failCount.get());
        } catch (Exception e) {
            status.setStatus("失败");
            log.error("用户数据缓存预热异常", e);
        }
    }

    @Override
    public void warmupStores() {
        WarmupStatus status = new WarmupStatus("stores", "预热中", 0, 0);
        warmupStatusMap.put("stores", status);

        try {
            log.info("开始预热门店数据缓存");
            List<String> storeIds = storeDataService.getAllStoreIds();
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (String storeId : storeIds) {
                try {
                    storeDataService.getStoreBasicInfo(storeId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("预热门店数据失败: storeId={}", storeId, e);
                }
            }

            status.setStatus("完成");
            status.setSuccessCount(successCount.get());
            status.setFailCount(failCount.get());
            log.info("门店数据缓存预热完成，成功：{}，失败：{}", successCount.get(), failCount.get());
        } catch (Exception e) {
            status.setStatus("失败");
            log.error("门店数据缓存预热异常", e);
        }
    }

    @Override
    public void warmupPositions() {
        WarmupStatus status = new WarmupStatus("positions", "预热中", 0, 0);
        warmupStatusMap.put("positions", status);

        try {
            log.info("开始预热职位数据缓存");
            List<Long> positionIds = positionDataService.getAllPositionIds();
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (Long positionId : positionIds) {
                try {
                    positionDataService.getPositionBasicInfo(positionId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("预热职位数据失败: positionId={}", positionId, e);
                }
            }

            status.setStatus("完成");
            status.setSuccessCount(successCount.get());
            status.setFailCount(failCount.get());
            log.info("职位数据缓存预热完成，成功：{}，失败：{}", successCount.get(), failCount.get());
        } catch (Exception e) {
            status.setStatus("失败");
            log.error("职位数据缓存预热异常", e);
        }
    }

    @Override
    public void warmupDepartments() {
        WarmupStatus status = new WarmupStatus("departments", "预热中", 0, 0);
        warmupStatusMap.put("departments", status);

        try {
            log.info("开始预热部门数据缓存");
            List<Long> departmentIds = departmentDataService.getAllDepartmentIds();
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (Long departmentId : departmentIds) {
                try {
                    departmentDataService.getDepartmentBasicInfo(departmentId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("预热部门数据失败: departmentId={}", departmentId, e);
                }
            }

            status.setStatus("完成");
            status.setSuccessCount(successCount.get());
            status.setFailCount(failCount.get());
            log.info("部门数据缓存预热完成，成功：{}，失败：{}", successCount.get(), failCount.get());
        } catch (Exception e) {
            status.setStatus("失败");
            log.error("部门数据缓存预热异常", e);
        }
    }

    @Override
    public void warmupPermissions() {
        WarmupStatus status = new WarmupStatus("permissions", "预热中", 0, 0);
        warmupStatusMap.put("permissions", status);

        try {
            log.info("开始预热权限数据缓存");
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 使用现有方法预热所有权限数据
            try {
                permissionService.getAllPermissions();
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                log.warn("预热所有权限数据失败", e);
            }

            // 预热菜单权限数据
            try {
                permissionService.getPermissionsByType("menu");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                log.warn("预热菜单权限数据失败", e);
            }

            // 预热权限树结构
            try {
                permissionService.getPermissionTree();
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                log.warn("预热权限树结构失败", e);
            }

            status.setStatus("完成");
            status.setSuccessCount(successCount.get());
            status.setFailCount(failCount.get());
            log.info("权限数据缓存预热完成，成功：{}，失败：{}", successCount.get(), failCount.get());
        } catch (Exception e) {
            status.setStatus("失败");
            log.error("权限数据缓存预热异常", e);
        }
    }

    @Override
    public Map<String, Object> getWarmupStatus() {
        Map<String, Object> result = new HashMap<>();
        warmupStatusMap.forEach((key, status) -> {
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("status", status.getStatus());
            statusInfo.put("successCount", status.getSuccessCount());
            statusInfo.put("failCount", status.getFailCount());
            result.put(key, statusInfo);
        });
        return result;
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleWarmup() {
        log.info("定时缓存预热任务开始执行");
        warmupAll();
        log.info("定时缓存预热任务执行完成");
    }

    private static class WarmupStatus {
        private String type;
        private String status;
        private int successCount;
        private int failCount;

        public WarmupStatus(String type, String status, int successCount, int failCount) {
            this.type = type;
            this.status = status;
            this.successCount = successCount;
            this.failCount = failCount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }
    }
}
