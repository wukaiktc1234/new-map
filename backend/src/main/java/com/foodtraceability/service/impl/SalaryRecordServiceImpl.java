package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.SalaryRecord;
import com.foodtraceability.mapper.SalaryRecordMapper;
import com.foodtraceability.service.SalaryRecordService;
import org.springframework.stereotype.Service;

/**
 * 薪资记录Service实现类
 * @author example
 * @since 2025-12-05
 */
@Service
public class SalaryRecordServiceImpl extends ServiceImpl<SalaryRecordMapper, SalaryRecord> implements SalaryRecordService {
    // 可以添加自定义业务方法实现
}