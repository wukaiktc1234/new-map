package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.service.finance.BankAccountService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 银行账户Service实现
 * 账号脱敏：保留后4位，前面用*替换
 */
@Service
public class BankAccountServiceImpl extends ServiceImpl<BankAccountMapper, BankAccount>
        implements BankAccountService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO create(BankAccountCreateDTO dto) {
        // 校验账号唯一性
        LambdaQueryWrapper<BankAccount> check = new LambdaQueryWrapper<>();
        check.eq(BankAccount::getAccountNumber, dto.getAccountNumber());
        if (baseMapper.selectCount(check) > 0) {
            throw new BusinessException("账号已存在");
        }

        BankAccount entity = new BankAccount();
        BeanUtils.copyProperties(dto, entity);
        entity.setAccountNumberMasked(maskAccountNumber(dto.getAccountNumber()));
        entity.setBalance(0L); // 新账户初始余额为0
        entity.setCurrency("CNY"); // 默认人民币
        entity.setStatus(1); // 默认启用
        baseMapper.insert(entity);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long accountId, BankAccountUpdateDTO dto) {
        BankAccount existing = baseMapper.selectById(accountId);
        if (existing == null) {
            throw new BusinessException("银行账户不存在");
        }

        BankAccount updateEntity = new BankAccount();
        updateEntity.setAccountId(accountId);

        if (dto.getAccountName() != null) {
            updateEntity.setAccountName(dto.getAccountName());
        }
        if (dto.getBankName() != null) {
            updateEntity.setBankName(dto.getBankName());
        }
        if (dto.getAccountType() != null) {
            updateEntity.setAccountType(dto.getAccountType());
        }
        if (dto.getStatus() != null) {
            updateEntity.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long accountId) {
        BankAccount existing = baseMapper.selectById(accountId);
        if (existing == null) {
            throw new BusinessException("银行账户不存在");
        }
        return baseMapper.deleteById(accountId) > 0;
    }

    @Override
    public BankAccountVO getDetail(Long accountId) {
        BankAccount entity = baseMapper.selectById(accountId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<BankAccountVO> getPage(BankAccountQueryDTO query) {
        Page<BankAccount> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();

        if (query.getAccountType() != null) {
            wrapper.eq(BankAccount::getAccountType, query.getAccountType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BankAccount::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(BankAccount::getAccountName, query.getKeyword())
                    .or().like(BankAccount::getBankName, query.getKeyword()));
        }
        wrapper.orderByDesc(BankAccount::getAccountId);

        IPage<BankAccount> entityPage = baseMapper.selectPage(page, wrapper);

        Page<BankAccountVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<BankAccountVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<BankAccountVO> result = (IPage<BankAccountVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBalance(Long accountId, Long newBalance) {
        BankAccount existing = baseMapper.selectById(accountId);
        if (existing == null) {
            throw new BusinessException("银行账户不存在");
        }
        BankAccount updateEntity = new BankAccount();
        updateEntity.setAccountId(accountId);
        updateEntity.setBalance(newBalance);
        return baseMapper.updateById(updateEntity) > 0;
    }

    /**
     * 账号脱敏：保留后4位，前面用*替换
     * @param accountNumber 原始账号
     * @return 脱敏后的账号
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int keepLength = 4;
        int maskLength = accountNumber.length() - keepLength;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            sb.append('*');
        }
        sb.append(accountNumber.substring(accountNumber.length() - keepLength));
        return sb.toString();
    }

    /**
     * 实体转VO（VO中仅返回脱敏账号，不返回原始账号）
     */
    private BankAccountVO convertToVO(BankAccount entity) {
        BankAccountVO vo = new BankAccountVO();
        BeanUtils.copyProperties(entity, vo);
        // 确保VO中不包含原始账号，仅返回脱敏账号
        vo.setAccountNumberMasked(entity.getAccountNumberMasked());
        return vo;
    }
}
