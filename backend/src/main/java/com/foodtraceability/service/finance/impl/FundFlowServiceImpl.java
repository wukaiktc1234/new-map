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
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FundFlowServiceImpl.class);

    private final BankAccountMapper bankAccountMapper;
    private final FundFlowMapper fundFlowMapper;

    public FundFlowServiceImpl(BankAccountMapper bankAccountMapper, FundFlowMapper fundFlowMapper) {
        this.bankAccountMapper = bankAccountMapper;
        this.fundFlowMapper = fundFlowMapper;
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

        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new BusinessException("资金流水金额必须大于0");
        }
        if (dto.getFlowDirection() == null || (dto.getFlowDirection() != 1 && dto.getFlowDirection() != 2)) {
            throw new BusinessException("资金流水方向非法（1=收入 2=支出）");
        }

        // 创建流水
        FundFlow entity = new FundFlow();
        BeanUtils.copyProperties(dto, entity);
        entity.setFlowNo(generateFlowNo());
        // 状态初值：默认 2=已完成。
        // 本系统资金流水全部由「钱已经动了」的路径创建（POS 销售入账 / 付款单 / 人工确认入账），
        // 不存在草稿态；历史数据已全量归一为 2（scripts/db-fix/2026-10-23-fund-flow-status-normalize.sql），
        // 新流水再写 1 会立刻造成口径分裂。调用方可显式覆盖。
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 2);

        // 审计合规：操作人必须留痕。优先用调用方显式传入的（POS 支付路径传当前收银员），
        // 其次取当前登录上下文；都取不到写 0 并 WARN —— 绝不写入 null。
        Long operatorId = dto.getCreateUserId() != null ? dto.getCreateUserId() : SecurityUtils.getCurrentUserId();
        if (operatorId == null) {
            operatorId = 0L;
            log.warn("[AUDIT] 资金流水缺少操作人上下文，create_user_id 兜底记 0: accountId={}, direction={}, amount={}分",
                    dto.getAccountId(), dto.getFlowDirection(), dto.getAmount());
        }
        entity.setCreateUserId(operatorId);

        // 操作人姓名同时落 created_by（varchar 列；与 create_user_id 双轨：
        // ID 便于关联查询，姓名便于人工核对，历史行也可读）。
        // 取值优先级：当前登录用户名 → DTO.createUserId → 解析出的 operatorId → "system"
        String createdBy = SecurityUtils.getCurrentUsername();
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = dto.getCreateUserId() != null
                    ? String.valueOf(dto.getCreateUserId())
                    : (operatorId > 0 ? String.valueOf(operatorId) : "system");
        }
        entity.setCreatedBy(createdBy);

        // 并发安全：数据库端原子增减余额。
        // 原实现「select 出余额 → 计算 → updateById 回写」在并发下会丢更新（多终端同时支付时余额少记）。
        long delta = dto.getFlowDirection() == 1 ? dto.getAmount() : -dto.getAmount();
        int updated = bankAccountMapper.addBalance(account.getAccountId(), delta);
        if (updated == 0) {
            throw new BusinessException("银行账户不存在或已删除，无法记账");
        }
        // 回读余额作为 balance_after（同一事务内可见本事务的原子更新结果）
        BankAccount afterAccount = bankAccountMapper.selectById(account.getAccountId());
        entity.setBalanceAfter(afterAccount != null && afterAccount.getBalance() != null
                ? afterAccount.getBalance() : delta);

        baseMapper.insert(entity);

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
            // 只取账户 id + 名称两列：本接口不需要账户明细字段，
            // 避免因 bank_accounts 其他列的脏数据（如 account_type 历史 varchar 值）
            // 让整页列表 500（traceId=680bd8650713）。
            List<BankAccount> accounts = bankAccountMapper.selectList(
                    new LambdaQueryWrapper<BankAccount>()
                            .select(BankAccount::getAccountId, BankAccount::getAccountName)
                            .in(BankAccount::getAccountId, accountIds));
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
     * 生成流水号：FF + yyyyMMdd + 4位序号。
     *
     * <p>并发安全：序号来自 order_sequence 号段表的原子自增（键 {@code FLOWyyyyMMdd}），
     * 不再「查最后一条 +1」——高并发支付时原实现会拿到相同号并撞唯一键，导致流水写不进去。</p>
     */
    private String generateFlowNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequenceKey = "FLOW" + datePart;
        String prefix = "FF" + datePart;
        try {
            // 首日/首次使用时，以当天已有最大流水号初始化号段（并发下只有一条 INSERT 生效）
            Integer maxExisting = fundFlowMapper.maxFlowSequenceByPrefix(prefix);
            fundFlowMapper.initFlowSequenceIfAbsent(sequenceKey, maxExisting != null ? maxExisting : 0);
            Integer seq = fundFlowMapper.nextFlowSequence(sequenceKey);
            if (seq != null && seq > 0) {
                return String.format("%s%04d", prefix, seq);
            }
        } catch (Exception e) {
            log.warn("流水号段分配失败，降级为时间戳流水号: {}", e.getMessage());
        }
        // 兜底：号段表异常时用时间戳保证唯一（仍满足 FF 前缀，仅序号段不再是 4 位顺序号）
        return prefix + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    /**
     * 实体转VO
     */
    private FundFlowVO convertToVO(FundFlow entity) {
        FundFlowVO vo = new FundFlowVO();
        BeanUtils.copyProperties(entity, vo);
        // 状态兜底：历史行可能是 0/NULL（无对应状态），统一按「待处理」呈现，避免前端显示「未知」
        if (vo.getStatus() == null || vo.getStatus() < 1 || vo.getStatus() > 3) {
            vo.setStatus(1);
        }
        vo.setStatusName(getStatusName(vo.getStatus()));
        return vo;
    }

    /**
     * 流水状态中文名：1-待处理 2-已完成 3-已取消
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "待处理";
        }
        return switch (status) {
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "待处理";
        };
    }
}
