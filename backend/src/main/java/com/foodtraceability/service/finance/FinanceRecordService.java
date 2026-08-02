package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceRecord;

/**
 * 收支流水Service接口
 * 管理企业所有收入、支出和转账业务
 */
public interface FinanceRecordService extends IService<FinanceRecord> {

    /**
     * 创建收支记录
     */
    FinanceRecordVO create(FinanceRecordCreateDTO dto);

    /**
     * 更新收支记录（仅待审批状态可修改）
     */
    boolean update(FinanceRecordUpdateDTO dto);

    /**
     * 获取收支记录详情
     */
    FinanceRecordVO getDetail(Long recordId);

    /**
     * 分页查询收支记录
     */
    IPage<FinanceRecordVO> getPage(FinanceRecordQueryDTO query);

    /**
     * 审批收支记录
     */
    boolean approve(Long recordId, boolean approved);
}
