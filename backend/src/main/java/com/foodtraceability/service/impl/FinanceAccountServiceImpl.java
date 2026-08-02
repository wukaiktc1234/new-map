package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.finance.FinanceAccount;
import com.foodtraceability.mapper.FinanceAccountMapper;
import com.foodtraceability.service.FinanceAccountService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FinanceAccountServiceImpl extends ServiceImpl<FinanceAccountMapper, FinanceAccount> implements FinanceAccountService {

    @Override
    public List<FinanceAccount> getAccountTree() {
        List<FinanceAccount> allAccounts = baseMapper.selectAllActive();
        return buildAccountTree(allAccounts, 0L);
    }

    @Override
    public List<FinanceAccount> getAccountsByType(String accountType) {
        return baseMapper.selectByType(accountType);
    }

    @Override
    public List<FinanceAccount> getAccountsByParentId(Long parentId) {
        return baseMapper.selectByParentId(parentId);
    }

    @Override
    public FinanceAccount getByAccountCode(String accountCode) {
        LambdaQueryWrapper<FinanceAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceAccount::getAccountCode, accountCode);
        return baseMapper.selectOne(wrapper);
    }

    private List<FinanceAccount> buildAccountTree(List<FinanceAccount> accounts, Long parentId) {
        List<FinanceAccount> tree = new ArrayList<>();
        for (FinanceAccount account : accounts) {
            if (parentId.equals(account.getParentId())) {
                List<FinanceAccount> children = buildAccountTree(accounts, account.getId());
                tree.add(account);
            }
        }
        return tree;
    }
}
