package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.finance.BankPaymentRecord;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.BankPaymentRecordMapper;
import com.foodtraceability.service.finance.BankPaymentRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 银行打款记录Service实现
 */
@Service
public class BankPaymentRecordServiceImpl extends ServiceImpl<BankPaymentRecordMapper, BankPaymentRecord>
        implements BankPaymentRecordService {

    private static final Logger log = LoggerFactory.getLogger(BankPaymentRecordServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankPaymentRecord create(BankPaymentRecord record) {
        log.info("创建银行打款记录，付款单ID：{}", record.getPaymentId());

        // 校验付款单ID
        if (record.getPaymentId() == null) {
            throw new BusinessException("付款单ID不能为空");
        }

        // 设置默认状态
        if (record.getStatus() == null) {
            record.setStatus(0); // 待回填
        }

        // 保存记录
        baseMapper.insert(record);
        log.info("银行打款记录已创建：{}", record.getRecordId());

        return record;
    }

    @Override
    public List<BankPaymentRecord> getByPaymentId(Long paymentId) {
        LambdaQueryWrapper<BankPaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankPaymentRecord::getPaymentId, paymentId)
               .orderByDesc(BankPaymentRecord::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public BankPaymentRecord getById(Long recordId) {
        BankPaymentRecord record = baseMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("银行打款记录不存在");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankPaymentRecord fillBankInfo(Long recordId, String bankTransactionNo,
                                          LocalDateTime bankTransactionTime,
                                          String voucherType, String voucherUrl) {
        log.info("回填银行记录，记录ID：{}", recordId);

        BankPaymentRecord record = getById(recordId);

        // 校验状态：只有待回填状态可以回填
        if (record.getStatus() != null && record.getStatus() != 0) {
            throw new BusinessException("当前状态不可回填银行记录");
        }

        // 回填银行信息
        record.setBankTransactionNo(bankTransactionNo);
        record.setBankTransactionTime(bankTransactionTime);
        record.setVoucherType(voucherType);
        record.setVoucherUrl(voucherUrl);
        record.setStatus(1); // 已回填

        baseMapper.updateById(record);
        log.info("银行记录已回填：{}", recordId);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankPaymentRecord confirm(Long recordId, String confirmResult,
                                     Long confirmUserId, String confirmUserName,
                                     String confirmRemark) {
        log.info("确认银行记录，记录ID：{}，结果：{}", recordId, confirmResult);

        BankPaymentRecord record = getById(recordId);

        // 校验状态：只有已回填状态可以确认
        if (record.getStatus() == null || record.getStatus() != 1) {
            throw new BusinessException("当前状态不可确认");
        }

        // 校验确认结果
        if (!"success".equals(confirmResult) && !"fail".equals(confirmResult)) {
            throw new BusinessException("确认结果必须为 success 或 fail");
        }

        // 设置确认信息
        record.setConfirmUserId(confirmUserId);
        record.setConfirmUserName(confirmUserName);
        record.setConfirmTime(LocalDateTime.now());
        record.setConfirmResult(confirmResult);
        record.setConfirmRemark(confirmRemark);

        // 根据确认结果更新状态
        if ("success".equals(confirmResult)) {
            record.setStatus(3); // 成功
        } else {
            record.setStatus(4); // 失败
        }

        baseMapper.updateById(record);
        log.info("银行记录已确认：{}，结果：{}", recordId, confirmResult);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandon(Long recordId) {
        log.info("废弃银行记录，记录ID：{}", recordId);

        BankPaymentRecord record = getById(recordId);

        // 校验状态：只有待回填或已回填状态可以废弃
        if (record.getStatus() == null || (record.getStatus() != 0 && record.getStatus() != 1)) {
            throw new BusinessException("当前状态不可废弃");
        }

        record.setStatus(5); // 已废弃
        baseMapper.updateById(record);
        log.info("银行记录已废弃：{}", recordId);
    }
}