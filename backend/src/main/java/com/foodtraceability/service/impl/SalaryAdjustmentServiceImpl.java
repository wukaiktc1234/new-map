package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.SalaryAdjustment;
import com.foodtraceability.mapper.SalaryAdjustmentMapper;
import com.foodtraceability.service.SalaryAdjustmentService;
import org.springframework.stereotype.Service;

/**
 * 薪资调整Service实现类
 * @author example
 * @since 2025-12-05
 */
@Service
public class SalaryAdjustmentServiceImpl extends ServiceImpl<SalaryAdjustmentMapper, SalaryAdjustment> implements SalaryAdjustmentService {
    // 可以添加自定义业务方法实现
}