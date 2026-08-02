package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.finance.FinanceAccount;
import java.util.List;

public interface FinanceAccountService extends IService<FinanceAccount> {
    
    List<FinanceAccount> getAccountTree();
    
    List<FinanceAccount> getAccountsByType(String accountType);
    
    List<FinanceAccount> getAccountsByParentId(Long parentId);
    
    FinanceAccount getByAccountCode(String accountCode);
}
