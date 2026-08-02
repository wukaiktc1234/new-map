package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.trace.InspectionCreateDTO;
import com.foodtraceability.dto.trace.InspectionQueryDTO;
import com.foodtraceability.dto.trace.InspectionStatisticsVO;
import com.foodtraceability.dto.trace.InspectionUpdateDTO;
import com.foodtraceability.dto.trace.InspectionVO;
import com.foodtraceability.entity.FoodInspection;
import com.foodtraceability.mapper.FoodInspectionMapper;
import com.foodtraceability.service.trace.FoodInspectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 检验记录服务实现类
 * 实现检验记录的增删改查、统计等业务逻辑
 */
@Service
public class FoodInspectionServiceImpl extends ServiceImpl<FoodInspectionMapper, FoodInspection>
        implements FoodInspectionService {

    private static final Logger log = LoggerFactory.getLogger(FoodInspectionServiceImpl.class);

    /** 检验编号前缀 */
    private static final String INSPECTION_NO_PREFIX = "INS";

    /** 日期格式化器（用于编号生成） */
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FoodInspectionMapper foodInspectionMapper;

    public FoodInspectionServiceImpl(FoodInspectionMapper foodInspectionMapper) {
        this.foodInspectionMapper = foodInspectionMapper;
    }

    /**
     * 分页查询检验记录
     */
    @Override
    public IPage<InspectionVO> queryPage(InspectionQueryDTO queryDTO) {
        Page<FoodInspection> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<FoodInspection> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(FoodInspection::getCreateTime);

        IPage<FoodInspection> pageResult = page(page, wrapper);

        // 转换为VO分页结果
        Page<InspectionVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 根据ID查询检验记录详情
     */
    @Override
    public InspectionVO getDetailById(Long inspectionId) {
        FoodInspection entity = getById(inspectionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.INSPECTION_NOT_FOUND);
        }
        return convertToVO(entity);
    }

    /**
     * 创建检验记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionVO create(InspectionCreateDTO dto, Long operatorId, String operatorName) {
        FoodInspection entity = new FoodInspection();
        // 自动生成检验编号（如未传入）
        if (dto.getInspectionNo() == null || dto.getInspectionNo().isEmpty()) {
            entity.setInspectionNo(generateNo(INSPECTION_NO_PREFIX));
        } else {
            entity.setInspectionNo(dto.getInspectionNo());
        }
        entity.setTraceCodeId(dto.getTraceCodeId());
        entity.setTraceCode(dto.getTraceCode());
        entity.setBatchNo(dto.getBatchNo());
        entity.setSupplierId(dto.getSupplierId());
        entity.setSupplierName(dto.getSupplierName());
        entity.setMaterialId(dto.getMaterialId());
        entity.setMaterialName(dto.getMaterialName());
        entity.setInspectionType(dto.getInspectionType());
        entity.setInspectionResult(dto.getInspectionResult());
        entity.setInspectionItem(dto.getInspectionItem());
        entity.setInspectionValue(dto.getInspectionValue());
        entity.setStandardValue(dto.getStandardValue());
        entity.setInspectionUnit(dto.getInspectionUnit());
        // 操作人作为检验员
        entity.setInspectorId(operatorId);
        entity.setInspectorName(operatorName != null ? operatorName : dto.getInspectorName());
        entity.setInspectionTime(dto.getInspectionTime() != null ? dto.getInspectionTime() : LocalDateTime.now());
        entity.setInspectionLocation(dto.getInspectionLocation());
        entity.setReportUrl(dto.getReportUrl());
        entity.setInspectionItems(dto.getInspectionItems());
        entity.setRemark(dto.getRemark());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        save(entity);
        log.info("创建检验记录成功: inspectionNo={}, batchNo={}", entity.getInspectionNo(), entity.getBatchNo());
        return convertToVO(entity);
    }

    /**
     * 更新检验记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionVO update(InspectionUpdateDTO dto) {
        FoodInspection entity = getById(dto.getInspectionId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.INSPECTION_NOT_FOUND);
        }

        // 按非空字段更新
        if (dto.getInspectionNo() != null) {
            entity.setInspectionNo(dto.getInspectionNo());
        }
        if (dto.getTraceCodeId() != null) {
            entity.setTraceCodeId(dto.getTraceCodeId());
        }
        if (dto.getTraceCode() != null) {
            entity.setTraceCode(dto.getTraceCode());
        }
        if (dto.getBatchNo() != null) {
            entity.setBatchNo(dto.getBatchNo());
        }
        if (dto.getSupplierId() != null) {
            entity.setSupplierId(dto.getSupplierId());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getMaterialId() != null) {
            entity.setMaterialId(dto.getMaterialId());
        }
        if (dto.getMaterialName() != null) {
            entity.setMaterialName(dto.getMaterialName());
        }
        if (dto.getInspectionType() != null) {
            entity.setInspectionType(dto.getInspectionType());
        }
        if (dto.getInspectionResult() != null) {
            entity.setInspectionResult(dto.getInspectionResult());
        }
        if (dto.getInspectionItem() != null) {
            entity.setInspectionItem(dto.getInspectionItem());
        }
        if (dto.getInspectionValue() != null) {
            entity.setInspectionValue(dto.getInspectionValue());
        }
        if (dto.getStandardValue() != null) {
            entity.setStandardValue(dto.getStandardValue());
        }
        if (dto.getInspectionUnit() != null) {
            entity.setInspectionUnit(dto.getInspectionUnit());
        }
        if (dto.getInspectorId() != null) {
            entity.setInspectorId(dto.getInspectorId());
        }
        if (dto.getInspectorName() != null) {
            entity.setInspectorName(dto.getInspectorName());
        }
        if (dto.getInspectionTime() != null) {
            entity.setInspectionTime(dto.getInspectionTime());
        }
        if (dto.getInspectionLocation() != null) {
            entity.setInspectionLocation(dto.getInspectionLocation());
        }
        if (dto.getReportUrl() != null) {
            entity.setReportUrl(dto.getReportUrl());
        }
        if (dto.getInspectionItems() != null) {
            entity.setInspectionItems(dto.getInspectionItems());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
        entity.setUpdateTime(LocalDateTime.now());

        updateById(entity);
        log.info("更新检验记录成功: inspectionId={}", entity.getInspectionId());
        return convertToVO(entity);
    }

    /**
     * 删除检验记录（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long inspectionId) {
        FoodInspection entity = getById(inspectionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.INSPECTION_NOT_FOUND);
        }
        boolean result = removeById(inspectionId);
        log.info("删除检验记录: inspectionId={}, result={}", inspectionId, result);
        return result;
    }

    /**
     * 按批次号查询检验记录
     */
    @Override
    public List<InspectionVO> queryByBatchNo(String batchNo) {
        LambdaQueryWrapper<FoodInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodInspection::getBatchNo, batchNo)
                .orderByDesc(FoodInspection::getInspectionTime);
        List<FoodInspection> list = list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 按供应商查询检验记录
     */
    @Override
    public IPage<InspectionVO> queryBySupplier(Long supplierId, InspectionQueryDTO queryDTO) {
        Page<FoodInspection> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<FoodInspection> wrapper = buildQueryWrapper(queryDTO);
        wrapper.eq(FoodInspection::getSupplierId, supplierId)
                .orderByDesc(FoodInspection::getCreateTime);

        IPage<FoodInspection> pageResult = page(page, wrapper);

        Page<InspectionVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 上传检验报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadReport(Long inspectionId, String reportUrl) {
        FoodInspection entity = getById(inspectionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.INSPECTION_NOT_FOUND);
        }
        entity.setReportUrl(reportUrl);
        entity.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(entity);
        log.info("上传检验报告: inspectionId={}, reportUrl={}", inspectionId, reportUrl);
        return result;
    }

    /**
     * 检验统计
     */
    @Override
    public InspectionStatisticsVO getStatistics(InspectionQueryDTO queryDTO) {
        LambdaQueryWrapper<FoodInspection> wrapper = buildQueryWrapper(queryDTO);
        List<FoodInspection> list = list(wrapper);

        InspectionStatisticsVO vo = new InspectionStatisticsVO();
        long total = list.size();
        long qualified = list.stream().filter(e -> "QUALIFIED".equals(e.getInspectionResult())).count();
        long unqualified = list.stream().filter(e -> "UNQUALIFIED".equals(e.getInspectionResult())).count();
        long conditional = list.stream().filter(e -> "CONDITIONAL".equals(e.getInspectionResult())).count();

        vo.setTotalCount(total);
        vo.setQualifiedCount(qualified);
        vo.setUnqualifiedCount(unqualified);
        vo.setConditionalCount(conditional);

        // 合格率 = 合格次数 / 总次数 * 100，保留2位小数
        if (total > 0) {
            BigDecimal rate = BigDecimal.valueOf(qualified)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            vo.setQualificationRate(rate);
        } else {
            vo.setQualificationRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        // 按检验类型统计
        Map<String, Long> typeCountMap = list.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getInspectionType() != null ? e.getInspectionType() : "UNKNOWN",
                        Collectors.counting()));
        List<Map<String, Object>> typeStatistics = new ArrayList<>();
        typeCountMap.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("inspectionType", type);
            item.put("inspectionTypeName", getInspectionTypeName(type));
            item.put("count", count);
            typeStatistics.add(item);
        });
        vo.setTypeStatistics(typeStatistics);

        return vo;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建查询条件包装器
     */
    private LambdaQueryWrapper<FoodInspection> buildQueryWrapper(InspectionQueryDTO queryDTO) {
        LambdaQueryWrapper<FoodInspection> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getInspectionNo() != null && !queryDTO.getInspectionNo().isEmpty()) {
            wrapper.like(FoodInspection::getInspectionNo, queryDTO.getInspectionNo());
        }
        if (queryDTO.getTraceCode() != null && !queryDTO.getTraceCode().isEmpty()) {
            wrapper.eq(FoodInspection::getTraceCode, queryDTO.getTraceCode());
        }
        if (queryDTO.getBatchNo() != null && !queryDTO.getBatchNo().isEmpty()) {
            wrapper.eq(FoodInspection::getBatchNo, queryDTO.getBatchNo());
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(FoodInspection::getSupplierId, queryDTO.getSupplierId());
        }
        if (queryDTO.getSupplierName() != null && !queryDTO.getSupplierName().isEmpty()) {
            wrapper.like(FoodInspection::getSupplierName, queryDTO.getSupplierName());
        }
        if (queryDTO.getMaterialName() != null && !queryDTO.getMaterialName().isEmpty()) {
            wrapper.like(FoodInspection::getMaterialName, queryDTO.getMaterialName());
        }
        if (queryDTO.getInspectionType() != null && !queryDTO.getInspectionType().isEmpty()) {
            wrapper.eq(FoodInspection::getInspectionType, queryDTO.getInspectionType());
        }
        if (queryDTO.getInspectionResult() != null && !queryDTO.getInspectionResult().isEmpty()) {
            wrapper.eq(FoodInspection::getInspectionResult, queryDTO.getInspectionResult());
        }
        if (queryDTO.getInspectorName() != null && !queryDTO.getInspectorName().isEmpty()) {
            wrapper.like(FoodInspection::getInspectorName, queryDTO.getInspectorName());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(FoodInspection::getInspectionTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(FoodInspection::getInspectionTime, queryDTO.getEndTime());
        }
        return wrapper;
    }

    /**
     * 实体转VO
     */
    private InspectionVO convertToVO(FoodInspection entity) {
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
     * 生成业务编号
     * 格式：prefix + yyyyMMdd + 6位随机数
     * @param prefix 编号前缀
     * @return 生成的编号
     */
    private String generateNo(String prefix) {
        String dateStr = LocalDateTime.now().format(NO_DATE_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return prefix + dateStr + random;
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
