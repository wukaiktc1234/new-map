package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.trace.QualityRecordCreateDTO;
import com.foodtraceability.dto.trace.QualityRecordVO;
import com.foodtraceability.dto.trace.QualityStandardCreateDTO;
import com.foodtraceability.dto.trace.QualityStandardQueryDTO;
import com.foodtraceability.dto.trace.QualityStandardUpdateDTO;
import com.foodtraceability.dto.trace.QualityStandardVO;
import com.foodtraceability.entity.FoodQualityRecord;
import com.foodtraceability.entity.FoodQualityStandard;
import com.foodtraceability.mapper.FoodQualityRecordMapper;
import com.foodtraceability.mapper.FoodQualityStandardMapper;
import com.foodtraceability.service.trace.FoodQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 质量追溯服务实现类
 * 实现质量记录管理、质量异常处理、质量标准维护等业务逻辑
 */
@Service
public class FoodQualityServiceImpl extends ServiceImpl<FoodQualityRecordMapper, FoodQualityRecord>
        implements FoodQualityService {

    private static final Logger log = LoggerFactory.getLogger(FoodQualityServiceImpl.class);

    /** 质量记录编号前缀 */
    private static final String RECORD_NO_PREFIX = "QR";

    /** 日期格式化器（用于编号生成） */
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 异常级别-正常 */
    private static final String ABNORMAL_NORMAL = "NORMAL";
    /** 异常级别-警告 */
    private static final String ABNORMAL_WARNING = "WARNING";
    /** 异常级别-严重 */
    private static final String ABNORMAL_CRITICAL = "CRITICAL";

    /** 处理状态-待处理 */
    private static final String STATUS_PENDING = "PENDING";
    /** 处理状态-处理中 */
    private static final String STATUS_PROCESSING = "PROCESSING";
    /** 处理状态-已解决 */
    private static final String STATUS_RESOLVED = "RESOLVED";
    /** 处理状态-已关闭 */
    private static final String STATUS_CLOSED = "CLOSED";

    /** 标准状态-生效 */
    private static final String STANDARD_ACTIVE = "ACTIVE";

    private final FoodQualityRecordMapper foodQualityRecordMapper;
    private final FoodQualityStandardMapper foodQualityStandardMapper;

    public FoodQualityServiceImpl(FoodQualityRecordMapper foodQualityRecordMapper,
                                  FoodQualityStandardMapper foodQualityStandardMapper) {
        this.foodQualityRecordMapper = foodQualityRecordMapper;
        this.foodQualityStandardMapper = foodQualityStandardMapper;
    }

    // ==================== 质量记录 ====================

    /**
     * 分页查询质量记录
     */
    @Override
    public IPage<QualityRecordVO> queryRecordsPage(QualityStandardQueryDTO queryDTO) {
        Page<FoodQualityRecord> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<FoodQualityRecord> wrapper = buildRecordQueryWrapper(queryDTO);
        wrapper.orderByDesc(FoodQualityRecord::getCreateTime);

        IPage<FoodQualityRecord> pageResult = page(page, wrapper);
        return convertToRecordVOPage(pageResult);
    }

    /**
     * 查询质量记录详情
     */
    @Override
    public QualityRecordVO getRecordDetail(Long recordId) {
        FoodQualityRecord entity = getById(recordId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "质量记录不存在");
        }
        return convertToRecordVO(entity);
    }

    /**
     * 创建质量记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityRecordVO createRecord(QualityRecordCreateDTO dto, Long operatorId, String operatorName) {
        FoodQualityRecord entity = new FoodQualityRecord();
        // 自动生成记录编号（如未传入）
        if (dto.getRecordNo() == null || dto.getRecordNo().isEmpty()) {
            entity.setRecordNo(generateNo(RECORD_NO_PREFIX));
        } else {
            entity.setRecordNo(dto.getRecordNo());
        }
        entity.setTraceCodeId(dto.getTraceCodeId());
        entity.setTraceCode(dto.getTraceCode());
        entity.setBatchNo(dto.getBatchNo());
        entity.setStandardId(dto.getStandardId());
        // 冗余标准名称
        if (dto.getStandardId() != null) {
            FoodQualityStandard standard = foodQualityStandardMapper.selectById(dto.getStandardId());
            if (standard != null) {
                entity.setStandardName(standard.getStandardName());
            }
        }
        entity.setMaterialId(dto.getMaterialId());
        entity.setMaterialName(dto.getMaterialName());
        entity.setInspectionData(dto.getInspectionData());
        entity.setAbnormalLevel(dto.getAbnormalLevel());
        // 新建记录默认为待处理状态
        entity.setHandlingStatus(STATUS_PENDING);
        entity.setRemark(dto.getRemark());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        save(entity);
        log.info("创建质量记录成功: recordNo={}, material={}", entity.getRecordNo(), entity.getMaterialName());
        return convertToRecordVO(entity);
    }

    /**
     * 删除质量记录（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRecord(Long recordId) {
        FoodQualityRecord entity = getById(recordId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "质量记录不存在");
        }
        boolean result = removeById(recordId);
        log.info("删除质量记录: recordId={}, result={}", recordId, result);
        return result;
    }

    /**
     * 质量异常列表（异常级别非正常的记录）
     */
    @Override
    public IPage<QualityRecordVO> queryAbnormalRecords(QualityStandardQueryDTO queryDTO) {
        Page<FoodQualityRecord> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<FoodQualityRecord> wrapper = buildRecordQueryWrapper(queryDTO);
        // 异常记录：abnormal_level 不为 NORMAL
        wrapper.ne(FoodQualityRecord::getAbnormalLevel, ABNORMAL_NORMAL)
                .orderByDesc(FoodQualityRecord::getCreateTime);

        IPage<FoodQualityRecord> pageResult = page(page, wrapper);
        return convertToRecordVOPage(pageResult);
    }

    /**
     * 处理质量异常
     * 更新 handling_status='RESOLVED', 记录处理结果、处理人、处理时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAbnormal(Long recordId, String handlingResult, Long operatorId, String operatorName) {
        FoodQualityRecord entity = getById(recordId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "质量记录不存在");
        }

        entity.setHandlingStatus(STATUS_RESOLVED);
        entity.setHandlingResult(handlingResult);
        entity.setHandledById(operatorId);
        entity.setHandledByName(operatorName);
        entity.setHandledTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        boolean result = updateById(entity);
        log.info("处理质量异常成功: recordId={}, operator={}", recordId, operatorName);
        return result;
    }

    /**
     * 质量统计
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<FoodQualityRecord> allRecords = list();

        long total = allRecords.size();
        long normalCount = allRecords.stream().filter(e -> ABNORMAL_NORMAL.equals(e.getAbnormalLevel())).count();
        long warningCount = allRecords.stream().filter(e -> ABNORMAL_WARNING.equals(e.getAbnormalLevel())).count();
        long criticalCount = allRecords.stream().filter(e -> ABNORMAL_CRITICAL.equals(e.getAbnormalLevel())).count();
        long pendingCount = allRecords.stream().filter(e -> STATUS_PENDING.equals(e.getHandlingStatus())).count();
        long processingCount = allRecords.stream().filter(e -> STATUS_PROCESSING.equals(e.getHandlingStatus())).count();
        long resolvedCount = allRecords.stream().filter(e -> STATUS_RESOLVED.equals(e.getHandlingStatus())).count();
        long closedCount = allRecords.stream().filter(e -> STATUS_CLOSED.equals(e.getHandlingStatus())).count();

        statistics.put("totalRecords", total);
        statistics.put("normalCount", normalCount);
        statistics.put("warningCount", warningCount);
        statistics.put("criticalCount", criticalCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("processingCount", processingCount);
        statistics.put("resolvedCount", resolvedCount);
        statistics.put("closedCount", closedCount);

        // 质量标准统计
        long totalStandards = foodQualityStandardMapper.selectCount(null);
        LambdaQueryWrapper<FoodQualityStandard> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(FoodQualityStandard::getStatus, STANDARD_ACTIVE);
        long activeStandards = foodQualityStandardMapper.selectCount(activeWrapper);
        statistics.put("totalStandards", totalStandards);
        statistics.put("activeStandards", activeStandards);

        return statistics;
    }

    // ==================== 质量标准 ====================

    /**
     * 分页查询质量标准
     */
    @Override
    public IPage<QualityStandardVO> queryStandardsPage(QualityStandardQueryDTO queryDTO) {
        Page<FoodQualityStandard> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<FoodQualityStandard> wrapper = buildStandardQueryWrapper(queryDTO);
        wrapper.orderByDesc(FoodQualityStandard::getCreateTime);

        IPage<FoodQualityStandard> pageResult = foodQualityStandardMapper.selectPage(page, wrapper);

        Page<QualityStandardVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToStandardVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 查询质量标准详情
     */
    @Override
    public QualityStandardVO getStandardDetail(Long standardId) {
        FoodQualityStandard entity = foodQualityStandardMapper.selectById(standardId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.QUALITY_STANDARD_NOT_FOUND);
        }
        return convertToStandardVO(entity);
    }

    /**
     * 创建质量标准
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityStandardVO createStandard(QualityStandardCreateDTO dto) {
        FoodQualityStandard entity = new FoodQualityStandard();
        entity.setStandardNo(dto.getStandardNo());
        entity.setStandardName(dto.getStandardName());
        entity.setStandardType(dto.getStandardType());
        entity.setCategory(dto.getCategory());
        entity.setTargetMaterialId(dto.getTargetMaterialId());
        entity.setTargetMaterialName(dto.getTargetMaterialName());
        entity.setIndicatorItems(dto.getIndicatorItems());
        // 默认生效
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : STANDARD_ACTIVE);
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setIssuingAuthority(dto.getIssuingAuthority());
        entity.setVersion(dto.getVersion());
        entity.setRemark(dto.getRemark());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        foodQualityStandardMapper.insert(entity);
        log.info("创建质量标准成功: standardNo={}, name={}", entity.getStandardNo(), entity.getStandardName());
        return convertToStandardVO(entity);
    }

    /**
     * 更新质量标准
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityStandardVO updateStandard(QualityStandardUpdateDTO dto) {
        FoodQualityStandard entity = foodQualityStandardMapper.selectById(dto.getStandardId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.QUALITY_STANDARD_NOT_FOUND);
        }

        if (dto.getStandardNo() != null) {
            entity.setStandardNo(dto.getStandardNo());
        }
        if (dto.getStandardName() != null) {
            entity.setStandardName(dto.getStandardName());
        }
        if (dto.getStandardType() != null) {
            entity.setStandardType(dto.getStandardType());
        }
        if (dto.getCategory() != null) {
            entity.setCategory(dto.getCategory());
        }
        if (dto.getTargetMaterialId() != null) {
            entity.setTargetMaterialId(dto.getTargetMaterialId());
        }
        if (dto.getTargetMaterialName() != null) {
            entity.setTargetMaterialName(dto.getTargetMaterialName());
        }
        if (dto.getIndicatorItems() != null) {
            entity.setIndicatorItems(dto.getIndicatorItems());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getEffectiveDate() != null) {
            entity.setEffectiveDate(dto.getEffectiveDate());
        }
        if (dto.getExpiryDate() != null) {
            entity.setExpiryDate(dto.getExpiryDate());
        }
        if (dto.getIssuingAuthority() != null) {
            entity.setIssuingAuthority(dto.getIssuingAuthority());
        }
        if (dto.getVersion() != null) {
            entity.setVersion(dto.getVersion());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
        entity.setUpdateTime(LocalDateTime.now());

        foodQualityStandardMapper.updateById(entity);
        log.info("更新质量标准成功: standardId={}", entity.getStandardId());
        return convertToStandardVO(entity);
    }

    /**
     * 删除质量标准（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStandard(Long standardId) {
        FoodQualityStandard entity = foodQualityStandardMapper.selectById(standardId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.QUALITY_STANDARD_NOT_FOUND);
        }
        int result = foodQualityStandardMapper.deleteById(standardId);
        log.info("删除质量标准: standardId={}, result={}", standardId, result > 0);
        return result > 0;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建质量记录查询条件包装器
     * 注意：QualityStandardQueryDTO 复用于质量记录查询，
     * targetMaterialName 映射为质量记录的 materialName，
     * status 映射为质量记录的 handlingStatus
     */
    private LambdaQueryWrapper<FoodQualityRecord> buildRecordQueryWrapper(QualityStandardQueryDTO queryDTO) {
        LambdaQueryWrapper<FoodQualityRecord> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getTargetMaterialName() != null && !queryDTO.getTargetMaterialName().isEmpty()) {
            wrapper.like(FoodQualityRecord::getMaterialName, queryDTO.getTargetMaterialName());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(FoodQualityRecord::getHandlingStatus, queryDTO.getStatus());
        }
        return wrapper;
    }

    /**
     * 构建质量标准查询条件包装器
     */
    private LambdaQueryWrapper<FoodQualityStandard> buildStandardQueryWrapper(QualityStandardQueryDTO queryDTO) {
        LambdaQueryWrapper<FoodQualityStandard> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStandardName() != null && !queryDTO.getStandardName().isEmpty()) {
            wrapper.like(FoodQualityStandard::getStandardName, queryDTO.getStandardName());
        }
        if (queryDTO.getStandardType() != null && !queryDTO.getStandardType().isEmpty()) {
            wrapper.eq(FoodQualityStandard::getStandardType, queryDTO.getStandardType());
        }
        if (queryDTO.getCategory() != null && !queryDTO.getCategory().isEmpty()) {
            wrapper.eq(FoodQualityStandard::getCategory, queryDTO.getCategory());
        }
        if (queryDTO.getTargetMaterialName() != null && !queryDTO.getTargetMaterialName().isEmpty()) {
            wrapper.like(FoodQualityStandard::getTargetMaterialName, queryDTO.getTargetMaterialName());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(FoodQualityStandard::getStatus, queryDTO.getStatus());
        }
        return wrapper;
    }

    /**
     * 实体分页转质量记录VO分页
     */
    private IPage<QualityRecordVO> convertToRecordVOPage(IPage<FoodQualityRecord> pageResult) {
        Page<QualityRecordVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToRecordVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 质量记录实体转VO
     */
    private QualityRecordVO convertToRecordVO(FoodQualityRecord entity) {
        QualityRecordVO vo = new QualityRecordVO();
        vo.setQualityRecordId(entity.getQualityRecordId());
        vo.setRecordNo(entity.getRecordNo());
        vo.setTraceCodeId(entity.getTraceCodeId());
        vo.setTraceCode(entity.getTraceCode());
        vo.setBatchNo(entity.getBatchNo());
        vo.setStandardId(entity.getStandardId());
        vo.setStandardName(entity.getStandardName());
        vo.setMaterialId(entity.getMaterialId());
        vo.setMaterialName(entity.getMaterialName());
        vo.setInspectionData(entity.getInspectionData());
        vo.setAbnormalLevel(entity.getAbnormalLevel());
        vo.setAbnormalLevelName(getAbnormalLevelName(entity.getAbnormalLevel()));
        vo.setHandlingStatus(entity.getHandlingStatus());
        vo.setHandlingStatusName(getHandlingStatusName(entity.getHandlingStatus()));
        vo.setHandlingResult(entity.getHandlingResult());
        vo.setHandledById(entity.getHandledById());
        vo.setHandledByName(entity.getHandledByName());
        vo.setHandledTime(entity.getHandledTime());
        vo.setRootCause(entity.getRootCause());
        vo.setAffectedTraceCodes(entity.getAffectedTraceCodes());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 质量标准实体转VO
     */
    private QualityStandardVO convertToStandardVO(FoodQualityStandard entity) {
        QualityStandardVO vo = new QualityStandardVO();
        vo.setStandardId(entity.getStandardId());
        vo.setStandardNo(entity.getStandardNo());
        vo.setStandardName(entity.getStandardName());
        vo.setStandardType(entity.getStandardType());
        vo.setStandardTypeName(getStandardTypeName(entity.getStandardType()));
        vo.setCategory(entity.getCategory());
        vo.setTargetMaterialId(entity.getTargetMaterialId());
        vo.setTargetMaterialName(entity.getTargetMaterialName());
        vo.setIndicatorItems(entity.getIndicatorItems());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStandardStatusName(entity.getStatus()));
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpiryDate(entity.getExpiryDate());
        vo.setIssuingAuthority(entity.getIssuingAuthority());
        vo.setVersion(entity.getVersion());
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
     * 异常级别中文名映射
     * NORMAL→正常, WARNING→警告, CRITICAL→严重
     */
    private static String getAbnormalLevelName(String level) {
        if (level == null) {
            return "未知";
        }
        switch (level) {
            case ABNORMAL_NORMAL:
                return "正常";
            case ABNORMAL_WARNING:
                return "警告";
            case ABNORMAL_CRITICAL:
                return "严重";
            default:
                return "未知";
        }
    }

    /**
     * 处理状态中文名映射
     * PENDING→待处理, PROCESSING→处理中, RESOLVED→已解决, CLOSED→已关闭
     */
    private static String getHandlingStatusName(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待处理";
            case STATUS_PROCESSING:
                return "处理中";
            case STATUS_RESOLVED:
                return "已解决";
            case STATUS_CLOSED:
                return "已关闭";
            default:
                return "未知";
        }
    }

    /**
     * 标准类型中文名映射
     * NATIONAL→国标, INDUSTRY→行标, ENTERPRISE→企标, LOCAL→地方标准
     */
    private static String getStandardTypeName(String type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case "NATIONAL":
                return "国标";
            case "INDUSTRY":
                return "行标";
            case "ENTERPRISE":
                return "企标";
            case "LOCAL":
                return "地方标准";
            default:
                return "未知";
        }
    }

    /**
     * 标准状态中文名映射
     * ACTIVE→生效, INACTIVE→失效
     */
    private static String getStandardStatusName(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STANDARD_ACTIVE:
                return "生效";
            case "INACTIVE":
                return "失效";
            default:
                return "未知";
        }
    }
}
