package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.trace.ExpiryAlertQueryDTO;
import com.foodtraceability.dto.trace.ExpiryAlertVO;
import com.foodtraceability.dto.trace.ExpiryDashboardVO;
import com.foodtraceability.dto.trace.ExpiryStatisticsVO;
import com.foodtraceability.entity.ExpiryAlertRecord;
import com.foodtraceability.mapper.ExpiryAlertRecordMapper;
import com.foodtraceability.service.trace.ExpiryAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 临期预警服务实现类
 * 实现临期预警的看板聚合、列表查询、一键报损/退货、数据刷新等业务逻辑
 */
@Service
public class ExpiryAlertServiceImpl extends ServiceImpl<ExpiryAlertRecordMapper, ExpiryAlertRecord>
        implements ExpiryAlertService {

    private static final Logger log = LoggerFactory.getLogger(ExpiryAlertServiceImpl.class);

    /** 预警级别-红色（已过期） */
    private static final String ALERT_LEVEL_RED = "RED";
    /** 预警级别-黄色（7天内） */
    private static final String ALERT_LEVEL_YELLOW = "YELLOW";
    /** 预警级别-绿色（7天外） */
    private static final String ALERT_LEVEL_GREEN = "GREEN";

    /** 处理状态-待处理 */
    private static final String STATUS_PENDING = "PENDING";
    /** 处理状态-已报损 */
    private static final String STATUS_SCRAPPED = "SCRAPPED";
    /** 处理状态-已退货 */
    private static final String STATUS_RETURNED = "RETURNED";
    /** 处理状态-已处理 */
    private static final String STATUS_RESOLVED = "RESOLVED";

    /** 黄色预警阈值（剩余天数 <= 7） */
    private static final int YELLOW_THRESHOLD = 7;

    private final ExpiryAlertRecordMapper expiryAlertRecordMapper;

    public ExpiryAlertServiceImpl(ExpiryAlertRecordMapper expiryAlertRecordMapper) {
        this.expiryAlertRecordMapper = expiryAlertRecordMapper;
    }

    /**
     * 临期预警看板聚合数据
     */
    @Override
    public ExpiryDashboardVO getDashboard() {
        ExpiryDashboardVO dashboard = new ExpiryDashboardVO();

        // 统计各预警级别数量
        dashboard.setRedCount(countByAlertLevel(ALERT_LEVEL_RED));
        dashboard.setYellowCount(countByAlertLevel(ALERT_LEVEL_YELLOW));
        dashboard.setGreenCount(countByAlertLevel(ALERT_LEVEL_GREEN));

        // 统计处理状态数量
        dashboard.setTotalPending(countByHandlingStatus(STATUS_PENDING));
        dashboard.setTotalScrapped(countByHandlingStatus(STATUS_SCRAPPED));
        dashboard.setTotalReturned(countByHandlingStatus(STATUS_RETURNED));

        // 最近预警列表（最多10条，按创建时间倒序）
        LambdaQueryWrapper<ExpiryAlertRecord> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.orderByDesc(ExpiryAlertRecord::getCreateTime).last("LIMIT 10");
        List<ExpiryAlertRecord> recentList = list(recentWrapper);
        dashboard.setRecentAlerts(recentList.stream().map(this::convertToVO).collect(Collectors.toList()));

        return dashboard;
    }

    /**
     * 临期列表（按剩余天数升序排序，剩余天数越少越靠前）
     */
    @Override
    public IPage<ExpiryAlertVO> queryExpiringSoon(ExpiryAlertQueryDTO queryDTO) {
        Page<ExpiryAlertRecord> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = buildQueryWrapper(queryDTO);
        // 临期列表：剩余天数 > 0（未过期），按剩余天数升序
        wrapper.gt(ExpiryAlertRecord::getRemainingDays, 0)
                .orderByAsc(ExpiryAlertRecord::getRemainingDays);

        IPage<ExpiryAlertRecord> pageResult = page(page, wrapper);
        return convertToVOPage(pageResult);
    }

    /**
     * 已过期列表
     */
    @Override
    public IPage<ExpiryAlertVO> queryExpired(ExpiryAlertQueryDTO queryDTO) {
        Page<ExpiryAlertRecord> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = buildQueryWrapper(queryDTO);
        // 已过期：剩余天数 <= 0，按剩余天数升序（过期越久越靠前）
        wrapper.le(ExpiryAlertRecord::getRemainingDays, 0)
                .orderByAsc(ExpiryAlertRecord::getRemainingDays);

        IPage<ExpiryAlertRecord> pageResult = page(page, wrapper);
        return convertToVOPage(pageResult);
    }

    /**
     * 一键报损
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scrap(Long traceCodeId, Long operatorId, String operatorName, String remark) {
        ExpiryAlertRecord entity = getByTraceCodeId(traceCodeId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "临期预警记录不存在");
        }
        // 校验当前状态必须为待处理
        if (!STATUS_PENDING.equals(entity.getHandlingStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前预警记录已处理，无法再次报损");
        }

        entity.setHandlingStatus(STATUS_SCRAPPED);
        entity.setHandlingAction("SCRAPPED");
        entity.setHandledById(operatorId);
        entity.setHandledByName(operatorName);
        entity.setHandledTime(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            entity.setRemark(remark);
        }
        entity.setUpdateTime(LocalDateTime.now());

        boolean result = updateById(entity);
        log.info("一键报损成功: traceCodeId={}, operator={}", traceCodeId, operatorName);
        return result;
    }

    /**
     * 一键退货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean returnGoods(Long traceCodeId, Long operatorId, String operatorName, String remark) {
        ExpiryAlertRecord entity = getByTraceCodeId(traceCodeId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "临期预警记录不存在");
        }
        if (!STATUS_PENDING.equals(entity.getHandlingStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前预警记录已处理，无法再次退货");
        }

        entity.setHandlingStatus(STATUS_RETURNED);
        entity.setHandlingAction("RETURNED");
        entity.setHandledById(operatorId);
        entity.setHandledByName(operatorName);
        entity.setHandledTime(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            entity.setRemark(remark);
        }
        entity.setUpdateTime(LocalDateTime.now());

        boolean result = updateById(entity);
        log.info("一键退货成功: traceCodeId={}, operator={}", traceCodeId, operatorName);
        return result;
    }

    /**
     * 预警统计
     */
    @Override
    public ExpiryStatisticsVO getStatistics() {
        ExpiryStatisticsVO vo = new ExpiryStatisticsVO();
        vo.setTotalAlerts(Math.toIntExact(count()));
        vo.setRedAlerts(countByAlertLevel(ALERT_LEVEL_RED));
        vo.setYellowAlerts(countByAlertLevel(ALERT_LEVEL_YELLOW));
        vo.setGreenAlerts(countByAlertLevel(ALERT_LEVEL_GREEN));
        vo.setPendingCount(countByHandlingStatus(STATUS_PENDING));
        vo.setScrappedCount(countByHandlingStatus(STATUS_SCRAPPED));
        vo.setReturnedCount(countByHandlingStatus(STATUS_RETURNED));
        vo.setResolvedCount(countByHandlingStatus(STATUS_RESOLVED));
        return vo;
    }

    /**
     * 刷新预警数据（定时任务调用）
     * 扫描所有待处理的预警记录，重新计算剩余天数和预警等级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshAlertData() {
        // 查询所有待处理的预警记录
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpiryAlertRecord::getHandlingStatus, STATUS_PENDING);
        List<ExpiryAlertRecord> pendingList = list(wrapper);

        int refreshCount = 0;
        LocalDate today = LocalDate.now();
        for (ExpiryAlertRecord record : pendingList) {
            if (record.getExpiryDate() == null) {
                continue;
            }
            // 实时计算剩余天数
            int remainingDays = (int) ChronoUnit.DAYS.between(today, record.getExpiryDate());
            String newLevel = calculateAlertLevel(remainingDays);

            // 仅在数据变化时更新，减少无意义的写操作
            boolean changed = false;
            if (record.getRemainingDays() == null || record.getRemainingDays() != remainingDays) {
                record.setRemainingDays(remainingDays);
                changed = true;
            }
            if (!newLevel.equals(record.getAlertLevel())) {
                record.setAlertLevel(newLevel);
                changed = true;
            }
            if (changed) {
                record.setUpdateTime(LocalDateTime.now());
                updateById(record);
                refreshCount++;
            }
        }
        log.info("刷新临期预警数据完成: 共刷新 {} 条记录", refreshCount);
        return refreshCount;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据追溯码ID查询预警记录
     */
    private ExpiryAlertRecord getByTraceCodeId(Long traceCodeId) {
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpiryAlertRecord::getTraceCodeId, traceCodeId)
                .orderByDesc(ExpiryAlertRecord::getCreateTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 按预警级别统计数量
     */
    private int countByAlertLevel(String level) {
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpiryAlertRecord::getAlertLevel, level);
        return Math.toIntExact(count(wrapper));
    }

    /**
     * 按处理状态统计数量
     */
    private int countByHandlingStatus(String status) {
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpiryAlertRecord::getHandlingStatus, status);
        return Math.toIntExact(count(wrapper));
    }

    /**
     * 构建查询条件包装器
     */
    private LambdaQueryWrapper<ExpiryAlertRecord> buildQueryWrapper(ExpiryAlertQueryDTO queryDTO) {
        LambdaQueryWrapper<ExpiryAlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getTraceCode() != null && !queryDTO.getTraceCode().isEmpty()) {
            wrapper.eq(ExpiryAlertRecord::getTraceCode, queryDTO.getTraceCode());
        }
        if (queryDTO.getTraceType() != null && !queryDTO.getTraceType().isEmpty()) {
            wrapper.eq(ExpiryAlertRecord::getTraceType, queryDTO.getTraceType());
        }
        if (queryDTO.getMaterialName() != null && !queryDTO.getMaterialName().isEmpty()) {
            wrapper.like(ExpiryAlertRecord::getMaterialName, queryDTO.getMaterialName());
        }
        if (queryDTO.getBatchNo() != null && !queryDTO.getBatchNo().isEmpty()) {
            wrapper.eq(ExpiryAlertRecord::getBatchNo, queryDTO.getBatchNo());
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(ExpiryAlertRecord::getSupplierId, queryDTO.getSupplierId());
        }
        if (queryDTO.getAlertLevel() != null && !queryDTO.getAlertLevel().isEmpty()) {
            wrapper.eq(ExpiryAlertRecord::getAlertLevel, queryDTO.getAlertLevel());
        }
        if (queryDTO.getHandlingStatus() != null && !queryDTO.getHandlingStatus().isEmpty()) {
            wrapper.eq(ExpiryAlertRecord::getHandlingStatus, queryDTO.getHandlingStatus());
        }
        if (queryDTO.getExpiryDateStart() != null) {
            wrapper.ge(ExpiryAlertRecord::getExpiryDate, queryDTO.getExpiryDateStart());
        }
        if (queryDTO.getExpiryDateEnd() != null) {
            wrapper.le(ExpiryAlertRecord::getExpiryDate, queryDTO.getExpiryDateEnd());
        }
        return wrapper;
    }

    /**
     * 实体分页转VO分页
     */
    private IPage<ExpiryAlertVO> convertToVOPage(IPage<ExpiryAlertRecord> pageResult) {
        Page<ExpiryAlertVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 实体转VO
     */
    private ExpiryAlertVO convertToVO(ExpiryAlertRecord entity) {
        ExpiryAlertVO vo = new ExpiryAlertVO();
        vo.setAlertId(entity.getAlertId());
        vo.setTraceCodeId(entity.getTraceCodeId());
        vo.setTraceCode(entity.getTraceCode());
        vo.setTraceType(entity.getTraceType());
        vo.setTraceTypeName(getTraceTypeName(entity.getTraceType()));
        vo.setMaterialId(entity.getMaterialId());
        vo.setMaterialName(entity.getMaterialName());
        vo.setBatchNo(entity.getBatchNo());
        vo.setSupplierId(entity.getSupplierId());
        vo.setSupplierName(entity.getSupplierName());
        vo.setProductionDate(entity.getProductionDate());
        vo.setExpiryDate(entity.getExpiryDate());
        // 实时计算剩余天数，确保展示数据准确
        if (entity.getExpiryDate() != null) {
            int remainingDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), entity.getExpiryDate());
            vo.setRemainingDays(remainingDays);
            // 实时刷新预警级别
            vo.setAlertLevel(calculateAlertLevel(remainingDays));
        } else {
            vo.setRemainingDays(entity.getRemainingDays());
            vo.setAlertLevel(entity.getAlertLevel());
        }
        vo.setAlertLevelName(getAlertLevelName(vo.getAlertLevel()));
        vo.setHandlingStatus(entity.getHandlingStatus());
        vo.setHandlingStatusName(getHandlingStatusName(entity.getHandlingStatus()));
        vo.setHandlingAction(entity.getHandlingAction());
        vo.setHandledById(entity.getHandledById());
        vo.setHandledByName(entity.getHandledByName());
        vo.setHandledTime(entity.getHandledTime());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 计算预警等级
     * remainingDays <= 0 → RED, 1-7 → YELLOW, > 7 → GREEN
     * @param remainingDays 剩余天数
     * @return 预警级别
     */
    private static String calculateAlertLevel(int remainingDays) {
        if (remainingDays <= 0) {
            return ALERT_LEVEL_RED;
        } else if (remainingDays <= YELLOW_THRESHOLD) {
            return ALERT_LEVEL_YELLOW;
        } else {
            return ALERT_LEVEL_GREEN;
        }
    }

    /**
     * 追溯类型中文名映射
     * MATERIAL→原料, FOOD→食品
     */
    private static String getTraceTypeName(String traceType) {
        if (traceType == null) {
            return "未知";
        }
        switch (traceType) {
            case "MATERIAL":
                return "原料";
            case "FOOD":
                return "食品";
            default:
                return "未知";
        }
    }

    /**
     * 预警级别中文名映射
     * RED→红色预警, YELLOW→黄色预警, GREEN→绿色预警
     */
    private static String getAlertLevelName(String level) {
        if (level == null) {
            return "未知";
        }
        switch (level) {
            case ALERT_LEVEL_RED:
                return "红色预警";
            case ALERT_LEVEL_YELLOW:
                return "黄色预警";
            case ALERT_LEVEL_GREEN:
                return "绿色预警";
            default:
                return "未知";
        }
    }

    /**
     * 处理状态中文名映射
     * PENDING→待处理, SCRAPPED→已报损, RETURNED→已退货, RESOLVED→已处理
     */
    private static String getHandlingStatusName(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待处理";
            case STATUS_SCRAPPED:
                return "已报损";
            case STATUS_RETURNED:
                return "已退货";
            case STATUS_RESOLVED:
                return "已处理";
            default:
                return "未知";
        }
    }
}
