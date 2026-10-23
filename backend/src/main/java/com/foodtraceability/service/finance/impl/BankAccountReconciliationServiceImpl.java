package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.AlertEntity;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.BankAccountReconciliation;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.mapper.AlertEntityMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.BankAccountReconciliationMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.service.finance.BankAccountReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 银行账户对账结果Service实现
 */
@Service
public class BankAccountReconciliationServiceImpl implements BankAccountReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(BankAccountReconciliationServiceImpl.class);

    private final BankAccountMapper bankAccountMapper;
    private final FundFlowMapper fundFlowMapper;
    private final BankAccountReconciliationMapper bankAccountReconciliationMapper;
    private final AlertEntityMapper alertEntityMapper;

    @Value("${finance.reconciliation.threshold:10000}")
    private Long reconciliationThreshold = 10000L; // 默认阈值100元（单位：分）

    public BankAccountReconciliationServiceImpl(
            BankAccountMapper bankAccountMapper,
            FundFlowMapper fundFlowMapper,
            BankAccountReconciliationMapper bankAccountReconciliationMapper,
            AlertEntityMapper alertEntityMapper) {
        this.bankAccountMapper = bankAccountMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.bankAccountReconciliationMapper = bankAccountReconciliationMapper;
        this.alertEntityMapper = alertEntityMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountReconciliation reconcile(Long accountId) {
        // 获取银行账户信息
        BankAccount account = bankAccountMapper.selectById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("银行账户不存在");
        }

        // 获取账户余额
        Long balance = account.getBalance();
        if (balance == null) {
            balance = 0L;
        }

        // 从流水推导余额
        Long derivedBalance = calculateDerivedBalance(accountId);

        // 计算差额
        Long difference = balance - derivedBalance;

        // 确定对账状态
        String reconciliationStatus;
        if (difference == 0L) {
            reconciliationStatus = "MATCH";
        } else {
            reconciliationStatus = "UNMATCH";
            log.warn("账户对账不一致：accountId={}, balance={}, derivedBalance={}, difference={}",
                    accountId, balance, derivedBalance, difference);
            
            // 检查差额是否超过阈值，超过则生成异常告警
            if (Math.abs(difference) > reconciliationThreshold) {
                createReconciliationAlert(accountId, account.getAccountName(), balance, derivedBalance, difference);
            }
        }

        // 创建对账结果
        BankAccountReconciliation reconciliation = new BankAccountReconciliation();
        reconciliation.setAccountId(accountId);
        reconciliation.setBalance(balance);
        reconciliation.setDerivedBalance(derivedBalance);
        reconciliation.setDifference(difference);
        reconciliation.setReconciliationStatus(reconciliationStatus);
        reconciliation.setLastReconciledAt(LocalDateTime.now());

        // 保存对账结果
        bankAccountReconciliationMapper.insert(reconciliation);

        return reconciliation;
    }

    @Override
    public BankAccountReconciliation getReconciliationResult(Long accountId) {
        LambdaQueryWrapper<BankAccountReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccountReconciliation::getAccountId, accountId)
               .orderByDesc(BankAccountReconciliation::getLastReconciledAt)
               .last("LIMIT 1");
        return bankAccountReconciliationMapper.selectOne(wrapper);
    }

    @Override
    public Map<Long, BankAccountReconciliation> getReconciliationResultsBatch(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 一次查询取每个账户的最新对账记录，按 accountId 分组取最新
        LambdaQueryWrapper<BankAccountReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BankAccountReconciliation::getAccountId, accountIds)
               .orderByAsc(BankAccountReconciliation::getAccountId)
               .orderByDesc(BankAccountReconciliation::getLastReconciledAt);
        List<BankAccountReconciliation> allRecords = bankAccountReconciliationMapper.selectList(wrapper);
        // 每个 accountId 只保留第一条（最新的）
        Map<Long, BankAccountReconciliation> resultMap = new HashMap<>();
        for (BankAccountReconciliation record : allRecords) {
            resultMap.putIfAbsent(record.getAccountId(), record);
        }
        return resultMap;
    }

    /**
     * 从流水推导余额
     * 推导逻辑：取该账户最后一笔流水的交易后余额（balanceAfter），即为推导余额
     * 若无流水，推导余额为0
     * 原理：每笔流水创建时已计算 balanceAfter = 上一笔余额 ± 本笔金额，
     *       因此最后一笔的 balanceAfter 代表了从期初到最新的完整推导余额
     */
    private Long calculateDerivedBalance(Long accountId) {
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundFlow::getAccountId, accountId)
               .orderByDesc(FundFlow::getBusinessDate)
               .orderByDesc(FundFlow::getFlowId)
               .last("LIMIT 1");

        FundFlow lastFlow = fundFlowMapper.selectOne(wrapper);
        if (lastFlow == null || lastFlow.getBalanceAfter() == null) {
            return 0L;
        }
        return lastFlow.getBalanceAfter();
    }

    /**
     * 创建对账异常告警
     * 当差额超过阈值时生成告警记录
     */
    private void createReconciliationAlert(Long accountId, String accountName, 
                                          Long balance, Long derivedBalance, Long difference) {
        AlertEntity alert = new AlertEntity();
        alert.setName("银行账户对账异常");
        alert.setDescription(String.format("银行账户[%s]对账异常，账户余额=%d分，推导余额=%d分，差额=%d分（阈值=%d分）",
                accountName, balance, derivedBalance, difference, reconciliationThreshold));
        alert.setSeverity("HIGH");
        alert.setStatus("ACTIVE");
        alert.setValue(difference.doubleValue());
        alert.setThreshold(reconciliationThreshold.doubleValue());
        alert.setSource("BANK_ACCOUNT_RECONCILIATION");
        alert.setSourceType("BANK_ACCOUNT");
        alert.setSourceId(String.valueOf(accountId));
        alert.setAlertTime(LocalDateTime.now());
        alert.setCreateTime(LocalDateTime.now());
        alert.setUpdateTime(LocalDateTime.now());
        
        alertEntityMapper.insert(alert);
        
        log.error("银行账户对账异常告警：accountId={}, accountName={}, balance={}, derivedBalance={}, difference={}, alertId={}",
                accountId, accountName, balance, derivedBalance, difference, alert.getAlertId());
    }
}