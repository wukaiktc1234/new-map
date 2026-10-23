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

    /**
     * 递归构建科目树。
     *
     * <p>修复前：递归算出 children 却从不挂回 account，返回的「树」实际只有顶层节点，
     * 前端展开科目时永远看不到子科目。现按 parentId 逐层挂载到 {@code children}。</p>
     *
     * @param accounts 全部在用科目（扁平）
     * @param parentId 当前层级的父 ID（顶层传 0L）
     */
    private List<FinanceAccount> buildAccountTree(List<FinanceAccount> accounts, Long parentId) {
        List<FinanceAccount> tree = new ArrayList<>();
        for (FinanceAccount account : accounts) {
            if (parentId.equals(account.getParentId())) {
                // 先递归算出子节点，再挂回当前节点，保证返回的是一棵真正的树
                List<FinanceAccount> children = buildAccountTree(accounts, account.getId());
                account.setChildren(children);
                tree.add(account);
            }
        }
        return tree;
    }
}
