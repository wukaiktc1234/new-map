package com.foodtraceability.dataservice.impl.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dataservice.schedule.SchedulePlanDataService;
import com.foodtraceability.dto.schedule.SchedulePlanVO;
import com.foodtraceability.entity.schedule.SchedulePlan;
import com.foodtraceability.mapper.schedule.SchedulePlanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排班方案数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 缓存键格式: schedule-plan:basic:{planId}
 */
@Service
public class SchedulePlanDataServiceImpl implements SchedulePlanDataService {

    private static final Logger log = LoggerFactory.getLogger(SchedulePlanDataServiceImpl.class);

    /** 缓存名称 */
    private static final String CACHE_NAME = "schedule-plan";
    /** 门店缓存名称前缀 */
    private static final String STORE_CACHE_PREFIX = "store:";
    /** 缓存过期时间：24小时（排班方案变更频率中等） */
    private static final long CACHE_EXPIRE_SECONDS = 86400;

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SchedulePlanMapper schedulePlanMapper;

    public SchedulePlanDataServiceImpl(SchedulePlanMapper schedulePlanMapper) {
        this.schedulePlanMapper = schedulePlanMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#planIds", unless = "#result == null || #result.isEmpty()")
    public Map<String, SchedulePlanVO> batchGetPlanBasicInfo(List<String> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SchedulePlan> entities = schedulePlanMapper.selectBatchIds(planIds);
        return entities.stream()
                .filter(entity -> entity != null)
                .collect(Collectors.toMap(
                        SchedulePlan::getPlanId,
                        this::convertToBasicVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #planId", unless = "#result == null")
    public SchedulePlanVO getPlanBasicInfo(String planId) {
        if (planId == null || planId.isEmpty()) {
            return null;
        }

        SchedulePlan entity = schedulePlanMapper.selectById(planId);
        return convertToBasicVO(entity);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'" + STORE_CACHE_PREFIX + "' + #storeId",
            unless = "#result == null || #result.isEmpty()")
    public List<SchedulePlanVO> getPlansByStore(Long storeId) {
        if (storeId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<SchedulePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlan::getStoreId, storeId)
               .orderByDesc(SchedulePlan::getUpdateTime);

        List<SchedulePlan> entities = schedulePlanMapper.selectList(wrapper);
        return entities.stream()
                .map(this::convertToBasicVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'basic:' + #planId")
    public void clearPlanCache(String planId) {
        log.debug("清除排班方案缓存: planId={}", planId);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearPlanBatchCache(List<String> planIds) {
        log.debug("批量清除排班方案缓存: count={}", planIds != null ? planIds.size() : 0);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearStorePlanCache(Long storeId) {
        log.debug("清除门店排班方案缓存: storeId={}", storeId);
    }

    // ==================== 私有方法 ====================

    /**
     * 将实体转换为基本信息视图对象（不含条目列表）
     * 用于缓存和批量查询场景，仅包含基础字段
     * @param entity 排班方案实体
     * @return 基本信息视图对象
     */
    private SchedulePlanVO convertToBasicVO(SchedulePlan entity) {
        if (entity == null) {
            return null;
        }

        SchedulePlanVO vo = new SchedulePlanVO();
        vo.setPlanId(entity.getPlanId());
        vo.setPlanName(entity.getPlanName());
        vo.setStoreId(entity.getStoreId());
        vo.setStatus(entity.getStatus());

        // 设置状态中文名称
        vo.setStatusName(getStatusDisplayName(entity.getStatus()));

        vo.setVersion(entity.getVersion());
        vo.setTemplateId(entity.getTemplateId());
        vo.setEmployeeCount(entity.getEmployeeCount());

        // 日期格式化
        if (entity.getStartDate() != null) {
            vo.setStartDate(entity.getStartDate().format(DATE_FORMATTER));
        }
        if (entity.getEndDate() != null) {
            vo.setEndDate(entity.getEndDate().format(DATE_FORMATTER));
        }

        // 工时显示
        vo.setTotalWorkHoursDisplay(formatWorkHours(entity.getTotalWorkHours()));

        // 发布信息
        vo.setPublisherId(entity.getPublisherId());
        vo.setPublisherName(entity.getPublisherName());
        vo.setPublishTime(entity.getPublishTime());

        // 撤回信息
        vo.setWithdrawerId(entity.getWithdrawerId());
        vo.setWithdrawerName(entity.getWithdrawerName());
        vo.setWithdrawTime(entity.getWithdrawTime());
        vo.setWithdrawReason(entity.getWithdrawReason());

        // 同步信息
        vo.setSyncStatus(entity.getSyncStatus());
        vo.setSyncToAttendanceTime(entity.getSyncToAttendanceTime());
        vo.setSyncMessage(entity.getSyncMessage());

        // 时间戳
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        return vo;
    }

    /**
     * 获取状态的中文名称
     * @param status 状态编码
     * @return 中文名称
     */
    private String getStatusDisplayName(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case "draft":
                return "草稿";
            case "published":
                return "已发布";
            case "executing":
                return "执行中";
            case "archived":
                return "已归档";
            default:
                return status;
        }
    }

    /**
     * 格式化工时显示（分钟转小时）
     * @param totalMinutes 总分钟数
     * @return 格式化的工时字符串
     */
    private String formatWorkHours(Integer totalMinutes) {
        if (totalMinutes == null || totalMinutes == 0) {
            return "0小时";
        }
        double hours = totalMinutes / 60.0;
        if (hours == Math.floor(hours)) {
            return (int) hours + "小时";
        }
        return String.format("%.1f小时", hours);
    }
}
