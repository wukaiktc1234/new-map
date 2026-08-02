package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.service.finance.FundFlowService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资金流水Service实现
 * 流水号格式：FF + yyyyMMdd + 4位序号
 */
@Service
public class FundFlowServiceImpl extends ServiceImpl<FundFlowMapper, FundFlow>
        implements FundFlowService {

    private final BankAccountMapper bankAccountMapper;

    public FundFlowServiceImpl(BankAccountMapper bankAccountMapper) {
        this.bankAccountMapper = bankAccountMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundFlowVO create(FundFlowCreateDTO dto) {
        // 校验银行账户存在且启用
        BankAccount account = bankAccountMapper.selectById(dto.getAccountId());
        if (account == null) {
            throw new BusinessException("银行账户不存在");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException("银行账户已停用");
        }

        // 创建流水
        FundFlow entity = new FundFlow();
        BeanUtils.copyProperties(dto, entity);
        entity.setFlowNo(generateFlowNo());

        // 计算交易后余额
        Long currentBalance = account.getBalance() != null ? account.getBalance() : 0L;
        Long newBalance;
        if (dto.getFlowDirection() == 1) {
            // 收入
            newBalance = currentBalance + dto.getAmount();
        } else {
            // 支出
            newBalance = currentBalance - dto.getAmount();
        }
        entity.setBalanceAfter(newBalance);

        baseMapper.insert(entity);

        // 更新银行账户余额
        BankAccount updateAccount = new BankAccount();
        updateAccount.setAccountId(account.getAccountId());
        updateAccount.setBalance(newBalance);
        bankAccountMapper.updateById(updateAccount);

        return getDetail(entity.getFlowId());
    }

    @Override
    public FundFlowVO getDetail(Long flowId) {
        FundFlow entity = baseMapper.selectById(flowId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<FundFlowVO> getPage(FundFlowQueryDTO query) {
        Page<FundFlow> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();

        if (query.getAccountId() != null) {
            wrapper.eq(FundFlow::getAccountId, query.getAccountId());
        }
        if (query.getFlowDirection() != null) {
            wrapper.eq(FundFlow::getFlowDirection, query.getFlowDirection());
        }
        if (query.getFlowCategory() != null) {
            wrapper.eq(FundFlow::getFlowCategory, query.getFlowCategory());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(FundFlow::getBusinessDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(FundFlow::getBusinessDate, query.getEndDate());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(FundFlow::getFlowNo, query.getKeyword())
                    .or().like(FundFlow::getCounterpartyName, query.getKeyword()));
        }
        wrapper.orderByDesc(FundFlow::getBusinessDate)
               .orderByDesc(FundFlow::getFlowId);

        IPage<FundFlow> entityPage = baseMapper.selectPage(page, wrapper);

        // 批量查询关联的银行账户名称
        Map<Long, String> accountNameMap = new HashMap<>();
        List<Long> accountIds = entityPage.getRecords().stream()
                .map(FundFlow::getAccountId)
                .distinct()
                .collect(Collectors.toList());
        if (!accountIds.isEmpty()) {
            List<BankAccount> accounts = bankAccountMapper.selectBatchIds(accountIds);
            for (BankAccount acc : accounts) {
                accountNameMap.put(acc.getAccountId(), acc.getAccountName());
            }
        }

        Page<FundFlowVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<FundFlowVO> voList = entityPage.getRecords().stream()
                .map(e -> {
                    FundFlowVO vo = convertToVO(e);
                    vo.setAccountName(accountNameMap.get(e.getAccountId()));
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<FundFlowVO> result = (IPage<FundFlowVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public Map<String, Object> getStatistics(Long accountId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundFlow::getAccountId, accountId);
        if (startDate != null) {
            wrapper.ge(FundFlow::getBusinessDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(FundFlow::getBusinessDate, endDate);
        }

        List<FundFlow> flows = baseMapper.selectList(wrapper);

        long totalIncome = 0L;
        long totalExpense = 0L;
        for (FundFlow flow : flows) {
            if (flow.getFlowDirection() != null && flow.getFlowDirection() == 1) {
                totalIncome += flow.getAmount() != null ? flow.getAmount() : 0L;
            } else if (flow.getFlowDirection() != null && flow.getFlowDirection() == 2) {
                totalExpense += flow.getAmount() != null ? flow.getAmount() : 0L;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netAmount", totalIncome - totalExpense);
        result.put("count", flows.size());
        return result;
    }

    /**
     * 生成流水号：FF + yyyyMMdd + 4位序号
     */
    private String generateFlowNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FundFlow::getFlowNo, "FF" + datePart)
               .orderByDesc(FundFlow::getFlowId)
               .last("LIMIT 1");
        FundFlow lastFlow = baseMapper.selectOne(wrapper);

        int seq = 1;
        if (lastFlow != null && lastFlow.getFlowNo() != null) {
            String lastNo = lastFlow.getFlowNo();
            String seqStr = lastNo.substring(lastNo.length() - 4);
            seq = Integer.parseInt(seqStr) + 1;
        }

        return String.format("FF%s%04d", datePart, seq);
    }

    /**
     * 实体转VO
     */
    private FundFlowVO convertToVO(FundFlow entity) {
        FundFlowVO vo = new FundFlowVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
