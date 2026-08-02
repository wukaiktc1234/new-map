package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.trace.PurchaseLedgerCreateDTO;
import com.foodtraceability.dto.trace.PurchaseLedgerQueryDTO;
import com.foodtraceability.dto.trace.PurchaseLedgerVO;
import com.foodtraceability.entity.PurchaseLedger;
import com.foodtraceability.mapper.PurchaseLedgerMapper;
import com.foodtraceability.service.trace.PurchaseLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 进货台账服务实现类
 */
@Service
public class PurchaseLedgerServiceImpl extends ServiceImpl<PurchaseLedgerMapper, PurchaseLedger>
        implements PurchaseLedgerService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseLedgerMapper purchaseLedgerMapper;

    public PurchaseLedgerServiceImpl(PurchaseLedgerMapper purchaseLedgerMapper) {
        this.purchaseLedgerMapper = purchaseLedgerMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseLedgerVO create(PurchaseLedgerCreateDTO dto) {
        // 生成台账编号
        String ledgerNo = "LD" + LocalDateTime.now().format(DATE_FMT) +
                String.format("%04d", System.currentTimeMillis() % 10000);

        PurchaseLedger ledger = new PurchaseLedger();
        ledger.setLedgerNo(ledgerNo);
        ledger.setPurchaseStockinId(dto.getPurchaseStockinId());
        ledger.setSupplierId(dto.getSupplierId());
        ledger.setSupplierName(dto.getSupplierName());
        ledger.setMaterialName(dto.getMaterialName());
        ledger.setSpecification(dto.getSpecification());
        ledger.setUnit(dto.getUnit());
        ledger.setQuantity(dto.getQuantity());
        ledger.setUnitPrice(dto.getUnitPrice());
        ledger.setAmount(dto.getAmount());
        ledger.setBatchNo(dto.getBatchNo());
        ledger.setProductionDate(dto.getProductionDate());
        ledger.setExpiryDate(dto.getExpiryDate());
        ledger.setQualityInspectionResult(dto.getQualityInspectionResult() != null ? dto.getQualityInspectionResult() : 3);
        ledger.setCertificateNo(dto.getCertificateNo());
        ledger.setCertificateType(dto.getCertificateType());
        ledger.setCertificateImageUrl(dto.getCertificateImageUrl());
        ledger.setStorageLocation(dto.getStorageLocation());
        ledger.setRemark(dto.getRemark());

        save(ledger);
        return convertToVO(ledger);
    }

    @Override
    public IPage<PurchaseLedgerVO> queryPage(PurchaseLedgerQueryDTO queryDTO) {
        Page<PurchaseLedger> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<PurchaseLedger> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(PurchaseLedger::getMaterialName, queryDTO.getKeyword())
                    .or().like(PurchaseLedger::getSupplierName, queryDTO.getKeyword())
                    .or().like(PurchaseLedger::getBatchNo, queryDTO.getKeyword()));
        }
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(PurchaseLedger::getSupplierId, queryDTO.getSupplierId());
        }
        if (queryDTO.getBatchNo() != null) {
            wrapper.eq(PurchaseLedger::getBatchNo, queryDTO.getBatchNo());
        }
        if (queryDTO.getQualityInspectionResult() != null) {
            wrapper.eq(PurchaseLedger::getQualityInspectionResult, queryDTO.getQualityInspectionResult());
        }

        wrapper.orderByDesc(PurchaseLedger::getCreateTime);

        IPage<PurchaseLedger> pageResult = page(page, wrapper);
        Page<PurchaseLedgerVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public PurchaseLedgerVO getDetailById(Long ledgerId) {
        PurchaseLedger ledger = getById(ledgerId);
        if (ledger == null) {
            throw new IllegalArgumentException("台账记录不存在: " + ledgerId);
        }
        return convertToVO(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateQualityResult(Long ledgerId, Integer result, Long inspectorId) {
        PurchaseLedger ledger = getById(ledgerId);
        if (ledger == null) {
            throw new IllegalArgumentException("台账记录不存在: " + ledgerId);
        }
        ledger.setQualityInspectionResult(result);
        ledger.setInspectionUserId(inspectorId);
        ledger.setInspectionTime(LocalDateTime.now());
        return updateById(ledger);
    }

    private PurchaseLedgerVO convertToVO(PurchaseLedger entity) {
        PurchaseLedgerVO vo = new PurchaseLedgerVO();
        vo.setLedgerId(entity.getLedgerId());
        vo.setLedgerNo(entity.getLedgerNo());
        vo.setPurchaseStockinId(entity.getPurchaseStockinId());
        vo.setSupplierId(entity.getSupplierId());
        vo.setSupplierName(entity.getSupplierName());
        vo.setMaterialName(entity.getMaterialName());
        vo.setSpecification(entity.getSpecification());
        vo.setUnit(entity.getUnit());
        vo.setQuantity(entity.getQuantity());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setAmount(entity.getAmount());
        vo.setBatchNo(entity.getBatchNo());
        vo.setProductionDate(entity.getProductionDate());
        vo.setExpiryDate(entity.getExpiryDate());
        vo.setQualityInspectionResult(entity.getQualityInspectionResult());
        vo.setQualityInspectionResultName(getQualityResultName(entity.getQualityInspectionResult()));
        vo.setInspectionTime(entity.getInspectionTime());
        vo.setCertificateNo(entity.getCertificateNo());
        vo.setCertificateType(entity.getCertificateType());
        vo.setCertificateImageUrl(entity.getCertificateImageUrl());
        vo.setStorageLocation(entity.getStorageLocation());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private static String getQualityResultName(Integer result) {
        if (result == null) return "待检";
        switch (result) {
            case 1: return "合格";
            case 2: return "不合格";
            case 3: return "待检";
            default: return "未知";
        }
    }
}
