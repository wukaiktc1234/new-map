package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.CostRecord;
import com.foodtraceability.mapper.finance.CostRecordMapper;
import com.foodtraceability.service.finance.CostService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成本核算Service实现
 */
@Service
public class CostServiceImpl extends ServiceImpl<CostRecordMapper, CostRecord>
        implements CostService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CostRecordVO create(CostRecordCreateDTO dto) {
        CostRecord entity = new CostRecord();
        entity.setCostNo(generateCostNo());
        BeanUtils.copyProperties(dto, entity);
        if (entity.getCalculationMethod() == null) {
            entity.setCalculationMethod(1); // 默认实际发生
        }

        baseMapper.insert(entity);
        return getDetail(entity.getCostId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(CostRecordUpdateDTO dto) {
        CostRecord existing = baseMapper.selectById(dto.getCostId());
        if (existing == null) {
            return false;
        }
        BeanUtils.copyProperties(dto, existing);
        return baseMapper.updateById(existing) > 0;
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
                .map(e -> { CostRecordVO v = new CostRecordVO(); BeanUtils.copyProperties(e, v); return v; })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<CostRecordVO> result = (IPage<CostRecordVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public Map<Integer, Long> summarizeByPeriod(String period) {
        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostRecord::getPeriod, period);
        List<CostRecord> records = baseMapper.selectList(wrapper);

        Map<Integer, Long> summary = new HashMap<>();
        for (CostRecord record : records) {
            summary.merge(record.getCostType(), record.getAmount(), Long::sum);
        }
        return summary;
    }

    @Override
    public CostRecordVO getDetail(Long costId) {
        CostRecord entity = baseMapper.selectById(costId);
        if (entity == null) {
            return null;
        }
        CostRecordVO vo = new CostRecordVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /** 生成成本编号: CR{yyyyMM}{序号} */
    private String generateCostNo() {
        String period = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String prefix = "CR" + period;
        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CostRecord::getCostNo, prefix)
               .orderByDesc(CostRecord::getCostNo)
               .last("LIMIT 1");
        CostRecord last = baseMapper.selectOne(wrapper);

        int seq = 1;
        if (last != null && last.getCostNo() != null) {
            try {
                String seqStr = last.getCostNo().substring(prefix.length());
                seq = Integer.parseInt(seqStr) + 1;
            } catch (Exception ignored) {}
        }
        return prefix + String.format("%04d", seq);
    }
}
