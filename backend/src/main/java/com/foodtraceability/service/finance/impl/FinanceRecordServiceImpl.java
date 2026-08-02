package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceRecord;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.FinanceRecordMapper;
import com.foodtraceability.service.finance.FinanceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 收支流水Service实现类
 */
@Service
public class FinanceRecordServiceImpl extends ServiceImpl<FinanceRecordMapper, FinanceRecord>
        implements FinanceRecordService {

    private static final Logger log = LoggerFactory.getLogger(FinanceRecordServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceRecordVO create(FinanceRecordCreateDTO dto) {
        log.info("创建收支记录，类型：{}，金额：{}", dto.getRecordType(), dto.getAmount());

        FinanceRecord record = new FinanceRecord();
        record.setRecordNo(generateRecordNo());
        record.setRecordType(dto.getRecordType());
        record.setRecordCategory(dto.getRecordCategory());
        record.setAmount(dto.getAmount());
        record.setPaymentMethod(dto.getPaymentMethod());
        record.setAccountSubjectId(dto.getAccountSubjectId());
        record.setCounterpartyName(dto.getCounterpartyName());
        record.setCounterpartyType(dto.getCounterpartyType());
        record.setBusinessDate(dto.getBusinessDate());
        record.setRecordDate(LocalDate.now());
        record.setApprovalStatus(0); // 待审批
        record.setRemark(dto.getRemark());

        this.save(record);

        return getDetail(record.getRecordId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(FinanceRecordUpdateDTO dto) {
        FinanceRecord record = this.getById(dto.getRecordId());
        if (record == null) {
            throw new BusinessException("记录不存在");
        }

        // 仅待审批状态可修改
        if (record.getApprovalStatus() != null && record.getApprovalStatus() != 0) {
            throw new BusinessException("只有待审批状态的记录可以修改");
        }

        if (dto.getRecordType() != null) record.setRecordType(dto.getRecordType());
        if (dto.getRecordCategory() != null) record.setRecordCategory(dto.getRecordCategory());
        if (dto.getAmount() != null) record.setAmount(dto.getAmount());
        if (dto.getPaymentMethod() != null) record.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getAccountSubjectId() != null) record.setAccountSubjectId(dto.getAccountSubjectId());
        if (dto.getCounterpartyName() != null) record.setCounterpartyName(dto.getCounterpartyName());
        if (dto.getCounterpartyType() != null) record.setCounterpartyType(dto.getCounterpartyType());
        if (dto.getBusinessDate() != null) record.setBusinessDate(dto.getBusinessDate());
        if (dto.getRemark() != null) record.setRemark(dto.getRemark());

        return this.updateById(record);
    }

    @Override
    public FinanceRecordVO getDetail(Long recordId) {
        FinanceRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public IPage<FinanceRecordVO> getPage(FinanceRecordQueryDTO query) {
        Page<FinanceRecord> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<FinanceRecord> recordPage = baseMapper.selectRecordPage(page,
                query.getRecordNo(), query.getRecordType(), query.getRecordCategory(),
                query.getPaymentMethod(), query.getCounterpartyName(),
                query.getStartDate(), query.getEndDate(), query.getApprovalStatus());

        Page<FinanceRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        java.util.List<FinanceRecordVO> records = new java.util.ArrayList<>();
        for (FinanceRecord r : recordPage.getRecords()) {
            records.add(convertToVO(r));
        }
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long recordId, boolean approved) {
        FinanceRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }

        if (approved) {
            record.setApprovalStatus(1); // 已审批
        } else {
            record.setApprovalStatus(2); // 已驳回
        }
        record.setApproveTime(java.time.LocalDateTime.now());

        return this.updateById(record);
    }

    /** 生成记录编号 */
    private String generateRecordNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FinanceRecord::getRecordNo, "SZ" + datePart)
               .orderByDesc(FinanceRecord::getRecordId)
               .last("LIMIT 1");
        FinanceRecord last = this.getOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getRecordNo() != null) {
            String seqStr = last.getRecordNo().substring(last.getRecordNo().length() - 4);
            seq = Integer.parseInt(seqStr) + 1;
        }

        return String.format("SZ%s%04d", datePart, seq);
    }

    private FinanceRecordVO convertToVO(FinanceRecord record) {
        FinanceRecordVO vo = new FinanceRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setRecordNo(record.getRecordNo());
        vo.setRecordType(record.getRecordType());
        vo.setRecordTypeName(getRecordTypeName(record.getRecordType()));
        vo.setRecordCategory(record.getRecordCategory());
        vo.setRecordCategoryName(getRecordCategoryName(record.getRecordCategory()));
        vo.setAmount(record.getAmount());
        vo.setAmountDisplay(formatAmount(record.getAmount()));
        vo.setPaymentMethod(record.getPaymentMethod());
        vo.setPaymentMethodName(getPaymentMethodName(record.getPaymentMethod()));
        vo.setAccountSubjectId(record.getAccountSubjectId());
        vo.setCounterpartyName(record.getCounterpartyName());
        vo.setCounterpartyType(record.getCounterpartyType());
        vo.setBusinessDate(record.getBusinessDate());
        vo.setRecordDate(record.getRecordDate());
        vo.setVoucherId(record.getVoucherId());
        vo.setApprovalStatus(record.getApprovalStatus());
        vo.setApprovalStatusName(getApprovalStatusName(record.getApprovalStatus()));
        vo.setApproveTime(record.getApproveTime());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private String getRecordTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) { case 1: return "收入"; case 2: return "支出"; case 3: return "转账"; default: return "未知"; }
    }

    private String getRecordCategoryName(Integer category) {
        if (category == null) return "未知";
        switch (category) {
            case 101: return "销售收入";
            case 102: return "服务收入";
            case 103: return "其他收入";
            case 201: return "采购支出";
            case 202: return "工资支出";
            case 203: return "租金支出";
            case 204: return "水电支出";
            case 205: return "其他支出";
            default: return "未知";
        }
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null) return "未知";
        switch (method) { case 1: return "现金"; case 2: return "银行存款"; case 3: return "微信"; case 4: return "支付宝"; default: return "未知"; }
    }

    private String getApprovalStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) { case 0: return "待审批"; case 1: return "已审批"; case 2: return "已驳回"; default: return "未知"; }
    }

    private String formatAmount(Long amount) {
        if (amount == null) return "0.00";
        return String.valueOf(amount / 100.0);
    }
}
