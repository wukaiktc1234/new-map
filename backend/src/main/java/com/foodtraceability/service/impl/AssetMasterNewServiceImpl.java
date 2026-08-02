package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.asset.AssetMasterCreateDTO;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.AssetMasterNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产主数据服务实现类
 * 包含资产的CRUD、状态管理、折旧计算等核心功能
 */
@Service
public class AssetMasterNewServiceImpl extends ServiceImpl<AssetMasterNewMapper, AssetMasterNew>
        implements AssetMasterNewService {

    private static final Logger log = LoggerFactory.getLogger(AssetMasterNewServiceImpl.class);

    private final AssetMasterNewMapper assetMasterNewMapper;
    private final AssetFlowRecordNewMapper assetFlowRecordNewMapper;
    private final AssetDepreciationRecordMapper assetDepreciationRecordMapper;
    private final AssetCategoryNewMapper assetCategoryNewMapper;

    public AssetMasterNewServiceImpl(AssetMasterNewMapper assetMasterNewMapper,
                                    AssetFlowRecordNewMapper assetFlowRecordNewMapper,
                                    AssetDepreciationRecordMapper assetDepreciationRecordMapper,
                                    AssetCategoryNewMapper assetCategoryNewMapper) {
        this.assetMasterNewMapper = assetMasterNewMapper;
        this.assetFlowRecordNewMapper = assetFlowRecordNewMapper;
        this.assetDepreciationRecordMapper = assetDepreciationRecordMapper;
        this.assetCategoryNewMapper = assetCategoryNewMapper;
    }

    @Override
    public Page<AssetMasterNew> getPageList(Page<AssetMasterNew> page, Long categoryId,
                                           Integer status, Long storeId, String keyword) {
        LambdaQueryWrapper<AssetMasterNew> wrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null) {
            wrapper.eq(AssetMasterNew::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(AssetMasterNew::getStatus, status);
        }
        if (storeId != null) {
            wrapper.eq(AssetMasterNew::getStoreId, storeId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(AssetMasterNew::getAssetCode, keyword)
                    .or().like(AssetMasterNew::getAssetName, keyword));
        }
        
        wrapper.orderByDesc(AssetMasterNew::getCreateTime);
        return assetMasterNewMapper.selectPage(page, wrapper);
    }

    @Override
    public AssetMasterNew getByAssetCode(String assetCode) {
        return assetMasterNewMapper.selectByAssetCode(assetCode);
    }

    @Override
    public AssetMasterNew getByQrCode(String qrCode) {
        return assetMasterNewMapper.selectByQrCode(qrCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetMasterNew createAsset(AssetMasterCreateDTO createDTO) {
        // 1. 元转分（金额转换）
        long originalCostFen = createDTO.getOriginalCostYuan()
                .multiply(new BigDecimal("100"))
                .longValue();

        // 2. 创建资产实体
        AssetMasterNew asset = new AssetMasterNew();
        
        // 生成或使用传入的编码
        if (createDTO.getAssetCode() == null || createDTO.getAssetCode().isEmpty()) {
            asset.setAssetCode(generateAssetCode("ASSET"));
        } else {
            asset.setAssetCode(createDTO.getAssetCode());
        }
        
        asset.setAssetName(createDTO.getAssetName());
        asset.setCategoryId(createDTO.getCategoryId());
        asset.setSpecification(createDTO.getSpecification());
        asset.setBrand(createDTO.getBrand());
        asset.setPurchaseDate(createDTO.getPurchaseDate());
        asset.setOriginalCost(originalCostFen);
        asset.setAccumulatedDepreciation(0L);
        asset.setNetBookValue(originalCostFen); // 初始净值等于原值
        
        // 设置折旧相关字段
        if (createDTO.getUsefulLifeMonths() != null) {
            asset.setUsefulLifeMonths(createDTO.getUsefulLifeMonths());
            asset.setRemainingMonths(createDTO.getUsefulLifeMonths());
        } else {
            // 从分类获取默认值
            AssetCategoryNew category = assetCategoryNewMapper.selectById(createDTO.getCategoryId());
            if (category != null) {
                Integer years = category.getUsefulLifeYears();
                asset.setUsefulLifeMonths(years * 12);
                asset.setRemainingMonths(years * 12);
                if (createDTO.getDepreciationMethod() == null) {
                    asset.setDepreciationMethod(category.getDepreciationMethod());
                }
            }
        }
        
        if (createDTO.getDepreciationMethod() != null) {
            asset.setDepreciationMethod(createDTO.getDepreciationMethod());
        }
        
        asset.setStatus(1); // 默认状态：在用
        asset.setLocation(createDTO.getLocation());
        asset.setResponsibleUserId(createDTO.getResponsibleUserId());
        asset.setStoreId(createDTO.getStoreId());
        asset.setSupplierInfo(createDTO.getSupplierInfo());
        asset.setWarrantyExpiry(createDTO.getWarrantyExpiry());
        asset.setNextMaintenanceDate(createDTO.getNextMaintenanceDate());
        
        // 生成二维码（默认使用资产编码）
        if (createDTO.getQrCode() == null || createDTO.getQrCode().isEmpty()) {
            asset.setQrCode(asset.getAssetCode());
        } else {
            asset.setQrCode(createDTO.getQrCode());
        }
        
        asset.setImageUrl(createDTO.getImageUrl());
        asset.setRemark(createDTO.getRemark());

        // 3. 保存资产
        assetMasterNewMapper.insert(asset);

        // 4. 记录购置变动
        recordFlow(asset.getAssetId(), 1, 0, originalCostFen, originalCostFen, 
                   "PURCHASE-" + asset.getAssetCode(), LocalDate.now(), null);

        log.info("创建资产成功: 编码={}, 名称={}", asset.getAssetCode(), asset.getAssetName());
        return asset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long assetId, Integer status, String reason) {
        AssetMasterNew asset = assetMasterNewMapper.selectById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在: " + assetId);
        }

        int oldStatus = asset.getStatus();
        asset.setStatus(status);
        assetMasterNewMapper.updateById(asset);

        // 记录状态变动
        int flowType = mapStatusToFlowType(status);
        recordFlow(assetId, flowType, oldStatus, status, 0L, 
                   reason, LocalDate.now(), null);

        log.info("资产状态变更: ID={}, {} -> {}", assetId, oldStatus, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scrapAsset(Long assetId, String reason) {
        updateStatus(assetId, 4, reason); // 4-已报废
        log.info("资产已报废: ID={}, 原因={}", assetId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disposeAsset(Long assetId, String reason) {
        updateStatus(assetId, 5, reason); // 5-已处置
        log.info("资产已处置: ID={}, 原因={}", assetId, reason);
    }

    @Override
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总数
        LambdaQueryWrapper<AssetMasterNew> countWrapper = new LambdaQueryWrapper<>();
        long total = assetMasterNewMapper.selectCount(countWrapper);
        stats.put("total", total);
        
        // 按状态统计
        List<Map<String, Object>> statusStats = assetMasterNewMapper.countByStatus();
        stats.put("statusStats", statusStats);
        
        // 按分类统计
        List<Map<String, Object>> categoryStats = assetMasterNewMapper.countByCategory();
        stats.put("categoryStats", categoryStats);
        
        // 按门店统计
        List<Map<String, Object>> storeStats = assetMasterNewMapper.countByStore();
        stats.put("storeStats", storeStats);
        
        return stats;
    }

    @Override
    public String generateAssetCode(String prefix) {
        // 查询最新的编码
        LambdaQueryWrapper<AssetMasterNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(AssetMasterNew::getAssetCode, prefix)
              .orderByDesc(AssetMasterNew::getAssetCode)
              .last("LIMIT 1");
        
        AssetMasterNew lastAsset = assetMasterNewMapper.selectOne(wrapper);
        
        int nextNum = 1;
        if (lastAsset != null && lastAsset.getAssetCode() != null) {
            String code = lastAsset.getAssetCode();
            String numStr = code.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                nextNum = Integer.parseInt(numStr) + 1;
            }
        }
        
        return String.format("%s%06d", prefix, nextNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executeMonthlyDepreciation(String period) {
        // 查询需要折旧的资产
        List<AssetMasterNew> assets = assetMasterNewMapper.selectAssetsNeedDepreciation();
        
        int count = 0;
        for (AssetMasterNew asset : assets) {
            try {
                calculateAndRecordDepreciation(asset, period);
                count++;
            } catch (Exception e) {
                log.error("折旧计算失败: 资产ID={}, 错误={}", asset.getAssetId(), e.getMessage());
            }
        }
        
        log.info("月度折旧执行完成: 期间={}, 折旧资产数量={}", period, count);
        return count;
    }

    /**
     * 计算并记录单个资产的折旧
     */
    private void calculateAndRecordDepreciation(AssetMasterNew asset, String period) {
        long originalCost = asset.getOriginalCost();
        long accumulatedDepreciation = asset.getAccumulatedDepreciation() != null ? 
                asset.getAccumulatedDepreciation() : 0L;
        
        // 已完全折旧
        if (accumulatedDepreciation >= originalCost) {
            return;
        }
        
        long thisPeriodDepreciation = 0L;
        int method = asset.getDepreciationMethod() != null ? asset.getDepreciationMethod() : 1;
        int usefulLifeMonths = asset.getUsefulLifeMonths() != null ? 
                asset.getUsefulLifeMonths() : 60; // 默认5年
        
        switch (method) {
            case 1: // 直线法
                thisPeriodDepreciation = originalCost / usefulLifeMonths;
                break;
            case 2: // 年数总和法（简化版，按月递减）
                int remainingMonths = asset.getRemainingMonths() != null ? 
                        asset.getRemainingMonths() : usefulLifeMonths;
                if (remainingMonths > 0) {
                    double sumOfYear = usefulLifeMonths * (usefulLifeMonths + 1) / 2.0;
                    thisPeriodDepreciation = (long) ((originalCost * remainingMonths) / sumOfYear);
                }
                break;
            case 3: // 工作量法（此处简化为直线法，实际应按工作量计算）
                thisPeriodDepreciation = originalCost / usefulLifeMonths;
                break;
            default:
                thisPeriodDepreciation = originalCost / usefulLifeMonths;
        }
        
        // 确保不超过剩余可折旧额
        long remainingDepreciable = originalCost - accumulatedDepreciation;
        if (thisPeriodDepreciation > remainingDepreciable) {
            thisPeriodDepreciation = remainingDepreciable;
        }
        
        // 更新累计折旧和净值
        long newAccumulatedDepreciation = accumulatedDepreciation + thisPeriodDepreciation;
        long newNetBookValue = originalCost - newAccumulatedDepreciation;
        
        asset.setAccumulatedDepreciation(newAccumulatedDepreciation);
        asset.setNetBookValue(newNetBookValue);
        if (asset.getRemainingMonths() != null && asset.getRemainingMonths() > 0) {
            asset.setRemainingMonths(asset.getRemainingMonths() - 1);
        }
        assetMasterNewMapper.updateById(asset);
        
        // 记录折旧明细
        AssetDepreciationRecord record = new AssetDepreciationRecord();
        record.setAssetId(asset.getAssetId());
        record.setPeriod(period);
        record.setOriginalCost(originalCost);
        record.setThisPeriodDepreciation(thisPeriodDepreciation);
        record.setAccumulatedDepreciation(newAccumulatedDepreciation);
        record.setNetBookValueAfter(newNetBookValue);
        assetDepreciationRecordMapper.insert(record);
        
        // 记录变动
        recordFlow(asset.getAssetId(), 2, accumulatedDepreciation, 
                  newAccumulatedDepreciation, thisPeriodDepreciation,
                  "DEPRECIATION-" + period, LocalDate.now(), null);
    }

    /**
     * 记录资产变动
     */
    private void recordFlow(Long assetId, int flowType, long beforeValue, long afterValue,
                            long changeAmount, String referenceNo, LocalDate flowDate, Long operatorId) {
        AssetFlowRecordNew flow = new AssetFlowRecordNew();
        flow.setAssetId(assetId);
        flow.setFlowType(flowType);
        flow.setBeforeValue(beforeValue);
        flow.setAfterValue(afterValue);
        flow.setChangeAmount(changeAmount);
        flow.setReferenceNo(referenceNo);
        flow.setFlowDate(flowDate != null ? flowDate : LocalDate.now());
        flow.setOperatorId(operatorId);
        assetFlowRecordNewMapper.insert(flow);
    }

    /**
     * 将状态码映射为变动类型
     */
    private int mapStatusToFlowType(int status) {
        switch (status) {
            case 1: return 0; // 在用（无特定类型）
            case 2: return 7; // 闲置
            case 3: return 4; // 维修中
            case 4: return 5; // 已报废
            case 5: return 6; // 已处置
            default: return 0;
        }
    }
}
