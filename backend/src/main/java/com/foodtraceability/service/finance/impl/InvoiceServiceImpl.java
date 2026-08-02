package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceInvoice;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.FinanceInvoiceMapper;
import com.foodtraceability.service.finance.InvoiceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 发票管理Service实现
 *
 * <p>Sprint 3.1 P0 F-003/F-004：发票 CRUD + 状态流转管理。</p>
 *
 * <p>状态码：
 * <ul>
 *   <li>0=draft（草稿）</li>
 *   <li>5=issued（已开具）</li>
 *   <li>6=void（已作废）</li>
 *   <li>4=red-flushed（已红冲）</li>
 * </ul>
 * </p>
 */
@Service
public class InvoiceServiceImpl extends ServiceImpl<FinanceInvoiceMapper, FinanceInvoice>
        implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    /** 发票状态：草稿 */
    private static final int STATUS_DRAFT = 0;
    /** 发票状态：已开具 */
    private static final int STATUS_ISSUED = 5;
    /** 发票状态：已作废 */
    private static final int STATUS_VOID = 6;
    /** 发票状态：已红冲 */
    private static final int STATUS_RED_FLUSHED = 4;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceInvoiceVO create(FinanceInvoiceCreateDTO dto) {
        // 校验发票号码唯一性
        LambdaQueryWrapper<FinanceInvoice> noCheck = new LambdaQueryWrapper<>();
        noCheck.eq(FinanceInvoice::getInvoiceNo, dto.getInvoiceNo());
        if (baseMapper.selectCount(noCheck) > 0) {
            throw new BusinessException("发票号码已存在: " + dto.getInvoiceNo());
        }

        FinanceInvoice entity = new FinanceInvoice();
        BeanUtils.copyProperties(dto, entity);

        if (dto.getInvoiceDate() != null && !dto.getInvoiceDate().isBlank()) {
            entity.setInvoiceDate(LocalDate.parse(dto.getInvoiceDate()));
        }
        if (dto.getReceiveDate() != null && !dto.getReceiveDate().isBlank()) {
            entity.setReceiveDate(LocalDate.parse(dto.getReceiveDate()));
        }

        // 默认状态为草稿（draft）
        if (entity.getInvoiceStatus() == null) {
            entity.setInvoiceStatus(STATUS_DRAFT);
        }

        baseMapper.insert(entity);
        return getDetail(entity.getInvoiceId());
    }

    @Override
    public FinanceInvoiceVO getDetail(Long invoiceId) {
        FinanceInvoice entity = baseMapper.selectById(invoiceId);
        if (entity == null) {
            return null;
        }
        FinanceInvoiceVO vo = new FinanceInvoiceVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public IPage<FinanceInvoiceVO> getPage(FinanceInvoiceQueryDTO query) {
        Page<FinanceInvoice> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<FinanceInvoice> wrapper = new LambdaQueryWrapper<>();

        if (query.getInvoiceNo() != null && !query.getInvoiceNo().isBlank()) {
            wrapper.like(FinanceInvoice::getInvoiceNo, query.getInvoiceNo());
        }
        if (query.getInvoiceType() != null) {
            wrapper.eq(FinanceInvoice::getInvoiceType, query.getInvoiceType());
        }
        if (query.getInvoiceCategory() != null) {
            wrapper.eq(FinanceInvoice::getInvoiceCategory, query.getInvoiceCategory());
        }
        if (query.getInvoiceStatus() != null) {
            wrapper.eq(FinanceInvoice::getInvoiceStatus, query.getInvoiceStatus());
        }
        if (query.getBuyerName() != null && !query.getBuyerName().isBlank()) {
            wrapper.like(FinanceInvoice::getBuyerName, query.getBuyerName());
        }
        if (query.getSellerName() != null && !query.getSellerName().isBlank()) {
            wrapper.like(FinanceInvoice::getSellerName, query.getSellerName());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(FinanceInvoice::getInvoiceDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(FinanceInvoice::getInvoiceDate, query.getEndDate());
        }
        wrapper.orderByDesc(FinanceInvoice::getCreateTime);

        IPage<FinanceInvoice> entityPage = baseMapper.selectPage(page, wrapper);

        Page<FinanceInvoiceVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<FinanceInvoiceVO> voList = entityPage.getRecords().stream()
                .map(e -> { FinanceInvoiceVO v = new FinanceInvoiceVO(); BeanUtils.copyProperties(e, v); return v; })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<FinanceInvoiceVO> result = (IPage<FinanceInvoiceVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceInvoiceVO update(FinanceInvoiceUpdateDTO dto) {
        log.info("更新发票，ID：{}", dto.getInvoiceId());

        FinanceInvoice entity = baseMapper.selectById(dto.getInvoiceId());
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        // 仅草稿状态可修改
        if (entity.getInvoiceStatus() == null || entity.getInvoiceStatus() != STATUS_DRAFT) {
            throw new BusinessException("只有草稿状态的发票可以修改");
        }

        // 校验发票号码唯一性（排除自身）
        if (dto.getInvoiceNo() != null && !dto.getInvoiceNo().equals(entity.getInvoiceNo())) {
            LambdaQueryWrapper<FinanceInvoice> noCheck = new LambdaQueryWrapper<>();
            noCheck.eq(FinanceInvoice::getInvoiceNo, dto.getInvoiceNo());
            noCheck.ne(FinanceInvoice::getInvoiceId, dto.getInvoiceId());
            if (baseMapper.selectCount(noCheck) > 0) {
                throw new BusinessException("发票号码已存在: " + dto.getInvoiceNo());
            }
            entity.setInvoiceNo(dto.getInvoiceNo());
        }

        // 选择性更新字段
        if (dto.getInvoiceCode() != null) {
            entity.setInvoiceCode(dto.getInvoiceCode());
        }
        if (dto.getInvoiceType() != null) {
            entity.setInvoiceType(dto.getInvoiceType());
        }
        if (dto.getInvoiceCategory() != null) {
            entity.setInvoiceCategory(dto.getInvoiceCategory());
        }
        if (dto.getBuyerName() != null) {
            entity.setBuyerName(dto.getBuyerName());
        }
        if (dto.getBuyerTaxNo() != null) {
            entity.setBuyerTaxNo(dto.getBuyerTaxNo());
        }
        if (dto.getSellerName() != null) {
            entity.setSellerName(dto.getSellerName());
        }
        if (dto.getSellerTaxNo() != null) {
            entity.setSellerTaxNo(dto.getSellerTaxNo());
        }
        if (dto.getTotalAmount() != null) {
            entity.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getTaxAmount() != null) {
            entity.setTaxAmount(dto.getTaxAmount());
        }
        if (dto.getTotalAmountWithTax() != null) {
            entity.setTotalAmountWithTax(dto.getTotalAmountWithTax());
        }
        if (dto.getInvoiceDate() != null && !dto.getInvoiceDate().isBlank()) {
            entity.setInvoiceDate(LocalDate.parse(dto.getInvoiceDate()));
        }
        if (dto.getReceiveDate() != null && !dto.getReceiveDate().isBlank()) {
            entity.setReceiveDate(LocalDate.parse(dto.getReceiveDate()));
        }
        if (dto.getImageUrl() != null) {
            entity.setImageUrl(dto.getImageUrl());
        }
        if (dto.getRelatedRecordId() != null) {
            entity.setRelatedRecordId(dto.getRelatedRecordId());
        }

        baseMapper.updateById(entity);
        return getDetail(entity.getInvoiceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long invoiceId) {
        log.info("删除发票，ID：{}", invoiceId);
        FinanceInvoice entity = baseMapper.selectById(invoiceId);
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        // 逻辑删除（@TableLogic 自动处理）
        return removeById(invoiceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceInvoiceVO issue(Long invoiceId) {
        log.info("开具发票，ID：{}", invoiceId);
        FinanceInvoice entity = baseMapper.selectById(invoiceId);
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        if (entity.getInvoiceStatus() == null || entity.getInvoiceStatus() != STATUS_DRAFT) {
            throw new BusinessException("只有草稿状态的发票可以开具");
        }
        entity.setInvoiceStatus(STATUS_ISSUED);
        baseMapper.updateById(entity);
        return getDetail(entity.getInvoiceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceInvoiceVO voidInvoice(Long invoiceId) {
        log.info("作废发票，ID：{}", invoiceId);
        FinanceInvoice entity = baseMapper.selectById(invoiceId);
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        if (entity.getInvoiceStatus() == null || entity.getInvoiceStatus() != STATUS_ISSUED) {
            throw new BusinessException("只有已开具状态的发票可以作废");
        }
        entity.setInvoiceStatus(STATUS_VOID);
        baseMapper.updateById(entity);
        return getDetail(entity.getInvoiceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceInvoiceVO redFlush(Long invoiceId) {
        log.info("红冲发票，ID：{}", invoiceId);
        FinanceInvoice entity = baseMapper.selectById(invoiceId);
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        if (entity.getInvoiceStatus() == null || entity.getInvoiceStatus() != STATUS_ISSUED) {
            throw new BusinessException("只有已开具状态的发票可以红冲");
        }
        entity.setInvoiceStatus(STATUS_RED_FLUSHED);
        baseMapper.updateById(entity);
        return getDetail(entity.getInvoiceId());
    }
}
