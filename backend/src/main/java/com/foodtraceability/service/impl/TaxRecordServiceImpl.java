package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.TaxRecordDTO;
import com.foodtraceability.entity.TaxRecord;
import com.foodtraceability.mapper.TaxRecordMapper;
import com.foodtraceability.service.TaxRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 税务记录Service实现类
 * @author example
 * @since 2025-12-05
 */
@Service
public class TaxRecordServiceImpl extends ServiceImpl<TaxRecordMapper, TaxRecord> implements TaxRecordService {
    
    @Override
    public IPage<TaxRecord> getTaxRecordPage(Page<TaxRecord> page, TaxRecordDTO taxRecordDTO) {
        LambdaQueryWrapper<TaxRecord> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按税种类型筛选
        if (taxRecordDTO.getTaxType() != null && !taxRecordDTO.getTaxType().isEmpty()) {
            queryWrapper.eq(TaxRecord::getTaxType, taxRecordDTO.getTaxType());
        }
        
        // 按纳税期间筛选
        if (taxRecordDTO.getTaxPeriod() != null && !taxRecordDTO.getTaxPeriod().isEmpty()) {
            queryWrapper.eq(TaxRecord::getTaxPeriod, taxRecordDTO.getTaxPeriod());
        }
        
        // 按纳税状态筛选
        if (taxRecordDTO.getTaxStatus() != null && !taxRecordDTO.getTaxStatus().isEmpty()) {
            queryWrapper.eq(TaxRecord::getTaxStatus, taxRecordDTO.getTaxStatus());
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc(TaxRecord::getCreateTime);
        
        return this.page(page, queryWrapper);
    }
    
    @Override
    public TaxRecord getTaxRecordById(Long id) {
        return this.getById(id);
    }
    
    @Override
    @SuppressWarnings("null")
    public boolean createTaxRecord(TaxRecordDTO taxRecordDTO) {
        TaxRecord taxRecord = new TaxRecord();
        BeanUtils.copyProperties(taxRecordDTO, taxRecord);
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        taxRecord.setCreateTime(now);
        taxRecord.setUpdateTime(now);
        
        return this.save(taxRecord);
    }
    
    @Override
    @SuppressWarnings("null")
    public boolean updateTaxRecord(Long id, TaxRecordDTO taxRecordDTO) {
        TaxRecord taxRecord = this.getById(id);
        if (taxRecord == null) {
            return false;
        }
        
        BeanUtils.copyProperties(taxRecordDTO, taxRecord);
        
        // 设置更新时间
        taxRecord.setUpdateTime(LocalDateTime.now());
        taxRecord.setId(id);
        
        return this.updateById(taxRecord);
    }
    
    @Override
    public boolean deleteTaxRecord(Long id) {
        return this.removeById(id);
    }
    
    @Override
    public boolean batchDeleteTaxRecord(Long[] ids) {
        return this.removeByIds(java.util.Arrays.asList(ids));
    }
}