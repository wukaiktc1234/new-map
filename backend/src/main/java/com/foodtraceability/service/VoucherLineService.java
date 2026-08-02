package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.VoucherLine;
import java.util.List;

/**
 * 凭证行服务接口
 * @author example
 * @since 2025-12-07
 */
public interface VoucherLineService extends IService<VoucherLine> {
    
    /**
     * 根据凭证ID获取凭证行列表
     * @param voucherId 凭证ID
     * @return 凭证行列表
     */
    List<VoucherLine> getByVoucherId(Long voucherId);
    
    /**
     * 根据科目ID获取凭证行列表
     * @param subjectId 科目ID
     * @return 凭证行列表
     */
    List<VoucherLine> getBySubjectId(Long subjectId);
    
    /**
     * 批量保存凭证行
     * @param voucherLines 凭证行列表
     * @return 操作结果
     */
    boolean batchSave(List<VoucherLine> voucherLines);
    
    /**
     * 根据凭证ID删除凭证行
     * @param voucherId 凭证ID
     * @return 操作结果
     */
    boolean deleteByVoucherId(Long voucherId);
}