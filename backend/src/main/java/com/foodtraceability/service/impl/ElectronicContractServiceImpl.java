package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.ElectronicContract;
import com.foodtraceability.mapper.ElectronicContractMapper;
import com.foodtraceability.service.ElectronicContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@Transactional(rollbackFor = Exception.class)
public class ElectronicContractServiceImpl extends ServiceImpl<ElectronicContractMapper, ElectronicContract> implements ElectronicContractService {

    @Override
    public Page<ElectronicContract> getElectronicContractPage(Page<ElectronicContract> page, String contractNo, Long supplierId, String status, String startDate, String endDate) {
        LambdaQueryWrapper<ElectronicContract> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(contractNo)) {
            queryWrapper.like(ElectronicContract::getContractNo, contractNo);
        }
        if (supplierId != null) {
            queryWrapper.eq(ElectronicContract::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(ElectronicContract::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(ElectronicContract::getCreateTime, startDate);
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(ElectronicContract::getCreateTime, endDate);
        }
        
        queryWrapper.orderByDesc(ElectronicContract::getCreateTime);
        return this.page(page, queryWrapper);
    }

    @Override
    public ElectronicContract createElectronicContract(ElectronicContract electronicContract) {
        electronicContract.setContractNo("EC" + System.currentTimeMillis());
        electronicContract.setStatus("draft");
        electronicContract.setCreateTime(LocalDateTime.now());
        this.save(electronicContract);
        return electronicContract;
    }

    @Override
    public ElectronicContract updateElectronicContract(Long id, ElectronicContract electronicContract) {
        ElectronicContract existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("电子合同不存在");
        }
        electronicContract.setId(id);
        electronicContract.setUpdateTime(LocalDateTime.now());
        this.updateById(electronicContract);
        return this.getById(id);
    }

    @Override
    public void deleteElectronicContract(Long id) {
        this.removeById(id);
    }

    @Override
    public ElectronicContract getElectronicContractById(Long id) {
        return this.getById(id);
    }

    @Override
    public ElectronicContract sendForSign(Long id) {
        ElectronicContract electronicContract = this.getById(id);
        if (electronicContract == null) {
            throw new RuntimeException("电子合同不存在");
        }
        electronicContract.setStatus("pending_sign");
        electronicContract.setUpdateTime(LocalDateTime.now());
        this.updateById(electronicContract);
        return electronicContract;
    }

    @Override
    public ElectronicContract signElectronicContract(Long id, String sealId) {
        ElectronicContract electronicContract = this.getById(id);
        if (electronicContract == null) {
            throw new RuntimeException("电子合同不存在");
        }
        electronicContract.setStatus("signed");
        // 记录签署使用的印章ID（关联 seals 表）
        if (sealId != null && !sealId.trim().isEmpty()) {
            electronicContract.setSealId(sealId);
        }
        electronicContract.setSignDate(LocalDateTime.now());
        electronicContract.setUpdateTime(LocalDateTime.now());
        this.updateById(electronicContract);
        return electronicContract;
    }

    @Override
    public ElectronicContract cancelElectronicContract(Long id, String reason) {
        ElectronicContract electronicContract = this.getById(id);
        if (electronicContract == null) {
            throw new RuntimeException("电子合同不存在");
        }
        electronicContract.setStatus("cancelled");
        electronicContract.setRemark(reason);
        electronicContract.setUpdateTime(LocalDateTime.now());
        this.updateById(electronicContract);
        return electronicContract;
    }
}
