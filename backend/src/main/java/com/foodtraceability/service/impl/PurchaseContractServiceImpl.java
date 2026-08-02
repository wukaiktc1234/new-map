package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.PurchaseContract;
import com.foodtraceability.mapper.PurchaseContractMapper;
import com.foodtraceability.service.PurchaseContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@Transactional(rollbackFor = Exception.class)
public class PurchaseContractServiceImpl extends ServiceImpl<PurchaseContractMapper, PurchaseContract> implements PurchaseContractService {

    @Override
    public Page<PurchaseContract> getPurchaseContractPage(Page<PurchaseContract> page, String contractNo, Long supplierId, String status, String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseContract> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(contractNo)) {
            queryWrapper.like(PurchaseContract::getContractNo, contractNo);
        }
        if (supplierId != null) {
            queryWrapper.eq(PurchaseContract::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(PurchaseContract::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(PurchaseContract::getCreateTime, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(PurchaseContract::getCreateTime, endDate);
        }
        
        queryWrapper.orderByDesc(PurchaseContract::getCreateTime);
        return this.page(page, queryWrapper);
    }

    @Override
    public PurchaseContract createPurchaseContract(PurchaseContract purchaseContract) {
        purchaseContract.setContractNo("PC" + System.currentTimeMillis());
        purchaseContract.setStatus("draft");
        purchaseContract.setCreateTime(LocalDateTime.now());
        this.save(purchaseContract);
        return purchaseContract;
    }

    @Override
    public PurchaseContract updatePurchaseContract(Long id, PurchaseContract purchaseContract) {
        PurchaseContract existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("采购合同不存在");
        }
        purchaseContract.setId(id);
        purchaseContract.setUpdateTime(LocalDateTime.now());
        this.updateById(purchaseContract);
        return this.getById(id);
    }

    @Override
    public void deletePurchaseContract(Long id) {
        this.removeById(id);
    }

    @Override
    public PurchaseContract getPurchaseContractById(Long id) {
        return this.getById(id);
    }

    @Override
    public PurchaseContract signPurchaseContract(Long id, String signatory) {
        PurchaseContract purchaseContract = this.getById(id);
        if (purchaseContract == null) {
            throw new RuntimeException("采购合同不存在");
        }
        purchaseContract.setStatus("signed");
        purchaseContract.setSignatory(signatory);
        purchaseContract.setSignDate(LocalDateTime.now());
        purchaseContract.setUpdateTime(LocalDateTime.now());
        this.updateById(purchaseContract);
        return purchaseContract;
    }

    @Override
    public PurchaseContract terminatePurchaseContract(Long id, String reason) {
        PurchaseContract purchaseContract = this.getById(id);
        if (purchaseContract == null) {
            throw new RuntimeException("采购合同不存在");
        }
        purchaseContract.setStatus("terminated");
        purchaseContract.setRemark(reason);
        purchaseContract.setUpdateTime(LocalDateTime.now());
        this.updateById(purchaseContract);
        return purchaseContract;
    }
}
