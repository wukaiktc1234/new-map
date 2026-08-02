package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.BankAccount;

/**
 * 银行账户Service接口
 * 管理银行账户信息，支持余额管理与账号脱敏
 */
public interface BankAccountService extends IService<BankAccount> {

    /**
     * 创建银行账户
     * @param dto 创建DTO
     * @return 账户VO
     */
    BankAccountVO create(BankAccountCreateDTO dto);

    /**
     * 更新银行账户
     * @param accountId 账户ID
     * @param dto 更新DTO
     * @return 是否成功
     */
    boolean update(Long accountId, BankAccountUpdateDTO dto);

    /**
     * 删除银行账户（逻辑删除）
     * @param accountId 账户ID
     * @return 是否成功
     */
    boolean delete(Long accountId);

    /**
     * 获取账户详情
     * @param accountId 账户ID
     * @return 账户VO
     */
    BankAccountVO getDetail(Long accountId);

    /**
     * 分页查询银行账户
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<BankAccountVO> getPage(BankAccountQueryDTO query);

    /**
     * 更新账户余额
     * @param accountId 账户ID
     * @param newBalance 新余额（单位：分）
     * @return 是否成功
     */
    boolean updateBalance(Long accountId, Long newBalance);
}
