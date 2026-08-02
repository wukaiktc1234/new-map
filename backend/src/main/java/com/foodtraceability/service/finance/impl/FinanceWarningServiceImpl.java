package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.FinanceWarningCreateDTO;
import com.foodtraceability.dto.finance.FinanceWarningProcessDTO;
import com.foodtraceability.dto.finance.FinanceWarningQueryDTO;
import com.foodtraceability.dto.finance.FinanceWarningStatsVO;
import com.foodtraceability.dto.finance.FinanceWarningVO;
import com.foodtraceability.entity.finance.FinanceWarning;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.FinanceWarningMapper;
import com.foodtraceability.service.finance.FinanceWarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 财务预警Service实现
 *
 * <p>Sprint F-020：财务风险预警管理，支持预警创建、处理流转与统计分析。</p>
 *
 * <p>状态机：UNHANDLED(未处理) → HANDLING(处理中) → RESOLVED(已解决)。
 * 允许 UNHANDLED 直接跳转到 RESOLVED。</p>
 */
@Service
public class FinanceWarningServiceImpl extends ServiceImpl<FinanceWarningMapper, FinanceWarning>
        implements FinanceWarningService {

    private static final Logger log = LoggerFactory.getLogger(FinanceWarningServiceImpl.class);

    /** 状态：未处理 */
    private static final String STATUS_UNHANDLED = "UNHANDLED";
    /** 状态：处理中 */
    private static final String STATUS_HANDLING = "HANDLING";
    /** 状态：已解决 */
    private static final String STATUS_RESOLVED = "RESOLVED";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FinanceWarningServiceImpl(FinanceWarningMapper financeWarningMapper) {
        // 构造函数注入 Mapper（通过 ServiceImpl 的 baseMapper 提供）
    }

    @Override
    public IPage<FinanceWarningVO> queryPage(FinanceWarningQueryDTO query) {
        int current = query.getCurrent() != null ? query.getCurrent() : 1;
        int size = query.getSize() != null ? query.getSize() : 10;
        Page<FinanceWarning> page = new Page<>(current, size);

        LambdaQueryWrapper<FinanceWarning> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(FinanceWarning::getWarningDate);

        IPage<FinanceWarning> entityPage = baseMapper.selectPage(page, wrapper);

        Page<FinanceWarningVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<FinanceWarningVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<FinanceWarningVO> result = (IPage<FinanceWarningVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public FinanceWarningVO getDetail(Long id) {
        FinanceWarning entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("预警记录不存在");
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceWarningVO create(FinanceWarningCreateDTO dto) {
        log.info("创建财务预警，类型：{}，级别：{}", dto.getWarningType(), dto.getWarningLevel());

        FinanceWarning entity = new FinanceWarning();
        entity.setWarningType(dto.getWarningType());
        entity.setWarningLevel(dto.getWarningLevel());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setBusinessId(dto.getBusinessId());
        entity.setBusinessType(dto.getBusinessType());
        entity.setWarningDate(LocalDateTime.now());
        entity.setStatus(STATUS_UNHANDLED);

        baseMapper.insert(entity);
        log.info("财务预警创建成功，ID：{}", entity.getId());
        return getDetail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean process(Long id, FinanceWarningProcessDTO dto) {
        log.info("处理财务预警，ID：{}，目标状态：{}", id, dto.getStatus());

        FinanceWarning entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("预警记录不存在");
        }

        String targetStatus = dto.getStatus();
        String currentStatus = entity.getStatus();

        // 校验状态流转合法性
        validateStatusTransition(currentStatus, targetStatus);

        entity.setStatus(targetStatus);
        entity.setHandleTime(LocalDateTime.now());
        // 处理人：当前未接入用户上下文，使用 system 占位
        entity.setHandler("system");
        // 合并处理措施与处理结果
        entity.setHandleResult(buildHandleResult(dto.getHandleMeasures(), dto.getHandleResult()));

        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        log.info("删除财务预警，ID：{}", id);
        FinanceWarning entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("预警记录不存在");
        }
        // 逻辑删除（@TableLogic 自动处理）
        return removeById(id);
    }

    @Override
    public FinanceWarningStatsVO getStats(String startDate, String endDate) {
        FinanceWarningStatsVO stats = new FinanceWarningStatsVO();

        // 总数（带日期范围过滤）
        LambdaQueryWrapper<FinanceWarning> totalWrapper = new LambdaQueryWrapper<>();
        applyDateRange(totalWrapper, startDate, endDate);
        stats.setTotal(baseMapper.selectCount(totalWrapper));

        // 待处理数
        stats.setPending(countByStatus(STATUS_UNHANDLED, startDate, endDate));
        // 处理中数
        stats.setHandling(countByStatus(STATUS_HANDLING, startDate, endDate));
        // 已解决数
        stats.setResolved(countByStatus(STATUS_RESOLVED, startDate, endDate));

        return stats;
    }

    /**
     * 按状态统计数量（带日期范围过滤）
     */
    private Long countByStatus(String status, String startDate, String endDate) {
        LambdaQueryWrapper<FinanceWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceWarning::getStatus, status);
        applyDateRange(wrapper, startDate, endDate);
        return baseMapper.selectCount(wrapper);
    }

    /**
     * 构建分页/列表查询条件
     */
    private LambdaQueryWrapper<FinanceWarning> buildQueryWrapper(FinanceWarningQueryDTO query) {
        LambdaQueryWrapper<FinanceWarning> wrapper = new LambdaQueryWrapper<>();
        if (query.getWarningType() != null && !query.getWarningType().isBlank()) {
            wrapper.eq(FinanceWarning::getWarningType, query.getWarningType());
        }
        if (query.getWarningLevel() != null && !query.getWarningLevel().isBlank()) {
            wrapper.eq(FinanceWarning::getWarningLevel, query.getWarningLevel());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(FinanceWarning::getStatus, query.getStatus());
        }
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate());
        return wrapper;
    }

    /**
     * 应用日期范围过滤到 warningDate 字段
     *
     * @param wrapper   查询构造器
     * @param startDate 起始日期（yyyy-MM-dd），可为空
     * @param endDate   结束日期（yyyy-MM-dd），可为空
     */
    private void applyDateRange(LambdaQueryWrapper<FinanceWarning> wrapper, String startDate, String endDate) {
        if (startDate != null && !startDate.isBlank()) {
            LocalDateTime start = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
            wrapper.ge(FinanceWarning::getWarningDate, start);
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDateTime end = LocalDate.parse(endDate, DATE_FORMATTER).atTime(23, 59, 59);
            wrapper.le(FinanceWarning::getWarningDate, end);
        }
    }

    /**
     * 校验状态流转合法性
     *
     * <p>允许的流转：
     * <ul>
     *   <li>UNHANDLED → HANDLING</li>
     *   <li>UNHANDLED → RESOLVED</li>
     *   <li>HANDLING → RESOLVED</li>
     * </ul>
     * </p>
     */
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        if (!STATUS_HANDLING.equals(targetStatus) && !STATUS_RESOLVED.equals(targetStatus)) {
            throw new BusinessException("目标状态非法，仅支持 HANDLING 或 RESOLVED");
        }
        if (STATUS_RESOLVED.equals(currentStatus)) {
            throw new BusinessException("已解决的预警不允许再次处理");
        }
        if (STATUS_HANDLING.equals(currentStatus) && STATUS_HANDLING.equals(targetStatus)) {
            throw new BusinessException("预警已在处理中，无需重复操作");
        }
        if (STATUS_HANDLING.equals(currentStatus) && STATUS_RESOLVED.equals(targetStatus)) {
            return;
        }
        if (STATUS_UNHANDLED.equals(currentStatus)) {
            return;
        }
        throw new BusinessException("当前状态不允许流转到目标状态");
    }

    /**
     * 合并处理措施与处理结果
     */
    private String buildHandleResult(String measures, String result) {
        boolean hasMeasures = measures != null && !measures.isBlank();
        boolean hasResult = result != null && !result.isBlank();
        if (hasMeasures && hasResult) {
            return "处理措施：" + measures + "；处理结果：" + result;
        }
        if (hasMeasures) {
            return measures;
        }
        if (hasResult) {
            return result;
        }
        return null;
    }

    /**
     * 实体转VO
     */
    private FinanceWarningVO convertToVO(FinanceWarning entity) {
        FinanceWarningVO vo = new FinanceWarningVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
