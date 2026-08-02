package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.CostRecord;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.CostRecordMapper;
import com.foodtraceability.service.finance.CostRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成本记录Service实现类
 *
 * <p>Sprint 3.1 P0 T-041：新建实现类，实现 {@link CostRecordService} 接口。
 * 提供 CRUD + 期间汇总 + 报销成本归集（F-011 联动）能力。</p>
 *
 * <p>报销成本归集 {@link #recordReimbursementCost} 由
 * {@code InvoiceReimbursementApprovedEventListener} 在主事务提交后异步调用，
 * 将报销金额按部门归集到成本记录表（costType=7 其他成本）。</p>
 */
@Service
public class CostRecordServiceImpl extends ServiceImpl<CostRecordMapper, CostRecord>
        implements CostRecordService {

    private static final Logger log = LoggerFactory.getLogger(CostRecordServiceImpl.class);

    /** 成本类型：其他成本（报销归集用） */
    private static final int COST_TYPE_OTHER = 7;

    /** 计算方式：实际发生 */
    private static final int CALCULATION_METHOD_ACTUAL = 1;

    /** 日期格式：yyyy-MM（成本归属期间） */
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 日期格式：yyyyMMdd（成本编号序号部分） */
    private static final DateTimeFormatter COST_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CostRecordVO create(CostRecordCreateDTO dto) {
        log.info("创建成本记录，成本类型：{}，金额：{}", dto.getCostType(), dto.getAmount());

        CostRecord entity = new CostRecord();
        entity.setCostNo(generateCostNo());
        entity.setCostType(dto.getCostType());
        entity.setPeriod(dto.getPeriod());
        entity.setCostCenterId(dto.getCostCenterId());
        entity.setAmount(dto.getAmount());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setCalculationMethod(dto.getCalculationMethod() != null ? dto.getCalculationMethod() : CALCULATION_METHOD_ACTUAL);
        entity.setRelatedVoucherId(dto.getRelatedVoucherId());
        entity.setRemark(dto.getRemark());

        this.save(entity);

        return getDetail(entity.getCostId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(CostRecordUpdateDTO dto) {
        log.info("更新成本记录，ID：{}", dto.getCostId());

        CostRecord existing = this.getById(dto.getCostId());
        if (existing == null) {
            throw new BusinessException("成本记录不存在");
        }

        if (dto.getCostType() != null) {
            existing.setCostType(dto.getCostType());
        }
        if (dto.getPeriod() != null) {
            existing.setPeriod(dto.getPeriod());
        }
        if (dto.getCostCenterId() != null) {
            existing.setCostCenterId(dto.getCostCenterId());
        }
        if (dto.getAmount() != null) {
            existing.setAmount(dto.getAmount());
        }
        if (dto.getCalculationMethod() != null) {
            existing.setCalculationMethod(dto.getCalculationMethod());
        }
        if (dto.getRemark() != null) {
            existing.setRemark(dto.getRemark());
        }

        return this.updateById(existing);
    }

    @Override
    public CostRecordVO getDetail(Long costId) {
        CostRecord entity = this.getById(costId);
        if (entity == null) {
            throw new BusinessException("成本记录不存在");
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<CostRecordVO> getPage(CostRecordQueryDTO query) {
        Page<CostRecord> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();

        if (query.getCostType() != null) {
            wrapper.eq(CostRecord::getCostType, query.getCostType());
        }
        if (query.getPeriod() != null && !query.getPeriod().isBlank()) {
            wrapper.eq(CostRecord::getPeriod, query.getPeriod());
        }
        if (query.getStartPeriod() != null && !query.getStartPeriod().isBlank()) {
            wrapper.ge(CostRecord::getPeriod, query.getStartPeriod());
        }
        if (query.getEndPeriod() != null && !query.getEndPeriod().isBlank()) {
            wrapper.le(CostRecord::getPeriod, query.getEndPeriod());
        }
        if (query.getCostCenterId() != null) {
            wrapper.eq(CostRecord::getCostCenterId, query.getCostCenterId());
        }
        if (query.getCalculationMethod() != null) {
            wrapper.eq(CostRecord::getCalculationMethod, query.getCalculationMethod());
        }
        wrapper.orderByDesc(CostRecord::getCostId);

        IPage<CostRecord> entityPage = baseMapper.selectPage(page, wrapper);

        Page<CostRecordVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CostRecordVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<CostRecordVO> result = (IPage<CostRecordVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public Map<String, Object> summarizeByPeriod(String period) {
        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostRecord::getPeriod, period);
        List<CostRecord> records = this.list(wrapper);

        Map<Integer, Long> byType = new HashMap<>();
        long totalAmount = 0L;
        for (CostRecord record : records) {
            Long amt = record.getAmount() != null ? record.getAmount() : 0L;
            byType.merge(record.getCostType(), amt, Long::sum);
            totalAmount += amt;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("period", period);
        summary.put("totalAmount", totalAmount);
        summary.put("byType", byType);
        summary.put("count", records.size());
        return summary;
    }

    /**
     * 记录报销成本（F-011 联动）
     *
     * <p>由 {@code InvoiceReimbursementApprovedEventListener} 在报销审批通过、
     * 主事务提交后异步调用。将报销金额按部门归集到成本记录表：</p>
     * <ul>
     *   <li>costType = 7（其他成本）</li>
     *   <li>costCenterId = departmentId（部门作为成本中心）</li>
     *   <li>period = occurDate 格式化 yyyy-MM</li>
     *   <li>costNo = CR + yyyyMMdd + 4位序号</li>
     *   <li>relatedVoucherId = reimbursementId（用于幂等判重，与监听器文档承诺一致）</li>
     *   <li>remark = "报销单号:" + reimbursementId</li>
     * </ul>
     *
     * <p>幂等性：方法入口通过 {@code relatedVoucherId = reimbursementId AND costType = 7}
     * 查询判重，若已存在则跳过插入（应对事件重发/@Async 重试/人工补偿场景）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordReimbursementCost(Long reimbursementId,
                                        Long departmentId,
                                        String reimbursementType,
                                        Long amount,
                                        LocalDate occurDate) {
        log.info("记录报销成本，报销单ID：{}，部门ID：{}，报销类型：{}，金额：{}，发生日期：{}",
                reimbursementId, departmentId, reimbursementType, amount, occurDate);

        if (amount == null || amount <= 0L) {
            log.warn("报销金额无效，跳过成本归集：reimbursementId={}, amount={}", reimbursementId, amount);
            return;
        }
        if (occurDate == null) {
            log.warn("发生日期为空，跳过成本归集：reimbursementId={}", reimbursementId);
            return;
        }

        // 幂等检查：通过 relatedVoucherId = reimbursementId 且 costType = COST_TYPE_OTHER 判重
        // 应对事件重发、@Async 重试或人工补偿场景下的重复归集
        long existCount = this.count(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getRelatedVoucherId, reimbursementId)
                .eq(CostRecord::getCostType, COST_TYPE_OTHER));
        if (existCount > 0L) {
            log.warn("报销成本已归集，跳过幂等重复：reimbursementId={}, existCount={}",
                    reimbursementId, existCount);
            return;
        }

        CostRecord record = new CostRecord();
        record.setCostNo(generateCostNo());
        record.setCostType(COST_TYPE_OTHER);
        record.setPeriod(occurDate.format(PERIOD_FORMATTER));
        record.setCostCenterId(departmentId);
        record.setAmount(amount);
        record.setCalculationMethod(CALCULATION_METHOD_ACTUAL);
        // 关联报销单ID用于幂等判重（监听器文档承诺：relatedVoucherId == reimbursementId 查询判重）
        record.setRelatedVoucherId(reimbursementId);
        record.setRemark("报销单号:" + reimbursementId
                + (reimbursementType != null ? "，类型:" + reimbursementType : ""));

        this.save(record);

        log.info("报销成本归集完成，报销单ID：{}，成本记录ID：{}", reimbursementId, record.getCostId());
    }

    /**
     * 生成成本编号：CR + yyyyMMdd + 4位序号
     */
    private String generateCostNo() {
        String datePart = LocalDate.now().format(COST_NO_DATE_FORMATTER);
        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CostRecord::getCostNo, "CR" + datePart)
               .orderByDesc(CostRecord::getCostId)
               .last("LIMIT 1");
        CostRecord last = this.getOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getCostNo() != null) {
            String no = last.getCostNo();
            try {
                // CR + 8位日期 + 4位序号，取末4位
                seq = Integer.parseInt(no.substring(no.length() - 4)) + 1;
            } catch (NumberFormatException ignored) {
                // 解析失败时从 1 开始
            }
        }
        return "CR" + datePart + String.format("%04d", seq);
    }

    /**
     * 实体转VO
     */
    private CostRecordVO convertToVO(CostRecord entity) {
        CostRecordVO vo = new CostRecordVO();
        vo.setCostId(entity.getCostId());
        vo.setCostNo(entity.getCostNo());
        vo.setCostType(entity.getCostType());
        vo.setCostTypeName(getCostTypeName(entity.getCostType()));
        vo.setPeriod(entity.getPeriod());
        vo.setCostCenterId(entity.getCostCenterId());
        vo.setAmount(entity.getAmount());
        vo.setAmountDisplay(formatAmount(entity.getAmount()));
        vo.setQuantity(entity.getQuantity());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setCalculationMethod(entity.getCalculationMethod());
        vo.setCalculationMethodName(getCalculationMethodName(entity.getCalculationMethod()));
        vo.setRelatedVoucherId(entity.getRelatedVoucherId());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String getCostTypeName(Integer costType) {
        if (costType == null) return "未知";
        switch (costType) {
            case 1: return "食材成本";
            case 2: return "人工成本";
            case 3: return "租金成本";
            case 4: return "水电成本";
            case 5: return "折旧成本";
            case 6: return "包装成本";
            case 7: return "其他成本";
            default: return "未知";
        }
    }

    private String getCalculationMethodName(Integer method) {
        if (method == null) return "未知";
        switch (method) {
            case 1: return "实际发生";
            case 2: return "分摊";
            case 3: return "预估";
            default: return "未知";
        }
    }

    private String formatAmount(Long amount) {
        if (amount == null) return "0.00";
        return String.valueOf(amount / 100.0);
    }
}
