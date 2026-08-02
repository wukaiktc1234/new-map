package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.trace.InspectionVO;
import com.foodtraceability.dto.trace.SupplierBatchVO;
import com.foodtraceability.dto.trace.SupplierTraceVO;
import com.foodtraceability.entity.FoodInspection;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.TraceCode;
import com.foodtraceability.mapper.FoodInspectionMapper;
import com.foodtraceability.mapper.MaterialTraceCodeMapper;
import com.foodtraceability.mapper.TraceCodeMapper;
import com.foodtraceability.service.trace.SupplierTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商追溯服务实现类
 * 聚合查询供应商维度的批次、合格率、召回、检验等追溯信息
 */
@Service
public class SupplierTraceServiceImpl implements SupplierTraceService {

    private static final Logger log = LoggerFactory.getLogger(SupplierTraceServiceImpl.class);

    /** 追溯码状态-已召回 */
    private static final int TRACE_STATUS_RECALLED = 4;

    private final MaterialTraceCodeMapper materialTraceCodeMapper;
    private final FoodInspectionMapper foodInspectionMapper;
    private final TraceCodeMapper traceCodeMapper;

    public SupplierTraceServiceImpl(MaterialTraceCodeMapper materialTraceCodeMapper,
                                    FoodInspectionMapper foodInspectionMapper,
                                    TraceCodeMapper traceCodeMapper) {
        this.materialTraceCodeMapper = materialTraceCodeMapper;
        this.foodInspectionMapper = foodInspectionMapper;
        this.traceCodeMapper = traceCodeMapper;
    }

    /**
     * 供应商追溯汇总（含批次、合格率、召回、检验）
     */
    @Override
    public SupplierTraceVO getSupplierTrace(Long supplierId) {
        SupplierTraceVO vo = new SupplierTraceVO();
        vo.setSupplierId(supplierId);

        // 批次列表
        List<SupplierBatchVO> batches = getSupplierBatches(supplierId);
        vo.setBatches(batches);
        vo.setTotalBatches(batches.size());

        // 供应商名称（从批次中取第一个，若存在）
        if (!batches.isEmpty()) {
            // 批次中不直接包含供应商名称，从原料追溯码表获取
            MaterialTraceCode firstMaterial = getFirstMaterialBySupplier(supplierId);
            if (firstMaterial != null) {
                vo.setSupplierName(firstMaterial.getSupplierName());
            }
        }

        // 检验记录
        List<InspectionVO> inspections = getInspections(supplierId);
        vo.setInspections(inspections);
        vo.setTotalInspections(inspections.size());

        // 合格率
        Map<String, Object> qualityRate = getQualityRate(supplierId);
        Object rateObj = qualityRate.get("qualificationRate");
        if (rateObj instanceof BigDecimal) {
            vo.setQualificationRate((BigDecimal) rateObj);
        } else {
            vo.setQualificationRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        // 召回记录（简化处理：返回数量统计）
        List<Map<String, Object>> recalls = getRecalls(supplierId);
        vo.setTotalRecalls(recalls.size());

        return vo;
    }

    /**
     * 供应商所有原料批次
     * 从 material_trace_code 表查询该供应商的所有批次
     */
    @Override
    public List<SupplierBatchVO> getSupplierBatches(Long supplierId) {
        // MaterialTraceCode 的 supplierId 是 String 类型，需要转换
        String supplierIdStr = String.valueOf(supplierId);
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTraceCode::getSupplierId, supplierIdStr)
                .orderByDesc(MaterialTraceCode::getCreateTime);

        List<MaterialTraceCode> materialList = materialTraceCodeMapper.selectList(wrapper);

        return materialList.stream().map(this::convertToBatchVO).collect(Collectors.toList());
    }

    /**
     * 供应商合格率
     * 从 food_inspection 表统计合格/不合格次数
     * 合格率 = qualifiedCount / totalCount * 100（保留2位小数）
     */
    @Override
    public Map<String, Object> getQualityRate(Long supplierId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<FoodInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodInspection::getSupplierId, supplierId);
        List<FoodInspection> inspectionList = foodInspectionMapper.selectList(wrapper);

        long total = inspectionList.size();
        long qualified = inspectionList.stream()
                .filter(e -> "QUALIFIED".equals(e.getInspectionResult())).count();
        long unqualified = inspectionList.stream()
                .filter(e -> "UNQUALIFIED".equals(e.getInspectionResult())).count();
        long conditional = inspectionList.stream()
                .filter(e -> "CONDITIONAL".equals(e.getInspectionResult())).count();

        result.put("totalCount", total);
        result.put("qualifiedCount", qualified);
        result.put("unqualifiedCount", unqualified);
        result.put("conditionalCount", conditional);

        // 合格率 = 合格次数 / 总次数 * 100，保留2位小数
        BigDecimal rate;
        if (total > 0) {
            rate = BigDecimal.valueOf(qualified)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        } else {
            rate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        result.put("qualificationRate", rate);

        return result;
    }

    /**
     * 供应商召回记录
     * 从 food_trace_codes 表查询 status=4(已召回) 的记录
     */
    @Override
    public List<Map<String, Object>> getRecalls(Long supplierId) {
        LambdaQueryWrapper<TraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceCode::getSupplierId, supplierId)
                .eq(TraceCode::getStatus, TRACE_STATUS_RECALLED)
                .orderByDesc(TraceCode::getUpdateTime);

        List<TraceCode> traceCodeList = traceCodeMapper.selectList(wrapper);

        List<Map<String, Object>> recallList = new ArrayList<>();
        for (TraceCode traceCode : traceCodeList) {
            Map<String, Object> item = new HashMap<>();
            item.put("traceCodeId", traceCode.getTraceCodeId());
            item.put("traceCode", traceCode.getTraceCode());
            item.put("targetName", traceCode.getTargetName());
            item.put("batchNo", traceCode.getBatchNo());
            item.put("status", traceCode.getStatus());
            item.put("statusName", "已召回");
            item.put("recallTime", traceCode.getUpdateTime());
            item.put("currentLocation", traceCode.getCurrentLocation());
            recallList.add(item);
        }
        return recallList;
    }

    /**
     * 供应商检验记录
     * 从 food_inspection 表查询该供应商的检验记录
     */
    @Override
    public List<InspectionVO> getInspections(Long supplierId) {
        LambdaQueryWrapper<FoodInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodInspection::getSupplierId, supplierId)
                .orderByDesc(FoodInspection::getInspectionTime);

        List<FoodInspection> inspectionList = foodInspectionMapper.selectList(wrapper);

        return inspectionList.stream().map(this::convertToInspectionVO).collect(Collectors.toList());
    }

    /**
     * 供应商追溯统计
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 统计供应商总数（去重，从原料追溯码表统计）
        LambdaQueryWrapper<MaterialTraceCode> supplierWrapper = new LambdaQueryWrapper<>();
        supplierWrapper.isNotNull(MaterialTraceCode::getSupplierId)
                .ne(MaterialTraceCode::getSupplierId, "");
        List<MaterialTraceCode> allMaterials = materialTraceCodeMapper.selectList(supplierWrapper);
        long supplierCount = allMaterials.stream()
                .map(MaterialTraceCode::getSupplierId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .count();
        statistics.put("totalSuppliers", supplierCount);

        // 总批次数
        statistics.put("totalBatches", allMaterials.size());

        // 总检验次数
        long totalInspections = foodInspectionMapper.selectCount(null);
        statistics.put("totalInspections", totalInspections);

        // 总召回次数
        LambdaQueryWrapper<TraceCode> recallWrapper = new LambdaQueryWrapper<>();
        recallWrapper.eq(TraceCode::getStatus, TRACE_STATUS_RECALLED);
        long totalRecalls = traceCodeMapper.selectCount(recallWrapper);
        statistics.put("totalRecalls", totalRecalls);

        // 整体合格率
        List<FoodInspection> allInspections = foodInspectionMapper.selectList(null);
        long totalQualified = allInspections.stream()
                .filter(e -> "QUALIFIED".equals(e.getInspectionResult())).count();
        BigDecimal overallRate;
        if (!allInspections.isEmpty()) {
            overallRate = BigDecimal.valueOf(totalQualified)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(allInspections.size()), 2, RoundingMode.HALF_UP);
        } else {
            overallRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        statistics.put("overallQualificationRate", overallRate);

        return statistics;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查询供应商的第一条原料追溯码记录（用于获取供应商名称）
     */
    private MaterialTraceCode getFirstMaterialBySupplier(Long supplierId) {
        String supplierIdStr = String.valueOf(supplierId);
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTraceCode::getSupplierId, supplierIdStr)
                .orderByDesc(MaterialTraceCode::getCreateTime)
                .last("LIMIT 1");
        return materialTraceCodeMapper.selectOne(wrapper);
    }

    /**
     * 原料追溯码实体转批次VO
     */
    private SupplierBatchVO convertToBatchVO(MaterialTraceCode entity) {
        SupplierBatchVO vo = new SupplierBatchVO();
        // traceCodeId 在 MaterialTraceCode 中是 String 类型
        if (entity.getTraceCodeId() != null) {
            try {
                vo.setTraceCodeId(Long.parseLong(entity.getTraceCodeId()));
            } catch (NumberFormatException e) {
                vo.setTraceCodeId(null);
            }
        }
        vo.setTraceCode(entity.getTraceCode());
        vo.setMaterialId(entity.getMaterialId());
        vo.setMaterialName(entity.getMaterialName());
        vo.setBatchNo(entity.getBatchNumber());
        vo.setInboundTime(entity.getGenerateTime());
        vo.setExpiryDate(entity.getExpiryDate());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getMaterialStatusName(entity.getStatus()));
        return vo;
    }

    /**
     * 检验记录实体转VO
     */
    private InspectionVO convertToInspectionVO(FoodInspection entity) {
        InspectionVO vo = new InspectionVO();
        vo.setInspectionId(entity.getInspectionId());
        vo.setInspectionNo(entity.getInspectionNo());
        vo.setTraceCodeId(entity.getTraceCodeId());
        vo.setTraceCode(entity.getTraceCode());
        vo.setBatchNo(entity.getBatchNo());
        vo.setSupplierId(entity.getSupplierId());
        vo.setSupplierName(entity.getSupplierName());
        vo.setMaterialId(entity.getMaterialId());
        vo.setMaterialName(entity.getMaterialName());
        vo.setInspectionType(entity.getInspectionType());
        vo.setInspectionTypeName(getInspectionTypeName(entity.getInspectionType()));
        vo.setInspectionResult(entity.getInspectionResult());
        vo.setInspectionResultName(getInspectionResultName(entity.getInspectionResult()));
        vo.setInspectionItem(entity.getInspectionItem());
        vo.setInspectionValue(entity.getInspectionValue());
        vo.setStandardValue(entity.getStandardValue());
        vo.setInspectionUnit(entity.getInspectionUnit());
        vo.setInspectorId(entity.getInspectorId());
        vo.setInspectorName(entity.getInspectorName());
        vo.setInspectionTime(entity.getInspectionTime());
        vo.setInspectionLocation(entity.getInspectionLocation());
        vo.setReportUrl(entity.getReportUrl());
        vo.setInspectionItems(entity.getInspectionItems());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 原料追溯码状态中文名映射
     */
    private static String getMaterialStatusName(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case "active":
            case "ACTIVE":
                return "在库";
            case "used":
            case "USED":
                return "已使用";
            case "locked":
            case "LOCKED":
                return "已锁定";
            case "expired":
            case "EXPIRED":
                return "已过期";
            case "scrapped":
            case "SCRAPPED":
                return "已报损";
            case "returned":
            case "RETURNED":
                return "已退货";
            default:
                return "未知";
        }
    }

    /**
     * 检验类型中文名映射
     * INCOMING→入库检验, PROCESS→加工检验, FINAL→成品检验
     */
    private static String getInspectionTypeName(String type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case "INCOMING":
                return "入库检验";
            case "PROCESS":
                return "加工检验";
            case "FINAL":
                return "成品检验";
            default:
                return "未知";
        }
    }

    /**
     * 检验结果中文名映射
     * QUALIFIED→合格, UNQUALIFIED→不合格, CONDITIONAL→有条件合格
     */
    private static String getInspectionResultName(String result) {
        if (result == null) {
            return "未知";
        }
        switch (result) {
            case "QUALIFIED":
                return "合格";
            case "UNQUALIFIED":
                return "不合格";
            case "CONDITIONAL":
                return "有条件合格";
            default:
                return "未知";
        }
    }
}
