package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.TaxRecord;
import com.foodtraceability.dto.TaxRecordDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 税务记录Service接口
 * @author example
 * @since 2025-12-05
 */
public interface TaxRecordService extends IService<TaxRecord> {
    /**
     * 分页查询税务记录
     * @param page 分页参数
     * @param taxRecordDTO 查询条件
     * @return 分页结果
     */
    IPage<TaxRecord> getTaxRecordPage(Page<TaxRecord> page, TaxRecordDTO taxRecordDTO);
    
    /**
     * 根据ID获取税务记录详情
     * @param id 税务记录ID
     * @return 税务记录详情
     */
    TaxRecord getTaxRecordById(Long id);
    
    /**
     * 创建税务记录
     * @param taxRecordDTO 税务记录DTO
     * @return 创建结果
     */
    boolean createTaxRecord(TaxRecordDTO taxRecordDTO);
    
    /**
     * 更新税务记录
     * @param id 税务记录ID
     * @param taxRecordDTO 税务记录DTO
     * @return 更新结果
     */
    boolean updateTaxRecord(Long id, TaxRecordDTO taxRecordDTO);
    
    /**
     * 删除税务记录
     * @param id 税务记录ID
     * @return 删除结果
     */
    boolean deleteTaxRecord(Long id);
    
    /**
     * 批量删除税务记录
     * @param ids 税务记录ID列表
     * @return 删除结果
     */
    boolean batchDeleteTaxRecord(Long[] ids);
}