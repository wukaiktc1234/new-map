package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.AssetMaster;
import com.foodtraceability.entity.AssetFlowRecord;
import com.foodtraceability.entity.AssetDepreciationRecord;
import com.foodtraceability.mapper.AssetMasterMapper;
import com.foodtraceability.mapper.AssetFlowRecordMapper;
import com.foodtraceability.mapper.AssetDepreciationRecordMapper;
import com.foodtraceability.service.AssetMasterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssetMasterServiceImpl extends ServiceImpl<AssetMasterMapper, AssetMaster> implements AssetMasterService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetMasterServiceImpl.class);
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    /** 元转分乘数 */
    private static final BigDecimal YUAN_TO_FEN = BigDecimal.valueOf(100L);

    private final AssetMasterMapper assetMasterMapper;
    private final AssetFlowRecordMapper assetFlowRecordMapper;
    private final AssetDepreciationRecordMapper assetDepreciationRecordMapper;

    // 临时内存存储（后续替换为数据库持久化）
    private final Map<String, Object> syncSettingsStore = new HashMap<>();
    private final List<Map<String, Object>> syncLogStore = new ArrayList<>();
    private final List<Map<String, Object>> disposalStore = new ArrayList<>();

    @Override
    public List<Map<String, Object>> countByStatus() {
        return assetMasterMapper.countByStatus();
    }

    @Override
    public List<Map<String, Object>> countByType() {
        return assetMasterMapper.countByType();
    }

    @Override
    public List<Map<String, Object>> countByStore() {
        return assetMasterMapper.countByStore();
    }

    @Override
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        LambdaQueryWrapper<AssetMaster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetMaster::getDeleted, 0);
        long total = count(wrapper);
        stats.put("total", total);
        stats.put("idle", assetMasterMapper.countStatus("idle"));
        stats.put("inUse", assetMasterMapper.countStatus("in_use"));
        stats.put("repairing", assetMasterMapper.countStatus("repairing"));
        stats.put("damaged", assetMasterMapper.countStatus("damaged"));
        stats.put("scrapped", assetMasterMapper.countStatus("scrapped"));
        return stats;
    }

    @Override
    public AssetMaster findByQrCode(String qrCode) {
        return assetMasterMapper.findByQrCode(qrCode);
    }

    @Override
    public AssetMaster findByAssetCode(String assetCode) {
        return assetMasterMapper.findByAssetCode(assetCode);
    }

    @Override
    @Transactional
    public void bindOrder(String assetCode, String orderId, Long kitchenOrderId, String operatorName) {
        AssetMaster asset = findByAssetCode(assetCode);
        if (asset == null) {
            throw new RuntimeException("资产不存在: " + assetCode);
        }
        if (!"idle".equals(asset.getStatus())) {
            throw new RuntimeException("资产当前状态不允许绑定: " + asset.getStatus());
        }
        String oldStatus = asset.getStatus();
        asset.setStatus("in_use");
        asset.setCurrentOrderId(orderId);
        asset.setCurrentKitchenOrderId(kitchenOrderId);
        asset.setBindTime(LocalDateTime.now());
        asset.setUseCount(asset.getUseCount() + 1);
        updateById(asset);
        AssetFlowRecord flow = new AssetFlowRecord();
        flow.setAssetId(asset.getId());
        flow.setAssetCode(asset.getAssetCode());
        flow.setAssetName(asset.getAssetName());
        flow.setFlowType("bind_order");
        flow.setFromStatus(oldStatus);
        flow.setToStatus("in_use");
        flow.setRelatedOrderId(orderId);
        flow.setRelatedKitchenOrderId(kitchenOrderId);
        flow.setOperatorName(operatorName);
        flow.setFlowTime(LocalDateTime.now());
        assetFlowRecordMapper.insert(flow);
        log.info("资产绑定订单: {} -> {}", assetCode, orderId);
    }

    @Override
    @Transactional
    public void releaseAsset(String assetCode, String operatorName) {
        AssetMaster asset = findByAssetCode(assetCode);
        if (asset == null) {
            throw new RuntimeException("资产不存在: " + assetCode);
        }
        String oldStatus = asset.getStatus();
        asset.setStatus("idle");
        asset.setCurrentOrderId(null);
        asset.setCurrentKitchenOrderId(null);
        asset.setBindTime(null);
        asset.setLastUseTime(LocalDateTime.now());
        updateById(asset);
        AssetFlowRecord flow = new AssetFlowRecord();
        flow.setAssetId(asset.getId());
        flow.setAssetCode(asset.getAssetCode());
        flow.setAssetName(asset.getAssetName());
        flow.setFlowType("release");
        flow.setFromStatus(oldStatus);
        flow.setToStatus("idle");
        flow.setOperatorName(operatorName);
        flow.setFlowTime(LocalDateTime.now());
        assetFlowRecordMapper.insert(flow);
        log.info("资产释放: {}", assetCode);
    }

    @Override
    @Transactional
    public void allocateToStore(Long assetId, Long storeId, String storeName, String operatorName) {
        AssetMaster asset = getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        String oldStoreName = asset.getStoreName();
        asset.setStoreId(storeId);
        asset.setStoreName(storeName);
        asset.setStatus("allocated");
        updateById(asset);
        AssetFlowRecord flow = new AssetFlowRecord();
        flow.setAssetId(asset.getId());
        flow.setAssetCode(asset.getAssetCode());
        flow.setAssetName(asset.getAssetName());
        flow.setFlowType("allocate");
        flow.setFromStoreName(oldStoreName);
        flow.setToStoreId(storeId);
        flow.setToStoreName(storeName);
        flow.setOperatorName(operatorName);
        flow.setFlowTime(LocalDateTime.now());
        assetFlowRecordMapper.insert(flow);
        log.info("资产分配门店: {} -> {}", asset.getAssetCode(), storeName);
    }

    @Override
    @Transactional
    public void startRepair(Long assetId, String operatorName) {
        AssetMaster asset = getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        String oldStatus = asset.getStatus();
        asset.setStatus("repairing");
        updateById(asset);
        AssetFlowRecord flow = new AssetFlowRecord();
        flow.setAssetId(asset.getId());
        flow.setAssetCode(asset.getAssetCode());
        flow.setAssetName(asset.getAssetName());
        flow.setFlowType("repair");
        flow.setFromStatus(oldStatus);
        flow.setToStatus("repairing");
        flow.setOperatorName(operatorName);
        flow.setFlowTime(LocalDateTime.now());
        assetFlowRecordMapper.insert(flow);
        log.info("资产开始维修: {}", asset.getAssetCode());
    }

    @Override
    @Transactional
    public void completeRepair(Long assetId, String operatorName) {
        AssetMaster asset = getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        String oldStatus = asset.getStatus();
        asset.setStatus("idle");
        updateById(asset);
        AssetFlowRecord flow = new AssetFlowRecord();
        flow.setAssetId(asset.getId());
        flow.setAssetCode(asset.getAssetCode());
        flow.setAssetName(asset.getAssetName());
        flow.setFlowType("repair_complete");
        flow.setFromStatus(oldStatus);
        flow.setToStatus("idle");
        flow.setOperatorName(operatorName);
        flow.setFlowTime(LocalDateTime.now());
        assetFlowRecordMapper.insert(flow);
        log.info("资产维修完成: {}", asset.getAssetCode());
    }

    @Override
    @Transactional
    public void scrapAsset(Long assetId, String reason, String operatorName) {
        AssetMaster asset = getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        String oldStatus = asset.getStatus();
        asset.setStatus("scrapped");
        updateById(asset);
        AssetFlowRecord flow = new AssetFlowRecord();
        flow.setAssetId(asset.getId());
        flow.setAssetCode(asset.getAssetCode());
        flow.setAssetName(asset.getAssetName());
        flow.setFlowType("scrap");
        flow.setFromStatus(oldStatus);
        flow.setToStatus("scrapped");
        flow.setReason(reason);
        flow.setOperatorName(operatorName);
        flow.setFlowTime(LocalDateTime.now());
        assetFlowRecordMapper.insert(flow);
        log.info("资产报废: {}, 原因: {}", asset.getAssetCode(), reason);
    }

    @Override
    public String generateAssetCode(String assetType) {
        String prefix = "ASSET";
        if ("tray".equals(assetType)) {
            prefix = "TRAY";
        } else if ("equipment".equals(assetType)) {
            prefix = "EQ";
        } else if ("furniture".equals(assetType)) {
            prefix = "FN";
        }
        LambdaQueryWrapper<AssetMaster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetMaster::getDeleted, 0).likeRight(AssetMaster::getAssetCode, prefix).orderByDesc(AssetMaster::getAssetCode).last("LIMIT 1");
        AssetMaster lastAsset = getOne(wrapper);
        int nextNum = 1;
        if (lastAsset != null && lastAsset.getAssetCode() != null) {
            String code = lastAsset.getAssetCode();
            String numStr = code.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                nextNum = Integer.parseInt(numStr) + 1;
            }
        }
        return String.format("%s%03d", prefix, nextNum);
    }

    public AssetMasterServiceImpl(final AssetMasterMapper assetMasterMapper,
                                  final AssetFlowRecordMapper assetFlowRecordMapper,
                                  final AssetDepreciationRecordMapper assetDepreciationRecordMapper) {
        this.assetMasterMapper = assetMasterMapper;
        this.assetFlowRecordMapper = assetFlowRecordMapper;
        this.assetDepreciationRecordMapper = assetDepreciationRecordMapper;

        // 初始化默认同步设置
        syncSettingsStore.put("autoSync", false);
        syncSettingsStore.put("syncInterval", 24);
        syncSettingsStore.put("syncScope", "all");
        syncSettingsStore.put("notifyOnComplete", false);
    }

    // ========== 同步设置实现 ==========

    @Override
    public Map<String, Object> getSyncSettings() {
        log.info("获取同步设置");
        return new HashMap<>(syncSettingsStore);
    }

    @Override
    public boolean updateSyncSettings(Map<String, Object> settings) {
        log.info("更新同步设置: {}", settings);
        syncSettingsStore.putAll(settings);
        return true;
    }

    @Override
    public String syncAssets(List<String> assetIds) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        log.info("同步资产数据, 资产数量: {}, 任务ID: {}", assetIds != null ? assetIds.size() : 0, taskId);
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("taskId", taskId);
        logEntry.put("assetCount", assetIds != null ? assetIds.size() : 0);
        logEntry.put("status", "completed");
        logEntry.put("syncTime", LocalDateTime.now().toString());
        logEntry.put("assetIds", assetIds);
        syncLogStore.add(logEntry);
        return taskId;
    }

    @Override
    public Map<String, Object> getSyncLogs(int page, int size) {
        log.info("获取同步日志, 页码: {}, 每页条数: {}", page, size);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, syncLogStore.size());
        List<Map<String, Object>> records;
        if (fromIndex >= syncLogStore.size()) {
            records = new ArrayList<>();
        } else {
            records = new ArrayList<>(syncLogStore.subList(fromIndex, toIndex));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", syncLogStore.size());
        result.put("current", page);
        result.put("size", size);
        return result;
    }

    // ========== 折旧实现（DB持久化） ==========

    @Override
    public List<Map<String, Object>> getDepreciationList(Long assetId, int page, int size) {
        log.info("获取折旧列表, 资产ID: {}, 页码: {}, 每页条数: {}", assetId, page, size);
        Page<AssetDepreciationRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AssetDepreciationRecord> wrapper = new LambdaQueryWrapper<>();
        if (assetId != null) {
            wrapper.eq(AssetDepreciationRecord::getAssetId, assetId);
        }
        wrapper.orderByDesc(AssetDepreciationRecord::getPeriod);
        Page<AssetDepreciationRecord> resultPage = assetDepreciationRecordMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        if (resultPage.getRecords() != null) {
            for (AssetDepreciationRecord record : resultPage.getRecords()) {
                records.add(convertDepreciationRecordToMap(record));
            }
        }
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calculateDepreciation(Long assetId, String method, int months) {
        log.info("计提折旧, 资产ID: {}, 方法: {}, 月数: {}", assetId, method, months);

        AssetMaster asset = getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在: " + assetId);
        }

        // 原值与累计折旧（元，BigDecimal）
        BigDecimal originalValueYuan = asset.getOriginalValue() != null
                ? asset.getOriginalValue() : BigDecimal.ZERO;
        BigDecimal accumulatedYuan = asset.getAccumulatedDepreciation() != null
                ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO;

        // 计算本期折旧额（元）：按月数平均摊销
        // 若未指定 months 或为 0，按一个月计算
        int effectiveMonths = months > 0 ? months : 1;
        Integer usefulLifeMonths = asset.getUsefulLifeMonths();
        BigDecimal thisPeriodYuan;
        if (usefulLifeMonths != null && usefulLifeMonths > 0) {
            // 直线法：原值 / 预计使用月数 * 本期月数
            thisPeriodYuan = originalValueYuan
                    .multiply(BigDecimal.valueOf(effectiveMonths))
                    .divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP);
        } else {
            // 无预计使用月数信息时，按 months 平均摊销原值
            thisPeriodYuan = originalValueYuan
                    .divide(BigDecimal.valueOf(effectiveMonths), 2, RoundingMode.HALF_UP);
        }

        // 防止超过剩余可折旧额
        BigDecimal remainingDepreciable = originalValueYuan.subtract(accumulatedYuan);
        if (thisPeriodYuan.compareTo(remainingDepreciable) > 0) {
            thisPeriodYuan = remainingDepreciable.max(BigDecimal.ZERO);
        }

        BigDecimal newAccumulatedYuan = accumulatedYuan.add(thisPeriodYuan);
        BigDecimal newNetValueYuan = originalValueYuan.subtract(newAccumulatedYuan);

        // 元转分（数据库存储以分为单位）
        Long originalCostFen = originalValueYuan.multiply(YUAN_TO_FEN).longValue();
        Long thisPeriodFen = thisPeriodYuan.multiply(YUAN_TO_FEN).longValue();
        Long newAccumulatedFen = newAccumulatedYuan.multiply(YUAN_TO_FEN).longValue();
        Long newNetValueFen = newNetValueYuan.multiply(YUAN_TO_FEN).longValue();

        // 折旧期间（当前年月）
        String period = YearMonth.now().format(PERIOD_FORMATTER);

        // 写入折旧记录表
        AssetDepreciationRecord record = new AssetDepreciationRecord();
        record.setAssetId(assetId);
        record.setPeriod(period);
        record.setOriginalCost(originalCostFen);
        record.setThisPeriodDepreciation(thisPeriodFen);
        record.setAccumulatedDepreciation(newAccumulatedFen);
        record.setNetBookValueAfter(newNetValueFen);
        assetDepreciationRecordMapper.insert(record);

        // 更新资产主表的累计折旧、净值、最后折旧日期
        asset.setAccumulatedDepreciation(newAccumulatedYuan);
        asset.setNetValue(newNetValueYuan);
        asset.setLastDepreciationDate(LocalDate.now());
        updateById(asset);

        Map<String, Object> result = convertDepreciationRecordToMap(record);
        result.put("method", method);
        result.put("months", months);
        result.put("status", "calculated");
        result.put("calculateTime", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 将折旧记录实体转换为Map（保持前端返回格式兼容）
     */
    private Map<String, Object> convertDepreciationRecordToMap(AssetDepreciationRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getRecordId());
        map.put("assetId", record.getAssetId());
        map.put("period", record.getPeriod());
        map.put("originalCost", record.getOriginalCost());
        map.put("thisPeriodDepreciation", record.getThisPeriodDepreciation());
        map.put("accumulatedDepreciation", record.getAccumulatedDepreciation());
        map.put("netBookValueAfter", record.getNetBookValueAfter());
        map.put("createTime", record.getCreateTime() != null
                ? record.getCreateTime().toString() : null);
        return map;
    }

    // ========== 处置实现 ==========

    @Override
    public List<Map<String, Object>> getDisposalList(int page, int size) {
        log.info("获取处置列表, 页码: {}, 每页条数: {}", page, size);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, disposalStore.size());
        if (fromIndex >= disposalStore.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(disposalStore.subList(fromIndex, toIndex));
    }

    @Override
    public Map<String, Object> getDisposalDetail(Long disposalId) {
        log.info("获取处置详情, 处置单ID: {}", disposalId);
        for (Map<String, Object> disposal : disposalStore) {
            Object id = disposal.get("id");
            if (id instanceof Long && disposalId.equals(id)) {
                return new HashMap<>(disposal);
            }
        }
        Map<String, Object> empty = new HashMap<>();
        empty.put("id", disposalId);
        return empty;
    }

    @Override
    public Long createDisposal(Long assetId, String type, String reason, String handler) {
        log.info("创建处置申请, 资产ID: {}, 类型: {}, 原因: {}, 处理人: {}", assetId, type, reason, handler);
        long disposalId = System.currentTimeMillis();
        Map<String, Object> disposal = new HashMap<>();
        disposal.put("id", disposalId);
        disposal.put("assetId", assetId);
        disposal.put("type", type);
        disposal.put("reason", reason);
        disposal.put("handler", handler);
        disposal.put("status", "pending");
        disposal.put("createTime", LocalDateTime.now().toString());
        disposalStore.add(disposal);
        return disposalId;
    }

    @Override
    public boolean approveDisposal(Long disposalId, boolean approved, String comment) {
        log.info("审批处置申请, 处置单ID: {}, 是否通过: {}, 意见: {}", disposalId, approved, comment);
        for (Map<String, Object> disposal : disposalStore) {
            Object id = disposal.get("id");
            if (id instanceof Long && disposalId.equals(id)) {
                disposal.put("status", approved ? "approved" : "rejected");
                disposal.put("approveComment", comment);
                disposal.put("approveTime", LocalDateTime.now().toString());
                return true;
            }
        }
        log.warn("处置单不存在: {}", disposalId);
        return false;
    }
}
